package cc.cd.ffurry.ai.xinfan

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder
import java.util.UUID

data class PendingImage(val id: String, val bitmap: ImageBitmap)

data class Message(
    val role: String,
    val content: String = "",
    val attachments: List<String> = emptyList(),
    val localBitmaps: List<ImageBitmap> = emptyList(),
    val reasoning: String? = null,
    val error: String? = null,
    val cost: Double? = null,
    val remaining: Double? = null,
    val buildTaskId: String? = null,
    val buildState: String? = null,
    val project: ProjectFiles? = null
)

data class StreamState(
    val active: Boolean = false,
    val text: String = "",
    val reasoning: String = "",
    val status: String = "正在请求...",
    val seconds: Int = 0,
    val error: String? = null,
    val cost: Double? = null,
    val remaining: Double? = null,
    val buildTaskId: String? = null,
    val project: ProjectFiles? = null
)

private class StreamAcc {
    var fullText = ""
    var pendingText = ""
    var reasoning = ""
    var error: String? = null
    var cost: Double? = null
    var remaining: Double? = null
    var buildTaskId: String? = null
    var project: ProjectFiles? = null
    var lastRender = 0L
}

class AppViewModel(app: Application) : AndroidViewModel(app) {

    val api = Api(app)
    private val p get() = api.prefs

    // ---------- 导航 ----------
    var screen by mutableStateOf("boot")          // boot / login / register / pending / main
    var page by mutableStateOf("chat")            // chat / notice / points / profile
    var drawerOpen by mutableStateOf(false)
    var dialog by mutableStateOf<String?>(null)   // delete / stop / sysNotice
    var toast by mutableStateOf<String?>(null)
    var sysTitle by mutableStateOf("")
    var sysContent by mutableStateOf("")
    var maintenance by mutableStateOf(false)
    var allowRegister by mutableStateOf(true)

    // ---------- 用户 / 模型 ----------
    var me by mutableStateOf(Me())
    var models by mutableStateOf<List<ModelInfo>>(emptyList())
    var modelsLoading by mutableStateOf(false)
    var selectedModel by mutableStateOf("")
    var modelFoldOpen by mutableStateOf(false)
    var mode by mutableStateOf(p.getString("last_mode", "chat") ?: "chat")
    var enableThinking by mutableStateOf(p.getBoolean("enable_thinking", false))
    var enableSearch by mutableStateOf(p.getBoolean("enable_search", false))

    // ---------- 会话 ----------
    var chats by mutableStateOf<List<ChatItem>>(emptyList())
    var currentChatId by mutableStateOf<String?>(null)
    val messages = mutableStateListOf<Message>()
    private val chatContext = mutableListOf<MsgPayload>()
    val pendingImages = mutableStateListOf<PendingImage>()
    var inputText by mutableStateOf("")
    var isBusy by mutableStateOf(false)
    var stream by mutableStateOf(StreamState())
    private var chatCall: Call? = null
    private var stopped = false
    var deletingChatId by mutableStateOf<String?>(null)

    // ---------- 通知 / 积分 ----------
    var notices by mutableStateOf<List<Notice>>(emptyList())
    var pointLogs by mutableStateOf<List<PointLog>>(emptyList())
    var noticeDot by mutableStateOf(false)

    // ---------- 登录 / 注册表单 ----------
    var loginType by mutableStateOf("password")
    var loginAccount by mutableStateOf("")
    var loginPassword by mutableStateOf("")
    var loginCode by mutableStateOf("")
    var loginError by mutableStateOf("")
    var loginBusy by mutableStateOf(false)
    var regUsername by mutableStateOf("")
    var regEmail by mutableStateOf("")
    var regCode by mutableStateOf("")
    var regPassword by mutableStateOf("")
    var regConfirm by mutableStateOf("")
    var regAnswer by mutableStateOf("")
    var regInvite by mutableStateOf("")
    var regError by mutableStateOf("")
    var regBusy by mutableStateOf(false)
    var codeCooldown by mutableStateOf(0)

    // ---------- 个人中心表单 ----------
    var newUsername by mutableStateOf("")
    var pwdCode by mutableStateOf("")
    var newPwd by mutableStateOf("")
    var newEmail by mutableStateOf("")
    var emailCode by mutableStateOf("")
    var redeemCode by mutableStateOf("")

    // ================== 通用 ==================

