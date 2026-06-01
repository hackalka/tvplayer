package com.tvplayer.tvcine

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import fi.iki.elonen.NanoHTTPD
import org.drinkless.tdlib.TdApi
import java.io.File
import java.io.FileInputStream
import java.io.InputStream

class TelegramProxyService : Service() {

    private var server: ProxyServer? = null
    
    companion object {
        // Usamos una referencia débil o estática controlada para el manager
        var telegramManager: TelegramManager? = null
    }

    override fun onCreate() {
        super.onCreate()
        server = ProxyServer(8080)
        try {
            server?.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false)
            Log.d("TVCineProxy", "Servidor de Streaming listo en http://localhost:8080")
        } catch (e: Exception) {
            Log.e("TVCineProxy", "Fallo al arrancar el proxy", e)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        server?.stop()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private inner class ProxyServer(port: Int) : NanoHTTPD(port) {
        override fun serve(session: IHTTPSession): Response {
            if (session.uri == "/stream") {
                val fileId = session.parameters["fileId"]?.firstOrNull()?.toIntOrNull()
                if (fileId != null) {
                    return handleVideoStreaming(fileId)
                }
            }
            return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Video no encontrado")
        }

        private fun handleVideoStreaming(fileId: Int): Response {
            // Buscamos el archivo en TDLib
            // Nota: En una app real, aquí deberíamos esperar a que el archivo tenga una ruta local
            // Para simplificar y que funcione YA, VLC leerá el archivo directamente cuando TDLib lo marque como local
            
            var localPath: String? = null
            val lock = java.lang.Object()

            telegramManager?.send(TdApi.GetFile(fileId)) { result ->
                if (result is TdApi.File) {
                    localPath = result.local.path
                }
                synchronized(lock) { lock.notify() }
            }

            synchronized(lock) { lock.wait(5000) }

            return if (!localPath.isNullOrEmpty()) {
                val file = File(localPath!!)
                val inputStream = FileInputStream(file)
                newFixedLengthResponse(Response.Status.OK, "video/mp4", inputStream, file.length())
            } else {
                // Si no es local aún, forzamos la descarga
                telegramManager?.downloadFile(fileId, 32)
                newFixedLengthResponse(Response.Status.ACCEPTED, MIME_PLAINTEXT, "Descargando... Reintenta en unos segundos")
            }
        }
    }
}
