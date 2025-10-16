// TARGET_BACKEND: JVM
// WITH_REFLECT
// JVM_ABI_K1_K2_DIFF: KT-63984, KT-76258

import kotlin.reflect.*
import kotlin.test.assertEquals
import kotlin.test.assertFalse

var default: Int = 0

var defaultAnnotated: Int = 0
    public set

var custom: Int = 0
    set(myName: Int) {}

fun checkPropertySetterParam(property: KMutableProperty<*>, name: String?) {
    val parameter = property.setter.parameters.single()
    assertEquals(0, parameter.index)
    assertEquals(name, parameter.name)
    assertFalse(parameter.isOptional)
    assertFalse(parameter.isVararg)
}

fun box(): String {
    checkPropertySetterParam(::default, null)
    checkPropertySetterParam(::defaultAnnotated, null)
    checkPropertySetterParam(::custom, "myName")

    return "OK"
}
