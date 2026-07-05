# Graph Report - .  (2026-07-05)

## Corpus Check
- Corpus is ~20,156 words - fits in a single context window. You may not need a graph.

## Summary
- 283 nodes · 352 edges · 37 communities (25 shown, 12 thin omitted)
- Extraction: 93% EXTRACTED · 7% INFERRED · 0% AMBIGUOUS · INFERRED: 24 edges (avg confidence: 0.81)
- Token cost: 0 input · 128,516 output

## Community Hubs (Navigation)
- [[_COMMUNITY_NFC Controller & State|NFC Controller & State]]
- [[_COMMUNITY_App Shell & Navigation|App Shell & Navigation]]
- [[_COMMUNITY_Graphify Extraction Pipeline|Graphify Extraction Pipeline]]
- [[_COMMUNITY_NDEF Write Path|NDEF Write Path]]
- [[_COMMUNITY_NDEF Parsing & Model|NDEF Parsing & Model]]
- [[_COMMUNITY_Read Screen UI|Read Screen UI]]
- [[_COMMUNITY_v1 Plan & Locked Decisions|v1 Plan & Locked Decisions]]
- [[_COMMUNITY_Write ViewModel|Write ViewModel]]
- [[_COMMUNITY_CIRelease Pipeline|CI/Release Pipeline]]
- [[_COMMUNITY_Tag Type Detection|Tag Type Detection]]
- [[_COMMUNITY_Tag Type Detector Tests|Tag Type Detector Tests]]
- [[_COMMUNITY_Ultralight Dump Reader|Ultralight Dump Reader]]
- [[_COMMUNITY_Write Screen UI|Write Screen UI]]
- [[_COMMUNITY_Graphify Maintenance & Rules|Graphify Maintenance & Rules]]
- [[_COMMUNITY_Read ViewModel|Read ViewModel]]
- [[_COMMUNITY_Tag Reader|Tag Reader]]
- [[_COMMUNITY_NDEF Parser Tests|NDEF Parser Tests]]
- [[_COMMUNITY_Navigation Routes|Navigation Routes]]
- [[_COMMUNITY_Graphify Query Command|Graphify Query Command]]
- [[_COMMUNITY_Application Entry Point|Application Entry Point]]
- [[_COMMUNITY_Ultralight Dump Reader Tests|Ultralight Dump Reader Tests]]
- [[_COMMUNITY_Graphify GitHub Clone & Merge|Graphify GitHub Clone & Merge]]
- [[_COMMUNITY_Tag Type Display|Tag Type Display]]
- [[_COMMUNITY_Testing & Build Order Notes|Testing & Build Order Notes]]
- [[_COMMUNITY_Graphify Explain Command|Graphify Explain Command]]
- [[_COMMUNITY_Graphify Path Command|Graphify Path Command]]
- [[_COMMUNITY_Graphify Video Transcription|Graphify Video Transcription]]
- [[_COMMUNITY_ADB Debug Workflow|ADB Debug Workflow]]
- [[_COMMUNITY_Graphify Fast-Path Shortcut|Graphify Fast-Path Shortcut]]
- [[_COMMUNITY_Graphify Install Check|Graphify Install Check]]
- [[_COMMUNITY_AGP9 Build Gotchas|AGP9 Build Gotchas]]

## God Nodes (most connected - your core abstractions)
1. `NfcController` - 16 edges
2. `WriteViewModel` - 11 edges
3. `TagTypeDetectorTest` - 11 edges
4. `WriteResult` - 9 edges
5. `Graphify Pipeline (/graphify)` - 9 edges
6. `ReadViewModel` - 8 edges
7. `Step 6: Obsidian Vault + HTML Export` - 8 edges
8. `Architecture Summary` - 8 edges
9. `TagType` - 7 edges
10. `AppRoot()` - 7 edges

## Surprising Connections (you probably didn't know these)
- `Legacy Node.js Release Workflow Template` --semantically_similar_to--> `CI publish job (tag + GitHub Release)`  [INFERRED] [semantically similar]
  references/gh-release.yml → .github/workflows/release.yml
