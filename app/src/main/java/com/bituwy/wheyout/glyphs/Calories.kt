package com.bituwy.wheyout.glyphs

import android.content.Context
import android.graphics.Point
import android.os.Handler
import android.os.Looper
import com.bituwy.wheyout.GlyphMatrixService
import com.bituwy.wheyout.helpers.GlyphMatrixHelper
import com.bituwy.wheyout.model.CaloriesTracker
import com.nothing.ketchum.GlyphMatrixFrame
import com.nothing.ketchum.GlyphMatrixFrameWithMarquee
import com.nothing.ketchum.GlyphMatrixManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.min
import kotlin.math.roundToInt
class Calories : GlyphMatrixService("Calories") {
    private lateinit var caloriesTracker: CaloriesTracker
    private lateinit var glyphHelper: GlyphMatrixHelper

    private val backgroundScope = CoroutineScope(Dispatchers.IO)
    val frameBuilder = GlyphMatrixFrame.Builder()
    var circleAnimated = false
    var circleAnimationStep = 0.0
    var circlePercent = 0.0
    val handler = Handler(Looper.getMainLooper())
    val tickRunnable: Runnable = Runnable { animationUpdate() }

    lateinit var frameWithMarquee : GlyphMatrixFrameWithMarquee

    suspend fun setRemainingCalories() {
        // TODO: Make the start of day configurable
        val startOfDay = LocalDate.now().atTime(5, 0)
        val remainingCalories: Int
        try {
            remainingCalories = caloriesTracker.remaining(startOfDay).toInt()
            circlePercent = caloriesTracker.percentConsumed(startOfDay)
        } catch (e: SecurityException) {
            frameBuilder.addTop(glyphHelper.buildCenteredText(SCREEN_LENGTH,"No Permissions", GlyphMatrixHelper.CenterOptions.VERTICAL))
            return
        }

        val remainingCaloriesText = glyphHelper.buildCenteredText(SCREEN_LENGTH, "${remainingCalories} kcal", GlyphMatrixHelper.CenterOptions.VERTICAL)
        frameBuilder.addTop(remainingCaloriesText)
    }

    fun advanceCircle() {
        if (circleAnimationStep + 0.01 >= circlePercent) return
        circleAnimationStep += 0.01
        val circleScreen = generateCircleProgress(circleAnimationStep)
        frameWithMarquee.setMid(circleScreen)
    }

    fun generateCircleProgress(percentDone: Double): IntArray {
        val circleProgressSteps = arrayOf(
            Point(12, 0), Point(13, 0), Point(14, 0), Point(15, 0),
            Point(16, 1), Point(17, 1), Point(18, 2), Point(19, 2),
            Point(20, 3), Point(21, 4), Point(22, 5), Point(22, 6),
            Point(23, 7), Point(23, 8), Point(24, 9), Point(24, 10),
            Point(24, 11), Point(24, 12), Point(24, 13), Point(24, 14),
            Point(24, 15), Point(23, 16), Point(23, 17), Point(22, 18),
            Point(22, 19), Point(21, 20), Point(20, 21), Point(19, 22),
            Point(18, 22), Point(17, 23), Point(16, 23), Point(15, 24),
            Point(14, 24), Point(13, 24), Point(12, 24), Point(11, 24),
            Point(10, 24), Point(9, 24), Point(8, 23), Point(7, 23),
            Point(6, 22), Point(5, 22), Point(4, 21), Point(3, 20),
            Point(2, 19), Point(2, 18), Point(1, 17), Point(1, 16),
            Point(0, 15), Point(0, 14), Point(0, 13), Point(0, 12),
            Point(0, 11), Point(0, 10), Point(0, 9), Point(1, 8),
            Point(1, 7), Point(2, 6), Point(2, 5), Point(3, 4),
            Point(4, 3), Point(5, 2), Point(6, 2), Point(7, 1),
            Point(8, 1), Point(9, 0), Point(10, 0), Point(11, 0)
        )
        val renderedProgressCircle = IntArray(SCREEN_LENGTH * SCREEN_LENGTH)
        val maxProgressSize = min(
            circleProgressSteps.size * percentDone, circleProgressSteps.size.toDouble(),
        )
        for (i in 0..maxProgressSize.roundToInt().dec()) {
            val column = circleProgressSteps[i].x
            val row = circleProgressSteps[i].y
            renderedProgressCircle[column + (row*SCREEN_LENGTH)] = 2047
        }

        return renderedProgressCircle
    }

    override fun performOnServiceConnected(
        context: Context,
        glyphMatrixManager: GlyphMatrixManager
    ) {
        caloriesTracker = CaloriesTracker(applicationContext)
        glyphHelper = GlyphMatrixHelper(applicationContext)

        backgroundScope.launch {
            setRemainingCalories()
            frameWithMarquee = frameBuilder.buildWithMarquee(
                applicationContext,
                handler,
                50,
                1
            ) { updatedFrame -> glyphMatrixManager.setMatrixFrame(updatedFrame) }

            frameWithMarquee.startMarquee()
            startAnimation()
        }
    }

    //TODO: Remove reliance on the GlyphMatrixFrameMarquee for rendering
    fun startAnimation() {
        handler.removeCallbacks(tickRunnable)
        handler.postDelayed(tickRunnable, 30)
        circleAnimated = true
    }

    fun animationUpdate() {
        if (circleAnimated) {
            advanceCircle()
            handler.postDelayed(tickRunnable, 30)
        }
    }

    fun stopAnimation(){
        handler.removeCallbacks(tickRunnable)
        circleAnimated = false
    }

    override fun performOnServiceDisconnected(context: Context) {
        frameWithMarquee.stopMarquee()
        stopAnimation()
        backgroundScope.cancel()
    }

    private companion object {
        private const val SCREEN_LENGTH = 25
    }
}