package com.guotu.app.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.guotu.app.ui.categories.CategoriesScreen
import com.guotu.app.ui.library.LibraryScreen
import com.guotu.app.ui.navigation.TopLevelDestination
import com.guotu.app.ui.settings.SettingsScreen
import com.guotu.app.ui.swipe.SwipeBrowseScreen
import com.guotu.app.ui.tags.TagsScreen
import com.guotu.app.ui.tasks.TasksScreen
import com.guotu.app.ui.theme.GuotuColors
import com.guotu.app.ui.theme.GuotuDimens
import com.guotu.app.ui.theme.GuotuTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GuotuTheme {
                GuotuApp()
            }
        }
    }
}

private const val ROUTE_MAIN = "main"
private const val ROUTE_SWIPE = "swipe/{startIndex}"

@Composable
private fun GuotuApp(
    viewModel: MainViewModel = hiltViewModel(),
) {
    val rootNav = rememberNavController()
    // 浏览页独立于带底栏的 main，右滑返回时整页滑动，不再和底栏叠层
    NavHost(
        navController = rootNav,
        startDestination = ROUTE_MAIN,
        modifier = Modifier.fillMaxSize(),
    ) {
        composable(ROUTE_MAIN) {
            MainShell(
                showTagsTabFlow = viewModel,
                onOpenSwipe = { startIndex ->
                    rootNav.navigate("swipe/$startIndex")
                },
            )
        }
        composable(
            route = ROUTE_SWIPE,
            arguments = listOf(navArgument("startIndex") { type = NavType.IntType }),
            enterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(320),
                ) + fadeIn(tween(220))
            },
            exitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(280),
                    targetOffset = { it / 5 },
                ) + fadeOut(tween(200))
            },
            popEnterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(320),
                    initialOffset = { it / 5 },
                ) + fadeIn(tween(220))
            },
            popExitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(300),
                ) + fadeOut(tween(180))
            },
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
            ) {
                SwipeBrowseScreen(onBack = { rootNav.popBackStack() })
            }
        }
    }
}

@Composable
private fun MainShell(
    showTagsTabFlow: MainViewModel,
    onOpenSwipe: (Int) -> Unit,
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showTagsTab by showTagsTabFlow.showTagsTab.collectAsStateWithLifecycle()
    val bottomDestinations = remember(showTagsTab) {
        TopLevelDestination.visibleItems(showTagsTab)
    }

    LaunchedEffect(showTagsTab, currentRoute) {
        if (!showTagsTab && currentRoute == TopLevelDestination.Tags.route) {
            navController.navigate(TopLevelDestination.Library.route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = GuotuColors.Canvas,
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            GuotuBottomBar(
                destinations = bottomDestinations,
                currentRoute = currentRoute,
                onNavigate = { route ->
                    navController.navigate(route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
            )
        },
    ) { innerPadding ->
        MainTabNavHost(
            navController = navController,
            onOpenSwipe = onOpenSwipe,
            showTagsTab = showTagsTab,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        )
    }
}

@Composable
private fun MainTabNavHost(
    navController: NavHostController,
    onOpenSwipe: (Int) -> Unit,
    showTagsTab: Boolean,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = TopLevelDestination.Library.route,
        modifier = modifier,
    ) {
        composable(TopLevelDestination.Library.route) {
            LibraryScreen(
                onOpenSwipe = onOpenSwipe,
                showTagsFeatures = showTagsTab,
            )
        }
        composable(TopLevelDestination.Categories.route) {
            CategoriesScreen(
                onOpenLibrary = {
                    navController.navigate(TopLevelDestination.Library.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
            )
        }
        composable(TopLevelDestination.Tags.route) {
            TagsScreen(
                onOpenLibrary = {
                    navController.navigate(TopLevelDestination.Library.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
            )
        }
        composable(TopLevelDestination.Tasks.route) { TasksScreen() }
        composable(TopLevelDestination.Settings.route) { SettingsScreen() }
    }
}

@Composable
private fun GuotuBottomBar(
    destinations: List<TopLevelDestination>,
    currentRoute: String?,
    onNavigate: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(GuotuColors.Panel)
            .navigationBarsPadding(),
    ) {
        HorizontalDivider(thickness = GuotuDimens.Hairline, color = GuotuColors.Rule)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = GuotuDimens.Grid, vertical = GuotuDimens.Grid),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            destinations.forEach { dest ->
                val selected = currentRoute == dest.route
                FlatNavItem(
                    selected = selected,
                    label = stringResource(dest.labelRes),
                    outlined = dest.outlinedIcon(),
                    filled = dest.filledIcon(),
                    onClick = { onNavigate(dest.route) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun FlatNavItem(
    selected: Boolean,
    label: String,
    outlined: ImageVector,
    filled: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val color = if (selected) GuotuColors.Signal else GuotuColors.InkMute
    Column(
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = if (selected) filled else outlined,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(22.dp),
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = color,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(3.dp))
        Box(
            modifier = Modifier
                .width(18.dp)
                .height(2.dp)
                .background(if (selected) GuotuColors.Signal else Color.Transparent),
        )
    }
}

private fun TopLevelDestination.outlinedIcon(): ImageVector = when (this) {
    TopLevelDestination.Library -> Icons.Outlined.PhotoLibrary
    TopLevelDestination.Categories -> Icons.Outlined.Category
    TopLevelDestination.Tags -> Icons.AutoMirrored.Outlined.Label
    TopLevelDestination.Tasks -> Icons.Outlined.TaskAlt
    TopLevelDestination.Settings -> Icons.Outlined.Settings
}

private fun TopLevelDestination.filledIcon(): ImageVector = when (this) {
    TopLevelDestination.Library -> Icons.Filled.PhotoLibrary
    TopLevelDestination.Categories -> Icons.Filled.Category
    TopLevelDestination.Tags -> Icons.AutoMirrored.Filled.Label
    TopLevelDestination.Tasks -> Icons.Filled.TaskAlt
    TopLevelDestination.Settings -> Icons.Filled.Settings
}
