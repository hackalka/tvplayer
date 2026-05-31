package com.tvplayer.tvcine

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import fi.iki.elonen.NanoHTTPD
import org.drinkless.tdlib.TdApi
import java.io.InputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.util.concurrent.ConcurrentHashMap

class TelegramProxyService : Service() {

    private var server: ProxyServer? = null
    companion object {
        var telegramManager: TelegramManager? = null
    }

    override fun onCreate() {
        super.onCreate()
        server = ProxyServer(8080)
        try {
            server?.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false)
            Log.d("ProxyService", "Servidor HTTP local iniciado en puerto 8080")
        } catch (e: Exception) {
            Log.e("ProxyService", "Error iniciando servidor", e)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        server?.stop()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private inner class ProxyServer(port: Int) : NanoHTTPD(port) {
        override fun serve(session: IHTTPSession): Response {
            val uri = session.uri
            if (uri == "/stream") {
                val fileId = session.parameters["fileId"]?.firstOrNull()?.toIntOrNull()
                if (fileId != null && telegramManager != null) {
                    return handleStream(fileId)
                }
            }
            return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Not Found")
        }

        private fun handleStream(fileId: Int): Response {
            val pipedOutputStream = PipedOutputStream()
            val pipedInputStream = PipedInputStream(pipedOutputStream)

            // Iniciamos la descarga/streaming desde TDLib
            // Nota: TDLib enviará actualizaciones de File que contienen los bytes descargados.
            // Para un streaming real con NanoHTTPD, necesitamos enganchar el receptor de actualizaciones.
            // Esta es una implementación simplificada que asume que el manager maneja el pipe.
            
            telegramManager?.send(TdApi.DownloadFile(fileId, 32, 0, 0, false)) { result ->
                // En una implementación completa, aquí capturaríamos los chunks
                // Para este ejemplo, servimos el flujo de datos
            }

            return newChunkedResponse(Response.Status.OK, "video/mp4", pipedInputStream)
        }
    }
}
