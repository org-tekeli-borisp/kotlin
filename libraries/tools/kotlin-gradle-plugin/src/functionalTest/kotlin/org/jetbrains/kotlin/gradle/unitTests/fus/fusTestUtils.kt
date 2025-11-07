/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests.fus

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.statistics.BuildFusService
import org.jetbrains.kotlin.gradle.plugin.statistics.FlowActionBuildFusService
import org.jetbrains.kotlin.statistics.metrics.BooleanMetrics
import org.jetbrains.kotlin.statistics.metrics.NumericalMetrics
import org.jetbrains.kotlin.statistics.metrics.StatisticsValuesConsumer
import org.jetbrains.kotlin.statistics.metrics.StringMetrics
import kotlin.test.assertNotNull

internal val Project.fusService: FlowActionBuildFusService
    get() {
        val fusServiceProvider = project.gradle.sharedServices.registrations.findByName(BuildFusService.serviceName)?.service
        assertNotNull(fusServiceProvider)

        // Only available since Gradle 8.9
        return fusServiceProvider.get() as FlowActionBuildFusService
    }

internal val Project.fusConfigurationTimeMetrics get() = fusService.getConfigurationTimeMetrics().get()
internal val Project.collectedFusConfigurationTimeMetrics: StubFUSConsumer
    get() {
        val stubConsumer = StubFUSConsumer()
        fusConfigurationTimeMetrics.forEach { it.addToConsumer(stubConsumer) }
        return stubConsumer
    }

internal class StubFUSConsumer : StatisticsValuesConsumer {

    val booleanMetrics = mutableMapOf<BooleanMetrics, Boolean>()
    val numericalMetrics = mutableMapOf<NumericalMetrics, Long>()
    val stringMetrics = mutableMapOf<StringMetrics, String>()

    override fun report(
        metric: BooleanMetrics,
        value: Boolean,
        subprojectName: String?,
        weight: Long?,
    ): Boolean {
        booleanMetrics[metric] = value
        return true
    }

    override fun report(
        metric: NumericalMetrics,
        value: Long,
        subprojectName: String?,
        weight: Long?,
    ): Boolean {
        numericalMetrics[metric] = value
        return true
    }

    override fun report(
        metric: StringMetrics,
        value: String,
        subprojectName: String?,
        weight: Long?,
    ): Boolean {
        stringMetrics[metric] = value
        return true
    }
}