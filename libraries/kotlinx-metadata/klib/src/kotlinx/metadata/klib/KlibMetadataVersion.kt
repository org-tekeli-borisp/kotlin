/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlinx.metadata.klib

import org.jetbrains.kotlin.metadata.deserialization.MetadataVersion

class KlibMetadataVersion(val major: Int, val minor: Int, val patch: Int) : Comparable<KlibMetadataVersion> {
    constructor(metadataVersion: MetadataVersion) : this(
        major = metadataVersion.major,
        minor = metadataVersion.minor,
        patch = metadataVersion.patch
    )

    constructor(intArray: IntArray) : this(intArray[0], intArray[1], intArray[2])

    override fun compareTo(other: KlibMetadataVersion): Int {
        val majors = major.compareTo(other.major)
        if (majors != 0) return majors
        val minors = minor.compareTo(other.minor)
        return if (minors != 0) minors else patch.compareTo(other.patch)
    }
}