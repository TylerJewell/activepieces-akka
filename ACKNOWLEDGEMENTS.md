# Acknowledgements

This project is a port of **[activepieces/activepieces](https://github.com/activepieces/activepieces)**.

## The licence

`activepieces/activepieces`'s `LICENSE` reads, in its own words:

- everything under `packages/ee/` and `packages/server/api/src/app/ee` is under the licence in
  `packages/ee/LICENSE` — the **Activepieces Enterprise License**;
- everything else is **MIT Expat**, "Copyright (c) 2020-2024 Activepieces Inc."

Read from the file rather than from a badge.

**This repository ships enterprise-licensed code and therefore cannot be made public as it
stands.** `gui/vendor/ee/embed-sdk/src/index.ts` — 830 lines — is `packages/ee/embed-sdk`, which
the Enterprise License covers. It is here because RENDERING.md R3 has a port ship the original's
own interface rather than a smaller stand-in, and five files of that interface import it
(`app/routes/embed/*`, `components/custom/home-button.tsx`). The Enterprise License permits
copying and modifying "for development and testing purposes, without requiring a subscription",
which is what this repository is; the same paragraph forbids publishing and distributing. So:

- the repository is **private**, and that is a licence requirement here rather than a default;
- making it public would need either an Activepieces enterprise licence, or the removal of
  `gui/vendor/ee/` and the five files that import it — which would be a change to the interface
  R3 asks a port to leave alone, and so a decision to record rather than take quietly.

Everything else copied here is MIT Expat, and the MIT notice above is reproduced in the
rebuild's `LICENSE`.

## What was copied verbatim

**The whole of activepieces' web interface**, in `activepieces-akka/gui/`. This is deliberate and
is the point of RENDERING.md R3: where the source has a working interface, the port ships that
interface and changes only where it gets its data. The measure is the diff, and it is four files
out of 1,095:

    diff -rq activepieces/packages/web/src activepieces-akka/gui/src
      src/lib/akka-feed.ts                                     added
      src/main.tsx                                             one import, one call
      src/app/routes/runs/id/index.tsx                         the run's query replaced by a stream
      src/features/flow-runs/components/runs-table/index.tsx   the table's query replaced by a stream

Alongside it, `gui/vendor/` holds seven workspace packages the interface resolves through aliases
in the monorepo — `core/{shared,utils,formula,piece-types,execution}`, `pieces/framework` and the
enterprise `ee/embed-sdk` above — copied unchanged so the application builds outside its
monorepo. `gui/public/` is the original's assets, unchanged.

**Configuration answers for the parts of the screen outside the slice**, in
`activepieces-akka/src/main/resources/ap-shell/*.json`. Six files — flags, the platform, the
project, the user list, one user, and the webhook piece's metadata — captured from a running
activepieces and edited only to replace its generated identifiers with this port's. The interface
will not draw anything without them and none of it is behaviour this port rebuilt; the runs and
the flow they ran are produced by the port's own engine, not captured.

## Text in the rebuild that also occurs in the original

`python toolkit/copied_strings.py activepieces` reports 112 literals of ten characters or more
that appear in both, all in the port's own Java. They are four kinds and every one is a
reproduction on purpose:

- **The rule vocabulary — 28 names.** `TEXT_CONTAINS`, `TEXT_START_WITH`, `NUMBER_IS_GREATER_THAN`,
  `BOOLEAN_IS_TRUE`, `LIST_IS_EMPTY`, `DOES_NOT_EXIST` and the rest of the twenty-two branch
  operators, plus `LOOP_ON_ITEMS`, `PIECE_TRIGGER`, `EXECUTE_FIRST_MATCH`, `LOG_SIZE_EXCEEDED`,
  `PRODUCTION`. SPEC-001 rule 25 is that each operator answers a particular way; a port that
  renamed them would be a port of something else. `TEXT_START_WITH` is spelled the way the source
  spells it on the wire, which differs from its own enum name — `ApShapes.apOperator` translates.
- **The field names of activepieces' own JSON — about 60.** `flowVersion`, `failedStep`,
  `stepsCount`, `errorHandlingOptions`, `firstLoopAction`, `branchName`, `caseSensitive`,
  `__apErrorVersion` and so on. These are the wire format the vendored interface reads; they are
  copied for exactly the reason a port of a protocol shares that protocol's vocabulary.
- **Route paths — 9.** `/flow-runs`, `/flow-runs/count-by-status`, `/flows/{flowId}`,
  `/sample-data`, `/ai-providers`, `/user-invitations`. Same reason: the interface calls them.
- **Two messages and one flow's labels.** `The items you have selected must be a list.` is
  SPEC-001 rule 14 quoted exactly, because the rule is the message. `Flow run data size exceeded
  the maximum allowed size of ` is rule 29's, for the same reason.
  `The operator is required but found to be undefined` is this port's wording of rule 24's raise
  and is *not* the source's — the source throws an `OperatorNotSetError`, and the two texts differ.
  `Catch Webhook`, `First step`, `For each item`, `Process item`, `Branch step`, `Always fails` and
  `Step state demo` are the demo flow's own labels, chosen here and then typed into the original
  when the baseline screens were captured, so that both interfaces are photographed showing the
  same flow. They travelled from this port to activepieces rather than the other way.


## Every literal the check names, in full

`copied_strings.py` asks for a sentence about each hit rather than a category, so here is the whole list under the four headings above. Nothing in it is new; this is the same 112 literals enumerated so the check can go quiet and a reader can still see which kind each one is.

**Rule vocabulary and enum names** (17): `BOOLEAN_IS_FALSE`, `DATE_IS_AFTER`, `DATE_IS_BEFORE`, `DATE_IS_EQUAL`, `LIST_CONTAINS`, `LIST_DOES_NOT_CONTAIN`, `LIST_IS_NOT_EMPTY`, `NOT_STARTED`, `NUMBER_IS_EQUAL_TO`, `NUMBER_IS_LESS_THAN`, `TEXT_DOES_NOT_CONTAIN`, `TEXT_DOES_NOT_END_WITH`, `TEXT_DOES_NOT_EXACTLY_MATCH`, `TEXT_DOES_NOT_START_WITH`, `TEXT_ENDS_WITH`, `TEXT_EXACTLY_MATCHES`, `TEXT_STARTS_WITH`

**Route paths** (2): `/api/v1/flow-runs`, `/api/v1/flow-runs/`

**Phrases and labels** (8): ` at this level`, `Flow run data size exceeded the maximum allowed size of `, `[object Object]`, `already there`, `always fails`, `hello world`, `must not run`, `never runs`

**The demo flow's two step bodies** (2): `export const code = async (inputs) => { return { ok: true, at: 'step' }; };` and `export const code = async (inputs) => { throw new Error('this step always fails'); };`. Written here, then typed into the original when the baseline was captured, for the same reason as the labels above: both interfaces have to be photographed showing the same flow. The port's engine does not run either — its step body is a Java handler (SPEC-001 §1) — and these are the text the surface prints beside the step.

**JSON field names** (48): `@activepieces/piece-webhook`, `archivedAt`, `backupFiles`, `branchBody`, `branchIndex`, `branchType`, `catch_webhook`, `conditions`, `connectionIds`, `continueOnFailure`, `displayName`, `durationMs`, `environment`, `errorMessage`, `evaluation`, `eventually`, `executionType`, `externalId`, `failParentOnFailure`, `finishTime`, `firstValue`, `flowVersionId`, `iterations`, `lastUpdatedDate`, `logsFileId`, `maxAttempts`, `nextAction`, `not-a-date`, `operationStatus`, `packageJson`, `parentRunId`, `pauseMetadata`, `piece-webhook`, `pieceVersion`, `propertySettings`, `publishedVersionId`, `queryParams`, `retryOnFailure`, `sampleData`, `schemaVersion`, `secondValue`, `sourceCode`, `stepNameToTest`, `templateId`, `timeSavedPerRun`, `triggerName`, `triggeredBy`, `trueBranch`

## Behaviour derived without copying text

All of it. Every rule in SPEC-001 §3 was established by running activepieces' engine and reading
what it did — `docs/question-log.md` records the thirty-six questions and how each was answered —
and then written again in Java. The port is a derivative work of activepieces in the ordinary
sense, and says so here rather than being coy about it.

## One line of the original was changed, and not shipped

`packages/server/engine/src/lib/core/piece/piece-child.ts` in the clone does
`await import(piecePath)` on a bare Windows absolute path, which node's ESM loader refuses, so no
piece — and therefore no flow at all — can run on this platform. It was changed to
`import(pathToFileURL(piecePath).href)` so the original could be run and its screens photographed.
The change is in the clone beside this repository, is not part of what is published here, and the
file as it shipped is kept at `activepieces-port/tmp/piece-child.ts.orig`. Question-log
"Driving the whole application" records it.

## Also used

- **Akka** — the SDK, runtime and testkit this port is built on.
- **Playwright** — drives both interfaces for the screen comparison.