- `CI / Releases (release.yml summary)` --shares_data_with--> `Android SDK Runner Install`  [INFERRED]
  CLAUDE.md → docs/self-hosted-runner-setup.md
- `CI / Releases (release.yml summary)` --shares_data_with--> `Runner Service .env Wiring`  [INFERRED]
  CLAUDE.md → docs/self-hosted-runner-setup.md
- `graphify Usage Rules (root CLAUDE.md)` --references--> `Graphify Pipeline (/graphify)`  [EXTRACTED]
  CLAUDE.md → .claude/skills/graphify/SKILL.md
- `CI / Releases (release.yml summary)` --references--> `CI test job (unit tests + lint)`  [EXTRACTED]
  CLAUDE.md → .github/workflows/release.yml

## Import Cycles
- None detected.

## Hyperedges (group relationships)
- **Graphify Extraction Pipeline (Detect -> AST -> Semantic -> Merge -> Build)** — _claude_skills_graphify_skill_step2_detect_files, _claude_skills_graphify_skill_part_a_ast, _claude_skills_graphify_skill_part_b_semantic, _claude_skills_graphify_skill_part_c_merge, _claude_skills_graphify_skill_step4_build_graph [EXTRACTED 1.00]
- **NFC Core Architecture Components (v1 Plan)** — planning_v1_plan_nfccontroller, planning_v1_plan_tagreader, planning_v1_plan_ndefparser, planning_v1_plan_tagtypedetector, planning_v1_plan_ndeftextwriter, planning_v1_plan_ultralightdumpreader, planning_v1_plan_mainactivity [EXTRACTED 1.00]
- **Release CI/CD Pipeline (test -> build -> publish)** — _github_workflows_release_test_job, _github_workflows_release_build_job, _github_workflows_release_publish_job [EXTRACTED 1.00]

## Communities (37 total, 12 thin omitted)

### Community 0 - "NFC Controller & State"
Cohesion: 0.14
Nodes (15): Activity, Idle, StateFlow, String, Tag, NfcAdapterState, NfcController, NfcEvent (+7 more)

### Community 1 - "App Shell & Navigation"
Cohesion: 0.10
Nodes (12): MainActivity, ActionCard(), HomeScreen(), String, AppBottomBar(), NavKey, AppRoot(), SettingsScreen() (+4 more)

### Community 2 - "Graphify Extraction Pipeline"
Cohesion: 0.11
Nodes (19): Token Reduction Benchmark, FalkorDB Export, MCP Server (graphify.serve), Neo4j Export, SVG / GraphML Export, Wiki Export (--wiki), Confidence Score Rubric, Node ID Format Convention (+11 more)

### Community 3 - "NDEF Write Path"
Cohesion: 0.18
Nodes (14): Error, ReadOnly, Success, TagLost, TooLarge, WriteResult, buildTextMessage(), NdefMessage (+6 more)

### Community 4 - "NDEF Parsing & Model"
Cohesion: 0.16
Nodes (10): NdefRecordKind, NdefRecordModel, decodeTextPayload(), decodeUriPayload(), ByteArray, List, NdefMessage, String (+2 more)

### Community 5 - "Read Screen UI"
Cohesion: 0.19
Nodes (13): TagInfo, HexDumpSection(), List, Modifier, String, ErrorView(), Modifier, String (+5 more)

### Community 6 - "v1 Plan & Locked Decisions"
Cohesion: 0.20
Nodes (15): Architecture Summary, Locked Decisions (do not re-litigate), Decisions Locked with the User, MainActivity, NdefParser, NdefTextWriter, NFC Reader-Mode Architecture, NfcController (+7 more)

### Community 7 - "Write ViewModel"
Cohesion: 0.24
Nodes (8): Editing, Failure, StateFlow, String, Success, WaitingForTag, WriteUiState, WriteViewModel

