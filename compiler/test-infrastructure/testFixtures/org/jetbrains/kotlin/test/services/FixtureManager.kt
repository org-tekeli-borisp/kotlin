/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.services

import java.io.File
import java.net.URL

class FixtureManager(protected val testServices: TestServices) : TestService {
    private val _allFixtures = mutableMapOf<String, File>()

    private val URL.resolvedFiled: File
        get() {
            val name = file.substringAfterLast("/")
            val dir = testServices.temporaryDirectoryManager.getOrCreateTempDirectory("filesFromResources")
            return dir.resolve(name)
        }

    fun readResourceFileByUrl(url: URL): Pair<File, String> {
        val originalContent = url.readText()
        return url.resolvedFiled.also {
            it.writeText(originalContent)
        } to originalContent
    }

    fun getFixtureWithPath(relativePath: String): File = _allFixtures.getOrPut(relativePath) {
        (this::class.java.classLoader.getResource(relativePath) ?: error("File $relativePath doesn't exist in resources"))
            .let { readResourceFileByUrl(it).first }
    }
}

val TestServices.fixtureManager: FixtureManager by TestServices.testServiceAccessor()

fun TestServices.getFixture(name: String): File {
    return fixtureManager.getFixtureWithPath(name)
}
