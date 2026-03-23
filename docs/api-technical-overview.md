# Croparia IF 技术文档（聚焦 `common/.../api`）

## 1. 文档目标
本文件基于源码阅读总结 Croparia IF 的：

- 代码架构（模块分层、运行链路、关键抽象）
- `api` 包下各类 API 能力与用途
- 对应玩法机制（作物、灌注、浸染、仪式、自动化与工具）

阅读重点目录：`common/src/main/java/cool/muyucloud/croparia/api`

## 2. 项目整体架构

### 2.1 多端工程结构（Architectury）
- 根工程：`common` + `fabric` + `neoforge`
- `common`：主要游戏逻辑与 API 抽象（本文件关注）
- `fabric` / `neoforge`：平台实现（如 `@ExpectPlatform` 对应实现）

### 2.2 运行分层（从底到上）
1. 基础序列化层  
`api.codec` + `api.json`：`CodecUtil`、`MultiCodec`、JSON/TOML/CDG 解析。

2. 数据模型层  
`api.crop`、`api.element`、`api.resource`、`api.recipe.entry`：定义“作物/元素/资源规格/配方输入输出”。

3. 资源仓库层  
`api.repo`：统一物品/流体存取模型（`Repo`、`RepoUnit`、`RepoBatch`、`RepoProxy`）。

4. 配方与结构层  
`api.recipe` + `api.core.recipe` + `api.recipe.structure`：自定义配方类型、结构匹配、合成行为。

5. 玩法方块与物品层  
`api.core.block`、`api.core.item`、`api.crop.block/item`：把配方系统映射到世界交互。

6. 数据生成层  
`api.generator` + `api.placeholder`：模板 + 占位符驱动资源包/数据包自动生成。

## 3. 关键运行链路

### 3.1 作物定义与加载链
`CropRegistry.readCrops()` 递归读取配置目录 JSON  
-> `Codec` 解码 `Crop` / `Melon`  
-> `crop.shouldLoad()`（依赖与配置判定）  
-> `crop.onRegister()` 动态注册种子/果实/方块。

### 3.2 方块触发配方链
- `Infusor.stepOn` 读取掉落物 -> 组装 `InfusorContainer` -> `Recipes.INFUSOR.find` -> 产物输出并重置灌注器
- `RitualStand.stepOn` -> `RITUAL_STRUCTURE` 验证结构 -> `RITUAL` 消耗并产出
- `ElementalStone.randomTick` 在周边尝试 `SOAK` 配方，按概率转化方块

### 3.3 数据生成链
`PackHandler.onTriggered()`：
读取内置生成器 + 外部生成器  
-> 按 `DataGenerator` 迭代注册表条目  
-> 用 `Template + Placeholder` 渲染  
-> 缓存聚合  
-> 输出到 `data/` 或 `assets/`。

## 4. `api` 目录能力地图

### 4.1 `api.crop`（作物域模型）
- `Crop`：普通作物定义（id、材料、颜色、tier、type、翻译、依赖）
- `Melon`：瓜类作物定义（stem/attach/melon/seed/item）
- `CropRegistry`：读取、注册、导出 JSON
- `CropAccess`：统一“从物品/方块拿到作物定义”的接口

设计特点：
- 完全数据驱动，`MapCodec` 支持配置文件热读
- `CropDependencies` 按 mod 可用性与配置决定加载
- 每个作物自动派生 block/seed/fruit 注册项

### 4.2 `api.element`（元素系统）
- `Element` 枚举：`AIR / EARTH / ELEMENTAL / FIRE / WATER`（含 `EMPTY`）
- 每个元素自动绑定：流体 source/flowing、液体方块、桶、药剂、宝石
- `ElementAccess`：元素统一 codec/stream codec + 获取接口

### 4.3 `api.resource` + `api.repo`（统一资源仓库 API）

核心抽象：
- `TypedResource<R>`：带类型令牌的资源对象（如 `ItemSpec`、`FluidSpec`）
- `TypeToken<T>`：类型注册与反序列化索引
- `Repo<T>`：统一资源容器接口（consume/accept/sim/capacity/amount）
- `RepoUnit<T>`：单槽实现
- `RepoBatch<T>`：多槽组合
- `ContainerRepo`：将 Minecraft `Container` 适配为 `Repo<ItemSpec>`
- `RepoProxy` / `ProxyProvider`：桥接 Fabric/NeoForge 平台存储 API

资源规格：
- `ItemSpec`：`Item + DataComponentPatch`
- `FluidSpec`：`Fluid + DataComponentMap`

意义：
- 配方/机器逻辑可以不关心平台差异，统一操作 item/fluid。

### 4.4 `api.recipe` + `api.core.recipe`（配方体系）

通用层：
- `DisplayableRecipe<C>`：可展示配方协议（输入/输出/工作站）
- `TypedSerializer<R>`：同时充当 `RecipeType + RecipeSerializer`
- `entry` 包：`ItemInput/Output`、`BlockInput/Output`、`SlotDisplay`

核心玩法配方：
- `InfusorRecipe`：元素灌注（元素 + 物品输入 -> 物品输出）
- `SoakRecipe`：浸染（元素 + 概率 + 方块输入 -> 方块输出）
- `RitualStructure`：3D 结构校验（支持旋转/变换匹配）
- `RitualRecipe`：仪式合成（结构匹配结果 + 掉落物 + 中心方块 -> 产物）

### 4.5 `api.recipe.structure`（结构表达）
- `Char2D/Char3D/MarkedChar3D/MarkedTransformableChar3D`
- 用字符矩阵描述多方块结构，`*` 标记祭坛，`$` 标记可消耗输入位

### 4.6 `api.generator` + `api.placeholder`（数据生成引擎）

