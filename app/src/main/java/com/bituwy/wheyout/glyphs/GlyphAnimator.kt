package com.bituwy.wheyout.glyphs

import android.os.Handler
import android.os.Looper

interface Animatable {
    var tickRate: Int
    val animationHandler: Handler
    val tickRunnable: Runnable
    fun onAnimationUpdate()
    fun startAnimation()
    fun stopAnimation()
}

class GlyphAnimator() : Animatable {
    override val animationHandler = Handler(Looper.getMainLooper())
    override val tickRunnable = Runnable { onAnimationUpdate() }
    override var tickRate = 30

    override fun startAnimation() {
        animationHandler.removeCallbacks(tickRunnable)
        animationHandler.postDelayed(tickRunnable, tickRate.toLong())
    }

    override fun stopAnimation() {
        animationHandler.removeCallbacks(tickRunnable)
    }

    override fun onAnimationUpdate() {
        animationHandler.postDelayed(tickRunnable, tickRate.toLong())
    }
}
