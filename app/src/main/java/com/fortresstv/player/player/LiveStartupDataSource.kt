package com.fortresstv.player.player

import android.net.Uri
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.HttpDataSource
import androidx.media3.datasource.TransferListener
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.atomic.AtomicBoolean

internal class LiveStartupTimeoutException(cause: IOException) : IOException(cause)

@UnstableApi
internal class LiveStartupDataSource(
    private val regular: HttpDataSource,
    private val fast: HttpDataSource?,
    private val firstOpen: AtomicBoolean,
    private val tune: LiveTuneBenchmark?
) : DataSource {
    private var active = regular

    override fun addTransferListener(transferListener: TransferListener) {
        regular.addTransferListener(transferListener)
        fast?.addTransferListener(transferListener)
    }

    override fun open(dataSpec: DataSpec): Long {
        val isFirstOpen = firstOpen.compareAndSet(true, false)
        val useFastTimeout = fast != null && isFirstOpen && dataSpec.position == 0L
        active = if (useFastTimeout) checkNotNull(fast) else regular
        try {
            return tune?.openHttp(active, dataSpec) ?: active.open(dataSpec)
        } catch (error: HttpDataSource.HttpDataSourceException) {
            if (useFastTimeout && error.type == HttpDataSource.HttpDataSourceException.TYPE_OPEN &&
                error.cause is SocketTimeoutException
            ) {
                throw LiveStartupTimeoutException(error)
            }
            throw error
        }
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int = active.read(buffer, offset, length)

    override fun getUri(): Uri? = active.uri

    override fun getResponseHeaders(): Map<String, List<String>> = active.responseHeaders

    override fun close() = active.close()
}
