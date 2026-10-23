package com.guotu.app.ui.navigation

sealed class TopLevelDestination(
    val route: String,
    val labelRes: Int,
) {
    data object Library : TopLevelDestination("library", com.guotu.app.R.string.nav_library)
    data object Categories : TopLevelDestination("categories", com.guotu.app.R.string.nav_categories)
    data object Tags : TopLevelDestination("tags", com.guotu.app.R.string.nav_tags)
    data object Tasks : TopLevelDestination("tasks", com.guotu.app.R.string.nav_tasks)
    data object Settings : TopLevelDestination("settings", com.guotu.app.R.string.nav_settings)

    companion object {
        val items = listOf(Library, Categories, Tags, Tasks, Settings)

        fun visibleItems(showTagsTab: Boolean): List<TopLevelDestination> =
            if (showTagsTab) items else items.filter { it != Tags }
    }
}
