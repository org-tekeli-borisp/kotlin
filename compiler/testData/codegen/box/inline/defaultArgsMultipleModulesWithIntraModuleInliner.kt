// ISSUE: KT-82065
// LANGUAGE: +IrIntraModuleInlinerBeforeKlibSerialization +IrCrossModuleInlinerBeforeKlibSerialization
// MODULE: lib
// FILE: lib.kt

inline fun foo(a: Int = 1) = a + 1
inline fun bar(b: Int, c: Int = 2) = b + c

// MODULE: main(lib)
// FILE: main.kt

fun box(): String {
    foo()
    // ^^^ problematic call:
    // main/main.kt:13:5: error: [IR VALIDATION] KlibIrValidationBeforeLoweringPhase: Statement in IrInlinedFunctionBlock has offsets range [71:76] that exceeds the offsets range [157:184] of the block
    //   VAR IR_TEMPORARY_VARIABLE_FOR_INLINED_PARAMETER name:a type:kotlin.Int [val]
    //     inside INLINED_BLOCK type=kotlin.Int origin=null
    //       inside RETURNABLE_BLOCK type=kotlin.Int origin=null
    //         inside TYPE_OP type=kotlin.Unit origin=IMPLICIT_COERCION_TO_UNIT typeOperand=kotlin.Unit
    //           inside BLOCK_BODY
    //             inside FUN name:box visibility:public modality:FINAL <> () returnType:kotlin.String
    //               inside FILE fqName:<root> fileName:main/main.kt
    foo(5)
    bar(10)
    bar(10, 20)
    return "OK"
}