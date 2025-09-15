/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tests

import org.jetbrains.kotlin.generators.dsl.junit5.generateTestGroupSuiteWithJUnit5
import org.jetbrains.kotlin.generators.model.annotation
import org.jetbrains.kotlin.js.test.klib.AbstractCustomJsCompilerFirstPhaseTest
import org.jetbrains.kotlin.js.test.klib.AbstractCustomJsCompilerSecondPhaseTest
import org.jetbrains.kotlin.test.HeavyTest

fun main(args: Array<String>) {
    val testsRoot = args[0]

    val jvmOnlyBoxTests = listOf("compileKotlinAgainstKotlin")

    generateTestGroupSuiteWithJUnit5(args) {
        testGroup(testsRoot, "compiler/testData/codegen") {
            testClass<AbstractCustomJsCompilerFirstPhaseTest>(
                annotations = listOf(annotation(HeavyTest::class.java))
            ) {
                model("box", excludeDirs = jvmOnlyBoxTests)
                model("boxInline")
            }
        }
        testGroup(testsRoot, "compiler/testData/codegen") {
            testClass<AbstractCustomJsCompilerSecondPhaseTest>(
                annotations = listOf(annotation(HeavyTest::class.java))
            ) {
                model("box", excludeDirs = jvmOnlyBoxTests)
                model("boxInline")
            }
        }
    }
}
