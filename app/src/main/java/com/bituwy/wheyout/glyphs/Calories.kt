package com.bituwy.wheyout.glyphs

import android.content.Context
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
import kotlin.random.Random

class Calories : GlyphMatrixService("Calories") {

    private val backgroundScope = CoroutineScope(Dispatchers.IO)
    private val uiScope = CoroutineScope(Dispatchers.Main)
    private val rainScreen = IntArray(SCREEN_LENGTH * SCREEN_LENGTH)
    val textBuilder = GlyphMatrixObject.Builder()
    val rainBuilder = GlyphMatrixObject.Builder()
    val frameBuilder = GlyphMatrixFrame.Builder()
    var frame: GlyphMatrixFrame? = null

    lateinit var caloriesTracker: CaloriesTracker

    suspend fun setRemainingCalories() {
        // TODO: Make the start of day configurable
        val startOfDay = LocalDate.now().atTime(4, 0)
        val remainingCalories = caloriesTracker.remaining(startOfDay).toInt()
        val (horizontalCenterOffset, verticalCenterOffset) = getCenteredTextOffsets(remainingCalories.toString())

        val remainingText = textBuilder.setText(remainingCalories.toString())
                                       .setPosition(horizontalCenterOffset, verticalCenterOffset)
        frameBuilder.addTop(remainingText.build())
    }

    fun updateRain() {
        advanceDroplets()
        if (Random.nextInt(100) < 8) generateDroplets()
        val rainFrame = rainBuilder
            .setRawArray(rainScreen)
            .build()
        if (rainFrame != null) frameBuilder.addLow(rainFrame)
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

        frame = frameBuilder
            .addLow(rainScreen)
            .build(applicationContext)

        glyphMatrixManager.setMatrixFrame(frame?.render())

        backgroundScope.launch {
            setRemainingCalories()
            while (isActive) {
                updateRain()
                frame = frameBuilder.build(applicationContext)

                uiScope.launch {
                    glyphMatrixManager.setMatrixFrame(frame?.render())
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