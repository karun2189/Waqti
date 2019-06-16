package uk.whitecrescent.waqti.frontend.customview.recyclerviews

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.android.synthetic.main.task_list.view.*
import org.jetbrains.anko.doAsync
import uk.whitecrescent.waqti.R
import uk.whitecrescent.waqti.backend.persistence.Caches
import uk.whitecrescent.waqti.backend.task.ID
import uk.whitecrescent.waqti.clearFocusAndHideSoftKeyboard
import uk.whitecrescent.waqti.commitTransaction
import uk.whitecrescent.waqti.doInBackground
import uk.whitecrescent.waqti.frontend.CREATE_TASK_FRAGMENT
import uk.whitecrescent.waqti.frontend.GoToFragment
import uk.whitecrescent.waqti.frontend.SimpleItemTouchHelperCallback
import uk.whitecrescent.waqti.frontend.VIEW_LIST_FRAGMENT
import uk.whitecrescent.waqti.frontend.fragments.create.CreateTaskFragment
import uk.whitecrescent.waqti.frontend.fragments.view.ViewListFragment
import uk.whitecrescent.waqti.mainActivity
import uk.whitecrescent.waqti.verticalFABOnScrollListener
import kotlin.math.roundToInt

open class BoardView
@JvmOverloads constructor(context: Context,
                          attributeSet: AttributeSet? = null,
                          defStyle: Int = 0) : RecyclerView(context, attributeSet, defStyle) {

    inline val boardAdapter: BoardAdapter
        get() = this.adapter as BoardAdapter

    val taskListAdapters = ArrayList<TaskListAdapter>()

    lateinit var itemTouchHelper: ItemTouchHelper
    lateinit var pagerSnapHelper: PagerSnapHelper

    init {
        layoutManager = LinearLayoutManager(context, HORIZONTAL, false)
    }

    override fun setAdapter(_adapter: Adapter<*>?) {
        super.setAdapter(_adapter)
        require(_adapter != null && _adapter is BoardAdapter) {
            "Adapter must be non null and a BoardAdapter, passed in $_adapter"
        }

        attachHelpers()
    }

    private fun attachHelpers() {

        itemTouchHelper = ItemTouchHelper(object : SimpleItemTouchHelperCallback() {

            override fun isLongPressDragEnabled() = false

            override fun clearView(recyclerView: RecyclerView, viewHolder: ViewHolder) {
                super.clearView(recyclerView, viewHolder)
                if (viewHolder is BoardViewHolder) {
                    viewHolder.itemView.alpha = 1F
                }
            }

            override fun onSelectedChanged(viewHolder: ViewHolder?, actionState: Int) {
                super.onSelectedChanged(viewHolder, actionState)
                if (viewHolder != null && viewHolder is BoardViewHolder) {
                    viewHolder.itemView.alpha = 0.7F
                }
            }

            override fun onMoved(recyclerView: RecyclerView, viewHolder: ViewHolder, fromPos: Int,
                                 target: ViewHolder, toPos: Int, x: Int, y: Int) {
                super.onMoved(recyclerView, viewHolder, fromPos, target, toPos, x, y)

                boardAdapter.apply {
                    board.move(fromPos, toPos).update()
                    matchOrder()
                    notifyItemMoved(fromPos, toPos)
                }
                mainActivity.viewModel.boardPosition = true to toPos
            }

        })
        itemTouchHelper.attachToRecyclerView(this)

        pagerSnapHelper = object : PagerSnapHelper() {
            override fun findTargetSnapPosition(layoutManager: LayoutManager?, velocityX: Int, velocityY: Int): Int {
                val currentBoardPos = super.findTargetSnapPosition(layoutManager, velocityX, velocityY)
                mainActivity.viewModel.boardPosition = true to currentBoardPos
                return currentBoardPos
            }
        }
        pagerSnapHelper.attachToRecyclerView(this)
    }


    fun addListAdapter(taskListAdapter: TaskListAdapter): TaskListAdapter {
        return taskListAdapter.also {
            taskListAdapters.add(it)
        }
    }

    fun addListAdapterIfNotExists(taskListAdapter: TaskListAdapter): TaskListAdapter {
        return taskListAdapter.also {
            if (!doesListAdapterExist(it.taskListID)) taskListAdapters.add(it)
        }
    }

    fun removeListAdapterIfExists(taskListAdapter: TaskListAdapter) {
        getListAdapter(taskListAdapter.taskListID).also {
            if (it != null) taskListAdapters.remove(it)
        }
    }

    fun getListAdapter(taskListID: ID): TaskListAdapter? {
        return taskListAdapters.find { it.taskListID == taskListID }
    }

    fun doesListAdapterExist(taskListID: ID): Boolean {
        return getListAdapter(taskListID) != null
    }

    fun getOrCreateListAdapter(taskListID: ID): TaskListAdapter {
        return getListAdapter(taskListID) ?: addListAdapter(TaskListAdapter(taskListID))
    }

}

