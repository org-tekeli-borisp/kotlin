/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.fir.SessionHolder
import org.jetbrains.kotlin.fir.isEnabled
import org.jetbrains.kotlin.fir.types.ConeAttribute
import org.jetbrains.kotlin.fir.types.ConeAttributes
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.withAttributes
import kotlin.reflect.KClass

/**
 * Contains the type that would have been present previously in place of the current one,
 * if the corresponding [LanguageFeature] had been disabled.
 * Present for the duration of the feature's deprecation cycle to relax the diagnostics
 * to warnings.
 */
data class TypeHasChangedAttribute(
    val oldType: ConeKotlinType,
    val languageFeature: LanguageFeature,
) : ConeAttribute<TypeHasChangedAttribute>() {
    // Those methods should not matter too much because it's only assumed to be used for explicit type arguments
    // for which we don't expect to perform complex operations
    override fun union(other: TypeHasChangedAttribute?): TypeHasChangedAttribute? = null
    override fun intersect(other: TypeHasChangedAttribute?): TypeHasChangedAttribute? = null
    override fun add(other: TypeHasChangedAttribute?): TypeHasChangedAttribute = other ?: this
    override fun isSubtypeOf(other: TypeHasChangedAttribute?): Boolean = true

    override val key: KClass<out TypeHasChangedAttribute>
        get() = TypeHasChangedAttribute::class

    override val keepInInferredDeclarationType: Boolean
        get() = true
}

val ConeAttributes.typeHasChangedAttribute: TypeHasChangedAttribute? by ConeAttributes.attributeAccessor()

//fun ConeKotlinType.hasChangedBecauseOf(feature: LanguageFeature): Boolean =
//    attributes.typeHasChangedAttribute.let { it != null && it.languageFeature == feature }

fun ConeKotlinType.typeChangeRelatedTo(feature: LanguageFeature): TypeHasChangedAttribute? =
    attributes.typeHasChangedAttribute?.takeIf { it.languageFeature == feature }

context(sessionHolder: SessionHolder)
fun <T : ConeKotlinType> T.withOldTypeBefore(feature: LanguageFeature, createOldType: () -> T): T = when {
    feature.isEnabled() -> this
    else -> when (val oldType = createOldType()) {
        this -> this
        else -> withAttributes(attributes.add(TypeHasChangedAttribute(oldType, feature)))
    }
}

///**
// * Returns the type that used to be there before the [feature] was enabled by default (if it was),
// * judging by their [LanguageFeature.sinceVersion].
// * The idea is to tell if the [feature] has anything to do with this specific type change,
// * and if it does, make use of the stored type information.
// */
//fun ConeKotlinType.typeBefore(feature: LanguageFeature): ConeKotlinType? {
//    val typeHasChangedAttribute = attributes.typeHasChangedAttribute ?: return null
//    val attributesSinceKotlin = typeHasChangedAttribute.languageFeature.sinceVersion
//    val featuresSinceKotlin = feature.sinceVersion
//
//    return when {
//        attributesSinceKotlin == null -> typeHasChangedAttribute.oldType
//        featuresSinceKotlin == null -> null
//        featuresSinceKotlin <= attributesSinceKotlin -> typeHasChangedAttribute.oldType
//        else -> null
//    }
//}
