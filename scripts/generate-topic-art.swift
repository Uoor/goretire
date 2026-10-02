import AppKit
import Foundation

let width = 1200
let height = 675
let outputDirectory = URL(fileURLWithPath: FileManager.default.currentDirectoryPath).appendingPathComponent("assets")

struct Topic {
  let file: String
  let title: String
  let subtitle: String
  let index: String
  let accent: NSColor
  let kind: String
}

let topics = [
  Topic(file: "topic-a-share.png", title: "A股行情", subtitle: "结构变化 · 资金脉络 · 市场温度", index: "01 / MARKET", accent: color(0xE65A3A), kind: "shares"),
  Topic(file: "topic-gold.png", title: "黄金行情", subtitle: "避险需求 · 利率预期 · 长期锚点", index: "02 / METALS", accent: color(0xC88C2B), kind: "gold"),
  Topic(file: "topic-global-equities.png", title: "港美股行情", subtitle: "跨市场观察 · 交易时区 · 新股动态", index: "03 / GLOBAL", accent: color(0x337A83), kind: "global"),
  Topic(file: "topic-fx.png", title: "汇率", subtitle: "币种流动 · 价差变化 · 跨境视角", index: "04 / CURRENCY", accent: color(0x398879), kind: "fx"),
  Topic(file: "topic-hotspots.png", title: "热点事件", subtitle: "事件脉络 · 影响路径 · 时间窗口", index: "05 / SIGNALS", accent: color(0xD85D4B), kind: "hotspots"),
  Topic(file: "topic-holdings.png", title: "大V持仓", subtitle: "公开披露 · 组合变化 · 观点线索", index: "06 / POSITIONS", accent: color(0x73804C), kind: "holdings"),
  Topic(file: "topic-ipo-calendar.png", title: "打新日历", subtitle: "申购节点 · 发行信息 · 风险观察", index: "07 / CALENDAR", accent: color(0xC65A36), kind: "calendar"),
  Topic(file: "topic-earnings.png", title: "财报日历", subtitle: "披露安排 · 经营信号 · 周期比较", index: "08 / REPORTS", accent: color(0x397B77), kind: "earnings"),
  Topic(file: "topic-ai.png", title: "AI 日报", subtitle: "产品进展 · 行业趋势 · Agent 边界", index: "09 / COMPUTE", accent: color(0x3D78A0), kind: "ai"),
  Topic(file: "topic-home-prices.png", title: "二手房价格推送", subtitle: "片区差异 · 成交节奏 · 居住选择", index: "10 / HOUSING", accent: color(0x55806D), kind: "housing")
]

func color(_ hex: UInt32, alpha: CGFloat = 1) -> NSColor {
  NSColor(calibratedRed: CGFloat((hex >> 16) & 0xff) / 255, green: CGFloat((hex >> 8) & 0xff) / 255, blue: CGFloat(hex & 0xff) / 255, alpha: alpha)
}

func topRect(_ x: CGFloat, _ y: CGFloat, _ w: CGFloat, _ h: CGFloat) -> NSRect {
  NSRect(x: x, y: CGFloat(height) - y - h, width: w, height: h)
}

func fillRect(_ x: CGFloat, _ y: CGFloat, _ w: CGFloat, _ h: CGFloat, _ fill: NSColor) {
  fill.setFill()
  NSBezierPath(rect: topRect(x, y, w, h)).fill()
}

func rounded(_ x: CGFloat, _ y: CGFloat, _ w: CGFloat, _ h: CGFloat, _ radius: CGFloat, _ fill: NSColor, stroke: NSColor? = nil, lineWidth: CGFloat = 1) {
  let path = NSBezierPath(roundedRect: topRect(x, y, w, h), xRadius: radius, yRadius: radius)
  fill.setFill()
  path.fill()
  if let stroke {
    stroke.setStroke()
    path.lineWidth = lineWidth
    path.stroke()
  }
}

