/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.generators.native.cache.version

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asTypeName
import java.nio.file.Path
import java.util.logging.Logger
import kotlin.io.path.Path

internal object NativeCacheKotlinVersionsGenerator {
    private val logger = Logger.getLogger(NativeCacheKotlinVersionsGenerator::class.java.name)

    fun generate(versions: Set<Triple<Int, Int, Int>>): Pair<Path, String> {
        logger.info("Generating .kt file with ${versions.size} entries")

        val className = "DisableCacheInKotlinVersion"
        val disableCacheInKotlinVersionClass = ClassName(KGP_MPP_PACKAGE, className)
        val comparableType = Comparable::class.asTypeName().parameterizedBy(disableCacheInKotlinVersionClass)
        val mainFileAppendable = createGeneratedFileAppendable()

        val versionObjects = versions.map { version ->
            val propertyName = "${version.first}_${version.second}_${version.third}"
            TypeSpec.objectBuilder(propertyName).apply {
                addModifiers(KModifier.PUBLIC)
                superclass(disableCacheInKotlinVersionClass)
                addSuperclassConstructorParameter("%L, %L, %L", version.first, version.second, version.third)
            }.build()
        }

        val mainFile = FileSpec.builder(KGP_MPP_PACKAGE, className).apply {
            addType(
                TypeSpec.classBuilder(className).apply {
                    addModifiers(KModifier.PUBLIC, KModifier.SEALED)
                    addSuperinterface(comparableType)
                    addKdoc(KDOC_SINCE_2_3_20)
                    addAnnotation(ANNOTATION_NATIVE_CACHE_API)

                    val majorProp = PropertySpec.builder("major", Int::class)
                        .addModifiers(KModifier.PUBLIC)
                        .initializer("major")
                        .build()
                    val minorProp = PropertySpec.builder("minor", Int::class)
                        .addModifiers(KModifier.PUBLIC)
                        .initializer("minor")
                        .build()
                    val patchProp = PropertySpec.builder("patch", Int::class)
                        .addModifiers(KModifier.PUBLIC)
                        .initializer("patch")
                        .build()

                    primaryConstructor(
                        FunSpec.constructorBuilder()
                            .addParameter("major", Int::class)
                            .addParameter("minor", Int::class)
                            .addParameter("patch", Int::class)
                            .addModifiers(KModifier.PRIVATE) // Constructor is private
                            .build()
                    )

                    addProperty(majorProp)
                    addProperty(minorProp)
                    addProperty(patchProp)
                    addTypes(versionObjects)
                    addFunction(
                        FunSpec.builder("toString")
                            .addModifiers(KModifier.OVERRIDE)
                            .returns(String::class)
                            .addStatement("return \"v\${major}_\${minor}_\${patch}\"")
                            .build()
                    )
                    addFunction(
                        FunSpec.builder("compareTo")
                            .addModifiers(KModifier.OVERRIDE)
                            .addParameter("other", disableCacheInKotlinVersionClass)
                            .returns(Int::class)
                            .addStatement("return compareValuesBy(this, other, { it.major }, { it.minor }, { it.patch })")
                            .build()
                    )
                }.build()
            )
        }.build()

        mainFile.writeTo(mainFileAppendable)

        return Path(mainFile.relativePath) to mainFileAppendable.toString()
    }
}