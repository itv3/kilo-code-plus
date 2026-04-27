---
name: jetbrains-ui-style
description: Use when creating, modifying, or reviewing Kotlin/Swing UI code for the Kilo JetBrains plugin. Applies to dialogs, settings pages, tool windows, forms, panels, component layout, sizing, spacing, colors, borders, icons, lists, trees, popups, notifications, and IntelliJ platform UI components.
---

# JetBrains UI Style

Use this skill whenever creating, modifying, or reviewing UI code in `packages/kilo-jetbrains`.

## Scope

This skill covers Kotlin/Swing UI code for the Kilo JetBrains plugin, including dialogs, settings pages, tool windows, forms, panels, component layout, sizing, spacing, colors, borders, icons, lists, trees, popups, notifications, and IntelliJ Platform UI components.

This plugin is a split-mode JetBrains plugin. UI code belongs in `frontend` unless there is a specific split-mode reason to place it elsewhere.

## Primary Rules

- Prefer Kotlin UI DSL v2 whenever the UI is a dialog, settings page, form, options panel, or structured component layout.
- Use manual Swing only for custom rendering, transcript/chat surfaces, highly dynamic panels, virtualized lists, custom painting, or layouts the DSL cannot express cleanly.
- Keep generated UI code minimal.
- Do not set default Swing properties explicitly. For example, avoid `isOpaque = false` unless the component default differs or there is a documented rendering reason.
- Avoid hardcoded dimensions. Prefer layout semantics, component defaults, DSL sizing helpers, and `JBUI` utilities.
- Do not add decorative helper functions, wrappers, or defensive UI code unless they materially improve clarity or correctness.
- Prefer IntelliJ platform components and theme-aware APIs.
- Put user-visible strings in `*.properties` files.

## Decision Tree

- Use Kotlin UI DSL v2 for dialogs, settings pages, forms, options panels, and structured component layouts.
- Use Kotlin UI DSL v2 inside tool windows when rendering form-like controls or structured subpanels.
- Use standard Swing with IntelliJ Platform component replacements for tool-window shells, action-driven UI, custom components, transcript surfaces, and custom renderers.
- Use the Action System for menus and toolbars.
- Do not use Kotlin Compose.
- Do not use JCEF.

| Need | API |
| --- | --- |
| Dialogs, settings pages, forms, any layout with components | Preferred: Kotlin UI DSL v2 (`com.intellij.ui.dsl.builder`) |
| Tool window panels, action-driven UI, custom components | Standard Swing with IntelliJ Platform component replacements |
| Menus and toolbars | Action System |

## Minimal Code Rule

Avoid explicit defaults and manual layout when the DSL can express the UI:

```kotlin
// Avoid
val panel = JPanel(BorderLayout()).apply {
    isOpaque = false
    preferredSize = Dimension(420, 260)
    border = EmptyBorder(12, 12, 12, 12)
}
```

Prefer DSL and platform spacing:

```kotlin
panel {
    row(KiloBundle.message("settings.name")) {
        textField()
            .align(AlignX.FILL)
            .resizableColumn()
    }
}
```

Avoid explicit default properties:

```kotlin
// Avoid unless there is a documented rendering reason
component.isOpaque = false
```

Prefer omitting the assignment and relying on the component/platform default.

Avoid raw fixed sizes:

```kotlin
// Avoid
component.preferredSize = Dimension(300, 40)
```

Prefer semantic sizing:

```kotlin
textField()
    .columns(COLUMNS_MEDIUM)
    .align(AlignX.FILL)
```

If a fixed size is genuinely required, use `JBUI.size(...)` or `JBUI.scale(...)` and keep the reason obvious from context.

## Do Not Use Kotlin Compose

Do not use Kotlin Compose or `intellij.platform.compose` in this plugin. The JetBrains modular template uses Compose for its demo tool window, but Kilo should use standard Swing with IntelliJ Platform components only. Keep all plugin UI in the existing Swing-based stack.

## Do Not Use JCEF

Do not use JCEF (`JBCefBrowser`) in this plugin. JCEF does not work in JetBrains remote development (split mode): the frontend process runs on the client machine but JCEF requires a display on the host, making it effectively unusable for remote users. Use standard Swing with IntelliJ Platform components for all UI.

## Kotlin UI DSL v2

Use Kotlin UI DSL v2 as the default way to build UI for dialogs, settings pages, forms, and any layout composed of standard components. It produces correct spacing, label alignment, HiDPI scaling, and accessibility automatically. Only fall back to manual Swing layout when you need a fully custom component, such as a canvas, rich list renderer, transcript surface, or tool-window chrome that the DSL cannot express.

