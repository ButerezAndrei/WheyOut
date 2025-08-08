package com.bituwy.wheyout.glyphs

import android.content.Context
import com.bituwy.wheyout.GlyphMatrixService
import com.nothing.ketchum.GlyphMatrixFrame
import com.nothing.ketchum.GlyphMatrixManager
import com.nothing.ketchum.GlyphMatrixObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sqrt
import kotlin.random.Random


class Calories : GlyphMatrixService("Calories") {

    private val backgroundScope = CoroutineScope(Dispatchers.IO)

    private val uiScope = CoroutineScope(Dispatchers.Main)

    private val screenLength = 25
    private val rainScreen = IntArray(screenLength * screenLength
    )
    val textBuilder = GlyphMatrixObject.Builder()
    val text: GlyphMatrixObject? = textBuilder
        .setText("Hello")
        .setPosition(0, 9)
        .build()
    val rainBuilder = GlyphMatrixObject.Builder()
    var rain: GlyphMatrixObject? = rainBuilder.build()
    val frameBuilder = GlyphMatrixFrame.Builder()
    var frame: GlyphMatrixFrame? = null

    fun updateRain() {
        advanceDroplets()
        if (Random.nextInt(100) < 8) generateDroplets()
        rain = rainBuilder
            .setRawArray(rainScreen)
            .build()
        frame = frameBuilder
            .addTop(text)
            .addMid(rain)
            .build(applicationContext)
    }

    fun advanceDroplets() {
        var dropletPosition = 0
        var oldDropletPosition = 0
        for (row in 25 downTo 2) {
            for (column in 1..25) {
                oldDropletPosition = ((row-1)*25) - column
                dropletPosition = (row*25) - column
                if(rainScreen[oldDropletPosition] != 0) {
                    rainScreen[dropletPosition] = rainScreen[oldDropletPosition]
                    rainScreen[oldDropletPosition] = (rainScreen[oldDropletPosition].toDouble() * 0.8).toInt()
                }
            }
        }
    }

    fun generateDroplets() {
        rainScreen.forEachIndexed { index, value ->
            if (index > 24) return@forEachIndexed
            if (Random.nextInt(100) < 8) rainScreen[index] = 2047
        }
    }

    override fun performOnServiceConnected(
        context: Context,
        glyphMatrixManager: GlyphMatrixManager
    ) {
        generateDroplets()
        frame = frameBuilder
            .addTop(text)
            .addMid(rainScreen)
            .build(applicationContext)

        glyphMatrixManager.setMatrixFrame(frame?.render())
        backgroundScope.launch {
            while (isActive) {
                updateRain()
                uiScope.launch {
                    glyphMatrixManager.setMatrixFrame(frame?.render())
                }
                // wait a bit
                delay(30)
            }
        }
    }

    override fun performOnServiceDisconnected(context: Context) {
        backgroundScope.cancel()
    }
}