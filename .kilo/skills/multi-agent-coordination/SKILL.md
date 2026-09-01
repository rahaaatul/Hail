---
name: multi-agent-coordination
description: Coordinate multiple AI agents across six proven patterns (fan-out/fan-in, pipeline, hierarchical delegation, blackboard, handoff chains, consensus) with framework-specific implementations for CrewAI, LangGraph, AutoGen/AG2, OpenAI Agents SDK, Google ADK, and Claude Code sub-agents. Use when the user asks about multi-agent orchestration, agent team design, or how to structure coordination between multiple AI agents.
---

# Multi-Agent AI Coordination

This skill documents how to coordinate multiple AI agents working together on complex tasks. It covers six architectural coordination patterns, when to apply each, practical code implementations across major frameworks, and production considerations for error handling, cost control, and observability.

## The Coordination Problem

Building a single agent is straightforward: give it a system prompt, connect tools, and let it run. But when two or more agents need to share state, hand off tasks, or merge outputs, coordination becomes the bottleneck. The six patterns below are architectural primitives that apply regardless of framework choice.

## Six Coordination Patterns

### 1. Fan-Out / Fan-In (Parallel Scatter-Gather)

Run N agents simultaneously on independent subtasks, then merge their outputs with a dedicated aggregator.

**When to use it:**

- Research across multiple sources
- Auditing different parts of a codebase
- Generating alternative implementations
- Any task where subtasks have zero dependencies on each other

**The trap:** The merge step is underestimated. Three agents producing three summaries is easy; reconciling contradictions, deduplicating, and producing a coherent final output requires a dedicated aggregator (another agent or a deterministic merge function).

```typescript
import Anthropic from "@anthropic-ai/sdk";

const client = new Anthropic();

interface AgentTask {
  name: string;
  prompt: string;
}

async function fanOutFanIn(tasks: AgentTask[], mergePrompt: string) {
  const results = await Promise.all(
    tasks.map(async (task) => {
      const response = await client.messages.create({
        model: "claude-sonnet-4-5-20250514",
        max_tokens: 4096,
        system: `You are a specialized ${task.name} agent. Be thorough and precise.`,
        messages: [{ role: "user", content: task.prompt }],
      });
      return {
        agent: task.name,
        output: response.content[0].type === "text" ? response.content[0].text : "",
      };
    })
  );

  const mergeInput = results
    .map((r) => `## ${r.agent}\n${r.output}`)
    .join("\n\n---\n\n");

  const merged = await client.messages.create({
    model: "claude-sonnet-4-5-20250514",
    max_tokens: 8192,
    system: "You are a synthesis agent. Merge the following agent outputs into a single coherent result. Resolve contradictions. Remove duplicates. Preserve all unique insights.",
    messages: [{ role: "user", content: `${mergePrompt}\n\n${mergeInput}` }],
  });

  return merged.content[0].type === "text" ? merged.content[0].text : "";
}
```

### 2. Pipeline (Sequential Handoff)

Agent A produces output that becomes Agent B's input. Each stage transforms, refines, or builds on the previous result. Output flows in one direction.

**When to use it:** Code generation followed by review. Research followed by synthesis followed by writing. Any workflow with clear stage dependencies.

**The trap:** Pipelines are fragile. If stage 2 produces malformed output, stage 3 crashes. Every pipeline needs validation between stages — either schema checks or a lightweight validator agent.

```python
from anthropic import Anthropic

client = Anthropic()

def run_pipeline(task: str, stages: list[dict]) -> str:
    current_input = task

    for stage in stages:
        response = client.messages.create(
            model="claude-sonnet-4-5-20250514",
            max_tokens=stage.get("max_tokens", 4096),
            system=stage["system_prompt"],
            messages=[{"role": "user", "content": current_input}],
        )
        output = response.content[0].text

        if "validator" in stage:
            is_valid, error = stage["validator"](output)
            if not is_valid:
                retry_response = client.messages.create(
                    model="claude-sonnet-4-5-20250514",
                    max_tokens=stage.get("max_tokens", 4096),
                    system=stage["system_prompt"],
                    messages=[
                        {"role": "user", "content": current_input},
                        {"role": "assistant", "content": output},
                        {"role": "user", "content": f"Validation failed: {error}. Fix and retry."},
                    ],
                )
                output = retry_response.content[0].text

        current_input = output

    return current_input

