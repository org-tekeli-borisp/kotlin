/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeSkippedPropertyWithImplicitTypeWhenResolvingInvokeCallWithExtensionReceiver

object FirSkippedPropertyWithImplicitTypeWhenResolvingInvokeCallWithExtensionReceiverChecker : FirQualifiedAccessExpressionChecker(
    MppCheckerKind.Common
) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirQualifiedAccessExpression) {
        expression.nonFatalDiagnostics.forEach {
            if (it is ConeSkippedPropertyWithImplicitTypeWhenResolvingInvokeCallWithExtensionReceiver) {
                reporter.reportOn(
                    expression.source,
                    FirErrors.SKIPPED_PROPERTY_WITH_IMPLICIT_TYPE_WHEN_RESOLVING_INVOKE_CALL_WITH_EXTENSION_RECEIVER,
                    it.property,
                )
            }
        }
    }
}