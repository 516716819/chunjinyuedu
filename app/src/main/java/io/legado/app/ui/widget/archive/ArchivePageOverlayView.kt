package io.legado.app.ui.widget.archive

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import io.legado.app.R

/**
 * 阅读页的档案页边标记。
 *
 * Wenku8 的阅读页把正文收进一页「登记纸」的语境里：页边两道装订竖线，
 * 上下各一对 L 形角标。这里只复刻这套线性标记，不铺网格、不加纸纹，
 * 因为阅读页第一优先级是可读性，任何纹理都会和正文抢注意力。
 *
 * 设计约束（都是有原因的）：
 * - 只画左右两侧，不画上下贯穿横线。正文区域的上下 padding 往往小于左右，
 *   横线容易压到首行/末行文字。
 * - 线条贴边 3dp，落在正文列宽之外的留白里。
 * - 放在 content_text_view 的下一层（XML 中声明在其之前），
 *   既不遮挡文字，也不参与触摸命中，翻页与选字完全不受影响。
 */
class ArchivePageOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val inset = ArchiveTokens.dp(this, 3f)
    private val endGap = ArchiveTokens.dp(this, 18f)
    private val cornerRun = ArchiveTokens.dp(this, 9f)

    private val rulePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = ArchiveTokens.dp(this@ArchivePageOverlayView, 0.6f)
        color = ContextCompat.getColor(context, R.color.divider)
        alpha = 80
    }

    private val markPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = ArchiveTokens.dp(this@ArchivePageOverlayView, 0.9f)
        strokeCap = Paint.Cap.SQUARE
        color = ContextCompat.getColor(context, R.color.divider)
        alpha = 170
    }

    init {
        setWillNotDraw(false)
        isClickable = false
        isFocusable = false
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width <= 0 || height <= 0) return
        val top = endGap
        val bottom = height - endGap
        if (bottom <= top) return

        val leftX = inset
        val rightX = width - inset

        canvas.drawLine(leftX, top, leftX, bottom, rulePaint)
        canvas.drawLine(rightX, top, rightX, bottom, rulePaint)

        // 四角 L 形：把竖线两端向内折一小段，形成档案页的定位角
        canvas.drawLine(leftX, top, leftX + cornerRun, top, markPaint)
        canvas.drawLine(leftX, top, leftX, top + cornerRun, markPaint)
        canvas.drawLine(leftX, bottom, leftX + cornerRun, bottom, markPaint)
        canvas.drawLine(leftX, bottom, leftX, bottom - cornerRun, markPaint)

        canvas.drawLine(rightX, top, rightX - cornerRun, top, markPaint)
        canvas.drawLine(rightX, top, rightX, top + cornerRun, markPaint)
        canvas.drawLine(rightX, bottom, rightX - cornerRun, bottom, markPaint)
        canvas.drawLine(rightX, bottom, rightX, bottom - cornerRun, markPaint)
    }
}
