# JUnit 覆盖可行性盘点（2026-03-06）

## 目标

识别项目里适合用 JUnit 覆盖的文件，并区分：

1. 可直接写 JUnit（无需改造）
2. 需要小程度解耦后可写 JUnit
3. 更适合集成测试（Fabric Loader / NeoForge GameTest）

## 当前测试现状（快速统计）

- `common/src/main/java`: 224 个 Java 文件
- `common/src/test/java`: 23 个测试文件
- 已覆盖重点：`util`、`api/codec`、`api/json`、`api/placeholder`、`api/repo`（部分）、`api/recipe/structure`（部分）
- 缺口集中：`api/generator`、`config`、`api/crop`、`api/core/recipe`、平台桥接层

## 进度更新（2026-03-08）

- 已新增 `common/src/test/java/cool/muyucloud/croparia/api/generator/DataGeneratorFlowTest.java`：
  覆盖 `DataGenerator` 的 `enabled/startup/whitelist` 生成分支，
  以及 `AggregatedGenerator`、`LangGenerator` 的聚合输出流程与异常分支。
- 已新增 `common/src/test/java/cool/muyucloud/croparia/api/core/recipe/container/RecipeContainerTest.java`：
  覆盖 `InfusorContainer`、`SoakContainer`、`RitualContainer` 的 `isEmpty/getItem/size` 核心行为。
- 已纳入并保留 `common/src/test/java/cool/muyucloud/croparia/util/DependenciesTest.java`。
- 将运行时依赖较强的新增测试迁移到 Fabric loader-backed 层：
  - `fabric/src/test/java/cool/muyucloud/croparia/api/generator/DataGeneratorFlowFabricTest.java`
  - `fabric/src/test/java/cool/muyucloud/croparia/api/core/recipe/container/RecipeContainerFabricTest.java`
- 在 NeoForge JUnit 层新增镜像测试（以运行时可用性为前置）：
  - `neoforge/src/test/java/cool/muyucloud/croparia/api/generator/DataGeneratorFlowNeoForgeTest.java`
  - `neoforge/src/test/java/cool/muyucloud/croparia/api/core/recipe/container/RecipeContainerNeoForgeTest.java`
- 为了让 NeoForge 测试可编译，补充了 `neoforge` 对 `:common` 的测试编译依赖：
  - `neoforge/build.gradle` 增加 `testImplementation(project(path: ':common', configuration: 'namedElements'))`
- 在迁移过程中修复生成器并发修改缺陷：
  - `LangGenerator.onGenerated` 改为对缓存条目快照遍历
  - `AggregatedGenerator.onGenerated` 改为对缓存条目快照遍历
- 新增 `common` 侧第二批覆盖：
  - `common/src/test/java/cool/muyucloud/croparia/api/crop/util/CropDependenciesTest.java`
  - `common/src/test/java/cool/muyucloud/croparia/config/ConfigTest.java`（覆盖非平台分支）
- 为支持 `ConfigTest` 的纯 JVM 场景，修复了 `Config(RawConfig)` 的提前平台调用：
  - `orElse(...)` 调整为 `orElseGet(...)`，避免在已有绝对路径时仍触发 `Platform.getGameFolder()`
- 新增 `common/src/test/java/cool/muyucloud/croparia/config/ConfigFileHandlerTest.java`：
  覆盖 `ConfigFileHandler` 的 `load/save/reload` 主分支（含“配置文件缺失时自动创建默认配置”）。
- 新增 `common/src/test/java/cool/muyucloud/croparia/api/crop/CropRegistryTest.java`：
  覆盖 `CropRegistry` 的 `register/readCrops/dumpCrop` 主流程与 `onRegister` 触发语义。
  已补充异常与边界分支：目录缺失自动创建、损坏 JSON 跳过、`dumpCrops` 批量输出。
- 新增 `common/src/test/java/cool/muyucloud/croparia/api/generator/pack/PackHandlerFlowTest.java`：
  覆盖 `PackHandler.onTriggered` 生命周期顺序、`override` 清理分支与缓存清理语义。
- 为支持 `ConfigFileHandler` 在纯 JVM 场景下测试，做了最小解耦：
  - `ConfigFileHandler` 移除对 `CropariaIf.LOGGER` 的静态依赖，改为本地 logger；
  - 配置路径改为惰性解析；
  - 增加 package-private 的 game folder supplier 测试注入点。
- 进一步增强 `Config.resolvePath` 的稳健性：当平台目录不可用时回退为绝对路径字符串。
- 进一步增强 `ConfigFileHandler` 的稳健性：当平台目录不可用时回退到临时目录路径，避免静态初始化时崩溃。
- 进一步增强 `CropRegistry.readCrop` 的健壮性：捕获解析期运行时异常，避免单个坏配置中断全量读取。

## A. 可直接补 JUnit 的文件（优先）

这些类主要是字符串/集合/缓存/编解码或“纯业务流程”，可在 `common` 的 JVM 单测直接覆盖。

