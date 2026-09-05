window.SITE_CONTENT = {
  stories: [
    {
      date: "09.04",
      type: "AI 工具",
      title: "把 AI 当同事之前，先把权限当回事",
      summary: "能力越强，越要知道哪些动作可以自动完成，哪些必须由人确认。",
      url: "#note-ai"
    },
    {
      date: "09.02",
      type: "判断方法",
      title: "信息很多的时候，我怎样避免被结论带着走",
      summary: "先找来源，再看前提，最后区分事实、解释和还没验证的猜测。",
      url: "#note-signal"
    },
    {
      date: "08.29",
      type: "个人清单",
      title: "离开一个岗位前，先把重要的事情整理好",
      summary: "项目、材料、权限和关系，最好在当下逐项确认，而不是离开以后再补。",
      url: "#tool-departure"
    }
  ],
  notes: [
    {
      id: "note-ai",
      date: "09.04",
      type: "工具与边界",
      title: "AI 能做更多以后，什么还应该留在人手里？",
      summary: "起草和整理可以交给工具，授权、发送、承诺与不可逆操作仍需要人确认。真正重要的不是拒绝自动化，而是把边界设计在行动发生之前。",
      image: "assets/ai-boundary.png",
      source: "阅读官方说明",
      url: "https://claude.com/blog/claudes-memory-works-everywhere-and-you-decide-whats-in-it"
    },
    {
      id: "note-signal",
      date: "09.02",
      type: "信息与判断",
      title: "信息很多，不代表判断更清楚",
      summary: "我会先问三个问题：它从哪里来，适用于什么情况，还有什么没有被说出来。把这三件事补齐以后，结论通常没那么绝对。"
    },
    {
      id: "note-small-step",
      date: "08.30",
      type: "选择与行动",
      title: "想不清楚时，先找一个可逆的小动作",
      summary: "不是每个决定都需要一次想透。先做成本低、能得到反馈、也允许回头的一步，往往比继续空想更接近答案。"
    }
  ],
  departure: {
    before: [
      { id: "b1", title: "盘点项目", note: "状态、风险、文档和可能的接手人" },
      { id: "b2", title: "核对材料", note: "确认之后可能需要的工作证明与记录" },
      { id: "b3", title: "安排节奏", note: "给交接、休息和下一阶段留出时间" }
    ],
    week: [
      { id: "w1", title: "完成交接", note: "先搭框架，再补资料与关键联系人" },
      { id: "w2", title: "整理权限", note: "工作资料与个人资料严格分开" },
      { id: "w3", title: "保留关系", note: "向值得长期联系的人认真告别" }
    ],
    day: [
      { id: "d1", title: "拿齐证明", note: "确认需要留存的必要材料" },
      { id: "d2", title: "检查设备", note: "退出个人账号，归还公司设备" },
      { id: "d3", title: "确认联系人", note: "知道后续问题应该找谁" }
    ],
    after: [
      { id: "a1", title: "做一次复盘", note: "写下想保留与不再重复的经验" },
      { id: "a2", title: "恢复节奏", note: "休息、探索和行动不必挤在同一天" },
      { id: "a3", title: "重新联系朋友", note: "带着具体问题开启新的交流" }
    ]
  }
};