func ellipse(_ x: CGFloat, _ y: CGFloat, _ w: CGFloat, _ h: CGFloat, _ fill: NSColor, stroke: NSColor? = nil, lineWidth: CGFloat = 1) {
  let path = NSBezierPath(ovalIn: topRect(x, y, w, h))
  fill.setFill()
  path.fill()
  if let stroke {
    stroke.setStroke()
    path.lineWidth = lineWidth
    path.stroke()
  }
}

func line(_ points: [(CGFloat, CGFloat)], _ stroke: NSColor, width: CGFloat = 1, dashed: Bool = false) {
  guard let first = points.first else { return }
  let path = NSBezierPath()
  path.lineWidth = width
  if dashed { path.setLineDash([5, 6], count: 2, phase: 0) }
  path.move(to: NSPoint(x: first.0, y: CGFloat(height) - first.1))
  for point in points.dropFirst() {
    path.line(to: NSPoint(x: point.0, y: CGFloat(height) - point.1))
  }
  stroke.setStroke()
  path.stroke()
}

func text(_ value: String, x: CGFloat, y: CGFloat, width: CGFloat, height: CGFloat, size: CGFloat, fill: NSColor, weight: NSFont.Weight = .regular, tracking: CGFloat = 0) {
  let font = NSFont(name: weight == .bold ? "HiraginoSans-W6" : "HiraginoSans-W3", size: size) ?? NSFont.systemFont(ofSize: size, weight: weight)
  let paragraph = NSMutableParagraphStyle()
  paragraph.lineBreakMode = .byTruncatingTail
  let attributes: [NSAttributedString.Key: Any] = [
    .font: font,
    .foregroundColor: fill,
    .paragraphStyle: paragraph,
    .kern: tracking
  ]
  NSAttributedString(string: value, attributes: attributes).draw(in: topRect(x, y, width, height))
}

func grid(_ x: CGFloat, _ y: CGFloat, _ w: CGFloat, _ h: CGFloat, columns: Int, rows: Int, accent: NSColor) {
  for column in 0...columns {
    let xx = x + w * CGFloat(column) / CGFloat(columns)
    line([(xx, y), (xx, y + h)], color(0x6B6056, alpha: column == 0 ? 0.13 : 0.075), width: 1)
  }
  for row in 0...rows {
    let yy = y + h * CGFloat(row) / CGFloat(rows)
    line([(x, yy), (x + w, yy)], color(0x6B6056, alpha: row == rows ? 0.13 : 0.075), width: 1)
  }
  line([(x, y + h), (x + w, y + h)], accent.withAlphaComponent(0.55), width: 1)
}

func drawShares(_ accent: NSColor) {
  rounded(565, 150, 565, 405, 20, color(0xFFFCF8), stroke: color(0xE9DED3), lineWidth: 1)
  grid(600, 190, 490, 280, columns: 7, rows: 4, accent: accent)
  let values: [(CGFloat, CGFloat)] = [(620,385),(678,350),(736,368),(794,290),(852,320),(910,248),(968,270),(1026,218)]
  for index in 0..<values.count {
    let (x, y) = values[index]
    let candleHeight: CGFloat = [35,48,28,64,39,56,32,71][index]
    let rising = index != 2 && index != 4 && index != 6
    let tone = rising ? accent : color(0x3F8A73)
    line([(x + 13, y - 14), (x + 13, y + candleHeight + 14)], tone.withAlphaComponent(0.75), width: 2)
    rounded(x, y, 26, candleHeight, 3, tone)
  }
  line([(620, 413), (678, 390), (736, 395), (794, 354), (852, 346), (910, 311), (968, 305), (1039, 260)], color(0x314B49, alpha: 0.72), width: 2)
  text("沪深结构", x: 604, y: 492, width: 140, height: 24, size: 13, fill: color(0x777069))
  text("资金节奏", x: 785, y: 492, width: 140, height: 24, size: 13, fill: color(0x777069))
  text("市场温度", x: 964, y: 492, width: 140, height: 24, size: 13, fill: color(0x777069))
}

