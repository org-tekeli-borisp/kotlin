/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.checkers.*
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.references.FirResolvedErrorReference
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.references.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeInapplicableWrongReceiver
import org.jetbrains.kotlin.fir.scopes.impl.typeAliasConstructorInfo
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.types.*

object FirUpperBoundViolatedQualifiedAccessExpressionChecker : FirQualifiedAccessExpressionChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirQualifiedAccessExpression) {
        // something that contains the type parameters
        // declarations with their declared bounds.
        // it may be the called function declaration
        // or the class declaration
        val calleeSymbol = when (val calleeReference = expression.calleeReference) {
            is FirResolvedErrorReference -> {
                if (calleeReference.diagnostic is ConeInapplicableWrongReceiver) {
                    return
                }
                calleeReference.toResolvedCallableSymbol()
            }
            is FirResolvedNamedReference -> calleeReference.toResolvedCallableSymbol()
            else -> null
        }
        val typeArguments: List<ConeTypeProjection> = expression.typeArguments.toTypeArgumentsWithSourceInfo()
        val typeParameters: List<FirTypeParameterSymbol> = calleeSymbol?.typeParameterSymbols ?: return

        // Neither common calls nor type alias constructor calls may contain projections
        // That should be checked somewhere else
        if (typeArguments.any { it !is ConeKotlinType }) {
            return
        }

        if (typeArguments.size != typeParameters.size) return

        val substitutor = createSubstitutorForUpperBoundViolationCheck(
            typeParameters,
            typeArguments,
            context.session,
        )

        val typeAliasConstructorInfo = (calleeSymbol as? FirConstructorSymbol)?.typeAliasConstructorInfo

        if (typeAliasConstructorInfo != null) {
            val typealiasType: ConeClassLikeType = typeAliasConstructorInfo.typeAliasSymbol.defaultType()
            checkUpperBoundViolated(
                typeRef = null,
                // Return types of constructors obtained from typealiases (e.g., `TA()`) remain expanded even when
                // `aliasedTypeExpansionGloballyEnabled == false`, hence the workaround instead of `abbreviatedTypeOrSelf`.`
                // See: `TypeAliasConstructorsSubstitutingScope.createTypealiasConstructor` and `typeAliasConstructorCrazyProjections.fir.kt`.
                notExpandedType = substitutor.substituteOrSelf(typealiasType) as ConeClassLikeType,
                fallbackSource = expression.source,
            )
        } else {
            checkUpperBoundViolated(
                typeParameters,
                typeArguments,
                substitutor,
                fallbackSource = expression.source,
                isTypealiasExpansion = false,
            )
        }
    }

    private fun ConeTypeProjection.withSourceRecursive(expression: FirQualifiedAccessExpression): ConeTypeProjection {
        // Recursively apply source to any arguments of this type.
        val type = when {
            this is ConeClassLikeType && typeArguments.isNotEmpty() -> withArguments { it.withSourceRecursive(expression) }
            else -> this
        }

        // Try to match the expanded type arguments back to the original expression type arguments.
        return when (val argument = expression.typeArguments.find { it.toConeTypeProjection() === this }) {
            // Unable to find a matching argument, fall back to marking the entire expression.
            null -> type.withSource(FirTypeRefSource(null, expression.source))
            // Found the original argument!
            else -> type.withSource(FirTypeRefSource((argument as? FirTypeProjectionWithVariance)?.typeRef, argument.source))
        }
    }
}
