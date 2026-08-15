# Design System: 校友安居（Alumni Housing）

> 参考原型：`docs/design/alumni-anju-h5-prototype.html`（可交互 H5 原型）
> 技术栈：Vue 3 + Vant 4 + Vite，钉钉内 H5 微应用（375 设计稿，postcss-px-to-viewport 转 vw）

## 1. Visual Theme & Atmosphere

校友安居是一个**只属于阿里/蚂蚁校友的可信租房网络**，界面气质是「温暖、可信赖、社群感」——不是冷冰冰的房产平台，也不是拥挤的中介信息流。

设计建立在 **暖白底（`#F5F5F4`）+ 活力橙（`#FF6A00`）** 的品牌基座上：白色卡片承载信息，橙色只用于"行动"（主按钮、价格强调、激活态、品牌标识）。界面像一本随手翻开的社区租房志：卡片式信息流、大圆角、橙色渐变的房源缩略图、底部中间凸起的发布按钮——浏览起来是轻松的、可信任的，而不是压迫式售卖。

**Key Characteristics:**
- 暖白画布 + 活力橙单一品牌强调色（`#FF6A00`），橙色只服务行动与价格
- 房源缩略图用柔和渐变占位（橙/蓝/绿三系），无图也不空
- 大圆角体系：10px 输入框 / 12–14px 卡片 / 16px 发布钮 / 20px 详情上浮 / 999px 胶囊
- 房源卡片即决策单元：只放 6 要素（价格户型/押付/标签/已核实/通勤/联系），不堆信息
- 底部 TabBar 中间**凸起橙色发布按钮**（52px 圆角 16 + 橙色投影），视觉重心明确
- 标签系统用语义色区分信任等级（房东直租橙 / 已核实绿 / 转租蓝 / 合租紫）
- 价格等数字一律用 DM Sans（`--num`），与中文正文形成对比
- 近黑文本 `rgba(0,0,0,0.85)` 而非纯黑，温暖不刺眼

## 2. Color Palette & Roles

### Brand（品牌）
- **Primary Orange** `#FF6A00`：`--primary` —— 主 CTA、品牌标识、激活态、价格强调、图标点缀
- **Primary Deep** `#E85D00`：`--primary-deep` —— 按压态、详情页主价格、选中文字
- **Primary Soft** `#FFF3E6`：`--primary-soft` —— 橙色浅底（标签底、选中底、hero 渐变起点）

### Semantic（语义）
- **Success Green** `#16A34A`：`--accent` —— "已核实"标、可养宠、Switch 开态
- **Success Soft** `#F0FDF4`：`--accent-soft` —— 绿色标签底
- **Danger Red** `#DC2626`：`--destructive` —— 错误、表单必填 `*`
- **Warning Gold** `#D48806`：`--warning` / 浅底 `#FFFBE6` / 深字 `#874D00` —— 避坑提醒条（guide-strip）
- **Info Blue** `#2563EB`（底 `#EFF6FF`）—— "校友转租"标签
- **Royal Purple** `#7C3AED`（底 `#F5F3FF`）—— "合租拼室友"标签

### Neutrals（中性）
- **Canvas** `#F5F5F4`：`--bg` —— 页面底色、分区 divider、spec 底
- **Card** `#FFFFFF`：`--card` —— 所有卡片、顶栏、TabBar
- **Input Fill** `#F2F2F1`：搜索框/输入框未激活底
- **Text Primary** `rgba(0,0,0,0.85)`：`--fg` —— 标题/正文
- **Text Secondary** `rgba(0,0,0,0.55)`：`--fg2` —— 元信息、次级文字
- **Text Tertiary** `rgba(0,0,0,0.35)`：`--fg3` —— 弱化文字、占位、图标
- **Border** `rgba(0,0,0,0.08)`：`--border` —— 卡片描边、分隔线

### Thumb Gradients（缩略图渐变占位）
- 橙系 `135deg #FFD9C2 → #FFB98A`（默认/主推）
- 蓝系 `135deg #C9E4FF → #8FC1FF`
- 绿系 `135deg #D8F5D8 → #A8E6B8`
- 详情 hero：`135deg #FFD9C2 → #FF9E5E`

## 3. Typography Rules

