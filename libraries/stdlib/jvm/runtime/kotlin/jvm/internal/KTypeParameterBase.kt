/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.jvm.internal

import java.lang.reflect.GenericDeclaration
import kotlin.LazyThreadSafetyMode.PUBLICATION
import kotlin.reflect.KTypeParameter

/**
 * The common base class for lite (stdlib-only) and full reflection implementation of type parameters.
 *
 * When creating an instance of a subclass, either pass [container] if it's known at creation time, or [computeContainer] if its
 * computation is heavy and should be delayed until needed.
 */
public abstract class KTypeParameterBase(
    container: Container?,
    private val computeContainer: (() -> Container)?, // TODO(review): maybe remove this and always compute container eagerly?
) : KTypeParameter {
    private val _container: Container? = container

    internal val container: Container
        get() = _container ?: computeContainer!!()

    internal val javaContainingDeclaration: GenericDeclaration? by lazy(PUBLICATION) {
        (this.container as? KotlinGenericDeclaration)?.findJvmDeclaration()
    }

    override fun equals(other: Any?): Boolean =
        other is KTypeParameterBase && container == other.container && name == other.name

    override fun hashCode(): Int =
        container.hashCode() * 31 + name.hashCode()

    override fun toString(): String =
        TypeParameterReference.toString(this)
}

// ClassReference | FunctionReference | PropertyReference
private typealias Container = Any