func drawGold(_ accent: NSColor) {
  ellipse(680, 150, 370, 370, color(0xF6E4C6), stroke: color(0xD7B77F, alpha: 0.7), lineWidth: 2)
  ellipse(704, 174, 322, 322, color(0xEBCB92), stroke: color(0xFFFFFF, alpha: 0.85), lineWidth: 2)
  ellipse(735, 205, 260, 260, color(0xF6E8D0), stroke: color(0xC88C2B, alpha: 0.65), lineWidth: 2)
  ellipse(790, 260, 150, 150, accent, stroke: color(0xA66A20), lineWidth: 2)
  text("Au", x: 816, y: 291, width: 100, height: 80, size: 54, fill: color(0xFFF9EE), weight: .bold)
  line([(588, 510), (658, 494), (728, 501), (798, 477), (868, 485), (938, 458), (1048, 468)], accent, width: 3)
  for point in [(588,510),(728,501),(868,485),(1048,468)] { ellipse(point.0 - 5, point.1 - 5, 10, 10, accent) }
  text("SPOT / 01", x: 570, y: 186, width: 100, height: 20, size: 11, fill: color(0x8F754E), weight: .bold, tracking: 1.2)
}

func drawGlobal(_ accent: NSColor) {
  rounded(565, 158, 565, 386, 20, color(0xF5FAF8), stroke: color(0xD9E5E1), lineWidth: 1)
  ellipse(624, 220, 140, 140, color(0xE5F0EC), stroke: accent.withAlphaComponent(0.65), lineWidth: 2)
  ellipse(920, 220, 140, 140, color(0xE7EFF5), stroke: color(0x4B7591, alpha: 0.65), lineWidth: 2)
  text("HK", x: 656, y: 264, width: 80, height: 48, size: 32, fill: accent, weight: .bold)
  text("NY", x: 951, y: 264, width: 80, height: 48, size: 32, fill: color(0x416B89), weight: .bold)
  line([(758,280),(818,228),(874,280),(818,332),(758,280)], color(0x74A29A, alpha: 0.5), width: 2)
  line([(766,280),(866,280)], color(0x4B8790, alpha: 0.6), width: 2)
  ellipse(807, 269, 22, 22, accent)
  rounded(602, 412, 480, 82, 12, color(0xFFFFFF), stroke: color(0xDFE8E2), lineWidth: 1)
  text("亚洲开市", x: 626, y: 430, width: 110, height: 24, size: 14, fill: color(0x526C65), weight: .bold)
  text("跨时区", x: 786, y: 430, width: 110, height: 24, size: 14, fill: color(0x526C65), weight: .bold)
  text("美洲收市", x: 940, y: 430, width: 110, height: 24, size: 14, fill: color(0x526C65), weight: .bold)
  line([(626,472),(740,472),(786,472),(900,472),(940,472),(1056,472)], accent.withAlphaComponent(0.35), width: 2)
}

func drawFX(_ accent: NSColor) {
  let nodes: [(CGFloat, CGFloat, String, UInt32)] = [(630,260,"¥",0xE7F1E8),(815,398,"$",0xE7EFF5),(1000,240,"€",0xF5E9D8)]
  line([(685,291),(790,365),(842,365),(955,280)], accent.withAlphaComponent(0.55), width: 4)
  line([(685,315),(790,388),(842,388),(955,303)], color(0xD9A45A, alpha: 0.45), width: 2)
  for node in nodes {
    ellipse(node.0 - 66, node.1 - 66, 132, 132, color(node.3), stroke: accent.withAlphaComponent(0.7), lineWidth: 2)
    text(node.2, x: node.0 - 32, y: node.1 - 36, width: 70, height: 76, size: 48, fill: accent, weight: .bold)
  }
  rounded(570, 500, 530, 1, 1, color(0xDDE4DC))
  text("CNY", x: 604, y: 515, width: 90, height: 22, size: 12, fill: color(0x6C7771), weight: .bold, tracking: 1)
  text("USD", x: 790, y: 515, width: 90, height: 22, size: 12, fill: color(0x6C7771), weight: .bold, tracking: 1)
  text("EUR", x: 976, y: 515, width: 90, height: 22, size: 12, fill: color(0x6C7771), weight: .bold, tracking: 1)
}

