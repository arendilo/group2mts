package com.example.group2mts

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.MotionEvent
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.abs
import kotlin.math.roundToInt

class WheelView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private val itemH = (40 * resources.displayMetrics.density).roundToInt()
    private var items: List<String> = emptyList()
    private var lastIndex = 0

    var onSelected: ((Int) -> Unit)? = null

    private val regular: Typeface =
        ResourcesCompat.getFont(context, R.font.nunito) ?: Typeface.DEFAULT
    private val bold: Typeface = Typeface.create(regular, Typeface.BOLD)

    private val band = View(context).apply {
        background = GradientDrawable().apply {
            cornerRadius = itemH / 3f
            setColor(ContextCompat.getColor(context, R.color.page_background))
        }
    }

    private val list = RecyclerView(context).apply {
        layoutManager = LinearLayoutManager(context)
        overScrollMode = OVER_SCROLL_NEVER
        isNestedScrollingEnabled = false
        clipToPadding = false
        setPadding(0, itemH, 0, itemH)
    }

    private val snap = LinearSnapHelper()
    private val adapter = WheelAdapter()

    init {
        addView(band, LayoutParams(LayoutParams.MATCH_PARENT, itemH, Gravity.CENTER_VERTICAL))
        addView(list, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        snap.attachToRecyclerView(list)
        list.adapter = adapter
        list.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                highlight()
            }

            override fun onScrollStateChanged(rv: RecyclerView, newState: Int) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    val idx = snappedIndex()
                    if (idx != -1 && idx != lastIndex) {
                        lastIndex = idx
                        onSelected?.invoke(idx)
                    }
                    highlight()
                }
            }
        })
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.actionMasked == MotionEvent.ACTION_DOWN) {
            parent?.requestDisallowInterceptTouchEvent(true)
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(
            widthMeasureSpec,
            MeasureSpec.makeMeasureSpec(itemH * 3, MeasureSpec.EXACTLY)
        )
    }

    fun setItems(newItems: List<String>, index: Int) {
        items = newItems
        lastIndex = index.coerceIn(0, (newItems.size - 1).coerceAtLeast(0))
        adapter.notifyDataSetChanged()
        (list.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(lastIndex, 0)
        list.post { highlight() }
    }

    fun currentIndex(): Int {
        val idx = snappedIndex()
        return if (idx != -1) idx else lastIndex
    }

    private fun snappedIndex(): Int {
        val lm = list.layoutManager ?: return -1
        val v = snap.findSnapView(lm) ?: return -1
        return lm.getPosition(v)
    }

    private fun highlight() {
        val mid = list.height / 2
        for (i in 0 until list.childCount) {
            val tv = list.getChildAt(i) as? TextView ?: continue
            val selected = abs((tv.top + tv.bottom) / 2 - mid) < itemH / 2
            tv.setTextColor(
                ContextCompat.getColor(context, if (selected) R.color.on_background else R.color.muted)
            )
            tv.typeface = if (selected) bold else regular
            tv.textSize = if (selected) 20f else 14f
        }
    }

    private inner class WheelAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        override fun getItemCount() = items.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val tv = TextView(parent.context).apply {
                layoutParams = RecyclerView.LayoutParams(
                    RecyclerView.LayoutParams.MATCH_PARENT, itemH
                )
                gravity = Gravity.CENTER
                textSize = 16f
            }
            return object : RecyclerView.ViewHolder(tv) {}
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val tv = holder.itemView as TextView
            tv.text = items[position]
            val selected = position == lastIndex
            tv.setTextColor(
                ContextCompat.getColor(context, if (selected) R.color.on_background else R.color.muted)
            )
            tv.typeface = if (selected) bold else regular
            tv.textSize = if (selected) 20f else 14f
        }
    }
}