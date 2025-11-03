/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:JvmName("ConstantExpressionUtils")

package org.jetbrains.kotlin.psi.utils

import com.intellij.util.text.LiteralFormatUtil
import org.jetbrains.kotlin.KtStubBasedElementTypes.BOOLEAN_CONSTANT
import org.jetbrains.kotlin.KtStubBasedElementTypes.CHARACTER_CONSTANT
import org.jetbrains.kotlin.KtStubBasedElementTypes.FLOAT_CONSTANT
import org.jetbrains.kotlin.KtStubBasedElementTypes.INTEGER_CONSTANT
import org.jetbrains.kotlin.KtStubBasedElementTypes.NULL
import org.jetbrains.kotlin.psi.KtConstantExpression
import org.jetbrains.kotlin.psi.stubs.ConstantValueKind
import org.jetbrains.kotlin.psi.stubs.KotlinConstantExpressionStub
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementType

/**
 * Checks whether the [text] representation of a number literal has a long number suffix.
 */
fun hasLongNumericLiteralSuffix(text: String): Boolean {
    return text.lastOrNull()?.let { it == 'l' || it == 'L' } ?: false
}

/**
 * Checks whether the [text] representation of a number literal has an unsigned number suffix.
 */
fun hasUnsignedNumericLiteralSuffix(text: String): Boolean {
    return text.lastOrNull()?.let { it == 'u' || it == 'U' } ?: false
}

/**
 * Checks whether the [text] representation of a number literal has both along and unsigned number suffixes.
 */
fun hasUnsignedLongNumericLiteralSuffix(text: String): Boolean {
    val length = text.length
    return length >= 2 && text[length - 2].let { it == 'u' || it == 'U' } && text[length - 1].let { it == 'l' || it == 'L' }
}

/**
 * Checks whether the [text] representation of a number literal has leading zeros.
 */
fun hasLeadingZeros(text: String): Boolean {
    return text.length > 1 && text[0] == '0' && text[1].let { it.isDigit() || it == '_' }
}

fun hasFloatNumericLiteralSuffix(text: String): Boolean {
    return text.lastOrNull()?.let { it == 'f' || it == 'F' } ?: false
}

/**
 * Parses the [text] representation of a number literal into a [Number] instance.
 *
 * For integer literals, always returns a [Long] instance.
 * For floating-point literals, returns either a [Float] or a [Double], depending on the literal precision.
 * Returns `null` if the text is not a valid representation of a number literal.
 *
 * @param isFloatingPointLiteral Specifies whether the number is floating-point (`Float` or `Double`)
 * or integer (`Byte`, `Short`, `Int` or `Long`).
 */
fun parseNumericLiteral(text: String, isFloatingPointLiteral: Boolean): Number? {
    val canonicalText = LiteralFormatUtil.removeUnderscores(text)
    return if (isFloatingPointLiteral) parseDecimalNumberLiteral(canonicalText) else parseLongNumericLiteral(canonicalText)
}

private fun parseLongNumericLiteral(text: String): Long? {
    return try {
        val firstIndex: Int
        val lastIndex: Int
        val radix: Int
        val isUnsigned: Boolean

        when {
            hasUnsignedLongNumericLiteralSuffix(text) -> {
                isUnsigned = true
                lastIndex = text.length - 2
            }
            hasUnsignedNumericLiteralSuffix(text) -> {
                isUnsigned = true
                lastIndex = text.length - 1
            }
            hasLongNumericLiteralSuffix(text) -> {
                isUnsigned = false
                lastIndex = text.length - 1
            }
            else -> {
                isUnsigned = false
                lastIndex = text.length
            }
        }

        if (text.length >= 2 && text[0] == '0') {
            when (text[1]) {
                'x', 'X' -> {
                    firstIndex = 2
                    radix = 16
                }
                'b', 'B' -> {
                    firstIndex = 2
                    radix = 2
                }
                else -> {
                    firstIndex = 0
                    radix = 10
                }
            }
        } else {
            firstIndex = 0
            radix = 10
        }

        val number = text.substring(firstIndex, lastIndex)

        if (isUnsigned) {
            java.lang.Long.parseUnsignedLong(number, radix)
        } else {
            java.lang.Long.parseLong(number, radix)
        }
    } catch (_: NumberFormatException) {
        null
    }
}