func drawHotspots(_ accent: NSColor) {
  rounded(570, 160, 545, 390, 18, color(0xFFFAF7), stroke: color(0xEADBD2), lineWidth: 1)
  line([(642,228),(642,478)], accent.withAlphaComponent(0.35), width: 3)
  let events: [(CGFloat, String, String)] = [(244,"政策窗口","规则与预期"),(330,"行业变化","供需与节奏"),(416,"公司动态","公告与回应"),(502,"市场反馈","价格与成交")]
  for (y,title,detail) in events {
    ellipse(628, y - 7, 28, 28, color(0xFFFAF7), stroke: accent, lineWidth: 3)
    ellipse(636, y + 1, 12, 12, accent)
    text(title, x: 680, y: y - 9, width: 165, height: 26, size: 18, fill: color(0x39342F), weight: .bold)
    text(detail, x: 875, y: y - 8, width: 190, height: 24, size: 13, fill: color(0x81776E))
    line([(680,y + 31),(1070,y + 31)], color(0xE8DED5), width: 1)
  }
}

func drawHoldings(_ accent: NSColor) {
  let center = NSPoint(x: 750, y: CGFloat(height) - 353)
  let segments: [(CGFloat, NSColor)] = [(118,color(0x74834F)),(82,color(0xC78D46)),(66,color(0xD76548)),(54,color(0x648B8C)),(40,color(0xD9C7A8))]
  var start: CGFloat = 88
  for (span,tone) in segments {
    let path = NSBezierPath()
    path.move(to: center)
    path.appendArc(withCenter: center, radius: 138, startAngle: start, endAngle: start + span, clockwise: false)
    path.close()
    tone.setFill()
    path.fill()
    start += span
  }
  ellipse(680, 283, 140, 140, color(0xFFFBF7))
  text("持仓", x: 714, y: 322, width: 90, height: 26, size: 17, fill: color(0x625D54), weight: .bold)
  text("观察", x: 716, y: 352, width: 86, height: 24, size: 13, fill: color(0x8C8378))
  rounded(935, 205, 168, 302, 14, color(0xFFFCF8), stroke: color(0xE6DDD2), lineWidth: 1)
  let rows: [(CGFloat, String, String, UInt32)] = [(239,"科技","32%",0x73804C),(299,"消费","24%",0xC88C2B),(359,"医疗","19%",0xD76548),(419,"制造","15%",0x4D7F80),(479,"其他","10%",0xD9C7A8)]
  for (y,label,share,tone) in rows {
    ellipse(954, y - 1, 12, 12, color(tone))
    text(label, x: 975, y: y - 7, width: 68, height: 22, size: 13, fill: color(0x49443D))
    text(share, x: 1044, y: y - 7, width: 44, height: 22, size: 12, fill: accent, weight: .bold)
  }
}

func drawCalendar(_ accent: NSColor) {
  rounded(590, 154, 508, 400, 18, color(0xFFFCF8), stroke: color(0xE8DCD0), lineWidth: 1)
  rounded(590, 154, 508, 76, 18, color(0xF7E8D9))
  fillRect(590, 210, 508, 20, color(0xF7E8D9))
  text("JULY", x: 624, y: 177, width: 120, height: 36, size: 26, fill: accent, weight: .bold, tracking: 1.1)
  text("申购日历 / 2026", x: 844, y: 187, width: 215, height: 24, size: 13, fill: color(0x81766B))
  let labels = ["一","二","三","四","五","六","日"]
  for column in 0..<7 { text(labels[column], x: 622 + CGFloat(column) * 64, y: 250, width: 36, height: 22, size: 12, fill: color(0x8B8177), weight: .bold) }
  let highlighted: Set<Int> = [6,13,21,28]
  for day in 1...35 {
    let column = (day - 1) % 7
    let row = (day - 1) / 7
    let x = 612 + CGFloat(column) * 64
    let y = 286 + CGFloat(row) * 48
    if highlighted.contains(day) { rounded(x, y, 40, 34, 8, accent) }
    text("\(day)", x: x + 9, y: y + 6, width: 28, height: 23, size: 13, fill: highlighted.contains(day) ? color(0xFFFFFF) : color(0x4F4942), weight: highlighted.contains(day) ? .bold : .regular)
  }
  ellipse(1040, 168, 14, 14, accent)
}

