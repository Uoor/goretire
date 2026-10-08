// 端到端 QA：需要本机具备 playwright 与 Chrome，且站点已在 BASE_URL 上运行。
//
//   npm start                 # 或 npm run preview（跑 dist 产物）
//   node tests/browser-qa.mjs
//
// playwright 装在非默认位置时用 PLAYWRIGHT_MODULE 指向它的 index.mjs；
// Chrome 不在默认路径时用 CHROME_PATH 指定。两者缺失会明确跳过，而不是崩溃。
import { existsSync } from "node:fs";
import { mkdir } from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const baseUrl = process.env.BASE_URL || "http://127.0.0.1:8774";
const output = path.join(root, "tests", "screenshots");

let chromium;
for (const candidate of [process.env.PLAYWRIGHT_MODULE, "playwright", "playwright-core"].filter(Boolean)) {
  try {
    ({ chromium } = await import(candidate));
    break;
  } catch {
    // 继续尝试下一个候选位置
  }
}
if (!chromium) {
  console.log([
    "SKIPPED tests/browser-qa.mjs — 未找到 playwright。",
    "  npm i -D playwright          # 或在别处安装后用 PLAYWRIGHT_MODULE 指向其 index.mjs",
    "本脚本不参与 `npm test`；静态门禁是 tests/verify-site.mjs。"
  ].join("\n"));
  process.exit(0);
}

const chromePath = [
  process.env.CHROME_PATH,
  "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome",
  "/Applications/Chromium.app/Contents/MacOS/Chromium"
].filter(Boolean).find((candidate) => existsSync(candidate));

await mkdir(output, { recursive: true });

const browser = await chromium.launch({ headless: true, ...(chromePath ? { executablePath: chromePath } : {}) });
const page = await browser.newPage({ viewport: { width: 1440, height: 900 }, deviceScaleFactor: 1 });
const errors = [];
page.on("console", (message) => { if (message.type() === "error") errors.push(message.text()); });
page.on("pageerror", (error) => errors.push(error.message));

// 首屏轮播默认自动播放，断言会取决于"此刻停在哪一屏"。模拟 prefers-reduced-motion: reduce
// 关掉它（PortalPage 用它决定 autoplay），结果才稳定。
await page.emulateMedia({ reducedMotion: "reduce" });

// ---------------------------------------------------------------------------
// 首页
// ---------------------------------------------------------------------------
await page.goto(`${baseUrl}/`, { waitUntil: "networkidle" });
await page.waitForTimeout(500);
await page.screenshot({ path: path.join(output, "desktop.png"), fullPage: true });

const initialActiveTopic = await page.locator(".topic-button[aria-pressed='true']").innerText();
const initialActiveCount = await page.locator(".topic-button[aria-pressed='true']").count();
if (initialActiveTopic !== "A股行情" || initialActiveCount !== 1) {
  throw new Error("the first topic should be the single default active item");
}
await page.getByRole("button", { name: "AI 日报" }).click();
await page.getByRole("button", { name: "AI 日报" }).click();
const activeTopics = await page.locator(".topic-button[aria-pressed='true']").allInnerTexts();
if (activeTopics.length !== 1 || activeTopics[0] !== "AI 日报") {
  throw new Error("topic selection should remain single-active after repeated clicks");
}
await page.locator("#topic-preview .ant-image-img").waitFor();
const topicPreviewLoaded = await page.locator("#topic-preview img").evaluate((image) => image.complete && image.naturalWidth > 0);

// hash 导航应把首屏轮播切到对应 slide。
// 注意：导航里的 #community-invites 条目当前是注释掉的，所以直接驱动 hash，
// 测的是 syncCarouselToHash 本身，而不是某个导航项是否存在。
await page.evaluate(() => { window.location.hash = "#community-invites"; });
await page.waitForFunction(
  () => Boolean(document.querySelector(".portal-top-carousel .slick-active #community-invites")),
  undefined,
  { timeout: 5000 }
);

// 知识库箭头应打开配置好的钉钉文档
const documentPopupPromise = page.context().waitForEvent("page");
await page.locator(".knowledge-open").first().click();
const documentPopup = await documentPopupPromise;
const documentUrl = documentPopup.url();
await documentPopup.close();

const desktop = await page.evaluate(() => ({
  title: document.title,
  width: document.documentElement.scrollWidth,
  viewport: window.innerWidth,
  bodySections: [...document.querySelectorAll("main > section")].map((section) => section.id),
  carouselSlides: document.querySelectorAll(".portal-top-carousel .slick-slide:not(.slick-cloned)").length,
  carouselSlideSections: [...document.querySelectorAll(".portal-top-carousel .slick-slide:not(.slick-cloned)")]
    .map((slide) => slide.querySelector("section")?.id ?? null),
  // 子群服务在首页是静态标签，只有 /ali 才渲染可悬停的二维码按钮
  communityButtons: document.querySelectorAll("#community .community-tag.ant-btn").length,
  hasLoginCopy: /登录|注册|验证码/.test(document.body.innerText)
}));

await page.evaluate(() => window.scrollTo(0, document.querySelector("#community").offsetTop));
await page.waitForTimeout(250);
const stickyHeaderTop = await page.locator(".site-header").evaluate((header) => header.getBoundingClientRect().top);

