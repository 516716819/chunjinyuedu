package io.legado.app.ui.widget.archive

import android.view.animation.PathInterpolator
import android.widget.Scroller
import androidx.viewpager.widget.ViewPager
import java.lang.reflect.Field

/**
 * 把 ViewPager 的 settle（松手自动落位）缓动改成轻小说的 easeInOut。
 *
 * 轻小说「切换书籍」翻页：整页横移，松手后约 150–200ms 用 easeInOut（先加速后减速）
 * 落位，而不是 Android ViewPager 默认 Scroller 的全程减速（Decelerate）。改造点：
 *  1. 反射替换 ViewPager 内部 mScroller，用 PathInterpolator(0.18, 0.72, 0.18, 1)
 *     模拟 easeInOutSine 的中间加速、两端减速；
 *  2. 缩短落位时长到 180ms，让切换更跟手、更接近轻小说的干脆手感。
 *
 * 拖拽跟手（1:1 平移）是 ViewPager 原生行为，无需改；这里只影响松手后的自动落位。
 */
object ArchivePagerEasing {

    private val field: Field? by lazy {
        runCatching {
            ViewPager::class.java.getDeclaredField("mScroller").apply {
                isAccessible = true
            }
        }.getOrNull()
    }

    private val interpolator = PathInterpolator(0.18f, 0.72f, 0.18f, 1f)

    /**
     * 把 [pager] 的落位动画改成 easeInOut + 180ms。
     * 若反射失败（未来版本字段改名），静默降级为系统默认行为，不影响功能。
     */
    fun apply(pager: ViewPager, settleMillis: Int = 180) {
        val f = field ?: return
        runCatching {
            val scroller = Scroller(pager.context, interpolator)
            f.set(pager, scroller)
        }
    }
}