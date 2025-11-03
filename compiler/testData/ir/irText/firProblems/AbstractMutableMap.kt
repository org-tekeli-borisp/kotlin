// WITH_STDLIB
// FULL_JDK
// JVM_TARGET: 1.8
// TARGET_BACKEND: JVM_IR
// SKIP_NEW_KOTLIN_REFLECT_COMPATIBILITY_CHECK

class MyMap<K : Any, V : Any> : AbstractMutableMap<K, V>() {
    override fun put(key: K, value: V): V? = null

    override val entries: MutableSet<MutableMap.MutableEntry<K, V>>
        get() = mutableSetOf()
}