func drawEarnings(_ accent: NSColor) {
  rounded(575, 158, 540, 394, 18, color(0xFCFBF7), stroke: color(0xDFE5DC), lineWidth: 1)
  text("季度观察", x: 610, y: 185, width: 120, height: 22, size: 13, fill: color(0x797A6B), weight: .bold)
  text("2026 / Q2", x: 928, y: 185, width: 140, height: 22, size: 13, fill: accent, weight: .bold)
  grid(612, 236, 462, 206, columns: 6, rows: 4, accent: accent)
  let bars: [(CGFloat, CGFloat)] = [(650,128),(710,102),(770,148),(830,117),(890,164),(950,139),(1010,184)]
  for index in 0..<bars.count {
    let (x,h) = bars[index]
    let tone = index == bars.count - 1 ? accent : color(0x8EB3A1, alpha: 0.76)
    rounded(x, 442 - h, 32, h, 5, tone)
  }
  line([(646,354),(706,334),(766,349),(826,311),(886,319),(946,286),(1024,298)], color(0xC77E49), width: 3)
  for point in [(646,354),(706,334),(766,349),(826,311),(886,319),(946,286),(1024,298)] { ellipse(point.0 - 4, point.1 - 4, 8, 8, color(0xC77E49)) }
  text("营收", x: 618, y: 480, width: 76, height: 20, size: 12, fill: color(0x6F776F))
  text("利润率", x: 762, y: 480, width: 88, height: 20, size: 12, fill: color(0x6F776F))
  text("经营现金流", x: 914, y: 480, width: 138, height: 20, size: 12, fill: color(0x6F776F))
}

func drawAI(_ accent: NSColor) {
  rounded(590, 164, 510, 385, 20, color(0xF6FAFC), stroke: color(0xDCE7EC), lineWidth: 1)
  rounded(749, 276, 194, 158, 22, color(0xE3EFF4), stroke: accent.withAlphaComponent(0.8), lineWidth: 2)
  rounded(790, 313, 112, 84, 14, color(0xFFFFFF), stroke: color(0x8FB3C1), lineWidth: 2)
  text("AI", x: 820, y: 331, width: 70, height: 48, size: 34, fill: accent, weight: .bold)
  let nodes: [(CGFloat,CGFloat)] = [(664,232),(760,205),(932,213),(1033,276),(1018,458),(908,500),(711,470),(648,352)]
  let center=(846.0,355.0)
  for node in nodes { line([(center.0,center.1),(node.0,node.1)], color(0x77A0B2, alpha: 0.45), width: 2) }
  for (index,node) in nodes.enumerated() {
    let diameter: CGFloat = index % 3 == 0 ? 27 : 17
    ellipse(node.0 - diameter/2, node.1 - diameter/2, diameter, diameter, index % 2 == 0 ? accent : color(0x8CB7C5))
  }
  text("模型", x: 636, y: 520, width: 70, height: 18, size: 11, fill: color(0x69818D), weight: .bold, tracking: 1)
  text("工具", x: 1006, y: 520, width: 70, height: 18, size: 11, fill: color(0x69818D), weight: .bold, tracking: 1)
}

func drawHousing(_ accent: NSColor) {
  rounded(565, 154, 565, 400, 18, color(0xF8FAF6), stroke: color(0xDDE5D9), lineWidth: 1)
  grid(602, 206, 492, 265, columns: 6, rows: 4, accent: accent)
  let heights: [CGFloat] = [80,132,102,172,142,208]
  let base: CGFloat = 470
  for index in 0..<heights.count {
    let x = 628 + CGFloat(index) * 74
    let h = heights[index]
    let shade = index == 3 ? accent : color(0xA8B9A4)
    rounded(x, base - h, 46, h, 7, shade.withAlphaComponent(index == 3 ? 0.95 : 0.76))
    let rows = Int(h / 32)
    for row in 0..<rows {
      for col in 0..<2 {
        rounded(x + 9 + CGFloat(col) * 17, base - h + 13 + CGFloat(row) * 28, 7, 11, 2, color(0xF9FAF5, alpha: 0.85))
      }
    }
  }
  line([(625,407),(700,373),(774,389),(848,323),(922,336),(996,282),(1065,260)], color(0xC78052), width: 3)
  for point in [(625,407),(774,389),(922,336),(1065,260)] { ellipse(point.0 - 5, point.1 - 5, 10, 10, color(0xC78052)) }
  text("片区成交 / INDEX", x: 611, y: 500, width: 220, height: 18, size: 11, fill: color(0x758276), weight: .bold, tracking: 1)
}

