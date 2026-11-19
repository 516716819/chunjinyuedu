package io.legado.app.ui.widget.archive

import android.content.Context
import android.graphics.Outline
import android.view.View
import android.view.ViewOutlineProvider
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import io.legado.app.R

/**
 * Wenku8 设计令牌的 Android 侧映射。
 *
 * 数值来源：Wenku8_Flutter/lib/archive_design.dart（ArchiveDesign）
 * 与 lib/app_palette.dart（M3 seed 0xff607d71）。
 *
 * 约定：主题相关的颜色一律走 @color/ 资源（见 02-res/values/colors.xml），
 * 这里的常量只用于自定义 View 里需要硬编码兜底的场合。
 */
object ArchiveTokens {

    @ColorInt val PAPER = 0xFFF4F3EF.toInt()
    @ColorInt val SURFACE = 0xFFFDFCFA.toInt()
    @ColorInt val MILK = 0xFFFEFDF8.toInt()
    @ColorInt val INK = 0xFF1A1A17.toInt()
    @ColorInt val SAGE = 0xFF607D71.toInt()

    @ColorInt val NIGHT_BG = 0xFF191919.toInt()
    @ColorInt val NIGHT_SURFACE = 0xFF242424.toInt()
    @ColorInt val NIGHT_RAISED = 0xFF2D2D2D.toInt()
    @ColorInt val NIGHT_INK = 0xFFF2F2F2.toInt()
    @ColorInt val NIGHT_MUTED = 0xFFB7B7B7.toInt()

    const val PANEL_RADIUS_DP = 4f
    const val COVER_RADIUS_DP = 3f
    const val GRID_STEP_DP = 48f
    const val MARK_STROKE_DP = 0.7f

    fun dp(context: Context, value: Float): Float =
        value * context.resources.displayMetrics.density

    fun dp(view: View, value: Float): Float = dp(view.context, value)

    @ColorInt
    fun ink(context: Context): Int = ContextCompat.getColor(context, R.color.primaryText)

    @ColorInt
    fun muted(context: Context): Int = ContextCompat.getColor(context, R.color.secondaryText)

    @ColorInt
    fun paper(context: Context): Int = ContextCompat.getColor(context, R.color.background)

    /**
     * 把任意 View 裁成 3dp 圆角（Wenku8 的 coverRadius）。
     * legado 的 CoverImageView 内部写死 12f，见 05-patches 里的补丁；
     * 这里提供一个不改控件也能生效的兜底方案。
     */
    fun clipToCover(view: View) {
        val radius = dp(view, COVER_RADIUS_DP)
        view.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(v: View, outline: Outline) {
                outline.setRoundRect(0, 0, v.width, v.height, radius)
            }
        }
        view.clipToOutline = true
        view.invalidateOutline()
    }

    // Wenku8: ArchiveDesign.fileCode() -> "WK-00001"
    fun fileCode(id: Int): String = "WK-" + id.toString().padStart(5, '0')
}
