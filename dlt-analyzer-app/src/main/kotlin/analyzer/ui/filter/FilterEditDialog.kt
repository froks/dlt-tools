package analyzer.ui.filter

import analyzer.filter.DltMessageFilter
import analyzer.filter.FilterAction
import analyzer.filter.toMessageTypeInfo
import com.formdev.flatlaf.FlatClientProperties
import com.formdev.flatlaf.extras.FlatSVGIcon
import com.jgoodies.forms.builder.FormBuilder
import com.jgoodies.forms.layout.CellConstraints
import dltcore.MessageType
import dltcore.MessageTypeInfo
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import javax.swing.*

class FilterEditDialog {
    private var active: JCheckBox
    private var name: JTextField
    private var positive: JRadioButton
    private var negative: JRadioButton
    private var marker: JRadioButton
    private var appIdActive: JCheckBox
    private var appId: JTextField
    private var loglevelActive: JCheckBox
    private var loglevelMin: JComboBox<String>
    private var loglevelMax: JComboBox<String>
    private var messageActive: JCheckBox
    private var message: JTextField
    private var messageIsCaseSensitive: JToggleButton
    private var messageIsRegex: JToggleButton
    private var colorButton: JButton
    private var colorActive: JCheckBox

    private var textColor: Color
        get() = (this.colorButton.icon as ColorIcon).color
        set(v) { (this.colorButton.icon as ColorIcon).color = v }

    private fun updateColorActiveEnabled() {
        colorActive.isEnabled = positive.isSelected
        if (marker.isSelected) {
            colorActive.isSelected = true
        } else if (positive.isSelected || negative.isSelected) {
            colorActive.isSelected = false
        }
    }

    init {
        colorActive = createCheckBox("Color")

        active = createCheckBox(null)
        name = JTextField()
        val type = ButtonGroup()
        positive = createRadioButton("Positive", type).also { btn ->
            btn.addActionListener { updateColorActiveEnabled() }
        }
        negative = createRadioButton("Negative", type).also { btn ->
            btn.addActionListener { updateColorActiveEnabled() }
        }
        marker = createRadioButton("Marker", type).also { btn ->
            btn.addActionListener { updateColorActiveEnabled() }
        }
        appIdActive = createCheckBox("App Id")
        appId = createTextField(appIdActive)

        loglevelActive = createCheckBox("Level")
        val loglevel = MessageTypeInfo.entries.filter { it.type == MessageType.DLT_TYPE_LOG }.map { it.shortText }
        loglevelMin = JComboBox(loglevel.toTypedArray())
        loglevelMax = JComboBox(loglevel.toTypedArray())

        messageActive = createCheckBox("Message")
        val textColor = UIManager.getColor("TextField.foreground")
        colorButton = JButton(ColorIcon(textColor))

        colorButton.addActionListener {
            val newColor = JColorChooser.showDialog(colorButton, "Choose a color", textColor, false)
            this.textColor = newColor ?: this.textColor
        }

        messageIsCaseSensitive = JToggleButton(FlatSVGIcon("icons/textfield/matchCase.svg")).also {
            it.rolloverIcon = FlatSVGIcon("icons/textfield/matchCaseHovered.svg")
            it.selectedIcon = FlatSVGIcon("icons/textfield/matchCaseSelected.svg")
            it.toolTipText = "Match Case"
        }

        messageIsRegex = JToggleButton(FlatSVGIcon("icons/textfield/regex.svg")).also {
            it.rolloverIcon = FlatSVGIcon("icons/textfield/regexHovered.svg")
            it.selectedIcon = FlatSVGIcon("icons/textfield/regexSelected.svg")
            it.toolTipText = "Regular Expression"
        }
        message = createTextField(messageActive).also { tf ->
            val tb = JToolBar()
            tb.add(messageIsCaseSensitive)
            tb.add(messageIsRegex)
            tf.putClientProperty(FlatClientProperties.TEXT_FIELD_TRAILING_COMPONENT, tb)
        }
    }

    private fun createTextField(linkedCheckBox: JCheckBox): JTextField =
        JTextField().also { tf ->
            tf.addKeyListener(object : KeyListener {
                override fun keyTyped(e: KeyEvent) {
                    linkedCheckBox.isSelected = tf.text.isNotEmpty()
                }

                override fun keyPressed(e: KeyEvent) {
                }

                override fun keyReleased(e: KeyEvent) {
                }
            })
        }

    private fun createRadioButton(text: String?, group: ButtonGroup): JRadioButton =
        JRadioButton(text).also {
            it.border = BorderFactory.createEmptyBorder()
            group.add(it)
        }

    private fun createCheckBox(text: String?): JCheckBox =
        JCheckBox(text).also { it.border = BorderFactory.createEmptyBorder() }

    private fun saveFilter(): DltMessageFilter {
        val action =
            if (positive.isSelected) FilterAction.INCLUDE
            else if (negative.isSelected) FilterAction.EXCLUDE
            else FilterAction.MARKER

        return DltMessageFilter(
            active = active.isSelected,
            filterName = name.text.trim(),
            action = action,
            appIdActive = appIdActive.isSelected,
            appId = appId.text.trim(),
            contextIdActive = false,
            contextId = null,
            timestampActive = false,
            timestampStart = null,
            timestampEnd = null,
            messageTypeActive = loglevelActive.isSelected,
            messageTypeMin = (loglevelMin.selectedItem as String?)?.toMessageTypeInfo(),
            messageTypeMax = (loglevelMax.selectedItem as String?)?.toMessageTypeInfo(),
            searchTextActive = messageActive.isSelected,
            searchText = message.text,
            searchCaseInsensitive = !messageIsCaseSensitive.isSelected,
            searchTextIsRegEx = messageIsRegex.isSelected,
            markerActive = colorActive.isSelected,
            textColor = textColor,
            tags = emptySet()
        )
    }

