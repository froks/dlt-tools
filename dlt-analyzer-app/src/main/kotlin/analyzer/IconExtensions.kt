package analyzer

import jiconfont.IconCode
import jiconfont.swing.IconFontSwing
import java.awt.Color

fun IconCode.asIcon(size: Float = 32f, color: Color = Color.lightGray) =
    IconFontSwing.buildIcon(this, size, color)

fun IconCode.asImage(size: Float = 32f, color: Color = Color.lightGray) =
    IconFontSwing.buildImage(this, size, color)
