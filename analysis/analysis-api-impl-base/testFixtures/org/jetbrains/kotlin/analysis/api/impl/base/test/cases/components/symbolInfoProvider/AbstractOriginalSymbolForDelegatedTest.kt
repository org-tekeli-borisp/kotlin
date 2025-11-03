/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.symbolInfoProvider

import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.stringRepresentation
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaDeclarationContainerSymbol
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.targets.getSingleTestTargetSymbolOfType
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractOriginalSymbolForDelegatedTest : AbstractAnalysisApiBasedTest() {
    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        val actual = copyAwareAnalyzeForTest(mainFile) {
            val containerSymbol = getSingleTestTargetSymbolOfType<KaDeclarationContainerSymbol>(testDataPath, mainFile)
            val delegatedCallables = containerSymbol.delegatedMemberScope.callables.toList()

            buildString {
                for (callableSymbol in delegatedCallables) {
                    val originalSymbol = callableSymbol.originalSymbolForDelegated

                    appendLine("CALLABLE:")
                    appendLine("  ${stringRepresentation(callableSymbol)}")
                    appendLine("ORIGINAL_SYMBOL_FOR_DELEGATED:")
                    appendLine("  ${stringRepresentation(originalSymbol)}")
                    appendLine()
                }
            }
        }

        testServices.assertions.assertEqualsToTestOutputFile(actual)
    }
}