// ---------------------------------------------------------------------------
// 移动端：Hero 与「关注我们」顺序对调，关注卡逐张成为独立 slide
// ---------------------------------------------------------------------------
await page.setViewportSize({ width: 390, height: 844 });
await page.reload({ waitUntil: "networkidle" });
await page.evaluate(() => window.scrollTo(0, 0));
await page.waitForTimeout(500);
await page.screenshot({ path: path.join(output, "mobile.png"), fullPage: true });
const mobile = await page.evaluate(() => ({
  width: document.documentElement.scrollWidth,
  viewport: window.innerWidth,
  heroVisible: document.querySelector("#home").getBoundingClientRect().height > 0,
  // 注意：首页移动端的关注卡（公众号）不带 id="community-invites"，
  // 该锚点只存在于桌面端，以及 /ali 移动端的第一张群卡上。
  firstSlideSection: document.querySelector(".portal-top-carousel .slick-slide:not(.slick-cloned) section")?.id ?? null,
  inviteSlides: document.querySelectorAll(".portal-top-carousel .community-invites").length
}));

// ---------------------------------------------------------------------------
// /ali：多一条离职 SOP 知识项；子群服务在这里才渲染二维码按钮
// ---------------------------------------------------------------------------
await page.setViewportSize({ width: 1440, height: 900 });
const aliResponse = await page.goto(`${baseUrl}/ali`, { waitUntil: "networkidle" });
await page.waitForTimeout(500);

// 用 accessible name（二维码的 alt）定位，避免 antd Popover 关闭后保留的隐藏节点
// 让 .community-popover-content img 匹配到多个元素。
const stockQrImage = page.getByRole("img", { name: "A股交流社群二维码" });
await page.getByRole("button", { name: "A股交流，查看进群二维码" }).hover();
await stockQrImage.waitFor();
const communityQrLoaded = await stockQrImage.evaluate((image) => image.complete && image.naturalWidth > 0);
const stockQrAlt = await stockQrImage.getAttribute("alt");
await page.mouse.move(0, 0);
await page.waitForTimeout(200);

const hkQrImage = page.getByRole("img", { name: "港美股交流社群二维码" });
await page.getByRole("button", { name: "港美股交流，查看进群二维码" }).hover();
await hkQrImage.waitFor();
const hkQrAlt = await hkQrImage.getAttribute("alt");

const aliPage = await page.evaluate(() => ({
  pathname: window.location.pathname,
  width: document.documentElement.scrollWidth,
  viewport: window.innerWidth,
  knowledgeItems: document.querySelectorAll(".knowledge-item").length,
  qrLoaded: (() => {
    const image = document.querySelector(".community-invites img");
    return image instanceof HTMLImageElement && image.complete && image.naturalWidth > 0;
  })()
}));

if (desktop.width > desktop.viewport) throw new Error("desktop horizontal overflow");
if (Math.abs(stickyHeaderTop) > 1) throw new Error("header should remain fixed while scrolling");
if (mobile.width > mobile.viewport) throw new Error("mobile horizontal overflow");
if (!mobile.heroVisible) throw new Error("mobile should still render the hero section");
if (mobile.firstSlideSection !== "home") throw new Error(`mobile top carousel should start with the hero, got ${mobile.firstSlideSection}`);
if (mobile.inviteSlides < 1) throw new Error("mobile should render the 关注我们 slide");
if (!topicPreviewLoaded) throw new Error("active topic should show its configured introduction image");
if (!communityQrLoaded) throw new Error("community card popover should load its configured QR image");
if (!documentUrl.startsWith("https://alidocs.dingtalk.com/")) throw new Error("knowledge arrow should open the configured document URL");
if (desktop.bodySections.join(",") !== "company-networks,insight,community,mutual-aid") throw new Error(`unexpected home sections: ${desktop.bodySections.join(",")}`);
if (desktop.carouselSlides !== 2 || desktop.carouselSlideSections.join(",") !== "community-invites,home") throw new Error(`desktop top carousel should be 关注我们 → Hero, got ${desktop.carouselSlideSections.join(",")}`);
if (desktop.communityButtons !== 0) throw new Error("home 子群服务 should render static labels, not buttons");
if (desktop.hasLoginCopy) throw new Error("login copy should not appear");
if (stockQrAlt !== "A股交流社群二维码" || hkQrAlt !== "港美股交流社群二维码") {
  throw new Error("each community tile should use its own QR configuration");
}
if (!aliResponse?.ok()) throw new Error("Ali SPA route should return the app shell");
if (!aliPage.pathname.endsWith("/ali")) throw new Error("Ali route should use the React Router SPA path");
if (aliPage.width > aliPage.viewport) throw new Error("Ali page horizontal overflow");
if (aliPage.knowledgeItems !== 3) throw new Error("Ali page should include the employee departure SOP");
if (!aliPage.qrLoaded) throw new Error("Ali page QR image should load from the nested route");
if (errors.length) throw new Error("browser errors: " + errors.join(" | "));

console.log(JSON.stringify({ desktop, mobile, aliPage, stickyHeaderTop, stockQrAlt, hkQrAlt, errors }, null, 2));
await browser.close();
