package io.legado.app.ui.widget.archive

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import io.legado.app.R

/**
 * 廉价版"毛玻璃"：把目标 View 的当前画面分两级降采样后画到自己身上。
 *
 * 为什么不用 RenderEffect：minSdk 26，`RenderEffect.createBlurEffect` 要 31；
 * Android 11 及以下没有 backdrop blur。这里用「先缩到 1/3、再缩到 1/9」的两跳采样，
 * 两次双线性过滤叠出来的衰减比一次 1/6 更接近高斯，块状感明显更少；
 * 再叠一层偏薄的纸色 tint + 顶部高光 + 内侧亮边，得到轻小说那种悬浮玻璃条。
 *
 * 只在目标滚动停下后刷新快照（220ms 防抖），拖动过程沿用上一帧，避免每帧截屏。
 */
class ArchiveFrostedView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private val density = resources.displayMetrics.density
    private fun dp(v: Float) = v * density

    private var target: View? = null
    private var snapshot: Bitmap? = null

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        isFilterBitmap = true
        isDither = true
    }
    private val tint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val sheen = Paint(Paint.ANTI_ALIAS_FLAG)
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = maxOf(1f, dp(0.7f))
        isAntiAlias = true
    }
    private val matrix = Matrix()
    private val clip = Path()
    private val shader = BitmapShader(
        Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888),
        Shader.TileMode.CLAMP,
        Shader.TileMode.CLAMP
    )

    var cornerRadius: Float = dp(24f)
        set(value) {
            field = value
            invalidate()
        }

    private val surfaceColor: Int
        get() = ContextCompat.getColor(context, R.color.background_menu)

    private val lineColor: Int
        get() = ContextCompat.getColor(context, R.color.divider)

    fun attachTo(view: View) {
        target = view
        view.viewTreeObserver.addOnScrollChangedListener { scheduleRefresh() }
        post { capture() }
    }

    private var pending: Runnable? = null

    /**
     * 滚动中不断重置计时，只有停手 220ms 才截一次，避免拖动时每帧做软件渲染。
     */
    fun scheduleRefresh() {
        pending?.let { removeCallbacks(it) }
        val task = Runnable { capture() }
        pending = task
        postDelayed(task, 220L)
    }

    private fun capture() {
        val view = target ?: return
        if (width <= 0 || height <= 0) return
        // 第一跳：1/3，第二跳：1/9。两跳之后放大回去的过渡带足够宽，接近高斯模糊。
        val stage1 = 3f
        val stage2 = 3f
        val w1 = (width / stage1).toInt().coerceAtLeast(1)
        val h1 = (height / stage1).toInt().coerceAtLeast(1)
        val mid = Bitmap.createBitmap(w1, h1, Bitmap.Config.ARGB_8888)
        val midCanvas = Canvas(mid)
        midCanvas.scale(1f / stage1, 1f / stage1)
        // 只截 target 中与本 View 重叠的那段区域：把 target 画面平移到本 View 坐标系，
        // 使得 target 上位于 (left, top) 处的像素恰好落到本 View 的 (0,0)。
        // 例如本 View 是贴底的底部毛玻璃条时，(left, top) 指向 target 底部同高度处，
        // 于是 target 底部的内容会被画进来，整条毛玻璃透出的是正下方滚过的内容。
        midCanvas.translate((left - view.left).toFloat(), (top - view.top).toFloat())
        runCatching { view.draw(midCanvas) }

        val w2 = (w1 / stage2).toInt().coerceAtLeast(1)
        val h2 = (h1 / stage2).toInt().coerceAtLeast(1)
        val small = Bitmap.createBitmap(w2, h2, Bitmap.Config.ARGB_8888)
        val smallCanvas = Canvas(small)
        smallCanvas.scale(w2 / w1.toFloat(), h2 / h1.toFloat())
        smallCanvas.drawBitmap(mid, 0f, 0f, paint)
        mid.recycle()

        snapshot?.recycle()
        snapshot = small
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        // 返回首页/重建 View 时尚未布局，宽高可能为 0，
        // 此时 addRoundRect / LinearGradient / setScale(0) 都会抛异常。
        if (w <= 0f || h <= 0f) return
        clip.reset()
        clip.addRoundRect(0f, 0f, w, h, cornerRadius, cornerRadius, Path.Direction.CW)

        val save = canvas.save()
        canvas.clipPath(clip)

        val bitmap = snapshot
        if (bitmap != null && !bitmap.isRecycled) {
            matrix.reset()
            matrix.setScale(w / bitmap.width, h / bitmap.height)
            shader.setLocalMatrix(matrix)
            paint.shader = shader
            canvas.drawRect(0f, 0f, w, h, paint)
            paint.shader = null
        }

        // 纸色 tint：压住底下的字形轮廓，只留色块，才是玻璃而不是半透明蒙版
        tint.color = withAlpha(surfaceColor, 0.66f)
        canvas.drawRect(0f, 0f, w, h, tint)

        // 玻璃厚度：顶部 45% 高度叠一层很淡的白，模拟受光面
        sheen.shader = LinearGradient(
            0f, 0f, 0f, h * 0.45f,
            intArrayOf(withAlpha(Color.WHITE, 0.13f), withAlpha(Color.WHITE, 0f)),
            null, Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, w, h * 0.45f, sheen)
        sheen.shader = null

        canvas.restoreToCount(save)

        // 边缘：上缘偏亮、下缘偏暗，形成一条有厚度的玻璃收边
        stroke.color = withAlpha(Color.WHITE, 0.55f)
        canvas.save()
        canvas.clipRect(0f, 0f, w, h * 0.5f)
        canvas.drawRoundRect(0f, 0f, w, h, cornerRadius, cornerRadius, stroke)
        canvas.restore()
        stroke.color = withAlpha(lineColor, 0.55f)
        canvas.save()
        canvas.clipRect(0f, h * 0.5f, w, h)
        canvas.drawRoundRect(0f, 0f, w, h, cornerRadius, cornerRadius, stroke)
        canvas.restore()
    }

    override fun onDetachedFromWindow() {
        pending?.let { removeCallbacks(it) }
        pending = null
        snapshot?.recycle()
        snapshot = null
        super.onDetachedFromWindow()
    }

    private fun withAlpha(color: Int, alpha: Float): Int {
        val a = (Color.alpha(color) * alpha).toInt().coerceIn(0, 255)
        return Color.argb(a, Color.red(color), Color.green(color), Color.blue(color))
    }

}
