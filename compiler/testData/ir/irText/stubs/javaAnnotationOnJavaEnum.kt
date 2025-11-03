// TARGET_BACKEND: JVM
// DUMP_EXTERNAL_CLASS: JavaEnum
// SKIP_NEW_KOTLIN_REFLECT_COMPATIBILITY_CHECK

// FILE: test.kt
fun foo() {
    val x = JavaEnum.FOO
}

// FILE: JavaPropertyAnnotation.java
public @interface JavaPropertyAnnotation {}

// FILE: JavaEnum.java
public enum JavaEnum {
    @JavaPropertyAnnotation FOO;
}