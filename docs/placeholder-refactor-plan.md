# Placeholder Refactor Plan

## Goals

1. Reduce mental overhead of parser flow
2. Improve error diagnosability
3. Preserve current placeholder compatibility

## Current Pain Points

1. `Placeholder` static initialization loads many heavy built-ins
2. `PlaceholderBuilder` is feature-rich but has dense branching and repeated patterns
3. Error messages lacked enough context for fast debugging

## Phased Plan

### Phase 1: Stabilize and Observe

1. Keep behavior intact, add tests for template/list/map and json transformer paths
2. Improve error context in exceptions and logs
3. Fix obvious semantic bugs discovered by tests (`LIST_GET_OR` default fallback)

### Phase 2: Decouple Built-ins

1. Move static built-in placeholders (`ITEM_OUTPUT`, `BLOCK_OUTPUT`, etc.) into lazy holders
2. Keep `Placeholder` core type lightweight and bootstrap-safe
3. Split "pure parsing" and "Minecraft-bound placeholder library" into separate entrypoints

### Phase 3: Simplify Builder Internals

1. Extract repeated `then(...mapper...)` branches into private helpers
2. Replace nested ad-hoc map/list transformation blocks with small composable methods
3. Add explicit trace hooks (optional debug mode) for placeholder traversal steps

### Phase 4: API Hardening

1. Introduce a small compatibility test matrix for commonly used placeholder forms
2. Freeze documented behavior for:
   - missing key
   - default value
   - escaping
   - quote helpers
3. Mark risky extension points with clear javadoc examples

## Immediate Next Tasks

1. Add lazy initialization wrapper for Minecraft-bound built-ins in `Placeholder`
2. Add traversal trace helper (`PlaceholderTrace`) behind debug flag
3. Add regression tests for `mapKey/mapValue/get/getOr/mapi` combinations