The top-level builder is `panel { }` and returns `DialogPanel`. Structure is `panel -> row -> cells`. Cell factory methods such as `textField()`, `checkBox()`, and `label()` add components. The DSL lives in `com.intellij.ui.dsl.builder`.

To explore DSL capabilities interactively: Tools -> Internal Actions -> UI -> Kotlin UI DSL -> UI DSL Showcase. This requires internal mode: `-Didea.is.internal=true`.

### Basics: Panel, Row, Cell

Rows occupy full width. The last cell in a row takes remaining space. Rows have a `layout` property.

```kotlin
panel {
    row("Row1 label:") {
        textField()
        label("Some text")
    }
    row("Row2:") {
        label("This text is aligned with previous row")
    }
}
```

### Row Layout

Every row uses one of three layouts. Default is `LABEL_ALIGNED` when a label is provided for the row, `INDEPENDENT` otherwise.

| Layout | Behavior |
| --- | --- |
| `LABEL_ALIGNED` | Label column and content columns, aligned across rows |
| `INDEPENDENT` | All cells are independent, no cross-row alignment |
| `PARENT_GRID` | Cells align with the parent grid columns across rows |

```kotlin
panel {
    row("PARENT_GRID:") {
        label("Col 1")
        label("Col 2")
    }.layout(RowLayout.PARENT_GRID)

    row("PARENT_GRID:") {
        textField()
        textField()
    }.layout(RowLayout.PARENT_GRID)

    row("LABEL_ALIGNED default with label:") {
        textField()
    }

    row {
        label("INDEPENDENT default without label:")
        textField()
    }
}
```

### Components Reference

All cell factory methods available inside `row { }`:

| Method | Description |
| --- | --- |
| `checkBox("text")` | Checkbox |
| `threeStateCheckBox("text")` | Three-state checkbox |
| `radioButton("text", value)` | Radio button, must be inside `buttonsGroup {}` |
| `button("text") {}` | Push button |
| `actionButton(action)` | Icon button bound to an `AnAction` |
| `actionsButton(action1, action2, ...)` | Dropdown actions button |
| `segmentedButton(items) { text = it }` | Segmented control |
| `tabbedPaneHeader(items)` | Tab header strip |
| `label("text")` | Static label |
| `text("html")` | Rich text with links, icons, line-width control |
| `link("text") {}` | Focusable clickable link |
| `browserLink("text", "url")` | Opens URL in browser |
| `dropDownLink("default", listOf(...))` | Dropdown link selector |
| `icon(AllIcons.*)` | Icon display |
| `contextHelp("description", "title")` | Help icon with popup |
| `textField()` | Text input |
| `passwordField()` | Password input |
| `textFieldWithBrowseButton()` | Text field and browse dialog |
| `expandableTextField()` | Expandable multi-line text field |
| `extendableTextField()` | Text field with extension icons |
| `intTextField(range)` | Integer input with validation |
| `spinner(intRange)` / `spinner(doubleRange, step)` | Numeric spinner |
| `slider(min, max, minorTick, majorTick)` | Slider, use `.labelTable()` for tick labels |
| `textArea()` | Multi-line text, use `.rows(n)` and `.align(AlignX.FILL)` |
| `comboBox(items)` | Combo box / dropdown |
| `comment("text")` | Gray comment text, standalone |
| `cell(component)` | Wrap any arbitrary Swing component |
| `scrollCell(component)` | Wrap component in a scroll pane |
| `cell()` | Empty placeholder cell for grid alignment |

Key component examples:

```kotlin
panel {
    var color = "grey"

    buttonsGroup {
        row("Color:") {
            radioButton("White", "white")
            radioButton("Grey", "grey")
        }
    }.bind({ color }, { color = it })

    row("Slider:") {
        slider(0, 10, 1, 5)
            .labelTable(mapOf(
                0 to JBLabel("0"),
                5 to JBLabel("5"),
                10 to JBLabel("10"),
            ))
    }

    row {
        label("Text area:")
            .align(AlignY.TOP)
            .gap(RightGap.SMALL)
        textArea()
            .rows(5)
            .align(AlignX.FILL)
    }.layout(RowLayout.PARENT_GRID)
}
```

### Component Labels

Labels for modifiable components must be connected via one of two methods. This ensures correct spacing, mnemonic support, and accessibility.

