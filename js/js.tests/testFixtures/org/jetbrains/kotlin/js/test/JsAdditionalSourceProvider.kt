/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test

import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.js.JavaScript
import org.jetbrains.kotlin.test.directives.JsEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives
import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.AdditionalSourceProvider
import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.configuration.JsEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.getFixture
import java.io.File
import java.io.FileFilter

class JsAdditionalSourceProvider(testServices: TestServices) : AdditionalSourceProvider(testServices) {
    override fun produceAdditionalFiles(
        globalDirectives: RegisteredDirectives,
        module: TestModule,
        testModuleStructure: TestModuleStructure,
    ): List<TestFile> {
        if (JsEnvironmentConfigurationDirectives.NO_COMMON_FILES in module.directives) return emptyList()
        // Add the files only to common modules with no dependencies, otherwise they'll produce "IrSymbol is already bound"
        if (module.allDependencies.isNotEmpty()) {
            return emptyList()
        }

        val testDirectory = module.files.first().originalFile.parent

        return buildList {
            add(testServices.getFixture(ASSERTION_HELPERS).toTestFile())
            getLocalCommonFile(testDirectory, KotlinFileType.EXTENSION)?.toTestFile()?.let(::add)
        }
    }

    companion object {
        private const val COMMON_FILES_NAME = "_common"
        private const val ASSERTION_HELPERS = "assertionHelpers.kt"

        private fun getLocalCommonFile(directory: String, extension: String): File? {
            val localCommonFilePath = "$directory/$COMMON_FILES_NAME.$extension"
            val localCommonFile = File(localCommonFilePath).takeIf { it.exists() } ?: return null
            return localCommonFile
        }

        fun getAdditionalJsFiles(directory: String): List<File> {
            return listOfNotNull(getLocalCommonFile(directory, JavaScript.EXTENSION))
        }
    }
}
