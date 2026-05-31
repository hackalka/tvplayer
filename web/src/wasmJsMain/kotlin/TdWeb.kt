import kotlin.js.JsAny

@JsModule("tdweb")
external class TdClient(options: JsAny) : JsAny {
    fun send(query: JsAny): JsAny // Devuelve una Promise
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

fun createQuery(type: String, params: JsAny? = null): JsAny = js("""
    (function(type, params) {
        var query = { '@type': type };
        if (params) {
            for (var key in params) {
                query[key] = params[key];
            }
        }
        return query;
    })
""")(type, params)