result = run_pipeline(
    task="Add rate limiting to the /api/generate endpoint",
    stages=[
        {
            "system_prompt": "You are an architect. Break this into implementation steps with file paths and code changes needed.",
            "validator": lambda x: (True, None) if "##" in x else (False, "Output must contain markdown headers for each step"),
        },
        {"system_prompt": "You are a senior developer. Implement each step from the plan. Output complete, working code.", "max_tokens": 8192},
        {"system_prompt": "You are a code reviewer. Review for bugs, security issues, and edge cases.", "max_tokens": 8192},
        {"system_prompt": "You are a technical writer. Write clear documentation for this feature."},
    ],
)
```

### 3. Hierarchical Delegation

A supervisor agent receives a complex task, decomposes it, assigns subtasks to specialist agents, monitors progress, and assembles the final result. The supervisor can reassign failed tasks or adjust the plan mid-execution.

**When to use it:** Complex projects with interdependencies. Tasks requiring adaptive planning where the next step depends on what happened in the previous one.

**The trap:** The supervisor becomes a bottleneck if it micromanages. Give subordinates autonomy within clear boundaries; only escalate to the supervisor on failures or ambiguous requirements.

```typescript
interface SubAgent {
  name: string;
  capabilities: string[];
  systemPrompt: string;
}

interface Task {
  id: string;
  description: string;
  requiredCapabilities: string[];
  dependencies: string[];
  status: "pending" | "running" | "complete" | "failed";
  result?: string;
}

class Supervisor {
  private agents: SubAgent[];
  private tasks: Map<string, Task> = new Map();
  private results: Map<string, string> = new Map();

  constructor(agents: SubAgent[]) { this.agents = agents; }

  findBestAgent(task: Task): SubAgent | undefined {
    return this.agents.find((agent) =>
      task.requiredCapabilities.every((cap) => agent.capabilities.includes(cap))
    );
  }

  async execute(goal: string): Promise<string> {
    const tasks = await this.decompose(goal);
    tasks.forEach((t) => this.tasks.set(t.id, { ...t, status: "pending" }));

    while ([...this.tasks.values()].some((t) => t.status === "pending")) {
      const ready = [...this.tasks.values()].filter(
        (t) =>
          t.status === "pending" &&
          t.dependencies.every((dep) => this.tasks.get(dep)?.status === "complete")
      );

      await Promise.all(
        ready.map(async (task) => {
          const agent = this.findBestAgent(task);
          if (!agent) { task.status = "failed"; return; }
          task.status = "running";

          const context = task.dependencies
            .map((dep) => `Result of ${dep}: ${this.results.get(dep)}`)
            .join("\n");

          const response = await client.messages.create({
            model: "claude-sonnet-4-5-20250514",
            max_tokens: 4096,
            system: agent.systemPrompt,
            messages: [{
              role: "user",
              content: `${task.description}\n\nContext from previous tasks:\n${context}`,
            }],
          });

          const result = response.content[0].type === "text" ? response.content[0].text : "";
          this.results.set(task.id, result);
          task.status = "complete";
        })
      );
    }

    return [...this.results.values()].join("\n\n---\n\n");
  }
}
```

### 4. Blackboard (Shared State)

All agents read from and write to a shared state object. No agent directly communicates with another. A controller monitors state and triggers agents when relevant sections change.

**When to use it:** Problems where the solution emerges from multiple perspectives iterating on shared data. Code review cycles. Collaborative document editing. Systems where agents need to react to each other's work without explicit messaging.

**The trap:** Race conditions. Two agents writing to the same state key simultaneously. Use optimistic locking or a queue-based write system.

```typescript
interface BlackboardState {
  [key: string]: { value: any; lastUpdatedBy: string; version: number; };
}

type AgentTrigger = {
  agent: SubAgent;
  watchKeys: string[];
  handler: (state: BlackboardState, changedKey: string) => Promise<Partial<BlackboardState>>;
};

class Blackboard {
  private state: BlackboardState = {};
  private triggers: AgentTrigger[] = [];

  register(trigger: AgentTrigger) { this.triggers.push(trigger); }

  async write(key: string, value: any, author: string) {
    const current = this.state[key];
    this.state[key] = { value, lastUpdatedBy: author, version: (current?.version ?? 0) + 1 };

    const watchers = this.triggers.filter(
      (t) => t.watchKeys.includes(key) && t.agent.name !== author
    );

    for (const watcher of watchers) {
      const updates = await watcher.handler(this.state, key);
      for (const [k, v] of Object.entries(updates)) {
        await this.write(k, v, watcher.agent.name);
      }
    }
  }