### Font Family
- **中文/UI**：`-apple-system, BlinkMacSystemFont, "PingFang SC", "Noto Sans SC", "Segoe UI", sans-serif`
- **数字（价格/面积/统计）**：`'DM Sans', -apple-system, sans-serif`（`--num`，weight 500–700）

### Hierarchy（字号基于 375 设计稿 px，1rem = 16px）

| Role | Size | Weight | Color | Notes |
|------|------|--------|-------|-------|
| 详情页主价格 | 1.5rem (24px) | 700 | `--primary-deep` | `--num`，`<small>` 单位 0.8rem fg2 |
| 品牌名 brand | 0.98rem (15.7px) | 700 | `--fg` | "安居"二字 `--primary` |
| 详情标题 d-title | 1rem (16px) | 600 | `--fg` | |
| 我的页昵称 | 1rem | 700 | `--fg` | |
| 房源卡标题 h-title | 0.86rem (13.8px) | 600 | `--fg` | 行高 1.35 |
| 详情小节 h5 | 0.82rem | 600 | `--fg` | |
| 主按钮 | 0.9rem | 600 | `#fff` | |
| 常规输入/描述 | 0.8rem | 400–600 | `--fg`/`--fg2` | |
| 卡片元信息 h-meta | 0.72rem | 400 | `--fg2` | |
| 弱化/角标 | 0.62–0.7rem | 400–600 | `--fg3` | tag 0.62rem 600 |
| 价格角标 pricetag | 0.72rem | 700 | `#fff` | `--num`，黑底 0.72 透明度 |

### Principles
- **价格必用 DM Sans**：所有金额、面积、统计数字（`.num`），与中文混排时对比鲜明
- **正文不用纯黑**：层级用 0.85 / 0.55 / 0.35 三档 rgba 表达，而非字号轰炸
- **强调靠 600 weight + 橙色**，标题区可用 700；避免 300/400 作标题
- 行高：正文 1.5–1.6，卡片标题 1.35，紧凑行 1.25

## 4. Component Stylings

### Buttons
**Primary（主按钮 .btn-primary）**
- Background `--primary`，Text `#fff`，Radius **10px**，Padding 12px（全宽）
- Box-shadow `0 2px 0 var(--primary-deep)`（底部 2px 立体边）
- Active：`translateY(1px)` + shadow 归零（按下沉效果）
- 文字 0.9rem / 600

**Ghost（.btn-ghost）**
- Background `#fff`，Text `--fg`，Border `1px var(--border)`，Radius 10px，Padding 11px

**Icon 按钮（.icon-btn）**
- 46×46，Radius 10px，Border `1px var(--border)`，`--fg2` 图标，用于详情底部操作栏

### Tags（信任语义色，.tag）
| 场景 | 底 | 字 |
|------|----|----|
| 房东直租（owner） | `--primary-soft` | `--primary-deep` |
| 已核实（verify） | `--accent-soft` | `--accent` |
| 校友转租（turn） | `#EFF6FF` | `#2563EB` |
| 合租拼室友（share） | `#F5F3FF` | `#7C3AED` |

- Size 0.62rem / 600，Padding 2px 7px，Radius 4px，内联排列

### Top Bar（.topbar）
- Sticky 顶部，`--card` 底，Padding 14px 16px 10px，下边框 `--border`
- 左：返回键（1.1rem fg2）或品牌（700，span 橙色）；中：居中小标题 0.9rem/600

### Search Bar（.searchbar / .search-input）
- 底 `#F2F2F1`，Radius 10px，Padding 10px 14px，0.85rem `--fg3`，内含搜索图标
- Active：`border-color var(--primary)` + 底变白 + 文字 `--fg`
- 下方为横向滚动**筛选 chips**：胶囊 999px，0.74rem，选中态橙底橙边（`--primary-soft` / `--primary` / `--primary-deep` 600）

### House Card（.h-card —— 核心组件）
- `--card` 底，**Radius 14px**，Padding 12px，`1px var(--border)` 描边，横向 flex
- **缩略图** 96×96，Radius 10px，渐变占位（橙/蓝/绿），左下角**价格角标**（黑底 0.72 + 白字 num 0.72rem/700，Radius 6px）
- **标题** 0.86rem/600；**元信息** 0.72rem fg2（押付 · 可养宠 · 上架时间）
- **标签** 语义色行
- **底部**：通勤（bicycle 图标 `--primary` + 文字 fg2，0.7rem）+ 右侧操作
- Active：橙阴影 `0 4px 16px rgba(255,106,0,0.15)`

