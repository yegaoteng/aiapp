package cc.cd.ffurry.ai.xinfan

import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

val LocalApi = staticCompositionLocalOf<Api> { error("Api 未提供") }

@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    maxLines: Int = 1,
    minHeight: Dp = 44.dp,
    radius: Dp = 8.dp,
    fieldBg: Color = Color.White,
    keyboardType: KeyboardType = KeyboardType.Text,
    password: Boolean = false,
    onSend: (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(radius))
            .background(fieldBg)
            .border(1.dp, Color(0xFFDDDDDD), RoundedCornerShape(radius))
            .heightIn(min = minHeight)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        if (value.isEmpty()) {
            Text(placeholder, color = Color(0xFF999999), fontSize = 14.sp)
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = true,
            textStyle = TextStyle(fontSize = 15.sp, color = TextMain, lineHeight = 22.sp),
            singleLine = singleLine,
            maxLines = maxLines,
            keyboardOptions = KeyboardOptions(
                keyboardType = if (password) KeyboardType.Password else keyboardType,
                imeAction = if (onSend != null) ImeAction.Send else ImeAction.Default
            ),
            keyboardActions = KeyboardActions(onSend = { onSend?.invoke() }),
            visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
            cursorBrush = SolidColor(Primary)
        )
    }
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    danger: Boolean = false,
    outline: Boolean = false,
    enabled: Boolean = true,
    fontSize: TextUnit = 16.sp,
    radius: Dp = 8.dp
) {
    val bg = when {
        outline -> Color.White
        danger -> RedColor
        else -> Primary
    }
    val fg = when {
        outline -> Primary
        else -> Color.White
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(radius))
            .background(if (enabled) bg else bg.copy(alpha = 0.55f))
            .then(if (outline) Modifier.border(1.dp, Primary, RoundedCornerShape(radius)) else Modifier)
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = fg, fontSize = fontSize, fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center)
    }
}

@Composable
fun GhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(SoftBg)
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 15.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontSize = 14.sp, color = if (enabled) TextMain else Muted)
    }
}

@Composable
fun Avatar(name: String, color: Color = Primary, size: Dp = 40.dp, fontSize: TextUnit = 18.sp) {
    val ch = name.firstOrNull()?.uppercase() ?: "?"
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(color),
        contentAlignment = Alignment.Center
    ) {
        Text(ch, color = Color.White, fontSize = fontSize, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun CardBox(
    modifier: Modifier = Modifier,
    radius: Dp = 12.dp,
    padding: Dp = 16.dp,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(radius))
            .background(CardWhite)
            .border(1.dp, Border, RoundedCornerShape(radius))
            .padding(padding)
    ) { content() }
}

@Composable
fun ToastOverlay(message: String?) {
    AnimatedVisibility(
        visible = message != null,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            modifier = Modifier
                .padding(top = 20.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xCC000000))
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            Text(message.orEmpty(), color = Color.White, fontSize = 14.sp)
        }
    }
}

@Composable
fun AppDialog(
    title: String,
    titleColor: Color = TextMain,
    content: @Composable () -> Unit,
    confirmText: String = "确认",
    cancelText: String? = "取消",
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    Dialog(onDismissRequest = onCancel) {
        Surface(shape = RoundedCornerShape(14.dp), color = Color.White) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    title,
                    color = titleColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(12.dp))
                content()
                Spacer(Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (cancelText != null) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(SoftBg)
                                .clickable { onCancel() }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) { Text(cancelText, color = TextMain, fontWeight = FontWeight.SemiBold) }
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (cancelText == null) Primary else RedColor)
                            .clickable { onConfirm() }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(confirmText, color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

private val imageCache = LruCache<String, androidx.compose.ui.graphics.ImageBitmap>(48)

@Composable
fun NetworkImage(
    url: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val api = LocalApi.current
    var bmp by remember(url) { mutableStateOf(imageCache.get(url)) }
    LaunchedEffect(url) {
        if (bmp == null) {
            val loaded = withContext(Dispatchers.IO) {
                val bytes = api.bytes(url)
                bytes?.let { BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap() }
            }
            if (loaded != null) {
                imageCache.put(url, loaded)
                bmp = loaded
            }
        }
    }
    val m = if (onClick != null) modifier.clickable { onClick() } else modifier
    if (bmp != null) {
        Image(
            bitmap = bmp!!,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = m
        )
    } else {
        Box(
            modifier = m
                .heightIn(min = 120.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFFEEEEEE))
        )
    }
}
