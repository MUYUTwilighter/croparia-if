# Testing Guide

This project currently supports three testing layers:

1. JVM unit tests in `common`
2. Fabric loader-backed tests in `fabric` (dependency scaffold only)
3. NeoForge GameTest samples in `neoforge`

## 1) JVM Unit Tests (common)

Put tests under:

- `common/src/test/java`

Run:

```powershell
.\gradlew.bat :common:test
```

Example test:

- `common/src/test/java/cool/muyucloud/croparia/util/CifUtilTest.java`

## 2) Fabric Loader JUnit (fabric)

The module includes:

- `testImplementation "net.fabricmc:fabric-loader-junit:${fabric_loader_version}"`

Use this when tests need a Fabric Loader runtime.

Note:

- Fabric loader JUnit will bootstrap Fabric/Mixin. If test runtime classpath is incomplete (for example missing mixin config resources), tests can fail before any assertion runs.
- This project adds a test-only `croparia-if-common.mixins.json` override under `fabric/src/test/resources` so loader-backed tests can run without loading production mixins.

## 3) NeoForge GameTest (integration)

Sample class:

- `neoforge/src/main/java/cool/muyucloud/croparia/neoforge/gametest/CropariaNeoForgeGameTests.java`

Current sample:

- `sanity` test with `@GameTest(template = "minecraft:empty")`

To expand:

1. Add more `@GameTest` methods
2. Add/choose proper structure templates if required by your scenarios
3. Launch dev server (for example `:neoforge:runServer`) and run tests with `/test runall croparia`

## Full Validation

Run all module tests:

```powershell
.\gradlew.bat test
```
