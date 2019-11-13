@file:Suppress("NOTHING_TO_INLINE")

package uk.whitecrescent.waqti.frontend.customview.drag

import android.annotation.SuppressLint
import android.graphics.PointF
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.SpringAnimation
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import org.jetbrains.anko.childrenRecursiveSequence
import uk.whitecrescent.waqti.extensions.F
import uk.whitecrescent.waqti.extensions.Observer
import uk.whitecrescent.waqti.extensions.globalVisibleRect
import uk.whitecrescent.waqti.extensions.logE
import uk.whitecrescent.waqti.extensions.mainActivity
import uk.whitecrescent.waqti.extensions.parentViewGroup
import uk.whitecrescent.waqti.extensions.parents
import java.util.concurrent.TimeUnit

open class DragBehavior(val view: View) {

    protected val dPoint = PointF()
    protected val touchPoint = PointF()
    val returnPoint = PointF()

    protected var isDragging = false
    protected var stealChildrenTouchEvents = false

    private var downCalled = false

    private var touchPointOutOfParentBounds = false

    protected var dampingRatio = 0.6F
    protected var stiffness = 1000F

    var synthesizedEvent: MotionEvent? = null

    protected val onTouchListener = View.OnTouchListener { v, event ->
        if (event != synthesizedEvent) {
            logE("Recycled Synthesized event!")
            synthesizedEvent?.recycle()
            synthesizedEvent = null
        }
        touchPoint.set(event.rawX, event.rawY)
        if (isDragging) {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    onDown(event)
                }
                MotionEvent.ACTION_MOVE -> {
                    onMove(event)
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    endDrag()
                }
                else -> {
                    logE("SOMETHING ELSE!")
                    // view.onTouchEvent(event)
                }
            }
            true
        } else {
            true
        }
    }

    init {
        returnPoint.set(view.x, view.y)
        view.setOnTouchListener(onTouchListener)
    }

    @SuppressLint("CheckResult")
    fun drag(event: MotionEvent) {
        if (event === synthesizedEvent) {
            logE("Recycled Original event!")
            view.parentViewGroup?.requestDisallowInterceptTouchEvent(true)
            event.recycle()
            view.parentViewGroup?.requestDisallowInterceptTouchEvent(true)
            startObserver(event)
        }
    }

    private inline fun startObserver(event: MotionEvent) {
        Observable.interval(1L, TimeUnit.MILLISECONDS)
                .takeWhile { synthesizedEvent != null }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe(Observer<Long>(
                        onNext = {
                            if (event !== synthesizedEvent) {
                                logE("Recycled Synthesized event!")
                                synthesizedEvent?.recycle()
                                synthesizedEvent = null
                            }
                            view.parentViewGroup?.requestDisallowInterceptTouchEvent(true)
                            //view.dispatchTouchEvent(event)
                            onTouchListener.onTouch(view, event)
                            if (it.rem(100L) == 0L) {
                                logE(event)
                                logE(synthesizedEvent)
                                logE(event == synthesizedEvent)
                                logE(event === synthesizedEvent)
                            }

                            view.parents.forEach {
                                it.isClickable = false
                                it.stopNestedScroll()
                                it.requestDisallowInterceptTouchEvent(true)
                                it.isEnabled = false
                                it.cancelPendingInputEvents()
                            }
                        },
                        onError = {

                        },
                        onComplete = {
                            logE("COMPLETED DRAGGING!")
                            logE(synthesizedEvent)
                        },
                        onSubscribe = {
                            logE("STARTED DRAGGING!")
                        }
                )
                )
    }

    protected open fun onDown(event: MotionEvent) {
        if (!downCalled) {
            dPoint.x = view.x - event.rawX
            dPoint.y = view.y - event.rawY
            touchPoint.set(event.rawX, event.rawY)
            view.parentViewGroup?.requestDisallowInterceptTouchEvent(true)
            downCalled = true
        }
    }

    protected open fun onMove(event: MotionEvent) {
        onDown(event)
        view.parentViewGroup?.requestDisallowInterceptTouchEvent(true)
        view.x = event.rawX + dPoint.x
        view.y = event.rawY + dPoint.y
        touchPoint.set(event.rawX, event.rawY)
    }

    protected open fun animateReturn() {
        SpringAnimation(view, DynamicAnimation.X, returnPoint.x).also {
            it.spring.dampingRatio = dampingRatio
            it.spring.stiffness = stiffness
        }.start()
        SpringAnimation(view, DynamicAnimation.Y, returnPoint.y).also {
            it.spring.dampingRatio = dampingRatio
            it.spring.stiffness = stiffness
            it.addEndListener { _, _, _, _ -> afterEndAnimation() }
        }.start()
    }

    protected open fun afterEndAnimation() {
        isDragging = false
        stealChildrenTouchEvents = false
        touchPointOutOfParentBounds = false
        downCalled = false
    }

    open fun startDrag() {
        returnPoint.set(view.x, view.y)
        isDragging = true
        stealChildrenTouchEvents = true
    }

    open fun endDrag() {
        synthesizedEvent = null
        view.cancelLongPress()
        animateReturn()
    }

    open fun endDragNow() {
        view.cancelLongPress()
        returnPoint.set(view.x, view.y)
        afterEndAnimation()
    }

    @SuppressLint("Recycle")
    open fun startDragFromView(otherView: View) {
        require(otherView in this.view.parentViewGroup!!.childrenRecursiveSequence()) {
            """"The passed in view must be a descendant of this DragView's parent! 
                Passed in View: $otherView 
                Parent: ${otherView.parent}"""
        }

        this.view.bringToFront()

        val parentBounds = this.view.parentViewGroup!!.globalVisibleRect
        val viewBounds = otherView.globalVisibleRect

        this.view.x = viewBounds.left.F - parentBounds.left.F
        this.view.y = viewBounds.top.F - parentBounds.top.F
        startDrag()

        // continuously run a synthesized drag loop (synthesized MotionEvent) until the user
        // actually starts dragging themselves in which case we will recycle the synthesized
        // MotionEvent and use the user's MotionEvent for Drags
        // How will we know? Well the Synthesized Event won't actually move anything, it'll act
        // and behave like a MOVE but it will be in the same spot, the touch point that is
        // triggering this start drag, this needs to be a new parameter, if onMove is called and
        // the touch point is different from the current one (either the event passed is not the
        // synthesized one, OR the actual point is different, both mean the passed in event is
        // not the synthesized one) then we can proceed as usual and do dragging

        otherView.parents.forEach {
            it.isClickable = false
            it.stopNestedScroll()
            it.requestDisallowInterceptTouchEvent(true)
            it.isEnabled = false
            it.cancelPendingInputEvents()
        }

        synthesizedEvent = MotionEvent.obtain(
                SystemClock.uptimeMillis(),
                SystemClock.uptimeMillis(),
                MotionEvent.ACTION_MOVE,
                view.mainActivity.currentTouchPoint.x,
                view.mainActivity.currentTouchPoint.y,
                0
        )

        synthesizedEvent?.also { drag(it) }

        /*otherView.setOnTouchListener { v, event ->

            /*
             * The otherView, (the one this comment is inside its touchListener) is having its
             * touch event cancelled when a notify function is called because that other View is
             * a member of the RecyclerView which is being redrawn and re-laid-out so naturally
             * it must cancel its touch events, but ideally this shouldn't effect us as the
             * dragBehavior since we only care about this.view
             *
             * So, either we look into a solution inside here to fix this, or we make it such
             * that when the user touches any Task, the dragShadow is placed there and matched
             * there and a long press will occur on it not the item. I really should try to fix
             * it here but that might not be possible, we'll see
             */

            v.backgroundColor = ColorScheme.getAllColorSchemes().random().main.toAndroidColor
            if (isDragging) {
                touchPoint.set(event.rawX, event.rawY)
                this.view.dispatchTouchEvent(event)
                this.view.parentViewGroup?.requestDisallowInterceptTouchEvent(true)
                v.onTouchEvent(event)
                v.parentViewGroup?.requestDisallowInterceptTouchEvent(true)
                true
            } else {
                false
            }
        }*/
    }

    inline fun onTouchEvent(event: MotionEvent) = this.view.onTouchEvent(event)

    inline fun dispatchTouchEvent(event: MotionEvent) = this.view.dispatchTouchEvent(event)

    companion object {

        val longPressTime: Int
            get() = ViewConfiguration.getLongPressTimeout()
    }

}

