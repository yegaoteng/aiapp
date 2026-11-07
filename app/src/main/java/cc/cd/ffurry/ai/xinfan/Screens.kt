package cc.cd.ffurry.ai.xinfan

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
private fun ErrorBox(msg: String) {
    if (msg.isEmpty()) return
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFFFFE5E5))
            .padding(8.dp)
    ) {
        Text(msg, color = RedColor, fontSize = 13.sp, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun AuthCard(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 400.dp)
                .padding(horizontal = 20.dp, vertical = 20.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
                .padding(horizontal = 20.dp, vertical = 30.dp)
                .verticalScroll(rememberScrollState())
        ) { content() }
    }
}

@Composable
private fun RowScope.TabItem(text: String, active: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .weight(1f)
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text, color = if (active) Primary else Muted, fontWeight = FontWeight.Medium,
            fontSize = 15.sp)
        Box(
            Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(if (active) Primary else Color.Transparent)
        )
    }
}

@Composable
fun LoginScreen(vm: AppViewModel) {
    AuthCard {
        Text("登录", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextMain,
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(20.dp))
        ErrorBox(vm.loginError)
        Row(Modifier.fillMaxWidth()) {
            TabItem("密码登录", vm.loginType == "password") { vm.loginType = "password" }
            TabItem("验证码登录", vm.loginType == "code") { vm.loginType = "code" }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFEEEEEE)))
        Spacer(Modifier.height(20.dp))

        AppTextField(
            value = vm.loginAccount,
            onValueChange = { vm.loginAccount = it },
            placeholder = if (vm.loginType == "password") "邮箱或用户名" else "邮箱 (验证码登录)",
            keyboardType = KeyboardType.Email,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(15.dp))

        if (vm.loginType == "password") {
            AppTextField(
                value = vm.loginPassword,
                onValueChange = { vm.loginPassword = it },
                placeholder = "密码",
                password = true,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppTextField(
                    value = vm.loginCode,
                    onValueChange = { vm.loginCode = it },
                    placeholder = "邮箱验证码",
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(10.dp))
                GhostButton(
                    text = if (vm.codeCooldown > 0) "${vm.codeCooldown}s 后重发" else "获取验证码",
                    enabled = vm.codeCooldown == 0,
                    onClick = { vm.sendCode(vm.loginAccount.trim(), "login") }
                )
            }
        }
        Spacer(Modifier.height(15.dp))

        PrimaryButton(
            text = if (vm.loginBusy) "正在登录..." else "登录",
            enabled = !vm.loginBusy,
            onClick = { vm.doLogin() },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(20.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Text("还没有账号？ ", color = Muted, fontSize = 14.sp)
            Text("注册一个", color = Primary, fontSize = 14.sp,
                modifier = Modifier.clickable { vm.gotoRegister() })
        }
    }
}

