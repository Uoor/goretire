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
 * @returns {Promise<Blob>} 压缩后的图片 Blob
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

  // 转换为 Blob
  return new Promise((resolve, reject) => {
    canvas.toBlob(
      (blob) => {
        if (blob) {
          // 如果压缩后更大，返回原文件
          if (blob.size >= file.size) {
            resolve(file)
          } else {
            resolve(blob)
          }
        } else {
          reject(new Error('图片压缩失败'))
        }
      },
      type,
      quality
    )
  })
}

/**
 * 加载图片
 * @param {File|Blob} file - 图片文件
 * @returns {Promise<HTMLImageElement>}
 */
function loadImage(file) {
  return new Promise((resolve, reject) => {
    const img = new Image()
    img.onload = () => resolve(img)
    img.onerror = () => reject(new Error('图片加载失败'))
    img.src = URL.createObjectURL(file)
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
