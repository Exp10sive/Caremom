package ru.explosive.caremom.util

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File
import java.io.FileOutputStream

class AudioRecorder(private val context: Context) {

    private var recorder: MediaRecorder? = null
    private var isRecording = false

    fun start(outputFile: File) {
        try {
            createRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(FileOutputStream(outputFile).fd)

                prepare()
                start()
                recorder = this
                isRecording = true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stop() {
        if (isRecording) {
            try {
                recorder?.stop()
                recorder?.reset()
                recorder?.release()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                recorder = null
                isRecording = false
            }
        }
    }

    private fun createRecorder(): MediaRecorder {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
    }
}
