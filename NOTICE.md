# Notice & Upstream Attribution

This project is a modified fork of **TempFly**, originally authored and created by **ChiefMoneyBags** (https://github.com/ChiefMoneyBags/TempFly) and contributors.

## Licensing & GPLv3 Compliance

This program is free software: you can redistribute it and/or modify it under the terms of the **GNU General Public License** as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the [LICENSE](LICENSE) file or the [GNU General Public License](https://www.gnu.org/licenses/gpl-3.0.html) for more details.

## Modifications in this Fork (GPLv3 Section 5)

In accordance with Section 5 of the GNU General Public License v3, the following prominent notices outline the major modifications made to the original work:

1. **Java 21 & Paper Target Alignment**: Upgraded build pipelines, compiler targets, and bytecode outputs to Java 21 and modern Paper (`1.21+`).
2. **Standard Maven Layout & Build Modernization**: Migrated the legacy repository structure to standard Maven convention (`src/main/java`, `src/main/resources`, `src/test/java`) and removed checked-in proprietary libraries (`lib/MVdWPlaceholderAPI.jar`) in favor of dynamic reflection-based safe integration.
3. **Telemetery Stripping**: Completely pruned bStats tracking and telemetry pipelines.
4. **Security Hardening**: Sanitized database credential templates, secured connection error logging, and hardened runtime artifact ignore patterns in `.gitignore`.
5. **Modern Text Formatting & MiniMessage Engine**: Upgraded message pipeline to support Adventure MiniMessage, hex colors, and parameterized tag injection protection.
6. **Persistence & Performance Overhauls**: Implemented thread-safe `UserRepository` abstractions, atomic balance modifications, batch persistence, and decoupled domain flight ticking from the primary server tick thread.
7. **Brigadier & Paper Command Lifecycle**: Replaced legacy command registration with modern Paper Brigadier lifecycle event registration and composite subcommand routers.
