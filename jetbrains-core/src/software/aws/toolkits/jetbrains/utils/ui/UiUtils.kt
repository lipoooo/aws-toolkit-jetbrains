// Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0
@file:JvmName("UiUtils")

package software.aws.toolkits.jetbrains.utils.ui

import com.intellij.icons.AllIcons
import com.intellij.ide.HelpTooltip
import com.intellij.lang.Language
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.GraphicsConfig
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.CellRendererPanel
import com.intellij.ui.ClickListener
import com.intellij.ui.EditorTextField
import com.intellij.ui.JBColor
import com.intellij.ui.JreHiDpiUtil
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.layout.Cell
import com.intellij.ui.layout.CellBuilder
import com.intellij.ui.layout.ComponentPredicate
import com.intellij.ui.layout.PropertyBinding
import com.intellij.ui.layout.Row
import com.intellij.ui.layout.applyToComponent
import com.intellij.ui.layout.toBinding
import com.intellij.ui.paint.LinePainter2D
import com.intellij.ui.speedSearch.SpeedSearchSupply
import com.intellij.util.text.DateFormatUtil
import com.intellij.util.text.SyncDateFormat
import com.intellij.util.ui.GraphicsUtil
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import org.jetbrains.annotations.Nls
import software.aws.toolkits.jetbrains.ui.KeyValueTextField
import software.aws.toolkits.jetbrains.utils.formatText
import java.awt.AlphaComposite
import java.awt.Color
import java.awt.Component
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Shape
import java.awt.event.MouseEvent
import java.awt.geom.RoundRectangle2D
import java.text.SimpleDateFormat
import javax.swing.AbstractButton
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JTable
import javax.swing.JTextArea
import javax.swing.JTextField
import javax.swing.ListModel
import javax.swing.table.TableCellRenderer
import javax.swing.text.Highlighter
import javax.swing.text.JTextComponent
import kotlin.reflect.KMutableProperty0

fun JTextField?.blankAsNull(): String? = if (this?.text?.isNotBlank() == true) {
    text
} else {
    null
}

@Suppress("UNCHECKED_CAST")
fun <T> JComboBox<T>?.selected(): T? = this?.selectedItem as? T

fun EditorTextField.formatAndSet(content: String, language: Language) {
    CommandProcessor.getInstance().runUndoTransparentAction {
        val formatted = formatText(this.project, language, content)
        runWriteAction {
            document.setText(formatted)
        }
    }
}

/**
 * Allows triggering [button] selection based on clicking on receiver component
 */
@JvmOverloads
fun JComponent.addQuickSelect(button: AbstractButton, postAction: Runnable? = null) {
    object : ClickListener() {
        override fun onClick(event: MouseEvent, clickCount: Int): Boolean {
            if (button.isSelected) {
                return false
            }
            button.isSelected = true
            postAction?.run()
            return true
        }
    }.installOn(this)
}

fun <T> ListModel<T>.find(predicate: (T) -> Boolean): T? {
    for (i in 0 until size) {
        val element = getElementAt(i)?.takeIf(predicate)
        if (element != null) {
            return element
        }
    }
    return null
}

// Error messages do not work on a disabled component. May be a JetBrains bug?
fun JComponent.validationInfo(message: String) = when {
    isEnabled -> ValidationInfo(message, this)
    else -> ValidationInfo(message)
}

val BETTER_GREEN = JBColor(Color(104, 197, 116), JBColor.GREEN.darker())

/**
 * Fork of JetBrain's intellij-community UIUtils.drawSearchMatch allowing us to highlight multiple a multi-line
 * text field (startY was added and used instead of constants). Also auto-converted to Kotlin by Intellij
 */
fun drawSearchMatch(
    g: Graphics2D,
    startX: Float,
    endX: Float,
    startY: Float,
    height: Int
) {
    val color1 = JBColor.namedColor("SearchMatch.startBackground", JBColor.namedColor("SearchMatch.startColor", 0xffeaa2))
    val color2 = JBColor.namedColor("SearchMatch.endBackground", JBColor.namedColor("SearchMatch.endColor", 0xffd042))
    drawSearchMatch(g, startX, endX, startY, height, color1, color2)
}

