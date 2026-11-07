package cc.cd.ffurry.ai.xinfan

import android.content.ContentValues
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** 后端 js / Worker 部署地址。APP 内不存放任何 token。 */
const val BASE = "https://ai.ffurry.cc.cd"
const val HOST = "ai.ffurry.cc.cd"

data class Me(
    val id: String = "",
    val email: String = "",
    val username: String = "用户",
    val points: Double = 0.0,
    val status: String = "approved",
    val inviteCode: String = "",
    val avatarColor: String = "#007aff"
)

data class ModelInfo(
    val id: String,
    val name: String,
    val desc: String,
    val multiplier: Double,
    val enabled: Boolean,
    val limitMsg: String = "",
    val supportsVision: Boolean = false
)

data class ChatItem(val id: String, val title: String, val mode: String, val model: String = "")
data class Notice(val id: String, val title: String, val content: String, val createdAt: Long, val isRead: Boolean)
data class PointLog(val amount: Double, val action: String, val createdAt: Long)
data class ProjectFiles(val name: String, val files: List<Pair<String, String>>)

/** 发给 /api/chat 的上下文消息（与界面消息分开维护）。 */
data class MsgPayload(
    val role: String,
    val content: String,
    val attachments: List<String> = emptyList()
)

/** 统一响应：code = -1 表示网络层异常，body 里是错误文案。 */
data class Resp(val code: Int, val body: String)

fun jsonOf(vararg pairs: Pair<String, Any?>): String {
    val o = JSONObject()
    for ((k, v) in pairs) {
        when (v) {
            null -> o.put(k, JSONObject.NULL)
            is JSONArray -> o.put(k, v)
            is JSONObject -> o.put(k, v)
            else -> o.put(k, v)
        }
    }
    return o.toString()
}

fun messagesJson(list: List<MsgPayload>): JSONArray {
    val arr = JSONArray()
    for (m in list) {
        val o = JSONObject()
        o.put("role", m.role)
        o.put("content", m.content)
        val a = JSONArray()
        m.attachments.forEach { a.put(it) }
        o.put("attachments", a)
        arr.put(o)
    }
    return arr
}

class Api(ctx: Context) {

    val prefs: SharedPreferences = ctx.getSharedPreferences("xinfan", Context.MODE_PRIVATE)
    private val appCtx = ctx.applicationContext

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)   // SSE 长连接不超时
        .writeTimeout(60, TimeUnit.SECONDS)
        .cookieJar(object : CookieJar {
            override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
                for (c in cookies) {
                    if (c.name == "auth_token") {
                        prefs.edit()
                            .putString("auth_token", c.value)
                            .apply()
                    }
                }
            }

            override fun loadForRequest(url: HttpUrl): List<Cookie> {
                val v = prefs.getString("auth_token", "").orEmpty()
                if (v.isEmpty()) return emptyList()
                return listOf(
                    Cookie.Builder()
                        .name("auth_token")
                        .value(v)
                        .domain(HOST)
                        .path("/")
                        .build()
                )
            }
        })
        .build()

    private fun buildRequest(path: String, method: String = "GET", json: String? = null): Request {
        val b = Request.Builder().url(BASE + path).header("Cache-Control", "no-cache")
        if (method == "POST") {
            b.post((json ?: "{}").toRequestBody("application/json; charset=utf-8".toMediaType()))
        }
        return b.build()
    }

    /** 普通 JSON 接口。 */
    suspend fun call(path: String, method: String = "GET", json: String? = null): Resp =
        withContext(Dispatchers.IO) {
            try {
                val r = client.newCall(buildRequest(path, method, json)).execute()
                val body = r.body?.string().orEmpty()
                r.close()
                Resp(r.code, body)
            } catch (e: IOException) {
                Resp(-1, e.message ?: "网络错误")
            }
        }

    /** 拿到裸 Call，供 /api/chat 的 SSE 流式读取。 */
    fun chatCall(json: String): Call = client.newCall(buildRequest("/api/chat", "POST", json))

    /** 图片上传（multipart/form-data，字段名 image）。 */
    suspend fun uploadImage(bytes: ByteArray, name: String, mime: String): Resp =
        withContext(Dispatchers.IO) {
            try {
                val body = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("image", name, bytes.toRequestBody(mime.toMediaType()))
                    .build()
                val req = Request.Builder().url(BASE + "/api/upload_image").post(body).build()
                val r = client.newCall(req).execute()
                val text = r.body?.string().orEmpty()
                r.close()
                Resp(r.code, text)
            } catch (e: IOException) {
                Resp(-1, e.message ?: "上传失败")
            }
        }

    /** 拉取图片字节（带 Cookie），用于消息里的附件 / 生成图。 */
    suspend fun bytes(urlOrPath: String): ByteArray? = withContext(Dispatchers.IO) {
        try {
            val url = if (urlOrPath.startsWith("http")) urlOrPath else BASE + urlOrPath
            val r = client.newCall(Request.Builder().url(url).build()).execute()
            val b = r.body?.bytes()
            r.close()
            b
        } catch (e: Exception) {
            null
        }
    }

    /** 下载 Jar 到系统下载目录。返回是否成功。 */
    suspend fun downloadToDownloads(urlOrPath: String, fileName: String): Boolean =
        withContext(Dispatchers.IO) {
            val data = bytes(urlOrPath) ?: return@withContext false
            saveToDownloads(fileName, "application/java-archive", data)
        }

    /** 把 project_files 事件里的文件打成 zip 保存到下载目录。 */
    suspend fun saveProjectZip(project: ProjectFiles): Boolean = withContext(Dispatchers.IO) {
        try {
            val bos = ByteArrayOutputStream()
            ZipOutputStream(bos).use { z ->
                for ((path, content) in project.files) {
                    z.putNextEntry(ZipEntry(path))
                    z.write(content.toByteArray(Charsets.UTF_8))
                    z.closeEntry()
                }
            }
            saveToDownloads("${project.name}.zip", "application/zip", bos.toByteArray())
        } catch (e: Exception) {
            false
        }
    }

    private fun saveToDownloads(name: String, mime: String, data: ByteArray): Boolean {
        val safe = name.replace('/', '_').replace('\\', '_').ifEmpty { "download.bin" }
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val cv = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, safe)
                    put(MediaStore.Downloads.MIME_TYPE, mime)
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
                val uri = appCtx.contentResolver
                    .insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv) ?: return false
                appCtx.contentResolver.openOutputStream(uri)?.use { it.write(data) }
                val done = ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }
                appCtx.contentResolver.update(uri, done, null, null)
            } else {
                val dir = appCtx.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                    ?: return false
                FileOutputStream(File(dir, safe)).use { it.write(data) }
            }
            true
        } catch (e: Exception) {
            false
        }
    }
}
