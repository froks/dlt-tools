package analyzer.ui.helper

import java.awt.Component
import javax.swing.JComponent
import javax.swing.JFrame

fun JComponent.findParentFrame(): JFrame {
    var c: Component? = this
    while (c !is JFrame && c is Component) {
        c = c.parent
    }
    if (c == null) {
        throw ClassCastException("Component doesn't have parent frame")
    }
    return c as JFrame
}
