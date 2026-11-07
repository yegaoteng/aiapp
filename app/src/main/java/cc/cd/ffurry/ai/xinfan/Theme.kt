package cc.cd.ffurry.ai.xinfan

import androidx.compose.ui.graphics.Color
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 与 index.html 的 :root 变量一一对应 */
val Primary = Color(0xFF007AFF)
val BgColor = Color(0xFFF5F7FA)
val TextMain = Color(0xFF333333)
val Border = Color(0xFFE1E4E8)
val Muted = Color(0xFF888888)
val CardWhite = Color(0xFFFFFFFF)
val SoftBg = Color(0xFFF0F2F5)
val BlueLight = Color(0xFFE6F0FF)
val BluePale = Color(0xFFF0F7FF)
val GreenLight = Color(0xFFE8F5E9)
val GreenText = Color(0xFF2E7D32)
val RedColor = Color(0xFFFF3B30)
val Orange = Color(0xFFFF9500)
val ErrorBg = Color(0xFFFFF2F2)
val ErrorText = Color(0xFFC62828)
val CodeBg = Color(0xFFF6F8FA)

fun hexColor(s: String?): Color = try {
    Color(android.graphics.Color.parseColor(s?.ifEmpty { "#007aff" } ?: "#007aff"))
} catch (e: Exception) {
    Primary
}

fun formatTime(sec: Long): String = try {
    SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(Date(sec * 1000))
} catch (e: Exception) {
    ""
}
