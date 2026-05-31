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
            Log.e("TelegramManager", "Native library tdjni not found.", e)
        }
    }

    fun start(onUpdate: (TdApi.Object) -> Unit) {
        client = Client.create({ update ->
            if (update is TdApi.UpdateAuthorizationState) {
                authorizationState = update.authorizationState
            }
            onUpdate(update)
        }, { exception ->
            Log.e("TelegramManager", "Client error", exception)
        }, { exception ->
            Log.e("TelegramManager", "Internal error", exception)
        })
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

        send(parameters) {
            Log.d("TelegramManager", "Parameters set: $it")
        }
    }

    fun setAuthenticationPhoneNumber(phoneNumber: String) {
        send(TdApi.SetAuthenticationPhoneNumber(phoneNumber, null)) {
            Log.d("TelegramManager", "Phone number set: $it")
        }
    }

    fun checkAuthenticationCode(code: String) {
        send(TdApi.CheckAuthenticationCode(code)) {
            Log.d("TelegramManager", "Code check: $it")
        }
    }

    fun checkAuthenticationPassword(password: String) {
        send(TdApi.CheckAuthenticationPassword(password)) {
            Log.d("TelegramManager", "Password check: $it")
        }
    }

    fun send(query: TdApi.Function<*>, callback: (TdApi.Object) -> Unit) {
        client?.send(query, callback)
    }

    fun getChats(limit: Int = 20, callback: (TdApi.Chats) -> Unit) {
        send(TdApi.GetChats(TdApi.ChatListMain(), limit)) { result ->
            if (result is TdApi.Chats) {
                callback(result)
            }
        }
    }

    fun getChat(chatId: Long, callback: (TdApi.Chat) -> Unit) {
        send(TdApi.GetChat(chatId)) { result ->
            if (result is TdApi.Chat) {
                callback(result)
            }
        }
    }

    fun downloadFile(fileId: Int, priority: Int = 1) {
        send(TdApi.DownloadFile(fileId, priority, 0, 0, false)) {}
    }

    fun addGroup(query: String, callback: (TdApi.Chat) -> Unit) {
        if (query.startsWith("https://t.me/joinchat/") || query.startsWith("https://t.me/+")) {
            send(TdApi.JoinChatByInviteLink(query)) { result ->
                if (result is TdApi.Chat) callback(result)
            }
        } else {
            val username = query.removePrefix("https://t.me/").removePrefix("@")
            send(TdApi.SearchPublicChat(username)) { result ->
                if (result is TdApi.Chat) {
                    send(TdApi.JoinChat(result.id)) { joinResult ->
                        if (joinResult is TdApi.Ok) callback(result)
                    }
                }
            }
        }
    }

    fun stop() {
        client?.send(TdApi.Close(), {
            client = null
        })
    }
}
