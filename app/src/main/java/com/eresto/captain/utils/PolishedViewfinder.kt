package com.eresto.captain.utils

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.SweepGradient
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.LinearInterpolator
import androidx.core.content.ContextCompat
import com.eresto.captain.R
import com.google.zxing.ResultPoint

class PolishedViewfinder(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    private lateinit var frameRect: RectF
    private var originalFrameRect: RectF? = null // To store the frame's initial state
    private val cornerRadius = 80f

    // --- Paints ---
    private val scrimPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val borderPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 8f }
    private val laserPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val resultPointPaint = Paint(Paint.ANTI_ALIAS_FLAG) // For the brand-colored dots

    // --- Drawing Paths & Objects ---
    private val scrimPath = Path()
    private val shimmerMatrix = Matrix()

    // --- Colors (Loaded from colors.xml) ---
    private val brandColorPrimary = ContextCompat.getColor(context, R.color.colorPrimary)
    private val brandColorSecondary = ContextCompat.getColor(context, R.color.blackText1)
    private val successColor = ContextCompat.getColor(context, R.color.color_check)

    // --- Animation & State ---
    private var laserAnimator: ValueAnimator? = null
    private var borderAnimator: ValueAnimator? = null
    private var laserY: Float = 0f
    private var resultPoints: List<ResultPoint>? = null
    private var scrimAlpha = 1f

    init {
        setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        resultPointPaint.color = brandColorPrimary
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val frameWidth = w * 0.75f
        val left = (w - frameWidth) / 2
        val top = ((h * 0.70f) - frameWidth) / 2
        frameRect = RectF(left, top, left + frameWidth, top + frameWidth)
        if (originalFrameRect == null) {
            originalFrameRect = RectF(frameRect)
        }
        updateScrimPath()
        setupShaders()
        startAnimations()
    }

    private fun updateScrimPath() {
        val framePath =
            Path().apply { addRoundRect(frameRect, cornerRadius, cornerRadius, Path.Direction.CW) }
        scrimPath.reset()
        scrimPath.addRect(0f, 0f, width.toFloat(), height.toFloat(), Path.Direction.CW)
        scrimPath.op(framePath, Path.Op.DIFFERENCE)
    }

    private fun setupShaders() {
        val scrimGradient = RadialGradient(
            frameRect.centerX(), frameRect.centerY(), frameRect.width(),
            intArrayOf(Color.TRANSPARENT, Color.parseColor("#A0000000")),
            floatArrayOf(0.7f, 1.0f), Shader.TileMode.CLAMP
        )
        scrimPaint.shader = scrimGradient

        val borderGradient = SweepGradient(
            frameRect.centerX(), frameRect.centerY(),
            intArrayOf(brandColorSecondary, brandColorPrimary, brandColorSecondary), null
        )
        borderPaint.shader = borderGradient

        val laserGradient = LinearGradient(
            frameRect.left, 0f, frameRect.right, 0f,
            intArrayOf(Color.TRANSPARENT, brandColorPrimary, Color.TRANSPARENT),
            floatArrayOf(0.2f, 0.5f, 0.8f), Shader.TileMode.CLAMP
        )
        laserPaint.shader = laserGradient
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        scrimPaint.alpha = (255 * scrimAlpha).toInt()
        canvas.drawPath(scrimPath, scrimPaint)
        canvas.drawRoundRect(frameRect, cornerRadius, cornerRadius, borderPaint)

        if (laserAnimator?.isRunning == true) {
            laserPaint.alpha = (255 * scrimAlpha).toInt()
            canvas.drawRect(frameRect.left, laserY - 4, frameRect.right, laserY + 4, laserPaint)
        }

        // Draw the brand-colored result points
        resultPoints?.forEach { point ->
            canvas.drawCircle(point.x, point.y, 10.0f, resultPointPaint)
        }
    }

    fun setResultPoints(points: List<ResultPoint>?) {
        this.resultPoints = points
        borderPaint.strokeWidth = if (points.isNullOrEmpty()) 8f else 12f
        invalidate()
    }

    fun triggerSuccessAndCollapseAnimation() {
        laserAnimator?.cancel()
        borderAnimator?.cancel()
        resultPoints = null

        val startRect = RectF(frameRect)
        val targetSize = 100f
        val endRect = RectF(
            (width - targetSize) / 2,
            height - targetSize * 2,
            (width + targetSize) / 2,
            height - targetSize
        )

        val successAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 700
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { anim ->
                val fraction = anim.animatedValue as Float
                scrimAlpha = 1f - fraction
                frameRect.left = startRect.left + (endRect.left - startRect.left) * fraction
                frameRect.top = startRect.top + (endRect.top - startRect.top) * fraction
                frameRect.right = startRect.right + (endRect.right - startRect.right) * fraction
                frameRect.bottom = startRect.bottom + (endRect.bottom - startRect.bottom) * fraction
                updateScrimPath()
                invalidate()
            }
        }
        borderPaint.shader = null
        borderPaint.color = successColor
        successAnimator.start()
    }

    private fun startAnimations() {
        laserAnimator = ValueAnimator.ofFloat(frameRect.top + 20, frameRect.bottom - 20).apply {
            addUpdateListener { laserY = it.animatedValue as Float; invalidate() }
            interpolator = AccelerateDecelerateInterpolator()
            duration = 2500
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
            start()
        }
        borderAnimator = ValueAnimator.ofFloat(0f, 360f).apply {
            addUpdateListener {
                shimmerMatrix.setRotate(
                    it.animatedValue as Float,
                    frameRect.centerX(),
                    frameRect.centerY()
                )
                (borderPaint.shader as? SweepGradient)?.setLocalMatrix(shimmerMatrix)
                invalidate()
            }
            interpolator = LinearInterpolator()
            duration = 4000
            repeatCount = ValueAnimator.INFINITE
            start()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        laserAnimator?.cancel()
        borderAnimator?.cancel()
    }
}