package cc.cd.ffurry.ai.xinfan

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar
import java.util.TimeZone

@Composable
private fun bubbleMaxWidth(): Dp = (LocalConfiguration.current.screenWidthDp * 0.85f).dp

private val AssistantShape = RoundedCornerShape(
    topStart = 14.dp, topEnd = 14.dp, bottomEnd = 14.dp, bottomStart = 4.dp
)
private val UserShape = RoundedCornerShape(
    topStart = 14.dp, topEnd = 14.dp, bottomStart = 14.dp, bottomEnd = 4.dp
)

@Composable
private fun ToolBtn(text: String, active: Boolean = false, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (active) BluePale else Color.White)
            .border(1.dp, if (active) Primary else Color(0xFFDDDDDD), RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(text, fontSize = 12.sp, color = if (active) Primary else Color(0xFF666666))
    }
}

@Composable
private fun StatusIndicator(status: String, seconds: Int) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFF8F9FA))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 2.dp, color = Primary)
        Spacer(Modifier.width(8.dp))
        Text(status, color = Muted, fontSize = 13.sp)
        Spacer(Modifier.width(8.dp))
        Text("${seconds}s", color = Muted, fontSize = 13.sp)
    }
}

@Composable
private fun ThinkBox(text: String, openInit: Boolean = false) {
    var open by remember { mutableStateOf(openInit) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFF8F9FA))
            .border(1.dp, Border, RoundedCornerShape(8.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { open = !open }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("思考过程", color = Color(0xFF666666), fontSize = 13.sp)
            Text(if (open) "▴" else "▾", color = Muted, fontSize = 12.sp)
        }
        if (open) {
            Box(Modifier.fillMaxWidth().height(1.dp).background(Border))
            Text(
                text, color = Muted, fontSize = 13.sp, fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}

@Composable
private fun BillingBar(cost: Double, remaining: Double) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(BluePale)
            .border(1.dp, Color(0xFFCCE5FF), RoundedCornerShape(8.dp))
            .padding(10.dp)
    ) {
        Text("本次消耗 $cost 积分，剩余 ${String.format("%.1f", remaining)} 积分",
            color = Primary, fontSize = 13.sp)
    }
}

@Composable
private fun ErrorBar(msg: String, onRetry: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(ErrorBg)
            .border(1.dp, Color(0xFFFFCDD2), RoundedCornerShape(8.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(msg, color = ErrorText, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(Color.White)
                .border(1.dp, ErrorText, RoundedCornerShape(4.dp))
                .clickable { onRetry() }
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) { Text("重试", color = ErrorText, fontSize = 12.sp) }
    }
}

@Composable
private fun BuildCard(taskId: String, state: String?, vm: AppViewModel) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFF0F7FF))
            .border(1.dp, Color(0xFFCCE5FF), RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        when (state) {
            "success" -> {
                Text("✅ 构建成功！", color = Color(0xFF34C759), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(Modifier.height(8.dp))
                PrimaryButton("下载 Mod Jar", { vm.downloadJar(taskId) }, fontSize = 14.sp)
            }
            "failed" -> Text("❌ 构建失败，正在自动发送错误日志给 AI 修复...",
                color = ErrorText, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            "timeout" -> Text("构建超时，请检查 Github Actions 是否报错或网络问题。",
                color = ErrorText, fontSize = 13.sp)
            else -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp, color = Primary)
                    Spacer(Modifier.width(8.dp))
                    Text("正在云端构建中，请稍候...", fontSize = 14.sp)
                }
                Text("构建可能需要几分钟，完成后会显示下载按钮。",
                    color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 5.dp))
            }
        }
    }
}

@Composable
private fun ProjectCard(project: ProjectFiles, vm: AppViewModel) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFF0F7FF))
            .border(1.dp, Color(0xFFCCE5FF), RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Text("📦 ${project.name}.zip 已生成", fontSize = 14.sp)
        Spacer(Modifier.height(8.dp))
        PrimaryButton("点击下载源码", { vm.downloadProject(project) }, fontSize = 14.sp)
    }
}