### Community 8 - "CI/Release Pipeline"
Cohesion: 0.20
Nodes (12): CI build job (debug + unsigned-release APKs), Promote Existing Pre-release on Main Merge, CI publish job (tag + GitHub Release), CI test job (unit tests + lint), CI / Releases (release.yml summary), Development Environment Constraints, Dropbox Crash Log Diagnosis, Unsigned Release APK Build + Debug-Signing Workaround (+4 more)

### Community 9 - "Tag Type Detection"
Cohesion: 0.30
Nodes (8): TagType, classifyGetVersionResponse(), classifyLegacyType(), ByteArray, Int, Tag, TagTypeDetector, totalPages()

### Community 11 - "Ultralight Dump Reader"
Cohesion: 0.29
Nodes (8): formatPage(), ByteArray, Int, List, String, Tag, UltralightDumpReader, MifareUltralight

### Community 12 - "Write Screen UI"
Cohesion: 0.29
Nodes (9): Modifier, NfcStatusBanner(), EditingContent(), Boolean, Modifier, String, ResultContent(), WaitingForTagContent() (+1 more)

### Community 13 - "Graphify Maintenance & Rules"
Cohesion: 0.22
Nodes (10): /graphify add <url>, --watch (folder watcher), graphify claude install (CLAUDE.md integration), graphify hook install (post-commit auto-rebuild), --cluster-only, Incremental --update, Honesty Rules, Graphify Pipeline (/graphify) (+2 more)

### Community 14 - "Read ViewModel"
Cohesion: 0.29
Nodes (7): Error, StateFlow, ReadUiState, ReadViewModel, Scanning, Success, ViewModel

### Community 15 - "Tag Reader"
Cohesion: 0.32
Nodes (5): Error, ScanResult, Success, Tag, TagReader

### Community 17 - "Navigation Routes"
Cohesion: 0.53
Nodes (5): HomeRoute, ReadRoute, SettingsRoute, WriteRoute, NavKey

### Community 18 - "Graphify Query Command"
Cohesion: 0.50
Nodes (4): save-result Feedback Loop, BFS/DFS Graph Traversal, Constrained Query Vocabulary Expansion, /graphify query

### Community 21 - "Graphify GitHub Clone & Merge"
Cohesion: 0.67
Nodes (3): graphify clone, graphify merge-graphs, Step 0: GitHub URL / Multi-path Merge

### Community 23 - "Testing & Build Order Notes"
Cohesion: 0.67
Nodes (3): Testing Notes (Robolectric / emulator limits), Suggested Build Order, Verification Plan (build/unit-test/on-device)

## Knowledge Gaps
- **39 isolated node(s):** `NdefRecordKind`, `Fast Path for Existing Graph`, `Step 1: Ensure Graphify Installed`, `Step 2: Detect Files`, `Step 2.5: Video and Audio Transcription` (+34 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **12 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `ReadScreen()` connect `Read Screen UI` to `App Shell & Navigation`, `Write Screen UI`, `Read ViewModel`?**
  _High betweenness centrality (0.126) - this node is a cross-community bridge._
- **Why does `NdefRecordRow()` connect `Read Screen UI` to `NDEF Parsing & Model`?**
  _High betweenness centrality (0.094) - this node is a cross-community bridge._
- **Why does `NdefRecordModel` connect `NDEF Parsing & Model` to `Read Screen UI`?**
  _High betweenness centrality (0.094) - this node is a cross-community bridge._
- **What connects `NdefRecordKind`, `Graphify Always-On Trigger Instruction`, `Fast Path for Existing Graph` to the rest of the system?**
  _47 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `NFC Controller & State` be split into smaller, more focused modules?**
  _Cohesion score 0.13768115942028986 - nodes in this community are weakly interconnected._
- **Should `App Shell & Navigation` be split into smaller, more focused modules?**
  _Cohesion score 0.1 - nodes in this community are weakly interconnected._
- **Should `Graphify Extraction Pipeline` be split into smaller, more focused modules?**
  _Cohesion score 0.10526315789473684 - nodes in this community are weakly interconnected._