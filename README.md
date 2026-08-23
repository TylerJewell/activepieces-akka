# activepieces-akka

Runs a flow: walks a tree of steps, loops over lists, takes one branch of a router, retries a
step that failed, and records what every step produced.

A port of [activepieces/activepieces](https://github.com/activepieces/activepieces) onto **Akka**,
built with **Akka Specify**.

---

## Where it came from

activepieces is a workflow builder: you draw a sequence of steps in a browser, connect it to
something that starts it, and it runs. It was ported to derive a specification format precise
enough to regenerate a system on a different stack — the port is the vehicle, the specification is
the deliverable.

The specifications the port was generated from are in
[TylerJewell/akka-specify-harness](https://github.com/TylerJewell/akka-specify-harness) under
`activepieces-port/`.

---

## activepieces/activepieces → this port

📉 692 TypeScript lines → **809 Java lines**<br>
📁 8 files → **18 files**<br>
⚡ 497,312 → **11,452** nanoseconds to decide twenty-one branches of a router<br>
⚡ 14,477,650 → **1,019,769** nanoseconds to run a two-hundred-item loop<br>
⚡ 2,691,954 → **391,500** nanoseconds to run ten loops inside a loop<br>
⚡ 2,699,409 → **233,757** nanoseconds to walk a fifty-step chain<br>
🎯 38 answers compared → **38 the same**<br>
🧪 68 tests<br>
🖼️ 2 screens compared against the original → **2 matching, 6 declared differences**

Full method and the numbers that did *not* make this list:
[`bench/REPORT.md`](https://github.com/TylerJewell/akka-specify-harness/blob/main/activepieces-port/bench/REPORT.md).

The line counts compare the parts of each system that do the same job, by symbol name, not the two
repositories. The four speed lines are the workloads that hold no step body, because a step body
in activepieces is a separate operating-system process and here it is a function call — on the two
workloads that do hold one, that process is 99.8% of activepieces' figure, and the report gives
both readings.

---

## What it took to build

⏱️ **5.8 hours** from the first command to the published repository, **4.7** of them active<br>
💬 **1,220** exchanges with the model<br>
✍️ **835,544** tokens written by the model, **367,524,223** counting everything sent and re-sent<br>
🙋 **0** questions to a human<br>
🧪 **68** tests

```bash
python toolkit/tokens.py --port activepieces
```

The record of every question, and where the time went, is in
[`port-log/`](https://github.com/TylerJewell/akka-specify-harness/tree/main/port-log).

---

## What it does

From the specification:

- **A step already recorded is not run a second time.** Walking the same flow again after it
  stopped partway picks up where it left off instead of starting over, unless the step was left
  waiting, in which case it runs again and its old result is replaced.
- **A step that fails is tried again only when it was told to be.** Four attempts by default,
  waiting four, then eight, then sixteen seconds, and every attempt starts from the record as it
  stood before the first one.
- **A loop that fails inside itself is not a loop that failed.** The run stops, the failing step
  inside the loop is marked failed, and the loop keeps the iterations it began and its own
  successful mark.
- **Every branch of a router is decided before any of them runs.** A branch marked as the
  otherwise-case is taken only when every other branch came out false, so two otherwise-cases
  cancel each other and nothing runs.
- **A run that grows too large stops.** The record of what each step produced is measured as it is
  written, and the step that pushes it past the limit is replaced by a failure saying so.
- **A watcher is sent the whole run, not the change.** Anyone watching a run gets its current state
  the moment they connect and again whenever it moves, so a browser that lost its connection and
  came back is told where things stand rather than left to work out what it missed.

---

## Design decisions

**The step body is a plug.** activepieces runs each step's code in a separate operating-system
process so somebody else's integration cannot bring the run down, and rebuilding that would have
been rebuilding the sandbox rather than the rules for running steps. Here a step calls a function
handed to it, which makes the rules testable in microseconds and keeps the port about the part it
set out to compare.

**One record per run.** Everything a run knows — which steps finished, what each produced, whether
it is still going — is kept together under the run's own name rather than spread across tables.
Reading a run is one lookup and writing to it cannot race with another run.

**The record is capped below what the platform can move.** A run's record is also what gets copied
between data centres, and past half a megabyte that copying stops working, so the limit on how
large a run's record may grow is set under that rather than at the larger figure activepieces uses.
A run that grows too big fails in the way the rules say it should instead of failing in a way
nobody described.

**The screen is activepieces' own.** The two screens that show a run were kept exactly as
activepieces draws them and only rewired to take their data from here, so a picture of one next to
a picture of the other is a comparison rather than a matter of taste. Four files out of 1,095
changed.

**Nothing asks twice.** The two screens hold one connection open and are sent each change as it
happens, instead of asking again every fifteen seconds. Sixty-five seconds with the page open and
nobody touching it produce no requests at all.

---

## Running it — the short path

You do not need Java, Maven, or the Akka CLI installed. Akka Specify installs them for you.

**1. Install Akka Specify** in Claude Code:

```
/plugin marketplace add akka/ai-marketplace
/plugin install akka@akka-ai-marketplace
```

Restart Claude Code when it asks.

**2. Give it this prompt:**

> Clone https://github.com/TylerJewell/activepieces-akka into a new directory and open it.
> Then run /akka:setup to install everything this project needs, build the browser interface
> with `cd gui && npm install && npx vite build`, and run /akka:build to compile it, run the
> tests, and start it locally. Then start a run with
> `curl -X POST http://localhost:9065/api/v1/flow-runs/start`.

**3. Open** http://localhost:9065.

---

## Running it — the developer path

### Requirements

- Java 21 or newer
- Maven 3.9 or newer
- An Akka download token — run `akka code token` once
- Node 20 or newer, to build the browser interface

### Build the browser interface

The interface is activepieces' own, kept in `gui/`. What it builds into is not stored here, so
build it once before starting the service:

```bash
cd gui
npm install
npx vite build
```

That writes into `src/main/resources/static-resources/`, which the service serves.

### Start the service

```bash
mvn compile
akka local run
```

The service starts on **port 9065**. Start a run and then watch it:

```bash
curl -X POST http://localhost:9065/api/v1/flow-runs/start
```

Open http://localhost:9065 and the run appears in the list; open it to watch the steps arrive.
The run takes about twenty-eight seconds, because its last step fails and is retried three times
with the waits the rules call for.

### Run the tests

```bash
mvn verify
```

62 run without a server; 6 start one and drive it over HTTP.

---

## Configuration

| Variable | Default | Notes |
|---|---|---|
| `akka.javasdk.dev-mode.http-port` | 9065 | in `src/main/resources/application.conf` |

No model provider is used: nothing here calls a language model.

---

## Where it differs from activepieces/activepieces

Everything not listed here behaves the same way on purpose, including the parts that look like
mistakes.

- **A step's body is a function, not a separate process.** activepieces compiles each step's code
  and runs it in its own operating-system process. This port calls a handler given to it when the
  flow is built, because the rules for *running* steps are what it set out to rebuild and the
  sandbox is a different system. Anything a real step body would do — installing packages, calling
  someone else's service, timing out — is therefore absent.
- **What a step is allowed to refer to is much smaller.** activepieces resolves
  `{{ step.output.field }}` through an expression language with its own functions and scripting.
  This port resolves a whole-value reference and a dotted path into an earlier step's result, and
  nothing else, because the expression language is a system of its own.
- **A step's recorded duration is under a millisecond instead of tens.** Both sides measure the
  same thing — how long the step's body took — and the bodies are not the same kind of thing. The
  number on the screen differs for that reason and not because one is faster at deciding anything.
- **The elapsed time of a run has one part, not four.** activepieces reports how long a run waited
  in a queue, how long a worker took to appear, how long it took to start, and how long it ran,
  because a run there is handed to a worker first. This port runs it where the request arrives, so
  the only part it can measure is the run.
- **The queue indicator on the runs list is drawn in its neutral state.** activepieces colours it
  from how many runs are waiting. There is no queue here, so there is nothing for it to report.
- **A failed step's technical details repeat its message.** activepieces puts the failing code's
  stack trace there. This port has no stack trace to offer, so the message appears in both places.
- **Entering the runner with a run that already stopped does nothing here, and runs one more step
  there.** activepieces checks whether the run is still going only after a step has run. Nothing in
  activepieces reaches that state — every caller checks first — so this is a difference in what the
  two would do rather than in what either does; this port checks before, because a rule that says
  the walk stops is easier to trust when it cannot be entered past.
- **Storing a step's result in a separate file when it is very large is not implemented, and not
  checked.** activepieces moves an oversized result out of the run's record and leaves a reference.
  This port keeps everything in the record and stops the run when the record grows too large. What
  activepieces does when such a reference is restored on resume is `not checked`.
- **How much a run may hold before it stops is half a megabyte, not five.** The number is a setting
  in both. This port sets it under the size at which a run's record stops being copied between data
  centres, so the failure a run gets is the one the rules describe.
- **A step that fails and is retried loses nothing here, and could lose something there.**
  Both hand every attempt the record as it stood before the first attempt. In activepieces a step
  body can write elsewhere before it throws, and that writing survives; here a step body only
  returns a value, so there is nothing for a retry to discard.
- **There is no signing in.** Who you are is outside what this port rebuilt, so the interface is
  handed a signed-in session when it starts.
- **There is one flow, and it cannot be edited.** activepieces is a builder for making flows. This
  port ships the one flow its rules are demonstrated on. The screens that show a *run* are
  activepieces' own; the screens for building are present but there is nothing here for them to
  save to.
- **The wording of an error differs.** A step that fails reports the message the failure carried;
  activepieces reports its own wrapper around the failing code's output. Whether a step failed, and
  which one, is the same on both sides — the words are not compared.

---

## Licence

activepieces/activepieces is MIT Expat, © 2020-2024 Activepieces Inc., except for everything under
`packages/ee/`, which is under the Activepieces Enterprise License. This port is a derived work and
ships activepieces' own browser interface, one directory of which comes from `packages/ee/`; see
[`ACKNOWLEDGEMENTS.md`](ACKNOWLEDGEMENTS.md), which records what that means for making this
repository public.
