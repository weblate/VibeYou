package app.suhasdissa.mellowmusic.backend.services

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.audiofx.LoudnessEnhancer
import android.net.Uri
import android.os.Handler
import androidx.core.graphics.drawable.toBitmap
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.RenderersFactory
import androidx.media3.exoplayer.audio.AudioRendererEventListener
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.audio.MediaCodecAudioRenderer
import androidx.media3.exoplayer.audio.SilenceSkippingAudioProcessor
import androidx.media3.exoplayer.audio.SonicAudioProcessor
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.session.BitmapLoader
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import app.suhasdissa.mellowmusic.MellowMusicApplication
import app.suhasdissa.mellowmusic.utils.DynamicDataSource
import app.suhasdissa.mellowmusic.utils.Pref
import app.suhasdissa.mellowmusic.utils.mediaIdList
import coil.ImageLoader
import coil.request.ErrorResult
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class PlayerService : MediaSessionService(), MediaSession.Callback, Player.Listener {
    private var mediaSession: MediaSession? = null
    private lateinit var cache: SimpleCache
    private lateinit var player: ExoPlayer

    private var loudnessEnhancer: LoudnessEnhancer? = null

    val container get() = (application as MellowMusicApplication).container

    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        val maxMBytes = Pref.sharedPreferences.getInt(Pref.exoCacheKey, 0)
        val cacheEvictor =
            if (maxMBytes > 0) LeastRecentlyUsedCacheEvictor(maxMBytes * 1024 * 1024L) else NoOpCacheEvictor()
        val directory = cacheDir.resolve("exoplayer").also { directory ->
            directory.mkdir()
        }
        cache = SimpleCache(directory, cacheEvictor, StandaloneDatabaseProvider(this))

        player = createPlayer()

        player.repeatMode = Player.REPEAT_MODE_OFF
        player.playWhenReady = true
        player.addListener(this)

        mediaSession = MediaSession.Builder(this, player).setCallback(this)
            .setBitmapLoader(CustomBitmapLoader(this))
            .build()
    }

    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    private fun createPlayer(): ExoPlayer {
        return ExoPlayer.Builder(this, createRendersFactory(), createMediaSourceFactory())
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true
            )
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    @SuppressLint("UnsafeOptInUsageError")
    class CustomBitmapLoader(private val context: Context) : BitmapLoader {
        private val scope = CoroutineScope(Dispatchers.IO)
        override fun decodeBitmap(data: ByteArray): ListenableFuture<Bitmap> {
            val future = SettableFuture.create<Bitmap>()
            try {
                val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size)
                assert(bitmap != null)
                future.set(bitmap)
            } catch (e: Exception) {
                future.setException(e)
            }
            return future
        }

        override fun loadBitmap(uri: Uri): ListenableFuture<Bitmap> {
            val future = SettableFuture.create<Bitmap>()
            scope.launch {
                if ("file" == uri.scheme) {
                    try {
                        val bitmap = BitmapFactory.decodeFile(uri.path)
                        future.set(bitmap)
                    } catch (e: Exception) {
                        future.setException(e)
                    }
                } else {
                    val imageLoader = ImageLoader.Builder(context).build()
                    val request = ImageRequest.Builder(context)
                        .data(uri)
                        .build()
                    val result = imageLoader.execute(request)
                    if (result is SuccessResult) {
                        future.set(result.drawable.toBitmap())
                    } else if (result is ErrorResult) {
                        future.setException(result.throwable)
                    }
                }
            }
            return future
        }
    }

    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    override fun onDestroy() {
        player.removeListener(this)
        mediaSession?.run {
            player.stop()
            player.release()
            mediaSession = null
        }
        cache.release()

        loudnessEnhancer?.release()

        super.onDestroy()
    }

    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    private fun createCacheDataSource(): DataSource.Factory {
        return CacheDataSource.Factory().setCache(cache).apply {
            setUpstreamDataSourceFactory(
                DefaultHttpDataSource.Factory()
                    .setConnectTimeoutMs(16000)
                    .setReadTimeoutMs(8000)
                    .setUserAgent(
                        "Mozilla/5.0 (Windows NT 10.0; rv:91.0) Gecko/20100101 Firefox/91.0"
                    )
            )
            setUpstreamDataSourceFactory(DefaultDataSource.Factory(this@PlayerService))
        }
    }

    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    private fun createDataSourceFactory(): DataSource.Factory {
        val chunkLength = 512 * 1024L

        val defaultDataSource = DefaultDataSource.Factory(this@PlayerService)
        val resolvingDataSource = ResolvingDataSource.Factory(createCacheDataSource()) { dataSpec ->
            val videoId = dataSpec.key ?: error("A key must be set")

            if (cache.isCached(videoId, dataSpec.position, chunkLength)) {
                dataSpec
            } else {
                val url = runBlocking {
                    container.pipedMusicRepository.getAudioSource(videoId)
                }
                url?.let {
                    dataSpec.withUri(it).subrange(dataSpec.uriPositionOffset, chunkLength)
                } ?: error("Stream not found")
            }
        }
        return DynamicDataSource.Companion.Factory(resolvingDataSource, defaultDataSource)
    }

    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    private fun createMediaSourceFactory(): MediaSource.Factory {
        return DefaultMediaSourceFactory(createDataSourceFactory())
    }

    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    override fun onAddMediaItems(
        mediaSession: MediaSession,
        controller: MediaSession.ControllerInfo,
        mediaItems: MutableList<MediaItem>
    ): ListenableFuture<MutableList<MediaItem>> {
        val mediaIdList = mediaSession.player.currentTimeline.mediaIdList
        val updatedMediaItems =
            mediaItems.filterNot {
                mediaIdList.contains(it.mediaId)
            }.map {
                it.buildUpon().setUri(it.mediaId).setCustomCacheKey(it.mediaId).build()
            }.toMutableList()
        return Futures.immediateFuture(updatedMediaItems)
    }

    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    private fun createRendersFactory(): RenderersFactory {
        val audioSink = DefaultAudioSink.Builder()
            .setEnableFloatOutput(false)
            .setEnableAudioTrackPlaybackParams(false)
            .setOffloadMode(DefaultAudioSink.OFFLOAD_MODE_DISABLED)
            .setAudioProcessorChain(
                DefaultAudioSink.DefaultAudioProcessorChain(
                    emptyArray(),
                    SilenceSkippingAudioProcessor(2_000_000, 20_000, 256),
                    SonicAudioProcessor()
                )
            )
            .build()
        return RenderersFactory { handler: Handler?, _, audioListener: AudioRendererEventListener?, _, _ ->
            arrayOf(
                MediaCodecAudioRenderer(
                    this,
                    MediaCodecSelector.DEFAULT,
                    handler,
                    audioListener,
                    audioSink
                )
            )
        }
    }
}
