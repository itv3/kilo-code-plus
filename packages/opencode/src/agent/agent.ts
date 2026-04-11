import { Config } from "../config/config"
import z from "zod"
import { Provider } from "../provider/provider"
import { ModelID, ProviderID } from "../provider/schema"
import { generateObject, streamObject, type ModelMessage } from "ai"
import { Instance } from "../project/instance"
import { Truncate } from "../tool/truncate"
import { Auth } from "../auth"
import { ProviderTransform } from "../provider/transform"

import PROMPT_GENERATE from "./generate.txt"
import PROMPT_COMPACTION from "./prompt/compaction.txt"
import PROMPT_DEBUG from "./prompt/debug.txt"
import PROMPT_EXPLORE from "./prompt/explore.txt"
import PROMPT_ASK from "./prompt/ask.txt"
import PROMPT_ORCHESTRATOR from "./prompt/orchestrator.txt"
import PROMPT_SUMMARY from "./prompt/summary.txt"
import PROMPT_TITLE from "./prompt/title.txt"
<<<<<<< HEAD

import { Permission } from "@/permission"
import { NamedError } from "@opencode-ai/util/error" // kilocode_change
import { Glob } from "../util/glob" // kilocode_change
=======
import { Permission } from "@/permission"
>>>>>>> catrielmuller/opencode-v1.3.1
import { mergeDeep, pipe, sortBy, values } from "remeda"
import { Global } from "@/global"
import path from "path"
import { Plugin } from "@/plugin"
import { Skill } from "../skill"
import { Effect, ServiceMap, Layer } from "effect"
import { InstanceState } from "@/effect/instance-state"
import { makeRunPromise } from "@/effect/run-service"

import { Telemetry } from "@kilocode/kilo-telemetry" // kilocode_change

export namespace Agent {
  export const Info = z
    .object({
      name: z.string(),
      displayName: z.string().optional(), // kilocode_change - human-readable name for org modes
      description: z.string().optional(),
      mode: z.enum(["subagent", "primary", "all"]),
      native: z.boolean().optional(),
      hidden: z.boolean().optional(),
      deprecated: z.boolean().optional(),
      topP: z.number().optional(),
      temperature: z.number().optional(),
      color: z.string().optional(),
      permission: Permission.Ruleset,
      model: z
        .object({
          modelID: ModelID.zod,
          providerID: ProviderID.zod,
        })
        .optional(),
      variant: z.string().optional(),
      prompt: z.string().optional(),
      options: z.record(z.string(), z.any()),
      steps: z.number().int().positive().optional(),
    })
    .meta({
      ref: "Agent",
    })
  export type Info = z.infer<typeof Info>

  export interface Interface {
    readonly get: (agent: string) => Effect.Effect<Agent.Info>
    readonly list: () => Effect.Effect<Agent.Info[]>
    readonly defaultAgent: () => Effect.Effect<string>
    readonly generate: (input: {
      description: string
      model?: { providerID: ProviderID; modelID: ModelID }
    }) => Effect.Effect<{
      identifier: string
      whenToUse: string
      systemPrompt: string
    }>
  }

<<<<<<< HEAD
    const skillDirs = await Skill.dirs()
    const whitelistedDirs = [Truncate.GLOB, ...skillDirs.map((dir) => path.join(dir, "*"))]
    // kilocode_change start — safe bash commands that don't need user approval.
    // only commands that cannot execute arbitrary code or subprocesses.
    const bash: Record<string, "allow" | "ask" | "deny"> = {
      "*": "ask",
      // read-only / informational
      "cat *": "allow",
      "head *": "allow",
      "tail *": "allow",
      "less *": "allow",
      "ls *": "allow",
      "tree *": "allow",
      "pwd *": "allow",
      "echo *": "allow",
      "wc *": "allow",
      "which *": "allow",
      "type *": "allow",
      "file *": "allow",
      "diff *": "allow",
      "du *": "allow",
      "df *": "allow",
      "date *": "allow",
      "uname *": "allow",
      "whoami *": "allow",
      "printenv *": "allow",
      "man *": "allow",
      // text processing
      "grep *": "allow",
      "rg *": "allow",
      "ag *": "allow",
      "sort *": "allow",
      "uniq *": "allow",
      "cut *": "allow",
      "tr *": "allow",
      "jq *": "allow",
      // file operations
      "touch *": "allow",
      "mkdir *": "allow",
      "cp *": "allow",
      "mv *": "allow",
      // compilers (no script execution)
      "tsc *": "allow",
      "tsgo *": "allow",
      // archive
      "tar *": "allow",
      "unzip *": "allow",
      "gzip *": "allow",
      "gunzip *": "allow",
    }
    // kilocode_change end

