package org.fossify.phone.helpers

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.MotionEvent
import android.view.ViewConfiguration
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.abs
import kotlin.math.min

/**
 * Swipe a list item to the left to start a call.
 *
 * The list lives inside the main ViewPager, which would otherwise steal horizontal drags.
 * Leftward drags are kept by the list (Recents is the last tab, so nothing is lost),
 * any other drag is handed back to the ViewPager so tabs can still be switched.
 */
class SwipeToCallHelper(
    private val recyclerView: RecyclerView,
    private val canSwipe: (position: Int) -> Boolean,
    private val onSwipedLeft: (position: Int) -> Unit,
) {
    private val context = recyclerView.context
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private val backgroundPaint = Paint().apply { color = SWIPE_COLOR }
    private val icon = ContextCompat.getDrawable(context, org.fossify.commons.R.drawable.ic_phone_vector)?.mutate()?.apply {
        setTint(Color.WHITE)
    }
    private val iconMargin = (24 * context.resources.displayMetrics.density).toInt()

    fun attach() {
        recyclerView.addOnItemTouchListener(directionGuard)
        ItemTouchHelper(swipeCallback).attachToRecyclerView(recyclerView)
    }

    private val directionGuard = object : RecyclerView.SimpleOnItemTouchListener() {
        private var downX = 0f
        private var downY = 0f
        private var decided = false

        override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = e.x
                    downY = e.y
                    decided = false
                    rv.parent?.requestDisallowInterceptTouchEvent(true)
                }

                MotionEvent.ACTION_MOVE -> if (!decided) {
                    val dx = e.x - downX
                    val dy = e.y - downY
                    if (abs(dx) > touchSlop || abs(dy) > touchSlop) {
                        decided = true
                        val isLeftSwipe = dx < 0 && abs(dx) > abs(dy)
                        if (!isLeftSwipe) {
                            rv.parent?.requestDisallowInterceptTouchEvent(false)
                        }
                    }
                }
            }
            return false
        }
    }

    private val swipeCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
        override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder) = false

        override fun getSwipeDirs(rv: RecyclerView, vh: RecyclerView.ViewHolder): Int {
            val position = vh.bindingAdapterPosition
            return if (position != RecyclerView.NO_POSITION && canSwipe(position)) ItemTouchHelper.LEFT else 0
        }

        override fun getSwipeThreshold(vh: RecyclerView.ViewHolder) = 0.35f

        override fun onSwiped(vh: RecyclerView.ViewHolder, direction: Int) {
            val position = vh.bindingAdapterPosition
            // put the row back in place, the list itself does not change
            recyclerView.adapter?.notifyItemChanged(position)
            if (position != RecyclerView.NO_POSITION) {
                onSwipedLeft(position)
            }
        }

        override fun onChildDraw(
            c: Canvas, rv: RecyclerView, vh: RecyclerView.ViewHolder,
            dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean
        ) {
            if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE && dX < 0) {
                val item = vh.itemView
                c.drawRect(item.right + dX, item.top.toFloat(), item.right.toFloat(), item.bottom.toFloat(), backgroundPaint)
                icon?.let {
                    val size = min(it.intrinsicHeight, item.height - iconMargin)
                    val top = item.top + (item.height - size) / 2
                    val right = item.right - iconMargin
                    if (right - size > item.right + dX) {
                        it.setBounds(right - size, top, right, top + size)
                        it.draw(c)
                    }
                }
            }
            super.onChildDraw(c, rv, vh, dX, dY, actionState, isCurrentlyActive)
        }
    }

    companion object {
        private val SWIPE_COLOR = Color.parseColor("#4CAF50")
    }
}
