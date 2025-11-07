/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal

import java.util.*
import kotlin.metadata.ClassKind
import kotlin.reflect.*
import kotlin.reflect.full.createType
import kotlin.reflect.full.declaredMembers
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.jvm.internal.types.AbstractKType
import kotlin.reflect.jvm.internal.types.KTypeSubstitutor
import kotlin.reflect.jvm.internal.types.MutableCollectionKClass
import kotlin.reflect.jvm.internal.types.ReflectTypeSystemContext
import kotlin.reflect.jvm.javaField
import kotlin.reflect.jvm.javaMethod

/**
 * Unfortunately, getting members is not a transitive operation
 */
internal fun getAllMembers_newKotlinReflectImpl(kClass: KClassImpl<*>): Collection<DescriptorKCallable<*>> {
    val membersReadOnly = kClass.data.value.allMembersPreservingTransitivity
    // Kotlin doesn't have statics (unless it's enum), and it never inherits statics from Java
    val isKotlin = kClass.java.isKotlin
    val doNeedToFilterOutStatics =
        membersReadOnly.containsInheritedStatics && kClass.classKind != ClassKind.ENUM_CLASS && isKotlin
    val doNeedToShrinkMembers = membersReadOnly.containsPackagePrivate || doNeedToFilterOutStatics
    val membersMutable = when (doNeedToShrinkMembers) {
        true -> lazyOf(
            membersReadOnly.allMembers.filterNotTo(HashMap(membersReadOnly.allMembers.size)) { (_, member) ->
                doNeedToFilterOutStatics && member.isStatic ||
                    member.isPackagePrivate && member.container.jClass.`package` != kClass.java.`package`
            }
        )
        false -> lazy(LazyThreadSafetyMode.NONE) { HashMap(membersReadOnly.allMembers) }
    }
    // Populating the 'members' list with things which are not inherited but appear in the 'members' list
    for (declaredMember in kClass.declaredDescriptorKCallableMembers) {
        val signature = declaredMember.toEquatableCallableSignature(isKotlin)
        if (// static members are not inherited, but the immediate statics in interfaces must appear in the 'members' list
            declaredMember.isStatic && kClass.classKind == ClassKind.INTERFACE ||

            // private members are not inherited, but immediate private members must appear in the 'members' list
            declaredMember.visibility == KVisibility.PRIVATE
        ) {
            membersMutable.value[signature] = declaredMember
        }
    }
    return (if (membersMutable.isInitialized()) membersMutable.value else membersReadOnly.allMembers)
        .map { it.value }
}

private fun nonDenotableSupertypesAreNotPossible(): Nothing = error("Non-denotable supertypes are not possible")
private fun starProjectionSupertypesAreNotPossible(): Nothing = error("Star projection supertypes are not possible")

// The Comparator assumes similar signature KCallables
private object CovariantOverrideComparator : Comparator<DescriptorKCallable<*>> {
    override fun compare(a: DescriptorKCallable<*>, b: DescriptorKCallable<*>): Int {
        val typeParametersEliminator = b.typeParameters.substituteTypeParametersInto(a.typeParameters)
        val aReturnType =
            typeParametersEliminator.substitute(a.returnType).type ?: starProjectionSupertypesAreNotPossible()
        val bReturnType = b.returnType
        val aIsSubtypeOfB = aReturnType.isSubtypeOf(bReturnType)
        val bIsSubtypeOfA = bReturnType.isSubtypeOf(aReturnType)

        return when {
            aIsSubtypeOfB && bIsSubtypeOfA -> {
                val isAFlexible = with(ReflectTypeSystemContext) { (aReturnType as? AbstractKType)?.isFlexible() == true }
                val isBFlexible = with(ReflectTypeSystemContext) { (bReturnType as? AbstractKType)?.isFlexible() == true }
                when {
                    isAFlexible && isBFlexible -> 0
                    isBFlexible -> -1
                    isAFlexible -> 1
                    else -> 0
                }
            }
            aIsSubtypeOfB -> -1
            bIsSubtypeOfA -> 1
            else -> 0
        }
    }
}

internal data class AllMembersPreservingTransitivity(
    val allMembers: Map<EquatableCallableSignature, DescriptorKCallable<*>>,
    val containsInheritedStatics: Boolean,
    val containsPackagePrivate: Boolean,
)