fun drawSearchMatch(graphics2D: Graphics2D, startXf: Float, endXf: Float, startY: Float, height: Int, gradientStart: Color, gradientEnd: Color) {
    val config = GraphicsConfig(graphics2D)
    var alpha = JBUI.getInt("SearchMatch.transparency", 70) / 100f
    alpha = if (alpha < 0 || alpha > 1) 0.7f else alpha
    graphics2D.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha)
    graphics2D.paint = UIUtil.getGradientPaint(startXf, startY + 2f, gradientStart, startXf, startY - height - 5.toFloat(), gradientEnd)
    if (JreHiDpiUtil.isJreHiDPI(graphics2D)) {
        val c = GraphicsUtil.setupRoundedBorderAntialiasing(graphics2D)
        graphics2D.fill(RoundRectangle2D.Float(startXf, startY + 2, endXf - startXf, (height - 4).toFloat(), 5f, 5f))
        c.restore()
        config.restore()
        return
    }
    val startX = startXf.toInt()
    val endX = endXf.toInt()
    graphics2D.fillRect(startX, startY.toInt() + 3, endX - startX, height - 5)
    val drawRound = endXf - startXf > 4
    if (drawRound) {
        LinePainter2D.paint(graphics2D, startX - 1.toDouble(), startY + 4.0, startX - 1.toDouble(), startY + height - 4.toDouble())
        LinePainter2D.paint(graphics2D, endX.toDouble(), startY + 4.0, endX.toDouble(), startY + height - 4.toDouble())
        graphics2D.color = Color(100, 100, 100, 50)
        LinePainter2D.paint(graphics2D, startX - 1.toDouble(), startY + 4.0, startX - 1.toDouble(), startY + height - 4.toDouble())
        LinePainter2D.paint(graphics2D, endX.toDouble(), startY + 4.0, endX.toDouble(), startY + height - 4.toDouble())
        LinePainter2D.paint(graphics2D, startX.toDouble(), startY + 3.0, endX - 1.toDouble(), startY + 3.0)
        LinePainter2D.paint(graphics2D, startX.toDouble(), startY + height - 3.toDouble(), endX - 1.toDouble(), startY + height - 3.toDouble())
    }
    config.restore()
}

fun Component.setSelectionHighlighting(table: JTable, isSelected: Boolean) {
    if (isSelected) {
        foreground = table.selectionForeground
        background = table.selectionBackground
    } else {
        foreground = table.foreground
        background = table.background
    }
}

private class SpeedSearchHighlighter : Highlighter.HighlightPainter {
    override fun paint(g: Graphics?, startingPoint: Int, endingPoint: Int, bounds: Shape?, component: JTextComponent?) {
        component ?: return
        val graphics = g as? Graphics2D ?: return
        val beginningRect = component.modelToView(startingPoint)
        val endingRect = component.modelToView(endingPoint)
        drawSearchMatch(graphics, beginningRect.x.toFloat(), endingRect.x.toFloat(), beginningRect.y.toFloat(), beginningRect.height)
    }
}

private fun JTextArea.speedSearchHighlighter(speedSearchEnabledComponent: JComponent) {
    // matchingFragments does work with wrapped text but not around words if they are wrapped, so it will also need to be extended
    // in the future
    val speedSearch = SpeedSearchSupply.getSupply(speedSearchEnabledComponent) ?: return
    val fragments = speedSearch.matchingFragments(text)?.iterator() ?: return
    fragments.forEach {
        highlighter?.addHighlight(it.startOffset, it.endOffset, SpeedSearchHighlighter())
    }
}

fun CellBuilder<JComponent>.visible(visibility: Boolean): CellBuilder<JComponent> {
    component.isVisible = visibility
    return this
}