open class ObservableDragBehavior(view: View) : DragBehavior(view) {

    val initialTouchPoint = PointF()

    var dragListener: DragListener? = null
        set(value) {
            field = value
            value?.onDragStateChanged(view, dragState)
        }

    protected var dragState: DragState = DragState.IDLE
        private set(value) {
            field = value
            dragListener?.onDragStateChanged(view, value)
        }

    override fun onMove(event: MotionEvent) {
        super.onMove(event)
        dragListener?.onUpdateLocation(view, touchPoint)
    }

    override fun animateReturn() {
        dragState = DragState.SETTLING
        super.animateReturn()
    }

    override fun afterEndAnimation() {
        super.afterEndAnimation()
        dragState = DragState.IDLE
        dragListener?.onEndDrag(view)
    }

    override fun startDrag() {
        super.startDrag()
        initialTouchPoint.set(touchPoint)
        dragState = DragState.DRAGGING
        dragListener?.onStartDrag(view)
    }

    override fun endDrag() {
        super.endDrag()
        initialTouchPoint.set(0F, 0F)
        dragListener?.onReleaseDrag(view, touchPoint)
    }

    override fun startDragFromView(otherView: View) {
        dragState = DragState.DRAGGING
        dragListener?.onStartDrag(view)

        super.startDragFromView(otherView)
    }

    enum class DragState {
        /** View is idle, no movement */
        IDLE,
        /** View is being dragged by user, movement from user */
        DRAGGING,
        /** View is settling into final position, movement is not from user */
        SETTLING
    }

    interface DragListener {

        /**
         * Called before the drag operation starts
         */
        fun onStartDrag(dragView: View)

        /**
         * Called when the drag touch location is updated, meaning when the user moves their
         * finger while dragging.
         *
         * Warning: This gets called **VERY OFTEN**, any code called in here should not be too
         * expensive, else you may experience jank while dragging.
         */
        fun onUpdateLocation(dragView: View, touchPoint: PointF)

        /**
         * Called when the user's touch is released. The [dragView] will start to animate its
         * return.
         */
        fun onReleaseDrag(dragView: View, touchPoint: PointF)

        /**
         * Called when the dragging operation has fully ended and the [dragView] has ended its
         * return animation.
         */
        fun onEndDrag(dragView: View)

        /**
         * Called when the [DragState] of the [dragView] is changed
         */
        fun onDragStateChanged(dragView: View, newState: DragState)

    }

    abstract class SimpleDragListener : DragListener {
        override fun onStartDrag(dragView: View) {}
        override fun onUpdateLocation(dragView: View, touchPoint: PointF) {}
        override fun onReleaseDrag(dragView: View, touchPoint: PointF) {}
        override fun onEndDrag(dragView: View) {}
        override fun onDragStateChanged(dragView: View, newState: DragState) {}
    }

}

inline fun View.addDragBehavior() = DragBehavior(this)

inline fun View.addObservableDragBehavior() = ObservableDragBehavior(this)