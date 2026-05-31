import kotlin.js.JsAny

external class TdClient(options: JsAny) : JsAny {
    fun send(query: JsAny): JsAny
    var onUpdate: (JsAny) -> Unit
}

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

fun getTdType(obj: JsAny): String = js("obj['@type']")
fun getAuthState(update: JsAny): String = js("update.authorization_state['@type']")