    private fun check(): Boolean {
        var isValid = true


        if (name.text.isBlank()) {
            isValid = false
            name.putClientProperty(FlatClientProperties.OUTLINE, FlatClientProperties.OUTLINE_ERROR)
        } else {
            name.putClientProperty(FlatClientProperties.OUTLINE, null)
        }

        if (appIdActive.isSelected) {
            if (appId.text.isBlank()) {
                isValid = false
                appId.putClientProperty(FlatClientProperties.OUTLINE, FlatClientProperties.OUTLINE_ERROR)
            } else {
                appId.putClientProperty(FlatClientProperties.OUTLINE, null)
            }
        } else {
            appId.putClientProperty(FlatClientProperties.OUTLINE, null)
        }

        if (messageActive.isSelected) {
            if (message.text.isEmpty()) {
                isValid = false
                message.putClientProperty(FlatClientProperties.OUTLINE, FlatClientProperties.OUTLINE_ERROR)
            } else {
                message.putClientProperty(FlatClientProperties.OUTLINE, null)
            }
        } else {
            message.putClientProperty(FlatClientProperties.OUTLINE, null)
        }

        return isValid
    }

    private fun showDialog(owner: JFrame, filter: DltMessageFilter?): DltMessageFilter? {
        var isOk = false
        val dialog = JDialog(owner, getTitle(filter), true)
        dialog.setLocationRelativeTo(owner)
        dialog.isResizable = false

        dialog.size = Dimension(800, 600)
        dialog.defaultCloseOperation = WindowConstants.DISPOSE_ON_CLOSE

        val okButton = JButton("OK").also { btn ->
            btn.addActionListener {
                if (!check()) {
                    return@addActionListener
                }
                isOk = true
                dialog.isVisible = false
            }
        }
        val cancelButton = JButton("Cancel").also { btn ->
            btn.addActionListener {
                isOk = false
                dialog.isVisible = false
            }
        }

        val colorButtonPanel = JPanel(FlowLayout(FlowLayout.LEFT))
        colorButtonPanel.add(colorButton)

        val loglevelPanel = JPanel(FlowLayout(FlowLayout.LEFT))
        loglevelPanel.add(loglevelMin)
        loglevelPanel.add(loglevelMax)

        val panel = FormBuilder.create()
            .padding("5dlu,5dlu,5dlu,5dlu")
            .columns("left:pref, 2dlu, pref:grow")
            .rows("pref, pref, pref, pref, 2dlu, pref, 2dlu, pref, pref, pref, 10dlu, pref")
            .addLabel("Active").xy(1, 1).add(active).xy(3, 1)
            .addLabel("Name").xy(1, 2).add(name).xy(3, 2)
            .addLabel("Type").xy(1, 3).addBar(positive, negative, marker).xy(3, 3)
            .add(colorActive).xy(1, 4).add(colorButtonPanel).xy(3, 4)
            .addSeparator(null).xyw(1, 6, 3)
            .add(loglevelActive).xy(1, 8).add(loglevelPanel).xy(3, 8)
            .add(appIdActive).xy(1, 9).add(appId).xy(3, 9)
            .add(messageActive).xy(1, 10).add(message).xy(3, 10)
            .addSeparator(null).xy(1, 11)
            .addBar(okButton, cancelButton).xyw(1, 12, 3, CellConstraints.RIGHT, CellConstraints.DEFAULT)
            .build()

        dialog.add(panel, BorderLayout.NORTH)

        loadFilter(filter)

        dialog.pack()
        dialog.isVisible = true

        if (isOk) {
            return saveFilter()
        }
        return null
    }

    private fun loadFilter(filter: DltMessageFilter?) {
        active.isSelected = filter?.active ?: true
        name.text = filter?.filterName ?: "New Filter"
        when (filter?.action) {
            FilterAction.INCLUDE, null -> {
                positive.isSelected = true
                negative.isSelected = false
                marker.isSelected = false
            }
            FilterAction.EXCLUDE -> {
                positive.isSelected = false
                negative.isSelected = true
                marker.isSelected = false
            }
            FilterAction.MARKER -> {
                positive.isSelected = false
                negative.isSelected = false
                marker.isSelected = true
            }
        }

        loglevelActive.isSelected = filter?.messageTypeActive ?: false
        loglevelMin.selectedIndex = loglevelMin.findItemIndex(filter?.messageTypeMin?.shortText)
        loglevelMax.selectedIndex = loglevelMax.findItemIndex(filter?.messageTypeMax?.shortText)

        appIdActive.isSelected = filter?.appIdActive ?: false
        appId.text = filter?.appId

        colorActive.isSelected = filter?.markerActive ?: false
        textColor = filter?.textColor ?: Color.white

        messageActive.isSelected = filter?.searchTextActive ?: false
        message.text = filter?.searchText
        messageIsCaseSensitive.isSelected = filter?.searchCaseInsensitive?.not() ?: true
        messageIsRegex.isSelected = filter?.searchTextIsRegEx ?: false
    }

    companion object {
        fun newFilter(owner: JFrame) =
            FilterEditDialog().showDialog(owner, null)

        fun editFilter(owner: JFrame, filter: DltMessageFilter) =
            FilterEditDialog().showDialog(owner, filter)

        private fun getTitle(filter: DltMessageFilter?): String =
            when (filter) {
                null -> "Create new filter"
                else -> "Edit '${filter.filterName}'"
            }
    }
}


fun JComboBox<String>.findItemIndex(value: String?): Int {
    if (value == null) {
        return -1
    }
    for (i in 0 until this.itemCount) {
        if (this.getItemAt(i) == value) {
            return i
        }
    }
    return -1
}