private fun parseDecimalNumberLiteral(text: String): Number? {
    if (hasFloatNumericLiteralSuffix(text)) {
        return parseFloat(text)
    }
    return parseDouble(text)
}

private fun parseDouble(text: String): Double? {
    return try {
        java.lang.Double.parseDouble(text)
    } catch (_: NumberFormatException) {
        null
    }
}

private fun parseFloat(text: String): Float? {
    return try {
        java.lang.Float.parseFloat(text)
    } catch (_: NumberFormatException) {
        null
    }
}

/**
 * Converts the given [text], either `true` or `false`, to a boolean value.
 *
 * @throws IllegalStateException if the [text] does not represent a valid boolean value.
 */
@Throws(IllegalStateException::class)
fun parseBooleanLiteral(text: String): Boolean {
    return when (text) {
        "true" -> true
        "false" -> false
        else -> throw IllegalStateException("'$text' is not a valid boolean literal")
    }
}

private val FP_LITERAL_PARTS = "([_\\d]*)\\.?([_\\d]*)e?[+-]?([_\\d]*)[f]?".toRegex()

/**
 * Checks whether the given [text] contains an underscore in an illegal position.
 * Underscores are allowed only between digits, not at the beginning or end of the number or one of its parts.
 */
fun hasIllegallyPositionedUnderscore(text: String, isFloatingPoint: Boolean): Boolean {
    return if (isFloatingPoint) {
        FP_LITERAL_PARTS.findAll(text).any { matchResult ->
            matchResult.groupValues.any { it.startsWith("_") || it.endsWith("_") }
        }
    } else {
        var start = 0
        var end: Int = text.length - 1
        if (text.startsWith("0x", ignoreCase = true) || text.startsWith("0b", ignoreCase = true)) start += 2
        if (text.endsWith('l', ignoreCase = true)) --end

        text.elementAtOrNull(start) == '_' || text.elementAtOrNull(end) == '_'
    }
}

/**
 * Converts the given [ConstantValueKind] to the corresponding [com.intellij.psi.tree.IElementType].
 */
fun ConstantValueKind.toConstantExpressionElementType(): KtStubElementType<out KotlinConstantExpressionStub, KtConstantExpression> {
    return when (this) {
        ConstantValueKind.NULL -> NULL
        ConstantValueKind.BOOLEAN_CONSTANT -> BOOLEAN_CONSTANT
        ConstantValueKind.FLOAT_CONSTANT -> FLOAT_CONSTANT
        ConstantValueKind.CHARACTER_CONSTANT -> CHARACTER_CONSTANT
        ConstantValueKind.INTEGER_CONSTANT -> INTEGER_CONSTANT
    }
}

/**
 * Converts the given [com.intellij.psi.tree.IElementType] to the corresponding [ConstantValueKind].
 * The element type must be one of the constant expression types. Otherwise, an [IllegalArgumentException] is thrown.
 */
fun KtStubElementType<out KotlinConstantExpressionStub, KtConstantExpression>.toConstantValueKind(): ConstantValueKind {
    return when (this) {
        NULL -> ConstantValueKind.NULL
        BOOLEAN_CONSTANT -> ConstantValueKind.BOOLEAN_CONSTANT
        FLOAT_CONSTANT -> ConstantValueKind.FLOAT_CONSTANT
        CHARACTER_CONSTANT -> ConstantValueKind.CHARACTER_CONSTANT
        INTEGER_CONSTANT -> ConstantValueKind.INTEGER_CONSTANT
        else -> throw IllegalArgumentException("Unknown constant node type: $this")
    }
}