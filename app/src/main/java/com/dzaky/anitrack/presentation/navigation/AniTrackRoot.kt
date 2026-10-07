package com.dzaky.anitrack.presentation.navigation

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.core.net.toUri
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.dzaky.anitrack.R
import com.dzaky.anitrack.presentation.detail.AnimeDetailScreen
import com.dzaky.anitrack.presentation.detail.AnimeDetailViewModel
import com.dzaky.anitrack.presentation.home.HomeScreen
import com.dzaky.anitrack.presentation.home.HomeViewModel
import com.dzaky.anitrack.presentation.library.FavoritesScreen
import com.dzaky.anitrack.presentation.library.LibraryViewModel
import com.dzaky.anitrack.presentation.library.WatchlistScreen
import com.dzaky.anitrack.presentation.search.SearchScreen
import com.dzaky.anitrack.presentation.search.SearchViewModel
import com.dzaky.anitrack.presentation.settings.SettingsScreen
import com.dzaky.anitrack.presentation.settings.SettingsViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

private enum class MainDestination(val route: String, @StringRes val label: Int, @DrawableRes val icon: Int) {
    Home("home", R.string.home, R.drawable.ic_home),
    Search("search", R.string.search, R.drawable.ic_search),
    Watchlist("watchlist", R.string.watchlist, R.drawable.ic_watchlist),
    Favorites("favorites", R.string.favorites, R.drawable.ic_heart_outline),
    Settings("settings", R.string.settings, R.drawable.ic_settings),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AniTrackRoot(settingsViewModel: SettingsViewModel) {
    val navigation = rememberNavController()
    val currentEntry by navigation.currentBackStackEntryAsState()
    val route = currentEntry?.destination?.route ?: MainDestination.Home.route
    val destination = MainDestination.entries.firstOrNull { it.route == route }
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val openAnime: (Int) -> Unit = { id -> navigation.navigate("anime/$id") { launchSingleTop = true } }
    val goHome: () -> Unit = { navigation.navigate(MainDestination.Home.route) {
        popUpTo(navigation.graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    } }
    MessagesEffect(settingsViewModel.events, snackbar)
    Scaffold(
        topBar = { TopAppBar(
            title = { Text(if (route == "home") "AniTrack" else destination?.let { stringResource(it.label) } ?: "Anime details", style = MaterialTheme.typography.titleLarge) },
            navigationIcon = {
                if (destination == null) IconButton(onClick = { navigation.navigateUp() }) {
                    Icon(painterResource(R.drawable.ic_back), stringResource(R.string.back), Modifier.size(24.dp))
                }
            },
        ) },
        bottomBar = {
            if (destination != null) NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
                MainDestination.entries.forEach { target ->
                    NavigationBarItem(selected = target == destination, onClick = {
                        navigation.navigate(target.route) {
                            popUpTo(navigation.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }, icon = { Icon(painterResource(target.icon), null, Modifier.size(24.dp)) },
                        label = { Text(stringResource(target.label), maxLines = 1, style = MaterialTheme.typography.labelSmall) })
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        NavHost(navigation, startDestination = "home", modifier = Modifier.padding(padding),
            enterTransition = { fadeIn(tween(180)) }, exitTransition = { fadeOut(tween(120)) }) {
            composable("home") {
                val viewModel: HomeViewModel = hiltViewModel()
                HomeScreen(viewModel, openAnime)
            }
            composable("search") {
                val viewModel: SearchViewModel = hiltViewModel()
                MessagesEffect(viewModel.events, snackbar)
                SearchScreen(viewModel, openAnime)
            }
            composable("watchlist") {
                val viewModel: LibraryViewModel = hiltViewModel()
                MessagesEffect(viewModel.events, snackbar)
                WatchlistScreen(viewModel, openAnime, goHome)
            }
            composable("favorites") {
                val viewModel: LibraryViewModel = hiltViewModel()
                MessagesEffect(viewModel.events, snackbar)
                FavoritesScreen(viewModel, openAnime, goHome)
            }
            composable("settings") { SettingsScreen(settingsViewModel) }
            composable("anime/{animeId}", arguments = listOf(navArgument("animeId") { type = NavType.IntType })) {
                val viewModel: AnimeDetailViewModel = hiltViewModel()
                MessagesEffect(viewModel.events, snackbar)
                AnimeDetailScreen(viewModel, openAnime, onTrailer = { url ->
                    try { context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri())) }
                    catch (error: ActivityNotFoundException) { scope.launch { snackbar.showSnackbar("No app is available to open the trailer.") } }
                })
            }
        }
    }
}

@Composable
private fun MessagesEffect(events: Flow<String>, snackbar: SnackbarHostState) {
    LaunchedEffect(events) { events.collect { snackbar.showSnackbar(it) } }
}
