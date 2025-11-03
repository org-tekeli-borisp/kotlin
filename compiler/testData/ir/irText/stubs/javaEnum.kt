// TARGET_BACKEND: JVM
// DUMP_EXTERNAL_CLASS: JEnum
// SKIP_NEW_KOTLIN_REFLECT_COMPATIBILITY_CHECK

// FILE: JEnum.java

public enum JEnum {
    ONE, TWO, THREE;
}

// FILE: javaEnum.kt

val test = JEnum.ONE
