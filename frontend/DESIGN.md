# Design System: 校友安居（Alumni Housing）

> 参考原型：`docs/design/alumni-anju-h5-prototype.html`（可交互 H5 原型）
> 技术栈：Vue 3 + Vant 4 + Vite，钉钉内 H5 微应用（375 设计稿，postcss-px-to-viewport 转 vw）
> 标记说明：标注「原型」= 直接来自原型；「规范」= 依据原型风格补定的实现规范

## 1. Visual Theme & Atmosphere

校友安居是一个**只属于阿里/蚂蚁校友的可信租房网络**。它的设计任务不是"好看"，而是**把信任变成可见的界面**：让浏览者一眼看出"这是真的房源、真的校友、真的经过审核"。整个视觉语言围绕这一任务展开。

设计基座是 **暖白（`#F5F5F4`）+ 活力橙（`#FF6A00`）**：白色卡片承载信息，橙色只服务于"行动与信任信号"——主按钮、价格、已核实、激活态。这样页面大部分时间是安静的，橙色一出现就是"这里可以点、这里很重要"。房源缩略图用柔和渐变占位（橙/蓝/绿），无实拍图也不显空，反而像社区手绘卡片，弱化了中介平台的冷硬感。

密度上它是**浏览型而非清单型**：卡片即决策单元，每张只放 6 要素（价格户型/押付/标签/已核实/通勤/联系），信息在详情页才展开。底部 TabBar 中间的**凸起橙色发布按钮**是视觉重心与转化锚点——「发布」是社区的供给端入口，设计上给了它最重的视觉权重。

**Key Characteristics:**
- 暖白画布 + 活力橙单一品牌强调色；**橙色只属于行动、价格与信任信号**
- 信任可视化：已核实绿标、标签语义色、避坑提醒黄条、房东卡片
- 卡片式信息流：白卡 + `1px var(--border)` 描边分级，大圆角（10/12-14/16/20/999）
- 缩略图渐变占位三系（橙/蓝/绿）+ 价格角标，无图不空
- 凸起发布钮（52px 圆角 16 + 橙光投影）—— 唯一重阴影元素
- 价格等数字一律 DM Sans（`--num`），与中文正文对比
- 近黑文本 `rgba(0,0,0,0.85)`（非纯黑），温暖不刺眼
- 动效克制：150–200ms 的过渡 + 按压缩放，无花哨动画

## 2. Color Palette & Roles

### Brand（品牌）
| Token | 值 | 用途 | 不要用于 |
|-------|----|------|---------|
| `--primary` | `#FF6A00` | 主 CTA、品牌标识、激活态、价格强调、图标点缀 | 大段背景、正文 |
| `--primary-deep` | `#E85D00` | 按压态、详情页主价格、选中文字 | 大面积填充 |
| `--primary-soft` | `#FFF3E6` | 橙标签底、选中底、hero 渐变起点、AI 提示 | 纯装饰 |

### Semantic（语义，表达信任与状态）
| Token | 值 | 用途 |
|-------|----|------|
| `--accent` | `#16A34A` | "已核实"标、可养宠、Switch 开态、成功 |
| `--accent-soft` | `#F0FDF4` | 绿标签底 |
| `--destructive` | `#DC2626` | 错误、表单必填 `*`、举报 |
| `--warning` | `#D48806` | 避坑提醒关键词 |
| `--warning-soft` | `#FFFBE6` | 避坑提醒条底（边 `#FFE58F`，深字 `#874D00`） |
| 转租蓝 | `#2563EB` / 底 `#EFF6FF` | "校友转租"标签 |
| 合租紫 | `#7C3AED` / 底 `#F5F3FF` | "合租拼室友"标签 |

