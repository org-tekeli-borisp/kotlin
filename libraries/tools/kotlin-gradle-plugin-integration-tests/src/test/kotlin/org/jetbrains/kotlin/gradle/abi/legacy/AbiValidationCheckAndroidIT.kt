/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalAbiValidation::class)

package org.jetbrains.kotlin.gradle.abi.legacy

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.abi.utils.*
import org.jetbrains.kotlin.gradle.dsl.abi.AbiValidationExtension
import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation
import org.jetbrains.kotlin.gradle.testbase.*

@AndroidGradlePluginTests
class AbiValidationCheckAndroidIT : KGPBaseTest() {
    @GradleAndroidTest
    fun testForDisabledAbiValidationWithVariants(
        gradleVersion: GradleVersion,
        agpVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        androidProject(gradleVersion, agpVersion, jdkVersion) {
            abiValidation<AbiValidationExtension> {
                variants.create("custom")
            }

            // skip lint as it shows the deprecation warning
            build("check", "-x", "lintDebug") {
                assertTasksAreNotInTaskGraph(":checkLegacyAbi")
                assertTasksAreNotInTaskGraph(":checkLegacyAbiCustom")
            }
        }
    }

    @GradleAndroidTest
    fun testForEnabledAbiValidationWithVariants(
        gradleVersion: GradleVersion,
        agpVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        androidProject(gradleVersion, agpVersion, jdkVersion) {
            abiValidation<AbiValidationExtension> {
                enabled.set(true)
                variants.create("custom")
            }

            // create the reference dumps to check
            build("updateLegacyAbi")
            build("updateLegacyAbiCustom")

            // skip lint as it shows the deprecation warning
            build("check", "-x", "lintDebug") {
                assertTasksExecuted(":checkLegacyAbi")
                assertTasksExecuted(":checkLegacyAbiCustom")
            }
        }
    }
}