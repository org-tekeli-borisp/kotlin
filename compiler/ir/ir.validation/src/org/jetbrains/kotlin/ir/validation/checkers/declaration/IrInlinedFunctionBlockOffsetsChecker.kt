/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.validation.checkers.declaration

import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.expressions.IrInlinedFunctionBlock
import org.jetbrains.kotlin.ir.validation.checkers.IrElementChecker
import org.jetbrains.kotlin.ir.validation.checkers.context.CheckerContext

object IrInlinedFunctionBlockOffsetsChecker : IrElementChecker<IrInlinedFunctionBlock>(IrInlinedFunctionBlock::class) {
    override fun check(
        element: IrInlinedFunctionBlock,
        context: CheckerContext,
    ) {
        fun checkInlinedFunctionBlockOffset(offset: Int, offsetName: String) {
            val (line, column) = element.inlinedFunctionFileEntry.getLineAndColumnNumbers(offset)
            if (line < 0 || column < 0) {
                context.error(
                    element,
                    "Inline function block's $offsetName offset $offset does not belong" +
                            " to the file entry ${element.inlinedFunctionFileEntry.name}"
                )
            }
        }

        if (element.inlinedFunctionEndOffset < 0) {
            if (element.inlinedFunctionStartOffset < 0) {
                // Both offsets are negative, nothing to check.
                return
            } else {
                checkInlinedFunctionBlockOffset(element.inlinedFunctionStartOffset, "inlinedFunctionStartOffset")
            }
        } else if (element.inlinedFunctionStartOffset > element.inlinedFunctionEndOffset) {
            // Malformed inlined function block.
            context.error(element, "Inline function block has invalid offsets: ${element.dumpOffsets()}")
            return
        } else {
            checkInlinedFunctionBlockOffset(element.inlinedFunctionEndOffset, "inlinedFunctionEndOffset")
        }

        element.statements.forEach { statement ->
            if ((statement.startOffset in 0..<element.inlinedFunctionStartOffset) ||
                (statement.endOffset >= 0 && statement.endOffset > element.inlinedFunctionEndOffset)
            ) {
                context.error(
                    statement,
                    "Statement has offsets range ${statement.dumpOffsets()} " +
                            " that exceeds the offsets range ${element.dumpOffsets()} of the inlined function block it belongs to"
                )
            }
        }
    }

    private fun IrInlinedFunctionBlock.checkOffset(offset: Int, offsetName: String) {
        val (line, column) = inlinedFunctionFileEntry.getLineAndColumnNumbers(offset)
        if (line < 0 || column < 0) {

        }
    }

    private fun IrStatement.dumpOffsets(): String = "[$startOffset:$endOffset]"
    private fun IrInlinedFunctionBlock.dumpOffsets(): String = "[$inlinedFunctionStartOffset:$inlinedFunctionEndOffset]"
}