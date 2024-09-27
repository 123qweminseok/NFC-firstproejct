package com.minseok.tel

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.widget.FrameLayout

class RoundedCornerLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val path = Path()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }
    private var cornerRadius = 100f

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        path.reset()
        path.addRoundRect(
            0f, 0f, w.toFloat(), h.toFloat(),
            cornerRadius, cornerRadius, Path.Direction.CW
        )
        path.close()
    }

    override fun dispatchDraw(canvas: Canvas) {
        val save = canvas.save()
        canvas.clipPath(path)
        canvas.drawPath(path, paint)
        super.dispatchDraw(canvas)
        canvas.restoreToCount(save)
    }
}