    fun showToast(msg: String) {
        viewModelScope.launch {
            toast = msg
            delay(2500)
            if (toast == msg) toast = null
        }
    }

    private suspend fun onMain(block: () -> Unit) {
        withContext(Dispatchers.Main.immediate) { block() }
    }

    private fun errText(r: Resp, fallback: String): String {
        if (r.code == -1) return r.body.ifEmpty { fallback }
        return try {
            val o = JSONObject(r.body)
            if (o.has("error")) o.getString("error") else fallback
        } catch (e: Exception) {
            fallback
        }
    }

    fun boot() {
        viewModelScope.launch {
            val s = api.call("/api/settings")
            try {
                val o = JSONObject(s.body)
                maintenance = o.optString("maintenance") == "1"
                allowRegister = o.optString("allow_register", "1") != "0"
            } catch (e: Exception) { /* 用默认值 */ }
            if (maintenance) {
                onMain { showToast("网站正在维护中，请稍后访问，谢谢!"); screen = "login" }
                return@launch
            }
            checkMe()
        }
    }

    private suspend fun checkMe() {
        val r = api.call("/api/user/me")
        if (r.code == 401) { onMain { screen = "login" }; return }
        if (r.code != 200) {
            onMain { showToast(errText(r, "加载失败")); screen = "login" }
            return
        }
        val o = try { JSONObject(r.body) } catch (e: Exception) { JSONObject() }
        val status = o.optString("status", "approved")
        if (status == "pending") { onMain { screen = "pending" }; return }
        if (status == "banned") { onMain { screen = "login" }; return }
        onMain {
            me = Me(
                id = o.optString("id"),
                email = o.optString("email"),
                username = o.optString("username", "用户"),
                points = o.optDouble("points", 0.0),
                status = status,
                inviteCode = o.optString("inviteCode"),
                avatarColor = o.optString("avatarColor", "#007aff")
            )
            screen = "main"
            loadModels()
            loadChats()
            loadPopup()
            loadNoticeDot()
        }
    }

    fun gotoLogin() { screen = "login" }
    fun gotoRegister() { screen = "register" }
    fun refreshPending() { viewModelScope.launch { checkMe() } }

    fun clearPending() {
        viewModelScope.launch {
            api.call("/api/auth/clear_pending", "POST")
            screen = "login"
        }
    }

    fun logout() {
        viewModelScope.launch {
            api.call("/api/auth/logout", "POST")
            p.edit().remove("auth_token").apply()
            onMain {
                screen = "login"
                me = Me()
                messages.clear()
                chatContext.clear()
                chats = emptyList()
                currentChatId = null
            }
        }
    }

    // ================== 认证 ==================

    fun doLogin() {
        val acc = loginAccount.trim()
        if (acc.isEmpty()) { loginError = "请输入账号或邮箱"; return }
        if (loginType == "code" && !acc.contains("@")) { loginError = "验证码登录仅支持邮箱"; return }
        loginBusy = true
        loginError = ""
        val payload = jsonOf(
            "account" to acc,
            "password" to loginPassword,
            "code" to loginCode,
            "loginType" to loginType
        )
        viewModelScope.launch {
            val r = api.call("/api/auth/login", "POST", payload)
            loginBusy = false
            if (r.code != 200) { loginError = errText(r, "登录失败"); return@launch }
            val o = try { JSONObject(r.body) } catch (e: Exception) { JSONObject() }
            if (o.optString("status") == "pending") { screen = "pending"; return@launch }
            checkMe()
        }
    }

    fun doRegister() {
        if (regUsername.length < 2) { regError = "用户名至少 2 位"; return }
        if (!regEmail.contains("@")) { regError = "邮箱格式错误"; return }
        if (regCode.isBlank()) { regError = "请填写验证码"; return }
        if (regPassword.length < 6) { regError = "密码至少 6 位"; return }
        if (regPassword != regConfirm) { regError = "两次输入的密码不一致"; return }
        if (regAnswer.length < 15 || regAnswer.length > 200) { regError = "问题回答需在 15 到 200 字之间"; return }
        if (!answerValid(regAnswer)) { regError = "检测到可能的乱码或无意义输入，请认真填写"; return }
        regBusy = true
        regError = ""
        val payload = jsonOf(
            "email" to regEmail.trim(),
            "code" to regCode.trim(),
            "username" to regUsername.trim(),
            "password" to regPassword,
            "answer" to regAnswer.trim(),
            "inviteCode" to regInvite.trim()
        )
        viewModelScope.launch {
            val r = api.call("/api/auth/register", "POST", payload)
            regBusy = false
            if (r.code != 200) { regError = errText(r, "注册失败"); return@launch }
            screen = "pending"
        }
    }