internal val mutableCollectionKType = typeOf<MutableCollection<*>>()
private fun KType.coerceMutableCollectionTypeToImmutableCollectionType(): KType = when {
    isSubtypeOf(mutableCollectionKType) -> {
        // Take the upper bound to coerce to immutable types.
        // The returned classifier for mutable types is immutable,
        // it's a known problem (KT-11754) that we rely on here
        val immutableCollectionClassifier = ((this as AbstractKType).upperBoundIfFlexible() ?: this).classifier
        // Substitute type args from lower bound to avoid star projections
        val args = (lowerBoundIfFlexible() ?: this).arguments
        (immutableCollectionClassifier ?: nonDenotableSupertypesAreNotPossible()).createType(args)
    }
    else -> this
}

private fun collectDeclaredMembersToMap(map: HashMap<EquatableCallableSignature, DescriptorKCallable<*>>) {

}

/**
 * todo KDoc
 */
internal fun getAllMembersPreservingTransitivity(kClass: KClassImpl<*>): AllMembersPreservingTransitivity {
    val jvmSignaturesMap = HashMap<EquatableCallableSignature, DescriptorKCallable<*>>()
    val thisReceiver = kClass.descriptor.thisAsReceiverParameter
    var containsInheritedStatics = false
    var containsPackagePrivate = false
    val isKotlin = kClass.java.isKotlin
    val declaredMembersSignature = if (isKotlin) EqualityMode.KotlinSignatures else EqualityMode.JvmSignatures
    val declaredMembersSignaturesMap = HashMap<EquatableCallableSignature, DescriptorKCallable<*>>()
    for (member in kClass.declaredDescriptorKCallableMembers) {
        if (member.visibility == KVisibility.PRIVATE) continue
        val isStaticMember = member.isStatic
        // static methods (but not fields) in interfaces are never inherited (not in Java, not in Kotlin enums)
        if (isStaticMember && kClass.classKind == ClassKind.INTERFACE && !member.isJavaField) continue
        containsInheritedStatics = containsInheritedStatics || isStaticMember
        containsPackagePrivate = containsPackagePrivate || member.isPackagePrivate
        declaredMembersSignaturesMap[member.toEquatableCallableSignature(declaredMembersSignature)] = member
    }
    for (rawSupertype in kClass.supertypes) {
        // Because mutable collections are severely broken in reflect. Related: KT-11754
        val supertype = rawSupertype.coerceMutableCollectionTypeToImmutableCollectionType()
        val supertypeKClass = supertype.classifier as? KClass<*> ?: nonDenotableSupertypesAreNotPossible()
        val substitutor = KTypeSubstitutor.create(supertype)
        val supertypeMembers = supertypeKClass.allMembersPreservingTransitivity
        containsInheritedStatics = containsInheritedStatics || supertypeMembers.containsInheritedStatics
        containsPackagePrivate = containsPackagePrivate || supertypeMembers.containsPackagePrivate
        for ((_, notSubstitutedMember) in supertypeMembers.allMembers) {
            val member = notSubstitutedMember.shallowCopy().apply {
                forceInstanceReceiverParameter = if (notSubstitutedMember.isStatic) null else thisReceiver
                kTypeSubstitutor = notSubstitutedMember.kTypeSubstitutor.combinedWith(substitutor)
            }
            val kotlinSignature = member.toEquatableCallableSignature(declaredMembersSignature)
            if (declaredMembersSignaturesMap.contains(kotlinSignature)) continue
            // Inherited signatures are always compared by JvmSignatures
            jvmSignaturesMap.merge(kotlinSignature.copy(equalityMode = EqualityMode.JvmSignatures), member) { a, b ->
                minOf(a, b, CovariantOverrideComparator)
            }
        }
    }
    for ((kotlinSignature, descriptor) in declaredMembersSignaturesMap) {
        jvmSignaturesMap[kotlinSignature.copy(equalityMode = EqualityMode.JvmSignatures)] = descriptor
    }
    return AllMembersPreservingTransitivity(jvmSignaturesMap, containsInheritedStatics, containsPackagePrivate)
}

internal val DescriptorKCallable<*>.isStatic: Boolean
    get() = instanceReceiverParameter == null