### Neutrals（中性）
| Token | 值 | 用途 |
|-------|----|------|
| `--bg` | `#F5F5F4` | 页面底、8px divider、spec 底 |
| `--card` | `#FFFFFF` | 卡片、顶栏、TabBar、表单分区 |
| Input Fill | `#F2F2F1` | 搜索框/输入框未激活底 |
| `--fg` | `rgba(0,0,0,0.85)` | 标题/正文 |
| `--fg2` | `rgba(0,0,0,0.55)` | 元信息、次级文字 |
| `--fg3` | `rgba(0,0,0,0.35)` | 弱化文字、占位、图标 |
| `--border` | `rgba(0,0,0,0.08)` | 卡片描边、分隔线 |

### Thumb Gradients（缩略图/hero 渐变占位）
- 橙系（默认/主推）`135deg #FFD9C2 → #FFB98A`；hero `#FFD9C2 → #FF9E5E`
- 蓝系 `135deg #C9E4FF → #8FC1FF`
- 绿系 `135deg #D8F5D8 → #A8E6B8`
- 分配策略（规范）：首图/主推用橙系，其余按房源 id 或轮换取蓝/绿，同屏避免相邻同色

## 3. Typography Rules

### Font Family
- **中文/UI**：`-apple-system, BlinkMacSystemFont, "PingFang SC", "Noto Sans SC", "Segoe UI", sans-serif`
- **数字**（价格/面积/统计/角标）：`'DM Sans', -apple-system, sans-serif`（`--num`，500–700）
- **图标**：Phosphor Icons regular（`ph` 类），与原型一致

### Hierarchy（375 设计稿 px，1rem=16px）

| Role | Size | Weight | Line H | Color | Notes |
|------|------|--------|--------|-------|-------|
| 详情主价格 | 24px (1.5rem) | 700 | 1.2 | `--primary-deep` | `--num`；单位 `<small>` 12.8px 500 `--fg2` |
| 页面/卡片大标题 | 15.7px (0.98rem) | 700 | 1.3 | `--fg` | brand、prompt hero big |
| 详情标题 | 16px (1rem) | 600 | 1.3 | `--fg` | d-title |
| 房源卡标题 | 13.8px (0.86rem) | 600 | 1.35 | `--fg` | h-title |
| 小节标题 | 13.1px (0.82rem) | 600 | 1.4 | `--fg` | 详情 h5、表单 label、订阅项 |
| 主按钮 | 14.4px (0.9rem) | 600 | 1.2 | `#fff` | |
| 正文/输入 | 12.8–13.6px | 400–600 | 1.5–1.6 | `--fg`/`--fg2` | |
| 卡片元信息 | 11.5px (0.72rem) | 400 | 1.4 | `--fg2` | h-meta、prompt small |
| 弱化/角标 | 10–11.2px | 400–600 | 1.4 | `--fg3` | tag 0.62rem 600 |
| 价格角标 | 11.5px | 700 | 1.2 | `#fff` | `--num`，黑底 0.72 |

### Principles
- **价格必用 DM Sans**：所有金额、面积、统计数字（`.num`）；金额格式 `5,800 元/月`（千分位，规范）
- **层级靠 rgba 三档表达**，不靠字号轰炸；正文永不用纯黑
- **强调 = 600 weight + 橙色**；标题可 700；避免 300/400 作标题
- 中文行高 ≥1.4（卡片标题 1.35，正文 1.5–1.6）
- 时间格式（规范）："今天/昨天/N 天前"，跨月显示日期

## 4. Component Stylings

### Buttons（全部含状态）

**Primary 主按钮（.btn-primary）**
- 底 `--primary` / 字 `#fff` / Radius **10px** / Padding 12px（全宽）/ 0.9rem 600
- 立体边 `box-shadow: 0 2px 0 var(--primary-deep)`；**Active**：`translateY(1px)` + 阴影归零
- **Disabled**（规范）：`opacity: 0.5`，去掉立体边，禁止点击
- **Loading**（规范）：按钮内转圈 + 文字"提交中…"，宽不变防跳动
- Transition `all 0.15s ease`

