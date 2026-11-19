package io.legado.app.ui.widget.archive

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import io.legado.app.R

/**
 * 页眉网格底纹（Wenku8 ArchiveBackdrop）。
 *
 * 原实现要点（archive_design.dart / _ArchiveBackdropPainter）：
 *  - 只覆盖页眉区域（约 top + 240dp），阅读画布保持干净
 *  - 48dp 网格线，ink @ 4.5% 不透明度
 *  - 两片斜切"档案面"：从 right 42% / right 60% 起，milk @ 42% / 28%
 *  - 顶部两侧各一个十字定位标记，ink @ 15%
 *  - 全程无 blur、无动画（这点很关键：不依赖 RenderEffect，Android 5.0 也能跑）
 *
 * 用法：作为页眉区域背景层放在 TitleBar 之下，高度建议 240dp。
 */
class ArchiveBackdropView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val gutter = ArchiveTokens.dp(context, 24f)

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = ArchiveTokens.dp(context, 0.5f)
        color = withAlpha(ArchiveTokens.ink(context), 0.045f)
    }

    private val filmPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = withAlpha(ArchiveTokens.MILK, 0.42f)
    }

    private val crossPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = ArchiveTokens.dp(context, 0.5f)
        color = withAlpha(ArchiveTokens.ink(context), 0.15f)
    }

    private var gridStep = ArchiveTokens.dp(context, ArchiveTokens.GRID_STEP_DP)
    private var filmColor = ArchiveTokens.MILK

    init {
        val ta = context.obtainStyledAttributes(attrs, R.styleable.ArchiveBackdropView)
        try {
            gridStep = ta.getDimension(
                R.styleable.ArchiveBackdropView_archiveGridStep, gridStep
            )
            filmColor = ta.getColor(
                R.styleable.ArchiveBackdropView_archiveFilmColor, filmColor
            )
        } finally {
            ta.recycle()
        }
        filmPaint.color = withAlpha(filmColor, 0.42f)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        val offset = ArchiveTokens.dp(this, 18f)

        // 第一片档案面：右侧 42% 起，斜切
        canvas.drawPath(
            Path().apply {
                moveTo(w * .42f, 0f)
                lineTo(w, 0f)
                lineTo(w, h * .82f)
                lineTo(w * .78f, h * .82f)
                close()
            },
            filmPaint
        )

        // 第二片档案面：右侧 60% 起，颜色更淡
        filmPaint.color = withAlpha(filmColor, 0.28f)
        canvas.drawPath(
            Path().apply {
                moveTo(w * .60f, offset)
                lineTo(w, offset)
                lineTo(w, h)
                lineTo(w * .92f, h)
                close()
            },
            filmPaint
        )

        // 网格
        var x = gutter
        while (x < w) {
            canvas.drawLine(x, 0f, x, h, gridPaint)
            x += gridStep
        }
        var y = ArchiveTokens.dp(this, 4f)
        while (y < h) {
            canvas.drawLine(gutter, y, w - gutter, y, gridPaint)
            y += gridStep
        }

        // 顶部十字定位标记
        val crossY = ArchiveTokens.dp(this, 60f)
        val half = ArchiveTokens.dp(this, 3f)
        canvas.drawLine(gutter - half, crossY, gutter + half, crossY, crossPaint)
        canvas.drawLine(gutter, crossY - half, gutter, crossY + half, crossPaint)
        canvas.drawLine(w - gutter - half, crossY, w - gutter + half, crossY, crossPaint)
        canvas.drawLine(w - gutter, crossY - half, w - gutter, crossY + half, crossPaint)
    }

    private fun withAlpha(color: Int, alpha: Float): Int = Color.argb(
        (alpha.coerceIn(0f, 1f) * 255f).toInt(),
        Color.red(color),
        Color.green(color),
        Color.blue(color)
    )
}