    // kilocode_change start — read-only bash commands for the ask agent.
    // Unlike the default bash allowlist, unknown commands are DENIED (not "ask")
    // because the ask agent must never modify the filesystem.
    const readOnlyBash: Record<string, "allow" | "ask" | "deny"> = {
      "*": "deny",
      // read-only / informational
      "cat *": "allow",
      "head *": "allow",
      "tail *": "allow",
      "less *": "allow",
      "ls *": "allow",
      "tree *": "allow",
      "pwd *": "allow",
      "echo *": "allow",
      "wc *": "allow",
      "which *": "allow",
      "type *": "allow",
      "file *": "allow",
      "diff *": "allow",
      "du *": "allow",
      "df *": "allow",
      "date *": "allow",
      "uname *": "allow",
      "whoami *": "allow",
      "printenv *": "allow",
      "man *": "allow",
      // text processing (stdout only, no file modification)
      "grep *": "allow",
      "rg *": "allow",
      "ag *": "allow",
      "sort *": "allow",
      "uniq *": "allow",
      "cut *": "allow",
      "tr *": "allow",
      "jq *": "allow",
      // git — allowlist of read-only subcommands, deny everything else
      "git *": "deny",
      "git log *": "allow",
      "git show *": "allow",
      "git diff *": "allow",
      "git status *": "allow",
      "git blame *": "allow",
      "git rev-parse *": "allow",
      "git rev-list *": "allow",
      "git ls-files *": "allow",
      "git ls-tree *": "allow",
      "git ls-remote *": "allow",
      "git shortlog *": "allow",
      "git describe *": "allow",
      "git cat-file *": "allow",
      "git name-rev *": "allow",
      "git stash list *": "allow",
      "git tag -l *": "allow",
      "git branch --list *": "allow",
      "git branch -a *": "allow",
      "git branch -r *": "allow",
      "git remote -v *": "allow",
      // gh — require user approval since commands vary widely
      "gh *": "ask",
    }

    // kilocode_change start — allow MCP tools in ask agent with user approval.
    // Generates per-server wildcard rules that override "*": "deny".
    const mcpRules: Record<string, "allow" | "ask" | "deny"> = {}
    for (const key of Object.keys(cfg.mcp ?? {})) {
      const sanitized = key.replace(/[^a-zA-Z0-9_-]/g, "_")
      mcpRules[sanitized + "_*"] = "ask"
    }
    // kilocode_change end

    const defaults = Permission.fromConfig({
      "*": "allow",
      bash, // kilocode_change
      doom_loop: "ask",
      recall: "ask", // kilocode_change
      external_directory: {
        "*": "ask",
        ...Object.fromEntries(whitelistedDirs.map((dir) => [dir, "allow"])),
      },
      question: "deny",
      plan_enter: "deny",
      plan_exit: "deny",
      // mirrors github.com/github/gitignore Node.gitignore pattern for .env files
      read: {
        "*": "allow",
        "*.env": "ask",
        "*.env.*": "ask",
        "*.env.example": "allow",
      },
    })
    const user = Permission.fromConfig(cfg.permission ?? {})

