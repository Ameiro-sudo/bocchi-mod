# bocchi-mod 无人值守任务认领日志

> 惯例对齐 IReckon `docs/UNATTENDED_LOG.md`: 动手前先登记, 完成后回填结果。
> 共享黑板(D:\project\AGENT_HANDOFF.md 八½节)同步登记; 多 clone 布局下以黑板为准。

## 任务 1: 设置面板 + Mods/Cfgs 持久化(2026-08-22 认领)

- **认领人**: ox-alpha 会话(goal-e1e8dab8)
- **分支**: `feat/settings-panel`(基于 main @ 35067d4)
- **范围**:
  1. 设置面板 UI: 主菜单(misayos/poulsen 双主题)新增 Cfgs 按钮进入, 控件复用
     skui 现有组件(SkContainer/SkScrollbar/FlowVerticalLayout)与既有视觉语言
  2. 配置保存回写: Cfgs 补对称 save 路径(与 load 同一 gson/同一 JSON 结构)
  3. 设置项消费: BooleanSetting/NumberSetting/RangedSetting/EnumSetting 全类型上板
     (745af6e 已修 Boolean Builder 参数传反)
  4. 测试: JUnit 持久化往返(save→load 对称性)+ 双树 gradle 构建; CI 补 src/** 的
     pull_request 门禁(此前 build-release 只认 tag/dispatch, PR 无构建验证)
- **不做**: 第三主题注册(美术资源未到位, ThemeManager 注册制保持就绪);
  不触碰 tools/bocchi-designer(另一会话 WIP 占用中)
- **工作模式**: 独立 clone `D:\project\bocchi-mod-settings`(共享检出内有并行会话活跃)

### 结果回填(2026-08-22 04:2x)

- **PR #7 已合并** → main @ 86a5080(merge commit), 分支 feat/settings-panel 共 9 提交:
  - ci: build-pr 门禁工作流(src/** PR 双树构建)
  - fix(setting): RangedSetting.load 类型保真(LazilyParsedNumber 污染 Integer 字段→save 丢值) + Builder value/defaultValue 语义统一
  - feat(cfg): C. Cfgs 对称 save 路径 + activeName 追踪(tmp+原子替换)
  - chore(mod): Aura 占位项自描述化(Test 泄漏整备, BM-05③)
  - test(common): JUnit5 基建 + 5 例(四类型 save/load 对称、Builder 回归、Cfgs 全链路重扫还原)
  - feat(ui): 设置面板(SettingsScreen/SettingsPanel/ScrollList/SettingRows 五控件)
  - feat(ui): 双主题 Cfgs 入口按钮(option.svg 复用零新资产) + design.json sTitle/sDone
- **CI**: PR #7 build-pr 双树构建绿; 合并后 main push 自动触发的 build-pr 绿(run 32522521780)
- **验证方式学**: 双树 :common:test 5/5 绿 + :fabric:build/:neoforge:build 全绿 ×2 轮;
  双树逐文件哈希核对 SAME(SettingsScreen/poulsen 屏保留 GlStateManager import 一行差异, 与既有约定一致);
  UI 视觉验收用同常量 HTML 预览页 + headless Edge 截图(本地 D:/project/bocchi-settings-shots/, 未入库)
- **遗留**: 面板滚动条拖拽手感未实测(无真机); 第三主题注册位就绪待美术; tools/** 由并行会话进行中(designer-reset/history), 本任务未触碰