@Composable
private fun LocalImageRow(bmps: List<ImageBitmap>) {
    if (bmps.isEmpty()) return
    Row(
        modifier = Modifier.padding(bottom = 8.dp).horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        bmps.forEach {
            Image(it, null, Modifier.size(150.dp).clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop)
        }
    }
}

@Composable
private fun RemoteImageRow(ids: List<String>) {
    if (ids.isEmpty()) return
    Row(
        modifier = Modifier.padding(bottom = 8.dp).horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ids.forEach {
            NetworkImage("/api/images/$it", Modifier.size(150.dp).clip(RoundedCornerShape(8.dp)))
        }
    }
}

@Composable
private fun MessageBubble(m: Message, vm: AppViewModel) {
    if (m.role == "user") {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
            Box(
                modifier = Modifier
                    .widthIn(max = bubbleMaxWidth())
                    .clip(UserShape)
                    .background(Primary)
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Column {
                    LocalImageRow(m.localBitmaps)
                    RemoteImageRow(m.attachments)
                    if (m.content.isNotEmpty()) {
                        Text(m.content, color = Color.White, fontSize = 15.sp, lineHeight = 24.sp)
                    }
                }
            }
        }
    } else {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(AssistantShape)
                .background(Color.White)
                .border(1.dp, Border, AssistantShape)
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Column {
                if (m.reasoning != null) ThinkBox(m.reasoning)
                if (m.content.isNotEmpty()) MarkdownView(m.content)
                if (m.error != null) ErrorBar(m.error) { vm.send(true) }
                if (m.cost != null && m.remaining != null) BillingBar(m.cost, m.remaining)
                if (m.buildTaskId != null) BuildCard(m.buildTaskId, m.buildState, vm)
                if (m.project != null) ProjectCard(m.project, vm)
            }
        }
    }
}

@Composable
private fun StreamBubble(vm: AppViewModel) {
    val s = vm.stream
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AssistantShape)
            .background(Color.White)
            .border(1.dp, Border, AssistantShape)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Column {
            StatusIndicator(s.status, s.seconds)
            Spacer(Modifier.height(10.dp))
            if (vm.enableThinking && s.reasoning.isNotEmpty()) {
                ThinkBox(s.reasoning, openInit = true)
            }
            if (s.text.isNotEmpty()) MarkdownView(s.text)
            if (s.error != null) ErrorBar(s.error) { vm.send(true) }
            if (s.cost != null && s.remaining != null) BillingBar(s.cost, s.remaining)
            if (s.buildTaskId != null) BuildCard(s.buildTaskId, "building", vm)
            if (s.project != null) ProjectCard(s.project, vm)
        }
    }
}

@Composable
private fun LazyItemScope.WelcomeCard(vm: AppViewModel) {
    val bj = Calendar.getInstance(TimeZone.getTimeZone("GMT+8")).get(Calendar.HOUR_OF_DAY)
    val greet = when {
        bj in 5..11 -> "早上好"
        bj in 12..17 -> "中午好"
        else -> "晚上好"
    }
    Box(
        modifier = Modifier.fillParentMaxHeight().fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$greet，${vm.me.username}", fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold, color = TextMain)
            Spacer(Modifier.height(8.dp))
            Text(
                if (vm.mode == "chat") "今天想聊点什么？" else "今天想构建什么项目？",
                color = Muted, fontSize = 15.sp
            )
        }
    }
}

