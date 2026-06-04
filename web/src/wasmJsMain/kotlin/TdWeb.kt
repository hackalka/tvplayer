import kotlin.js.JsAny

fun createTdClient(options: JsAny): JsAny? = js("window.createClient(options)")

fun sendQuery(client: JsAny, query: JsAny): JsAny = js("client.send(query)")

fun setUpdateHandler(client: JsAny, handler: (JsAny) -> Unit): Unit = js("client.onUpdate = handler")

fun createTdOptions(apiId: Int, apiHash: String): JsAny = js("""
    ({
        api_id: apiId,
        api_hash: apiHash,
        use_database: true,
        use_message_database: true,
        use_chat_info_database: true,
        use_file_database: true,
        use_secret_chats: false,
        system_language_code: 'es',
        device_model: 'WebBrowser',
        system_version: 'Chrome',
        application_version: '2.1'
    })
""")

fun createBaseQuery(type: String): JsAny = js("""({ '@type': type })""")

fun addParamToQuery(query: JsAny, key: String, value: String): JsAny = js("""
    (function(q, k, v) {
        q[k] = v;
        return q;
    })(query, key, value)
""")

fun addIntParamToQuery(query: JsAny, key: String, value: Int): JsAny = js("""
    (function(q, k, v) {
        q[k] = v;
        return q;
    })(query, key, value)
""")

fun addBooleanParamToQuery(query: JsAny, key: String, value: Boolean): JsAny = js("""
    (function(q, k, v) {
        q[k] = v;
        return q;
    })(query, key, value)
""")

fun getTdType(obj: JsAny?): String = js("window.getTdType(obj)")
fun getAuthState(update: JsAny?): String = js("window.getAuthState(update)")
fun hideLoadingScreen(): Unit = js("window.hideLoadingScreen()")

fun loadGroupVideos(client: JsAny, inviteLink: String, limit: Int, handler: (JsAny) -> Unit): Unit = js("""
    (function(client, inviteLink, limit, handler) {
        function loadHistory(chatId) {
            client.send({
                '@type': 'getChatHistory',
                chat_id: chatId,
                from_message_id: 0,
                offset: 0,
                limit: limit,
                only_local: false
            }).then(function(history) {
                var messages = history && history.messages ? history.messages : [];
                handler({ '@type': 'loadedGroupVideos', videos: messages });
            }).catch(function(err) {
                handler({ '@type': 'loadGroupVideosError', message: 'Error cargando historial' });
            });
        }

        client.send({ '@type': 'checkChatInviteLink', invite_link: inviteLink })
            .then(function(info) {
                if (info && info.chat_id) return loadHistory(info.chat_id);
                return client.send({ '@type': 'joinChatByInviteLink', invite_link: inviteLink })
                    .then(function(chat) {
                        if (chat && chat.id) return loadHistory(chat.id);
                        handler({ '@type': 'loadGroupVideosError', message: 'No se pudo entrar al grupo' });
                    });
            })
            .catch(function(err) {
                // Si falla el link de invitacion, probamos con el ID directo si es posible
                handler({ '@type': 'loadGroupVideosError', message: err.message || 'Link inválido' });
            });
    })(client, inviteLink, limit, handler)
""")

fun videosCount(result: JsAny?): Int = js("""
    (result && result.videos && result.videos.length) ? result.videos.length : 0
""")

fun videoTitle(result: JsAny?, index: Int): String = js("""
    (function() {
        var msg = result.videos[index];
        if (msg && msg.content && msg.content.caption && msg.content.caption.text) {
            return msg.content.caption.text.split('\n')[0];
        }
        return 'Video sin título';
    })()
""")

fun videoGenre(result: JsAny?, index: Int): String = js("'Telegram'")

fun videoPosterUrl(result: JsAny?, index: Int): String = js("''")

fun getErrorMessage(result: JsAny?): String = js("""
    (result && result.message) ? result.message : 'Error desconocido'
""")
