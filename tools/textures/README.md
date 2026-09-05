# Applied Flooring 灰度材质与原生 BakedModel CTM 连接系统开发原理指南

本文档详细记录了 **Applied Flooring** 模组中 ME 地板（ME Flooring）、ME 电梯（ME Elevator）以及 ME 激光连接器（ME Laser Connector）的灰度动态着色规范、原生 BakedModel CTM 象限切片算法、全量预烘焙缓存优化机制，以及后续维护指南。

---

## 1. 架构演进与设计对比

在重构前，模组为每种颜色分别生成独立的贴图和 Athena/Fusion 切片文件，导致以下严重问题：
1. **贴图膨胀**：17 种颜色 $\times$ 3 种方块 $\times$ 2 状态 $\times$ 4 切片，全模组贴图超过 400 张 PNG，加载时严重占用纹理图集（Texture Atlas）显存与内存；
2. **强依赖外部模组**：依赖 Athena 或 Fusion 才能渲染连接效果，原版客户端方块间无法连接。

### 重构后架构（基于 BakedModel + ColorProvider）：
1. **动态色彩注入 (`BlockColorProvider` / `ItemColorProvider`)**：
   - 全模组 17 种颜色（16 染料色 + 未染色 Fluix 紫色）完全收敛至单一**灰度母版贴图**；
   - 客户端通过 Architectury `ColorHandlerRegistry` 注册颜色提供器，在渲染时根据方块/物品的 `AEColor.mediumVariant` 进行色彩乘法：
     $$C_{\text{final}} = C_{\text{gray}} \times C_{\text{mediumVariant}} / 255$$
   - 彻底消除了所有颜色的重复贴图，贴图数量从 408+ 张减少至 **~14 张**。
2. **原生连接烘焙模型 (`MEFlooringCTMHelper` + Platform BakedModels)**：
   - 彻底脱离第三方模组（无需安装 Athena 或 Fusion），纯净客户端即享完整 CTM 效果；
   - 采用四象限 CTM 算法与 **240 Quads 全量预烘焙缓存**，渲染时 $O(1)$ 位运算查表，运行时 **零内存分配垃圾（Zero Allocation）**。

---

## 2. 16x16 像素几何划分与象限设计

方块的表面几何划分如下：
```text
       x = 0   1   2   3 ... 12  13  14  15
y = 0  +---+---+---+-----------+---+---+---+  <-- 外边框顶
y = 1  |   +---+---+-----------+---+---+   |  <-- 内框架顶
y = 2  |   |   +---+-----------+---+   |   |  <-- 倒角暗槽顶
y = 3  |   |   |                   |   |   |
  :    |   |   |   中心金属面板     |   |   |
       |   |   |   (10x10 Plate)   |   |   |  <-- 基础平铺面板
y = 12 |   |   |                   |   |   |
y = 13 |   |   +---+-----------+---+   |   |  <-- 倒角暗槽底
y = 14 |   +---+---+-----------+---+---+   |  <-- 内框架底
y = 15 +---+---+---+-----------+---+---+---+  <-- 外边框底
```

### 关键约束：
- **中心金属面板**：`x ∈ [3..12], y ∈ [3..12]`（10x10 像素），是无缝平铺的主要视觉基底；
- **功能图标范围**：电梯（箭头）与激光连接器（透镜）的图标严格位于 `[3..12, 3..12]`，在 BakedModel 中以独立图层渲染（`tintIndex = -1`，不染色保持金属/发光质感）；
- **100% 外围共享**：电梯、激光连接器与普通地板在 `3..12` 以外区域完全共享 CTM 连通逻辑，实现三者无缝混排互连。

---

## 3. 四象限 CTM 算法与状态判定

每个方块面（UP, DOWN, NORTH, SOUTH, WEST, EAST）被划分为 4 个 $8 \times 8$ 象限：
- **象限 0（Top-Left）**：检查横向 $H$（左）、纵向 $V$（上）、对角 $D$（左上）
- **象限 1（Top-Right）**：检查横向 $H$（右）、纵向 $V$（上）、对角 $D$（右上）
- **象限 2（Bottom-Left）**：检查横向 $H$（左）、纵向 $V$（下）、对角 $D$（左下）
- **象限 3（Bottom-Right）**：检查横向 $H$（右）、纵向 $V$（下）、对角 $D$（右下）

### 象限切片真值表（5 种切片）：
| 连接状态 | 切片类型 | 说明 |
| :--- | :--- | :--- |
| `!H && !V` | `BASE` | 外凸角（双向均无连接，保留外边框与暗槽） |
| `H && !V` | `H` | 横向连接（仅横向通，纵向封闭） |
| `!H && V` | `V` | 纵向连接（仅纵向通，横向封闭） |
| `H && V && D` | `EMPTY` | 全连通内部（三方均连通，无边框暗槽无缝面板） |
| `H && V && !D` | `CENTER` | 内凹角（两相邻连通但对角缺失，保留单个内凹交汇点） |

---

## 4. 全量预烘焙 Quad 缓存 (Zero Allocation)

为了保证 chunk 渲染性能与原版静态方块一致：
- 2 种通电状态（在线/离线）$\times$ 6 个面 $\times$ 4 个象限 $\times$ 5 种切片 = **共 240 个 `BakedQuad`**；
- 电梯与激光连接器中心图标：2 状态 $\times$ 6 面 = 12 个 overlay `BakedQuad`；
- 所有 Quad 在客户端启动/模型加载时通过 `FaceBakery` 一次性烘焙完成，缓存至数组：
  `quadCache[powerState][face][quadrant][sliceType]`；
- 运行时仅做 $O(1)$ 位运算获取邻居状态并查表返回 Quad，**完全无内存垃圾产生**。

---

## 5. 多平台适配与模型挂载

- **Forge (1.19.2 / 1.20.1)**：
  - 1.19.2 监听 `ModelEvent.BakingCompleted`；1.20.1 监听 `ModelEvent.ModifyBakingResult`；
  - 实现 `IForgeBakedModel`，通过 `getModelData(level, pos, state, modelData)` 传递连接掩码，并在 `getQuads` 中高效分发；
- **NeoForge (1.21.1)**：
  - 监听 `ModelEvent.ModifyBakingResult`，通过 `net.neoforged.neoforge.client.model.data.*` 传递数据并分发 Quads；
- **Fabric (1.19.2 / 1.20.1)**：
  - 实现 `FabricBakedModel`，通过 `emitBlockQuads` 将预烘焙 Quad 批量送入 `context.emitter`，完美兼容 Sodium / Iris / Indium。

---

## 6. 贴图修改维护指南

若未来需要调整地板或图标的美工设计：
1. **仅需编辑极少数灰度母版文件**（位于 `common/src/main/resources/assets/appliedflooring/textures/block/`）：
   - `me_flooring.png` & `me_flooring_offline.png`（基础面板）
   - `me_flooring_empty.png` & `_offline_empty.png`（内部平铺板）
   - `me_flooring_h.png` & `_offline_h.png`（横向切片）
   - `me_flooring_v.png` & `_offline_v.png`（纵向切片）
   - `me_flooring_center.png` & `_offline_center.png`（内凹角切片）
   - `me_elevator_icon.png` & `_offline_icon.png`（电梯箭头图标）
   - `me_laser_connector_icon.png` & `_offline_icon.png`（激光透镜图标）
2. **修改后直接运行构建验证**：
   ```bash
   ./gradlew build -x test
   ```
   无需再运行任何外部 Python 脚本，所有 16 种染色和所有平台自动同步生效！
