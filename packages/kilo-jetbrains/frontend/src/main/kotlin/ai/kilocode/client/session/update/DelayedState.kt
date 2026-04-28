package ai.kilocode.client.session.update

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import javax.swing.Timer

internal class DelayedState<T : Any>(
    private val ms: Long,
    private val current: () -> T,
) : Disposable {
    private var timer: Timer? = null
    private var state: T? = null
    @Volatile private var alive = true

    fun run(state: T, action: (T) -> Unit) {
        edt {
            if (!alive) return@edt
            timer?.stop()
            this.state = state
            if (ms <= 0) {
                apply(state, action)
                return@edt
            }
            timer = Timer(ms.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()) {
                apply(state, action)
            }.apply {
                isRepeats = false
                start()
            }
        }
    }

    fun cancel() {
        edt {
            timer?.stop()
            timer = null
            state = null
        }
    }

    private fun apply(state: T, action: (T) -> Unit) {
        if (!alive) return
        if (this.state != state) return
        if (current() != state) return
        this.state = null
        timer = null
        action(state)
    }

    private fun edt(block: () -> Unit) {
        val app = ApplicationManager.getApplication()
        if (app.isDispatchThread) {
            block()
            return
        }
        app.invokeLater(block)
    }

    override fun dispose() {
        alive = false
        cancel()
    }
}