    private fun answerValid(s: String): Boolean {
        if (Regex("(.)\\1{3,}").containsMatchIn(s)) return false
        if (Regex("qwer|asdf|zxcv|1234|aaaa|bbbb", RegexOption.IGNORE_CASE).containsMatchIn(s)) return false
        val letters = s.replace(Regex("[^a-zA-Z]"), "")
        if (letters.length > 10 && !Regex("[aeiouAEIOU]").containsMatchIn(letters)) return false
        return true
    }

    /** 验证码：登录 / 注册 / 改密 / 改邮箱共用。 */
    fun sendCode(email: String, purpose: String) {
        if (!email.contains("@")) { showToast("请输入正确的邮箱"); return }
        if (codeCooldown > 0) { showToast("请等待 ${codeCooldown}s 后再试"); return }
        viewModelScope.launch {
            if (purpose == "register" || purpose == "login") {
                val action = if (purpose == "register") "register" else "login"
                val chk = api.call("/api/auth/check_email", "POST",
                    jsonOf("email" to email, "action" to action))
                if (chk.code != 200 || (try { JSONObject(chk.body).has("error") } catch (e: Exception) { false })) {
                    showToast(errText(chk, "邮箱检查失败")); return@launch
                }
            }
            val r = api.call("/api/auth/send_code", "POST",
                jsonOf("email" to email, "purpose" to purpose))
            if (r.code == 200) {
                showToast("验证码已发送，请查收")
                codeCooldown = 60
                while (codeCooldown > 0) { delay(1000); codeCooldown-- }
            } else showToast(errText(r, "发送失败"))
        }
    }

    fun sendProfileCode(purpose: String) {
        val email = if (purpose == "change_password") me.email else newEmail.trim()
        sendCode(email, purpose)
    }

    // ================== 模型 / 模式 ==================

    fun loadModels() {
        viewModelScope.launch {
            modelsLoading = true
            val r = api.call("/api/models?mode=$mode&t=${System.currentTimeMillis()}")
            modelsLoading = false
            val o = try { JSONObject(r.body) } catch (e: Exception) { JSONObject() }
            val arr = o.optJSONArray("models") ?: JSONArray()
            val list = mutableListOf<ModelInfo>()
            for (i in 0 until arr.length()) {
                val m = arr.optJSONObject(i) ?: continue
                list.add(
                    ModelInfo(
                        id = m.optString("id"),
                        name = m.optString("name"),
                        desc = m.optString("desc"),
                        multiplier = m.optDouble("multiplier", 1.0),
                        enabled = m.optBoolean("enabled", true),
                        limitMsg = m.optString("limitMsg"),
                        supportsVision = m.optBoolean("supportsVision", false)
                    )
                )
            }
            models = list
            if (list.isEmpty()) { selectedModel = ""; return@launch }
            val saved = p.getString("last_model_$mode", "").orEmpty()
            selectedModel = when {
                list.any { it.id == selectedModel && it.enabled } -> selectedModel
                saved.isNotEmpty() && list.any { it.id == saved && it.enabled } -> saved
                else -> list.firstOrNull { it.enabled }?.id.orEmpty()
            }
        }
    }

    fun selectModel(id: String) {
        val m = models.firstOrNull { it.id == id } ?: return
        if (!m.enabled) return
        selectedModel = id
        p.edit().putString("last_model_$mode", id).apply()
        modelFoldOpen = false
    }

    fun toggleModelFold() { if (!modelFoldOpen) loadModels(); modelFoldOpen = !modelFoldOpen }
    fun toggleDrawer() { drawerOpen = !drawerOpen }
    fun closeDrawer() { drawerOpen = false }

    fun toggleThink() {
        enableThinking = !enableThinking
        p.edit().putBoolean("enable_thinking", enableThinking).apply()
        showToast(if (enableThinking) "思考模式已开启" else "思考模式已关闭")
    }

    fun toggleSearch() {
        enableSearch = !enableSearch
        p.edit().putBoolean("enable_search", enableSearch).apply()
        showToast(if (enableSearch) "联网搜索已开启" else "联网搜索已关闭")
    }

