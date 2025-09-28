package com.example.teamozy.feature.face.util

import android.graphics.Bitmap
import android.graphics.Matrix
fun Bitmap.rotateAndMirror(rotationDegrees: Int, mirror: Boolean = true): Bitmap {
    val m = Matrix()
    if (rotationDegrees != 0) m.postRotate(rotationDegrees.toFloat())
    if (mirror) m.postScale(-1f, 1f)
    return Bitmap.createBitmap(this, 0, 0, width, height, m, true)
}