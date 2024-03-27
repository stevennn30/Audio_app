package com.serafimtech.serafimaudio.Compose

import android.net.Uri
import android.util.Log
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.serafimtech.serafimaudio.R

//import com.google.android.exoplayer2.ExoPlayer
//import com.google.android.exoplayer2.MediaItem
//import com.google.android.exoplayer2.Player
//import com.google.android.exoplayer2.ui.StyledPlayerView

@OptIn(UnstableApi::class)
@Composable
fun PlayVideo(function: (Boolean) -> Unit) {
    val context = LocalContext.current
    //設定網路視訊路徑
    val i = (Math.random() * 3).toInt() + 1
    val uri = Uri.parse(context.filesDir.path + "/video/0"+i+".mp4")
    //Test
//    val uri = Uri.parse(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).getAbsolutePath() + "/1.mp4")

    val exoPlayer = remember(context) {
        ExoPlayer.Builder(context).build().apply {
            addListener(
                object : Player.Listener {

                    @Deprecated("Deprecated in Java")
                    override fun onPlayerStateChanged(
                        playWhenReady: Boolean,
                        playbackState: Int,
                    ) {
                        when (playbackState) {
                            Player.STATE_ENDED -> {
                                Log.i("ExoPlayer", "STATE_ENDED")
                                function.invoke(false)
                            }
                            Player.STATE_BUFFERING -> {
                                Log.i("ExoPlayer", "STATE_BUFFERING")
                            }
                            Player.STATE_IDLE -> {
                                Log.i("ExoPlayer", "STATE_IDLE")
                            }
                            Player.STATE_READY -> {
                                Log.i("ExoPlayer", "STATE_READY")
                            }
                        }
                    }
                }
            )

            setMediaItem(MediaItem.fromUri(uri))
            prepare()
            play()
        }
    }

    Box(contentAlignment = Alignment.TopEnd) {
        AndroidView(
            factory = {
                /*StyledPlayerView(it).apply {
                    player = exoPlayer
                    hideController()
                    useController = false
                }*/
                PlayerView(it).apply {
                    player = exoPlayer
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT
                    hideController()
                    useController = false
                }
            }, modifier = Modifier.fillMaxSize()
        )


        Button(onClick = {
            function.invoke(false)
            exoPlayer.stop()
        },
            colors = ButtonDefaults.buttonColors(backgroundColor = Color.Transparent)) {
            Text(text = context.resources.getString(R.string.skip))
        }
    }
}