// RUN_PIPELINE_TILL: FRONTEND
// RENDER_DIAGNOSTICS_FULL_TEXT
package foo

class C<T> {
    inner class D<R>
    inner class D2<R, S> {
        inner class E<V, W, Y>
    }

    class NonInner<R>
}

class D

fun test() {
    val d: D <!INITIALIZER_TYPE_MISMATCH!>=<!> C<String>().D<Int>()
    val d2: D <!INITIALIZER_TYPE_MISMATCH!>=<!> C<String>().D2<Int, Boolean>().E<Char, Long, Short>()
    val d3: D <!INITIALIZER_TYPE_MISMATCH!>=<!> C.NonInner<String>()
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, localProperty, nestedClass, propertyDeclaration */
