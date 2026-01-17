package com.bituwy.wheyout.glyphs

import android.content.Context
import android.graphics.Point
import android.os.Handler
import android.os.Looper
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
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import kotlin.math.min
import kotlin.math.roundToInt

class CaloriesGlyph() : Glyph("Calories") {
    private companion object {
        private const val SCREEN_LENGTH = 25

        // TODO: Make the offset of when we switch over displaying the new day configurable
        // This delays the new day display until offset hour is past
        // i.e if offset is 3 and we are at 2 on the next day we'll display the previous day stats
        private const val DISPLAY_HOUR_OFFSET = 3
    }

    private lateinit var caloriesTracker: CaloriesTracker
    private lateinit var glyphHelper: GlyphMatrixHelper

    private val backgroundScope = CoroutineScope(Dispatchers.IO)
    val textFrameBuilder = GlyphMatrixFrame.Builder()
    private lateinit var textFrame: GlyphMatrixFrameWithMarquee
    val animationFrameBuilder = GlyphMatrixFrame.Builder()
    private lateinit var matrixManager: GlyphMatrixManager
    var circleAnimated = false
    var circleAnimationStep = 0.0
    var circlePercent = 0.0

    suspend fun setRemainingCalories() {
        val remainingCalories: Int
        val nutritionalValues: Map<String, Double>
        val dateNow = LocalDate.now()
        val startOfDay = if (LocalDateTime.now().hour < DISPLAY_HOUR_OFFSET) {
            dateNow.minus(1, ChronoUnit.DAYS).atTime(0, 0)
        } else {
            dateNow.atTime(0, 0)
        }
        try {
            caloriesTracker = CaloriesTracker.create(applicationContext, startOfDay)
            remainingCalories = caloriesTracker.remaining().toInt()
            circlePercent = caloriesTracker.percentConsumed()
            nutritionalValues = caloriesTracker.nutritionalValues()

        } catch (e: SecurityException) {
            val text = glyphHelper.buildCenteredText(
                SCREEN_LENGTH,
                e.message ?: "No Permission",
                GlyphMatrixHelper.CenterOptions.VERTICAL
            )
            textFrame.setTop(text)
            return
        }

        val remainingCaloriesText = glyphHelper.buildCenteredText(
            SCREEN_LENGTH,
            "${remainingCalories} kcal ${nutritionalValues["proteins"]?.toInt()}p ${nutritionalValues["carbs"]?.toInt()}c ${nutritionalValues["fats"]?.toInt()}f",
            GlyphMatrixHelper.CenterOptions.VERTICAL
        )
        textFrame.setTop(remainingCaloriesText)
    }

    fun advanceCircle() {
        if (circleAnimationStep + 0.01 >= circlePercent) return
        circleAnimationStep += 0.01
        val circleScreen = generateCircleProgress(circleAnimationStep)
        animationFrameBuilder.addMid(circleScreen)
    }

    fun generateCircleProgress(percentDone: Double): IntArray {
        //TODO: Move this at the instance level as it's reallocated each time
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
        glyphHelper = GlyphMatrixHelper(applicationContext)
        matrixManager = glyphMatrixManager
        textFrame = textFrameBuilder.buildWithMarquee(
            applicationContext,
            animationHandler,
            tickRate ,
            1
        ) { updatedFrame ->
            animationFrameBuilder.addLow(updatedFrame)
        }
        backgroundScope.launch {
            setRemainingCalories()
            startTextMarquee()
            startAnimation()
        }
    }

    fun startTextMarquee() {
        // We're delaying the text marquee in order for the text to be readable instead of scrolling from the start
        animationFrameBuilder.addLow(textFrame.render())
        animationHandler.postDelayed(
            Runnable { textFrame.startMarquee() },
            700
        )
    }

    override fun startAnimation() {
        super.startAnimation()
        circleAnimated = true
    }

    override fun onAnimationUpdate() {
        // TODO: I think I can move even more of the animation logic into Glyph
        if (circleAnimated) {
            advanceCircle()
            matrixManager.setMatrixFrame(animationFrameBuilder.build(applicationContext).render())
            animationHandler.postDelayed(tickRunnable, tickRate.toLong())
        }
    }

    override fun stopAnimation() {
        super.stopAnimation()
        circleAnimated = false
    }

    override fun performOnServiceDisconnected(context: Context) {
        if (this::textFrame.isInitialized) textFrame.stopMarquee()
        stopAnimation()
        backgroundScope.cancel()
    }
}