func drawMotif(_ kind: String, accent: NSColor) {
  switch kind {
  case "shares": drawShares(accent)
  case "gold": drawGold(accent)
  case "global": drawGlobal(accent)
  case "fx": drawFX(accent)
  case "hotspots": drawHotspots(accent)
  case "holdings": drawHoldings(accent)
  case "calendar": drawCalendar(accent)
  case "earnings": drawEarnings(accent)
  case "ai": drawAI(accent)
  case "housing": drawHousing(accent)
  default: break
  }
}

func render(_ topic: Topic) throws {
  let bitmap = NSBitmapImageRep(pixelsWide: width, pixelsHigh: height, bitsPerSample: 8, samplesPerPixel: 4, hasAlpha: true, isPlanar: false, colorSpaceName: .deviceRGB, bytesPerRow: 0, bitsPerPixel: 0)
  guard let context = NSGraphicsContext(bitmapImageRep: bitmap) else { throw NSError(domain: "TopicArt", code: 1) }
  NSGraphicsContext.saveGraphicsState()
  NSGraphicsContext.current = context
  context.imageInterpolation = .high
  fillRect(0, 0, CGFloat(width), CGFloat(height), color(0xF8F5EF))

  let bgPath = NSBezierPath(rect: topRect(0, 0, CGFloat(width), CGFloat(height)))
  NSGradient(colors: [color(0xFFFCF8), color(0xF7F2EA)])?.draw(in: bgPath, angle: -12)
  for index in 0...12 {
    let x = CGFloat(index) * 100
    line([(x,0),(x,CGFloat(height))], color(0x8D7E6C, alpha: 0.045), width: 1)
  }
  for index in 0...6 {
    let y = CGFloat(index) * 100
    line([(0,y),(CGFloat(width),y)], color(0x8D7E6C, alpha: 0.045), width: 1)
  }
  ellipse(960, -160, 400, 400, topic.accent.withAlphaComponent(0.055))
  line([(70, 55),(1130,55)], color(0xD9CFC2, alpha: 0.8), width: 1)
  text(topic.index, x: 72, y: 76, width: 230, height: 24, size: 12, fill: topic.accent, weight: .bold, tracking: 1.8)
  rounded(72, 122, 42, 4, 2, topic.accent)
  text(topic.title, x: 72, y: 153, width: 470, height: 72, size: topic.title.count > 6 ? 39 : 46, fill: color(0x29251F), weight: .bold)
  text(topic.subtitle, x: 74, y: 232, width: 440, height: 56, size: 18, fill: color(0x776D62))
  text("一起提前退休  /  主题速览", x: 74, y: 591, width: 300, height: 21, size: 12, fill: color(0x968A7C), tracking: 0.4)
  text("FIELD NOTE     ·     2026", x: 890, y: 591, width: 240, height: 21, size: 11, fill: color(0x968A7C), tracking: 1.0)
  drawMotif(topic.kind, accent: topic.accent)

  context.flushGraphics()
  NSGraphicsContext.restoreGraphicsState()
  guard let data = bitmap.representation(using: .png, properties: [:]) else { throw NSError(domain: "TopicArt", code: 2) }
  let destination = outputDirectory.appendingPathComponent(topic.file)
  try data.write(to: destination, options: .atomic)
  print("generated \(destination.lastPathComponent)")
}

for topic in topics { try render(topic) }