    const result: Record<string, Info> = {
      // kilocode_change start
      code: {
        name: "code",
        description: "The default agent. Executes tools based on configured permissions.",
        // kilocode_change end
        options: {},
        permission: Permission.merge(
          defaults,
          Permission.fromConfig({
            question: "allow",
            plan_enter: "allow",
          }),
          user,
        ),
        mode: "primary",
        native: true,
      },
      plan: {
        name: "plan",
        description: "Plan mode. Only allows editing plan files; asks before editing anything else.",
        options: {},
        permission: Permission.merge(
          defaults,
          Permission.fromConfig({
            question: "allow",
            plan_exit: "allow",
            bash: readOnlyBash, // kilocode_change: read-only bash for plan mode (mirrors ask agent)
            ...mcpRules, // kilocode_change: MCP with user approval for plan mode
            external_directory: {
              [path.join(Global.Path.data, "plans", "*")]: "allow",
            },
            edit: {
              "*": "ask", // kilocode_change: ask (not deny) so user can approve edits outside plan files
              [path.join(".kilo", "plans", "*.md")]: "allow", // kilocode_change
              [path.join(".opencode", "plans", "*.md")]: "allow", // kilocode_change: .opencode fallback
              [path.relative(Instance.worktree, path.join(Global.Path.data, path.join("plans", "*.md")))]: "allow",
            },
          }),
          user,
        ),
        mode: "primary",
        native: true,
      },
      // kilocode_change start - add debug, orchestrator, and ask agents
      debug: {
        name: "debug",
        description: "Diagnose and fix software issues with systematic debugging methodology.",
        prompt: PROMPT_DEBUG,
        options: {},
        permission: Permission.merge(
          defaults,
          Permission.fromConfig({
            question: "allow",
            plan_enter: "allow",
          }),
          user,
        ),
        mode: "primary",
        native: true,
      },
      orchestrator: {
        name: "orchestrator",
        description: "Coordinate complex tasks by delegating to specialized agents in parallel.",
        prompt: PROMPT_ORCHESTRATOR,
        options: {},
        permission: Permission.merge(
          defaults,
          Permission.fromConfig({
            "*": "deny",
            read: "allow",
            grep: "allow",
            glob: "allow",
            list: "allow",
            // bash: "allow", // kilocode_change - disabled to prevent orchestrator from writing files via shell commands instead of delegating to sub-agents
            question: "allow",
            task: "allow",
            todoread: "allow",
            todowrite: "allow",
            webfetch: "allow",
            websearch: "allow",
            codesearch: "allow",
            codebase_search: "allow", // kilocode_change
            external_directory: {
              [Truncate.GLOB]: "allow",
            },
          }),
          user,
          // kilocode_change start - enforce bash deny after user so user config cannot re-enable shell
          Permission.fromConfig({
            bash: "deny",
          }),
          // kilocode_change end
        ),
        mode: "primary",
        native: true,
        deprecated: true,
      },
      ask: {
        name: "ask",
        description: "Get answers and explanations without making changes to the codebase.",
        prompt: PROMPT_ASK,
        options: {},
        permission: Permission.merge(
          defaults,
          user, // kilocode_change: user before ask-specific so ask's deny+allowlist wins
          Permission.fromConfig({
            "*": "deny",
            bash: readOnlyBash,
            read: {
              "*": "allow",
              "*.env": "ask",
              "*.env.*": "ask",
              "*.env.example": "allow",
            },
            grep: "allow",
            glob: "allow",
            list: "allow",
            question: "allow",
            webfetch: "allow",
            websearch: "allow",
            codesearch: "allow",
            codebase_search: "allow", // kilocode_change
            external_directory: {
              [Truncate.GLOB]: "allow",
            },
            ...mcpRules,
          }),
          user.filter((r) => r.action === "deny"), // kilocode_change: re-apply user denies so explicit MCP blocks win over mcpRules
        ),
        mode: "primary",
        native: true,
      },
      // kilocode_change end
      general: {
        name: "general",
        description: `General-purpose agent for researching complex questions and executing multi-step tasks. Use this agent to execute multiple units of work in parallel.`,
        permission: Permission.merge(
          defaults,
          Permission.fromConfig({
            todoread: "deny",
            todowrite: "deny",
          }),
          user,
        ),
        options: {},
        mode: "subagent",
        native: true,
      },
      explore: {
        name: "explore",
        permission: Permission.merge(
          defaults,
          Permission.fromConfig({
            "*": "deny",
            grep: "allow",
            glob: "allow",
            list: "allow",
            bash: "allow",
            webfetch: "allow",
            websearch: "allow",
            codesearch: "allow",
            codebase_search: "allow", // kilocode_change
            read: "allow",
=======
  type State = Omit<Interface, "generate">

  export class Service extends ServiceMap.Service<Service, Interface>()("@opencode/Agent") {}

  export const layer = Layer.effect(
    Service,
    Effect.gen(function* () {
      const config = () => Effect.promise(() => Config.get())
      const auth = yield* Auth.Service

      const state = yield* InstanceState.make<State>(
        Effect.fn("Agent.state")(function* (ctx) {
          const cfg = yield* config()
          const skillDirs = yield* Effect.promise(() => Skill.dirs())
          const whitelistedDirs = [Truncate.GLOB, ...skillDirs.map((dir) => path.join(dir, "*"))]

          const defaults = Permission.fromConfig({
            "*": "allow",
            doom_loop: "ask",
>>>>>>> catrielmuller/opencode-v1.3.1
            external_directory: {
              "*": "ask",
              ...Object.fromEntries(whitelistedDirs.map((dir) => [dir, "allow"])),
            },
<<<<<<< HEAD
          }),
          user,
        ),
        description: `Fast agent specialized for exploring codebases. Use this when you need to quickly find files by patterns (eg. "src/components/**/*.tsx"), search code for keywords (eg. "API endpoints"), or answer questions about the codebase (eg. "how do API endpoints work?"). When calling this agent, specify the desired thoroughness level: "quick" for basic searches, "medium" for moderate exploration, or "very thorough" for comprehensive analysis across multiple locations and naming conventions.`,
        // kilocode_change - only advertise codebase_search when the experimental flag is on
        prompt: cfg.experimental?.codebase_search
          ? `Prefer using the codebase_search tool for codebase searches — it performs intelligent multi-step code search and returns the most relevant code spans.\n\n${PROMPT_EXPLORE}`
          : PROMPT_EXPLORE,
        options: {},
        mode: "subagent",
        native: true,
      },
      compaction: {
        name: "compaction",
        mode: "primary",
        native: true,
        hidden: true,
        prompt: PROMPT_COMPACTION,
        permission: Permission.merge(
          defaults,
          Permission.fromConfig({
            "*": "deny",
          }),
          user,
        ),
        options: {},
      },
      title: {
        name: "title",
        mode: "primary",
        options: {},
        native: true,
        hidden: true,
        temperature: 0.5,
        permission: Permission.merge(
          defaults,
          Permission.fromConfig({
            "*": "deny",
          }),
          user,
        ),
        prompt: PROMPT_TITLE,
      },
      summary: {
        name: "summary",
        mode: "primary",
        options: {},
        native: true,
        hidden: true,
        permission: Permission.merge(
          defaults,
          Permission.fromConfig({
            "*": "deny",
          }),
          user,
        ),
        prompt: PROMPT_SUMMARY,
      },
    }

    for (const [key, value] of Object.entries(cfg.agent ?? {})) {
      // kilocode_change start
      // Treat "build" config as "code" for backward compatibility
      const effectiveKey = key === "build" ? "code" : key
      if (value.disable) {
        delete result[effectiveKey]
        continue
      }
      let item = result[effectiveKey]
      if (!item)
        item = result[effectiveKey] = {
          name: effectiveKey,
          mode: "all",
          permission: Permission.merge(defaults, user),
          options: {},
          native: false,
        }
      // kilocode_change end
      if (value.model) item.model = Provider.parseModel(value.model)
      item.variant = value.variant ?? item.variant
      item.prompt = value.prompt ?? item.prompt
      item.description = value.description ?? item.description
      item.temperature = value.temperature ?? item.temperature
      item.topP = value.top_p ?? item.topP
      item.mode = value.mode ?? item.mode
      item.color = value.color ?? item.color
      item.hidden = value.hidden ?? item.hidden
      item.deprecated = value.deprecated ?? item.deprecated
      item.name = value.name ?? item.name
      item.steps = value.steps ?? item.steps
      item.options = mergeDeep(item.options, value.options ?? {})
      // kilocode_change  start - populate displayName from org mode options
      if (item.options?.displayName && typeof item.options.displayName === "string") {
        item.displayName = item.options.displayName
      }
      // kilocode_change end
      item.permission = Permission.merge(item.permission, Permission.fromConfig(value.permission ?? {}))
    }
=======
            question: "deny",
            plan_enter: "deny",
            plan_exit: "deny",
            // mirrors github.com/github/gitignore Node.gitignore pattern for .env files
            read: {
              "*": "allow",
              "*.env": "ask",
              "*.env.*": "ask",
              "*.env.example": "allow",
            },
          })

          const user = Permission.fromConfig(cfg.permission ?? {})
>>>>>>> catrielmuller/opencode-v1.3.1

          const agents: Record<string, Info> = {
            build: {
              name: "build",
              description: "The default agent. Executes tools based on configured permissions.",
              options: {},
              permission: Permission.merge(
                defaults,
                Permission.fromConfig({
                  question: "allow",
                  plan_enter: "allow",
                }),
                user,
              ),
              mode: "primary",
              native: true,
            },
            plan: {
              name: "plan",
              description: "Plan mode. Disallows all edit tools.",
              options: {},
              permission: Permission.merge(
                defaults,
                Permission.fromConfig({
                  question: "allow",
                  plan_exit: "allow",
                  external_directory: {
                    [path.join(Global.Path.data, "plans", "*")]: "allow",
                  },
                  edit: {
                    "*": "deny",
                    [path.join(".opencode", "plans", "*.md")]: "allow",
                    [path.relative(Instance.worktree, path.join(Global.Path.data, path.join("plans", "*.md")))]:
                      "allow",
                  },
                }),
                user,
              ),
              mode: "primary",
              native: true,
            },
            general: {
              name: "general",
              description: `General-purpose agent for researching complex questions and executing multi-step tasks. Use this agent to execute multiple units of work in parallel.`,
              permission: Permission.merge(
                defaults,
                Permission.fromConfig({
                  todoread: "deny",
                  todowrite: "deny",
                }),
                user,
              ),
              options: {},
              mode: "subagent",
              native: true,
            },
            explore: {
              name: "explore",
              permission: Permission.merge(
                defaults,
                Permission.fromConfig({
                  "*": "deny",
                  grep: "allow",
                  glob: "allow",
                  list: "allow",
                  bash: "allow",
                  webfetch: "allow",
                  websearch: "allow",
                  codesearch: "allow",
                  read: "allow",
                  external_directory: {
                    "*": "ask",
                    ...Object.fromEntries(whitelistedDirs.map((dir) => [dir, "allow"])),
                  },
                }),
                user,
              ),
              description: `Fast agent specialized for exploring codebases. Use this when you need to quickly find files by patterns (eg. "src/components/**/*.tsx"), search code for keywords (eg. "API endpoints"), or answer questions about the codebase (eg. "how do API endpoints work?"). When calling this agent, specify the desired thoroughness level: "quick" for basic searches, "medium" for moderate exploration, or "very thorough" for comprehensive analysis across multiple locations and naming conventions.`,
              prompt: PROMPT_EXPLORE,
              options: {},
              mode: "subagent",
              native: true,
            },
            compaction: {
              name: "compaction",
              mode: "primary",
              native: true,
              hidden: true,
              prompt: PROMPT_COMPACTION,
              permission: Permission.merge(
                defaults,
                Permission.fromConfig({
                  "*": "deny",
                }),
                user,
              ),
              options: {},
            },
            title: {
              name: "title",
              mode: "primary",
              options: {},
              native: true,
              hidden: true,
              temperature: 0.5,
              permission: Permission.merge(
                defaults,
                Permission.fromConfig({
                  "*": "deny",
                }),
                user,
              ),
              prompt: PROMPT_TITLE,
            },
            summary: {
              name: "summary",
              mode: "primary",
              options: {},
              native: true,
              hidden: true,
              permission: Permission.merge(
                defaults,
                Permission.fromConfig({
                  "*": "deny",
                }),
                user,
              ),
              prompt: PROMPT_SUMMARY,
            },
          }

<<<<<<< HEAD
      result[name].permission = Permission.merge(
        result[name].permission,
        Permission.fromConfig({ external_directory: { [Truncate.GLOB]: "allow" } }),
=======
          for (const [key, value] of Object.entries(cfg.agent ?? {})) {
            if (value.disable) {
              delete agents[key]
              continue
            }
            let item = agents[key]
            if (!item)
              item = agents[key] = {
                name: key,
                mode: "all",
                permission: Permission.merge(defaults, user),
                options: {},
                native: false,
              }
            if (value.model) item.model = Provider.parseModel(value.model)
            item.variant = value.variant ?? item.variant
            item.prompt = value.prompt ?? item.prompt
            item.description = value.description ?? item.description
            item.temperature = value.temperature ?? item.temperature
            item.topP = value.top_p ?? item.topP
            item.mode = value.mode ?? item.mode
            item.color = value.color ?? item.color
            item.hidden = value.hidden ?? item.hidden
            item.name = value.name ?? item.name
            item.steps = value.steps ?? item.steps
            item.options = mergeDeep(item.options, value.options ?? {})
            item.permission = Permission.merge(item.permission, Permission.fromConfig(value.permission ?? {}))
          }

          // Ensure Truncate.GLOB is allowed unless explicitly configured
          for (const name in agents) {
            const agent = agents[name]
            const explicit = agent.permission.some((r) => {
              if (r.permission !== "external_directory") return false
              if (r.action !== "deny") return false
              return r.pattern === Truncate.GLOB
            })
            if (explicit) continue

            agents[name].permission = Permission.merge(
              agents[name].permission,
              Permission.fromConfig({ external_directory: { [Truncate.GLOB]: "allow" } }),
            )
          }

          const get = Effect.fnUntraced(function* (agent: string) {
            return agents[agent]
          })

          const list = Effect.fnUntraced(function* () {
            const cfg = yield* config()
            return pipe(
              agents,
              values(),
              sortBy(
                [(x) => (cfg.default_agent ? x.name === cfg.default_agent : x.name === "build"), "desc"],
                [(x) => x.name, "asc"],
              ),
            )
          })

          const defaultAgent = Effect.fnUntraced(function* () {
            const c = yield* config()
            if (c.default_agent) {
              const agent = agents[c.default_agent]
              if (!agent) throw new Error(`default agent "${c.default_agent}" not found`)
              if (agent.mode === "subagent") throw new Error(`default agent "${c.default_agent}" is a subagent`)
              if (agent.hidden === true) throw new Error(`default agent "${c.default_agent}" is hidden`)
              return agent.name
            }
            const visible = Object.values(agents).find((a) => a.mode !== "subagent" && a.hidden !== true)
            if (!visible) throw new Error("no primary visible agent found")
            return visible.name
          })

          return {
            get,
            list,
            defaultAgent,
          } satisfies State
        }),
>>>>>>> catrielmuller/opencode-v1.3.1
      )

      return Service.of({
        get: Effect.fn("Agent.get")(function* (agent: string) {
          return yield* InstanceState.useEffect(state, (s) => s.get(agent))
        }),
        list: Effect.fn("Agent.list")(function* () {
          return yield* InstanceState.useEffect(state, (s) => s.list())
        }),
        defaultAgent: Effect.fn("Agent.defaultAgent")(function* () {
          return yield* InstanceState.useEffect(state, (s) => s.defaultAgent())
        }),
        generate: Effect.fn("Agent.generate")(function* (input: {
          description: string
          model?: { providerID: ProviderID; modelID: ModelID }
        }) {
          const cfg = yield* config()
          const model = input.model ?? (yield* Effect.promise(() => Provider.defaultModel()))
          const resolved = yield* Effect.promise(() => Provider.getModel(model.providerID, model.modelID))
          const language = yield* Effect.promise(() => Provider.getLanguage(resolved))

          const system = [PROMPT_GENERATE]
          yield* Effect.promise(() =>
            Plugin.trigger("experimental.chat.system.transform", { model: resolved }, { system }),
          )
          const existing = yield* InstanceState.useEffect(state, (s) => s.list())

          const params = {
            experimental_telemetry: {
              isEnabled: cfg.experimental?.openTelemetry,
              metadata: {
                userId: cfg.username ?? "unknown",
              },
            },
            temperature: 0.3,
            messages: [
              ...system.map(
                (item): ModelMessage => ({
                  role: "system",
                  content: item,
                }),
              ),
              {
                role: "user",
                content: `Create an agent configuration based on this request: \"${input.description}\".\n\nIMPORTANT: The following identifiers already exist and must NOT be used: ${existing.map((i) => i.name).join(", ")}\n  Return ONLY the JSON object, no other text, do not wrap in backticks`,
              },
            ],
            model: language,
            schema: z.object({
              identifier: z.string(),
              whenToUse: z.string(),
              systemPrompt: z.string(),
            }),
          } satisfies Parameters<typeof generateObject>[0]

          // TODO: clean this up so provider specific logic doesnt bleed over
          const authInfo = yield* auth.get(model.providerID).pipe(Effect.orDie)
          if (model.providerID === "openai" && authInfo?.type === "oauth") {
            return yield* Effect.promise(async () => {
              const result = streamObject({
                ...params,
                providerOptions: ProviderTransform.providerOptions(resolved, {
                  store: false,
                }),
                onError: () => {},
              })
              for await (const part of result.fullStream) {
                if (part.type === "error") throw part.error
              }
              return result.object
            })
          }

          return yield* Effect.promise(() => generateObject(params).then((r) => r.object))
        }),
      })
    }),
  )

  export const defaultLayer = layer.pipe(Layer.provide(Auth.layer))

  const runPromise = makeRunPromise(Service, defaultLayer)

  export async function get(agent: string) {
<<<<<<< HEAD
    // kilocode_change start -  Treat "build" as "code" for backward compatibility
    const effectiveAgent = agent === "build" ? "code" : agent
    return state().then((x) => x[effectiveAgent])
    // kilocode_change end
  }

  export async function list() {
    const cfg = await Config.get()
    return pipe(
      await state(),
      values(),
      sortBy(
        [(x) => (cfg.default_agent ? x.name === cfg.default_agent : x.name === "code"), "desc"], // kilocode_change - renamed from "build" to "code"
        [(x) => x.name, "asc"],
      ),
    )
  }

  export async function defaultAgent() {
    const cfg = await Config.get()
    const agents = await state()

    if (cfg.default_agent) {
      // kilocode_change start -  Treat "build" as "code" for backward compatibility
      const effectiveDefault = cfg.default_agent === "build" ? "code" : cfg.default_agent
      const agent = agents[effectiveDefault]
      if (!agent) throw new Error(`default agent "${cfg.default_agent}" not found`)
      // kilocode_change end
      if (agent.mode === "subagent") throw new Error(`default agent "${cfg.default_agent}" is a subagent`)
      if (agent.hidden === true) throw new Error(`default agent "${cfg.default_agent}" is hidden`)
      return agent.name
    }

    const primaryVisible = Object.values(agents).find((a) => a.mode !== "subagent" && a.hidden !== true)
    if (!primaryVisible) throw new Error("no primary visible agent found")
    return primaryVisible.name
  }

  export async function generate(input: { description: string; model?: { providerID: ProviderID; modelID: ModelID } }) {
    const cfg = await Config.get()
    const defaultModel = input.model ?? (await Provider.defaultModel())
    const model = await Provider.getModel(defaultModel.providerID, defaultModel.modelID)
    const language = await Provider.getLanguage(model)

    const system = [PROMPT_GENERATE]
    await Plugin.trigger("experimental.chat.system.transform", { model }, { system })
    const existing = await list()

    const params = {
      // kilocode_change start - enable telemetry by default with custom PostHog tracer
      experimental_telemetry: {
        isEnabled: cfg.experimental?.openTelemetry !== false,
        recordInputs: false, // Prevent recording prompts, messages, tool args
        recordOutputs: false, // Prevent recording completions, tool results
        tracer: Telemetry.getTracer() ?? undefined,
        metadata: {
          userId: cfg.username ?? "unknown",
        },
      },
      // kilocode_change end
      temperature: 0.3,
      messages: [
        ...system.map(
          (item): ModelMessage => ({
            role: "system",
            content: item,
          }),
        ),
        {
          role: "user",
          content: `Create an agent configuration based on this request: \"${input.description}\".\n\nIMPORTANT: The following identifiers already exist and must NOT be used: ${existing.map((i) => i.name).join(", ")}\n  Return ONLY the JSON object, no other text, do not wrap in backticks`,
        },
      ],
      model: language,
      schema: z.object({
        identifier: z.string(),
        whenToUse: z.string(),
        systemPrompt: z.string(),
      }),
    } satisfies Parameters<typeof generateObject>[0]

    // TODO: clean this up so provider specific logic doesnt bleed over
    if (defaultModel.providerID === "openai" && (await Auth.get(defaultModel.providerID))?.type === "oauth") {
      const result = streamObject({
        ...params,
        providerOptions: ProviderTransform.providerOptions(model, {
          store: false,
        }),
        onError: () => {},
      })
      for await (const part of result.fullStream) {
        if (part.type === "error") throw part.error
      }
      return result.object
    }

    const result = await generateObject(params)
    return result.object
=======
    return runPromise((svc) => svc.get(agent))
  }

  export async function list() {
    return runPromise((svc) => svc.list())
  }

  export async function defaultAgent() {
    return runPromise((svc) => svc.defaultAgent())
  }

  export async function generate(input: { description: string; model?: { providerID: ProviderID; modelID: ModelID } }) {
    return runPromise((svc) => svc.generate(input))
>>>>>>> catrielmuller/opencode-v1.3.1
  }

  // kilocode_change start
  export const RemoveError = NamedError.create(
    "AgentRemoveError",
    z.object({
      name: z.string(),
      message: z.string(),
    }),
  )

  /**
   * Remove a custom agent by deleting its markdown source file and/or
   * removing it from legacy .kilocodemodes YAML files.
   * Scans all config directories for agent/mode .md files matching the name,
   * then also checks the .kilocodemodes files the ModesMigrator reads.
   */
  export async function remove(name: string) {
    const agents = await state()
    const agent = agents[name]
    if (!agent) throw new RemoveError({ name, message: "agent not found" })
    if (agent.native) throw new RemoveError({ name, message: "cannot remove native agent" })
    // kilocode_change start - prevent removal of organization-managed agents
    if (agent.options?.source === "organization")
      throw new RemoveError({ name, message: "cannot remove organization agent — manage it from the cloud dashboard" })
    // kilocode_change end

    const { unlink, readFile, writeFile } = await import("fs/promises")
    let found = false

    // 1. Delete .md files from config directories
    const dirs = await Config.directories()
    const patterns = ["{agent,agents}/**/" + name + ".md", "{mode,modes}/" + name + ".md"]
    for (const dir of dirs) {
      for (const pattern of patterns) {
        const matches = await Glob.scan(pattern, { cwd: dir, absolute: true, dot: true })
        for (const file of matches) {
          if (await Bun.file(file).exists()) {
            await unlink(file)
            found = true
          }
        }
      }
    }

    // 2. Remove from legacy .kilocodemodes YAML files (read by ModesMigrator)
    const { ModesMigrator } = await import("@/kilocode/modes-migrator")
    const { KilocodePaths } = await import("@/kilocode/paths")
    const os = await import("os")
    const matter = (await import("gray-matter")).default
    const home = os.default.homedir()
    const modesFiles = [
      path.join(KilocodePaths.vscodeGlobalStorage(), "settings", "custom_modes.yaml"),
      path.join(home, ".kilocode", "cli", "global", "settings", "custom_modes.yaml"),
      path.join(home, ".kilocodemodes"),
      path.join(Instance.directory, ".kilocodemodes"),
    ]

    for (const file of modesFiles) {
      const modes = await ModesMigrator.readModesFile(file)
      if (!modes.length) continue

      const filtered = modes.filter((m) => m.slug !== name)
      if (filtered.length === modes.length) continue

      // Rewrite the file without the removed mode
      const yaml = matter
        .stringify("", { customModes: filtered })
        .replace(/^---\n/, "")
        .replace(/\n---\n?$/, "")
      await writeFile(file, yaml)
      found = true
    }

    if (!found) throw new RemoveError({ name, message: "no agent file found on disk" })

    await Instance.dispose()
  }
  // kilocode_change end
}
