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

fun getTdType(obj: JsAny?): String = js("window.getTdType(obj)")
fun getAuthState(update: JsAny?): String = js("window.getAuthState(update)")
fun hideLoadingStatus(): Unit = js("document.getElementById('status').style.display = 'none'")