private val DescriptorKCallable<*>.isJavaField: Boolean
    get() = this is KProperty<*> && this.javaField?.declaringClass?.isKotlin == false

private val KClass<*>.allMembersPreservingTransitivity: AllMembersPreservingTransitivity
    get() = when (this) {
        is KClassImpl<*> -> data.value.allMembersPreservingTransitivity
        is MutableCollectionKClass<*> -> klass.allMembersPreservingTransitivity
        else -> error("Unknown type ${this::class}")
    }

private fun DescriptorKCallable<*>.toEquatableCallableSignature(equalityMode: EqualityMode): EquatableCallableSignature {
    val parameterTypes = parameters.filter { it.kind != KParameter.Kind.INSTANCE }.map { it.type }
    val kind = when (this) {
        is KProperty<*> if javaField?.declaringClass?.isKotlin == false -> SignatureKind.FIELD_IN_JAVA_CLASS
        is KProperty<*> -> SignatureKind.PROPERTY
        is KFunction<*> -> SignatureKind.FUNCTION
        else -> error("Unknown kind for ${this::class}")
    }
    val javaParameterTypes = (this as? KFunction<*>)?.javaMethod?.parameterTypes.orEmpty().toList()
    return EquatableCallableSignature(
        kind,
        name,
        this.typeParameters,
        parameterTypes,
        javaParameterTypes,
        isStatic,
        equalityMode,
    )
}

internal enum class SignatureKind {
    FUNCTION, PROPERTY, FIELD_IN_JAVA_CLASS
}

internal val Class<*>.isKotlin: Boolean get() = getAnnotation(Metadata::class.java) != null

@Suppress("UNCHECKED_CAST")
private val KClass<*>.declaredDescriptorKCallableMembers: Collection<DescriptorKCallable<*>>
    get() = declaredMembers as Collection<DescriptorKCallable<*>>

private fun List<KTypeParameter>.substituteTypeParametersInto(typeParameters: List<KTypeParameter>): KTypeSubstitutor {
    if (isEmpty() || typeParameters.isEmpty()) return KTypeSubstitutor.EMPTY
    val arguments = this
    val substitutionMap = typeParameters.zip(arguments)
        .associate { (x, y) -> Pair(x, KTypeProjection.invariant(y.createType())) }
    return KTypeSubstitutor(substitutionMap)
}

enum class EqualityMode {
    KotlinSignatures, JvmSignatures
}

// Signatures that you can test for equality
internal data class EquatableCallableSignature(
    val kind: SignatureKind,
    val name: String,
    val typeParameters: List<KTypeParameter>,
    val parameterTypes: List<KType>,
    val javaParameterTypesIfFunction: List<Class<*>>,
    val isStatic: Boolean,
    val equalityMode: EqualityMode,
) {
    init {
        check(kind != SignatureKind.FIELD_IN_JAVA_CLASS || parameterTypes.isEmpty() && typeParameters.isEmpty())
    }

    override fun hashCode(): Int = Objects.hash(kind, name, parameterTypes.size, isStatic)

    override fun equals(other: Any?): Boolean {
        if (other !is EquatableCallableSignature) return false
        check(equalityMode == other.equalityMode) {
            "Equality modes must be the same. Please recreate signatures on inheritance"
        }
        if (kind != other.kind) return false
        if (name != other.name) return false
        if (isStatic != other.isStatic) return false
        if (parameterTypes.size != other.parameterTypes.size) return false
        if (equalityMode == EqualityMode.JvmSignatures && kind == SignatureKind.FUNCTION) {
            if (javaParameterTypesIfFunction.size != other.javaParameterTypesIfFunction.size) return false
            for (i in javaParameterTypesIfFunction.indices) {
                if (javaParameterTypesIfFunction[i] != other.javaParameterTypesIfFunction[i]) return false
            }
        } else {
            if (typeParameters.size != other.typeParameters.size) return false
            val functionTypeParametersEliminator = other.typeParameters.substituteTypeParametersInto(typeParameters)
            for (i in parameterTypes.indices) {
                val x = functionTypeParametersEliminator.substitute(parameterTypes[i]).type
                    ?: starProjectionSupertypesAreNotPossible()
                val y = other.parameterTypes[i]
                if (!x.isSubtypeOf(y) || !y.isSubtypeOf(x)) return false
            }
        }
        return true
    }
}
