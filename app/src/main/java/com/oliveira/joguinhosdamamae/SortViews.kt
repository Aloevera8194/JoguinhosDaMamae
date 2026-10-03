package com.oliveira.joguinhosdamamae

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.view.View

fun shade(c: Int, f: Float): Int =
    Color.rgb(
        (Color.red(c) * f).toInt().coerceIn(0, 255),
        (Color.green(c) * f).toInt().coerceIn(0, 255),
        (Color.blue(c) * f).toInt().coerceIn(0, 255)
    )

class NutView(c: Context, private val col: Int) : View(c) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    override fun onDraw(cv: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val path = Path().apply {
            moveTo(w * 0.22f, 0f); lineTo(w * 0.78f, 0f); lineTo(w, h * 0.5f)
            lineTo(w * 0.78f, h); lineTo(w * 0.22f, h); lineTo(0f, h * 0.5f); close()
        }
        p.style = Paint.Style.FILL
        p.color = col
        cv.drawPath(path, p)
        cv.save()
        cv.clipPath(path)
        p.color = 0x33000000
        cv.drawRect(0f, h * 0.65f, w, h, p)
        p.color = 0x66FFFFFF
        cv.drawRect(0f, 0f, w, h * 0.28f, p)
        p.color = 0x22FFFFFF
        cv.drawRect(0f, h * 0.28f, w * 0.4f, h * 0.65f, p)
        cv.restore()
        p.color = shade(col, 0.55f)
        cv.drawOval(RectF(w * 0.33f, h * 0.22f, w * 0.67f, h * 0.68f), p)
        p.color = 0xFFECEFF1.toInt()
        cv.drawOval(RectF(w * 0.36f, h * 0.24f, w * 0.64f, h * 0.60f), p)
        p.style = Paint.Style.STROKE
        p.strokeWidth = 2f
        p.color = 0x66000000
        cv.drawPath(path, p)
    }
}

class BoltView(c: Context, private val d: Float) : View(c) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    override fun onDraw(cv: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        p.style = Paint.Style.FILL
        p.color = 0xFFB0BEC5.toInt()
        cv.drawRoundRect(RectF(w / 2 - 7 * d, 0f, w / 2 + 7 * d, h - 8 * d), 6 * d, 6 * d, p)
        p.color = 0x55FFFFFF
        cv.drawRect(w / 2 - 5 * d, 4 * d, w / 2 - 2 * d, h - 14 * d, p)
        p.color = 0x33000000
        var y = 6 * d
        while (y < h - 14 * d) {
            cv.drawRect(w / 2 - 7 * d, y, w / 2 + 7 * d, y + 1.5f * d, p)
            y += 8 * d
        }
        p.color = 0xFFCFD8DC.toInt()
        cv.drawRoundRect(RectF(w * 0.04f, h - 14 * d, w * 0.96f, h), 7 * d, 7 * d, p)
        p.color = 0x33000000
        cv.drawRoundRect(RectF(w * 0.04f, h - 6 * d, w * 0.96f, h), 5 * d, 5 * d, p)
    }
}

class BirdView(c: Context, private val k: Int) : View(c) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val body = intArrayOf(
        0xFFE53935.toInt(), 0xFF1E88E5.toInt(), 0xFFFBC02D.toInt(),
        0xFF43A047.toInt(), 0xFF8E24AA.toInt(), 0xFFFB8C00.toInt(),
        0xFF455A64.toInt(), 0xFFEC407A.toInt(), 0xFF26C6DA.toInt()
    )
    override fun onDraw(cv: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val b = body[k % body.size]
        p.style = Paint.Style.FILL
        p.color = shade(b, 0.7f)
        cv.drawOval(RectF(0f, h * 0.5f, w * 0.38f, h * 0.78f), p)
        p.color = b
        cv.drawOval(RectF(w * 0.1f, h * 0.2f, w * 0.92f, h * 0.92f), p)
        p.color = 0xFFFFF8E1.toInt()
        cv.drawOval(RectF(w * 0.34f, h * 0.5f, w * 0.84f, h * 0.9f), p)
        p.color = shade(b, 0.75f)
        cv.drawOval(RectF(w * 0.18f, h * 0.45f, w * 0.55f, h * 0.78f), p)
        when (k % 3) {
            0 -> {
                p.color = shade(b, 0.8f)
                cv.drawCircle(w * 0.55f, h * 0.2f, w * 0.1f, p)
            }
            1 -> {
                p.color = shade(b, 0.8f)
                val l = Path().apply { moveTo(w * 0.42f, h * 0.3f); lineTo(w * 0.46f, h * 0.06f); lineTo(w * 0.58f, h * 0.26f); close() }
                val r = Path().apply { moveTo(w * 0.62f, h * 0.26f); lineTo(w * 0.74f, h * 0.06f); lineTo(w * 0.78f, h * 0.32f); close() }
                cv.drawPath(l, p)
                cv.drawPath(r, p)
            }
        }
        p.color = 0xFFFF9800.toInt()
        val beak = Path().apply { moveTo(w * 0.86f, h * 0.38f); lineTo(w, h * 0.46f); lineTo(w * 0.86f, h * 0.55f); close() }
        cv.drawPath(beak, p)
        p.color = Color.WHITE
        cv.drawCircle(w * 0.68f, h * 0.38f, w * 0.1f, p)
        p.color = Color.BLACK
        cv.drawCircle(w * 0.71f, h * 0.38f, w * 0.05f, p)
        p.color = 0xFFFF9800.toInt()
        p.strokeWidth = w * 0.05f
        cv.drawLine(w * 0.45f, h * 0.9f, w * 0.45f, h, p)
        cv.drawLine(w * 0.65f, h * 0.9f, w * 0.65f, h, p)
    }
}

class BranchView(c: Context, private val d: Float) : View(c) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    override fun onDraw(cv: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        p.color = 0xFF5D4037.toInt()
        cv.drawRoundRect(RectF(0f, h - 12 * d, w, h - 2 * d), 5 * d, 5 * d, p)
        p.color = 0xFF8D6E63.toInt()
        cv.drawRoundRect(RectF(0f, h - 12 * d, w, h - 8 * d), 4 * d, 4 * d, p)
        p.color = 0xFF66BB6A.toInt()
        cv.drawOval(RectF(w * 0.08f, h - 18 * d, w * 0.08f + 14 * d, h - 9 * d), p)
        cv.drawOval(RectF(w * 0.9f - 14 * d, h - 18 * d, w * 0.9f, h - 9 * d), p)
    }
}