- Row label: `row("&Label:") { textField() }`, mnemonic via `&`, label in left column
- Cell label: `textField().label("&Label:", LabelPosition.TOP)`, label attached to cell, optionally on top

```kotlin
panel {
    row("&Row label:") {
        textField()
        textField()
            .label("Cell label at &left:")
    }
    row {
        textField()
            .label("Cell label at &top:", LabelPosition.TOP)
    }
}
```

When a row contains a `checkBox` or `radioButton`, the DSL automatically increases space after the row label per IntelliJ UI Guidelines.

### Comments

Three types of comments, each with different placement and semantics:

| Type | Method | Placement |
| --- | --- | --- |
| Cell comment, bottom | `cell.comment("text")` | Below the cell |
| Cell comment, right | `cell.commentRight("text")` | Right of the cell |
| Cell context help | `cell.contextHelp("text", "title")` | Help icon with popup |
| Row comment | `row.rowComment("text")` | Below the entire row |
| Arbitrary comment | `comment("text")` | Standalone gray text |

```kotlin
panel {
    row {
        textField()
            .comment("Bottom comment")
        textField()
            .commentRight("Right comment")
        textField()
            .contextHelp("Help popup text")
    }

    row("Label:") {
        textField()
    }.rowComment("This comment sits below the whole row")

    row {
        comment("Standalone comment, supports <a href='link'>links</a> and <icon src='AllIcons.General.Information'>&nbsp;icons")
    }
}
```

Comments support HTML with clickable links, bundled icons via `<icon src='...'>`, and line width control via `maxLineLength`. Use `MAX_LINE_LENGTH_NO_WRAP` to prevent wrapping.

### Groups and Structure

| Method | Grid | Description |
| --- | --- | --- |
| `panel {}` | Own grid | Sub-panel occupying full width |
| `rowsRange {}` | Parent grid | Grouped rows sharing parent grid, useful with `enabledIf` |
| `group("Title") {}` | Own grid | Titled section with vertical spacing before/after |
| `groupRowsRange("Title") {}` | Parent grid | Titled section sharing parent grid alignment |
| `collapsibleGroup("Title") {}` | Own grid | Expandable section, Tab-focusable, supports mnemonics |
| `buttonsGroup("Title") {}` | None | Groups `radioButton` or `checkBox` under a title |
| `separator()` | None | Horizontal separator line |
| Row `panel {}` | Own grid | Sub-panel inside a cell |

```kotlin
panel {
    group("Settings") {
        row("Name:") { textField() }
        row("Path:") { textFieldWithBrowseButton() }
    }

    collapsibleGroup("Advanced") {
        row("Timeout:") { intTextField(0..1000) }
    }

    var enabled = true
    buttonsGroup("Mode:") {
        row { radioButton("Automatic", true) }
        row { radioButton("Manual", false) }
    }.bind({ enabled }, { enabled = it })

    separator()

    row {
        label("Nested panels:")
        panel {
            row("Sub row 1:") { textField() }
            row("Sub row 2:") { textField() }
        }
    }
}
```

### Gaps and Spacing

- Horizontal gaps: `cell.gap(RightGap.SMALL)` between a label-like checkbox and its related field. Medium gap is the default between cells.
- Two-column layout: `twoColumnsRow({}, {})` or `gap(RightGap.COLUMNS)` with `.layout(RowLayout.PARENT_GRID)`.
- Left indent: `indent {}` for indented sub-content.
- Vertical gaps: `.topGap(TopGap.MEDIUM)` / `.bottomGap(BottomGap.MEDIUM)` on rows to separate unrelated groups. Attach gaps to the related row so hiding rows does not break layout.

```kotlin
panel {
    group("Horizontal Gaps") {
        row {
            val cb = checkBox("Use mail:")
                .gap(RightGap.SMALL)
            textField()
                .enabledIf(cb.selected)
        }
        row("Width:") {
            textField()
                .gap(RightGap.SMALL)
            label("pixels")
        }
    }

    group("Indent") {
        row { label("Not indented") }
        indent {
            row { label("Indented row") }
        }
    }

    group("Two Columns") {
        twoColumnsRow({
            checkBox("First column")
        }, {
            checkBox("Second column")
        })
    }

    group("Vertical Gaps") {
        row { checkBox("Option 1") }
        row { checkBox("Option 2") }
        row { checkBox("Unrelated option") }
            .topGap(TopGap.MEDIUM)
    }
}
```

### Enabled and Visible State

Bind enabled/visible state to a checkbox or other observable. Works on rows, `indent {}` blocks, `rowsRange {}`, and individual cells.

