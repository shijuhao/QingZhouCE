package com.example.toolbox.function.yunhu.yhbotmaker.runtime

import com.example.toolbox.AppJson
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.luaj.vm2.*
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.VarArgFunction
import org.luaj.vm2.lib.jse.*

class LuaEngine(
    token: String,
    private val chatId: String,
    private val chatType: String,
    private val onPrint: (message: String, type: Int) -> Unit
) {
    private val httpClient = OkHttpClient()
    private val globals: Globals
    private val apiService = YunHuApiService(token)

    init {
        onPrint("LuaEngine 开始初始化", 5)

        globals = JsePlatform.standardGlobals()

        try {
            globals.load("""
                -- 基础库已经由 JsePlatform.standardGlobals() 加载
                -- 这里不需要额外操作
            """.trimIndent()).call()
            onPrint("基础库加载完成", 5)
        } catch (e: Exception) {
            onPrint("基础库加载失败: ${e.message}", 2)
        }

        registerFunctions()
    }

    private fun registerFunctions() {
        // print 函数
        globals.set("print", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val sb = StringBuilder()
                for (i in 1..args.narg()) {
                    if (i > 1) sb.append("\t")
                    sb.append(args.arg(i).tojstring())
                }
                var msgType = 0
                if (args.narg() >= 2) {
                    val typeArg = args.arg(2)
                    if (typeArg.isnumber()) {
                        msgType = typeArg.toint()
                    }
                }
                onPrint(sb.toString(), msgType)
                return NIL
            }
        })

        // sendText
        globals.set("sendText", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue {
                val content = arg.tojstring()
                apiService.sendMessage(
                    recvId = chatId,
                    recvType = chatType,
                    contentType = "text",
                    content = content,
                    onSuccess = { _, _ ->
                        onPrint("✅ 消息发送成功: $content", 1)
                    },
                    onError = { err ->
                        onPrint("❌ 发送失败: $err", 2)
                    }
                )
                return NIL
            }
        })

        // sendMarkdown
        globals.set("sendMarkdown", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue {
                val content = arg.tojstring()
                apiService.sendMessage(
                    recvId = chatId,
                    recvType = chatType,
                    contentType = "markdown",
                    content = content,
                    onSuccess = { _, _ ->
                        onPrint("✅ MD发送成功", 1)
                    },
                    onError = { err ->
                        onPrint("❌ MD发送失败: $err", 2)
                    }
                )
                return NIL
            }
        })

        // sendHTML
        globals.set("sendHTML", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue {
                val content = arg.tojstring()
                apiService.sendMessage(
                    recvId = chatId,
                    recvType = chatType,
                    contentType = "html",
                    content = content,
                    onSuccess = { _, _ ->
                        onPrint("✅ HTML发送成功", 1)
                    },
                    onError = { err ->
                        onPrint("❌ HTML发送失败: $err", 2)
                    }
                )
                return NIL
            }
        })

        // recallMessage
        globals.set("recallMessage", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue {
                val msgId = arg.tojstring()
                apiService.recallMessage(
                    chatId = chatId,
                    chatType = chatType,
                    msgId = msgId,
                    onSuccess = { _, _ ->
                        onPrint("✅ 撤回成功: $msgId", 1)
                    },
                    onError = { err ->
                        onPrint("❌ 撤回失败: $err", 2)
                    }
                )
                return NIL
            }
        })

        fun parseHeaders(json: String?): Map<String, String> {
            if (json.isNullOrBlank()) return emptyMap()
            return try {
                AppJson.json.decodeFromString<Map<String, String>>(json)
            } catch (_: Exception) {
                emptyMap()
            }
        }

        val requestFunction = object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val n = args.narg()
                if (n < 2) error("至少需要 method 和 url")
                val method = args.arg(1).tojstring().uppercase()
                val url = args.arg(2).tojstring()
                var data: String? = null
                var headersJson: String? = null
                var contentType: String? = null

                var index = 3
                while (index <= n) {
                    val arg = args.arg(index)
                    when {
                        arg.isstring() -> {
                            if (data == null) data = arg.tojstring()
                            else if (headersJson == null) headersJson = arg.tojstring()
                            else if (contentType == null) contentType = arg.tojstring()
                            else error("参数过多")
                        }
                        else -> error("参数类型错误，只支持字符串")
                    }
                    index++
                }

                val headers = parseHeaders(headersJson)
                val requestBuilder = Request.Builder().url(url)
                headers.forEach { (k, v) -> requestBuilder.addHeader(k, v) }

                if (method == "GET" || method == "DELETE") {
                    requestBuilder.method(method, null)
                } else {
                    val mediaType = contentType?.toMediaType() ?: "application/json".toMediaType()
                    val body = (data ?: "").toRequestBody(mediaType)
                    requestBuilder.method(method, body)
                }

                return try {
                    val response = httpClient.newCall(requestBuilder.build()).execute()
                    response.use {
                        if (it.isSuccessful) {
                            valueOf(it.body.string())
                        } else {
                            error("HTTP ${it.code}: ${it.message}")
                        }
                    }
                } catch (e: Exception) {
                    error(e.message ?: "HTTP 请求失败")
                }
            }
        }

        // 创建 http 表
        val httpTable = LuaTable()

        // GET
        httpTable.set("get", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val n = args.narg()
                if (n < 1) error("缺少 url")
                val url = args.arg(1).tojstring()
                val headersJson = if (n >= 2) args.arg(2).tojstring() else ""
                return requestFunction.invoke(
                    varargsOf(
                        valueOf("GET"),
                        valueOf(url),
                        valueOf(headersJson)
                    )
                )
            }
        })

        // POST
        httpTable.set("post", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val n = args.narg()
                if (n < 2) error("缺少 url 和 data")
                val url = args.arg(1).tojstring()
                val data = args.arg(2).tojstring()
                val headersJson = if (n >= 3) args.arg(3).tojstring() else ""
                val contentType = if (n >= 4) args.arg(4).tojstring() else null
                val params = mutableListOf<LuaValue>(
                    valueOf("POST"),
                    valueOf(url),
                    valueOf(data),
                    valueOf(headersJson)
                )
                contentType?.let { params.add(valueOf(it)) }
                return requestFunction.invoke(varargsOf(params.toTypedArray()))
            }
        })

        // PUT
        httpTable.set("put", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val n = args.narg()
                if (n < 2) error("缺少 url 和 data")
                val url = args.arg(1).tojstring()
                val data = args.arg(2).tojstring()
                val headersJson = if (n >= 3) args.arg(3).tojstring() else ""
                val contentType = if (n >= 4) args.arg(4).tojstring() else null
                val params = mutableListOf<LuaValue>(
                    valueOf("PUT"),
                    valueOf(url),
                    valueOf(data),
                    valueOf(headersJson)
                )
                contentType?.let { params.add(valueOf(it)) }
                return requestFunction.invoke(varargsOf(params.toTypedArray()))
            }
        })

        // DELETE
        httpTable.set("delete", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val n = args.narg()
                if (n < 1) error("缺少 url")
                val url = args.arg(1).tojstring()
                val headersJson = if (n >= 2) args.arg(2).tojstring() else ""
                return requestFunction.invoke(
                    varargsOf(
                        valueOf("DELETE"),
                        valueOf(url),
                        valueOf(headersJson)
                    )
                )
            }
        })

        globals.set("http", httpTable)
    }

    fun runStartupCode(code: String): Boolean {
        return try {
            globals.load(code).call()
            true
        } catch (e: Exception) {
            onPrint("❌ 启动代码错误: ${e.message}", 2)
            false
        }
    }

    fun runLoopCode(code: String, callback: Map<String, Any>): Boolean {
        return try {
            val luaTable = LuaTable()

            fun convertToLuaValue(value: Any?): LuaValue {
                return when (value) {
                    null -> LuaValue.NIL
                    is String -> LuaValue.valueOf(value)
                    is Int -> LuaValue.valueOf(value)
                    is Long -> LuaValue.valueOf(value.toInt())
                    is Double -> LuaValue.valueOf(value)
                    is Boolean -> LuaValue.valueOf(value)
                    is Map<*, *> -> {
                        val table = LuaTable()
                        (value as Map<String, Any>).forEach { (k, v) ->
                            table.set(k, convertToLuaValue(v))
                        }
                        table
                    }
                    is List<*> -> {
                        val table = LuaTable()
                        value.forEachIndexed { index, item ->
                            table.set(index + 1, convertToLuaValue(item))
                        }
                        table
                    }
                    else -> LuaValue.valueOf(value.toString())
                }
            }

            callback.forEach { (k, v) ->
                luaTable.set(k, convertToLuaValue(v))
            }

            globals.set("callback", luaTable)
            globals.load(code).call()
            true
        } catch (e: Exception) {
            onPrint("❌ 循环代码错误: ${e.message}", 2)
            false
        }
    }
}