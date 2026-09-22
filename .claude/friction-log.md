# Friction Log

Record of conflicts between installed skills, CLAUDE.md rules, and project
decisions. Each entry describes the friction, quotes the conflicting sources,
records the decision trail, and states the resolution or pending action.

Format:
```
## FL-NNN: <short title>
**Skill:** <skill name>
**Conflict:** <what the skill says> vs <what the project does>
**Decision trail:** <what was proposed, what was tried, what was decided>
**Resolution:** <accepted | pending skill update | override>
**Open risks:** <known risks from the decision>
**Action:** <what to do>
```

---

## FL-001: Feature-to-feature dependency rule vs per-feature vertical slicing

**Skill:** modularization

**Conflict:**

The modularization skill states:

> *"Features never depend on each other. Two features that need to communicate
> do so through a navigator interface or a shared contract type in :domain or
> :core:model, assembled in the app module. A feature importing another feature
> is always wrong."*

The skill assumes a single global `:domain` and `:data` module. ADR-001 uses
per-feature vertical slicing (`feature/X/{domain,data,ui}` as separate Gradle
modules), which creates a question the skill does not address: when
`feature:saved` needs to display event data, what does it depend on?

**Decision trail:**

1. The initial ADR-001 draft proposed relaxing the rule: "A feature's `:domain`
   is its public API. Other features may depend on it." Under this model,
   `feature:saved:ui` would depend on `feature:events:domain` to access the
   `Event` model. `feature:events:ui` would depend on `feature:saved:domain`
   for the save-button state. No domain-to-domain dependency, so no cycle.
   The dependency was `:ui` -> another feature's `:domain`, a strict downward
   edge.

2. This was rejected. The rule "features never depend on each other" was
   kept without exception, on the grounds that the modularization skill's
   blanket prohibition is correct and should be followed.

3. Working the Saved-displays-Events case through without cross-feature
   dependencies led to the contract-type-plus-callback pattern:
   - `core/model` holds `EventSummary`, a lightweight data class with the
     fields saved needs (name, date, venue, image URL, `EventId`).
   - `feature:events:ui` fires an `onSaveEvent: (EventSummary) -> Unit`
     callback wired in `:shared`.
   - `:shared` routes it to `feature:saved:domain`'s repository.
   - `feature:saved:data` stores denormalized `EventSummary` fields in its
     own Room database.
   - For read-back (is this event saved?), `:shared` exposes
     `isEventSaved: (EventId) -> Flow<Boolean>` to `feature:events:ui`.
   - Neither feature imports the other.

4. This pattern preserves the skill's rule exactly. The cost is denormalized
   data (saved events can go stale) and a new type in `core/model`.

**Resolution:** Skill rule kept. ADR-001 adopted the contract-type-plus-callback
pattern. The skill's examples and diagrams only illustrate the global-module
layout and do not show how the rule works with per-feature vertical slicing.
A skill update should add that illustration.

**Open risks:**

- **`core/model` growth.** Every cross-feature data flow adds a contract type
  to `core/model`. All feature modules depend on `core/model`, so a change to
  any contract type recompiles every feature. At three features with one
  contract type this is trivial. At ten-fifteen features the breaking-point
  signals are: `core/model` changing more than once per feature cycle, or a
  contract type's fields diverging by consumer. ADR-001 documents the likely
  next step (per-feature `:api` module) and why it is not justified now.

- **`:shared` callback sprawl.** `:shared` wires all cross-feature callbacks.
  At three features this is 5-8 callbacks. At fifteen this is a guess: 20-40.
  The breaking-point signal is `:shared` containing branching logic for
  cross-feature actions rather than mechanical routing. ADR-001 documents this.

- **Denormalized staleness.** Saved events store a snapshot at save time with
  no reconciliation in v1. The user sees stale data on the saved list until
  they drill into event detail (which fetches live). ADR-001 accepts this
  explicitly.

**Action:** Update the modularization skill to add a section showing the
per-feature vertical slice layout and how the "features never depend on each
other" rule applies to it via contract types in `core/model` and callback
wiring in the app/shared module. **Do not edit the skill until explicitly
instructed.**
