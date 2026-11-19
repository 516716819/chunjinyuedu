package io.legado.app.ui.widget.archive

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Path
import android.graphics.Typeface
import android.text.TextPaint
import android.text.TextUtils
import android.util.AttributeSet
import android.view.View
import kotlin.math.min

/**
 * 档案风入场幕布（对应轻小说 ArchivePlaneTransition 的 incoming 平面）。
 *
 * 不是整页淡入：一块与纸张同色的幕布上开一条斜切洞口，洞口从左向右推进，
 * 斜边倾角上限 180dp，与轻小说 0.46*width / 180 的上限一致。
 * 幕布下方的内容同时做 (-24dp, 12dp) → 0 的落位位移，所以文字从第一帧就是清晰的。
 */
class ArchiveCurtainView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context) {

    private val paperPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val edgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.2f * resources.displayMetrics.density
    }

    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val screenPath = Path()
    private val holePath = Path()
    private val shadowPath = Path()

    private val labelPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.MONOSPACE
        textSize = 13f * resources.displayMetrics.scaledDensity
    }
    private val codePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.MONOSPACE
        textSize = 9f * resources.displayMetrics.scaledDensity
    }

    private val density = resources.displayMetrics.density
    private val slantMax = 180f * density
    private val travelX = 24f * density
    private val travelY = 12f * density

    /** 0 = 完全遮住，1 = 完全揭开 */
    var progress: Float = 1f
        set(value) {
            field = value.coerceIn(0f, 1f)
            invalidate()
        }

    var paperColor: Int = 0xFFE7E4DA.toInt()
        set(value) {
            field = value
            paperPaint.color = value
            invalidate()
        }

    /** 幕布上的墨色：纸边、封套编号与书名都由它派生 */
    var inkColor: Int = 0xFF3A3A36.toInt()
        set(value) {
            field = value
            edgePaint.color = withAlpha(value, 0.24f)
            labelPaint.color = withAlpha(value, 0.82f)
            codePaint.color = withAlpha(value, 0.48f)
            invalidate()
        }

    /** 幕布上印的书名与档案编号，像一张档案封套；揭开时随纸幕一起被切走 */
    var label: String = ""
    var fileCode: String = ""
        set(value) {
            field = value
            invalidate()
        }

    /** 内容落位位移，供入场动画驱动 */
    fun applyLanding(target: View) {
        val remaining = 1f - progress
        target.translationX = -travelX * remaining
        target.translationY = travelY * remaining
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return
        if (progress >= 1f) return

        val slant = min(slantMax, w * 0.46f)
        val edge = (w + slant) * progress

        screenPath.reset()
        screenPath.addRect(0f, 0f, w, h, Path.Direction.CW)

        holePath.reset()
        holePath.moveTo(0f, 0f)
        holePath.lineTo(edge, 0f)
        holePath.lineTo(edge - slant, h)
        holePath.lineTo(0f, h)
        holePath.close()

        val save = canvas.save()
        canvas.clipPath(screenPath)
        canvas.clipOutPath(holePath)
        canvas.drawColor(paperPaint.color)
        // 洞口从左向右推进，文字放在右侧才能在整段揭示里都看得见
        val textX = w * 0.70f
        if (fileCode.isNotEmpty()) {
            canvas.drawText(fileCode, textX, h * 0.46f, codePaint)
        }
        if (label.isNotEmpty()) {
            val maxWidth = w * 0.5f
            val text = TextUtils.ellipsize(
                label, labelPaint, maxWidth, TextUtils.TruncateAt.END
            ).toString()
            canvas.drawText(text, textX, h * 0.46f + 26f * density, labelPaint)
        }
        canvas.restoreToCount(save)

        // 纸厚：沿斜边往幕布内侧 26dp 的柔和投影，再压一条墨线
        val depth = 26f * density
        shadowPath.reset()
        shadowPath.moveTo(edge, 0f)
        shadowPath.lineTo(edge - slant, h)
        shadowPath.lineTo(edge - slant + depth, h)
        shadowPath.lineTo(edge + depth, 0f)
        shadowPath.close()
        shadowPaint.shader = LinearGradient(
            edge, 0f, edge + depth, 0f,
            intArrayOf(0x4D1A1A14, 0x001A1A14), null, Shader.TileMode.CLAMP
        )
        canvas.drawPath(shadowPath, shadowPaint)
        shadowPaint.shader = null
        canvas.drawPath(holePath, edgePaint)
    }

    private fun withAlpha(color: Int, alpha: Float): Int {
        val a = (alpha.coerceIn(0f, 1f) * 255).toInt()
        return (a shl 24) or (color and 0x00FFFFFF)
    }

}
