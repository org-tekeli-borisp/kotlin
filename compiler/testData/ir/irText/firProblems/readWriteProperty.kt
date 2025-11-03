// WITH_STDLIB
// WITH_REFLECT

// SKIP_NEW_KOTLIN_REFLECT_COMPATIBILITY_CHECK
//
// To fix this test we need to migrate callables to kotlinx-metadata.
// The fix patch then should look smth like this (`kotlinFunction?.data` not being available yet):
//
//     diff --git a/core/reflection.jvm/src/kotlin/reflect/jvm/internal/KClassImpl.kt b/core/reflection.jvm/src/kotlin/reflect/jvm/internal/KClassImpl.kt
//     index 9ed68ab32888..d9e6ba610fe6 100644
//     --- a/core/reflection.jvm/src/kotlin/reflect/jvm/internal/KClassImpl.kt
//     +++ b/core/reflection.jvm/src/kotlin/reflect/jvm/internal/KClassImpl.kt
//     @@ -57,6 +57,7 @@ import kotlin.reflect.*
//      import kotlin.reflect.jvm.internal.KClassImpl.MemberBelonginess.DECLARED
//      import kotlin.reflect.jvm.internal.KClassImpl.MemberBelonginess.INHERITED
//      import kotlin.reflect.jvm.internal.types.DescriptorKType
//     +import kotlin.reflect.jvm.kotlinFunction
//      import org.jetbrains.kotlin.descriptors.ClassKind as DescriptorClassKind
//      import org.jetbrains.kotlin.descriptors.Modality as DescriptorModality
//
//     @@ -194,7 +195,9 @@ internal class KClassImpl<T : Any>(
//                  else
//                      TypeParameterTable.create(
//                          kmClass!!.typeParameters,
//     -                    (jClass.enclosingClass?.takeIf { kmClass!!.isInner }?.kotlin as? KClassImpl<*>)?.data?.value?.typeParameterTable,
//     +                    jClass.enclosingMethod?.kotlinFunction?.data?.value?.typeParameterTable
//     +                        ?: (jClass.enclosingClass?.takeIf<Class<*>> { kmClass!!.isInner }?.kotlin as? KClassImpl<*>)
//     +                            ?.data?.value?.typeParameterTable,
//                          this@KClassImpl,
//                          jClass.safeClassLoader,
//                      )

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

class SettingType<out V : Any>(
    val type : KClass<out V>
)

class SettingReference<V : Any, T : SettingType<V>>(
    var t : T,
    var v : V
)

class IdeWizard {
    var projectTemplate by setting(SettingReference(SettingType(42::class), 42))

    private fun <V : Any, T : SettingType<V>> setting(reference: SettingReference<V, T>) =
        object : ReadWriteProperty<Any?, V?> {
            override fun setValue(thisRef: Any?, property: KProperty<*>, value: V?) {
                if (value == null) return
                reference.t = SettingType(value::class) as T
                reference.v = value
            }

            override fun getValue(thisRef: Any?, property: KProperty<*>): V? {
                return reference.v
            }
        }
}
