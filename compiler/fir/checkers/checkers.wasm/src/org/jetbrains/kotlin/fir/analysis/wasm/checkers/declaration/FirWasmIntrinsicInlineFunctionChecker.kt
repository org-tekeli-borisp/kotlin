/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.wasm.checkers.declaration

import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirSimpleFunctionChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.wasm.FirWasmErrors.INTRINSICS_INLINED_IN_KLIB
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.declarations.utils.isInline
import org.jetbrains.kotlin.fir.declarations.utils.visibility
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.visitors.FirDefaultVisitorVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds

object FirWasmIntrinsicInlineFunctionChecker : FirSimpleFunctionChecker(MppCheckerKind.Platform) {
    private val wasmInternal = FqName("kotlin.wasm.internal")
    private val excludedFromCodegenFqName = ClassId(wasmInternal, Name.identifier("ExcludedFromCodegen"))
    private val wasmOpFqName = ClassId(wasmInternal, Name.identifier("WasmOp"))
    private val wasmNoOpCastFqName = ClassId(wasmInternal, Name.identifier("WasmNoOpCast"))
    private val noKlibInlining = ClassId(FqName("kotlin"), Name.identifier("NoKlibInlining"))

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirSimpleFunction) {
        if (context.session.moduleData.name.toString() != "<kotlin>") return
        if (!declaration.isInline) return
        if (declaration.visibility != Visibilities.Public && !declaration.hasAnnotation(StandardClassIds.Annotations.PublishedApi, context.session)) return
        if (declaration.hasAnnotation(noKlibInlining, context.session)) return

        val body = declaration.body ?: return

        val intrinsicCallFinder = object : FirDefaultVisitorVoid() {
            override fun visitElement(element: FirElement) {
                element.acceptChildren(this)
            }

            override fun visitFunctionCall(functionCall: FirFunctionCall) {
                val resolvedSymbol = functionCall.toResolvedCallableSymbol() ?: return

                val hasWasmIntrinsicCall =
                    resolvedSymbol.hasAnnotation(excludedFromCodegenFqName, context.session) ||
                    resolvedSymbol.hasAnnotation(wasmOpFqName, context.session) ||
                    resolvedSymbol.hasAnnotation(wasmNoOpCastFqName, context.session) ||
                    resolvedSymbol.hasAnnotation(StandardClassIds.Annotations.PublishedApi, context.session)

                if (hasWasmIntrinsicCall) {
                    reporter.reportOn(functionCall.source, INTRINSICS_INLINED_IN_KLIB)
                }

                super.visitFunctionCall(functionCall)
            }
        }

        body.accept(intrinsicCallFinder)
    }
}