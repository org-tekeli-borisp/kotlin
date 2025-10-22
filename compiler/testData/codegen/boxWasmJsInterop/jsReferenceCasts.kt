// TARGET_BACKEND: WASM
// MODULE: main

// FILE: jsReferenceCasts.kt

class C(val x: Int)

val c = C(1)

fun testTypeOperations(obj: JsReference<C>) : String? {
    if (obj !is Any)
        return "!is Any"
    if (obj !is C)
        return "!is C"
    if (obj is String)
        return "is String"
    if (obj != c)
        return "!= C"
    if (obj !== c)
        return "!== C"
    return null
}

// same checks as above, but with another formal type of the object
// unfortunately, we have to duplicate the code as external types cannot be used as reified type arguments
fun testTypeOperations(obj: JsReference<String>) : String? {
    if (obj !is Any)
        return "!is Any"
    if (obj !is C)
        return "!is C"
    if (obj is String)
        return "is String"
    if (obj != c)
        return "!= C"
    if (obj !== c)
        return "!== C"
    return null
}

fun testTypeOperations(obj: JsAny) : String? {
    if (obj !is Any)
        return "!is Any"
    if (obj !is C)
        return "!is C"
    if (obj != c)
        return "!= C"
    if (obj !== c)
        return "!== C"
    return null
}

fun box(): String {
    val jsReference: JsReference<C> = c.toJsReference()
    testTypeOperations(jsReference)?.let { return "Fail: JsReference<C> $it"}

    val jsReferenceWithIncorrectType: JsReference<String> = jsReference.unsafeCast<JsReference<String>>()
    testTypeOperations(jsReferenceWithIncorrectType)?.let { return "Fail: JsReference<C>.unsafeCast<JsReference<String>>() $it"}

    val jsReferenceAsJsAny: JsAny = jsReference
    testTypeOperations(jsReferenceAsJsAny)?.let { return "Fail: (JsReference<C> as JsAny) $it"}

    val c2 : C = jsReference as Any as C
    if (c !== c2)
        return "Fail: implicit cast of JsReference<C>->Any->C shall result in the original object"

    val c3 : C = jsReference as C
    if (c !== c3)
        return "Fail: implicit restoration of JsReference<C>->C shall result in the original object"

    return "OK"
}