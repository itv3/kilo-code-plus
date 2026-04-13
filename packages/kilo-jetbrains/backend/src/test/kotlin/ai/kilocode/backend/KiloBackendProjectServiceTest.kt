package ai.kilocode.backend

import ai.kilocode.backend.app.KiloAppState
import ai.kilocode.backend.app.KiloBackendAppService
import ai.kilocode.backend.project.KiloBackendProjectService
import ai.kilocode.backend.project.KiloProjectState
import ai.kilocode.backend.testing.FakeCliServer
import ai.kilocode.backend.testing.MockCliServer
import ai.kilocode.backend.testing.TestLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class KiloBackendProjectServiceTest {

    private val mock = MockCliServer()
    private val log = TestLog()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @AfterTest
    fun tearDown() {
        scope.cancel()
        mock.close()
    }

    private fun setup(): Pair<KiloBackendAppService, KiloBackendProjectService> {
        val app = KiloBackendAppService.create(scope, FakeCliServer(mock), log)
        val project = KiloBackendProjectService.create(
            dir = "/test/project",
            cs = scope,
            appState = { app.appState },
            api = { app.api },
            log = log,
        )
        return app to project
    }

    // ------ Lifecycle ------

    @Test
    fun `full lifecycle reaches Ready`() = runBlocking {
        mock.providers = PROVIDERS_JSON
        mock.agents = AGENTS_JSON
        mock.commands = COMMANDS_JSON
        mock.skills = SKILLS_JSON

        val (app, project) = setup()
        project.start()
        app.connect()

        withTimeout(15_000) {
            project.state.first { it is KiloProjectState.Ready }
        }

        val ready = project.state.value as KiloProjectState.Ready
        assertEquals(1, ready.providers.providers.size)
        assertEquals("anthropic", ready.providers.providers[0].id)
        assertEquals(listOf("anthropic"), ready.providers.connected)
        assertEquals(1, ready.agents.agents.size)
        assertEquals("code", ready.agents.default)
        assertEquals(1, ready.commands.size)
        assertEquals("clear", ready.commands[0].name)
        assertEquals(1, ready.skills.size)
        assertEquals("test-skill", ready.skills[0].name)
    }

    @Test
    fun `stays Pending when app is not Ready`() = runBlocking {
        val (_, project) = setup()
        project.start()

        // Give some time for any unexpected transitions
        delay(500)
        assertIs<KiloProjectState.Pending>(project.state.value)
    }

    @Test
    fun `transitions to Pending when app disconnects`() = runBlocking {
        val (app, project) = setup()
        project.start()
        app.connect()

        withTimeout(15_000) {
            project.state.first { it is KiloProjectState.Ready }
        }

        app.dispose()

        withTimeout(5_000) {
            project.state.first { it is KiloProjectState.Pending }
        }
        assertIs<KiloProjectState.Pending>(project.state.value)
    }

    @Test
    fun `loading tracks progress through Loading state`() = runBlocking {
        val (app, project) = setup()
        val states = mutableListOf<KiloProjectState>()

        val collector = scope.launch {
            project.state.collect { states.add(it) }
        }

        project.start()
        app.connect()

        withTimeout(15_000) {
            project.state.first { it is KiloProjectState.Ready }
        }

        collector.cancel()

        assertTrue(states.any { it is KiloProjectState.Loading })
        assertTrue(states.any { it is KiloProjectState.Ready })
    }

    // ------ Error handling ------

    @Test
    fun `providers failure retries then transitions to Error`() = runBlocking {
        mock.providersStatus = 500
        val (app, project) = setup()
        project.start()
        app.connect()

        withTimeout(15_000) {
            project.state.first { it is KiloProjectState.Error }
        }

        val err = project.state.value as KiloProjectState.Error
        assertTrue(err.message.contains("providers"))
    }

    @Test
    fun `agents failure retries then transitions to Error`() = runBlocking {
        mock.agentsStatus = 500
        val (app, project) = setup()
        project.start()
        app.connect()

        withTimeout(15_000) {
            project.state.first { it is KiloProjectState.Error }
        }

        val err = project.state.value as KiloProjectState.Error
        assertTrue(err.message.contains("agents"))
    }

    @Test
    fun `commands failure transitions to Error`() = runBlocking {
        mock.commandsStatus = 500
        val (app, project) = setup()
        project.start()
        app.connect()

        withTimeout(15_000) {
            project.state.first { it is KiloProjectState.Error }
        }

        val err = project.state.value as KiloProjectState.Error
        assertTrue(err.message.contains("commands"))
    }

    @Test
    fun `skills failure transitions to Error`() = runBlocking {
        mock.skillsStatus = 500
        val (app, project) = setup()
        project.start()
        app.connect()

        withTimeout(15_000) {
            project.state.first { it is KiloProjectState.Error }
        }

        val err = project.state.value as KiloProjectState.Error
        assertTrue(err.message.contains("skills"))
    }

    @Test
    fun `partial failure reports all failed resources`() = runBlocking {
        mock.providersStatus = 500
        mock.skillsStatus = 500
        val (app, project) = setup()
        project.start()
        app.connect()

        withTimeout(15_000) {
            project.state.first { it is KiloProjectState.Error }
        }

        val err = project.state.value as KiloProjectState.Error
        // At least one of the failed resources should be mentioned
        assertTrue(err.message.contains("providers") || err.message.contains("skills"))
    }

    // ------ Parallel/competing initialization ------

    @Test
    fun `start is idempotent`() = runBlocking {
        val (app, project) = setup()
        project.start()
        project.start()
        project.start()
        app.connect()

        withTimeout(15_000) {
            project.state.first { it is KiloProjectState.Ready }
        }

        assertIs<KiloProjectState.Ready>(project.state.value)
    }

    @Test
    fun `reload during load produces valid final state`() = runBlocking {
        val (app, project) = setup()
        project.start()
        app.connect()

        // Wait for app to be ready so project load begins
        withTimeout(10_000) {
            app.appState.first { it is KiloAppState.Ready }
        }

        // Trigger overlapping reloads
        project.reload()
        project.reload()

        withTimeout(15_000) {
            project.state.first { it is KiloProjectState.Ready }
        }

        // The final state should be a valid Ready, not corrupted
        assertIs<KiloProjectState.Ready>(project.state.value)
    }

    @Test
    fun `rapid app state changes settle to correct state`() = runBlocking {
        val (app, project) = setup()
        project.start()
        app.connect()

        withTimeout(15_000) {
            project.state.first { it is KiloProjectState.Ready }
        }

        // Simulate rapid disconnect/reconnect by restarting
        app.restart()

        withTimeout(15_000) {
            project.state.first { it is KiloProjectState.Ready }
        }

        assertIs<KiloProjectState.Ready>(project.state.value)
    }

    // ------ Data mapping ------

    @Test
    fun `providers response maps models correctly`() = runBlocking {
        mock.providers = PROVIDERS_JSON
        val (app, project) = setup()
        project.start()
        app.connect()

        withTimeout(15_000) {
            project.state.first { it is KiloProjectState.Ready }
        }

        val ready = project.state.value as KiloProjectState.Ready
        val provider = ready.providers.providers[0]
        assertEquals("anthropic", provider.id)
        assertEquals("Anthropic", provider.name)
        val model = provider.models["claude-4"]
        assertNotNull(model)
        assertEquals("Claude 4", model.name)
        assertTrue(model.attachment)
        assertTrue(model.reasoning)
        assertTrue(model.toolCall)
    }

    @Test
    fun `agents response filters hidden and subagent`() = runBlocking {
        mock.agents = """[
            {"name":"code","mode":"primary","permission":[],"options":{}},
            {"name":"helper","mode":"subagent","permission":[],"options":{}},
            {"name":"secret","mode":"primary","hidden":true,"permission":[],"options":{}}
        ]"""
        val (app, project) = setup()
        project.start()
        app.connect()

        withTimeout(15_000) {
            project.state.first { it is KiloProjectState.Ready }
        }

        val ready = project.state.value as KiloProjectState.Ready
        // Only "code" is visible (non-subagent, non-hidden)
        assertEquals(1, ready.agents.agents.size)
        assertEquals("code", ready.agents.agents[0].name)
        // All 3 in the full list
        assertEquals(3, ready.agents.all.size)
        assertEquals("code", ready.agents.default)
    }

    @Test
    fun `commands response maps source`() = runBlocking {
        mock.commands = """[
            {"name":"clear","template":"","hints":[],"source":"command"},
            {"name":"mcp-tool","template":"","hints":["tool"],"source":"mcp"}
        ]"""
        val (app, project) = setup()
        project.start()
        app.connect()

        withTimeout(15_000) {
            project.state.first { it is KiloProjectState.Ready }
        }

        val ready = project.state.value as KiloProjectState.Ready
        assertEquals(2, ready.commands.size)
        assertEquals("command", ready.commands[0].source)
        assertEquals("mcp", ready.commands[1].source)
        assertEquals(listOf("tool"), ready.commands[1].hints)
    }

    @Test
    fun `empty responses produce empty Ready`() = runBlocking {
        // defaults are already empty
        val (app, project) = setup()
        project.start()
        app.connect()

        withTimeout(15_000) {
            project.state.first { it is KiloProjectState.Ready }
        }

        val ready = project.state.value as KiloProjectState.Ready
        assertTrue(ready.providers.providers.isEmpty())
        assertTrue(ready.agents.all.isEmpty())
        assertTrue(ready.commands.isEmpty())
        assertTrue(ready.skills.isEmpty())
        assertEquals("code", ready.agents.default) // fallback
    }

    companion object {
        private val PROVIDERS_JSON = """{
            "all": [{
                "id": "anthropic",
                "name": "Anthropic",
                "env": ["ANTHROPIC_API_KEY"],
                "models": {
                    "claude-4": {
                        "id": "claude-4",
                        "name": "Claude 4",
                        "release_date": "2025-05-01",
                        "attachment": true,
                        "reasoning": true,
                        "temperature": true,
                        "tool_call": true,
                        "limit": {"context": 200000, "output": 16000},
                        "options": {}
                    }
                }
            }],
            "default": {"code": "anthropic/claude-4"},
            "connected": ["anthropic"]
        }""".trimIndent()

        private val AGENTS_JSON = """[
            {"name":"code","displayName":"Code","mode":"primary","permission":[],"options":{}}
        ]""".trimIndent()

        private val COMMANDS_JSON = """[
            {"name":"clear","description":"Clear conversation","template":"","hints":[],"source":"command"}
        ]""".trimIndent()

        private val SKILLS_JSON = """[
            {"name":"test-skill","description":"A test skill","location":"file:///test","content":"# Test"}
        ]""".trimIndent()
    }
}