1. `common/src/main/java/cool/muyucloud/croparia/api/generator/util/DgReader.java`
2. `common/src/main/java/cool/muyucloud/croparia/api/generator/util/PackCache.java`
3. `common/src/main/java/cool/muyucloud/croparia/api/generator/util/DgRegistry.java`
4. `common/src/main/java/cool/muyucloud/croparia/api/generator/AggregatedGenerator.java`
5. `common/src/main/java/cool/muyucloud/croparia/api/generator/LangGenerator.java`
6. `common/src/main/java/cool/muyucloud/croparia/api/generator/DataGenerator.java`
7. `common/src/main/java/cool/muyucloud/croparia/api/core/recipe/container/InfusorContainer.java`
8. `common/src/main/java/cool/muyucloud/croparia/api/core/recipe/container/SoakContainer.java`
9. `common/src/main/java/cool/muyucloud/croparia/api/core/recipe/container/RitualContainer.java`

建议断言点：

- `DgReader`: CDG 语法、引号/转义、三引号、多段 `@meta`、报错上下文
- `PackCache`: `cache/query/occupy/getAll` 的所有权转移语义
- `DgRegistry`: `register/ofEnum/ofMap/forName` 行为
- `DataGenerator` 系列：`enabled/startup/whitelist` 分支、聚合输出拼接逻辑
- 三个 `container`: `isEmpty/getItem(size 越界)` 与迭代语义

## B. 需要“小程度解耦”后可测（高收益）

这些类逻辑本身可测，但目前直接依赖静态平台 API、文件系统或全局状态。建议先做小改造，再写 JUnit。

1. `common/src/main/java/cool/muyucloud/croparia/config/Config.java`
2. `common/src/main/java/cool/muyucloud/croparia/config/ConfigFileHandler.java`
3. `common/src/main/java/cool/muyucloud/croparia/util/Dependencies.java`
4. `common/src/main/java/cool/muyucloud/croparia/api/crop/util/CropDependencies.java`
5. `common/src/main/java/cool/muyucloud/croparia/api/crop/CropRegistry.java`
6. `common/src/main/java/cool/muyucloud/croparia/api/generator/pack/PackHandler.java`

建议的最小解耦切口：

- 把 `Platform.getGameFolder()`、`Platform.isModLoaded()` 提取为可注入函数/接口
- 把文件读写与目录遍历（`FileUtil.*`）提成 `FileOps` 适配层
- 把日志输出和“全局注册表/全局 map”访问封装在可替换协作者中
- 保持对外 API 不变，仅新增可测试构造器或 package-private setter

这样可以覆盖的关键行为：

- `Config`: 路径归一化、黑名单解析、正则匹配
- `ConfigFileHandler`: load/save/reload 分支
- `Dependencies/CropDependencies`: 多候选可用性判定
- `CropRegistry`: `register/read/dump` 的加载与覆盖行为
- `PackHandler`: `onTriggered` 流程与缓存生命周期

## C. 更适合集成测试（不建议先用纯 JUnit）

这类文件强依赖 Minecraft 世界状态、注册表、Block/Entity Tick 或 Mixin 注入点。优先用 Fabric Loader 测试或 NeoForge GameTest。

1. `common/src/main/java/cool/muyucloud/croparia/api/core/block/**`
2. `common/src/main/java/cool/muyucloud/croparia/api/core/block/entity/**`
3. `common/src/main/java/cool/muyucloud/croparia/api/core/item/**`
4. `common/src/main/java/cool/muyucloud/croparia/mixin/**`
5. `common/src/main/java/cool/muyucloud/croparia/registry/**`
6. `fabric/src/main/java/**` 与 `neoforge/src/main/java/**` 的平台入口初始化类

备注：`api/core/recipe/*.java`（如 `InfusorRecipe`/`SoakRecipe`/`RitualRecipe`）可先做“窄 JUnit”验证纯匹配逻辑，但涉及 `Items/Component/Registry` 的分支建议放入 loader-backed 测试。

## 分阶段补测建议

1. 第一批（低成本高回报）  
   `DgReader`、`PackCache`、`DgRegistry`、三个 `container`、`DataGenerator/AggregatedGenerator/LangGenerator`
2. 第二批（先做轻解耦）  
   `Config`、`ConfigFileHandler`、`Dependencies`、`CropDependencies`、`CropRegistry`、`PackHandler`
3. 第三批（集成层）  
   核心方块/物品/配方世界交互、平台桥接、Mixin 回归

## 建议新增测试文件（示例）

1. `common/src/test/java/cool/muyucloud/croparia/api/generator/util/DgReaderTest.java`
2. `common/src/test/java/cool/muyucloud/croparia/api/generator/util/PackCacheTest.java`
3. `common/src/test/java/cool/muyucloud/croparia/api/generator/util/DgRegistryTest.java`
4. `common/src/test/java/cool/muyucloud/croparia/api/generator/DataGeneratorFlowTest.java`
5. `common/src/test/java/cool/muyucloud/croparia/api/core/recipe/container/RecipeContainerTest.java`
6. `common/src/test/java/cool/muyucloud/croparia/config/ConfigTest.java`（解耦后）
7. `common/src/test/java/cool/muyucloud/croparia/api/crop/CropRegistryTest.java`（解耦后）
