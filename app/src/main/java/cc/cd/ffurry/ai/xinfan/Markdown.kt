package cc.cd.ffurry.ai.xinfan

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

fun openBrowser(ctx: Context, url: String) {
    val u = if (url.startsWith("http")) url else BASE + url
    try {
        ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(u))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: Exception) {
        // 忽略
    }
}

private sealed class Block {
    data class Paragraph(val text: String) : Block()
    data class Code(val text: String) : Block()
    data class Heading(val text: String, val level: Int) : Block()
    data class Quote(val text: String) : Block()
    data class ListItem(val text: String) : Block()
    data class Image(val alt: String, val url: String) : Block()
}

private val imageLine = Regex("""^\s*!\[(.*?)\]\((.*?)\)\s*$""")
private val inlineRe = Regex("""!?\[([^\]]+)\]\(([^)]+)\)|\*\*(.+?)\*\*|`(.+?)`""")

private fun parseBlocks(src: String): List<Block> {
    val out = mutableListOf<Block>()
    val para = StringBuilder()
    var inCode = false
    val code = StringBuilder()

    fun flush() {
        if (para.isNotBlank()) {
            out.add(Block.Paragraph(para.toString().trim()))
            para.clear()
        } else para.clear()
    }

    for (raw in src.replace("\r\n", "\n").split("\n")) {
        val line = raw
        if (line.trim().startsWith("```")) {
            if (inCode) {
                out.add(Block.Code(code.toString().trimEnd()))
                code.clear()
                inCode = false
            } else {
                flush()
                inCode = true
            }
            continue
        }
        if (inCode) {
            code.append(line).append('\n')
            continue
        }
        if (line.isBlank()) { flush(); continue }

        val im = imageLine.find(line)
        if (im != null) {
            flush()
            out.add(Block.Image(im.groupValues[1], im.groupValues[2]))
            continue
        }
        val head = Regex("""^(#{1,6})\s+(.*)$""").find(line)
        if (head != null) {
            flush()
            out.add(Block.Heading(head.groupValues[2], head.groupValues[1].length))
            continue
        }
        if (line.trimStart().startsWith("> ")) {
            flush()
            out.add(Block.Quote(line.trimStart().removePrefix("> ")))
            continue
        }
        val li = Regex("""^\s*(?:[-*]|\d+\.)\s+(.*)$""").find(line)
        if (li != null) {
            flush()
            out.add(Block.ListItem(li.groupValues[1]))
            continue
        }
        if (para.isNotEmpty()) para.append('\n')
        para.append(line)
    }
    if (inCode && code.isNotBlank()) out.add(Block.Code(code.toString().trimEnd()))
    flush()
    return out
}

private fun buildInline(text: String): AnnotatedString = buildAnnotatedString {
    var last = 0
    for (m in inlineRe.findAll(text)) {
        if (m.range.first > last) append(text.substring(last, m.range.first))
        val linkText = m.groupValues.getOrNull(1).orEmpty()
        val linkUrl = m.groupValues.getOrNull(2).orEmpty()
        val bold = m.groupValues.getOrNull(3).orEmpty()
        val code = m.groupValues.getOrNull(4).orEmpty()
        when {
            bold.isNotEmpty() -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(bold) }
            code.isNotEmpty() -> withStyle(
                SpanStyle(fontFamily = FontFamily.Monospace, background = CodeBg, color = Color(0xFFC7254E))
            ) { append(code) }
            linkUrl.isNotEmpty() -> {
                pushStringAnnotation("url", linkUrl)
                withStyle(SpanStyle(color = Primary)) { append(linkText) }
                pop()
            }
            else -> append(m.value)
        }
        last = m.range.last + 1
    }
    if (last < text.length) append(text.substring(last))
}

@Composable
private fun InlineText(text: String) {
    val annotated = buildInline(text)
    val ctx = LocalContext.current
    ClickableText(
        text = annotated,
        style = TextStyle(fontSize = 15.sp, color = TextMain, lineHeight = 24.sp),
        onClick = { offset ->
            annotated.getStringAnnotations("url", offset, offset).firstOrNull()?.let {
                openBrowser(ctx, it.item)
            }
        }
    )
}

@Composable
fun MarkdownView(text: String, modifier: Modifier = Modifier) {
    val blocks = parseBlocks(text.replace("<!-- BUILD_TASK:.*? -->".toRegex(), ""))
    Column(modifier = modifier) {
        blocks.forEachIndexed { i, b ->
            when (b) {
                is Block.Paragraph -> InlineText(b.text)
                is Block.Heading -> Text(
                    b.text,
                    fontSize = when (b.level) { 1 -> 22.sp; 2 -> 19.sp; else -> 17.sp },
                    fontWeight = FontWeight.Bold,
                    color = TextMain
                )
                is Block.Code -> Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(CodeBg)
                        .padding(10.dp)
                        .horizontalScroll(rememberScrollState())
                ) {
                    Text(b.text, fontFamily = FontFamily.Monospace, fontSize = 13.sp,
                        color = TextMain, softWrap = false)
                }
                is Block.Quote -> Row {
                    Box(Modifier.width(3.dp).height(20.dp).background(Border))
                    Spacer(Modifier.width(10.dp))
                    InlineText(b.text)
                }
                is Block.ListItem -> Row {
                    Text("• ", color = Muted, fontSize = 15.sp, lineHeight = 24.sp)
                    InlineText(b.text)
                }
                is Block.Image -> {
                    val ctx = LocalContext.current
                    NetworkImage(
                        url = b.url,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { openBrowser(ctx, b.url) }
                    )
                }
            }
            if (i != blocks.lastIndex) Spacer(Modifier.height(8.dp))
        }
    }
}
