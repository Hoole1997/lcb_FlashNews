package com.example.lcb.app.news.ui

import android.animation.ValueAnimator
import android.content.Context
import android.view.View
import android.view.animation.PathInterpolator
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.example.lcb.app.R

/**
 * 新闻列表专用 ItemAnimator。
 *
 * 只动画 transform/alpha，避免在列表滚动和分页时触发布局重算；收藏页删除时也能复用同一套移除动画。
 */
class NewsListItemAnimator(context: Context) : SimpleItemAnimator() {
    private val slideDistance = context.resources.getDimension(R.dimen.flash_news_list_item_slide_distance)
    private val easeOut = PathInterpolator(0.22f, 1f, 0.36f, 1f)

    private val pendingAdds = mutableListOf<RecyclerView.ViewHolder>()
    private val pendingRemoves = mutableListOf<RecyclerView.ViewHolder>()
    private val pendingMoves = mutableListOf<MoveInfo>()
    private val addAnimations = mutableListOf<RecyclerView.ViewHolder>()
    private val removeAnimations = mutableListOf<RecyclerView.ViewHolder>()
    private val moveAnimations = mutableListOf<RecyclerView.ViewHolder>()

    init {
        addDuration = 220L
        removeDuration = 160L
        moveDuration = 220L
        changeDuration = 120L
        supportsChangeAnimations = false
    }

    override fun animateAdd(holder: RecyclerView.ViewHolder): Boolean {
        endAnimation(holder)
        if (!shouldAnimate()) {
            resetView(holder.itemView)
            dispatchAddFinished(holder)
            return false
        }
        holder.itemView.alpha = 0f
        holder.itemView.translationY = slideDistance
        pendingAdds.add(holder)
        return true
    }

    override fun animateRemove(holder: RecyclerView.ViewHolder): Boolean {
        endAnimation(holder)
        if (!shouldAnimate()) {
            resetView(holder.itemView)
            dispatchRemoveFinished(holder)
            return false
        }
        pendingRemoves.add(holder)
        return true
    }

    override fun animateMove(
        holder: RecyclerView.ViewHolder,
        fromX: Int,
        fromY: Int,
        toX: Int,
        toY: Int,
    ): Boolean {
        val view = holder.itemView
        val startX = fromX + view.translationX.toInt()
        val startY = fromY + view.translationY.toInt()
        endAnimation(holder)
        val deltaX = toX - startX
        val deltaY = toY - startY
        if (deltaX == 0 && deltaY == 0) {
            dispatchMoveFinished(holder)
            return false
        }
        if (!shouldAnimate()) {
            resetView(view)
            dispatchMoveFinished(holder)
            return false
        }
        view.translationX = -deltaX.toFloat()
        view.translationY = -deltaY.toFloat()
        pendingMoves.add(MoveInfo(holder, deltaX, deltaY))
        return true
    }

    override fun animateChange(
        oldHolder: RecyclerView.ViewHolder,
        newHolder: RecyclerView.ViewHolder?,
        fromLeft: Int,
        fromTop: Int,
        toLeft: Int,
        toTop: Int,
    ): Boolean {
        if (oldHolder === newHolder) {
            return animateMove(oldHolder, fromLeft, fromTop, toLeft, toTop)
        }
        dispatchChangeFinished(oldHolder, true)
        if (newHolder != null) {
            resetView(newHolder.itemView)
            dispatchChangeFinished(newHolder, false)
        }
        return false
    }

    override fun runPendingAnimations() {
        if (!isRunning) return

        val removals = pendingRemoves.toList()
        pendingRemoves.clear()
        removals.forEach(::runRemoveAnimation)

        val moves = pendingMoves.toList()
        pendingMoves.clear()
        moves.forEach(::runMoveAnimation)

        val adds = pendingAdds.toList()
        pendingAdds.clear()
        adds.forEachIndexed { index, holder -> runAddAnimation(holder, index) }
    }

    override fun endAnimation(item: RecyclerView.ViewHolder) {
        val view = item.itemView
        view.animate().cancel()

        if (pendingAdds.remove(item)) {
            resetView(view)
            dispatchAddFinished(item)
        }
        if (pendingRemoves.remove(item)) {
            resetView(view)
            dispatchRemoveFinished(item)
        }
        endPendingMove(item)
        if (addAnimations.remove(item)) {
            resetView(view)
            dispatchAddFinished(item)
        }
        if (removeAnimations.remove(item)) {
            resetView(view)
            dispatchRemoveFinished(item)
        }
        if (moveAnimations.remove(item)) {
            resetView(view)
            dispatchMoveFinished(item)
        }
        dispatchFinishedWhenDone()
    }

