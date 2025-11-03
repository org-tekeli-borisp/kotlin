// SKIP_KT_DUMP
// ISSUE: KT-65448
// TARGET_BACKEND: JVM
// SKIP_NEW_KOTLIN_REFLECT_COMPATIBILITY_CHECK

// FILE: Java1.java
import java.util.ArrayList;

public abstract class Java1 extends ArrayList { }

// FILE: 1.kt
abstract class E : Java1(){
}