```kotlin
panel {
    group("Enabled") {
        lateinit var cb: Cell<JBCheckBox>
        row { cb = checkBox("Enable options") }
        indent {
            row { checkBox("Option 1") }
            row { checkBox("Option 2") }
        }.enabledIf(cb.selected)
    }

    group("Visible") {
        lateinit var cb: Cell<JBCheckBox>
        row { cb = checkBox("Show options") }
        indent {
            row { checkBox("Option 1") }
            row { checkBox("Option 2") }
        }.visibleIf(cb.selected)
    }
}
```

### Binding

Bind component values to model properties. Values are applied on `DialogPanel.apply()`, checked with `.isModified()`, and reverted with `.reset()`.

| Method | Component |
| --- | --- |
| `bindSelected(model::prop)` | checkBox |
| `bindText(model::prop)` | textField |
| `bindIntText(model::prop)` | intTextField |
| `bindItem(model::prop.toNullableProperty())` | comboBox |
| `bindValue(model::prop)` | slider |
| `bindIntValue(model::prop)` | spinner |
| `buttonsGroup {}.bind(model::prop)` | radio group |

```kotlin
enum class Theme { LIGHT, DARK }

data class Settings(
    var name: String = "",
    var count: Int = 0,
    var enabled: Boolean = false,
    var theme: Theme = Theme.LIGHT,
)

val model = Settings()

val panel = panel {
    row("Name:") {
        textField().bindText(model::name)
    }
    row("Count:") {
        intTextField(0..100).bindIntText(model::count)
    }
    row {
        checkBox("Enabled").bindSelected(model::enabled)
    }
    buttonsGroup("Theme:") {
        row { radioButton("Light", Theme.LIGHT) }
        row { radioButton("Dark", Theme.DARK) }
    }.bind(model::theme)
}

panel.isModified()
panel.apply()
panel.reset()
```

### Validation

Attach input validation rules to cells. Rules run continuously and display inline error/warning indicators.

```kotlin
panel {
    row("Username:") {
        textField()
            .columns(COLUMNS_MEDIUM)
            .cellValidation {
                addInputRule("Must not be empty") {
                    it.text.isBlank()
                }
            }
    }
    row("Port:") {
        textField()
            .columns(COLUMNS_MEDIUM)
            .cellValidation {
                addInputRule("Contains non-numeric characters", level = Level.WARNING) {
                    it.text.contains(Regex("[^0-9]"))
                }
            }
    }
}
```

Activate validators by calling `dialogPanel.registerValidators(disposable)` after creating the panel.

### Tips and Common Patterns

| Pattern | Usage |
| --- | --- |
| `.bold()` | Bold text on any cell |
| `.columns(COLUMNS_MEDIUM)` | Set preferred width of textField / comboBox / textArea |
| `.text("initial")` | Set initial text on text components |
| `.resizableColumn()` | Column fills remaining horizontal space |
| `cell()` | Empty placeholder cell for grid alignment |
| `.widthGroup("name")` | Equalize widths across rows, cannot combine with `AlignX.FILL` |
| `.align(AlignX.FILL)` | Stretch component to fill available width |
| `.align(AlignY.TOP)` | Top-align component in its cell |
| `.applyToComponent { }` | Direct access to the underlying Swing component |
| `.selected(true)` | Default-select a radioButton when no bound value matches |
| `.gap(RightGap.COLUMNS)` | Column-level gap for multi-column layouts |

```kotlin
panel {
    row { label("Title").bold() }

    row("Name:") {
        textField()
            .columns(COLUMNS_MEDIUM)
            .resizableColumn()
            .align(AlignX.FILL)
    }

    row("") {
        textField()
    }.rowComment("""Use row("") for an empty label column that aligns with labeled rows""")

    row {
        text("Comment-colored text")
            .applyToComponent { foreground = JBUI.CurrentTheme.ContextHelp.FOREGROUND }
    }
}
```

## Manual Swing

Use manual Swing only when Kotlin UI DSL cannot express the UI cleanly.

Preferred manual Swing rules:

- Use IntelliJ platform components instead of raw Swing components.
- Use layout managers and component defaults rather than fixed sizes.
- Use `JBUI.Borders.empty(...)`, `JBUI.insets(...)`, `JBUI.size(...)`, and `JBUI.scale(...)` instead of raw AWT/Swing sizing primitives.
- Do not set default properties explicitly.
- Keep custom components small and focused.
- For painting, use theme-aware colors and `JBUI.scale(...)` for pixel values.

