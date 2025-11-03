// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +ImprovedResolutionInSecondaryConstructors
// FIR_IDENTICAL
// ISSUE: KT-77275
abstract class Super(x: String)

val y: String = ""

class A(x: String = y) : Super(y) {
    constructor(
        w: Int,
        z: Int,
        x: String = y,
    ) : this(y)

    val y: Int = 1
}

/* GENERATED_FIR_TAGS: classDeclaration, integerLiteral, primaryConstructor, propertyDeclaration, secondaryConstructor,
stringLiteral */