  getState(): BlackboardState { return structuredClone(this.state); }
}
```

### 5. Handoff Chain (Agent-to-Agent Transfer)

One agent works on a task until it hits the boundary of its expertise, then transfers control (and full context) to a more appropriate agent. Handoffs are non-linear — Agent A might hand off to B, which hands off to C, which hands back to A.

This is the model that OpenAI Agents SDK and Claude Code's sub-agent system use natively.

**When to use it:** Customer support routing. Complex debugging where the problem crosses domains. Any workflow where the right specialist depends on runtime conditions.

```python
from agents import Agent, Runner

frontend_agent = Agent(
    name="Frontend Specialist",
    instructions="You handle React, CSS, and browser-side issues. Hand off to backend_agent for API or database problems.",
    handoffs=["backend_agent"],
)

backend_agent = Agent(
    name="Backend Specialist",
    instructions="You handle API routes, database queries, and server logic. Hand off to devops_agent for deployment or infrastructure problems.",
    handoffs=["devops_agent"],
)

devops_agent = Agent(
    name="DevOps Specialist",
    instructions="You handle deployment, CI/CD, Docker, and infrastructure. Hand off to frontend_agent if the issue is client-side.",
    handoffs=["frontend_agent"],
)

triage_agent = Agent(
    name="Triage",
    instructions="Analyze the issue and hand off to the most appropriate specialist.",
    handoffs=[frontend_agent, backend_agent, devops_agent],
)

result = await Runner.run(triage_agent, "The /api/users endpoint returns 500 but only in production")
```

### 6. Consensus (Vote and Merge)

Multiple agents independently solve the same problem, then a judge agent evaluates the solutions and selects the best one (or synthesizes elements from multiple solutions).

**When to use it:** High-stakes code generation where correctness matters more than speed. Architectural decisions with multiple valid approaches. Any task where you want diversity of solutions before committing.

```typescript
async function consensus(
  task: string,
  numCandidates: number = 3,
  evaluationCriteria: string
) {
  const candidates = await Promise.all(
    Array.from({ length: numCandidates }, (_, i) =>
      client.messages.create({
        model: "claude-sonnet-4-5-20250514",
        max_tokens: 4096,
        system: `You are solution generator #${i + 1}. Solve the task independently. Do not hedge.`,
        messages: [{ role: "user", content: task }],
      })
    )
  );

  const solutions = candidates.map((c, i) => ({
    id: i + 1,
    content: c.content[0].type === "text" ? c.content[0].text : "",
  }));

  const judgeInput = solutions
    .map((s) => `## Solution ${s.id}\n${s.content}`)
    .join("\n\n---\n\n");

  const judgment = await client.messages.create({
    model: "claude-sonnet-4-5-20250514",
    max_tokens: 4096,
    system: `You are an expert evaluator. Compare solutions against: ${evaluationCriteria}. Select the best or synthesize the strongest elements. Output JSON: { "winner": "...", "reasoning": "..." }`,
    messages: [{ role: "user", content: judgeInput }],
  });

  return JSON.parse(judgment.content[0].type === "text" ? judgment.content[0].text : "{}");
}
```

## Framework Implementation Guide

### CrewAI: Role-Based Crews

CrewAI models agents as team members with roles, goals, and backstories. Coordination happens through Crews (groups of agents executing Tasks) and Flows (event-driven pipelines connecting multiple Crews).

```python
from crewai import Agent, Task, Crew, Process

researcher = Agent(
    role="Senior Research Analyst",
    goal="Find comprehensive technical information about the given topic",
    backstory="Veteran technical researcher who values accuracy over speed.",
    tools=[web_search, scrape_url],
    verbose=True,
)

writer = Agent(
    role="Technical Writer",
    goal="Transform research into clear, actionable documentation",
    backstory="Writes for practitioners who want to build, not theorize.",
    verbose=True,
)

reviewer = Agent(
    role="Technical Editor",
    goal="Ensure accuracy, completeness, and clarity",
    backstory="Zero tolerance for hand-waving.",
    verbose=True,
)

research_task = Task(
    description="Research {topic} comprehensively.",
    expected_output="A structured research report with sections, code blocks, citations.",
    agent=researcher,
)

