/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:OptIn(ExperimentalBuildToolsApi::class)

package org.jetbrains.kotlin.buildtools.api.internal.wrappers

import org.jetbrains.kotlin.buildtools.api.*
import org.jetbrains.kotlin.buildtools.api.jvm.ClasspathEntrySnapshot
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain
import org.jetbrains.kotlin.buildtools.api.jvm.JvmSnapshotBasedIncrementalCompilationOptions
import org.jetbrains.kotlin.buildtools.api.jvm.operations.JvmClasspathSnapshottingOperation
import org.jetbrains.kotlin.buildtools.api.jvm.operations.JvmCompilationOperation
import java.nio.file.Path
import java.nio.file.Paths

/**
 * A wrapper class for `KotlinToolchains` to accommodate functionality
 * changes and compatibility adjustments for versions pre Kotlin 2.3.0.
 *
 * Delegates the majority of functionality to the `base` implementation,
 * while selectively overriding methods to either introduce new behavior
 * or adapt existing operations.
 *
 * @param base The base implementation of `KotlinToolchains` to wrap.
 */
@Suppress("DEPRECATION")
internal class PreKotlin230Wrapper(
    private val base: KotlinToolchains,
) : KotlinToolchains by base {

    @Suppress("UNCHECKED_CAST")
    override fun <T : KotlinToolchains.Toolchain> getToolchain(type: Class<T>): T = when (type) {
        JvmPlatformToolchain::class.java -> JvmPlatformToolchainWrapper(base.getToolchain(type))
        else -> base.getToolchain(type)
    } as T

    override fun createBuildSession(): KotlinToolchains.BuildSession {
        return BuildSessionWrapper(base.createBuildSession())
    }

    class BuildSessionWrapper(private val base: KotlinToolchains.BuildSession) : KotlinToolchains.BuildSession by base {
        override fun <R> executeOperation(operation: BuildOperation<R>): R {
            return this.executeOperation(operation, logger = null)
        }

        override fun <R> executeOperation(operation: BuildOperation<R>, executionPolicy: ExecutionPolicy, logger: KotlinLogger?): R {
            // we need to unwrap due to an `operation is BuildOperationImpl` check inside `executeOperation`
            val realOperation = if (operation is BuildOperationWrapper) operation.baseOperation else operation
            return base.executeOperation(realOperation, executionPolicy, logger)
        }
    }

    private abstract class BuildOperationWrapper<R>(val baseOperation: BuildOperation<R>) : BuildOperation<R>

    private class JvmPlatformToolchainWrapper(private val base: JvmPlatformToolchain) : JvmPlatformToolchain by base {
        override fun jvmCompilationOperationBuilder(sources: List<Path>, destinationDirectory: Path): JvmCompilationOperationWrapper {
            return JvmCompilationOperationWrapper(
                base.createJvmCompilationOperation(sources, destinationDirectory),
                this,
                sources,
                destinationDirectory
            )
        }

        @Deprecated(
            "Use newJvmCompilationOperation instead",
            replaceWith = ReplaceWith("newJvmCompilationOperation(sources, destinationDirectory)")
        )
        override fun createJvmCompilationOperation(sources: List<Path>, destinationDirectory: Path): JvmCompilationOperation {
            return jvmCompilationOperationBuilder(sources, destinationDirectory)
        }

        @Deprecated("Use createSnapshotBasedIcOptions instead")
        override fun snapshotBasedIcOptionsBuilder(
            workingDirectory: Path,
            sourcesChanges: SourcesChanges,
            dependenciesSnapshotFiles: List<Path>,
            shrunkClasspathSnapshot: Path
        ): JvmSnapshotBasedIncrementalCompilationOptions.Builder {
            val options = createJvmCompilationOperation(emptyList(), Paths.get("")).createSnapshotBasedIcOptions()
            return JvmSnapshotBasedIncrementalCompilationOptionsWrapper(
                workingDirectory,
                sourcesChanges,
                dependenciesSnapshotFiles,
                shrunkClasspathSnapshot,
                options
            )
        }

        inner class JvmSnapshotBasedIncrementalCompilationOptionsWrapper(
            workingDirectory: Path,
            sourcesChanges: SourcesChanges,
            dependenciesSnapshotFiles: List<Path>,
            shrunkClasspathSnapshot: Path,
            options: JvmSnapshotBasedIncrementalCompilationOptions,
        ) : org.jetbrains.kotlin.buildtools.api.jvm.JvmSnapshotBasedIncrementalCompilationConfiguration(
            workingDirectory,
            sourcesChanges,
            dependenciesSnapshotFiles,
            shrunkClasspathSnapshot,
            options
        ), JvmSnapshotBasedIncrementalCompilationOptions, JvmSnapshotBasedIncrementalCompilationOptions.Builder {
            override fun toBuilder(): JvmSnapshotBasedIncrementalCompilationOptions.Builder = deepCopy()

            override fun <V> get(key: JvmSnapshotBasedIncrementalCompilationOptions.Option<V>): V {
                return options[key]
            }

            override fun <V> set(
                key: JvmSnapshotBasedIncrementalCompilationOptions.Option<V>,
                value: V,
            ) {
                options[key] = value
            }

            override fun build(): JvmSnapshotBasedIncrementalCompilationOptions = deepCopy()

            private fun deepCopy(): JvmSnapshotBasedIncrementalCompilationOptionsWrapper {
                return JvmSnapshotBasedIncrementalCompilationOptionsWrapper(
                    workingDirectory, sourcesChanges, dependenciesSnapshotFiles, shrunkClasspathSnapshot,
                    createJvmCompilationOperation(emptyList(), Paths.get("")).createSnapshotBasedIcOptions().also { newOptions ->
                        JvmSnapshotBasedIncrementalCompilationOptions::class.java.declaredFields.filter {
                            it.type.isAssignableFrom(
                                JvmSnapshotBasedIncrementalCompilationOptions.Option::class.java
                            )
                        }.forEach { field ->
                            try {
                                @Suppress("UNCHECKED_CAST")
                                newOptions[field.get(JvmSnapshotBasedIncrementalCompilationOptions.Companion) as JvmSnapshotBasedIncrementalCompilationOptions.Option<Any?>] =
                                    this[field.get(JvmSnapshotBasedIncrementalCompilationOptions.Companion) as JvmSnapshotBasedIncrementalCompilationOptions.Option<*>]
                            } catch (_: IllegalStateException) {
                                // this field was not set and has no default
                            }
                        }
                    }
                )
            }
        }

        override fun classpathSnapshottingOperationBuilder(classpathEntry: Path): JvmClasspathSnapshottingOperationWrapper {
            return JvmClasspathSnapshottingOperationWrapper(base.createClasspathSnapshottingOperation(classpathEntry), this, classpathEntry)
        }

        @Deprecated(
            "Use classpathSnapshottingOperationBuilder instead",
            replaceWith = ReplaceWith("classpathSnapshottingOperationBuilder(classpathEntry)")
        )
        override fun createClasspathSnapshottingOperation(classpathEntry: Path): JvmClasspathSnapshottingOperation {
            return classpathSnapshottingOperationBuilder(classpathEntry)
        }


        private inner class JvmClasspathSnapshottingOperationWrapper(
            private val base: JvmClasspathSnapshottingOperation,
            private val toolchain: JvmPlatformToolchainWrapper,
            private val classpathEntry: Path,
        ) : BuildOperationWrapper<ClasspathEntrySnapshot>(base), JvmClasspathSnapshottingOperation by base,
            JvmClasspathSnapshottingOperation.Builder {
            override fun toBuilder(): JvmClasspathSnapshottingOperation.Builder {
                return copy()
            }

            override fun build(): JvmClasspathSnapshottingOperation {
                return copy()
            }

            fun copy() = toolchain.classpathSnapshottingOperationBuilder(classpathEntry)
                .also { newOperation: JvmClasspathSnapshottingOperationWrapper ->
                    newOperation.copyBuildOperationOptions(this)
                    JvmClasspathSnapshottingOperation::class.java.declaredFields.filter {
                        it.type.isAssignableFrom(
                            JvmClasspathSnapshottingOperation.Option::class.java
                        )
                    }.forEach { field ->
                        try {
                            @Suppress("UNCHECKED_CAST")
                            newOperation[field.get(JvmClasspathSnapshottingOperation.Companion) as JvmClasspathSnapshottingOperation.Option<Any?>] =
                                this[field.get(JvmClasspathSnapshottingOperation.Companion) as JvmClasspathSnapshottingOperation.Option<*>]
                        } catch (_: IllegalStateException) {
                            // this field was not set and has no default
                        }
                    }
                }
        }

        private inner class JvmCompilationOperationWrapper(
            private val base: JvmCompilationOperation,
            private val toolchain: JvmPlatformToolchainWrapper,
            private val sources: List<Path>,
            private val destinationDirectory: Path,
        ) : BuildOperationWrapper<CompilationResult>(base), JvmCompilationOperation by base, JvmCompilationOperation.Builder {
            override fun toBuilder(): JvmCompilationOperation.Builder {
                return copy()
            }

            override fun build(): JvmCompilationOperation {
                return copy()
            }

            private fun copy(): JvmCompilationOperationWrapper {
                return toolchain.jvmCompilationOperationBuilder(sources, destinationDirectory)
                    .also { newOperation: JvmCompilationOperationWrapper ->
                        newOperation.copyBuildOperationOptions(this)
                        JvmCompilationOperation::class.java.declaredFields.filter { it.type.isAssignableFrom(JvmCompilationOperation.Option::class.java) }
                            .forEach { field ->
                                try {
                                    @Suppress("UNCHECKED_CAST")
                                    newOperation[field.get(JvmCompilationOperation.Companion) as JvmCompilationOperation.Option<Any?>] =
                                        this[field.get(JvmCompilationOperation.Companion) as JvmCompilationOperation.Option<*>]
                                } catch (_: IllegalStateException) {
                                    // this field was not set and has no default
                                }
                            }
                        newOperation.compilerArguments.applyArgumentStrings(this.compilerArguments.toArgumentStrings())
                    }
            }
        }

        private fun BuildOperationWrapper<*>.copyBuildOperationOptions(from: BuildOperation<*>) {
            BuildOperation::class.java.declaredFields.filter { it.type.isAssignableFrom(BuildOperation.Option::class.java) }
                .forEach { field ->
                    try {
                        @Suppress("UNCHECKED_CAST")
                        this[field.get(BuildOperation.Companion) as BuildOperation.Option<Any?>] =
                            from[field.get(BuildOperation.Companion) as BuildOperation.Option<*>]
                    } catch (_: IllegalStateException) {
                        // this field was not set and has no default
                    }
                }
        }
    }
}