## Tool Windows

- Register declaratively in module XML via `com.intellij.toolWindow` extension point.
- Implement `ToolWindowFactory.createToolWindowContent()`, called lazily on first click.
- Use `SimpleToolWindowPanel(vertical = true)` as a convenient base for toolbar and content layout.
- Add tabs via `ToolWindow.contentManager`: create content with `ContentFactory.getInstance().createContent(component, title, isLockable)`, then `contentManager.addContent()`.
- For conditional display, implement `ToolWindowFactory.isApplicableAsync(project)`.
- Always use `ToolWindowManager.invokeLater()` instead of `Application.invokeLater()` for tool-window-related EDT tasks.

## Dialogs

- Extend `DialogWrapper`.
- Call `init()` from the constructor.
- Override `createCenterPanel()` to return UI content.
- Prefer Kotlin UI DSL v2 for panel contents.
- Override `getPreferredFocusedComponent()` for initial focus.
- Override `getDimensionServiceKey()` for size persistence when useful.
- Show with `showAndGet()` for modal boolean result, or `show()` and then `getExitCode()`.
- For input validation, call `initValidation()` in the constructor and override `doValidate()` to return `null` if valid or `ValidationInfo(message, component)` if invalid.

## Platform Components

Always use IntelliJ platform components instead of raw Swing where an equivalent exists.

| Instead of | Use | Package |
| --- | --- | --- |
| `JLabel` | `JBLabel` | `com.intellij.ui.components` |
| `JTextField` | `JBTextField` | `com.intellij.ui.components` |
| `JTextArea` | `JBTextArea` | `com.intellij.ui.components` |
| `JList` | `JBList` | `com.intellij.ui.components` |
| `JScrollPane` | `JBScrollPane` | `com.intellij.ui.components` |
| `JTable` | `JBTable` | `com.intellij.ui.table` |
| `JTree` | `Tree` | `com.intellij.ui.treeStructure` |
| `JSplitPane` | `JBSplitter` | `com.intellij.ui` |
| `JTabbedPane` | `JBTabs` | `com.intellij.ui.tabs` |
| `JCheckBox` | `JBCheckBox` | `com.intellij.ui.components` |
| `Color` | `JBColor` | `com.intellij.ui` |
| `EmptyBorder` | `JBUI.Borders.empty()` | `com.intellij.util.ui` |
| Hardcoded pixel sizes | `JBUI.scale(px)` | `com.intellij.util.ui` |

Inspection `Plugin DevKit | Code | Undesirable class usage` highlights raw Swing usage where a platform replacement exists.

## Multi-line and Rich Text

| Need | Component |
| --- | --- |
| Rich HTML with modern CSS, icons, shortcuts | `JBHtmlPane` (`com.intellij.ui.components.JBHtmlPane`) |
| Simple multi-line label with HTML | `JBLabel` + `XmlStringUtil.wrapInHtml()` |
| Scrollable / wrapping HTML panel | `SwingHelper.createHtmlViewer()` |
| High-performance colored text fragments in trees/lists/tables | `SimpleColoredComponent` |
| Plain-text newline splitting | `MultiLineLabel`, legacy, do not use in new code |

- Build HTML programmatically with `HtmlChunk` / `HtmlBuilder` (`com.intellij.openapi.util.text.HtmlChunk`). Avoid raw HTML string concatenation because it risks injection and breaks localization.
- For simple wrapping/escaping, use `XmlStringUtil.wrapInHtml(content)`, `XmlStringUtil.wrapInHtmlLines(lines...)`, and `XmlStringUtil.escapeString(text)`.
- For selectable/copyable label text, use `JBLabel.setCopyable(true)`, which switches internally to `JEditorPane` while preserving label appearance. Use `setAllowAutoWrapping(true)` for auto-wrap.
- When creating a `JEditorPane` manually, always use `HTMLEditorKitBuilder` instead of constructing `HTMLEditorKit` directly: `editorPane.setEditorKit(HTMLEditorKitBuilder.simple())` or `.withWordWrapViewFactory().build()`.
- For single-line overflow/ellipsis, use `SwingTextTrimmer`. Do not manually truncate strings.
- Put all user-visible strings in `*.properties` files. HTML markup in values is acceptable.

## Colors and Theming

