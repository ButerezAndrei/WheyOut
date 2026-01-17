package com.bituwy.wheyout.glyphs

import android.content.Context
import com.nothing.ketchum.GlyphMatrixManager

class WeightGlyph : Glyph("Weight") {
    private lateinit var matrixManager: GlyphMatrixManager

    override fun performOnServiceConnected(
        context: Context,
        glyphMatrixManager: GlyphMatrixManager
    ) {
        matrixManager = glyphMatrixManager
        startAnimation()
    }

    override fun performOnServiceDisconnected(context: Context) {
    }
}