    fun switchMode(m: String) {
        if (isBusy) { showToast("请先停止当前生成"); return }
        if (mode == m) { drawerOpen = false; return }
        mode = m
        p.edit().putString("last_mode", m).apply()
        newChat()
        drawerOpen = false
        loadModels()
    }

    fun switchPage(pg: String) {
        page = pg
        drawerOpen = false
        modelFoldOpen = false
        when (pg) {
            "notice" -> { noticeDot = false; loadNotices() }
            "points" -> loadPointsLogs()
        }
    }

    // ================== 会话 ==================

    fun loadChats() {
        viewModelScope.launch {
            val r = api.call("/api/chats")
            val o = try { JSONObject(r.body) } catch (e: Exception) { JSONObject() }
            val arr = o.optJSONArray("chats") ?: JSONArray()
            val list = mutableListOf<ChatItem>()
            for (i in 0 until arr.length()) {
                val c = arr.optJSONObject(i) ?: continue
                list.add(ChatItem(c.optString("id"), c.optString("title", "新对话"), c.optString("mode", "chat"), c.optString("model")))
            }
            chats = list
        }
    }

    fun newChat() {
        if (isBusy) { showToast("请先停止当前生成"); return }
        currentChatId = null
        messages.clear()
        chatContext.clear()
        pendingImages.clear()
        inputText = ""
        models.firstOrNull { it.enabled }?.let { selectModel(it.id) }
        drawerOpen = false
    }

    fun openChat(id: String) {
        if (isBusy) { showToast("当前有对话未完成，请先停止"); return }
        viewModelScope.launch {
            val r = api.call("/api/chat/messages?chatId=" + URLEncoder.encode(id, "UTF-8"))
            if (r.code != 200) { showToast(errText(r, "加载失败")); return@launch }
            val o = JSONObject(r.body)
            currentChatId = id
            messages.clear()
            chatContext.clear()
            val arr = o.optJSONArray("messages") ?: JSONArray()
            val buildRe = Regex("<!-- BUILD_TASK:(.*?) -->")
            for (i in 0 until arr.length()) {
                val m = arr.optJSONObject(i) ?: continue
                val role = m.optString("role")
                val content = m.optString("content")
                val at = m.optJSONArray("attachments")
                val ids = mutableListOf<String>()
                if (at != null) for (j in 0 until at.length()) ids.add(at.optString(j))
                val taskId = buildRe.find(content)?.groupValues?.getOrNull(1)
                val clean = content.replace(Regex("<!-- BUILD_TASK:.*? -->"), "")
                messages.add(
                    Message(
                        role = role, content = clean, attachments = ids,
                        buildTaskId = taskId,
                        buildState = if (taskId != null) "checking" else null
                    )
                )
                chatContext.add(MsgPayload(role, content, ids))
                if (taskId != null) pollBuild(taskId)
            }
            val m = o.optString("mode", "")
            if (m.isNotEmpty() && m != mode) {
                mode = m
                p.edit().putString("last_mode", m).apply()
                loadModels()
            }
            val md = o.optString("model", "")
            if (md.isNotEmpty() && models.any { it.id == md }) selectedModel = md
            page = "chat"
            drawerOpen = false
        }
    }

    fun requestDelete(id: String) { deletingChatId = id; dialog = "delete" }

    fun confirmDelete() {
        val id = deletingChatId ?: return
        dialog = null
        viewModelScope.launch {
            val r = api.call("/api/chat/delete", "POST", jsonOf("chatId" to id))
            if (r.code == 200) {
                showToast("已删除")
                if (currentChatId == id) newChat()
                loadChats()
            } else showToast(errText(r, "删除失败"))
            deletingChatId = null
        }
    }

    // ================== 图片上传 ==================

