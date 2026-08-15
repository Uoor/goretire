// 格式化工具（价格/时间/图片）

/** 图片 URL 解析：兼容 JSON 数组字符串与逗号分隔 */
export function parseImages(images) {
  if (!images) return []
  if (Array.isArray(images)) return images
  if (typeof images === 'string') {
    try {
      const arr = JSON.parse(images)
      if (Array.isArray(arr)) return arr
    } catch {
      /* fallthrough */
    }
    return images.split(',').map((s) => s.trim()).filter(Boolean)
  }
  return []
}

/** 相对时间："3 天前上架"（MVP 简单实现） */
export function timeAgo(datetime) {
  if (!datetime) return ''
  const diff = Date.now() - new Date(datetime).getTime()
  const day = Math.floor(diff / 86400000)
  if (day <= 0) return '今天'
  if (day === 1) return '昨天'
  if (day < 30) return `${day} 天前`
  return new Date(datetime).toLocaleDateString('zh-CN')
}

/** 金额格式化：5800.00 → "5,800" */
export function formatMoney(value) {
  if (value == null) return ''
  return Number(value).toLocaleString('zh-CN')
}
