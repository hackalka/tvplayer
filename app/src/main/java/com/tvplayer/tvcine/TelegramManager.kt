package com.tvplayer.tvcine

import android.content.Context
import android.util.Log
import org.drinkless.tdlib.Client
import org.drinkless.tdlib.TdApi
import java.io.File

class TelegramManager(private val context: Context) {

    private var client: Client? = null
    var authorizationState: TdApi.AuthorizationState? = null
        private set

    init {
        try {
            System.loadLibrary("tdjni")
        } catch (e: UnsatisfiedLinkError) {
            Log.e("TelegramManager", "Librería nativa TDLib no cargada", e)
        }
    }

    fun start(onUpdate: (TdApi.Object) -> Unit) {
        client = Client.create({ update ->
            if (update is TdApi.UpdateAuthorizationState) {
                authorizationState = update.authorizationState
            }
            onUpdate(update)
        }, { Log.e("TelegramManager", "Error: $it") }, { Log.e("TelegramManager", "Internal Error: $it") })
    }

    fun setupParameters() {
        val databasePath = File(context.filesDir, "tdlib").absolutePath
        val parameters = TdApi.SetTdlibParameters().apply {
            apiId = TelegramConfig.API_ID
            apiHash = TelegramConfig.API_HASH
            useMessageDatabase = true
            useChatInfoDatabase = true
            useSecretChats = true
            systemLanguageCode = "es"
            deviceModel = android.os.Build.MODEL
            systemVersion = android.os.Build.VERSION.RELEASE
            applicationVersion = "1.0"
            databaseDirectory = databasePath
            useFileDatabase = true
        }
        send(parameters) {}
    }

    fun setAuthenticationPhoneNumber(phoneNumber: String) {
        send(TdApi.SetAuthenticationPhoneNumber(phoneNumber, null)) {}
    }

    fun checkAuthenticationCode(code: String) {
        send(TdApi.CheckAuthenticationCode(code)) {}
    }

    fun checkAuthenticationPassword(password: String) {
        send(TdApi.CheckAuthenticationPassword(password)) {}
    }

    fun send(query: TdApi.Function<*>, callback: (TdApi.Object) -> Unit) {
        client?.send(query, callback)
    }

    fun getChats(callback: (TdApi.Chats) -> Unit) {
        send(TdApi.GetChats(TdApi.ChatListMain(), 30)) { result ->
            if (result is TdApi.Chats) callback(result)
        }
    }

    fun getChat(chatId: Long, callback: (TdApi.Chat) -> Unit) {
        send(TdApi.GetChat(chatId)) { result ->
            if (result is TdApi.Chat) callback(result)
        }
    }

    fun downloadFile(fileId: Int, priority: Int = 1) {
        send(TdApi.DownloadFile(fileId, priority, 0, 0, false)) {}
    }

    fun addGroup(query: String, callback: (TdApi.Chat) -> Unit) {
        val invitePrefix = "https://t.me/+"
        if (query.startsWith(invitePrefix) || query.contains("joinchat")) {
            send(TdApi.JoinChatByInviteLink(query)) { result ->
                if (result is TdApi.Chat) callback(result)
            }
        } else {
            val username = query.removePrefix("https://t.me/").removePrefix("@")
            send(TdApi.SearchPublicChat(username)) { result ->
                if (result is TdApi.Chat) {
                    send(TdApi.JoinChat(result.id)) { callback(result) }
                }
            }
        }
    }

    fun getChatMessages(chatId: Long, limit: Int = 50, callback: (TdApi.Messages) -> Unit) {
        send(TdApi.GetChatHistory(chatId, 0, 0, limit, false)) { result ->
            if (result is TdApi.Messages) callback(result)
        }
    }

    fun stop() {
        client?.send(TdApi.Close(), { client = null })
    }
}