    override fun endAnimations() {
        pendingMoves.toList().forEach { move ->
            pendingMoves.remove(move)
            resetView(move.holder.itemView)
            dispatchMoveFinished(move.holder)
        }
        pendingRemoves.toList().forEach { holder ->
            pendingRemoves.remove(holder)
            resetView(holder.itemView)
            dispatchRemoveFinished(holder)
        }
        pendingAdds.toList().forEach { holder ->
            pendingAdds.remove(holder)
            resetView(holder.itemView)
            dispatchAddFinished(holder)
        }

        moveAnimations.toList().forEach(::endAnimation)
        removeAnimations.toList().forEach(::endAnimation)
        addAnimations.toList().forEach(::endAnimation)
        dispatchFinishedWhenDone()
    }

    override fun isRunning(): Boolean {
        return pendingAdds.isNotEmpty() ||
            pendingRemoves.isNotEmpty() ||
            pendingMoves.isNotEmpty() ||
            addAnimations.isNotEmpty() ||
            removeAnimations.isNotEmpty() ||
            moveAnimations.isNotEmpty()
    }

    private fun runAddAnimation(holder: RecyclerView.ViewHolder, index: Int) {
        val view = holder.itemView
        addAnimations.add(holder)
        dispatchAddStarting(holder)
        view.animate()
            .alpha(1f)
            .translationY(0f)
            .setStartDelay((index.coerceAtMost(MAX_STAGGER_INDEX) * ADD_STAGGER_DELAY_MS).toLong())
            .setDuration(addDuration)
            .setInterpolator(easeOut)
            .withEndAction {
                if (addAnimations.remove(holder)) {
                    resetView(view)
                    dispatchAddFinished(holder)
                    dispatchFinishedWhenDone()
                }
            }
            .start()
    }

    private fun runRemoveAnimation(holder: RecyclerView.ViewHolder) {
        val view = holder.itemView
        removeAnimations.add(holder)
        dispatchRemoveStarting(holder)
        view.animate()
            .alpha(0f)
            .translationX(slideDistance)
            .setStartDelay(0L)
            .setDuration(removeDuration)
            .setInterpolator(easeOut)
            .withEndAction {
                if (removeAnimations.remove(holder)) {
                    resetView(view)
                    dispatchRemoveFinished(holder)
                    dispatchFinishedWhenDone()
                }
            }
            .start()
    }

    private fun runMoveAnimation(move: MoveInfo) {
        val holder = move.holder
        val view = holder.itemView
        moveAnimations.add(holder)
        dispatchMoveStarting(holder)
        view.animate()
            .translationX(0f)
            .translationY(0f)
            .setStartDelay(0L)
            .setDuration(moveDuration)
            .setInterpolator(easeOut)
            .withEndAction {
                if (moveAnimations.remove(holder)) {
                    resetView(view)
                    dispatchMoveFinished(holder)
                    dispatchFinishedWhenDone()
                }
            }
            .start()
    }

    private fun endPendingMove(item: RecyclerView.ViewHolder) {
        val iterator = pendingMoves.iterator()
        while (iterator.hasNext()) {
            val move = iterator.next()
            if (move.holder == item) {
                resetView(item.itemView)
                iterator.remove()
                dispatchMoveFinished(item)
            }
        }
    }

    private fun resetView(view: View) {
        view.animate().setStartDelay(0L)
        view.alpha = 1f
        view.translationX = 0f
        view.translationY = 0f
    }

    private fun shouldAnimate(): Boolean = ValueAnimator.areAnimatorsEnabled()

    private fun dispatchFinishedWhenDone() {
        if (!isRunning) {
            dispatchAnimationsFinished()
        }
    }

    private data class MoveInfo(
        val holder: RecyclerView.ViewHolder,
        val deltaX: Int,
        val deltaY: Int,
    )

    private companion object {
        private const val ADD_STAGGER_DELAY_MS = 24
        private const val MAX_STAGGER_INDEX = 6
    }
}
