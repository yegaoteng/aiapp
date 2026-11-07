package cc.cd.ffurry.ai.xinfan

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val vm: AppViewModel = viewModel()
            CompositionLocalProvider(LocalApi provides vm.api) {
                AppRoot(vm)
            }
        }
    }
}

@Composable
private fun BootScreen() {
    Box(
        Modifier.fillMaxSize().background(BgColor),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = Primary)
            Spacer(Modifier.height(16.dp))
            Text("正在加载...", color = Primary, fontSize = 16.sp)
        }
    }
}

@Composable
fun AppRoot(vm: AppViewModel) {
    LaunchedEffect(Unit) { vm.boot() }

    Box(Modifier.fillMaxSize().background(BgColor)) {
        when (vm.screen) {
            "boot" -> BootScreen()
            "login" -> LoginScreen(vm)
            "register" -> RegisterScreen(vm)
            "pending" -> PendingScreen(vm)
            "main" -> MainScreen(vm)
        }

        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            ToastOverlay(vm.toast)
        }

        when (vm.dialog) {
            "stop" -> AppDialog(
                title = "确认停止",
                content = { Text("是否确认停止 AI 回答？", color = Muted, fontSize = 14.sp) },
                onConfirm = { vm.confirmStop() },
                onCancel = { vm.dialog = null }
            )
            "delete" -> AppDialog(
                title = "删除对话",
                titleColor = RedColor,
                content = {
                    Text(
                        "即将删除该聊天的所有消息记录及相关代码文件，删除后不可恢复，是否确认？",
                        color = Muted, fontSize = 14.sp, lineHeight = 22.sp
                    )
                },
                onConfirm = { vm.confirmDelete() },
                onCancel = { vm.dialog = null }
            )
            "sysNotice" -> AppDialog(
                title = vm.sysTitle,
                titleColor = Primary,
                content = {
                    Column(
                        Modifier
                            .heightIn(max = 420.dp)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 4.dp)
                    ) {
                        Text(vm.sysContent, color = TextMain, fontSize = 14.sp, lineHeight = 22.sp)
                    }
                },
                confirmText = "我知道了",
                cancelText = null,
                onConfirm = { vm.dialog = null },
                onCancel = { vm.dialog = null }
            )
        }
    }
}
