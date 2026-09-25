package com.aistra.hail.utils

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import coil.request.ImageRequest

@Immutable
data class AppIconRequest(
    val packageName: String,
    val userId: Int = 0,
    val size: Int = (48.dp).roundToPx(),
    val grayscale: Boolean = false,
    val synthesizeAdaptive: Boolean = HailData.synthesizeAdaptiveIcons,
    val iconPack: String = HailData.iconPack
) : ImageRequest.Data {

    override fun toString(): String {
        return "appicon://$packageName|$userId|$size|$grayscale|$synthesizeAdaptive|$iconPack"
    }

    override fun hashCode(): Int {
        var result = packageName.hashCode()
        result = 31 * result + userId
        result = 31 * result + size
        result = 31 * result + grayscale.hashCode()
        result = 31 * result + synthesizeAdaptive.hashCode()
        result = 31 * result + iconPack.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AppIconRequest) return false
        if (packageName != other.packageName) return false
        if (userId != other.userId) return false
        if (size != other.size) return false
        if (grayscale != other.grayscale) return false
        if (synthesizeAdaptive != other.synthesizeAdaptive) return false
        if (iconPack != other.iconPack) return false
        return true
    }
}