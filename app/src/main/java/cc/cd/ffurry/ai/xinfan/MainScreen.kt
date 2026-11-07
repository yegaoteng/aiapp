package cc.cd.ffurry.ai.xinfan

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
private fun RowScope.ModeBtn(text: String, active: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(6.dp))
            .background(if (active) Color.White else Color.Transparent)
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = if (active) Primary else Color(0xFF666666),
            fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun NavItem(
    text: String,
    active: Boolean = false,
    dot: Boolean = false,
    orange: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (active) BlueLight else Color.Transparent)
            .clickable(enabled = !orange) { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text,
                color = when {
                    orange -> Orange
                    active -> Primary
                    else -> TextMain
                },
                fontSize = 14.sp,
                fontWeight = if (orange) FontWeight.Bold else FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            if (dot) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(RedColor))
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChatRow(c: ChatItem, vm: AppViewModel) {
    val active = c.id == vm.currentChatId
    val busy = active && vm.isBusy
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (active) Color(0xFFEEF4FF) else Color.Transparent)
            .combinedClickable(
                onClick = { vm.openChat(c.id) },
                onLongClick = { vm.requestDelete(c.id) }
            )
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                c.title, fontSize = 14.sp, fontWeight = FontWeight.Medium,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (c.mode == "chat") Color(0xFFEEF4FF) else GreenLight)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(if (c.mode == "chat") "聊天" else "代码",
                    color = if (c.mode == "chat") Primary else GreenText,
                    fontSize = 11.sp)
            }
        }
        if (busy) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(BlueLight)
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(0.3f)
                        .height(3.dp)
                        .background(Primary)
                )
            }
        }
    }
}

@Composable
private fun DrawerContent(vm: AppViewModel, modifier: Modifier = Modifier) {
    Column(modifier.background(Color.White).border(1.dp, Border)) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(SoftBg)
                    .padding(4.dp)
            ) {
                ModeBtn("聊天模式", vm.mode == "chat") { vm.switchMode("chat") }
                ModeBtn("代码模式", vm.mode == "code") { vm.switchMode("code") }
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(Border))
        Column(Modifier.padding(10.dp)) {
            NavItem("对话", vm.page == "chat") { vm.switchPage("chat") }
            NavItem("通知中心", vm.page == "notice", dot = vm.noticeDot) { vm.switchPage("notice") }
            NavItem("积分明细", vm.page == "points") { vm.switchPage("points") }
            NavItem("剩余积分：${String.format("%.1f", vm.me.points)}", orange = true) {}
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(Border))
        if (vm.page == "chat") {
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(10.dp)
            ) {
                Text("历史记录 (长按删除)", color = Muted, fontSize = 12.sp,
                    modifier = Modifier.padding(start = 10.dp, top = 10.dp, bottom = 5.dp))
                if (vm.chats.isEmpty()) {
                    Text("暂无历史记录", color = Color(0xFFAAAAAA), fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(16.dp))
                } else {
                    vm.chats.forEach { ChatRow(it, vm) }
                }
            }
        } else {
            Spacer(Modifier.weight(1f))
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(Border))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFFAFAFA))
                .clickable { vm.switchPage("profile") }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Avatar(vm.me.username, hexColor(vm.me.avatarColor))
            Spacer(Modifier.width(10.dp))
            Column {
                Text(vm.me.username, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text("修改信息 >", color = Muted, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun MainHeader(vm: AppViewModel) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .background(Color.White)
            .border(1.dp, Border)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(SoftBg)
                .clickable { vm.toggleDrawer() },
            contentAlignment = Alignment.Center
        ) { Text("☰", fontSize = 20.sp, color = TextMain) }
        Text("新帆 · 启航 (${if (vm.mode == "chat") "聊天" else "代码"})",
            fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Primary)
                .clickable { vm.newChat() },
            contentAlignment = Alignment.Center
        ) { Text("+", color = Color.White, fontSize = 24.sp) }
    }
}

@Composable
private fun PageHeader(vm: AppViewModel) {
    val title = when (vm.page) {
        "notice" -> "通知中心"
        "points" -> "积分明细 (近30天)"
        else -> "个人中心"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .border(1.dp, Border)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(36.dp)
                .clickable { vm.toggleDrawer() },
            contentAlignment = Alignment.Center
        ) { Text("☰", fontSize = 20.sp, color = TextMain) }
        Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(36.dp))
    }
}

@Composable
private fun MainContent(vm: AppViewModel, wide: Boolean) {
    Column(Modifier.fillMaxSize()) {
        if (!wide) {
            if (vm.page == "chat") MainHeader(vm) else PageHeader(vm)
        }
        Box(Modifier.weight(1f)) {
            when (vm.page) {
                "chat" -> ChatPage(vm)
                "notice" -> NoticePage(vm)
                "points" -> PointsPage(vm)
                "profile" -> ProfilePage(vm)
            }
        }
    }
}

@Composable
fun MainScreen(vm: AppViewModel) {
    BoxWithConstraints(Modifier.fillMaxSize().background(BgColor)) {
        val wide = maxWidth >= 768.dp
        if (wide) {
            Row(Modifier.fillMaxSize()) {
                DrawerContent(vm, Modifier.width(280.dp).fillMaxHeight())
                Box(Modifier.weight(1f).fillMaxHeight()) { MainContent(vm, true) }
            }
        } else {
            Box(Modifier.fillMaxSize()) {
                MainContent(vm, false)
                if (vm.drawerOpen) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(Color(0x66000000))
                            .clickable { vm.closeDrawer() }
                    )
                }
                val dx by animateDpAsState(
                    targetValue = if (vm.drawerOpen) 0.dp else (-280).dp
                )
                DrawerContent(
                    vm,
                    Modifier
                        .width(280.dp)
                        .fillMaxHeight()
                        .offset(x = dx)
                )
            }
        }
    }
}
