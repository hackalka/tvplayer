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
            filesDirectory = File(context.filesDir, "tdlib_files").absolutePath
            useFileDatabase = true
        }
        send(parameters) { result ->
            if (result is TdApi.Error) {
                Log.e("TelegramManager", "Error setupParameters: ${result.message}")
            }
        }
    }

    fun setAuthenticationPhoneNumber(phoneNumber: String) {
        val settings = TdApi.PhoneNumberAuthenticationSettings()
        send(TdApi.SetAuthenticationPhoneNumber(phoneNumber, settings)) { result ->
            if (result is TdApi.Error) {
                Log.e("TelegramManager", "Error setPhoneNumber: ${result.message}")
            }
        }
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
        Log.d("TelegramManager", "Adding group: $query")
        
        // Extraer ID de links tipo https://web.telegram.org/a/#-1003749684388
        if (query.contains("#-100")) {
            val idStr = query.substringAfter("#") // "-1003749684388"
            val chatId = idStr.toLongOrNull()
            if (chatId != null) {
                getChat(chatId) { chat ->
                    callback(chat)
                    send(TdApi.JoinChat(chat.id)) {}
                }
                return
            }
        }

        val invitePrefix = "https://t.me/+"
        if (query.startsWith(invitePrefix) || query.contains("joinchat")) {
            send(TdApi.CheckChatInviteLink(query)) { result ->
                if (result is TdApi.ChatInviteLinkInfo) {
                    val chatId = result.chatId
                    if (chatId != 0L) {
                        getChat(chatId) { chat ->
                            callback(chat)
                            send(TdApi.JoinChatByInviteLink(query)) {}
                        }
                    } else {
                        send(TdApi.JoinChatByInviteLink(query)) { res ->
                            if (res is TdApi.Chat) callback(res)
                        }
                    }
                } else {
                    send(TdApi.JoinChatByInviteLink(query)) { res ->
                        if (res is TdApi.Chat) callback(res)
                    }
                }
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

    fun getForumTopics(chatId: Long, callback: (TdApi.ForumTopics) -> Unit) {
        send(TdApi.GetForumTopics(chatId, "", 0, 0, 0, 100)) { result ->
            if (result is TdApi.ForumTopics) callback(result)
        }
    }

    fun getChatMessages(chatId: Long, fromMessageId: Long = 0, limit: Int = 50, callback: (TdApi.Messages) -> Unit) {
        send(TdApi.GetChatHistory(chatId, fromMessageId, 0, limit, false)) { result ->
            if (result is TdApi.Messages) callback(result)
        }
    }

    fun getMessageThreadHistory(chatId: Long, messageThreadId: Long, fromMessageId: Long = 0, limit: Int = 50, callback: (TdApi.Messages) -> Unit) {
        send(TdApi.GetMessageThreadHistory(chatId, messageThreadId, fromMessageId, 0, limit)) { result ->
            if (result is TdApi.Messages) callback(result)
        }
    }

    fun stop() {
        client?.send(TdApi.Close(), { client = null })
    }
}
