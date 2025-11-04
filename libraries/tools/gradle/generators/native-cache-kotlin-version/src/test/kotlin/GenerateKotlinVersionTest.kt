/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.jetbrains.kotlin.gradle.generators.native.cache.version.NativeCacheKotlinVersionsFile
import org.jetbrains.kotlin.gradle.generators.native.cache.version.NativeCacheKotlinVersionsGenerator
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.assertEquals

class GenerateKotlinVersionTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    @DisplayName("updateAndGetAll should add a new version to an existing file")
    fun `test NativeCacheKotlinVersionsFile - adds new version`() {
        val versionsFile = tempDir.resolve("native-cache-kotlin-versions.txt")
        versionsFile.writeText("v2_0_0\n")

        val versions = NativeCacheKotlinVersionsFile.updateAndGetAll(
            versionsFile,
            Triple(2, 1, 0),
        )

        assertEquals(
            setOf(Triple(2, 0, 0), Triple(2, 1, 0)),
            versions
        )
        assertEquals("v2_0_0\nv2_1_0", versionsFile.readText().trim())
    }

    @Test
    @DisplayName("updateAndGetAll should not rewrite file if version already exists")
    fun `test NativeCacheKotlinVersionsFile - does not add existing version`() {
        val versionsFile = tempDir.resolve("supported-kotlin-versions.txt")
        versionsFile.writeText("v2_0_0\nv2_1_0")
        val originalContent = versionsFile.readText()
        val lastModified = versionsFile.toFile().lastModified()

        val versions = NativeCacheKotlinVersionsFile.updateAndGetAll(
            versionsFile,
            Triple(2, 1, 0)
        )

        assertEquals(
            setOf(Triple(2, 0, 0), Triple(2, 1, 0)),
            versions
        )
        // File should not have been modified
        assertEquals(originalContent, versionsFile.readText())
        assertEquals(lastModified, versionsFile.toFile().lastModified())
    }

    @Test
    @DisplayName("updateAndGetAll should create a new file if one does not exist")
    fun `test NativeCacheKotlinVersionsFile - creates new file`() {
        val versionsFile = tempDir.resolve("supported-kotlin-versions.txt")

        val versions = NativeCacheKotlinVersionsFile.updateAndGetAll(
            versionsFile,
            Triple(2, 2, 0)
        )

        assertEquals(setOf(Triple(2, 2, 0)), versions)
        assertEquals("v2_2_0", versionsFile.readText().trim())
    }

    @Test
    @DisplayName("NativeCacheKotlinVersionsGenerator should generate the sealed class correctly")
    fun `test NativeCacheKotlinVersionsGenerator - generates correct code`() {
        val versions = setOf(
            Triple(2, 0, 0),
            Triple(2, 1, 0)
        )
        val (_, actualContent) = NativeCacheKotlinVersionsGenerator.generate(versions)

        // Use a multiline string to assert the exact file content is generated correctly.
        val expectedContent = """
        // This file was generated automatically. See the README.md file
        // DO NOT MODIFY IT MANUALLY.
        
        package org.jetbrains.kotlin.gradle.plugin.mpp
        
        import kotlin.Comparable
        import kotlin.Int
        import kotlin.String
        
        /**
         * @since 2.3.20
         */
        @KotlinNativeCacheApi
        public sealed class DisableCacheInKotlinVersion private constructor(
          public val major: Int,
          public val minor: Int,
          public val patch: Int,
        ) : Comparable<DisableCacheInKotlinVersion> {
          override fun toString(): String = "v${'$'}{major}_${'$'}{minor}_${'$'}{patch}"
        
          override fun compareTo(other: DisableCacheInKotlinVersion): Int = compareValuesBy(this, other, { it.major }, { it.minor }, { it.patch })
        
          public object `2_0_0` : DisableCacheInKotlinVersion(2, 0, 0)
        
          public object `2_1_0` : DisableCacheInKotlinVersion(2, 1, 0)
        }
        
        """.trimIndent()

        // We must normalize line endings to '\n' (which KotlinPoet uses) to ensure
        // the test passes on Windows (which might use '\r\n').
        assertEquals(
            expectedContent.replace("\r\n", "\n"),
            actualContent.replace("\r\n", "\n")
        )
    }
}