生成器类型：
- `DataGenerator`：按条目生成文件
- `LangGenerator`：聚合语言条目
- `AggregatedGenerator`：聚合任意内容片段

输出载体：
- `DataPackHandler`（输出到 `data`）
- `ResourcePackHandler`（输出到 `assets`）

模板系统：
- `Template`：`${...}` 占位符扫描与替换
- `Placeholder<T>`：类型安全占位符解析树
- 支持嵌套字段、列表、映射、codec 映射、扩展占位符

### 4.7 `api.core.*`（游戏实现 API）
- `core.block`：`Infusor`、`RitualStand`、`ElementalStone`、`Greenhouse`、`CropTransmuter`（作物果实 -> 材料选择输出）
- `core.block.entity`：温室库存与自动收获逻辑
- `core.item`：`RecipeWizard`、温室方块物品
- `core.item.relic`：遗物道具逻辑
- `core.command`：作物创建/导出、生成器管理、配置命令
- `core.util`：`DropsCache`、`RecipeWizardGenerator`

### 4.8 其他支撑包
- `api.network`：统一包类型定义与收发
- `api.codec`：多路 codec、字段兼容、stream codec 映射
- `api.json`：支持 JSON / TOML / CDG 的统一解析入口

## 5. 玩法系统（从源码还原）

### 5.1 作物玩法
- 玩家通过 Croparia 等阶物品 + 手持材料，可命令生成新作物定义
- 作物成熟后产出 fruit；fruit 可还原材料（受 `fruitUse` 配置控制）
- `CropDependencies` 支持跨模组条件加载，便于“装了某模组才启用对应作物”

### 5.2 Infusor（元素灌注）
- 对灌注器使用元素药剂可“注入元素”
- 将物品丢到灌注器上触发配方匹配并合成
- 使用空瓶可“退灌注”取回药剂

### 5.3 Soak（元素浸染）
- 元素石上方放置已灌注元素的 Infusor
- 元素石随机 tick 时对周围方块做多次浸染尝试（`soakAttempts`）
- 按 `SoakRecipe` 概率将目标方块转为输出方块

### 5.4 Ritual（仪式）
- 先由 `RitualStructure` 校验多方块结构
- 在祭坛附近投掷物品触发 `RitualRecipe`
- 成功后消耗结构中的 `$` 输入位方块与物品，产出结果
- 特殊处理：附魔书叠加、刷怪蛋批量使用

### 5.5 Greenhouse（自动温室）
- 放在作物上方可自动加速生长并收获成熟作物
- 收获结果进入方块实体库存（9 格）
- 通过 `RepoProxy<ItemSpec>` 可与外部物流系统对接

### 5.6 Recipe Wizard（配方向导）
- 面向作者/开发者的“点方块生成模板文件”工具
- 支持默认 + 扩展占位符（主手/副手/目标方块/灌注元素/仪式输入等）
- 可用于快速产出配方 JSON 模板，降低数据包编写成本

### 5.7 遗物玩法（relic）
- `InfiniteApple`：无限食用 + 冷却
- `MidasHand`：消耗经验将方块/实体“点金”
- `HornPlenty`：消耗经验随机生成食物
- `MagicRope`：记录并传送到目标位置

## 6. 常用 API 速查（面向二次开发）

### 6.1 注册新的数据生成器类型
1. 实现 `DataGenerator` 子类并定义 `MapCodec`
2. 调用 `DataGenerator.register(id, codec)`
3. 在 pack 的 `generator` 目录投放配置

### 6.2 增加自定义占位符
1. 构建 `Placeholder<T>` 节点（`then` / `thenMap` / `thenList`）
2. 在对应场景拼接 `overwrite/concat`
3. 用 `Template.parse()` 渲染模板

### 6.3 自定义机器库存适配
1. 机器实现 `Repo<T>`（或组合 `RepoUnit/RepoBatch`）
2. `RepoProxy.item(repo)` / `RepoProxy.fluid(repo)` 桥接平台能力
3. `ProxyProvider.registerItem/registerFluid` 注册到方块

### 6.4 读取或查询自定义配方
1. 定义 `DisplayableRecipe` + `TypedSerializer`
2. `Recipes.register(typedSerializer)` 注册
3. 运行时 `typedSerializer.find(input, level)` 查询

## 7. 命令与配置（运维入口）

服务器主命令：`/cropariaServer ...`

主要子命令：
- `crop` / `melon`：查询、导出、从手持材料快速创建配置
- `generator`：导出/清理内置生成器、查询 pack 和生成器
- `config`：切换 `infusor`、`ritual`、`fruitUse`、`soakAttempts`、`autoReload`、`override` 等

## 8. 设计评价与注意事项

优点：
- 高度数据驱动：大量玩法行为由 JSON + Codec 决定
- API 边界清晰：配方、资源存储、模板生成相互解耦
- 跨平台友好：Architectury + `@ExpectPlatform` 统一 common 层逻辑

需要注意：
- 部分平台代理方法标记 `@Unreliable`，跨端行为可能有细节差异
- 多处逻辑依赖运行时配置（mod 黑白名单、crop 开关），排障时需同时看配置
- `Template`/Placeholder 失败时通常保留原占位符或抛异常，生成器开发要做错误处理

## 9. 结语
Croparia IF 的 `api` 层本质上是一个“可扩展的玩法 DSL 运行时”：  
用 `Codec + Placeholder + Repo + TypedSerializer` 把“数据定义 -> 世界行为”打通。  
对二开来说，最稳定的扩展路径是：

- 新增/调整作物与配方 JSON
- 扩展 `RecipeWizardGenerator` 占位符
- 复用 `Repo` 接口接入自定义机器与物流
