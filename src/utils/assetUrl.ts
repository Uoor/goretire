export default function assetUrl(path: string) {
  const basePath = __APP_BASE_PATH__.replace(/\/?$/, "/");
  return `${basePath}${path.replace(/^\/+/, "")}`;
}
