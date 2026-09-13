package ru.explosive.caremom.util

import android.content.Context
import android.media.MediaPlayer
import java.io.File
import java.io.IOException

class AudioPlayer(private val context: Context) {
    private var player: MediaPlayer? = null

    fun playFile(path: String) {
        val file = File(path)
        if (!file.exists()) return

        try {
            stop()
            player = MediaPlayer().apply {
                setDataSource(path)
                prepare()
                start()
                setOnCompletionListener { 
                    stop()
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun stop() {
        try {
            player?.let {
                if (it.isPlaying) it.stop()
                it.release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            player = null
        }
    }
}
