import kotlin.js.JsAny

// Usamos funciones globales para evitar problemas con constructores externos en Wasm
fun createTdClient(options: JsAny): JsAny = js("new window.tdweb(options)")

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

fun getTdType(obj: JsAny): String = js("obj && obj['@type'] ? obj['@type'] : ''")
fun getAuthState(update: JsAny): String = js("update && update.authorization_state && update.authorization_state['@type'] ? update.authorization_state['@type'] : ''")