- Never use `java.awt.Color` directly.
- Use `JBColor(lightColor, darkColor)` or `JBColor.namedColor("key", fallback)` for theme-aware colors.
- For lazy color retrieval, such as in painting, use `JBColor.lazy { UIManager.getColor("key") }`.
- Check current theme with `JBColor.isBright()`.
- Use generic UI colors such as `UIUtil.getContextHelpForeground()`, `UIUtil.getLabelForeground()`, and `UIUtil.getPanelBackground()`.

## Borders, Insets, and Spacing

- Always create borders via `JBUI.Borders.empty(top, left, bottom, right)` and insets via `JBUI.insets()` so they are DPI-aware and auto-update on zoom.
- Use `JBUI.scale(int)` for any pixel dimension to ensure proper HiDPI scaling.
- Prefer Kotlin UI DSL gaps and row layout semantics over manually assigning borders or insets.
- Do not use `EmptyBorder`, raw `Insets`, or raw `Dimension` unless there is no platform alternative and the reason is obvious.

## Icons

- Reuse platform icons. Browse at https://intellij-icons.jetbrains.design and access via `AllIcons.*` constants.
- Custom icons belong in `resources/icons/`.
- Load custom icons via `IconLoader.getIcon("/icons/foo.svg", MyClass::class.java)`.
- Organize custom icons in an `icons` package or a `*Icons` object with `@JvmField` on each constant.
- Icon sizing guideline values: actions/nodes = 16x16, tool window = 13x13 classic or 20x20 + 16x16 compact New UI, editor gutter = 12x12 classic or 14x14 New UI.
- Dark variants: `icon.svg` + `icon_dark.svg`.
- HiDPI variants: `icon@2x.svg` + `icon@2x_dark.svg`.
- New UI support: place New UI icons in `expui/`, create `*IconMappings.json`, register via `com.intellij.iconMapper` extension point.
- New UI icon colors: light `#6C707E`, dark `#CED0D6`.

## Notifications

- Declare in module XML: `<notificationGroup id="Kilo Code" displayType="BALLOON"/>`.
- Show with `Notification("Kilo Code", "message", NotificationType.INFORMATION).notify(project)`.
- Add actions with `.addAction(NotificationAction.createSimpleExpiring("Label") { ... })`.
- Sticky notifications use `displayType="STICKY_BALLOON"` and `.setSuggestionType(true)`.
- Tool-window-bound notifications use `displayType="TOOL_WINDOW" toolWindowId="Kilo Code"`.
- Prefer non-modal notifications over `Messages.show*()` dialogs.

## Popups

- Use `JBPopupFactory.getInstance()` for lightweight floating UI with no chrome and auto-dismiss on focus loss.
- Use `createComponentPopupBuilder(component, focusable)` for arbitrary Swing content.
- Use `createPopupChooserBuilder(list)` for item selection.
- Use `createActionGroupPopup()` for action menus.
- Show with `showInBestPositionFor(editor)`, `showUnderneathOf(component)`, or `showInCenterOf(component)`.

## Lists and Trees

- Use `JBList`, not `JList`, for empty text, busy indicator, and tooltip truncation.
- Use `Tree`, not `JTree`, for wide selection painting and auto-scroll on drag-and-drop.
- Use `ColoredListCellRenderer` / `ColoredTreeCellRenderer` for custom renderers.
- Use `append()` for styled text and `setIcon()` for icons.
- Use `ListSpeedSearch(list)` / `TreeSpeedSearch(tree)` for speed search.
- Use `ToolbarDecorator.createDecorator(list).setAddAction { }.setRemoveAction { }.createPanel()` for editable lists with add/remove/reorder toolbar.

## Before Returning Code

Review generated UI code and remove:

- Explicit default property assignments such as unnecessary `isOpaque = false`
- Unnecessary `preferredSize`, `minimumSize`, or `maximumSize`
- Raw `Dimension`, `Insets`, `EmptyBorder`, or `Color`
- Raw Swing components where IntelliJ replacements exist
- Manual layout code that Kotlin UI DSL can express cleanly
- Hardcoded spacing that should be a DSL gap, row gap, or `JBUI` value
- Extra helpers that do not make the UI clearer or more reusable

## References

- IntelliJ Platform UI Guidelines: https://jetbrains.design/intellij/
- User Interface Components: https://plugins.jetbrains.com/docs/intellij/user-interface-components.html
- UI FAQ: https://plugins.jetbrains.com/docs/intellij/ui-faq.html
- Kotlin UI DSL v2: https://plugins.jetbrains.com/docs/intellij/kotlin-ui-dsl-version-2.html
- Split mode for remote development: https://plugins.jetbrains.com/docs/intellij/split-mode-for-remote-development.html
