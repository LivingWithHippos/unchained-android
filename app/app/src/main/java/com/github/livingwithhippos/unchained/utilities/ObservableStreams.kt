package com.github.livingwithhippos.unchained.utilities

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

// inspired by https://gaumala.com/posts/2020-01-27-working-with-streams-kotlin.html

class ObservableInputStream(
    private val wrapped: InputStream,
    private val onBytesRead: (Long) -> Unit
) : InputStream() {
    private var bytesRead: Long = 0

    @Throws(IOException::class)
    override fun read(): Int {
        val res = wrapped.read()
        if (res > -1) {
            bytesRead++
        }
        onBytesRead(bytesRead)
        return res
    }

    @Throws(IOException::class)
    override fun read(b: ByteArray): Int {
        val res = wrapped.read(b)
        if (res > -1) {
            bytesRead += res
            onBytesRead(bytesRead)
        }
        return res
    }

    @Throws(IOException::class)
    override fun read(b: ByteArray, off: Int, len: Int): Int {
        val res = wrapped.read(b, off, len)
        if (res > -1) {
            bytesRead += res
            onBytesRead(bytesRead)
        }
        return res
    }

    @Throws(IOException::class)
    override fun skip(n: Long): Long {
        val res = wrapped.skip(n)
        if (res > -1) {
            bytesRead += res
            onBytesRead(bytesRead)
        }
        return res
    }

    @Throws(IOException::class)
    override fun available(): Int {
        return wrapped.available()
    }

    override fun markSupported(): Boolean {
        return wrapped.markSupported()
    }

    override fun mark(readlimit: Int) {
        wrapped.mark(readlimit)
    }

    @Throws(IOException::class)
    override fun reset() {
        wrapped.reset()
    }

    @Throws(IOException::class)
    override fun close() {
        wrapped.close()
    }
}

class ObservableOutputStream(private val wrapped: OutputStream,
                             private val onBytesWritten: (Long) -> Unit): OutputStream() {
    private var bytesWritten: Long = 0

    @Throws(IOException::class)
    override fun write(b: Int) {
        wrapped.write(b)
        bytesWritten++
        onBytesWritten(bytesWritten)
    }

    @Throws(IOException::class)
    override fun write(b: ByteArray) {
        wrapped.write(b)
        bytesWritten += b.size.toLong()
        onBytesWritten(bytesWritten)
    }

    @Throws(IOException::class)
    override fun write(b: ByteArray, off: Int, len: Int) {
        wrapped.write(b, off, len)
        bytesWritten += len.toLong()
        onBytesWritten(bytesWritten)
    }

    @Throws(IOException::class)
    override fun flush() {
        wrapped.flush()
    }

    @Throws(IOException::class)
    override fun close() {
        wrapped.close()
    }
}

class FlowableInputStream(
    private val wrapped: InputStream
) : InputStream() {

    private val _counter: MutableStateFlow<Long> = MutableStateFlow(0)
    val counter: StateFlow<Long> get() = _counter

    @Throws(IOException::class)
    override fun read(): Int {
        val res = wrapped.read()
        if (res > -1) {
            _counter.value++
        }
        return res
    }

    @Throws(IOException::class)
    override fun read(b: ByteArray): Int {
        val res = wrapped.read(b)
        if (res > -1) {
            _counter.value+= res
        }
        return res
    }

    @Throws(IOException::class)
    override fun read(b: ByteArray, off: Int, len: Int): Int {
        val res = wrapped.read(b, off, len)
        if (res > -1) {
            _counter.value+= res
        }
        return res
    }

    @Throws(IOException::class)
    override fun skip(n: Long): Long {
        val res = wrapped.skip(n)
        if (res > -1) {
            _counter.value+= res
        }
        return res
    }

    @Throws(IOException::class)
    override fun available(): Int {
        return wrapped.available()
    }

    override fun markSupported(): Boolean {
        return wrapped.markSupported()
    }

    override fun mark(readlimit: Int) {
        wrapped.mark(readlimit)
    }

    @Throws(IOException::class)
    override fun reset() {
        wrapped.reset()
    }

    @Throws(IOException::class)
    override fun close() {
        wrapped.close()
    }
}

class FlowingOutputStream(private val wrapped: OutputStream): OutputStream() {

    private val _counter: MutableStateFlow<Long> = MutableStateFlow(0)
    val counter: StateFlow<Long> get() = _counter

    @Throws(IOException::class)
    override fun write(b: Int) {
        wrapped.write(b)
        _counter.value++
    }

    @Throws(IOException::class)
    override fun write(b: ByteArray) {
        wrapped.write(b)
        _counter.value += b.size.toLong()
    }

    @Throws(IOException::class)
    override fun write(b: ByteArray, off: Int, len: Int) {
        wrapped.write(b, off, len)
        _counter.value += len.toLong()
    }

    @Throws(IOException::class)
    override fun flush() {
        wrapped.flush()
    }

    @Throws(IOException::class)
    override fun close() {
        wrapped.close()
    }
}