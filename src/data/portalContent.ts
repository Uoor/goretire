export type PortalVariant = "home" | "ali";

export const portalContent = {
  brand: {
    name: "一起提前退休",
    caption: "大厂社区",
    symbol: "休"
  },
  navigation: [
    { href: "#home", label: "Slogan" },
    { href: "#insight", label: "内容观点" },
    { href: "#community", label: "子群服务" },
    // { href: "#community-invites", label: "加入社群" },
    { href: "#mutual-aid", label: "资源互助" }
  ],
  topics: [
    { label: "A股行情", accent: false, intro: "市场复盘和 IPO 观察，帮助快速掌握近期值得关注的变化", image: "assets/bots-a-shares.jpg", imageAlt: "近期市场与 IPO 复盘" },
    { label: "黄金行情", accent: false, intro: "追踪贵金属相关的市场动态与重要数据", image: "assets/bots-gold.jpg", imageAlt: "市场数据与行情复盘" },
    { label: "港美股行情", accent: false, intro: "关注港美股市场动态与新股信息", image: "assets/bots-hk-us-share.jpg", imageAlt: "港股新股市场观察" },
    { label: "汇率", accent: false, intro: "汇率变化与跨境生活相关信息整理", image: "assets/bots-exchange-rate.jpg", imageAlt: "跨境市场信息整理" },
    { label: "热点事件", accent: false, intro: "把近期事件脉络梳理清楚，再看它可能带来的影响", image: "assets/bots-hot-event.jpg", imageAlt: "近期科技行业热点" },
    { label: "大V持仓", accent: false, intro: "汇总公开持仓变化，作为继续查证的线索", image: "assets/bots-big-v.jpg", imageAlt: "公开市场信息复盘" },
    { label: "打新日历", accent: false, intro: "整理新股申购时间与相关公开信息", image: "assets/bots-calendar.jpg", imageAlt: "港股新股日历" },
    { label: "财报日历", accent: false, intro: "提前关注财报披露安排与公司动态", image: "assets/bots-financial-report.jpg", imageAlt: "财报与市场信息复盘" },
    { label: "AI 日报", accent: false, intro: "AI 产品、行业与 Agent 权限边界的每日精选", image: "assets/bots-ai-daily.jpg", imageAlt: "AI Agent 权限边界专题" },
    { label: "二手房价格推送", accent: false, intro: "按城市和片区追踪公开房价变化", image: "assets/bots-second-hand-house-price.jpg", imageAlt: "公开市场数据参考图" }
  ],
  // 大厂社群展示开关：true = 轮播展示全部公司；false = 只展示阿里卡片 + 「更多大厂社群接入中」占位卡（无轮播动效）
  // URL 拼接 ?debug=1 时强制置为 true，便于预览全量效果
  companyNetworksShowAll: typeof window !== "undefined" && new URLSearchParams(window.location.search).get("debug") === "1",
  companyNetworks: [
    {
      id: "ali",
      name: "阿里巴巴",
      description: "离职校友 SOP、校友内推与 6000+ 阿里校友的专属交流圈",
      link: "/ali",
      linkLabel: "进入阿里专区",
      qrImage: "assets/ali-community-qr.png",
      qrAlt: "阿里校友总群二维码"
    },
    { id: "bytedance", name: "字节跳动", description: "字节校友在职/跳槽交流与内推互助", qrImage: "assets/zijie-community-qr.jpg", qrAlt: "字节校友群二维码" },
    { id: "ctrip", name: "携程", description: "携程校友交流群，出行圈信息互通", qrImage: "assets/xiecheng-community-qr.jpg", qrAlt: "携程校友群二维码" },
    { id: "dewu", name: "得物", description: "得物校友交流群，潮流电商圈互助", qrImage: "assets/ali-community-qr.png", qrAlt: "得物校友群二维码" },
    { id: "xiaomi", name: "小米", description: "小米校友交流群，硬件与生态圈互助", qrImage: "assets/ali-community-qr.png", qrAlt: "小米校友群二维码" },
    { id: "hikvision", name: "海康威视", description: "海康校友交流群，安防圈信息互通", qrImage: "assets/ali-community-qr.png", qrAlt: "海康校友群二维码" }
  ],
  knowledge: {
    shared: [
      { icon: "⌁", title: "房产拐点知识库", detail: "周期观察 · 城市数据 · 决策框架", url: "https://alidocs.dingtalk.com/i/nodes/amweZ92PV6yogvAwTgXmqnk9WxEKBD6p" },
      { icon: "✦", title: "AI 每日日报", detail: "产品动态 · 行业趋势 · 实用工具", url: "https://alidocs.dingtalk.com/i/nodes/amweZ92PV6yogvAwTga3zezGWxEKBD6p" }
    ],
    ali: { icon: "⊟", title: "离职员工 SOP", detail: "离职准备 · 交接清单 · 离职后衔接", url: "https://alidocs.dingtalk.com/i/nodes/lyQod3RxJKvLO3RpI4rj0EpqVkb4Mw9r" }
  },
  communityGroups: [
    {
      title: "每日交流",
      subtitle: "一起聊市场，也聊变化",
      links: [
        { label: "A股交流", url:"https://qr.dingtalk.com/action/joingroup?code=v1,k1,nYV0Gc8LPGhoKjojhlz+49TPVGO5LYSTZypc/qZuX1FuRVJIwrSsXmL8oFqU5ajJ&_dt_no_comment=1&origin=11?",qrImage: "assets/groups-agujiaoliu.jpg", qrAlt: "A股交流社群二维码" },
        { label: "港美股交流",url: "https://qr.dingtalk.com/action/joingroup?code=v1,k1,bdVh4BikBa96rCbp9q3Cm9TPVGO5LYSTa6veUVEtLh5uRVJIwrSsXmL8oFqU5ajJ&_dt_no_comment=1&origin=11?", qrImage: "assets/groups-gangmeigujiaoliu.png", qrAlt: "港美股交流社群二维码" },
        { label: "AI 交流", url:"https://qr.dingtalk.com/action/joingroup?code=v1,k1,tWZcJD6gNGsdvHz0NL4QrtTPVGO5LYSTJsgSTKhBBiidR7ksupjDEA==&_dt_no_comment=1&origin=11?",qrImage: "assets/groups-aijiaoliu.jpg", qrAlt: "AI 交流社群二维码" }
      ]
    },
    {
      title: "金融工具",
      subtitle: "理解工具，理性做选择",
      links: [
        { label: "信贷资源汇总", url: "https://alidocs.dingtalk.com/i/p/O1pMzN6O07ezBnePqWXwPVvj98E19m31"},
        { label: "融资服务", url: "https://alidocs.dingtalk.com/notable/share/form/v014j6OJ5PzGG5YEq3p_dv19yqvsgs3oebp3pcjys_1qX0QQ0?source=link" },
        { label: "节税专区", url: "https://alidocs.dingtalk.com/i/nodes/NZQYprEoWobMqeRpCqyRp7Xz81waOeDk?utm_scene=team_space", },
        { label: "港险避坑", url: "https://alidocs.dingtalk.com/i/nodes/P7QG4Yx2Jp3jl702Hg01n16bV9dEq3XD?utm_scene=team_space",  },
        { label: "港美股交流", url: "https://alidocs.dingtalk.com/i/nodes/XPwkYGxZV3y5vRlwTprqlYjE8AgozOKL?utm_scene=team_space",  },
        { label: "A股证券开户", url: "https://alidocs.dingtalk.com/i/nodes/kDnRL6jAJMDrw3bztXxjpRZRVyMoPYe1?utm_scene=team_space",  }

      ]
    },
    {
      title: "工作生活",
      subtitle: "让校友关系产生真实价值",
      links: [
        { label: "校友租房", url: "https://goretire.cn/ali/house/#/", qrPopover: true, qrImage: "assets/groups-xiaoyouzufang.jpg", qrAlt: "校友租房社群二维码" },
        { label: "招聘内推", url: "https://www.axureshow.com/project/puAKIzIU/"},
        { label: "香港身份 DIY", url:"https://qr.dingtalk.com/action/joingroup?code=v1,k1,zzVGmjTxMHzFdLERPcucsdTPVGO5LYSTFwlWOqLhHXBuRVJIwrSsXmL8oFqU5ajJ&_dt_no_comment=1&origin=11?",qrImage: "assets/groups-xianggangshenfen.png", qrAlt: "香港身份 DIY 社群二维码" },
        { label: "团建轰趴", url:"https://qr.dingtalk.com/action/joingroup?code=v1,k1,NPqb2xjkAVTlcriThPGrodTPVGO5LYSTTcEEFV7bwnhuRVJIwrSsXmL8oFqU5ajJ&_dt_no_comment=1&origin=11?",qrImage: "assets/groups-bieshuhongpa.jpg", qrAlt: "团建轰趴社群二维码" },
        { label: "育儿交流", url:"https://qr.dingtalk.com/action/joingroup?code=v1,k1,bK1FbZglRJVLWYTb9mZQZtTPVGO5LYSTZxI4mT1g0rBuRVJIwrSsXmL8oFqU5ajJ&_dt_no_comment=1&origin=11?",qrImage: "assets/groups-yuerjiaoliu.png", qrAlt: "育儿交流社群二维码" }
      ]
    }
  ],
  footer: {
    tagline: "提升认知 · 拉平信息差 · 互助避坑 · 善用金融工具 · 探索更自由人生",
    qrCodes: [
      {
        label: "钉钉扫码加入",
        badge: "仅限阿里校友",
        image: "assets/ali-community-qr.png",
        alt: "一起提前退休总群二维码",
        notes: [
          "在职/离职校友均可钉钉申请入群，已有6000+阿里校友加入",
          "阿里钉/蚂蚁钉申请可快速审批，个人钉申请需备注阿里身份信息"
        ]
      },
      {
        qrImage: "assets/wechat-qrcode-only.jpg",
        qrAlt: "提钱退休笔记公众号二维码",
        searchImage: "assets/hiorizental-wechat.jpg",
        searchAlt: "微信搜一搜 提钱退休笔记"
      }
    ],
    filing: { label: "浙ICP备2026071844号-1", url: "https://beian.miit.gov.cn/" }
  }
} as const;
