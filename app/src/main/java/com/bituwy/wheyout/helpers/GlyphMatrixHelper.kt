package com.bituwy.wheyout.helpers

import android.content.Context
import com.nothing.ketchum.GlyphMatrixObject
import com.nothing.ketchum.GlyphMatrixUtils


class GlyphMatrixHelper(val context: Context){

    enum class CenterOptions { BOTH, VERTICAL, HORIZONTAL}
    val textBuilder = GlyphMatrixObject.Builder()

    fun buildCenteredText(screenSize: Int, text: String, centerBy: GlyphMatrixHelper.CenterOptions = CenterOptions.BOTH): GlyphMatrixObject? {
        var (horizontalCenterOffset, verticalCenterOffset) = getCenteredTextOffsets(
            Pair(screenSize, screenSize),
            text
        )

        when (centerBy) {
            CenterOptions.BOTH -> {}
            CenterOptions.VERTICAL -> horizontalCenterOffset = 0
            CenterOptions.HORIZONTAL -> verticalCenterOffset = 0
        }

        return textBuilder
                .setText(text, 1)
                .setPosition(horizontalCenterOffset, verticalCenterOffset)
                .build()
    }


    fun getCenteredTextOffsets(offsets: Pair<Int, Int>, text: String) : Pair<Int, Int>{
        val letters = GlyphMatrixUtils.getLetterConfigs(text, context, null)
        val horizontalLength = GlyphMatrixUtils.getLetterMaxLength(letters, false)
        val verticalLength = letters.first().height
        val horizontalCenterOffset = (offsets.first.dec() - horizontalLength)/2
        val verticalCenterOffset = (offsets.second.dec() - verticalLength)/2

        return horizontalCenterOffset to verticalCenterOffset
    }
}