@file:Suppress("NOTHING_TO_INLINE")

package uk.whitecrescent.waqti.frontend

import android.text.Editable
import android.text.TextWatcher
import androidx.core.view.isVisible
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton

open class SimpleTextWatcher : TextWatcher {

    override fun afterTextChanged(editable: Editable?) {}

    override fun beforeTextChanged(string: CharSequence?, start: Int, count: Int, after: Int) {}

    override fun onTextChanged(string: CharSequence?, start: Int, before: Int, count: Int) {}
}

open class SimpleItemTouchHelperCallback : ItemTouchHelper.Callback() {

    override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
        return makeMovementFlags(ItemTouchHelper.UP or ItemTouchHelper.DOWN or ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT, 0)
    }

    override fun isLongPressDragEnabled() = true

    override fun isItemViewSwipeEnabled() = false

    override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder,
                        target: RecyclerView.ViewHolder): Boolean {
        return true
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        /*This will never be called as we do not support swiping*/
    }

    override fun interpolateOutOfBoundsScroll(recyclerView: RecyclerView, viewSize: Int,
                                              viewSizeOutOfBounds: Int, totalSize: Int,
                                              msSinceStartScroll: Long): Int {
        return super.interpolateOutOfBoundsScroll(
                recyclerView, viewSize, viewSizeOutOfBounds, totalSize, 1500)
    }

}

open class FABOnScrollListener(val fab: FloatingActionButton, val orientation: Orientation)
    : RecyclerView.OnScrollListener() {

    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {

        /*val lastItemPos = recyclerView.adapter?.lastPosition
        val lastVisiblePos = (recyclerView.layoutManager as LinearLayoutManager?)
                ?.findLastCompletelyVisibleItemPosition()*/

        // TODO: 12-Jun-19  Issue #57,
        //  The hiding FABs need to be always showing when the list is not exceeding the size
        //  limit required to scroll
        // so something like make sure the last item in the list is fully visible and
        // is not below the bottom of the list so that a scroll is needed

        when (orientation) {
            Orientation.HORIZONTAL -> {
                if (dx > 0 && fab.isVisible) fab.hide()
                else if (dx < 0 && !fab.isVisible) fab.show()
            }
            Orientation.VERTICAL -> {
                if (dy > 0 && fab.isVisible) fab.hide()
                else if (dy < 0 && !fab.isVisible) fab.show()
            }
        }
    }

    enum class Orientation {
        HORIZONTAL, VERTICAL
    }
}

data class ChangedPositionPair(var positionChanged: Boolean,
                               var position: Int) {

    inline fun changeTo(pair: Pair<Boolean, Int>) = changeTo(pair.first, pair.second)

    inline fun changeTo(positionChanged: Boolean, position: Int) {
        this.positionChanged = positionChanged
        this.position = position
    }
}

/**
 * Just a utility to show us where there are Fragment changes happening
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.EXPRESSION)
annotation class GoToFragment