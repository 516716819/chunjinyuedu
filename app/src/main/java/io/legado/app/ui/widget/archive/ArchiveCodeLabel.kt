package io.legado.app.ui.widget.archive

import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.TypedValue
import androidx.appcompat.widget.AppCompatTextView

/**
 * 档案编号标签（Wenku8 ArchiveCode）。
 *
 * 视觉：monospace / 9sp / letterSpacing 0.12em / 次要文字色，
 * 形态固定为 "WK-00001"（5 位补零）。
 *
 * XML 用法：
 *   <io.legado.app.ui.widget.archive.ArchiveCodeLabel
 *       android:id="@+id/tv_code"
 *       android:layout_width="wrap_content"
 *       android:layout_height="wrap_content" />
 *
 * 代码里：tvCode.setFileCode(book.bookId)
 */
class ArchiveCodeLabel @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.textViewStyle
) : AppCompatTextView(context, attrs, defStyleAttr) {

    init {
        maxLines = 1
        includeFontPadding = false
        letterSpacing = 0.12f
        typeface = Typeface.MONOSPACE
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 9f)
        setTextColor(ArchiveTokens.muted(context))
        if (text.isNullOrEmpty()) {
            text = fileCode(0)
        }
    }

    fun setFileCode(id: Int) {
        text = fileCode(id)
    }

    companion object {
        fun fileCode(id: Int): String = "WK-" + id.toString().padStart(5, '0')
    }
}