class WrappingCellRenderer(
    private val wrapOnSelection: Boolean = false,
    private val wrapOnToggle: Boolean = false,
    private val truncateAfterChars: Int? = null
) : CellRendererPanel(), TableCellRenderer {
    var wrap: Boolean = false

    private val textArea = JBTextArea()

    init {
        textArea.font = UIUtil.getLabelFont()
        textArea.wrapStyleWord = true

        add(textArea)
    }

    override fun getTableCellRendererComponent(table: JTable?, value: Any?, isSelected: Boolean, hasFocus: Boolean, row: Int, column: Int): Component {
        if (table == null) {
            return this
        }

        textArea.lineWrap = (wrapOnSelection && isSelected) || (wrapOnToggle && wrap)
        val text = (value as? String) ?: ""
        textArea.text = if (truncateAfterChars != null) {
            text.take(truncateAfterChars)
        } else {
            text
        }
        textArea.setSelectionHighlighting(table, isSelected)

        setSize(table.columnModel.getColumn(column).width, preferredSize.height)
        if (table.getRowHeight(row) != preferredSize.height) {
            table.setRowHeight(row, preferredSize.height)
        }

        textArea.speedSearchHighlighter(table)

        return this
    }
}

// TODO: figure out why this has weird hysteresis during rendering causing no text
class ResizingDateColumnRenderer(showSeconds: Boolean) : ResizingColumnRenderer() {
    private val formatter: SyncDateFormat = if (showSeconds) {
        SyncDateFormat(SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS"))
    } else {
        DateFormatUtil.getDateTimeFormat()
    }

    override fun getText(value: Any?): String? = (value as? String)?.toLongOrNull()?.let { formatter.format(it) }
}

class ResizingTextColumnRenderer : ResizingColumnRenderer() {
    override fun getText(value: Any?): String? = (value as? String)?.trim()
}

/**
 * When a panel is made of more than one panel, apply and the validation callbacks do not work as expected for
 * the child panels. This function makes it so the validation callbacks and apply are actually called.
 * @param applies An additional function that allows control based on visibility of other components or other factors
 */
fun CellBuilder<DialogPanel>.installOnParent(applies: () -> Boolean = { true }): CellBuilder<DialogPanel> {
    withValidationOnApply {
        if (!applies()) {
            null
        } else {
            val errors = this@installOnParent.component.validateCallbacks.mapNotNull { it() }
            if (errors.isEmpty()) {
                this@installOnParent.component.apply()
            }
            errors.firstOrNull()
        }
    }
    return this
}

/**
 * Similar to {@link com.intellij.ui.layout.CellBuilder#enableIf} but for component visibility
 */
fun <T : JComponent> CellBuilder<T>.visibleIf(predicate: ComponentPredicate): CellBuilder<T> {
    component.isVisible = predicate()
    predicate.addListener {
        component.isVisible = it
    }
    return this
}

fun Row.visibleIf(predicate: ComponentPredicate): Row {
    visible = predicate()
    predicate.addListener { visible = it }
    return this
}

val AbstractButton.notSelected: ComponentPredicate
    get() = object : ComponentPredicate() {
        override fun invoke(): Boolean = !isSelected

        override fun addListener(listener: (Boolean) -> Unit) {
            addChangeListener { listener(!isSelected) }
        }
    }

/**
 * Add a contextual help icon component
 */
fun Cell.contextualHelp(description: String): CellBuilder<JBLabel> {
    val l = JBLabel(AllIcons.General.ContextHelp)
    HelpTooltip().apply {
        setDescription(description)
        installOn(l)
    }
    return component(l)
}

fun CellBuilder<KeyValueTextField>.withBinding(binding: PropertyBinding<Map<String, String>>) =
    this.withBinding(
        componentGet = { component -> component.envVars },
        componentSet = { component, value -> component.envVars = value },
        binding
    )

fun Row.keyValueTextField(title: String? = null, property: KMutableProperty0<Map<String, String>>): CellBuilder<KeyValueTextField> {
    val field = if (title == null) {
        KeyValueTextField()
    } else {
        KeyValueTextField(title)
    }

    return component(field)
        .withBinding(property.toBinding())
}

fun <T : JComponent> CellBuilder<T>.toolTipText(@Nls text: String): CellBuilder<T> {
    applyToComponent {
        toolTipText = text
    }

    return this
}
