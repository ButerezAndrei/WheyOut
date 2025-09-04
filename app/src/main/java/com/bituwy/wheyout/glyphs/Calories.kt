package com.bituwy.wheyout.glyphs

import android.content.Context
import android.graphics.Point
import com.bituwy.wheyout.GlyphMatrixService
import com.bituwy.wheyout.model.CaloriesTracker
import com.nothing.ketchum.GlyphMatrixFrame
import com.nothing.ketchum.GlyphMatrixManager
import com.nothing.ketchum.GlyphMatrixObject
import com.nothing.ketchum.GlyphMatrixUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.random.Random
class Calories : GlyphMatrixService("Calories") {

    private val backgroundScope = CoroutineScope(Dispatchers.IO)
    private val uiScope = CoroutineScope(Dispatchers.Main)
    private val rainScreen = IntArray(SCREEN_LENGTH * SCREEN_LENGTH)
    val textBuilder = GlyphMatrixObject.Builder()
    val frameBuilder = GlyphMatrixFrame.Builder()

    lateinit var caloriesTracker: CaloriesTracker

    suspend fun setRemainingCalories() {
        // TODO: Make the start of day configurable
        val startOfDay = LocalDate.now().atTime(4, 0)
        val remainingCalories = caloriesTracker.remaining(startOfDay).toInt()
        val (horizontalCenterOffset, verticalCenterOffset) = getCenteredTextOffsets(remainingCalories.toString())

        val remainingText = textBuilder.setText(remainingCalories.toString())
                                       .setPosition(horizontalCenterOffset, verticalCenterOffset)
        val consumed = caloriesTracker.consumed(startOfDay).toInt()

        // TODO: Improve the interface of the calories target in order to not repeat calcs
        circlePercent = consumed/caloriesTracker.target
        frameBuilder.addTop(remainingText.build())
    }


    var circleAnimationStep = 0.0
    var circlePercent = 0.0

    fun advanceCircle() {
        if (circleAnimationStep + 0.007 >= circlePercent) return
        circleAnimationStep += 0.007
        val circleScreen = generateCircleProgress(circleAnimationStep)
        frameBuilder.addMid(circleScreen)
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
        val rawArray = IntArray(SCREEN_LENGTH * SCREEN_LENGTH)

        for (i in 0..(circleProgressSteps.size * percentDone).roundToInt().dec()) {
            val column = circleProgressSteps[i].x
            val row = circleProgressSteps[i].y
            rawArray[column + (row*SCREEN_LENGTH)] = 2047
        }

        return rawArray
    }

    fun updateRain() {
        advanceDroplets()
        if (Random.nextInt(100) < 8) generateDroplets()
    }

    fun advanceDroplets() {
        var dropletPosition = 0
        var oldDropletPosition = 0
        for (row in SCREEN_LENGTH downTo 2) {
            for (column in 1..SCREEN_LENGTH) {
                oldDropletPosition = ((row-1)*SCREEN_LENGTH) - column
                dropletPosition = (row*SCREEN_LENGTH) - column
                if(rainScreen[oldDropletPosition] != 0) {
                    rainScreen[dropletPosition] = rainScreen[oldDropletPosition]
                    rainScreen[oldDropletPosition] = (rainScreen[oldDropletPosition].toDouble() * 0.8).toInt()
                }
            }
        }
    }

    fun generateDroplets() {
        rainScreen.forEachIndexed { index, value ->
            if (index > SCREEN_LENGTH.dec()) return@forEachIndexed
            if (Random.nextInt(100) < 8) rainScreen[index] = 2047
        }
    }

    override fun performOnServiceConnected(
        context: Context,
        glyphMatrixManager: GlyphMatrixManager
    ) {
        caloriesTracker = CaloriesTracker(applicationContext)
        generateDroplets()

        var frame = frameBuilder
            //TODO: Refactor the effects to be modular
//            .addLow(rainScreen)
            .build(applicationContext)

        glyphMatrixManager.setMatrixFrame(frame.render())

        backgroundScope.launch {
            setRemainingCalories()
            while (isActive) {
//                updateRain()
                advanceCircle()
                frame = frameBuilder.build(applicationContext)

                uiScope.launch {
                    glyphMatrixManager.setMatrixFrame(frame.render())
                }

                delay(30)
            }
        }
    }

    fun getCenteredTextOffsets(text: String) : Pair<Int, Int>{
        val letters = GlyphMatrixUtils.getLetterConfigs(text, applicationContext, null)
        val horizontalLength = GlyphMatrixUtils.getLetterMaxLength(letters, false)
        val verticalLength = letters.first().height
        val horizontalCenterOffset = (SCREEN_LENGTH.dec() - horizontalLength)/2
        val verticalCenterOffset = (SCREEN_LENGTH.dec() - verticalLength)/2

        return horizontalCenterOffset to verticalCenterOffset
    }

    override fun performOnServiceDisconnected(context: Context) {
        backgroundScope.cancel()
    }

    private companion object {
        private const val SCREEN_LENGTH = 25
    }
}