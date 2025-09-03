package com.sudokuscout

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat

class NumberButtonView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var number: Int = 1
    private var remainingCount: Int = 9
    private var isEnabled: Boolean = true

    // 業界最佳實踐：使用明確的視覺層次
    private val mainTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
        textSize = dpToPx(24f)
        color = ContextCompat.getColor(context, R.color.primary)
    }

    private val countTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT
        textSize = dpToPx(12f)
        color = ContextCompat.getColor(context, R.color.text_secondary)
    }

    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = ContextCompat.getColor(context, R.color.white)
    }

    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dpToPx(2f)
        color = ContextCompat.getColor(context, R.color.primary)
    }

    private val disabledPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = ContextCompat.getColor(context, R.color.background_light)
    }
    
    // 高效能：預先創建所有 Paint 物件避免在 onDraw 中重複創建
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x20000000.toInt()
        maskFilter = BlurMaskFilter(dpToPx(4f), BlurMaskFilter.Blur.NORMAL)
    }
    
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dpToPx(2f)
    }
    
    private val gradientPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    fun setNumberData(number: Int, remainingCount: Int) {
        this.number = number
        this.remainingCount = remainingCount
        this.isEnabled = remainingCount > 0
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val width = width.toFloat()
        val height = height.toFloat()
        val radius = dpToPx(14f)

        // 優化：更新漸層而非重建 Paint
        gradientPaint.shader = LinearGradient(
            0f, 0f, 0f, height,
            if (isEnabled) intArrayOf(
                ContextCompat.getColor(context, R.color.white),
                0xFFF8F9FA.toInt()
            ) else intArrayOf(
                0xFFF5F5F5.toInt(),
                0xFFEEEEEE.toInt()
            ),
            null,
            Shader.TileMode.CLAMP
        )
        
        // 繪製陰影
        canvas.drawRoundRect(
            dpToPx(1f), dpToPx(2f), 
            width - dpToPx(1f), height, 
            radius, radius, shadowPaint
        )
        
        // 繪製背景
        canvas.drawRoundRect(0f, 0f, width, height - dpToPx(2f), radius, radius, gradientPaint)
        
        // 優化：更新顏色而非重建 Paint
        borderPaint.color = if (isEnabled) {
            ContextCompat.getColor(context, R.color.primary)
        } else {
            ContextCompat.getColor(context, R.color.text_secondary)
        }
        canvas.drawRoundRect(
            dpToPx(1f), dpToPx(1f), 
            width - dpToPx(1f), height - dpToPx(3f), 
            radius, radius, borderPaint
        )

        val centerX = width / 2
        
        // 設置文字顏色
        if (isEnabled) {
            mainTextPaint.color = ContextCompat.getColor(context, R.color.primary)
            countTextPaint.color = ContextCompat.getColor(context, R.color.text_secondary)
        } else {
            mainTextPaint.color = ContextCompat.getColor(context, R.color.text_secondary)
            countTextPaint.color = ContextCompat.getColor(context, R.color.text_secondary)
        }

        // 簡化的文字位置計算 - 確保不被切斷
        val mainText = number.toString()
        val countText = if (remainingCount > 0) remainingCount.toString() else "✓"
        
        // 主要數字位置 - 稍微偏上
        val mainTextY = height * 0.42f // 42% 位置
        canvas.drawText(mainText, centerX, mainTextY, mainTextPaint)
        
        // 小數字位置 - 底部留足夠空間
        val countTextY = height * 0.75f // 75% 位置，確保不被切斷
        canvas.drawText(countText, centerX, countTextY, countTextPaint)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredWidth = dpToPx(48f).toInt()
        val desiredHeight = dpToPx(82f).toInt()

        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)

        val width = when (widthMode) {
            MeasureSpec.EXACTLY -> widthSize
            MeasureSpec.AT_MOST -> minOf(desiredWidth, widthSize)
            else -> desiredWidth
        }

        val height = when (heightMode) {
            MeasureSpec.EXACTLY -> heightSize
            MeasureSpec.AT_MOST -> minOf(desiredHeight, heightSize)
            else -> desiredHeight
        }

        setMeasuredDimension(width, height)
    }

    private fun dpToPx(dp: Float): Float {
        return android.util.TypedValue.applyDimension(
            android.util.TypedValue.COMPLEX_UNIT_DIP,
            dp,
            resources.displayMetrics
        )
    }
}