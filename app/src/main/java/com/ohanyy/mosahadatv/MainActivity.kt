package com.ohanyy.mosahadatv

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.ui.PlayerView
import kotlinx.coroutines.*
import java.net.URL

data class MediaItemModel(val title: String, val url: String, val group: String)

class MainActivity : AppCompatActivity() {

    private var player: ExoPlayer? = null
    private lateinit var playerView: PlayerView
    private lateinit var tvStatus: TextView
    private val m3uUrl = "http://mytvnet.net:2095/get.php?username=7686670927&password=4030453187&type=m3u&output=mpegts"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        playerView = findViewById(R.id.playerView)
        tvStatus = findViewById(R.id.tvStatus)

        fetchAndParseM3U()
    }

    private fun fetchAndParseM3U() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val content = URL(m3uUrl).readText()
                val mediaList = parseM3U(content)

                withContext(Dispatchers.Main) {
                    if (mediaList.isNotEmpty()) {
                        tvStatus.text = "تم فنط وجلب ${mediaList.size} عنصر بنجاح!"
                        playVideo(mediaList[0].url)
                    } else {
                        tvStatus.text = "لم يتم العثور على روابط داخل الملف."
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    tvStatus.text = "خطأ في الاتصال بالسرڤر: ${e.message}"
                }
            }
        }
    }

    private fun parseM3U(content: String): List<MediaItemModel> {
        val list = mutableListOf<MediaItemModel>()
        val lines = content.lines()
        var title = "بدون عنوان"
        var group = "عام"

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.startsWith("#EXTINF:")) {
                if (trimmed.contains("group-title=\"")) {
                    try {
                        val start = trimmed.indexOf("group-title=\"") + 13
                        val end = trimmed.indexOf("\"", start)
                        if (end != -1) group = trimmed.substring(start, end)
                    } catch (_: Exception) {}
                }
                val comma = trimmed.lastIndexOf(',')
                if (comma != -1 && comma < trimmed.length - 1) {
                    title = trimmed.substring(comma + 1).trim()
                }
            } else if (trimmed.isNotEmpty() && !trimmed.startsWith("#")) {
                list.add(MediaItemModel(title, trimmed, group))
                title = "بدون عنوان"
                group = "عام"
            }
        }
        return list
    }

    private fun playVideo(url: String) {
        player?.release()
        player = ExoPlayer.Builder(this).build().also { exoPlayer ->
            playerView.player = exoPlayer
            val mediaItem = MediaItem.fromUri(url)
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        player?.release()
        player = null
    }
}

