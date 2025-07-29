package com.convenient.salescall.service

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class AudioRecorderService(private val context: Context) {
    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private var recordingThread: Thread? = null
    private var outputFile: File? = null

    // 配置参数
    private val sampleRate = 44100 // 采样率
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO // 单声道
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT // 16位采样
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

    // 添加录音状态回调
    interface RecordingCallback {
        fun onRecordingStarted()
        fun onRecordingStopped(file: File)
        fun onError(error: String)
    }

    private var callback: RecordingCallback? = null

    fun setCallback(callback: RecordingCallback) {
        this.callback = callback
    }

    fun startRecording() {
        if (isRecording) return

        try {
            checkPermissions()
            checkAudioParameters()
            prepareRecordingFile()
            initializeAudioRecord()
            startRecordingThread()

            callback?.onRecordingStarted()
            Log.d(TAG, "录音已成功启动")
        } catch (e: Exception) {
            handleError("录音启动失败：${e.message}", e)
        }
    }

    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            throw SecurityException("录音权限未授予，请授予权限后重试")
        }
    }

    private fun checkAudioParameters() {
        val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        if (minBufferSize <= 0) {
            throw RuntimeException("音频参数无效，无法初始化 AudioRecord")
        }
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private fun initializeAudioRecord() {
        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            channelConfig,
            audioFormat,
            bufferSize
        ).also { recorder ->
            if (recorder.state != AudioRecord.STATE_INITIALIZED) {
                throw RuntimeException("AudioRecord 初始化失败，请检查音频参数和设备状态")
            }
            recorder.startRecording()
        }
        isRecording = true
    }

    private fun prepareRecordingFile() {
        outputFile = createRecordFile()
    }

    private fun createRecordFile(): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "audio_record_$timestamp.pcm"

        val recordDir = File(context.getExternalFilesDir(null), "AudioRecords").apply {
            if (!exists()) mkdirs()
        }

        return File(recordDir, fileName)
    }

    private fun startRecordingThread() {
        recordingThread = Thread {
            val buffer = ByteArray(bufferSize)
            var outputStream: FileOutputStream? = null

            try {
                outputStream = FileOutputStream(outputFile)
                while (isRecording) {
                    val bytesRead = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    when {
                        bytesRead > 0 -> outputStream.write(buffer, 0, bytesRead)
                        bytesRead == AudioRecord.ERROR_INVALID_OPERATION ||
                                bytesRead == AudioRecord.ERROR_BAD_VALUE -> {
                            handleError("录音过程中发生错误：读取数据失败")
                            break
                        }
                    }
                }
            } catch (e: Exception) {
                handleError("录音过程中发生未知错误：${e.message}", e)
            } finally {
                outputStream?.close()
            }
        }.apply { start() }
    }

    fun stopRecording() {
        if (!isRecording) return

        try {
            isRecording = false
            recordingThread?.join()

            audioRecord?.apply {
                stop()
                release()
            }
            audioRecord = null

            // 转换文件格式
            outputFile?.let { pcmFile ->
                val wavFile = File(pcmFile.parent, "${pcmFile.nameWithoutExtension}.wav")
                convertPcmToWav(
                    pcmFile = pcmFile,
                    wavFile = wavFile,
                    sampleRate = sampleRate,
                    channels = if (channelConfig == AudioFormat.CHANNEL_IN_MONO) 1 else 2,
                    bitDepth = 16
                )

                // 删除原始PCM文件
                pcmFile.delete()

                callback?.onRecordingStopped(wavFile)
                Log.d(TAG, "录音已保存为WAV文件：${wavFile.absolutePath}")
            }
        } catch (e: Exception) {
            handleError("录音停止失败：${e.message}", e)
        }
    }

    private fun handleError(errorMessage: String, exception: Exception? = null) {
        Log.e(TAG, errorMessage, exception)
        callback?.onError(errorMessage)
        Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
    }

    private fun convertPcmToWav(
        pcmFile: File,
        wavFile: File,
        sampleRate: Int,
        channels: Int,
        bitDepth: Int
    ) {
        try {
            val rawData = pcmFile.readBytes()
            val wavData = ByteArray(44 + rawData.size) // WAV 文件头 + 原始数据

            // 填写 WAV 文件头
            // "RIFF" 标识
            "RIFF".toByteArray().copyInto(wavData, 0)

            // 文件大小 (不包括RIFF和大小字段)
            wavData.putInt(4, 36 + rawData.size)

            // "WAVE" 标识
            "WAVE".toByteArray().copyInto(wavData, 8)

            // "fmt " 子块标识
            "fmt ".toByteArray().copyInto(wavData, 12)

            // 子块1大小 (16 for PCM)
            wavData.putInt(16, 16)

            // 音频格式 (1 for PCM)
            wavData.putShort(20, 1)

            // 通道数
            wavData.putShort(22, channels)

            // 采样率
            wavData.putInt(24, sampleRate)

            // 字节率 (SampleRate * NumChannels * BitsPerSample/8)
            wavData.putInt(28, sampleRate * channels * (bitDepth / 8))

            // 块对齐 (NumChannels * BitsPerSample/8)
            wavData.putShort(32, channels * (bitDepth / 8))

            // 位深度
            wavData.putShort(34, bitDepth)

            // "data" 子块标识
            "data".toByteArray().copyInto(wavData, 36)

            // 数据大小
            wavData.putInt(40, rawData.size)

            // 复制PCM数据
            rawData.copyInto(wavData, 44)

            // 写入WAV文件
            wavFile.writeBytes(wavData)

            Log.d(TAG, "PCM转WAV成功：${wavFile.absolutePath}")
        } catch (e: Exception) {
            handleError("PCM转WAV失败：${e.message}", e)
            throw e
        }
    }

    // 扩展函数：将Int转换为字节数组（小端序）
    private fun ByteArray.putInt(offset: Int, value: Int) {
        this[offset] = (value and 0xFF).toByte()
        this[offset + 1] = (value shr 8 and 0xFF).toByte()
        this[offset + 2] = (value shr 16 and 0xFF).toByte()
        this[offset + 3] = (value shr 24 and 0xFF).toByte()
    }

    // 扩展函数：将Int转换为短整型字节数组（小端序）
    private fun ByteArray.putShort(offset: Int, value: Int) {
        this[offset] = (value and 0xFF).toByte()
        this[offset + 1] = (value shr 8 and 0xFF).toByte()
    }

    // 扩展函数：将字符串转换为字节数组
    private fun String.toByteArray(): ByteArray {
        return this.map { it.code.toByte() }.toByteArray()
    }

    companion object {
        private const val TAG = "测试"
    }
}