writing_task = Task(
    description="Write a technical guide based on the research.",
    expected_output="A 2000+ word guide with introduction, sections, code examples.",
    agent=writer,
    context=[research_task],
)

review_task = Task(
    description="Review the guide for technical accuracy.",
    expected_output="Reviewed guide with corrections applied and editor notes.",
    agent=reviewer,
    context=[writing_task],
)

crew = Crew(
    agents=[researcher, writer, reviewer],
    tasks=[research_task, writing_task, review_task],
    process=Process.sequential,
    memory=True,
    planning=True,
)

result = crew.kickoff(inputs={"topic": "WebSocket authentication patterns"})
```

**CrewAI Flows** connect multiple Crews with conditional routing:

```python
from crewai.flow.flow import Flow, listen, start

class ContentPipeline(Flow):
    @start()
    def research_phase(self):
        research_crew = Crew(agents=[researcher], tasks=[research_task])
        self.state["research"] = research_crew.kickoff()

    @listen(research_phase)
    def writing_phase(self):
        if len(self.state["research"].raw) < 500:
            return self.research_phase()
        writing_crew = Crew(agents=[writer], tasks=[writing_task])
        self.state["draft"] = writing_crew.kickoff()

    @listen(writing_phase)
    def review_phase(self):
        review_crew = Crew(agents=[reviewer], tasks=[review_task])
        self.state["final"] = review_crew.kickoff()

pipeline = ContentPipeline()
result = pipeline.kickoff()
```

### LangGraph: State Machines

LangGraph models coordination as a directed graph with typed state. Nodes are functions; edges are transitions; state is the communication channel.

```python
from langgraph.graph import StateGraph, START, END
from typing import TypedDict, Annotated
from operator import add

class AgentState(TypedDict):
    task: str
    research: Annotated[list[str], add]
    code: str
    review: str
    final_output: str

def research_node(state: AgentState) -> dict:
    result = research_agent.invoke({"messages": [{"role": "user", "content": state["task"]}]})
    return {"research": [result["messages"][-1].content]}

def code_node(state: AgentState) -> dict:
    context = "\n".join(state["research"])
    result = code_agent.invoke({
        "messages": [{"role": "user", "content": f"Task: {state['task']}\nResearch: {context}"}]
    })
    return {"code": result["messages"][-1].content}

def review_node(state: AgentState) -> dict:
    result = review_agent.invoke({
        "messages": [{"role": "user", "content": f"Review this code:\n{state['code']}"}]
    })
    return {"review": result["messages"][-1].content}

def should_revise(state: AgentState) -> str:
    return "finalize" if "APPROVED" in state["review"] else "code"

graph = StateGraph(AgentState)
graph.add_node("research", research_node)
graph.add_node("code", code_node)
graph.add_node("review", review_node)
graph.add_node("finalize", lambda s: {"final_output": s["code"]})

graph.add_edge(START, "research")
graph.add_edge("research", "code")
graph.add_edge("code", "review")
graph.add_conditional_edges("review", should_revise, {"finalize": "finalize", "code": "code"})
graph.add_edge("finalize", END)

app = graph.compile()
result = app.invoke({"task": "Build a rate limiter middleware for Express"})
```

LangGraph's strength is explicit control flow. Every loop, branch, and convergence is visible and debuggable. State is typed and trackable. Use LangSmith for built-in tracing.

### AutoGen / AG2: Conversation-Based

AG2 models multi-agent coordination as conversations. Agents send messages to each other, and the framework manages turn-taking, termination conditions, and group dynamics.

```python
from autogen import ConversableAgent, GroupChat, GroupChatManager

planner = ConversableAgent(
    name="Planner",
    system_message="You break down complex tasks into actionable steps. Output numbered lists.",
    llm_config={"model": "claude-sonnet-4-5-20250514"},
)

coder = ConversableAgent(
    name="Coder",
    system_message="You write production-quality TypeScript with error handling and types.",
    llm_config={"model": "claude-sonnet-4-5-20250514"},
)

critic = ConversableAgent(
    name="Critic",
    system_message="You review code for bugs, performance, and security. Be specific.",
    llm_config={"model": "claude-sonnet-4-5-20250514"},
)

group_chat = GroupChat(
    agents=[planner, coder, critic],
    messages=[],
    max_round=12,
    speaker_selection_method="auto",
)

manager = GroupChatManager(groupchat=group_chat)

