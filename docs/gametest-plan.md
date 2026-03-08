# NeoForge GameTest 规划（2026-03-08）

## 1. 目标

- 将测试重心从“纯逻辑 JUnit”扩展到“世界交互/注册表/方块行为”验证。
- 用可重复的 GameTest 场景覆盖高风险玩法链路，降低跨版本回归风险。

## 2. 范围与分层

### 2.1 已由 JUnit 覆盖

- `common`：codec/json/placeholder/repo/generator util/config/crop registry 等纯逻辑。
- `fabric`/`neoforge` JUnit：loader 运行时可达性、部分运行时语义镜像。

### 2.2 计划由 GameTest 覆盖

- `Infusor` 世界交互链路（踩踏/投掷触发、元素状态、产物生成）
- `RitualStructure + RitualRecipe`（结构匹配、材料消耗、失败分支）
- `Soak`（随机 tick 触发的方块转化）
- 关键方块实体流程（如温室自动收获）中的“世界状态变化”

## 3. 优先级计划

### P0（先落地）

1. `sanity_bootstrap`  
   - 目标：验证 GameTest 基础环境稳定、测试命名空间可执行。  
   - 模板：`minecraft:empty`

2. `infusor_basic_craft`  
   - 目标：验证 Infusor 在有效输入下产出预期结果。  
   - 验收：结果物出现、输入被消耗、状态变更符合预期。

### P1（第二批）

3. `ritual_structure_match_success`  
   - 目标：结构正确时可触发仪式链路。  
   - 验收：结构匹配成功并产生产物。

4. `ritual_structure_match_fail`  
   - 目标：关键方块缺失/错误时不应触发。  
   - 验收：无产物、无额外消耗。

### P2（第三批）

5. `soak_block_transform`  
   - 目标：浸染流程可触发方块转化。  
   - 验收：目标方块在测试窗口内变为期望输出。

6. `greenhouse_harvest_cycle`  
   - 目标：温室完整收获循环可执行。  
   - 验收：作物成熟被收获、库存写入正确。

## 4. 模板与命名规范

- GameTest 方法命名：`<system>_<scenario>_<expected>`
- 结构模板命名：`croparia:<system>/<scenario>`
- 每个测试只验证一个主断言，复杂流程拆分多用例。

## 5. 执行方式

1. 在 `neoforge/src/main/java/.../gametest/` 增加 `@GameTest` 方法。  
2. 在对应结构目录增加模板（从 `minecraft:empty` 逐步迁移）。  
3. 启动 `:neoforge:runServer` 后执行：`/test runall croparia`。  
4. 将失败案例回归到 JUnit（纯逻辑）或保留在 GameTest（世界交互）。

## 6. 里程碑与完成定义

- M1：P0 全绿，GameTest 执行链稳定。
- M2：P1 全绿，仪式核心路径可回归。
- M3：P2 至少完成 1 项（优先 `soak_block_transform`）。

完成定义：
- 每个里程碑包含可复现执行步骤、通过日志、失败定位说明。

## 7. 基于 resources 的可测清单（可直接转 GameTest）

### 7.1 Infusor（配方与状态）

- 数据来源：
  - `common/src/main/resources/data/croparia/recipe/infusor/dragon_breath.json`
  - `common/src/main/resources/data/croparia/recipe/infusor/elemental_stone.json`
  - `common/src/main/resources/data/croparia/recipe/infusor/activated_shrieker.json`
- 建议用例：
  1. `infusor_air_awkward_to_dragon_breath`
  2. `infusor_elemental_stone_result`
  3. `infusor_elemental_activated_shrieker_result`
- 断言重点：
  - 输入消耗、产物正确、Infusor 元素状态符合预期。

### 7.2 Ritual（结构/标签/产物）

- 数据来源：
  - `common/src/main/resources/data/croparia/recipe/ritual/sculk_sensor.json`
  - `common/src/main/resources/data/croparia/recipe/ritual/relic/magic_rope.json`
  - `common/src/main/resources/data/croparia/recipe/ritual/enchantment/mending.json`
  - `common/src/main/resources/data/croparia/tags/block/ritual_stands*.json`
- 建议用例：
  1. `ritual_stand_tag_chain_accepts_level1`
  2. `ritual_stand_tag_chain_accepts_level2`
  3. `ritual_enchant_book_mending_components`
- 断言重点：
  - 结构匹配成功/失败、tag 层级匹配、附魔书 `components` 精确性。

### 7.3 Soak（高影响细节，优先 100% 概率）

- 数据来源：
  - `common/src/main/resources/data/croparia/recipe/soak/crying_obsidian.json`（`probability: 1.0`）
  - `common/src/main/resources/data/croparia/recipe/soak/sculk.json`（`probability: 1.0`）
  - `common/src/main/resources/data/croparia/recipe/soak/infusor/croparia_air.json`
- 建议用例：
  1. `soak_obsidian_to_crying_obsidian_deterministic`
  2. `soak_moss_to_sculk_deterministic`
  3. `soak_infusor_state_empty_to_air`
- 断言重点：
  - 方块转换正确、Infusor 方块属性变化正确。

### 7.4 Crafting 与掉落（体验敏感）

- 数据来源：
  - `common/src/main/resources/data/croparia/recipe/crafting/infusor.json`
  - `common/src/main/resources/data/croparia/recipe/crafting/greenhouse.json`
  - `common/src/main/resources/data/croparia/loot_table/blocks/infusor.json`
  - `common/src/main/resources/data/croparia/loot_table/blocks/greenhouse.json`
- 建议用例：
  1. `craft_infusor_recipe_valid`
  2. `craft_greenhouse_recipe_valid`
  3. `break_infusor_drops_self`
  4. `break_greenhouse_drops_self`

### 7.5 进度数据（Advancement）

- 数据来源：
  - `common/src/main/resources/data/croparia/advancement/croparia/infusor.json`
  - `common/src/main/resources/data/croparia/advancement/croparia/ritual_stand.json`
  - `common/src/main/resources/data/croparia/advancement/croparia/greenhouse.json`
- 建议用例：
  1. `adv_infusor_inventory_changed_grants`
  2. `adv_ritual_stand_inventory_changed_grants`
  3. `adv_greenhouse_inventory_changed_grants`
- 断言重点：
  - 给予物品后 advancement 达成，避免“有物品但不触发”的体验级故障。

## 8. 模板资源现状与补充计划

- 当前仓库未发现现成 `data/*/structures` GameTest 结构模板。
- 建议先新增最小模板集：
  1. `croparia:gametest/infusor_basic`
  2. `croparia:gametest/ritual_basic`
  3. `croparia:gametest/soak_basic`
  4. `croparia:gametest/crafting_and_loot`
- 所有 P0/P1 用例优先绑定上述模板，减少搭建成本。
