package io.legado.app.ui.book.read.page.delegate

import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Shader
import androidx.core.graphics.withTranslation
import io.legado.app.ui.book.read.page.ReadView
import io.legado.app.ui.book.read.page.entities.PageDirection
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.pow

/**
 * 档案风斜切翻页：整张旧页像一块带斜边的纸板被抽出，新页在底下斜向落位。
 *
 * 几何取自轻小说 ArchivePlaneTransition：
 *   slant = min(180dp, 宽 × 0.46)
 *   travel = (宽 + slant) × progress          纸沿横向走完整张纸的行程
 *   skew   = slant × min(1, progress × 2.4)   斜度在前 40% 长满，起步时无缝
 *
 * 视觉要点（前几版修过的三个坑）：
 *  1. 斜边方向：顶部领先底部半个 skew，纸边呈"右上—左下"的倾角，不能反过来。
 *  2. 层序：旧页是上层的纸片，必须整体平移出去（内容跟着动），只做裁切会让纸"没动过"。
 *  3. 松手后的缓动：legado 的 Scroller 用 LinearInterpolator，时长按位移比例算，
 *     半屏滑动只剩 78ms，肉眼看不到动画。这里固定 460ms 并对进度做 easeOut 重映射，
 *     且以松手瞬间的进度为锚点，避免松手那一下画面突跳。
 */
class SlantPageDelegate(readView: ReadView) : HorizontalPageDelegate(readView) {

    private val density = readView.resources.displayMetrics.density

    /** 松手后的落位时长。轻小说整段 620ms，这里只跑收尾，500ms 更跟手 */
    private val settleDuration = 500

    /** 松手瞬间的线性进度，缓动重映射的锚点 */
    private var releaseProgress = 0f

