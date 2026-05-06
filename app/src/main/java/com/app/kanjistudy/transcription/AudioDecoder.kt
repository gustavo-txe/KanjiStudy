package com.app.kanjistudy.transcription

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import java.nio.ByteBuffer
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AudioDecoder(private val context: Context) {

    suspend fun decodeToPcm16kMono(
        uri: Uri,
        clipStartMs: Long,
        clipEndMs: Long
    ): ShortArray = withContext(Dispatchers.IO) {
        val extractor = MediaExtractor()
        extractor.setDataSource(context, uri, null)
        val trackIndex = (0 until extractor.trackCount).firstOrNull {
            extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
        } ?: throw IllegalArgumentException("No audio track found")

        extractor.selectTrack(trackIndex)
        val format = extractor.getTrackFormat(trackIndex)
        val mime = format.getString(MediaFormat.KEY_MIME) ?: throw IllegalStateException("Unknown MIME")
        val sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        val channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)

        val codec = MediaCodec.createDecoderByType(mime)
        codec.configure(format, null, null, 0)
        codec.start()

        val clipStartUs = clipStartMs * 1000
        val clipEndUs = clipEndMs * 1000
        extractor.seekTo(clipStartUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)

        val out = ArrayList<Short>()
        val info = MediaCodec.BufferInfo()
        var inputDone = false
        var outputDone = false

        while (!outputDone) {
            if (!inputDone) {
                val inputIndex = codec.dequeueInputBuffer(10_000)
                if (inputIndex >= 0) {
                    val inputBuffer = codec.getInputBuffer(inputIndex) ?: continue
                    val sampleSize = extractor.readSampleData(inputBuffer, 0)
                    val sampleTime = extractor.sampleTime
                    if (sampleSize < 0 || sampleTime < 0 || sampleTime > clipEndUs) {
                        codec.queueInputBuffer(inputIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        inputDone = true
                    } else {
                        codec.queueInputBuffer(inputIndex, 0, sampleSize, sampleTime, 0)
                        extractor.advance()
                    }
                }
            }

            val outputIndex = codec.dequeueOutputBuffer(info, 10_000)
            when {
                outputIndex >= 0 -> {
                    val outputBuffer: ByteBuffer = codec.getOutputBuffer(outputIndex) ?: continue
                    if (info.size > 0 && info.presentationTimeUs in clipStartUs..clipEndUs) {
                        val bytes = ByteArray(info.size)
                        outputBuffer.get(bytes)
                        val samples = ShortArray(bytes.size / 2)
                        var i = 0
                        while (i < samples.size) {
                            val low = bytes[i * 2].toInt() and 0xff
                            val high = bytes[i * 2 + 1].toInt()
                            samples[i] = ((high shl 8) or low).toShort()
                            i++
                        }
                        out.addAll(toMono(samples, channels).toList())
                    }
                    outputBuffer.clear()
                    codec.releaseOutputBuffer(outputIndex, false)
                    if ((info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        outputDone = true
                    }
                }
                outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> Unit
            }
        }

        codec.stop(); codec.release(); extractor.release()
        resampleTo16k(out.toShortArray(), sampleRate)
    }

    private fun toMono(input: ShortArray, channels: Int): ShortArray {
        if (channels == 1) return input
        val mono = ShortArray(input.size / channels)
        var i = 0
        while (i < mono.size) {
            var sum = 0
            for (c in 0 until channels) sum += input[i * channels + c]
            mono[i] = (sum / channels).toShort()
            i++
        }
        return mono
    }

    private fun resampleTo16k(input: ShortArray, inSampleRate: Int): ShortArray {
        if (inSampleRate == 16000) return input
        val ratio = 16000.0 / inSampleRate
        val outSize = (input.size * ratio).roundToInt().coerceAtLeast(1)
        val out = ShortArray(outSize)
        for (i in 0 until outSize) {
            val src = i / ratio
            val idx = src.toInt().coerceIn(0, input.lastIndex)
            out[i] = input[idx]
        }
        return out
    }
}