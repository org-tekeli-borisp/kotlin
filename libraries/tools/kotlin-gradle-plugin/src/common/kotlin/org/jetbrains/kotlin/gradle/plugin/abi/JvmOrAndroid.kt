/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.abi

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.dsl.abi.AbiValidationVariantSpec
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget

/**
 * Finalizes the configuration of the report variant for the JVM version of the Kotlin Gradle plugin.
 */
internal fun AbiValidationVariantSpec.finalizeJvmVariant(
    project: Project,
    abiClasspath: Configuration,
    target: KotlinTarget,
) {
    finalizeVariant(project, abiClasspath,KotlinCompilation.MAIN_COMPILATION_NAME, target)
}


/**
 * Finalizes the configuration of the report variant for the Android version of the Kotlin Gradle plugin.
 */
internal fun AbiValidationVariantSpec.finalizeAndroidVariant(
    project: Project,
    abiClasspath: Configuration,
    target: KotlinTarget,
) {
    finalizeVariant(project, abiClasspath,ANDROID_RELEASE_BUILD_TYPE, target)
}

private fun AbiValidationVariantSpec.finalizeVariant(
    project: Project,
    abiClasspath: Configuration,
    compilationName: String,
    target: KotlinTarget
) {
    val taskSet = AbiValidationTaskSet(project, name)
    taskSet.setClasspath(abiClasspath)

    val classfiles = project.files()
    taskSet.addSingleJvmTarget(classfiles)
    target.compilations.withCompilationIfExists(compilationName) {
        classfiles.from(output.classesDirs)
    }
}

internal fun Project.addDependencyWithCheckTask(variant: AbiValidationVariantSpec, isEnabled: Provider<Boolean>) {
    // extract the task provider to pass it into the mapping lambda instead of the variant overall
    val legacyCheckTaskProvider = variant.legacyDump.legacyCheckTaskProvider

    // add dependency on checkLegacyAbi task only if ABI validation is enabled
    val dependencyTasks = isEnabled.map {
        if (it) {
            listOf(legacyCheckTaskProvider)
        } else {
            emptyList()
        }
    }

    tasks.getByName("check").dependsOn(dependencyTasks)
}
