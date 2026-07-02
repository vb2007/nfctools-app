package hu.vb2007.nfctool.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute : NavKey

@Serializable
data object SettingsRoute : NavKey

@Serializable
data object ReadRoute : NavKey

@Serializable
data object WriteRoute : NavKey
