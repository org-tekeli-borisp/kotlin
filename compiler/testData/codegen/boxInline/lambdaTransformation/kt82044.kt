// NO_CHECK_LAMBDA_INLINING
// LANGUAGE: +IrIntraModuleInlinerBeforeKlibSerialization +IrCrossModuleInlinerBeforeKlibSerialization

// MODULE: a
// FILE: a.kt

inline fun <T, R> T.myLet(block: (T) -> R): R {
    return block(this)
}

// MODULE: b(a)
// FILE: b.kt

fun box(): String {
    "OK".myLet { return it }
}
