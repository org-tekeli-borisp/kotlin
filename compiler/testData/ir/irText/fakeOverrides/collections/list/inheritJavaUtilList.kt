// TARGET_BACKEND: JVM
// SKIP_KT_DUMP
// FULL_JDK
// SKIP_NEW_KOTLIN_REFLECT_COMPATIBILITY_CHECK

interface KotlinList<T> : java.util.List<T>

interface SpecificList : KotlinList<String>
