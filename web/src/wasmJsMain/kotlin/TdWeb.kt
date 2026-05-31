import kotlin.js.JsAny

@JsModule("tdweb")
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

// Versión simplificada para cumplir con las restricciones de Kotlin/Wasm
fun createBaseQuery(type: String): JsAny = js("""({ '@type': type })""")

// Para añadir parámetros, usaremos una función auxiliar en JS
fun addParamToQuery(query: JsAny, key: String, value: String): JsAny = js("""
    (function(q, k, v) {
        q[k] = v;
        return q;
    })(query, key, value)
""")