    fun onImagesPicked(uris: List<Uri>) {
        viewModelScope.launch {
            if (pendingImages.size + uris.size > 5) { showToast("最多只能上传 5 张图片"); return@launch }
            for (u in uris) {
                val bytes = withContext(Dispatchers.IO) {
                    getApplication<Application>().contentResolver.openInputStream(u)?.use { it.readBytes() }
                } ?: continue
                if (bytes.size > 5 * 1024 * 1024) { showToast("图片超过 5MB 限制"); continue }
                val mime = getApplication<Application>().contentResolver.getType(u) ?: "image/png"
                if (mime != "image/png" && mime != "image/jpeg") { showToast("格式不支持（仅 png/jpg）"); continue }
                showToast("正在上传图片...")
                val r = api.uploadImage(bytes, "image.${if (mime == "image/png") "png" else "jpg"}", mime)
                val o = try { JSONObject(r.body) } catch (e: Exception) { JSONObject() }
                if (r.code == 200 && o.optBoolean("success")) {
                    val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                    if (bmp != null) pendingImages.add(PendingImage(o.optString("id"), bmp))
                    showToast("上传成功")
                } else showToast(o.optString("error", "上传失败"))
            }
        }
    }

    fun removeImage(index: Int) {
        if (index in pendingImages.indices) pendingImages.removeAt(index)
    }

    // ================== 聊天 SSE ==================

    fun handleSendOrStop() {
        if (isBusy) dialog = "stop" else send()
    }

    fun confirmStop() {
        dialog = null
        stopped = true
        chatCall?.cancel()
    }

