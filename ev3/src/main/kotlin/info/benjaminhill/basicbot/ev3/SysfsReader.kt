package info.benjaminhill.basicbot.ev3

import java.io.File
import java.io.RandomAccessFile
import java.nio.file.Path


/**
 * Read sensor values from files really, really fast with minimal overhead.
 * Not even a little thread-safe.
 * You decide if you want Long or Float values
 */
class SysfsReader(file: File) : AutoCloseable {
    init {
        // Only check once at startup
        require(file.exists() && file.canRead()) { "Unable to read $file" }
    }

    constructor(path: Path) : this(path.toFile())

    private val raf = RandomAccessFile(file, READ_MODE)
    private val buf = ByteArray(512)

    private var readBytes = 0
    private var value = 0L
    private var isPositive = true
    private var isValid = false
    private var floatExp = 0L

    private fun readNumber() {
        isValid = false
        raf.seek(0)
        readBytes = raf.read(buf)
        if (readBytes <= 0) {
            return
        }
        isValid = true
        value = 0
        floatExp = 0
        isPositive = true
        allChars@ for (i in 0 until readBytes) {
            when (buf[i]) {
                MINUS -> isPositive = false
                PERIOD -> floatExp = 1
                in ZERO..NINE -> {
                    value *= 10
                    value += buf[i] - ZERO
                    floatExp *= 10
                }
                else -> break@allChars
            }
        }
        if (!isPositive) {
            value *= -1
        }
    }

    /** Null if an issue */
    fun readLong(): Long? {
        readNumber()
        if (!isValid) {
            return null
        }
        if (floatExp != 0L) {
            // Not efficient, don't do this.
            return (value.toFloat() / Math.max(1, floatExp)).toLong()
        }
        return value
    }


    /** NaN if there was an issue */
    fun readFloat(): Float? {
        readNumber()
        if (!isValid) {
            return null
        }
        return value.toFloat() / Math.max(1, floatExp)
    }

    override fun close() {
        raf.close()
    }

    companion object {
        const val MINUS = '-'.toByte()
        const val PERIOD = '.'.toByte()
        const val ZERO = '0'.toByte()
        const val NINE = '9'.toByte()
        const val READ_MODE = "r"
    }
}