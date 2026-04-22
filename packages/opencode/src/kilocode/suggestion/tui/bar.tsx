/** @jsxImportSource @opentui/solid */
// kilocode_change - new file

import { useKeyboard } from "@opentui/solid"
import type { SuggestionRequest } from "@kilocode/sdk/v2"
import { createMemo, createSignal, For, Show } from "solid-js"
import { useKeybind } from "../../../cli/cmd/tui/context/keybind"
import { useSDK } from "../../../cli/cmd/tui/context/sdk"
import { tint, useTheme } from "../../../cli/cmd/tui/context/theme"
import { useDialog } from "../../../cli/cmd/tui/ui/dialog"

export function SuggestBar(props: { request: SuggestionRequest; inputFocused?: () => boolean }) {
  const sdk = useSDK()
  const { theme } = useTheme()
  const keybind = useKeybind()
  const dialog = useDialog()

  const options = createMemo(() => props.request.actions)
  const [selected, setSelected] = createSignal(0)
  const [busy, setBusy] = createSignal(false)

  function accept(index: number) {
    if (busy()) return
    setBusy(true)
    sdk.client.suggestion
      .accept({
        requestID: props.request.id,
        index,
      })
      .catch(() => setBusy(false))
  }

  function reject() {
    if (busy()) return
    setBusy(true)
    sdk.client.suggestion
      .dismiss({
        requestID: props.request.id,
      })
      .catch(() => setBusy(false))
  }

  useKeyboard((evt) => {
    if (dialog.stack.length > 0) return
    if (evt.defaultPrevented) return

    if (evt.name === "escape" || keybind.match("app_exit", evt)) {
      evt.preventDefault()
      reject()
      return
    }

    // Skip digit shortcuts when the prompt has focus so typed digits go into
    // the prompt rather than triggering the bar. Matches the non-blocking
    // suppression pattern from the old footer overlay.
    if (props.inputFocused?.()) return

    const max = Math.min(options().length, 2)
    const digit = Number(evt.name)
    if (!Number.isNaN(digit) && digit >= 1 && digit <= max) {
      evt.preventDefault()
      accept(digit - 1)
    }
  })

  return (
    <box
      marginTop={1}
      paddingLeft={2}
      paddingTop={0}
      paddingBottom={0}
      gap={1}
      border={["left"]}
      borderColor={theme.secondary}
    >
      <box paddingLeft={1}>
        <text fg={theme.text}>{props.request.text}</text>
      </box>
      <box flexDirection="row" gap={2} paddingLeft={1}>
        <For each={options()}>
          {(opt, i) => {
            const active = () => i() === selected()
            return (
              <box
                flexDirection="row"
                onMouseOver={() => setSelected(i())}
                onMouseDown={() => setSelected(i())}
                onMouseUp={() => accept(i())}
              >
                <box backgroundColor={active() ? theme.backgroundElement : undefined} paddingRight={1}>
                  <text fg={active() ? tint(theme.textMuted, theme.secondary, 0.6) : theme.textMuted}>
                    {`${i() + 1}.`}
                  </text>
                </box>
                <box backgroundColor={active() ? theme.backgroundElement : undefined} paddingRight={1}>
                  <text fg={active() ? theme.secondary : theme.text}>{opt.label}</text>
                </box>
              </box>
            )
          }}
        </For>
        <text fg={theme.textMuted}>
          <Show when={busy()} fallback={<>esc dismiss</>}>
            Waiting...
          </Show>
        </text>
      </box>
    </box>
  )
}
