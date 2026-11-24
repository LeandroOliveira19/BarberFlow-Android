package barberflow.cliente.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import barberflow.cliente.R
import kotlin.math.cos
import kotlin.math.sin

/**
 * Fumo dourado animado (o mesmo efeito do topo do site e do login da gestão):
 * três manchas radiais em dourado, âmbar e castanho que se movem devagar por trás do conteúdo.
 * Com as animações do sistema desligadas fica parado.
 */
class GradienteAnimadoView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null,
) : View(context, attrs) {

    private val dourado = ContextCompat.getColor(context, R.color.bf_dourado)
    private val cores = intArrayOf(
        dourado,
        ColorUtils.blendARGB(dourado, Color.rgb(0x7A, 0x3E, 0x10), 0.45f),
        ColorUtils.blendARGB(dourado, Color.BLACK, 0.75f),
    )
    /** Por mancha: centro (fração da largura/altura), raio (fração do maior lado), amplitude do movimento e velocidade. */
    private val manchas = arrayOf(
        floatArrayOf(0.15f, 0.10f, 0.75f, 0.20f, 1.0f),
        floatArrayOf(0.90f, 0.95f, 0.70f, 0.25f, 0.8f),
        floatArrayOf(0.55f, 0.55f, 0.55f, 0.18f, 1.3f),
    )
    private val alphas = intArrayOf(120, 110, 170)
    private val pincel = Paint(Paint.ANTI_ALIAS_FLAG)
    private var fase = 0f
    private var animador: ValueAnimator? = null

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!ValueAnimator.areAnimatorsEnabled()) return
        animador = ValueAnimator.ofFloat(0f, (2 * Math.PI).toFloat()).apply {
            duration = 26_000
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener { fase = it.animatedValue as Float; invalidate() }
            start()
        }
    }

    override fun onDetachedFromWindow() {
        animador?.cancel()
        animador = null
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return
        val lado = maxOf(w, h)
        manchas.forEachIndexed { i, m ->
            val cx = (m[0] + m[3] * sin(fase * m[4] + i)) * w
            val cy = (m[1] + m[3] * cos(fase * m[4] * 0.8f + i * 2)) * h
            val raio = m[2] * lado
            val cor = ColorUtils.setAlphaComponent(cores[i], alphas[i])
            pincel.shader = RadialGradient(cx, cy, raio, cor, Color.TRANSPARENT, Shader.TileMode.CLAMP)
            canvas.drawCircle(cx, cy, raio, pincel)
        }
    }
}