**Ghost（.btn-ghost）**：白底 / `--fg` / `1px var(--border)` / Radius 10px / Padding 11px；Active 底 `--bg`

**Icon 按钮（.icon-btn）**：46×46 / Radius 10px / `1px var(--border)` / `--fg2` 图标；Active 底 `--bg`

### Tags（信任语义，.tag）
Size 0.62rem/600，Padding 2px 7px，Radius 4px：
- 房东直租：`--primary-soft` / `--primary-deep`
- 已核实：`--accent-soft` / `--accent`
- 校友转租：`#EFF6FF` / `#2563EB`
- 合租拼室友：`#F5F3FF` / `#7C3AED`

### Top Bar（.topbar）
Sticky / `--card` / Padding 14px 16px 10px / 下边框 `--border`。
左：返回键 1.1rem `--fg2` 或品牌（700，span 橙）；中：居中小标题 0.9rem/600。

### Search + Chips
- **search-input**：底 `#F2F2F1` / Radius 10px / Padding 10px 14px / 0.85rem `--fg3` + 搜索图标；**Active**：橙边框 + 白底 + `--fg`
- **chips**（横向滚动，隐藏滚动条）：胶囊 999px / 0.74rem / Padding 5px 12px / `1px var(--border)`；**选中**：`--primary-soft` 底 + `--primary` 边 + `--primary-deep` 600

### House Card（.h-card —— 核心组件）
- `--card` / Radius 14px / Padding 12px / `1px var(--border)` / 横向 flex，gap 12px
- **缩略图** 96×96 / Radius 10px / 渐变占位 / 左下价格角标（黑底 0.72 + 白字 num）
- **标题** 0.86rem/600；**元信息** 0.72rem `--fg2`（押付 · 可养宠 · 上架时间）
- **标签** 语义行；**底部** 通勤（bicycle 图标 `--primary` + `--fg2`）
- **Active**：橙晕 `0 4px 16px rgba(255,106,0,0.15)`；Transition 0.2s

### Tab Bar（.tabbar）
- 底部吸底 / `rgba(255,255,255,0.96)` + `blur(10px)` / 高 64px / 上边框 / 底部安全区 padding 8px
- 5 Tab：图标 1.25rem + 文字 0.62rem；选中橙 600
- **发布凸起**：52×52 / Radius 16px / `--primary` + 白 `+` / `box-shadow 0 6px 16px rgba(255,106,0,0.4)` / 上移 18px / 4px `--bg` 描边

### House Detail
- **hero**：高 210px / 渐变 `#FFD9C2→#FF9E5E` / 底部白圆点指示器（当前 6px 白，其余 0.5 白）
- **主体**：白卡上浮 `margin-top:-16px` / `border-radius: 20px 20px 0 0` / Padding 18px 16px
- **specs 网格**：3 列 / `--bg` 底 Radius 10px / 值 0.86rem 600 + 键 0.64rem `--fg3`
- **房东卡**：`1px var(--border)` Radius 12px / 40px 圆渐变头像（首字白 700）/ 昵称 + 右侧"已核实"胶囊
- **避坑条（guide-strip）**：`--warning-soft` 底 + `1px #FFE58F` / Radius 12px / 0.74rem `#874D00`，关键词 `--warning` 700
- **操作栏（action-bar）**：吸底白卡 / 主按钮 flex:1 + 46px 图标钮

### Forms
- **分区**：白卡 + 下边框；label 0.8rem/600，必填 `*` `--destructive`，hint 右侧 0.66rem `--fg3`
- **上传格**：3 列 1:1 / `1.5px dashed #D9D9D9` Radius 10px / 底 `#FAFAFA`；已传渐变实图 + 角标删除（规范）
- **输入行（form-field）**：`1px var(--border)` Radius 10px / 输入右对齐 / 占位 `--fg3`
- **Focus**（规范）：`1px var(--primary)` 边框；**Error**（规范）：`1px var(--destructive)` + label 红字
- **分段（seg）**：方格 Radius 8px / 选中橙底橙边 600
- **AI 提示（ai-tip）**：`linear-gradient(90deg, --primary-soft, #FFF7E6)` + 橙边 0.2 透明度 / `#874D00` + 橙图标