class BoardAdapter(val boardID: ID) : RecyclerView.Adapter<BoardViewHolder>() {

    val board = Caches.boards[boardID]

    lateinit var boardView: BoardView

    init {
        this.setHasStableIds(true)
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        require(recyclerView is BoardView) {
            "Recycler View attached to a BoardAdapter must be a BoardView"
        }
        boardView = recyclerView

        // TODO: 15-Jun-19 Check!!
        doAsync {
            board.forEach {
                boardView.getOrCreateListAdapter(it.id)
            }

            matchOrder()
        }

    }

    override fun getItemCount(): Int {
        return board.size
    }

    override fun getItemId(position: Int): Long {
        return board[position].id
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BoardViewHolder {
        return BoardViewHolder(
                LayoutInflater.from(parent.context)
                        .inflate(R.layout.task_list, parent, false)
        )
    }

    override fun onBindViewHolder(holder: BoardViewHolder, position: Int) {

        // Adapters get created and destroyed because their associated views do too, actually
        // more specifically, they get recycled

        holder.taskListView.adapter = boardView.getOrCreateListAdapter(board[position].id)

        holder.itemView.taskList_rootView.updateLayoutParams {

            val percent = boardView.mainActivity
                    .waqtiPreferences.taskListWidth / 100.0

            width = (boardView.mainActivity.dimensions.first.toFloat() * percent).roundToInt()
        }

        holder.header.doInBackground {
            setOnLongClickListener {
                this@BoardAdapter.boardView.itemTouchHelper.startDrag(holder)
                true
            }
            text = board[position].name
            setOnClickListener {
                @GoToFragment
                it.mainActivity.supportFragmentManager.commitTransaction {

                    it.mainActivity.viewModel.listID = board[holder.adapterPosition].id

                    it.clearFocusAndHideSoftKeyboard()

                    addToBackStack("")
                    replace(R.id.fragmentContainer, ViewListFragment(), VIEW_LIST_FRAGMENT)
                }
            }
        }
        holder.addButton.doInBackground {
            setOnClickListener {

                @GoToFragment
                it.mainActivity.supportFragmentManager.commitTransaction {

                    it.mainActivity.viewModel.boardID = this@BoardAdapter.boardID
                    it.mainActivity.viewModel.listID = holder.taskListView.listAdapter.taskListID

                    it.clearFocusAndHideSoftKeyboard()

                    replace(R.id.fragmentContainer, CreateTaskFragment(), CREATE_TASK_FRAGMENT)
                    addToBackStack(null)
                }
            }
        }
        holder.taskListView.addOnScrollListener(holder.addButton.verticalFABOnScrollListener)


    }

    fun matchOrder() {
        doAsync {
            val taskListAdaptersCopy = ArrayList(boardView.taskListAdapters)
            if (doesNotMatchOrder()) {

                board.filter { taskList -> taskList.id in taskListAdaptersCopy.map { it?.taskListID } }
                        .mapIndexed { index, taskList -> index to taskList }.toMap()
                        .forEach { entry ->
                            val (index, taskList) = entry

                            boardView.taskListAdapters[index] =
                                    taskListAdaptersCopy.find { it?.taskListID == taskList.id }!!
                        }

            }
        }
    }

    private fun doesNotMatchOrder(): Boolean {
        val adapterIDs = boardView.taskListAdapters.map { it.taskListID }

        return adapterIDs != board.take(adapterIDs.size).map { it.id }
    }
}


class BoardViewHolder(view: View) : ViewHolder(view) {
    val header: TextView = itemView.taskListHeader_textView
    val taskListView: TaskListView = itemView.taskList_recyclerView
    val addButton: FloatingActionButton = itemView.taskListFooter_textView
}

class PreCachingLayoutManager(context: Context,
                              @RecyclerView.Orientation
                              orientation: Int = HORIZONTAL,
                              reverseLayout: Boolean = false,
                              private val extraLayoutSpacePx: Int = 600) :
        LinearLayoutManager(context, orientation, reverseLayout) {

    @Deprecated("Use calculateExtraLayoutSpace instead")
    override fun getExtraLayoutSpace(state: RecyclerView.State): Int {
        return extraLayoutSpacePx
    }
}