@Composable
private fun ModelOption(m: ModelInfo, vm: AppViewModel) {
    val selected = m.id == vm.selectedModel
    val bg = when {
        selected -> BluePale
        !m.enabled -> Color(0xFFF5F5F5)
        else -> Color.White
    }
    val rate = if (m.multiplier % 1.0 == 0.0) m.multiplier.toInt().toString() else m.multiplier.toString()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .border(1.dp, if (selected) Primary else Border, RoundedCornerShape(8.dp))
            .clickable(enabled = m.enabled) { vm.selectModel(m.id) }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(m.name, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                color = if (m.enabled) TextMain else Muted)
            Text(m.desc, fontSize = 12.sp, color = Muted)
            if (m.limitMsg.isNotEmpty()) {
                Text(m.limitMsg, fontSize = 10.sp,
                    color = if (!m.enabled) RedColor else Muted)
            }
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFFFFF8E1))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text("x$rate", color = Orange, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ModelFold(vm: AppViewModel) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 250.dp)
            .verticalScroll(rememberScrollState())
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .background(Color(0xFAFFFFFF))
            .border(1.dp, Border, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        when {
            vm.modelsLoading -> Text("正在加载模型...", color = Muted, fontSize = 13.sp,
                modifier = Modifier.fillMaxWidth().padding(20.dp))
            vm.models.isEmpty() -> Text("暂无可用模型 (API维护中)", color = Muted, fontSize = 13.sp,
                modifier = Modifier.fillMaxWidth().padding(20.dp))
            else -> vm.models.forEach { ModelOption(it, vm) }
        }
    }
}

@Composable
private fun PreviewRow(vm: AppViewModel) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        vm.pendingImages.forEachIndexed { i, img ->
            Box(Modifier.size(50.dp)) {
                Image(img.bitmap, null,
                    Modifier.size(50.dp).clip(RoundedCornerShape(6.dp)),
                    contentScale = ContentScale.Crop)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(RedColor)
                        .clickable { vm.removeImage(i) },
                    contentAlignment = Alignment.Center
                ) { Text("×", color = Color.White, fontSize = 12.sp) }
            }
        }
    }
}

@Composable
private fun Composer(vm: AppViewModel, onPickImages: () -> Unit) {
    val modelName = vm.models.firstOrNull { it.id == vm.selectedModel }?.name ?: "选择模型"
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .border(1.dp, Border, RoundedCornerShape(0.dp))
            .padding(12.dp)
    ) {
        if (vm.modelFoldOpen) {
            ModelFold(vm)
            Spacer(Modifier.height(10.dp))
        }
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ToolBtn("$modelName ▾") { vm.toggleModelFold() }
            ToolBtn("思考模式", vm.enableThinking) { vm.toggleThink() }
            ToolBtn("联网搜索", vm.enableSearch) { vm.toggleSearch() }
            ToolBtn("+", onClick = onPickImages)
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFFAFAFA))
                    .border(1.dp, Border, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                if (vm.pendingImages.isNotEmpty()) PreviewRow(vm)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 24.dp, max = 120.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    if (vm.inputText.isEmpty()) {
                        Text("发消息给 AI 助手…", color = Muted, fontSize = 15.sp)
                    }
                    BasicTextField(
                        value = vm.inputText,
                        onValueChange = { vm.inputText = it },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = TextStyle(fontSize = 15.sp, color = TextMain, lineHeight = 22.sp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { vm.handleSendOrStop() }),
                        cursorBrush = SolidColor(Primary),
                        enabled = !vm.isBusy
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (vm.isBusy) RedColor else Primary)
                    .clickable { vm.handleSendOrStop() },
                contentAlignment = Alignment.Center
            ) {
                Text(if (vm.isBusy) "停止" else "发送", color = Color.White,
                    fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ChatPage(vm: AppViewModel) {
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris -> vm.onImagesPicked(uris) }
    val listState = rememberLazyListState()

    LaunchedEffect(
        vm.messages.size, vm.stream.active, vm.stream.text, vm.stream.reasoning,
        vm.stream.error, vm.stream.cost, vm.stream.buildTaskId, vm.stream.project
    ) {
        val n = listState.layoutInfo.totalItemsCount
        if (n > 0) listState.scrollToItem(n - 1)
    }

    Column(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(start = 12.dp, top = 16.dp, end = 12.dp, bottom = 20.dp)
        ) {
            if (vm.messages.isEmpty() && !vm.stream.active) {
                item { WelcomeCard(vm) }
            }
            items(vm.messages) { m -> MessageBubble(m, vm) }
            if (vm.stream.active) {
                item { StreamBubble(vm) }
            }
        }
        Composer(vm) { launcher.launch("image/*") }
    }
}
