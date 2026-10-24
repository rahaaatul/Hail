package com.aistra.hail.views

import android.content.Context
import android.util.AttributeSet
import android.view.ContextMenu.ContextMenuInfo
import android.view.View
import androidx.recyclerview.widget.RecyclerView

/**
 * Customized RecyclerView with ContextMenu support.
 * */
class HRecyclerView : RecyclerView {
    private var contextMenuInfo: RecyclerViewContextMenuInfo? = null

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    override fun getContextMenuInfo(): ContextMenuInfo? = contextMenuInfo

    override fun showContextMenuForChild(originalView: View?): Boolean {
        val result = if (originalView != null && adapter != null) {
            val longPressPosition = getChildAdapterPosition(originalView)
            if (longPressPosition >= 0) {
                contextMenuInfo = RecyclerViewContextMenuInfo(longPressPosition, getChildViewHolder(originalView))
                super.showContextMenuForChild(originalView)
            } else {
                false
            }
        } else {
            false
        }
        return result
    }

    class RecyclerViewContextMenuInfo(val position: Int, val viewHolder: ViewHolder) : ContextMenuInfo
}
