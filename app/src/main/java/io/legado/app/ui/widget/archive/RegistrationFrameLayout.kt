package io.legado.app.ui.widget.archive

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.widget.FrameLayout
import io.legado.app.R

/**
 * 四角定位标记（Wenku8 ArchiveRegistration）。
 *
 * 原实现是 Flutter 的 CustomPainter，绘制 4 条 "⌜ ⌝ ⌞ ⌟" 折线，
 * 线宽 0.7px、长度取 min(7, 短边/3)。
 * Android 侧改为在 dispatchDraw 里画，保证压在子 View 之上。
 *
 * XML 用法：
 *   <io.legado.app.ui.widget.archive.RegistrationFrameLayout
 *       app:archiveMarkInset="0dp"
 *       app:archiveMarkLength="7dp"
 *       app:archiveMarkColor="@color/secondaryText" />
 */
class RegistrationFrameLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val markPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.SQUARE
        strokeWidth = ArchiveTokens.dp(context, ArchiveTokens.MARK_STROKE_DP)
        color = ArchiveTokens.muted(context)
    }

    var markLength: Float = ArchiveTokens.dp(context, 7f)
        set(value) {
            field = value
            invalidate()
        }

    var markInset: Float = 0f
        set(value) {
            field = value
            invalidate()
        }

    init {
        setWillNotDraw(false)
        val ta = context.obtainStyledAttributes(attrs, R.styleable.RegistrationFrameLayout)
        try {
            markLength = ta.getDimension(
                R.styleable.RegistrationFrameLayout_archiveMarkLength, markLength
            )
            markInset = ta.getDimension(
                R.styleable.RegistrationFrameLayout_archiveMarkInset, markInset
            )
            markPaint.strokeWidth = ta.getDimension(
                R.styleable.RegistrationFrameLayout_archiveMarkStroke, markPaint.strokeWidth
            )
            markPaint.color = ta.getColor(
                R.styleable.RegistrationFrameLayout_archiveMarkColor, markPaint.color
            )
        } finally {
            ta.recycle()
        }
    }

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        if (width <= 0 || height <= 0) return

        val inset = markInset + markPaint.strokeWidth / 2f
        val left = inset
        val top = inset
        val right = width - inset
        val bottom = height - inset
        val len = markLength.coerceAtMost(minOf(right - left, bottom - top) / 3f)
        if (len <= 0f) return

        // 左上
        canvas.drawLine(left, top, left + len, top, markPaint)
        canvas.drawLine(left, top, left, top + len, markPaint)
        // 右上
        canvas.drawLine(right - len, top, right, top, markPaint)
        canvas.drawLine(right, top, right, top + len, markPaint)
        // 左下
        canvas.drawLine(left, bottom - len, left, bottom, markPaint)
        canvas.drawLine(left, bottom, left + len, bottom, markPaint)
        // 右下
        canvas.drawLine(right - len, bottom, right, bottom, markPaint)
        canvas.drawLine(right, bottom - len, right, bottom, markPaint)
    }
}
