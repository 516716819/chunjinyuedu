package io.legado.app.ui.main.bookshelf.style1.books

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.legado.app.data.entities.Book
import io.legado.app.databinding.ItemBookshelfPickBinding
import io.legado.app.ui.main.bookshelf.updateBookshelfReadProgress
import io.legado.app.utils.visible
import kotlin.math.absoluteValue

/**
 * 顶部"抽书"条：横向排列书架封面，居中项为当前选中的书。
 *
 * 视觉对应 Wenku8 书架里被抽出的一本：选中项恢复原尺寸并加 1dp 描边，
 * 其余项缩小到 0.84 并压到 45% 不透明度，留出档案纸的呼吸感。
 */
class BooksPickAdapter(
    context: Context,
    private val onOpen: (Book) -> Unit,
    private val onFocus: (Int) -> Unit,
) : RecyclerView.Adapter<BooksPickAdapter.Holder>() {

    private val inflater = LayoutInflater.from(context)
    private val items = arrayListOf<Book>()
    private var selected = 0

    init {
        setHasStableIds(true)
    }

    class Holder(val binding: ItemBookshelfPickBinding) : RecyclerView.ViewHolder(binding.root)

    override fun getItemId(position: Int): Long =
        items.getOrNull(position)?.bookUrl?.hashCode()?.toLong() ?: RecyclerView.NO_ID

    override fun getItemCount(): Int = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder =
        Holder(ItemBookshelfPickBinding.inflate(inflater, parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val book = items[position]
        holder.binding.run {
            tvPickCode.setFileCode(book.bookUrl.hashCode().absoluteValue % 100000)
            tvPickName.text = book.name
            ivPickCover.load(book, false)
            pbPickProgress.updateBookshelfReadProgress(book)
            applyFocus(root, flPickFace, vwPickFrame, position == selected, false)
            flPickFace.translationY = 0f
            root.setOnClickListener {
                // 抽出感：封面先上浮 6dp 再落回，同时打开书籍
                val lift = io.legado.app.ui.widget.archive.ArchiveTokens.dp(root, 6f)
                flPickFace.animate().translationY(-lift).setDuration(110L)
                    .withEndAction {
                        flPickFace.animate().translationY(0f).setDuration(180L).start()
                    }.start()
                onOpen(book)
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun submit(books: List<Book>) {
        val sameOrder = items.size == books.size &&
                items.indices.all { items[it].bookUrl == books[it].bookUrl }
        items.clear()
        items.addAll(books)
        if (selected >= items.size) selected = (items.size - 1).coerceAtLeast(0)
        if (sameOrder) {
            notifyItemRangeChanged(0, items.size, PAYLOAD_PROGRESS)
        } else {
            notifyDataSetChanged()
        }
    }

    fun getItem(position: Int): Book? = items.getOrNull(position)

    fun indexOf(bookUrl: String): Int = items.indexOfFirst { it.bookUrl == bookUrl }

    val selectedIndex: Int get() = selected

    /**
     * 设置选中的书。
     * @param notifyFocus 是否把变化回传给页面（用户操作时为 true，内部联动时为 false）
     */
    fun setSelected(index: Int, notifyFocus: Boolean = false) {
        if (items.isEmpty()) return
        val target = index.coerceIn(0, items.size - 1)
        if (target == selected) return
        val previous = selected
        selected = target
        notifyItemChanged(previous, PAYLOAD_FOCUS)
        notifyItemChanged(target, PAYLOAD_FOCUS)
        if (notifyFocus) onFocus(target)
    }

    override fun onBindViewHolder(holder: Holder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads)
            return
        }
        val book = items[position]
        holder.binding.run {
            when {
                payloads.contains(PAYLOAD_FOCUS) -> {
                    applyFocus(root, flPickFace, vwPickFrame, position == selected, true)
                }

                payloads.contains(PAYLOAD_PROGRESS) -> {
                    pbPickProgress.updateBookshelfReadProgress(book)
                }

                else -> super.onBindViewHolder(holder, position, payloads)
            }
        }
    }

    private fun applyFocus(
        root: android.view.View,
        face: android.view.View,
        frame: android.view.View,
        focused: Boolean,
        animate: Boolean,
    ) {
        val scale = if (focused) 1f else 0.84f
        val alpha = if (focused) 1f else 0.45f
        if (animate) {
            face.animate().scaleX(scale).scaleY(scale).setDuration(180L).start()
            root.animate().alpha(alpha).setDuration(180L).start()
        } else {
            face.scaleX = scale
            face.scaleY = scale
            root.alpha = alpha
        }
        frame.visible(focused)
    }

    companion object {
        private const val PAYLOAD_FOCUS = "focus"
        private const val PAYLOAD_PROGRESS = "progress"
    }

}
