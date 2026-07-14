# Decoupling Implementation Plan (Mihon to Native Client)

This document maps out the specific implementation phases for decoupling **Neo Manga v3** from the legacy Mihon decentralized Extension framework and transitioning to a native, client-server model.

---

## Implementation Phases

### Phase 1: Gradle & Dependency Integration [COMPLETED]
* **Objective**: Add required client dependencies to support Retrofit and Kotlinx Serialization parsing.
* **Steps**:
  1. Add Retrofit (`com.squareup.retrofit2:retrofit`) and its Kotlinx Serialization Converter (`com.squareup.retrofit2:converter-kotlinx-serialization`) to `gradle/libs.versions.toml`.
  2. Implement imports inside the `core:common` module (`core/common/build.gradle.kts`) so that it can serve as our network engine.
  3. Run a Gradle sync to verify configurations compile successfully.

### Phase 2: Local Developer DB Migration [COMPLETED]
* **Objective**: Align local developer testing database manga records with our static virtual source ID.
* **Steps**:
  1. Create a SQLDelight migration file `15.sqm` in `data/src/main/sqldelight/tachiyomi/migrations/`.
  2. Write standard transactional SQL updates to safely map existing local manga records matching developer test source IDs (such as the legacy MeshManga source ID or Olympus source ID) to the virtual source ID `9999L`.
  3. Ensure no local read progress records or tracking indicators are lost during migration.

### Phase 3: Dynamic Base URL Configuration [COMPLETED]
* **Objective**: Link the API Client builder with user and developer preferences helper classes.
* **Steps**:
  1. Leverage existing app preference frameworks (`PreferencesHelper`) to store the API Server Base URL.
  2. Construct `NeoMangaApiClient` dynamically: read the user-configured preference value, falling back to the production API URL.
  3. Expose developer settings in the UI to allow switching the API Base URL seamlessly between production and `localhost` (e.g. `http://10.0.2.2:8000/api/v1/`) for fast local debug cycles.

### Phase 4: Virtual Source Bridge & DI Registry [COMPLETED]
* **Objective**: Implement the mock `HttpSource` (ID `9999L`) and wire up all Injekt DI bindings.
* **Steps**:
  1. Create `NeoMangaVirtualSource` extending `HttpSource` inside `app/src/main/java/eu/kanade/tachiyomi/source/online/`.
  2. Masquerade it as a system source with a fixed unique ID `9999L`.
  3. Delegate details, chapters, and pages queries to `NeoMangaApiClient` internally, returning legacy structures like `SManga`, `List<SChapter>`, and `List<Page>` to the reader activity boundaries.
  4. Register `NeoMangaVirtualSource` statically inside the active sources registry in `AndroidSourceManager.kt`.
  5. Bind the singleton `NeoMangaApiClient` in `AppModule.kt` and the new interactors in `DomainModule.kt`.
### Phase 5: Multi-Source Convergence [COMPLETED]
* **Objective**: Reactivate "Olympus Staff" source registration alongside "MeshManga" (Team X) on the client, and implement backend chapter-merging with strict "MeshManga-over-Olympus" priority.
* **Steps**:
  1. Register `NeoMangaMasterExtension` alongside `MeshMangaExtension` inside `AndroidSourceManager.kt`.
  2. Override source name to `"Olympus Staff"` and ID to `1382165189279060276L` in `NeoMangaMasterExtension.kt` to prevent SQLite source conflicts.
  3. Align `MeshMangaExtension.kt` chapter mapping logic to use absolute URLs and API-supplied float numbers.
  4. Refactor `upsert_manga_entry` in the FastAPI backend to store multi-source details under a nested `sources` dictionary.
  5. Refactor backend details endpoint `/api/v1/manga/details` to fetch sources concurrently using `asyncio.gather` with a 5.0s timeout wrapper, merging chapters chronologically and enforcing priority overrides.
  6. Route page requests dynamically in `/api/v1/chapters/pages` based on URL domain.

---

## Core Guard Rules

> [!IMPORTANT]
> **Documentation Guard Policy**: For every future modification, feature addition, or refactoring you perform, you must immediately update both `PROJECT_STATUS.md` and `decoupling_implementation_plan.md` first to keep the ledger completely synchronous and accurate.
