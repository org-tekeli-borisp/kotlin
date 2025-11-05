// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +MultiDollarInterpolation
// WITH_EXPERIMENTAL_CHECKERS
// DIAGNOSTICS: -warnings +REDUNDANT_INTERPOLATION_PREFIX

fun test() {
    "foo\\\$bar"
    $$"foo\\$bar"
    $$"foo\\\\$bar"
    "foo\\$<!UNRESOLVED_REFERENCE!>bar<!>"
    "\$%"
    <!REDUNDANT_INTERPOLATION_PREFIX!>$$"$%"<!>
    "$%"
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"foo"<!>
    <!REDUNDANT_INTERPOLATION_PREFIX!>$"foo$<!UNRESOLVED_REFERENCE!>bar<!>"<!>
}

/* GENERATED_FIR_TAGS: functionDeclaration, stringLiteral */
