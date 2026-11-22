package ru.school28.diary

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator

class DrawingLogoView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(4f)
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = Color.WHITE
    }

    private val cursorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.rgb(255, 64, 129)
    }

    private val logoPath = Path()
    private val pathMeasure = PathMeasure()
    private var pathLength = 0f

    private var progress = 0f

    private var animator: ValueAnimator? = null

    init {
        buildPath()
    }

    private fun buildPath() {
        logoPath.reset()
        val w = 200f
        val h = 260f
        val left = -w / 2
        val top = -h / 2

        logoPath.addRoundRect(
            left, top, left + w, top + h, 24f, 24f, Path.Direction.CW
        )
        logoPath.moveTo(left - 18f, top + 30f)
        logoPath.lineTo(left - 18f, top + 90f)
        for (i in 0 until 4) {
            val y = top + 50f + i * 38f
            logoPath.moveTo(left + 26f, y)
            logoPath.lineTo(left + w - 26f, y)
        }
        val bx = left + 50f
        val by = top + h - 46f
        logoPath.moveTo(bx, by)
        logoPath.lineTo(bx + 30f, by + 30f)
        logoPath.lineTo(bx + 74f, by - 18f)

        pathMeasure.setPath(logoPath, false)
        pathLength = pathMeasure.length
    }

    private val tmpPath = Path()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.save()
        canvas.translate(width / 2f, height / 2f)

        val len = pathLength * progress
        tmpPath.reset()
        pathMeasure.getSegment(0f, len, tmpPath, true)
        canvas.drawPath(tmpPath, strokePaint)

        if (progress > 0.02f && progress < 0.98f) {
            val pos = FloatArray(2)
            val tan = FloatArray(2)
            pathMeasure.getPosTan(len, pos, tan)
            canvas.drawCircle(pos[0], pos[1], dp(7f), cursorPaint)
        }

        canvas.restore()
    }

    fun startDrawing() {
        animator?.cancel()
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1800L
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                progress = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        startDrawing()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator?.cancel()
    }

    private fun dp(v: Float): Float = v * resources.displayMetrics.density
}