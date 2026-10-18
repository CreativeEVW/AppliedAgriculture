---
navigation:
  parent: index.md
  title: 作物装配室
  icon: crop_assembly_chamber
item_ids:
- appliedagriculture:crop_assembly_chamber
---

# 作物装配室 (Crop Assembly Chamber)

<BlockImage id="crop_assembly_chamber" scale="8" />

接入 ME 网络后，模拟作物的种植、催熟（age 直接拉满，不使用骨粉）与破坏，
将破坏产生的全部掉落物存入网络。模拟仅内置进行，不会在世界中放置任何方块。

## 消耗（每轮）

- 能量：200 AE（取自网络）
- 水：1b（取自网络流体存储）

## 输入（中心槽，不消耗）

| 输入类型 | 产出 |
|----------|------|
| 作物种子（带 age 属性的方块，如小麦种子/萝卜/土豆/甜菜/地狱疣等） | 成熟后破坏的掉落物 |
| 花（小花与高花标签） | 同种花 ×3 |
| 紫颂花 | 紫颂花 ×1 + 紫颂果 ×1 |

## 升级卡（8 槽）

| 卡 | 最大数量 | 效果 |
|----|----------|------|
| 扩增卡 | 6 | 每张使产出 ×3（满配 ×729） |
| 加速卡 | 2 | 周期 10s → 约 3.2s → 1s |

<RecipeFor id="crop_assembly_chamber" />
