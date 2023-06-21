package app.suhasdissa.mellowmusic.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddToQueue
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.suhasdissa.mellowmusic.R
import app.suhasdissa.mellowmusic.backend.database.entities.Song
import app.suhasdissa.mellowmusic.backend.viewmodel.PlayerViewModel
import app.suhasdissa.mellowmusic.backend.viewmodel.SongViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongSettingsSheet(
    onDismissRequest: () -> Unit,
    song: Song,
    songViewModel: SongViewModel = viewModel(factory = SongViewModel.Factory),
    playerViewModel: PlayerViewModel = viewModel(factory = PlayerViewModel.Factory)
) {
    val songSettingsSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = songSettingsSheetState,
        dragHandle = null,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                modifier = Modifier
                    .size(64.dp)
                    .padding(8.dp)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(8.dp)),
                model = ImageRequest.Builder(context = LocalContext.current)
                    .data(song.thumbnailUrl).crossfade(true).build(),
                contentDescription = null,
                contentScale = ContentScale.Crop
            )
            Column(
                Modifier
                    .weight(1f)
                    .padding(8.dp)
            ) {
                Text(
                    song.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                song.artistsText?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

            }
            Column(
                Modifier
                    .padding(8.dp)
            ) {
                var favouriteState by remember { mutableStateOf(song.isFavourite) }
                IconButton(onClick = {
                    playerViewModel.toggleFavourite(song.id)
                    favouriteState = !favouriteState
                }) {
                    if (favouriteState) {
                        Icon(Icons.Default.Favorite, contentDescription = null)
                    } else {
                        Icon(Icons.Default.FavoriteBorder, contentDescription = null)
                    }
                }
            }
        }
        Column(Modifier.padding(vertical = 16.dp)) {
            SheetSettingItem(
                icon = Icons.Default.PlayArrow,
                description = R.string.play_song,
                onClick = {
                    playerViewModel.playSong(song)
                    onDismissRequest()
                })
            SheetSettingItem(
                icon = Icons.Default.AddToQueue,
                description = R.string.enqueue_song,
                onClick = {
                    playerViewModel.schedulePlay(song)
                    onDismissRequest()
                })
            SheetSettingItem(
                icon = Icons.Default.Delete,
                description = R.string.delete_song,
                onClick = {
                    songViewModel.removeSong(song)
                    onDismissRequest()
                })
        }
    }
}

@Composable
fun SheetSettingItem(icon: ImageVector, @StringRes description: Int, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { onClick() }) {
        Icon(imageVector = icon, contentDescription = null)
        Spacer(Modifier.width(16.dp))
        Text(text = stringResource(id = description))
    }
}