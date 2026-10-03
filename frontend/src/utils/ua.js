/**
 * 运行环境检测
 */

/**
 * 华为 UWS 内核（鸿蒙 WebView / 华为手机自带浏览器）检测。
 * UA 形如 "... UWS/5.12.11.0 ... Hmos/4.2.0 ..."（UA 谎报 Android 12）。
 * 该内核 canvas.toBlob / 图片解码兼容性差，vant-uploader 的 after-read 也可能不触发，
 * 上传链路需走最兼容路径（跳过压缩、原生 XHR、fileList 兜底监听）。
 */
export const isHuaweiUws =
  typeof navigator !== 'undefined' &&
  /UWS\/|Hmos\/|HarmonyOS/.test(navigator.userAgent || '')