### Demand / Subscribe
- **prompt-hero**：`linear-gradient(135deg, --primary-soft, #FFEBD6)` / big 0.98rem 700 + small 0.72rem `--fg2`
- **prompt-input**：白底 Radius 12px / `1px var(--primary)` / 橙晕 `0 4px 14px rgba(255,106,0,0.12)`
- **match-card**：白卡 Radius 14px / "为什么推给我"条：`--primary-soft` 底 `--primary-deep` 0.7rem，Radius 6px
- **sub-item**：白卡 Radius 12px / 问题 0.82rem 600 + 状态 0.66rem `--fg3` + Switch
- **Switch**（规范，与原型一致）：40×22 / Radius 999px / 开 `--accent`、关 `--border` / 白圆钮 16px 位移 0.2s

### Feedback & Empty（规范，依据原型风格补充）
- **Toast**：Vant Toast，深色底（近黑 0.9），白字 0.8rem，圆角 10px
- **空态**：居中 0.85rem `--fg3` + Phosphor 图标 2rem `--fg3`，文案如"暂无房源，先发布一套吧"
- **加载**：列表首屏 Vant Skeleton（卡片骨架：96×96 方块 + 三行条）；下拉/上拉 Vant 默认
- **确认弹层**：Vant Dialog，主按钮 `--primary`

## 5. Layout Principles

### 页面骨架（每页 = 固定区块组合）
| 页面 | 骨架（自上而下） |
|------|----------------|
| 首页 | topbar(品牌) → searchbar → chips → feed(房源卡) → tabbar |
| 详情 | topbar(返回+标题) → hero → 上浮白卡(价格/specs/房东/避坑) → 吸底 action-bar |
| 发布 | topbar(返回) → 滚动表单分区(上传/房源/租金/通勤/描述) → 吸底提交钮 |
| 求租 | topbar(品牌) → prompt-hero(一句话需求+输入) → 需求墙列表 → tabbar |
| 订阅 | topbar(品牌) → prompt-hero(创建订阅) → 订阅列表 → tabbar |
| 我的 | topbar(品牌) → me-hero(头像) → me-grid(统计) → me-list(入口) → tabbar |

### 间距与网格
- 基准 375，内容容器 max 480px 居中；**页面边距 16px**（feed 12px 16px）
- 卡片间距 **12px**；区块间 **8px `--bg` divider**；分区内 padding 14px 16px
- 滚动内容 `padding-bottom: 76px` 避开 TabBar；滚动条隐藏
- 三列网格：specs / 上传 / 我的统计；横向滚动：chips / 图集
- **圆角体系**：4 标签 / 10 输入按钮 / 12-14 卡片 / 16 发布钮 / 20 详情上浮 / 999 胶囊
- 移动端单列为主，勿做多列瀑布

## 6. Depth & Elevation

**原则：平铺为主（描边分级），阴影只属于橙色强调元素。**

| 元素 | 阴影 | 用途 |
|------|------|------|
| 主按钮 | `0 2px 0 var(--primary-deep)` | 2px 立体边（按压下沉） |
| 凸起发布钮 | `0 6px 16px rgba(255,106,0,0.4)` | 唯一重阴影，视觉锚点 |
| 房源卡 active | `0 4px 16px rgba(255,106,0,0.15)` | 点按反馈 |
| prompt-input | `0 4px 14px rgba(255,106,0,0.12)` | 聚焦吸引力 |
| TabBar/TopBar | `backdrop-filter: blur(10px)` | 毛玻璃浮层 |

