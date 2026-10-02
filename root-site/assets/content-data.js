window.COMMUNITY_CONTENT = Object.freeze({
  updatedAt: "2026.09.05 · 近期整理",
  groups: [
    {
      id: "general",
      shortName: "总群",
      name: "提前退休 · 总群",
      mark: "ER",
      presence: "话题最广",
      title: "从离职准备，聊到了怎样保留选择权",
      summary: "有人在核对社保和补偿，有人分享杭州租房，也有人讨论周末能不能先验证一个小项目。",
      topics: [
        { label: "离职之后", text: "社保怎么接、文件留什么、现金流要准备多久。" },
        { label: "生活选择", text: "继续租还是考虑买，先把通勤和家庭计划摊开聊。" },
        { label: "做点事情", text: "从熟人、小范围测试开始，先验证有没有真实需求。" }
      ]
    },
    {
      id: "stock",
      shortName: "A 股群",
      name: "投资学习 · A 股群",
      mark: "A",
      presence: "讨论最热",
      title: "涨跌之外，大家更关心怎么少犯错",
      summary: "盘中变化和打新是高频话题。观点会分成很多派，但讨论会回到仓位、估值和自己的承受力。",
      topics: [
        { label: "盘中变化", text: "先找公开信息和成交变化，不只看一句情绪化结论。" },
        { label: "打新日历", text: "发行价、行业估值和近期破发记录被放在一起比较。" },
        { label: "大跌之后", text: "先看仓位，再看逻辑，最后才决定要不要动作。" }
      ]
    },
    {
      id: "ai",
      shortName: "AI 群",
      name: "AI 实践 · 交流群",
      mark: "AI",
      presence: "问答密集",
      title: "不只追新模型，更关心怎样用到工作里",
      summary: "产品、研发和运营都在交换自己的工作流。工具能做什么很重要，权限开到哪里同样重要。",
      topics: [
        { label: "岗位提效", text: "把团队规则、资料和模板整理好，再让 AI 参与周报与复盘。" },
        { label: "Agent 边界", text: "起草可以自动，授权、发送和承诺仍保留人工确认。" },
        { label: "互助排错", text: "把失败过程也发出来，别人更容易帮你找到问题。" }
      ]
    }
  ],
  signals: [
    {
      mark: "AI",
      time: "今天值得看",
      title: "Agent 越能执行，权限越要分层",
      detail: "把读取、修改、发送、付款拆开授权，并保留日志和回滚。",
      image: "assets/ai-boundary.png",
      imageAlt: "AI Agent 权限边界专题"
    },
    {
      mark: "房",
      time: "方法更新",
      title: "判断房产，先分城市、板块和小区",
      detail: "同一座城里也不是一个市场。先看供需，再谈自己的租买选择。"
    },
    {
      mark: "新",
      time: "打新提醒",
      title: "申购前，把三个数字放在一起",
      detail: "发行价、行业估值、近期破发率。提醒只负责到点，决定仍由自己做。"
    }
  ],
  subscriptions: ["群聊摘要", "AI 日报", "房产观察", "打新日历", "市场复盘"],
  question: "如果暂时不用考虑收入，你最想把时间用在哪里？",
  property: {
    rent: {
      intro: "租房不是过渡方案。把通勤、总成本和稳定性排好顺序，就能少被房源文案牵着走。",
      questions: ["算过房租之外的通勤与搬家成本", "明确自己最不能妥协的一项", "看过同片区至少三套可比房源"],
      result: ["先把总成本算完整", "边界正在变清楚", "可以带着清单去看房"]
    },
    buy: {
      intro: "买房先看会住多久、收入能否波动，以及付完首付后还剩多少安全空间。",
      questions: ["预计在这座城市生活五年以上", "月供不超过家庭稳定收入的三分之一", "付完首付仍留有一年必要支出"],
      result: ["先确认城市与家庭计划", "再做一次压力测试", "准备度不错，继续比较具体房源"]
    },
    sell: {
      intro: "卖房先说清为什么卖。现金流、置换、离开城市和资产调整，对应完全不同的时间表。",
      questions: ["已经明确卖房后的居住安排", "知道自己最多可以等待多久", "算过税费、中介和资金占用成本"],
      result: ["先明确卖房原因", "时间表正在清楚", "可以开始准备挂牌材料"]
    }
  },
  wealthBriefs: [
    {
      label: "市场学习",
      title: "A 股 · 港美股 · 打新",
      summary: "看日历、异动与复盘，也看风险边界。",
      points: ["事件提醒", "估值比较", "群友复盘"],
      linkLabel: "看今日信号",
      url: "#pulse"
    },
    {
      label: "跨境生活",
      title: "身份 · 港卡 · 安排",
      summary: "先看真实生活需求，再比较路径与成本。",
      points: ["适用场景", "时间成本", "长期维护"],
      linkLabel: "打开资料",
      url: "https://alidocs.dingtalk.com/i/spaces/QqWXwEVodvbo5G31/overview"
    },
    {
      label: "家庭财务",
      title: "现金流 · 保障 · 计划",
      summary: "先守住家庭底盘，再讨论更远的目标。",
      points: ["应急空间", "固定支出", "共同决策"],
      linkLabel: "加入讨论",
      url: "#connect"
    }
  ],
  journeys: [
    {
      id: "working",
      tab: "还在岗位",
      label: "让工作轻一点",
      title: "先把时间拿回来",
      summary: "从 AI 工具、行业信息和工作方法里，挑一个真正能减少重复劳动的动作。",
      actions: ["AI 工作流", "行业摘要", "能力更新"],
      toolLabel: "这周可以试",
      toolTitle: "整理一份团队常用规则",
      toolNote: "让 AI 先读懂上下文，再参与起草和复盘。",
      url: "#pulse",
      linkLabel: "看 AI 群在聊什么"
    },
    {
      id: "leaving",
      tab: "准备离开",
      label: "把底盘理清",
      title: "离开前，先把该留的留下",
      summary: "工作文件、证明、补偿、社保和现金流，按时间顺序核对，少给未来的自己留麻烦。",
      actions: ["离职清单", "现金缓冲", "经验问答"],
      toolLabel: "现有资料",
      toolTitle: "大厂离职 SOP",
      toolNote: "从决定前到离开后，按阶段核对。",
      url: "https://alidocs.dingtalk.com/i/spaces/QqWXwEVodvbo5G31/overview",
      linkLabel: "打开离职清单"
    },
    {
      id: "searching",
      tab: "寻找机会",
      label: "让别人看见你",
      title: "先把自己说清楚",
      summary: "你做过什么、想去哪里、需要怎样的连接。具体，通常比一句“帮忙留意”更有用。",
      actions: ["自我介绍", "岗位信息", "校友连接"],
      toolLabel: "表达模板",
      toolTitle: "四句话介绍自己",
      toolNote: "我是谁、做过什么、正在找什么、希望怎样连接。",
      url: "#connect",
      linkLabel: "去互助区"
    },
    {
      id: "building",
      tab: "想做点事",
      label: "从小范围验证",
      title: "先找到第一批真实反馈",
      summary: "创业、副业或一个小项目，都可以先讲清楚服务谁、解决什么，以及希望遇到怎样的伙伴。",
      actions: ["项目共创", "资源交换", "合作验证"],
      toolLabel: "合作前先写",
      toolTitle: "一张资源说明卡",
      toolNote: "来源、适用人群、边界和利益关系，一次说清。",
      url: "#connect",
      linkLabel: "聊聊合作"
    }
  ],
  resources: [
    { label: "住房", title: "校友转租与房源", detail: "区域、价格、入住时间与来源先核对。" },
    { label: "机会", title: "岗位与项目连接", detail: "带着具体背景和需求来，双方同意后引荐。" },
    { label: "经验", title: "走过的人来回答", detail: "离职、转型、跨境生活与 AI 实践。" },
    { label: "合作", title: "福利与社区共创", detail: "先说明适用人群、有效期和利益关系。" }
  ]
});
