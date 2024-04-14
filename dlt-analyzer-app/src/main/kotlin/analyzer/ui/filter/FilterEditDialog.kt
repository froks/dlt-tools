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
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class FilterEditDialog {
    private var active: JCheckBox
    private var name: JTextField
    private var positive: JRadioButton
    private var negative: JRadioButton
    private var marker: JRadioButton
    private var ecuIdActive: JCheckBox
    private var ecuId: JTextField
    private var appIdActive: JCheckBox
    private var appId: JTextField
    private var contextIdActive: JCheckBox
    private var contextId: JTextField
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

        ecuIdActive = createCheckBox("ECU Id")
        ecuId = createTextField(ecuIdActive)

        appIdActive = createCheckBox("App Id")
        appId = createTextField(appIdActive)

        contextIdActive = createCheckBox("Context Id")
        contextId = createTextField(contextIdActive)

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
//            tb.add(messageIsRegex)
            tf.putClientProperty(FlatClientProperties.TEXT_FIELD_TRAILING_COMPONENT, tb)
        }
    }

    private fun createTextField(linkedCheckBox: JCheckBox): JTextField =
        JTextField().also { tf ->
            tf.document.addDocumentListener(object : DocumentListener {
                override fun insertUpdate(e: DocumentEvent) {
                    linkedCheckBox.isSelected = e.document.length > 0
                }

                override fun removeUpdate(e: DocumentEvent) {
                    linkedCheckBox.isSelected = e.document.length > 0
                }

                override fun changedUpdate(e: DocumentEvent) {
                    linkedCheckBox.isSelected = e.document.length > 0
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
            ecuIdActive = ecuIdActive.isSelected,
            ecuId = ecuId.text.trim(),
            appIdActive = appIdActive.isSelected,
            appId = appId.text.trim(),
            contextIdActive = contextIdActive.isSelected,
            contextId = contextId.text.trim(),
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

    private fun checkTextField(checkBox: JCheckBox?, textField: JTextField): Boolean {
        if (checkBox == null || checkBox.isSelected) {
            if (textField.text.isBlank()) {
                textField.putClientProperty(FlatClientProperties.OUTLINE, FlatClientProperties.OUTLINE_ERROR)
                return false
            } else {
                textField.putClientProperty(FlatClientProperties.OUTLINE, null)
            }
        } else {
            textField.putClientProperty(FlatClientProperties.OUTLINE, null)
        }
        return true
    }

    private fun check(): Boolean {
        val isValid = checkTextField(null, name) &&
                checkTextField(ecuIdActive, ecuId) &&
                checkTextField(appIdActive, appId) &&
                checkTextField(contextIdActive, contextId) &&
                checkTextField(messageActive, message)

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
            dialog.rootPane.defaultButton = btn
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
            .rows("pref, pref, pref, pref, 2dlu, pref, 2dlu, pref, pref, pref, pref, pref, 10dlu, pref")
            .addLabel("Active").xy(1, 1).add(active).xy(3, 1)
            .addLabel("Name").xy(1, 2).add(name).xy(3, 2)
            .addLabel("Type").xy(1, 3).addBar(positive, negative, marker).xy(3, 3)
            .add(colorActive).xy(1, 4).add(colorButtonPanel).xy(3, 4)
            .addSeparator(null).xyw(1, 6, 3)
            .add(loglevelActive).xy(1, 8).add(loglevelPanel).xy(3, 8)
            .add(ecuIdActive).xy(1, 9).add(ecuId).xy(3, 9)
            .add(appIdActive).xy(1, 10).add(appId).xy(3, 10)
            .add(contextIdActive).xy(1, 11).add(contextId).xy(3, 11)
            .add(messageActive).xy(1, 12).add(message).xy(3, 12)
            .addSeparator(null).xy(1, 13)
            .addBar(okButton, cancelButton).xyw(1, 14, 3, CellConstraints.RIGHT, CellConstraints.DEFAULT)
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

        ecuIdActive.isSelected = filter?.ecuIdActive ?: false
        ecuId.text = filter?.ecuId

        appIdActive.isSelected = filter?.appIdActive ?: false
        appId.text = filter?.appId

        contextIdActive.isSelected = filter?.contextIdActive ?: false
        contextId.text = filter?.contextId

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
