package analyzer.ui.filter

import com.formdev.flatlaf.icons.FlatAbstractIcon
import java.awt.Color
import java.awt.Component
import java.awt.Graphics2D

class ColorIcon(color: Color) : FlatAbstractIcon(16, 16, color) {
    var color: Color
        get() = super.color
        set(v) { super.color = v }

    override fun paintIcon(c: Component, g: Graphics2D) {
        g.color = super.color
        g.fillRoundRect(1, 1, width - 2, height - 2, 5, 5)
    }
}