    fun send(isRetry: Boolean = false) {
        if (isBusy) { showToast("当前有对话未完成，请点击停止或等待完成"); return }
        if (selectedModel.isEmpty()) { showToast("请先选择模型"); return }

        if (!isRetry) {
            val txt = inputText.trim()
            if (txt.isEmpty() && pendingImages.isEmpty()) return
            if (currentChatId == null) currentChatId = UUID.randomUUID().toString()
            val ids = pendingImages.map { it.id }
            val bmps = pendingImages.map { it.bitmap }
            messages.add(Message(role = "user", content = txt, localBitmaps = bmps))
            chatContext.add(MsgPayload("user", txt, ids))
            inputText = ""
            pendingImages.clear()
        } else {
            val idx = messages.indexOfLast { it.role == "assistant" && it.error != null }
            if (idx >= 0) messages.removeAt(idx)
        }

        isBusy = true
        stopped = false
        stream = StreamState(active = true, status = "正在请求...")
        val payload = jsonOf(
            "messages" to messagesJson(chatContext),
            "modelId" to selectedModel,
            "mode" to mode,
            "chatId" to currentChatId,
            "enableThinking" to enableThinking,
            "useSearch" to enableSearch,
            "isRetry" to isRetry
        )
        val call = api.chatCall(payload)
        chatCall = call
        val acc = StreamAcc()
        val start = System.currentTimeMillis()

        viewModelScope.launch {
            while (isBusy) {
                delay(1000)
                if (isBusy) stream = stream.copy(seconds = ((System.currentTimeMillis() - start) / 1000).toInt())
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            var resp: Response? = null
            try {
                resp = call.execute()
                if (!resp.isSuccessful) {
                    val t = resp.body?.string().orEmpty()
                    val msg = try { JSONObject(t).optString("error") } catch (e: Exception) { "" }
                    throw IOException(if (msg.isNotEmpty()) msg else "HTTP ${resp.code}")
                }
                val reader = resp.body!!.byteStream().bufferedReader(Charsets.UTF_8)
                var ev = "message"
                val dataLines = mutableListOf<String>()
                while (true) {
                    val line = reader.readLine() ?: break
                    when {
                        line.isEmpty() -> {
                            if (dataLines.isNotEmpty()) handleEvent(ev, dataLines.joinToString("\n"), acc)
                            ev = "message"
                            dataLines.clear()
                        }
                        line.startsWith("event:") -> ev = line.removePrefix("event:").trim()
                        line.startsWith("data:") -> dataLines.add(line.removePrefix("data:").trimStart())
                    }
                }
            } catch (e: Exception) {
                if (!stopped && !call.isCanceled()) acc.error = e.message ?: "请求失败"
            } finally {
                try { resp?.close() } catch (e: Exception) { }
            }

            onMain {
                val hasAny = acc.fullText.isNotEmpty() || acc.error != null || acc.cost != null ||
                        acc.buildTaskId != null || acc.project != null
                if (hasAny) {
                    messages.add(
                        Message(
                            role = "assistant",
                            content = acc.fullText,
                            reasoning = acc.reasoning.takeIf { it.isNotEmpty() },
                            error = acc.error ?: if (stopped) "已手动中止回答。" else null,
                            cost = acc.cost,
                            remaining = acc.remaining,
                            buildTaskId = acc.buildTaskId,
                            buildState = if (acc.buildTaskId != null) "building" else null,
                            project = acc.project
                        )
                    )
                    if (acc.fullText.isNotEmpty()) chatContext.add(MsgPayload("assistant", acc.fullText))
                }
                isBusy = false
                stream = StreamState()
                loadChats()
            }
        }
    }

    private suspend fun updateStream(block: (StreamState) -> StreamState) {
        onMain { stream = block(stream) }
    }

    private suspend fun maybeRender(acc: StreamAcc) {
        val now = System.currentTimeMillis()
        if (now - acc.lastRender < 100) return
        acc.lastRender = now
        val txt = acc.pendingText
        val rea = if (acc.reasoning.length > 2000) "...(内容过长，仅显示最新部分)...\n" + acc.reasoning.takeLast(2000) else acc.reasoning
        val status = if (acc.fullText.isEmpty() && rea.isNotEmpty()) "思考中..." else "输出正文中..."
        updateStream { it.copy(text = txt, reasoning = rea, status = status) }
    }

    private suspend fun handleEvent(ev: String, raw: String, acc: StreamAcc) {
        val o = try { JSONObject(raw) } catch (e: Exception) { return }
        when (ev) {
            "status" -> updateStream { it.copy(status = o.optString("text")) }
            "chat_id" -> onMain { loadChats() }
            "reasoning" -> {
                acc.reasoning += o.optString("text")
                maybeRender(acc)
            }
            "delta" -> {
                val t = o.optString("text")
                acc.fullText += t
                acc.pendingText += t
                maybeRender(acc)
            }
            "image_generated" -> {
                val u = o.optString("url")
                val md = "\n\n![生成的图片]($u)\n\n"
                acc.fullText += md
                acc.pendingText += md
                maybeRender(acc)
            }
            "build_start" -> {
                val id = o.optString("taskId")
                acc.buildTaskId = id
                updateStream { it.copy(buildTaskId = id) }
                onMain { pollBuild(id) }
            }
            "project_files" -> {
                val name = o.optString("project_name", "project")
                val arr = o.optJSONArray("files") ?: JSONArray()
                val files = mutableListOf<Pair<String, String>>()
                for (i in 0 until arr.length()) {
                    val f = arr.optJSONObject(i) ?: continue
                    files.add(f.optString("path") to f.optString("content"))
                }
                val pf = ProjectFiles(name, files)
                acc.project = pf
                updateStream { it.copy(project = pf) }
            }
            "billing" -> {
                val c = o.optDouble("cost", 0.0)
                val r = o.optDouble("remaining", 0.0)
                acc.cost = c
                acc.remaining = r
                onMain { me = me.copy(points = r) }
                updateStream { it.copy(cost = c, remaining = r) }
            }
            "error" -> {
                val msg = o.optString("message")
                acc.error = msg
                updateStream { it.copy(error = msg, status = "发生错误") }
            }
        }
    }

    // ================== 构建 / 下载 ==================

    fun pollBuild(taskId: String) {
        viewModelScope.launch {
            repeat(60) {
                delay(5000)
                val r = api.call("/api/download_jar?taskId=$taskId")
                when {
                    r.code == 200 -> { updateBuild(taskId, "success"); return@launch }
                    r.code == 404 -> { /* 仍在构建 */ }
                    else -> {
                        updateBuild(taskId, "failed")
                        val log = if (r.body.length > 1500) "...(日志过长截取末尾)\n" + r.body.takeLast(1500) else r.body
                        inputText = "云端构建失败了。请帮我修复代码。\n" +
                                "**注意：修复完成后，必须立即调用 build_mcmod_jar 工具重新构建，不要只输出文本！**\n\n" +
                                "错误日志：\n```\n$log\n```"
                        delay(1000)
                        if (!isBusy && inputText.contains("错误日志：")) send()
                        return@launch
                    }
                }
            }
            updateBuild(taskId, "timeout")
        }
    }

    private fun updateBuild(id: String, state: String) {
        val idx = messages.indexOfFirst { it.buildTaskId == id }
        if (idx >= 0) messages[idx] = messages[idx].copy(buildState = state)
    }

    fun downloadJar(taskId: String) {
        viewModelScope.launch {
            val ok = api.downloadToDownloads("/api/download_jar?taskId=$taskId", "$taskId.jar")
            showToast(if (ok) "Mod Jar 已保存到下载目录" else "下载失败")
        }
    }

    fun downloadProject(project: ProjectFiles) {
        viewModelScope.launch {
            val ok = api.saveProjectZip(project)
            showToast(if (ok) "源码包已保存到下载目录" else "打包失败")
        }
    }

    // ================== 通知 / 积分 / 公告 ==================

    fun loadNotices() {
        viewModelScope.launch {
            val r = api.call("/api/notifications")
            val o = try { JSONObject(r.body) } catch (e: Exception) { JSONObject() }
            val arr = o.optJSONArray("notifications") ?: JSONArray()
            val list = mutableListOf<Notice>()
            for (i in 0 until arr.length()) {
                val n = arr.optJSONObject(i) ?: continue
                list.add(
                    Notice(
                        n.optString("id"), n.optString("title"), n.optString("content"),
                        n.optLong("createdAt"), n.optBoolean("isRead")
                    )
                )
            }
            notices = list
        }
    }

    fun showNotice(n: Notice) {
        sysTitle = n.title
        sysContent = n.content
        dialog = "sysNotice"
        if (!n.isRead) {
            viewModelScope.launch {
                api.call("/api/notifications/read", "POST", jsonOf("id" to n.id))
                loadNoticeDot()
                loadNotices()
            }
        }
    }

    fun loadNoticeDot() {
        viewModelScope.launch {
            val r = api.call("/api/notifications")
            val o = try { JSONObject(r.body) } catch (e: Exception) { JSONObject() }
            val arr = o.optJSONArray("notifications") ?: JSONArray()
            var unread = false
            for (i in 0 until arr.length()) {
                if (!(arr.optJSONObject(i)?.optBoolean("isRead") ?: true)) { unread = true; break }
            }
            noticeDot = unread
        }
    }

    fun loadPointsLogs() {
        viewModelScope.launch {
            val r = api.call("/api/user/points_logs")
            val o = try { JSONObject(r.body) } catch (e: Exception) { JSONObject() }
            val arr = o.optJSONArray("logs") ?: JSONArray()
            val list = mutableListOf<PointLog>()
            for (i in 0 until arr.length()) {
                val l = arr.optJSONObject(i) ?: continue
                list.add(PointLog(l.optDouble("amount"), l.optString("action"), l.optLong("createdAt")))
            }
            pointLogs = list
        }
    }

    fun loadPopup() {
        viewModelScope.launch {
            val r = api.call("/api/popup")
            val o = try { JSONObject(r.body) } catch (e: Exception) { return@launch }
            val pop = o.optJSONObject("popup") ?: return@launch
            val key = "popup_${pop.optString("id")}_v${pop.optInt("version")}"
            val last = p.getLong(key, 0L)
            val now = System.currentTimeMillis()
            if (last == 0L || now - last > 24 * 3600 * 1000) {
                p.edit().putLong(key, now).apply()
                sysTitle = pop.optString("title")
                sysContent = pop.optString("content")
                dialog = "sysNotice"
            }
        }
    }

    // ================== 个人中心 ==================

    fun updateProfile(type: String) {
        val payload = when (type) {
            "username" -> jsonOf("type" to type, "username" to newUsername.trim())
            "password" -> jsonOf("type" to type, "code" to pwdCode.trim(), "newPassword" to newPwd)
            "email" -> jsonOf("type" to type, "newEmail" to newEmail.trim(), "code" to emailCode.trim())
            else -> return
        }
        viewModelScope.launch {
            val r = api.call("/api/user/profile", "POST", payload)
            if (r.code == 200) {
                showToast("修改成功")
                if (type == "username") me = me.copy(username = newUsername.trim())
            } else showToast(errText(r, "修改失败"))
        }
    }

    fun redeem() {
        val code = redeemCode.trim()
        if (code.isEmpty()) { showToast("请输入兑换码"); return }
        viewModelScope.launch {
            val r = api.call("/api/user/redeem", "POST", jsonOf("code" to code))
            if (r.code == 200) {
                val o = try { JSONObject(r.body) } catch (e: Exception) { JSONObject() }
                showToast("成功兑换 ${o.optDouble("points", 0.0)} 积分")
                redeemCode = ""
                checkMe()
            } else showToast(errText(r, "兑换失败"))
        }
    }

    fun copyInvite() {
        val link = "$BASE/register?stf=${me.inviteCode}"
        val cm = getApplication<Application>().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("link", link))
        showToast("邀请链接已复制")
    }
}
