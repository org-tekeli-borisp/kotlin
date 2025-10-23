/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin

import org.gradle.api.Action
import org.gradle.api.GradleException
import org.gradle.api.internal.file.FileOperations
import org.gradle.api.model.ObjectFactory
import org.gradle.process.ExecOperations
import org.gradle.process.ExecResult
import org.gradle.process.ExecSpec
import org.jetbrains.kotlin.konan.target.*
import java.io.File
import javax.inject.Inject

abstract class ExecClang @Inject constructor(
        private val platformManager: PlatformManager,
) {

    @get:Inject
    protected abstract val fileOperations: FileOperations
    @get:Inject
    protected abstract val execOperations: ExecOperations

    // FIXME: See KT-65542 for details
    private fun fixBrokenMacroExpansionInXcode15_3(target: String?) = fixBrokenMacroExpansionInXcode15_3(platformManager.targetManager(target).target)

    private fun fixBrokenMacroExpansionInXcode15_3(target: KonanTarget): List<String> {
        return when (target) {
            KonanTarget.MACOS_ARM64, KonanTarget.MACOS_X64 -> hashMapOf(
                "TARGET_OS_OSX" to "1",
            )
            KonanTarget.IOS_ARM64 -> hashMapOf(
                "TARGET_OS_EMBEDDED" to "1",
                "TARGET_OS_IPHONE" to "1",
                "TARGET_OS_IOS" to "1",
            )
            KonanTarget.TVOS_ARM64 -> hashMapOf(
                "TARGET_OS_EMBEDDED" to "1",
                "TARGET_OS_IPHONE" to "1",
                "TARGET_OS_TV" to "1",
            )
            KonanTarget.WATCHOS_ARM64, KonanTarget.WATCHOS_ARM32, KonanTarget.WATCHOS_DEVICE_ARM64 -> hashMapOf(
                "TARGET_OS_EMBEDDED" to "1",
                "TARGET_OS_IPHONE" to "1",
                "TARGET_OS_WATCH" to "1",
            )
            else -> emptyMap()
        }.map { "-D${it.key}=${it.value}" }
    }

    private fun clangArgsSpecificForKonanSources(target: KonanTaret): List<String> {
        val konanOptions = listOfNotNull(
                target.architecture.name.takeIf { target != KonanTarget.WATCHOS_ARM64 },
                "ARM32".takeIf { target == KonanTarget.WATCHOS_ARM64 },
                target.family.name.takeIf { target.family != Family.MINGW },
                "WINDOWS".takeIf { target.family == Family.MINGW },
                "MACOSX".takeIf { target.family == Family.OSX },
                "APPLE".takeIf { target.family.isAppleFamily },

                "NO_64BIT_ATOMIC".takeUnless { target.supports64BitAtomics() },
                "NO_UNALIGNED_ACCESS".takeUnless { target.supportsUnalignedAccess() },
                "FORBID_BUILTIN_MUL_OVERFLOW".takeUnless { target.supports64BitMulOverflow() },

                "OBJC_INTEROP".takeIf { target.supportsObjcInterop() },
                "HAS_FOUNDATION_FRAMEWORK".takeIf { target.hasFoundationFramework() },
                "HAS_UIKIT_FRAMEWORK".takeIf { target.hasUIKitFramework() },
                "REPORT_BACKTRACE_TO_IOS_CRASH_LOG".takeIf { target.supportsIosCrashLog() },
                "SUPPORTS_GRAND_CENTRAL_DISPATCH".takeIf { target.supportsGrandCentralDispatch },
                "SUPPORTS_SIGNPOSTS".takeIf { target.supportsSignposts },
        ).map { "KONAN_$it=1" }
        val otherOptions = listOfNotNull(
                "USE_ELF_SYMBOLS=1".takeIf { target.binaryFormat() == BinaryFormat.ELF },
                "ELFSIZE=${target.pointerBits()}".takeIf { target.binaryFormat() == BinaryFormat.ELF },
                "MACHSIZE=${target.pointerBits()}".takeIf { target.binaryFormat() == BinaryFormat.MACH_O },
                "__ANDROID__".takeIf { target.family == Family.ANDROID },
                "USE_PE_COFF_SYMBOLS=1".takeIf { target.binaryFormat() == BinaryFormat.PE_COFF },
                "UNICODE".takeIf { target.family == Family.MINGW },
                "USE_WINAPI_UNWIND=1".takeIf { target.supportsWinAPIUnwind() },
                "USE_GCC_UNWIND=1".takeIf { target.supportsGccUnwind() }
        )
        return (konanOptions + otherOptions).map { "-D$it" } + fixBrokenMacroExpansionInXcode15_3(target)
    }

    private fun clangArgsForCompiler(target: KonanTarget, compiler: String): List<String> {
        val clangArgs = platformManager.platform(target).clang
        return when (compiler) {
            "clang" -> clangArgs.clangArgs.asList()
            "clang++" -> clangArgs.clangXXArgs.asList()
            else -> throw GradleException("unsupported clang executable: $compiler")
        }
    }

    fun clangArgsForCppRuntime(targetName: String?, compiler: String): List<String> {
        val target = platformManager.targetManager(targetName).target
        return clangArgsForCompiler(target, compiler) + clangArgsSpecificForKonanSources(target)
    }

    fun resolveExecutable(executableOrNull: String?): String {
        val executable = executableOrNull ?: "clang"

        if (listOf("clang", "clang++").contains(executable)) {
            return "${platformManager.hostPlatform.absoluteLlvmHome}/bin/$executable"
        } else {
            throw GradleException("unsupported clang executable: $executable")
        }
    }

    fun resolveToolchainExecutable(target: KonanTarget, executableOrNull: String?): String {
        val executable = executableOrNull ?: "clang"

        if (listOf("clang", "clang++").contains(executable)) {
            // TODO: This is copied from `BitcodeCompiler`. Consider sharing the code instead.
            val platform = platformManager.platform(target)
            return "${platform.absoluteTargetToolchain}/bin/$executable"
        } else {
            throw GradleException("unsupported clang executable: $executable")
        }
    }

    // The konan ones invoke clang with konan provided sysroots.
    // So they require a target or assume it to be the host.
    // The target can be specified as KonanTarget or as a
    // (nullable, which means host) target name.

    fun execKonanClang(target: String, compiler: String, action: Action<in ExecSpec>): ExecResult {
        return this.execClang(
                compiler,
                clangArgsForCppRuntime(target, compiler),
                action
        )
    }

    // The toolchain ones execute clang from the toolchain.

    fun execToolchainClang(target: KonanTarget, action: Action<in ExecSpec>): ExecResult {
        val extendedAction = Action<ExecSpec> {
            action.execute(this)
            executable = resolveToolchainExecutable(target, executable)
        }
        return execOperations.exec(extendedAction)
    }

    private fun execClang(compiler: String, defaultArgs: List<String>, action: Action<in ExecSpec>): ExecResult {
        val extendedAction = Action<ExecSpec> {
            action.execute(this)
            executable = resolveExecutable(compiler)

            val hostPlatform = platformManager.hostPlatform
            environment["PATH"] = fileOperations.configurableFiles(hostPlatform.clang.clangPaths).asPath +
                    File.pathSeparator + environment["PATH"]
            args = args + defaultArgs
        }
        return execOperations.exec(extendedAction)
    }

    companion object {
        @JvmStatic
        fun create(objects: ObjectFactory, platformManager: PlatformManager) =
                objects.newInstance(ExecClang::class.java, platformManager)
    }
}