### Tab Bar（.tabbar）
- 底部吸底，`rgba(255,255,255,0.96)` + `backdrop-filter: blur(10px)`，高 **64px**，上边框 `--border`
- 5 Tab（首页/求租/**发布**/订阅/我的），图标 1.25rem + 文字 0.62rem，选中橙 600
- **发布 Tab 凸起**：52×52 圆角 16，`--primary` 底 + 白 `+` 图标，`box-shadow 0 6px 16px rgba(255,106,0,0.4)`，上移 18px，4px `--bg` 描边

### House Detail
- **Hero**：高 210px，渐变 `#FFD9C2→#FF9E5E`，底部白色圆点指示器
- **主体**：白卡上浮（`margin-top: -16px`，`border-radius: 20px 20px 0 0`，Padding 18px 16px）
- **价格行**：1.5rem/700 `--primary-deep` num + small 单位
- **Specs 网格**：3 列，`--bg` 底 Radius 10px，值 0.86rem/600 + 键 0.64rem fg3
- **房东卡（.landlord）**：`1px var(--border)` Radius 12px，40px 圆形渐变头像（首字）+ 昵称 + 右侧"已核实"胶囊（`--accent-soft`/`--accent`）
- **避坑提醒（.guide-strip）**：`#FFFBE6` 底 + `1px #FFE58F` 边 Radius 12px，文字 0.74rem `#874D00`，关键词 `--warning` 加粗
- **底部操作栏（.action-bar）**：吸底白卡，主按钮 flex:1 + 46px 图标钮（举报/分享）

### Forms（发布）
- **分区（.form-sec）**：白卡 + 下边框；**label** 0.8rem/600，必填 `*` 用 `--destructive`，hint 右侧 0.66rem fg3
- **上传格（.upload-grid）**：3 列 1:1，`1.5px dashed #D9D9D9` Radius 10px，底 `#FAFAFA`；已传换渐变实图
- **输入行（.form-field）**：`1px var(--border)` Radius 10px，Padding 10px 12px，输入右对齐无边框
- **分段选择（.seg / .seg-item）**：胶囊方格（Radius 8px），选中橙底橙边 600
- **AI 提示（.ai-tip）**：`linear-gradient(90deg, var(--primary-soft), #FFF7E6)` 底 + 橙边框 0.2 透明度，`#874D00` 文字 + 橙色图标

### Demand / Subscribe
- **Hero（.prompt-hero）**：`linear-gradient(135deg, var(--primary-soft), #FFEBD6)`，大字 0.98rem/700 + 小字 0.72rem fg2 + 输入框
- **Prompt 输入**：白底 Radius 12px，**`1px var(--primary)` 边框** + 橙阴影 `0 4px 14px rgba(255,106,0,0.12)`
- **匹配卡（.match-card）**：白卡 Radius 14px，底部"为什么推给我"橙底理由条（`--primary-soft`/`--primary-deep` 0.7rem）
- **订阅项（.sub-item）**：白卡 Radius 12px，问题 0.82rem/600 + 状态 0.66rem fg3 + 右侧 Switch（40×22，开态 `--accent`，白圆钮）

### Me
- **Hero（.me-hero）**：`linear-gradient(135deg, var(--primary-soft), #FFE3CC)`，56px 渐变头像 + 昵称 1rem/700
- **统计格（.me-grid）**：3 列，数字 1.05rem/700 num + 键 0.66rem fg3
- **列表（.me-row）**：白底行，图标 1.15rem fg2 + 文字 0.84rem + 右侧箭头/badge（橙底胶囊）

## 5. Layout Principles

- **基准 375 设计稿**，内容容器 max 480px 居中；16px 页面边距（feed 12px 16px）
- **卡片间距 12px**（feed gap）；分区之间用 8px `--bg` divider 隔断
- **层级结构**：sticky topbar → 搜索/chips → 滚动内容（`padding-bottom: 76px` 避开 TabBar）→ 底部 TabBar（64px）
- 详情页/表单用**白卡分区堆叠**，卡片间 border 分隔，不做大阴影堆叠
- **圆角体系**：4px 标签 / 10px 输入与按钮 / 12–14px 卡片 / 16px 发布钮 / 20px 详情上浮 / 999px 胶囊
- 三列网格用于 specs / 上传 / 我的统计；横向滚动用于 chips 与图集
- 移动端单列为主，勿做多列瀑布