planner.initiate_chat(
    manager,
    message="We need to add WebSocket support to our Express API with JWT authentication.",
)
```

AG2's MemoryStream architecture makes every conversation event-driven and replayable. You can step through execution event by event for debugging and pause for human review.

### Google ADK: Hierarchical Agent Trees

Google's ADK models coordination as a hierarchy. A root agent delegates to child agents, which can have their own children. The framework handles routing, context passing, and result aggregation.

```python
from google.adk.agents import Agent
from google.adk.runners import Runner
from google.adk.sessions import InMemorySessionService

research_agent = Agent(
    name="researcher",
    model="gemini-2.5-flash",
    instruction="Research the given topic thoroughly. Return structured findings.",
    tools=[google_search, web_scraper],
)

code_agent = Agent(
    name="coder",
    model="gemini-2.5-pro",
    instruction="Write clean, tested code based on specifications.",
    tools=[code_execution],
)

coordinator = Agent(
    name="coordinator",
    model="gemini-2.5-pro",
    instruction="Coordinate a development team. Delegate research to @researcher, coding to @coder, synthesize final deliverables.",
    sub_agents=[research_agent, code_agent],
)

session_service = InMemorySessionService()
runner = Runner(agent=coordinator, app_name="dev-team", session_service=session_service)
result = runner.run(user_id="dev", session_id="s1", new_message="Build a CLI tool for transcoding video files")
```

ADK's advantage is deep Google Cloud integration. Deploy to Vertex AI Agent Engine, Cloud Run, or GKE with built-in auth and Cloud Trace observability.

### Claude Code: Native Task Delegation

Claude Code handles multi-agent coordination through its built-in Task tool and custom sub-agents defined in markdown files. No external framework needed.

```markdown
<!-- .claude/agents/researcher.md -->
---
name: researcher
description: Researches technical topics using web search and documentation
tools:
  - WebSearch
  - WebFetch
  - Read
---

You are a technical research specialist. When given a topic:
1. Search for the latest documentation and release notes
2. Find working code examples
3. Identify common pitfalls and known issues
4. Return structured findings with source URLs
```

```markdown
<!-- .claude/agents/implementer.md -->
---
name: implementer
description: Writes production code based on specifications
tools:
  - Read
  - Edit
  - Write
  - Bash
---

