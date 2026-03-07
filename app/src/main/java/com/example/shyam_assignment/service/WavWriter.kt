package com.example.shyam_assignment.service

import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile

/**
 * Writes raw PCM audio data wrapped as a WAV file.
 * Call [start] to open, [write] to append PCM bytes, and [finish] to
 * write the correct header sizes and close the file.
 *
 * Format: 16-bit mono PCM at the given sample rate.
 */
class WavWriter(
    private val file: File,
    private val sampleRate: Int = 16_000,
    private val channels: Int = 1,
    private val bitsPerSample: Int = 16
) {
    private var outputStream: FileOutputStream? = null
    private var totalDataBytes: Long = 0L

    /**
     * Opens the file and writes a placeholder WAV header (44 bytes).
     * The header sizes will be patched in [finish].
     */
    fun start() {
        outputStream = FileOutputStream(file)
        // Write placeholder header — sizes filled in by finish()
        val header = buildWavHeader(0)
        outputStream?.write(header)
        totalDataBytes = 0L
    }

    /**
     * Appends raw PCM bytes to the data section.
     */
    fun write(buffer: ByteArray, offset: Int = 0, length: Int = buffer.size) {
        outputStream?.write(buffer, offset, length)
        totalDataBytes += length
    }

    /**
     * Flushes, patches the WAV header with correct sizes, and closes the file.
     */
    fun finish() {
        try {
            outputStream?.flush()
            outputStream?.close()
        } catch (_: Exception) {}
        outputStream = null

        // Patch header with actual data size
        try {
            RandomAccessFile(file, "rw").use { raf ->
                val totalFileSize = 44L + totalDataBytes
                // Bytes 4-7: file size - 8
                raf.seek(4)
                raf.write(intToLittleEndian((totalFileSize - 8).toInt()))
                // Bytes 40-43: data chunk size
                raf.seek(40)
                raf.write(intToLittleEndian(totalDataBytes.toInt()))
            }
        } catch (_: Exception) {}
    }

    /** Total PCM data bytes written so far. */
    val dataBytesWritten: Long get() = totalDataBytes

    // ── Internal ───────────────────────────────────────────────────────

    private fun buildWavHeader(dataSize: Int): ByteArray {
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val blockAlign = channels * bitsPerSample / 8
        val totalSize = 36 + dataSize

        return ByteArray(44).also { h ->
            // RIFF header
            "RIFF".toByteArray().copyInto(h, 0)
            intToLittleEndian(totalSize).copyInto(h, 4)
            "WAVE".toByteArray().copyInto(h, 8)

            // fmt sub-chunk
            "fmt ".toByteArray().copyInto(h, 12)
            intToLittleEndian(16).copyInto(h, 16)           // sub-chunk size
            shortToLittleEndian(1).copyInto(h, 20)          // PCM format
            shortToLittleEndian(channels).copyInto(h, 22)
            intToLittleEndian(sampleRate).copyInto(h, 24)
            intToLittleEndian(byteRate).copyInto(h, 28)
            shortToLittleEndian(blockAlign).copyInto(h, 32)
            shortToLittleEndian(bitsPerSample).copyInto(h, 34)

            // data sub-chunk
            "data".toByteArray().copyInto(h, 36)
            intToLittleEndian(dataSize).copyInto(h, 40)
        }
    }

    private fun intToLittleEndian(value: Int): ByteArray = byteArrayOf(
        (value and 0xFF).toByte(),
        (value shr 8 and 0xFF).toByte(),
        (value shr 16 and 0xFF).toByte(),
        (value shr 24 and 0xFF).toByte()
    )

    private fun shortToLittleEndian(value: Int): ByteArray = byteArrayOf(
        (value and 0xFF).toByte(),
        (value shr 8 and 0xFF).toByte()
    )
}