## 6. Depth & Elevation

原型整体**平铺为主**（border 分隔 > 阴影），阴影只用于三处"强调"：
- **主按钮**：`0 2px 0 var(--primary-deep)`（底部 2px 立体边，按压下沉）
- **凸起发布钮**：`0 6px 16px rgba(255,106,0,0.4)`（橙色发光，唯一重阴影）
- **房源卡 active**：`0 4px 16px rgba(255,106,0,0.15)`（点按反馈）
- **Prompt 输入**：`0 4px 14px rgba(255,106,0,0.12)`（聚焦吸引力）
- **TabBar / TopBar**：`backdrop-filter: blur(10px)` 毛玻璃，浮于内容之上

原则：**阴影只属于橙色强调元素**，普通卡片靠 `1px var(--border)` 描边区分层级，不做灰色大阴影。

## 7. Do's and Don'ts

**Do：**
- 卡片只放 6 要素（价格户型/押付/标签/已核实/通勤/联系），一切为"决策"服务
- 所有金额/面积/统计数字用 DM Sans（`.num`）
- 橙色只用于行动与强调（CTA、价格、激活、图标点缀），其余用中性色
- 信任信号可视化：已核实绿标、标签语义色、避坑提醒黄条
- 无图房源用橙色渐变占位 + 价格角标，保持卡片完整
- 用 8px divider 与 12px 间距组织信息密度，留白充足

**Don't：**
- 不要引入第二个主色（除橙外不再有并列强调色）
- 不要堆信息：房源卡不做"全字段罗列"，详情页才展开
- 不在公开卡片展示房号/手机号等敏感信息（房号仅审核可见）
- 不用纯黑 `#000` 作正文（用 0.85 rgba）
- 不用重灰色阴影做卡片层级（描边即可，阴影留给橙色元素）
- 发布按钮必须凸起居中，不要做成普通 Tab

## 8. Responsive Behavior

- **vw 适配**：375 设计稿经 postcss-px-to-viewport 转 vw，一套代码全机型
- **容器**：max 480px 居中（钉钉内宽度），`viewport-fit=cover` 适配刘海
- **触控目标**：主按钮/列表行 ≥ 44px 高；图标钮 ≥ 40px
- **横向滚动**：筛选 chips、上传格（3 列不滚）、图集 dots；隐藏滚动条
- **TabBar 安全区**：底部 padding 兼容 iOS 底部安全区（padding-bottom 8px）
- 详情 hero 高度固定 210px；卡片缩略图固定 96×96 避免布局抖动

## 9. Agent Prompt Guide

> 将本 DESIGN.md 与原型 `docs/design/alumni-anju-h5-prototype.html` 一起提供给前端 Agent。

**推荐 Prompt 模板（中文）：**

```
按 docs/design/2026-08-16-校友安居-前端实现方案.md 与 DESIGN.md 实现「首页·房源广场」：

- 视觉严格遵循 DESIGN.md：暖白底 #F5F5F4、活力橙 #FF6A00 主 CTA、
  大圆角（卡片 14px、胶囊 chips 999px）、价格用 DM Sans（.num）、
  卡片 6 要素布局（缩略图 96×96 渐变占位 + 价格角标 + 标题 + 元信息 + 标签 + 通勤）
- 结构：sticky topbar（品牌"校友安居"）→ 搜索框（placeholder 说人话找房）
  → 横向筛选 chips（选中橙底）→ 房源卡片流（12px 间距）→ 底部 5 Tab TabBar（中间凸起发布钮）
- 数据：GET /api/houses（后端已就绪），空态给"暂无房源"文案
- 样式令牌用 src/styles/tokens.css 的 CSS 变量，不写死色值
```

**风格关键词：** 温暖可信 · 活力橙 · 大圆角 · 卡片 6 要素 · DM Sans 数字 · 凸起发布钮 · 渐变缩略图 · 语义色标签 · 描边层级 · 少阴影