You are a senior developer. Write clean, typed, tested code.
Follow the project's existing patterns. Check CLAUDE.md for conventions.
```

Claude Code agents share project context inherently. They can read the file system and understand the codebase without external tooling or API wiring.

## Choosing the Right Pattern

| Pattern | Best For | Key Decision Factor |
|---|---|---|
| Fan-Out/Fan-In | Independent subtasks | Do subtasks depend on each other? |
| Pipeline | Sequential dependencies | Is output of stage N required input for stage N+1? |
| Hierarchical | Adaptive planning | Does the plan need to change mid-execution? |
| Blackboard | Iterative refinement | Do agents need to react to each other's work on shared state? |
| Handoff Chain | Runtime routing | Does the right specialist depend on runtime conditions? |
| Consensus | High-stakes decisions | Does correctness matter more than speed? |

**Start with fan-out/fan-in** if your subtasks are independent. Most tasks are more parallelizable than you think. Add a dedicated merge/aggregator step — never skip it.

## Production Considerations

### State Management

| Framework | State Model | Strengths |
|---|---|---|
| LangGraph | Typed state with reducers | Explicit, checkpointable, serializable |
| CrewAI | Shared memory (short/long/entity/contextual) | Cross-agent knowledge retention |
| AG2 | MemoryStream (pub/sub per conversation) | Event-driven, replayable, isolated |
| Google ADK | Hierarchical state | Deep Cloud integration |
| Claude Code | File system | Simple, debuggable, zero infrastructure |

### Error Handling

Agents fail. Models hallucinate. API calls time out. Production systems need four strategies:

1. **Retry with context** — when an agent fails, retry with the error message in context so it can self-correct
2. **Fallback agents** — if the primary agent fails after retries, route to a different agent or model
3. **Circuit breakers** — if an agent loop exceeds N iterations without progress, break and escalate
4. **Structured outputs** — use JSON schemas or Pydantic models to validate agent outputs at every handoff

```typescript
async function resilientAgentCall(
  agent: SubAgent,
  input: string,
  maxRetries: number = 3
): Promise<string> {
  let lastError = "";

  for (let attempt = 0; attempt < maxRetries; attempt++) {
    try {
      const prompt = lastError
        ? `Previous attempt failed: ${lastError}\n\nOriginal task: ${input}`
        : input;

      const response = await client.messages.create({
        model: "claude-sonnet-4-5-20250514",
        max_tokens: 4096,
        system: agent.systemPrompt,
        messages: [{ role: "user", content: prompt }],
      });

      const output = response.content[0].type === "text" ? response.content[0].text : "";

      if (agent.outputSchema) {
        agent.outputSchema.parse(JSON.parse(output));
      }

      return output;
    } catch (error) {
      lastError = error instanceof Error ? error.message : String(error);
    }
  }

  throw new Error(`Agent ${agent.name} failed after ${maxRetries} attempts: ${lastError}`);
}
```

### Cost Control

Multi-agent systems multiply API costs linearly with the number of agents. Control costs by:

- **Using cheaper models for simple tasks.** Route research and summarization to Claude Haiku or GPT-4o-mini. Reserve Sonnet or Opus for complex reasoning.
- **Setting iteration caps.** Never let a review loop run indefinitely. Three iterations is usually enough.
- **Caching aggressively.** If multiple agents need the same context (file contents, API docs), fetch once and share.
- **Monitoring token usage per agent.** The agent consuming the most tokens is either doing the most work or the most wasted work. Instrument and measure.

### Observability

You cannot debug a multi-agent system by reading logs alone. You need traces that show which agent ran when, what input it received, what output it produced, and how long it took. Use framework-native tracing (LangSmith for LangGraph, verbose mode for CrewAI, step-through for AG2) or instrument with OpenTelemetry spans per agent call.

## Pragmatic Path

1. **Start with fan-out/fan-in using raw API calls.** No framework. Just `Promise.all()` with a merge step. This handles ~60% of multi-agent use cases.
2. **Add a framework when you need loops or state.** If your agents need to iterate (review cycles, planning loops), LangGraph makes those loops explicit and debuggable. For role-based teams with memory, CrewAI gets you there faster.
3. **Use Claude Code's native agents for development workflows.** If the use case is "help me build software faster," Claude Code's sub-agent system is the most practical option because it already understands codebases, the file system, and development tools.
4. **Use OpenAI Agents SDK for customer-facing handoff flows.** The handoff primitive is first-class and the SDK is lightweight. Good for support bots, triage systems, and intelligent routing.
5. **Use Google ADK if you are in the Google Cloud ecosystem.** The deployment story to Vertex AI is seamless, and the hierarchical model maps well to organizational structures.

The framework choice matters less than the coordination pattern. Get the pattern right first, then pick the framework that makes that pattern easiest to implement and debug.

## Field-Tested Operating Model for Agent Fleets

The following rules are operational guardrails proven in production when running dozens of agents in parallel against a single codebase. Each rule exists because the alternative caused a real failure.

### 1. Single-owner file scopes

Never two writers per file. Every agent gets a scope, and scopes do not overlap at the file level. When two agents both need to touch a shared file, that is a signal to serialize them, not to let them both edit and merge later.

Decompose along ownership lines, not feature lines. A change spanning a shared file means one agent lands the shared change first, and the others build on top of it.

### 2. Serialized dependency installs

Package management is a shared-file problem with extra teeth. Two agents running `pnpm add` simultaneously race on the lockfile and `package.json`, and the loser's install silently vanishes or corrupts the tree. One agent owns `package.json` at a time.

### 3. A verification gate on every handoff

The orchestrator runs the same gate on every handoff, not at the end. The gate is:

1. **Typecheck**
2. **Style check** (for banned patterns)
3. **Full build**
4. **Isolated-worktree check** — checks out the actual commit into a throwaway worktree and typechecks it in isolation from the working directory. This catches the case where a commit imports a file that exists on disk but was never staged, which passes every local check and then fails in CI.

Principle: verify the artifact you are about to ship, not the environment you built it in. The working directory lies; the commit does not.

```bash
# Isolated-worktree verification gate
git commit -m "WIP: agent changes"
WORKDIR=$(mktemp -d)
git worktree add "$WORKDIR" HEAD
cd "$WORKDIR"
yarn install --frozen-lockfile
yarn typecheck
EXIT_CODE=$?
git worktree remove --force "$WORKDIR"
exit $EXIT_CODE
```

### 4. Draft-first for anything externally visible

Anything that leaves the building starts as a draft for review. Content, public copy, anything a reader or customer would see. The agent produces it, a human or review pass approves it, and only then does it ship. The cost of a review pass is small; the cost of a bad externally-visible change is asymmetric.

### 5. Standing constraints broadcast to all agents

Some rules apply to every agent regardless of scope: banned topics, the design contract (square corners, hairline borders, no gradients, no em dashes), etc. Broadcast them as standing constraints and codify them in project instructions so they are inherited, not remembered. A constraint you have to remember is a constraint you will eventually break.

### 6. Fail-closed defaults for anything that spends money

Any action that spends money or touches a live external system defaults to off. If an agent is unsure whether it is authorized, the default is to stop and ask, not to proceed and apologize. Fail-closed is the only safe default for irreversible or costly actions.

### 7. Continuous shipping, never batch a day's work

Verify, commit, push per increment. Never let a day of parallel work pile up into one giant unreviewed merge. Each increment goes through the gate and ships on its own. Small, continuous, verified increments keep the blast radius of any single mistake tiny.

### Failure modes and guardrails

| Failure | Root Cause | Guardrail |
|---|---|---|
| Agent imports unshipped sibling export | Agent builds against promised work, not committed work | Agents build against what is committed, not what is promised |
| Mid-write file breaks global CSS | Shared global file in a mid-write state | Shared global files have a single owner who lands complete, verified changes |
| Silent idle with no report | Orchestrator cannot distinguish stuck from working | Every agent reports on handoff; silence is treated as stalled |
| Env file clobbered by a tool | Tool overwrites shared config as a side effect | Env and shared config files are owned, single-writer surfaces |
| Deploy breaks on package-manager default | Warm local environment hides cold CI failures | Test dependency/config changes against a clean, frozen-lockfile install that mirrors CI |

**Common thread:** the failures were never the model being dumb. They were two pieces of work making incompatible assumptions about shared state — a file, an export, an env var, a lockfile, a build default. The fix was always the same shape: make the shared thing owned, verify the real artifact, and never assume a sibling's promise is a sibling's commit.

### Starter checklist for fleet coordination

1. Assign single-owner file scopes. No file has two writers.
2. Serialize dependency installs through one owner.
3. Run a verification gate on every handoff: typecheck, style check, build.
4. Add the isolated-worktree check to that gate.
5. Draft-first everything externally visible.
6. Broadcast standing constraints to all agents; codify in project instructions.
7. Default to fail-closed on anything that spends money or touches production.
8. Ship continuously: verify, commit, push per increment.
9. Require a report on every handoff. Treat silence as stalled.
10. Test dependency and config changes against a clean, CI-like install before pushing.

## Frequently Asked Questions

### What is multi-agent AI orchestration?

Coordinating multiple AI agents to work together on complex tasks. Instead of one agent doing everything, you decompose work across specialists — a researcher agent, a coding agent, a reviewer agent — and coordinate their outputs. The challenge is communication, shared context, and conflict resolution.

### Which multi-agent framework should I use?

- **LangGraph** — explicit control flow and debuggable state machines
- **CrewAI** — role-based teams with shared memory
- **AutoGen/AG2** — conversation-based coordination
- **OpenAI Agents SDK** — customer-facing handoff flows
- **Google ADK** — Google Cloud deployments
- **Claude Code sub-agents** — development workflows

Most teams start with raw API calls and `Promise.all()` before adopting a framework.

### Difference between fan-out and pipeline?

Fan-out runs multiple agents in parallel on independent subtasks and merges outputs. Pipeline runs agents sequentially where each stage transforms the previous output. Use fan-out when subtasks have no dependencies; pipeline when there are clear sequential dependencies.

### How do AI agents communicate?

Four main mechanisms: shared state (blackboard), message passing (AutoGen), explicit handoffs (OpenAI Agents SDK, Claude Code), and typed state transitions (LangGraph). Choose based on whether you need reactive updates, turn-taking, or explicit control flow.

### How do I handle errors?

Use retry with context, fallback agents, circuit breakers (break loops after N iterations), and structured output validation with JSON schemas at every handoff point.

### How expensive are multi-agent systems?

Three agents running in parallel cost 3x a single agent. A five-iteration review loop costs 5x a single pass. Use cheaper models for simple tasks (Haiku for research, Sonnet for complex reasoning), set iteration caps, cache shared context, and monitor token usage per agent.