    /** 纸的横断面：抽走时露出来的一道暖白细边 */
    private val paperEdgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.6f * density
        color = 0xB3F6F4EC.toInt()
    }

    /** 纸边受光的高光，压在斜边上让切口不发闷 */
    private val lipPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 0.8f * density
        color = 0x66FFFFFF
    }

    private val clipPath = Path()
    private val shadowPath = Path()
    private val lipPath = Path()
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    /** DST_IN：只保留路径内的当前页像素，路径外被擦掉，形成斜切纸边 */
    private val keepPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
    }

    override fun onDraw(canvas: Canvas) {
        if (!isRunning) return
        val offsetX = touchX - startX
        // 反向越界时不绘制，交给基类的取消逻辑
        if ((mDirection == PageDirection.NEXT && offsetX > 0)
            || (mDirection == PageDirection.PREV && offsetX < 0)
        ) {
            return
        }
        val w = viewWidth.toFloat()
        val h = viewHeight.toFloat()
        if (w <= 0f || h <= 0f) return

        val linear = (abs(offsetX) / w).coerceIn(0f, 1f)
        if (linear <= 0f) return
        val progress = if (isStarted && !isCancel) settleEase(linear) else linear
        if (progress <= 0f) return

        val isNext = mDirection == PageDirection.NEXT
        val slant = min(180f * density, w * 0.46f)
        // 纸的行程：起步与手指 1:1 跟手，末段多带出半个斜边，保证滑到屏边时纸已完全离开
        val travel = abs(offsetX) + slant * progress
        // 斜度在前 40% 长满，起步第一帧不会突然缺一角
        val skew = slant * min(1f, progress * 2.4f)

        // 1. 底层：新页带一个斜向偏移，随进度收拢到原位，像纸片落下。
        //    drift 不能太小，否则两层贴在一起，翻页会显得只有一层皮。
        val incoming = if (isNext) nextRecorder else prevRecorder
        val driftX = if (isNext) 70f * density else -70f * density
        val driftY = 14f * density
        val sink = (1f - progress).pow(1.8f)
        canvas.withTranslation(driftX * sink, driftY * sink) {
            incoming.draw(this)
        }
        // 新页压在旧页底下，整张罩一层暗；纸移开后逐渐恢复。
        // 少了这层，两页亮度一致，看着像并排两栏而不是一叠纸。
        val dim = ((1f - progress) * 0.18f * 255f).toInt()
        if (dim > 0) {
            canvas.drawColor((dim shl 24) or 0x1A1714)
        }

        // 2. 上层：旧页整张平移出去，斜边只裁掉露出底下的那一角
        val half = skew * 0.5f
        clipPath.reset()
        lipPath.reset()
        if (isNext) {
            // 向左抽出，可见窗口在左半边，纸边斜向右上
            val topEdge = w - travel + half
            val bottomEdge = w - travel - half
            clipPath.moveTo(0f, 0f)
            clipPath.lineTo(topEdge, 0f)
            clipPath.lineTo(bottomEdge, h)
            clipPath.lineTo(0f, h)
            lipPath.moveTo(topEdge, 0f)
            lipPath.lineTo(bottomEdge, h)
        } else {
            // 向右抽出，可见窗口在右半边，纸边斜向左上
            val topEdge = travel + half
            val bottomEdge = travel - half
            clipPath.moveTo(w, 0f)
            clipPath.lineTo(topEdge, 0f)
            clipPath.lineTo(bottomEdge, h)
            clipPath.lineTo(w, h)
            lipPath.moveTo(topEdge, 0f)
            lipPath.lineTo(bottomEdge, h)
        }
        clipPath.close()

        // 注意：legado 的页面快照是 RenderNode，drawRenderNode 只认矩形裁剪，
        // clipPath 会被退化成竖切，所以必须走图层 + DST_IN。
        // 平移与擦除都在同一图层内完成：restore 之后再画 DST_IN 已经无效了。
        val shift = if (isNext) -travel else travel
        val layer = canvas.saveLayer(null, null)
        canvas.translate(shift, 0f)
        curRecorder.draw(canvas)
        canvas.translate(-shift, 0f)
        canvas.drawPath(clipPath, keepPaint)
        canvas.restoreToCount(layer)

        // 3. 纸的立体感靠一处不对称的明暗：纸边自身受光（内侧亮带），
        //    纸挡住了背后的光（外侧一片柔和投影）。两侧都压暗只会像描边。
        val anchorX = if (isNext) w - travel else travel
        val inward = if (isNext) -1f else 1f
        val litWidth = 9f * density
        val shadeWidth = 30f * density * (0.4f + 0.6f * progress)

        // 内侧：纸面沿边卷起来的那道受光，从纸边往里迅速衰减
        shadowPath.reset()
        shadowPath.moveTo(anchorX + half, 0f)
        shadowPath.lineTo(anchorX + half + inward * litWidth, 0f)
        shadowPath.lineTo(anchorX - half + inward * litWidth, h)
        shadowPath.lineTo(anchorX - half, h)
        shadowPath.close()
        shadowPaint.shader = LinearGradient(
            anchorX, 0f, anchorX + inward * litWidth, 0f,
            intArrayOf(0x3DFBF8F0, 0x00FBF8F0), null, Shader.TileMode.CLAMP
        )
        canvas.drawPath(shadowPath, shadowPaint)

        // 外侧：纸压在新页上的投影，宽而淡，是"纸浮起来"的主要线索
        shadowPath.reset()
        shadowPath.moveTo(anchorX + half, 0f)
        shadowPath.lineTo(anchorX + half - inward * shadeWidth, 0f)
        shadowPath.lineTo(anchorX - half - inward * shadeWidth, h)
        shadowPath.lineTo(anchorX - half, h)
        shadowPath.close()
        shadowPaint.shader = LinearGradient(
            anchorX, 0f, anchorX - inward * shadeWidth, 0f,
            intArrayOf(0x5A17140F, 0x0017140F), null, Shader.TileMode.CLAMP
        )
        canvas.drawPath(shadowPath, shadowPaint)
        shadowPaint.shader = null

        // 4. 收边：一道暖白细边 + 一点高光，纸的断面就出来了。
        //    斜边扫到屏外就不再画，免得起手/收尾留一根孤线。
        if (progress < 0.97f) {
            canvas.drawPath(lipPath, paperEdgePaint)
            canvas.drawPath(lipPath, lipPaint)
        }
    }

    /**
     * 松手后的进度缓动：以松手瞬间的线性进度为锚点，把剩余行程按 easeOut 铺开。
     * 直接用缓动函数变换会跳帧（0.4 → 0.75），所以只重映射 [releaseProgress, 1] 这一段。
     */
    private fun settleEase(linear: Float): Float {
        if (releaseProgress >= 0.999f) return 1f
        val span = 1f - releaseProgress
        val x = ((linear - releaseProgress) / span).coerceIn(0f, 1f)
        val eased = 1f - (1f - x).pow(2.4f)
        return releaseProgress + span * eased
    }

    override fun onAnimStop() {
        if (!isCancel) {
            readView.fillPage(mDirection)
        }
    }

    override fun onAnimStart(animationSpeed: Int) {
        val distanceX: Float
        when (mDirection) {
            PageDirection.NEXT -> distanceX =
                if (isCancel) {
                    var dis = viewWidth - startX + touchX
                    if (dis > viewWidth) {
                        dis = viewWidth.toFloat()
                    }
                    viewWidth - dis
                } else {
                    -(touchX + (viewWidth - startX))
                }

            else -> distanceX =
                if (isCancel) {
                    -(touchX - startX)
                } else {
                    viewWidth - (touchX - startX)
                }
        }
        // 缓动锚点：松手这一刻画面上纸已经走了多少
        releaseProgress = if (isCancel) 0f
        else (abs(touchX - startX) / viewWidth).coerceIn(0f, 1f)

        // 基类按 animationSpeed × 位移比例算时长，滑半屏只剩一百来毫秒。
        // 反推一个 speed，让落位固定压满 settleDuration。
        val distance = abs(distanceX)
        val speed = if (distance > 1f) {
            (settleDuration * viewWidth / distance).toInt()
        } else {
            settleDuration
        }
        startScroll(touchX.toInt(), 0, distanceX.toInt(), 0, speed.coerceIn(1, 200_000))
    }
}
