package analyzer.ui.swing

import java.awt.Color
import java.awt.Graphics
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import javax.swing.plaf.basic.BasicTextFieldUI

class HintTextFieldUI(
    var hint: String,
    var hideOnFocus: Boolean = false,
    var color: Color? = null
) : BasicTextFieldUI(), FocusListener {
    private fun repaint() {
        if (component != null) {
            component.repaint()
        }
    }

    override fun paintSafely(g: Graphics) {
        super.paintSafely(g)

        if (component.text.isEmpty() && !(hideOnFocus && component.hasFocus())) {
            if (color != null) {
                g.color = color
            } else {
                g.color = component.foreground.brighter().brighter().brighter()
            }
            val width = g.fontMetrics.stringWidth(hint)
            val padding = (component.height - component.font.size) / 2
            g.drawString(hint, component.width/2 - width/2, component.height - padding - 1)
        }
    }

    override fun focusGained(e: FocusEvent) {
        if (hideOnFocus) repaint()
    }

    override fun focusLost(e: FocusEvent) {
        if (hideOnFocus) repaint()
    }

    override fun installListeners() {
        super.installListeners()
        component.addFocusListener(this)
    }

    override fun uninstallListeners() {
        super.uninstallListeners()
        component.removeFocusListener(this)
    }
}
