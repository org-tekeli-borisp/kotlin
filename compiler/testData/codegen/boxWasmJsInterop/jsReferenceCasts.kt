// TARGET_BACKEND: WASM
// MODULE: main

// FILE: jsReferenceCasts.kt

class C(val x: Int)

fun box(): String {
    val c = C(1)
    val jsReference: JsReference<C> = c.toJsReference()
    if (jsReference !is Any)
        return "Fail: JsReference !is Any"
    // currenty forbidden with IMPOSSIBLE_IS_CHECK_ERROR
    // TODO: fix here or in the shared-prototype branch
    if (jsReference !is C)
        return "Fail: JsReference<C> !is C"

    val c2 : C = jsReference as Any as C
    if (c !== c2)
        return "Fail: implicit cast of JsReference<C>->Any->C shall result in the original object"

    val c3 : C = jsReference as C
    if (c !== c3)
        return "Fail: implicit restoration of JsReference<C>->C shall result in the original object"

    return "OK"
}


