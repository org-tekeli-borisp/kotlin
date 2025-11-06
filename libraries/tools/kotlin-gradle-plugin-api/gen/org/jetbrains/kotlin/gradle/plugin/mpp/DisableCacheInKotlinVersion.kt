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
  override fun toString(): String = "v${major}_${minor}_${patch}"

  override fun compareTo(other: DisableCacheInKotlinVersion): Int = compareValuesBy(this, other, { it.major }, { it.minor }, { it.patch })

  public object `2_3_20` : DisableCacheInKotlinVersion(2, 3, 20)

  public object `2_3_255` : DisableCacheInKotlinVersion(2, 3, 255)
}
