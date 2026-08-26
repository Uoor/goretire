/**
 * 图片压缩工具
 * 用于上传前压缩图片，减少流量消耗和上传时间
 */

/**
 * 压缩图片
 * @param {File|Blob} file - 原始图片文件
 * @param {Object} options - 压缩选项
 * @param {number} options.maxWidth - 最大宽度，默认 1200
 * @param {number} options.maxHeight - 最大高度，默认 1200
 * @param {number} options.quality - 压缩质量 0-1，默认 0.8
 * @param {string} options.type - 输出类型，默认 'image/jpeg'
 * @returns {Promise<File|Blob>} 压缩后的图片（带文件名的 File；无法压缩时返回原文件）
 */
export async function compressImage(file, options = {}) {
  const {
    maxWidth = 1200,
    maxHeight = 1200,
    quality = 0.8,
    type = 'image/jpeg'
  } = options

  // 非图片文件直接返回
  if (!file.type.startsWith('image/')) {
    return file
  }

  try {
    // 创建 Image 对象
    const img = await loadImage(file)

    // 计算压缩后的尺寸
    let { width, height } = img
    if (width > maxWidth || height > maxHeight) {
      const ratio = Math.min(maxWidth / width, maxHeight / height)
      width = Math.round(width * ratio)
      height = Math.round(height * ratio)
    }

    // 创建 Canvas 并绘制
    const canvas = document.createElement('canvas')
    canvas.width = width
    canvas.height = height
    const ctx = canvas.getContext('2d')
    ctx.drawImage(img, 0, 0, width, height)

    // 部分定制 WebView（如鸿蒙 UWS / 蚂蚁专属钉钉）不支持 canvas.toBlob，
    // 直接回退原图，避免选图后静默失败（缩略图看似已上传，实际请求从未发出）
    if (typeof canvas.toBlob !== 'function') {
      console.warn('[image] canvas.toBlob 不可用，跳过压缩直接上传原图')
      return file
    }

    // 转换为 Blob
    const blob = await new Promise((resolve, reject) => {
      canvas.toBlob(
        (b) => (b ? resolve(b) : reject(new Error('图片压缩失败'))),
        type,
        quality
      )
    })
    // 如果压缩后更大，返回原文件
    if (blob.size >= file.size) {
      return file
    }
    // canvas.toBlob 产出无文件名的 Blob，multipart 默认文件名会是 "blob"，
    // 后端按扩展名校验会直接拒绝；包装成带 .jpg 文件名的 File（输出固定 jpeg）
    const name = (file.name || 'image').replace(/\.[^.]*$/, '') + '.jpg'
    return new File([blob], name, { type })
  } catch (e) {
    // 压缩链路任何异常（图片解码/画布绘制失败等）→ 回退原图直传，不阻断上传
    console.warn('[image] 图片压缩失败，回退原图上传:', e)
    return file
  }
}

/**
 * 加载图片
 * @param {File|Blob} file - 图片文件
 * @returns {Promise<HTMLImageElement>}
 */
function loadImage(file) {
  return new Promise((resolve, reject) => {
    const url = URL.createObjectURL(file)
    const img = new Image()
    // 加载完成后释放 ObjectURL，避免连续选图时大图驻留内存（移动端 WebView 敏感）
    img.onload = () => {
      URL.revokeObjectURL(url)
      resolve(img)
    }
    img.onerror = () => {
      URL.revokeObjectURL(url)
      reject(new Error('图片加载失败'))
    }
    img.src = url
  })
}

/**
 * 批量压缩图片
 * @param {File[]} files - 图片文件数组
 * @param {Object} options - 压缩选项
 * @param {Function} onProgress - 进度回调 (current, total)
 * @returns {Promise<Blob[]>}
 */
export async function compressImages(files, options = {}, onProgress) {
  const results = []
  for (let i = 0; i < files.length; i++) {
    const compressed = await compressImage(files[i], options)
    results.push(compressed)
    if (onProgress) {
      onProgress(i + 1, files.length)
    }
  }
  return results
}

/**
 * 获取图片尺寸
 * @param {File|Blob} file - 图片文件
 * @returns {Promise<{width: number, height: number}>}
 */
export async function getImageSize(file) {
  const img = await loadImage(file)
  return { width: img.width, height: img.height }
}

/**
 * 检查图片是否需要压缩
 * @param {File|Blob} file - 图片文件
 * @param {Object} options - 检查选项
 * @param {number} options.maxSize - 最大文件大小（字节），默认 500KB
 * @param {number} options.maxWidth - 最大宽度，默认 1200
 * @param {number} options.maxHeight - 最大高度，默认 1200
 * @returns {Promise<boolean>}
 */
export async function shouldCompress(file, options = {}) {
  const {
    maxSize = 500 * 1024, // 500KB
    maxWidth = 1200,
    maxHeight = 1200
  } = options

  // 文件大小超过阈值
  if (file.size > maxSize) {
    return true
  }

  // 检查尺寸
  const { width, height } = await getImageSize(file)
  if (width > maxWidth || height > maxHeight) {
    return true
  }

  return false
}
