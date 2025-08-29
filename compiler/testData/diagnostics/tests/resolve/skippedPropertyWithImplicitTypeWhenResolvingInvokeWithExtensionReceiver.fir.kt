// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-76240, KTLC-367
// RENDER_DIAGNOSTICS_FULL_TEXT
// DIAGNOSTICS: -DEBUG_INFO_LEAKING_THIS

// FILE: ResolveWithDeprecationWarning.kt

fun Int.f(): String = "ext func"
val Int.p: String
    get() = "ext prop"

class Foo {
    private val f = f()
    // New warning should be reported
    private fun f() = 42.f()

    private val p = p()
    private fun p() = 42.p // Property call is resolved correctly because there is no attempt to resolve invoke call
}

fun String.g(): Boolean = false

class Bar {
    // New warning should be reported
    private fun g() = "s2".g()
    private val g = <!TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM!>g()<!>
}

// FILE: GreenCodeBecomesRed.kt

// Previously: resolve to invoke and green code.
// After: `FUNCTION_EXPECTED` error + new warning.
fun f2() = O1.x1()

object O1 {
    val x1 = O1
    operator fun invoke(): String = "s1"
}

// FILE: ChangedBehaviorOnGreenCode.kt

fun O2.<!EXTENSION_FUNCTION_SHADOWED_BY_MEMBER_PROPERTY_WITH_INVOKE!>x2<!>() = "s2"

// Previously: resolving to the `invoke` below.
// After: resolving to the extension function above (despite the fact it has `EXTENSION_FUNCTION_SHADOWED_BY_MEMBER_PROPERTY_WITH_INVOKE` warning) + new warning.
fun f3() = O2.x2()

object O2 {
    val x2 = O2
    operator fun invoke(): String = "s1"
}

// FILE: ChangedDiagnosticsOnRedCode.kt

// Previously: UNRESOLVED_REFERENCE.
// After: UNRESOLVED_REFERENCE + new warning.
val p4 = 1.<!UNRESOLVED_REFERENCE!>p4<!>

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration, getter, integerLiteral,
objectDeclaration, operator, propertyDeclaration, propertyWithExtensionReceiver, stringLiteral */
