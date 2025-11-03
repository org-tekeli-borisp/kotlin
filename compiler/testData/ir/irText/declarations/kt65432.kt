// WITH_REFLECT
// TARGET_BACKEND: JVM_IR
// SKIP_NEW_KOTLIN_REFLECT_COMPATIBILITY_CHECK

// FILE: Java1.java
public class Java1 extends KotlinClass { }

// FILE: 1.kt
import java.util.ArrayList

class A : Java1()

open class KotlinClass : ArrayList<Int>()
