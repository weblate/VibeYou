package app.suhasdissa.vibeyou.presentation.screens.onlinemusic

import android.view.SoundEffectConstants
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.suhasdissa.vibeyou.R
import app.suhasdissa.vibeyou.navigation.Destination
import app.suhasdissa.vibeyou.presentation.screens.onlinemusic.components.SongsScreen
import app.suhasdissa.vibeyou.presentation.screens.player.model.PlayerViewModel
import app.suhasdissa.vibeyou.presentation.screens.playlists.PlaylistsScreen
import kotlinx.coroutines.launch

@Composable
fun MusicScreen(
    onNavigate: (Destination) -> Unit,
    playerViewModel: PlayerViewModel
) {
    val pagerState = rememberPagerState { 3 }
    val scope = rememberCoroutineScope()
    Column {
        TabRow(selectedTabIndex = pagerState.currentPage, Modifier.fillMaxWidth()) {
            val view = LocalView.current
            Tab(selected = (pagerState.currentPage == 0), onClick = {
                view.playSoundEffect(SoundEffectConstants.CLICK)
                scope.launch {
                    pagerState.animateScrollToPage(
                        0
                    )
                }
            }) {
                Text(
                    stringResource(R.string.songs),
                    Modifier.padding(10.dp),
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Tab(selected = (pagerState.currentPage == 1), onClick = {
                view.playSoundEffect(SoundEffectConstants.CLICK)
                scope.launch {
                    pagerState.animateScrollToPage(
                        1
                    )
                }
            }) {
                Text(
                    stringResource(R.string.favourite_songs),
                    Modifier.padding(10.dp),
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Tab(selected = (pagerState.currentPage == 2), onClick = {
                view.playSoundEffect(SoundEffectConstants.CLICK)
                scope.launch {
                    pagerState.animateScrollToPage(
                        2
                    )
                }
            }) {
                Text(
                    stringResource(R.string.playlists),
                    Modifier.padding(10.dp),
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { index ->
            when (index) {
                0 -> SongsScreen(showFavourites = false, playerViewModel)
                1 -> SongsScreen(showFavourites = true, playerViewModel)
                2 -> PlaylistsScreen(onNavigate = onNavigate)
            }
        }
    }
}