@Composable
fun RegisterScreen(vm: AppViewModel) {
    AuthCard {
        Text("注册账号", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextMain,
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(20.dp))
        ErrorBox(vm.regError)

        AppTextField(vm.regUsername, { vm.regUsername = it }, "用户名 (至少2位)", Modifier.fillMaxWidth())
        Spacer(Modifier.height(15.dp))
        AppTextField(vm.regEmail, { vm.regEmail = it }, "邮箱", Modifier.fillMaxWidth(),
            keyboardType = KeyboardType.Email)
        Spacer(Modifier.height(15.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppTextField(vm.regCode, { vm.regCode = it }, "邮箱验证码", Modifier.weight(1f))
            Spacer(Modifier.width(10.dp))
            GhostButton(
                text = if (vm.codeCooldown > 0) "${vm.codeCooldown}s 后重发" else "获取验证码",
                enabled = vm.codeCooldown == 0 && vm.allowRegister,
                onClick = { vm.sendCode(vm.regEmail.trim(), "register") }
            )
        }
        Text("若 5 分钟内未收到邮件，请前往垃圾邮件查看或换个邮箱。",
            color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
        Spacer(Modifier.height(15.dp))
        AppTextField(vm.regPassword, { vm.regPassword = it }, "密码 (至少6位)", Modifier.fillMaxWidth(), password = true)
        Spacer(Modifier.height(15.dp))
        AppTextField(vm.regConfirm, { vm.regConfirm = it }, "确认密码", Modifier.fillMaxWidth(), password = true)
        Spacer(Modifier.height(15.dp))
        AppTextField(vm.regInvite, { vm.regInvite = it }, "邀请码 (选填)", Modifier.fillMaxWidth())
        Spacer(Modifier.height(15.dp))
        AppTextField(
            value = vm.regAnswer,
            onValueChange = { vm.regAnswer = it },
            placeholder = "你用 AI 来做什么？(至少 15 字，最高 200 字)",
            modifier = Modifier.fillMaxWidth().height(80.dp),
            singleLine = false,
            maxLines = 4
        )
        Spacer(Modifier.height(15.dp))
        PrimaryButton(
            text = if (!vm.allowRegister) "注册功能已关闭" else if (vm.regBusy) "正在提交..." else "注册",
            enabled = vm.allowRegister && !vm.regBusy,
            onClick = { vm.doRegister() },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(20.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Text("已经有账号了？ ", color = Muted, fontSize = 14.sp)
            Text("前往登录", color = Primary, fontSize = 14.sp,
                modifier = Modifier.clickable { vm.gotoLogin() })
        }
    }
}

@Composable
fun PendingScreen(vm: AppViewModel) {
    Box(Modifier.fillMaxSize().background(BgColor), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier
                .widthIn(max = 400.dp)
                .padding(horizontal = 20.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
                .padding(horizontal = 20.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("🐧", fontSize = 50.sp)
            Spacer(Modifier.height(20.dp))
            Text("当前账户审核中", color = Primary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            Text("预计 48 小时内通过，请耐心等待！\n通过后刷新一下即可进入。",
                color = Color(0xFF666666), fontSize = 15.sp, textAlign = TextAlign.Center,
                lineHeight = 24.sp)
            Spacer(Modifier.height(20.dp))
            PrimaryButton("刷新状态", { vm.refreshPending() }, Modifier.fillMaxWidth())
            Spacer(Modifier.height(10.dp))
            PrimaryButton("清除状态(退出此账号登录状态)", { vm.clearPending() },
                Modifier.fillMaxWidth(), danger = true, fontSize = 14.sp)
        }
    }
}

@Composable
fun NoticePage(vm: AppViewModel) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        if (vm.notices.isEmpty()) {
            Text("暂无通知", color = Muted, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 50.dp))
            return@Column
        }
        vm.notices.forEach { n ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .border(1.dp, Border, RoundedCornerShape(12.dp))
                    .clickable { vm.showNotice(n) }
            ) {
                if (!n.isRead) Box(Modifier.width(4.dp).height(64.dp).background(Primary))
                Column(Modifier.padding(16.dp).weight(1f)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(n.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        if (!n.isRead) Text("未读", color = Primary, fontSize = 12.sp)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(n.content, color = Color(0xFF666666), fontSize = 14.sp, maxLines = 2)
                }
            }
        }
    }
}

private fun actionName(a: String): String = when (a) {
    "chat" -> "对话消耗"
    "code" -> "代码消耗"
    "admin_add" -> "后台修改"
    "register_bonus" -> "注册赠送"
    "monthly_grant" -> "每月赠送"
    "invite_register" -> "邀请注册获赠"
    "invite_inviter" -> "邀请他人获赠"
    "redeem" -> "兑换码"
    else -> a
}

@Composable
fun PointsPage(vm: AppViewModel) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        if (vm.pointLogs.isEmpty()) {
            Text("近 30 天无明细", color = Muted, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 50.dp))
            return@Column
        }
        vm.pointLogs.forEach { l ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White)
                    .border(1.dp, Border, RoundedCornerShape(8.dp))
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(actionName(l.action), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(formatTime(l.createdAt), color = Muted, fontSize = 13.sp,
                        modifier = Modifier.padding(top = 4.dp))
                }
                val plus = l.amount > 0
                Text(
                    (if (plus) "+" else "") + String.format("%.1f", l.amount),
                    color = if (plus) Color(0xFF34C759) else RedColor,
                    fontWeight = FontWeight.Bold, fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
private fun GroupTitle(text: String, color: androidx.compose.ui.graphics.Color = TextMain) {
    Text(text, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = color,
        modifier = Modifier.padding(bottom = 10.dp))
}

@Composable
fun ProfilePage(vm: AppViewModel) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        CardBox {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Avatar(vm.me.username, hexColor(vm.me.avatarColor), 64.dp, 28.sp)
                Spacer(Modifier.height(10.dp))
                Text(vm.me.username, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(vm.me.email, color = Muted, fontSize = 13.sp,
                    modifier = Modifier.padding(top = 5.dp))
            }
        }
        Spacer(Modifier.height(16.dp))

        CardBox {
            GroupTitle("修改用户名 (每7天一次)")
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppTextField(vm.newUsername, { vm.newUsername = it }, "新用户名", Modifier.weight(1f))
                Spacer(Modifier.width(10.dp))
                PrimaryButton("保存", { vm.updateProfile("username") }, fontSize = 14.sp)
            }
        }
        Spacer(Modifier.height(16.dp))

        CardBox {
            GroupTitle("修改密码")
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppTextField(vm.pwdCode, { vm.pwdCode = it }, "邮箱验证码", Modifier.weight(1f))
                Spacer(Modifier.width(10.dp))
                GhostButton("获取", { vm.sendProfileCode("change_password") })
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppTextField(vm.newPwd, { vm.newPwd = it }, "新密码 (至少6位)", Modifier.weight(1f), password = true)
                Spacer(Modifier.width(10.dp))
                PrimaryButton("修改", { vm.updateProfile("password") }, fontSize = 14.sp)
            }
        }
        Spacer(Modifier.height(16.dp))

        CardBox {
            GroupTitle("修改绑定邮箱")
            AppTextField(vm.newEmail, { vm.newEmail = it }, "新邮箱", Modifier.fillMaxWidth(),
                keyboardType = KeyboardType.Email)
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppTextField(vm.emailCode, { vm.emailCode = it }, "新邮箱验证码", Modifier.weight(1f))
                Spacer(Modifier.width(10.dp))
                GhostButton("获取", { vm.sendProfileCode("change_email") })
            }
            Spacer(Modifier.height(10.dp))
            PrimaryButton("确认绑定", { vm.updateProfile("email") }, Modifier.fillMaxWidth(), fontSize = 14.sp)
        }
        Spacer(Modifier.height(16.dp))

        CardBox {
            GroupTitle("我的邀请码", Primary)
            Text("邀请好友注册，双方各得 500 积分！", color = Color(0xFF666666), fontSize = 13.sp)
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(BluePale)
                    .border(1.dp, Primary, RoundedCornerShape(12.dp))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(vm.me.inviteCode.ifEmpty { "------" }, color = Primary,
                        fontSize = 24.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    Spacer(Modifier.height(10.dp))
                    PrimaryButton("复制邀请链接", { vm.copyInvite() }, outline = true, fontSize = 14.sp)
                }
            }
        }
        Spacer(Modifier.height(16.dp))

        CardBox {
            GroupTitle("积分兑换")
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppTextField(vm.redeemCode, { vm.redeemCode = it }, "输入兑换码", Modifier.weight(1f))
                Spacer(Modifier.width(10.dp))
                PrimaryButton("兑换", { vm.redeem() }, fontSize = 14.sp)
            }
        }
        Spacer(Modifier.height(16.dp))
        PrimaryButton("退出登录", { vm.logout() }, Modifier.fillMaxWidth(), danger = true, fontSize = 16.sp)
        Spacer(Modifier.height(30.dp))
    }
}
