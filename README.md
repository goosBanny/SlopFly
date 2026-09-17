# TempFly (Modern Revamp Fork)

A modernized, high-performance, and hardened fork of **TempFly** for modern Paper and Spigot Minecraft servers. This revamped version transforms the classic temporary flight plugin into an enterprise-ready, lag-free solution built for modern server infrastructure and modern Minecraft versions (1.20+ through 1.21+).

---

## Key Highlights & Modern Overhauls

### 1. High-Performance Architecture
* **Paper-Native Asynchronous Execution:** Database round-trips and file I/O operations are offloaded to dedicated worker pools, completely freeing the primary server tick thread from blocking queries.
* **Ticking & State Decoupling:** Flight time calculation, idle detection, and region relative multipliers have been extracted into a pure, in-memory domain flight engine separated from Bukkit entity instances, eliminating main-thread overhead during server sweeps.
* **Per-Second Action Bar Caching:** Action bar rendering and formatting now operate on an intelligent per-second text cache, eliminating repeated string regex replacements and color parsers on every tick.
* **Particle Trail Optimization:** Particle trail lookups leverage static enum caches and optimized vector emission, avoiding recurrent reflection, redundant enum scans, and memory leaks.

### 2. Modern Formatting & MiniMessage Integration
* **Universal MiniMessage Support:** Modern rich tags (`<gradient>`, `<rainbow>`, `<hover>`, `<click>`) are fully supported across all messages, broadcasts, action bars, and titles.
* **Injection-Proof Message Pipeline:** All player inputs and user placeholders are sanitized through unparsed placeholder bindings, preventing malicious tag injection, formatting hijacking, and client-side exploits.
* **Pre-Compiled Template Caching:** Frequently broadcasted messages and templates are retained in a bounded LRU memory cache, eliminating overhead from repeated syntax builds.
* **Seamless Legacy Backward Compatibility:** Servers can mix and match legacy color codes (`&`, `§`), modern hex codes (`&#RRGGBB`, `§x§r§r§g§g§b§b`), and MiniMessage tags seamlessly.

### 3. Enterprise Persistence & Thread-Safe Repositories
* **Consolidated Single-Query User Loading:** User login data fetching has been optimized from six separate synchronous queries down to a single asynchronous repository fetch on player handshake.
* **Atomic SQL Balance Operations:** Offline player time transactions utilize atomic SQL updates with condition clauses, eliminating race conditions, balance overwrites, and desynchronizations during network-wide time deliveries.
* **JDBC Batch Saving:** Bulk data persists via batched SQL statements, dramatically reducing I/O friction during auto-saves and server shutdowns.
* **Dual Storage Support:** Native support for both MySQL / MariaDB networks and fast local SQLite instances, alongside thread-safe YAML flatfile fallback.

### 4. Resilient Fall Safety Engine
* **Universal Fall Protection Service:** Negates fall damage gracefully after flight deactivation (via command, region exit, combat entry, or timer expiration) using a dedicated, ground-contact event safety tracker.
* **Auto-Expiring Cleanups:** Scheduled tick safety tasks automatically purge lingering tracking states when players land, teleport, switch worlds, or disconnect, preventing memory leaks and invulnerability exploits.

### 5. Composite Command Engine & Brigadier Autocompletion
* **Zero-Offset Command Routing:** The command system has been overhauled using a clean composite architecture that natively handles zero-offset argument slicing and permission gatekeeping.
* **Dynamic Autocompletion:** Tab completion intelligently queries registered subcommands, online players, and numeric parameters without hardcoded index branches.
* **Native Console Support:** Administrative commands cleanly differentiate between console senders and player sessions, providing safe, descriptive CLI feedback.

### 6. Deprecations & Streamlining
* **Removed Legacy Shop GUI System:** The unused, legacy built-in GUI shop has been completely stripped out to reduce codebase bloat, prevent memory retention in inventory sessions, and delegate economy purchases to dedicated server shop or menu plugins where they belong.
* **Refined Infinite Flight & Bypass:** Fully resolved infinite time formatting across action bars, tab lists, name tags, Clip PlaceholderAPI, and MvdW PlaceholderAPI, cleanly rendering configured infinity symbols instead of broken numeric counters.

---

## Core Features & Capabilities

* **Temporary Timed Flight:** Grant, deduct, set, and transfer flight time measured accurately down to fractions of a second.
* **Infinite Flight & Bypass Modes:** Server administrators and VIP ranks can toggle infinite flight time and requirement bypasses without losing their underlying stored balance.
* **Combat & Damage Hooks:** Automatically drop players from flight upon entering PvP combat or taking damage, complete with configurable combat timers and fall safety.
* **Region & World Flight Environments:** Restrict flight or alter time decay speeds per WorldGuard region, territory claim, or world.
* **Speed Management & Limits:** Granular player flight speeds customizable per world, region, or rank permissions with automatic speed-limit correction.
* **Daily Play Bonuses & Time Decay:** Reward daily logins with flight time bonuses and clean up stale balances of long-inactive players through configurable time decay.
* **Cosmetic Particle Trails:** Extensive particle trails for flying players with full vanish/spectator support and permission controls.

---

## Commands & Permissions

| Subcommand | Alias | Permission | Description |
| :--- | :--- | :--- | :--- |
| `/tf fly [player]` | `/tf toggle` | `tempfly.toggle.self`, `tempfly.toggle.other` | Toggle flight mode on or off |
| `/tf time [player]` | | `tempfly.time.self`, `tempfly.time.other` | View current flight time and status |
| `/tf give <player> <time>` | | `tempfly.give` | Grant flight time to a player |
| `/tf remove <player> <time>` | | `tempfly.remove` | Deduct flight time from a player |
| `/tf set <player> <time>` | | `tempfly.set` | Set a player's flight time |
| `/tf giveall <time>` | | `tempfly.giveall` | Grant flight time to all online players |
| `/tf pay <player> <time>` | | `tempfly.pay` | Pay flight time from your balance to another player |
| `/tf speed <value> [player]` | `/tf sp` | `tempfly.speed.self`, `tempfly.speed.other` | Adjust your personal flight speed |
| `/tf infinite [player]` | `/tf inf` | `tempfly.infinite.toggle` | Toggle infinite flight time |
| `/tf bypass [player]` | `/tf bp` | `tempfly.bypass.toggle` | Toggle flight requirement bypass |
| `/tf trails` | | `tempfly.trails` | Open the cosmetic particle trails menu |
| `/tf reload` | `/tf rl` | `tempfly.reload` | Reload all configurations, hooks, and aesthetics |

---

## External Integrations

* **Placeholders:** Full integration with PlaceholderAPI (PAPI) and MVdWPlaceholderAPI.
* **Economy:** Vault integration for economy balance support and flight time gifting.
* **Territory & Protection:** WorldGuard, Towny, Factions, Lands, ASkyBlock, BentoBox, and GriefPrevention hooks for region-based flight restrictions and relative time consumption.

---

## Building from Source

This project requires **Java 21+** and **Maven 3.8+** to build:

```bash
mvn clean package
```

The compiled JAR file will be located in `target/TempFly-3.1.7.jar`.