**动效（规范，遵循原型节奏）**：
- 微交互 150ms `ease`（按钮按压、chip 切换、输入 focus）
- 卡片/弹层 200ms（卡片 active、Switch、图片淡入）
- 按压反馈统一"1px 下沉或 scale(0.98)"，不做位移动画
- 页面切换用 Vant 默认淡入，不引入转场库（保持克制）

## 7. Do's and Don'ts

### Do
- 房源卡只放 6 要素，一切为"决策"服务；信息展开在详情页
- 所有金额/面积/统计用 DM Sans（`.num`），金额千分位 + "元/月"
- 橙色只用于行动、价格、激活、信任信号（已核实/选中）——它出现即有含义
- 信任可视化：已核实绿标、标签语义色、避坑黄条、房东卡
- 无图房源用渐变占位 + 价格角标，卡片保持完整可点
- 用 8px divider 与 12px 间距组织密度，留白充足（浏览型，非清单型）
- 必填标记用红色 `*`，错误提示红色边框 + 文案
- 列表永远有加载态/空态，网络失败给重试

### Don't
- 不要引入第二个主色（橙之外不再有并列强调色）
- 不要在房源卡堆全字段（押付/标签/通勤等放 6 要素内，其余进详情）
- 不在公开卡片展示房号、手机号、联系方式原文（房号仅审核可见）
- 不用纯黑 `#000` 正文、不用灰色重阴影做卡片层级（描边 + 橙晕即可）
- 发布按钮必须凸起居中，不做成普通 Tab
- 不做花哨动画（无转场库、无弹跳、无滚动视差）
- 不要让橙色大面积铺底（hero 渐变除外，且须配深字）

## 8. Responsive Behavior

- **vw 适配**：375 设计稿 postcss-px-to-viewport 转 vw，一套代码全机型
- **容器**：max 480px 居中；`viewport-fit=cover` + 安全区适配（TabBar 底部、action-bar 底部 padding）
- **触控目标**：主按钮/列表行 ≥44px 高；图标钮 ≥40px；chips ≥28px 高可点
- **横向滚动**：chips、图集（隐藏滚动条）；三列网格固定不滚
- **字体**：不随屏幕缩放（vw 只作用于布局尺寸，字号用 px 或 rem 控制，规范）
- 钉钉容器内：hash 路由刷新不 404；`dd.ready` 后调用 JSAPI

## 9. Agent Prompt Guide

> 实现页面时，将本 DESIGN.md 与原型 `docs/design/alumni-anju-h5-prototype.html`、令牌 `src/styles/tokens.css` 一并提供给前端 Agent。

**推荐 Prompt 模板：**

```
按 docs/design/2026-08-16-校友安居-前端实现方案.md、frontend/DESIGN.md 与
src/styles/tokens.css 实现「首页·房源广场」：

- 严格遵循 DESIGN.md：暖白底 #F5F5F4、活力橙 #FF6A00 仅用于 CTA/价格/激活；
  卡片 14px 圆角 + 1px --border 描边；价格用 DM Sans(.num) 千分位；卡片 6 要素布局
- 区块顺序：sticky topbar(品牌"校友安居") → 搜索框(占位"说人话找房：西溪附近 6000 以内两居")
  → 横向 chips(价格/户型/房东直租/可养宠/通勤/新上架，选中橙底) → 房源卡流(12px 间距)
  → 底部 5 Tab TabBar(中间凸起发布钮)
- 数据：GET /api/houses（后端已就绪，统一响应已解包）；空态给"暂无房源"文案；
  首屏 Vant Skeleton 加载；失败 Toast + 重试
- 交互：卡片 active 橙晕(0.15s)；价格角标黑底白字 num；无图用橙系渐变占位
- 样式只允许用 tokens.css 变量与 .num/.tag 等既有类，不写死色值
```

**风格关键词：** 温暖可信 · 活力橙（克制使用）· 大圆角 · 卡片 6 要素 · DM Sans 数字 · 凸起发布钮 · 渐变缩略图 · 语义色信任标 · 描边分级 · 橙晕微阴影 · 150-200ms 微动效
