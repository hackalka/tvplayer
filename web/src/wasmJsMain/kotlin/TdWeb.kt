import kotlin.js.JsAny

fun createTdClient(options: JsAny): JsAny? = js("window.createClient(options)")

fun sendQuery(client: JsAny, query: JsAny): JsAny = js("client.send(query)")

fun setUpdateHandler(client: JsAny, handler: (JsAny) -> Unit): Unit = js("client.onUpdate = handler")

fun createTdOptions(apiId: Int, apiHash: String): JsAny = js("""
    ({
        instanceName: 'tvcine_web',
        readOnly: false,
        isBackground: false,
        logVerbosityLevel: 1,
        jsLogVerbosityLevel: 1,
        useDatabase: true
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
fun hideLoadingStatus(): Unit = js("document.getElementById('status').style.display = 'none'")

fun hideIntro(): Unit = js("window.hideIntro()")

fun loadGroupVideos(client: JsAny, inviteLink: String, limit: Int, handler: (JsAny) -> Unit): Unit = js("""
    (function(client, inviteLink, limit, handler) {
        function safeSend(query) {
            try {
                var result = client.send(query);
                if (result && typeof result.then === 'function') {
                    return result;
                }
                return Promise.resolve(result);
            } catch (e) {
                return Promise.reject(e);
            }
        }

        function loadHistory(chatId) {
            return safeSend({
                '@type': 'getChatHistory',
                chat_id: chatId,
                from_message_id: 0,
                offset: 0,
                limit: limit,
                only_local: false
            }).then(function(history) {
                var messages = history && history.messages ? history.messages : [];
                var videos = messages
                    .filter(function(message) {
                        return message && message.content && message.content['@type'] === 'messageVideo';
                    })
                    .map(function(message) {
                        var video = message.content.video || {};
                        var file = video.video || {};
                        var thumbnail = video.thumbnail || {};
                        var thumbnailFile = thumbnail.file || {};
                        return {
                            title: (video.file_name || message.content.caption && message.content.caption.text || 'Tema sin titulo'),
                            genre: 'Telegram',
                            posterUrl: thumbnailFile.local && thumbnailFile.local.path ? thumbnailFile.local.path : '',
                            fileId: file.id || 0
                        };
                    });
                handler({ '@type': 'loadedGroupVideos', videos: videos });
            });
        }

        safeSend({ '@type': 'checkChatInviteLink', invite_link: inviteLink })
            .then(function(info) {
                if (info && info.chat_id) {
                    return loadHistory(info.chat_id);
                }
                return safeSend({ '@type': 'joinChatByInviteLink', invite_link: inviteLink })
                    .then(function(chat) {
                        if (chat && chat.id) {
                            return loadHistory(chat.id);
                        }
                        throw new Error('No se pudo abrir el grupo');
                    });
            })
            .catch(function(error) {
                handler({
                    '@type': 'loadGroupVideosError',
                    message: error && error.message ? error.message : String(error)
                });
            });
    })(client, inviteLink, limit, handler)
""")

fun videosCount(result: JsAny?): Int = js("""
    (result && result.videos && result.videos.length) ? result.videos.length : 0
""")

fun videoTitle(result: JsAny?, index: Int): String = js("""
    (result && result.videos && result.videos[index] && result.videos[index].title) ? result.videos[index].title : 'Tema sin titulo'
""")

fun videoGenre(result: JsAny?, index: Int): String = js("""
    (result && result.videos && result.videos[index] && result.videos[index].genre) ? result.videos[index].genre : 'Telegram'
""")

fun videoPosterUrl(result: JsAny?, index: Int): String = js("""
    (result && result.videos && result.videos[index] && result.videos[index].posterUrl) ? result.videos[index].posterUrl : ''
""")

fun getErrorMessage(result: JsAny?): String = js("""
    (result && result.message) ? result.message : 'No se pudieron cargar los temas'
""")
