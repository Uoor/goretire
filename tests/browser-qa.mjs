import { chromium } from "/Users/zhuanz/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/playwright/index.mjs";
import { mkdir } from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const baseUrl = process.env.BASE_URL || "http://127.0.0.1:8774";
const output = path.join(root, "tests", "screenshots");
await mkdir(output, { recursive: true });

const browser = await chromium.launch({ headless: true, executablePath: "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome" });
const page = await browser.newPage({ viewport: { width: 1440, height: 900 }, deviceScaleFactor: 1 });
const errors = [];
page.on("console", (message) => { if (message.type() === "error") errors.push(message.text()); });
page.on("pageerror", (error) => errors.push(error.message));

await page.goto(`${baseUrl}/`, { waitUntil: "networkidle" });
await page.waitForTimeout(800);
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
const stockCommunity = page.getByRole("button", { name: "A股交流，查看进群二维码" });
await stockCommunity.hover();
await page.locator(".community-popover-content img").waitFor();
const communityQrLoaded = await page.locator(".community-popover-content img").evaluate((image) => image.complete && image.naturalWidth > 0);
const stockQrAlt = await page.locator(".community-popover-content img").getAttribute("alt");
await page.mouse.move(0, 0);
await page.getByRole("button", { name: "港美股交流，查看进群二维码" }).click();
await page.getByText("扫码加入港美股交流").waitFor();
const hkQrAlt = await page.getByRole("img", { name: "港美股交流社群二维码" }).getAttribute("alt");
if (stockQrAlt !== "A股交流社群二维码" || hkQrAlt !== "港美股交流社群二维码") {
  throw new Error("each innermost community tile should use its own QR configuration");
}
await page.locator('.top-nav a[href="#community-invites"]').click();
await page.waitForFunction(() => location.hash === "#community-invites" && Boolean(document.querySelector(".portal-top-carousel .slick-active #community-invites")));
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
  carouselSections: [
    document.querySelector(".portal-top-carousel #home")?.id,
    document.querySelector(".portal-top-carousel #community-invites")?.id
  ],
  hasLoginCopy: /登录|注册|验证码/.test(document.body.innerText)
}));

await page.evaluate(() => window.scrollTo(0, document.querySelector("#community").offsetTop));
await page.waitForTimeout(250);
const stickyHeaderTop = await page.locator(".site-header").evaluate((header) => header.getBoundingClientRect().top);

await page.setViewportSize({ width: 390, height: 844 });
await page.reload({ waitUntil: "networkidle" });
await page.evaluate(() => window.scrollTo(0, 0));
await page.waitForTimeout(800);
await page.screenshot({ path: path.join(output, "mobile.png"), fullPage: true });
const mobile = await page.evaluate(() => ({ width: document.documentElement.scrollWidth, viewport: window.innerWidth, heroVisible: document.querySelector("#home").getBoundingClientRect().height > 0 }));

const aliResponse = await page.goto(`${baseUrl}/ali`, { waitUntil: "networkidle" });
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
if (!topicPreviewLoaded) throw new Error("active topic should show its configured introduction image");
if (!communityQrLoaded) throw new Error("community card popover should load its configured QR image");
if (!documentUrl.startsWith("https://alidocs.dingtalk.com/")) throw new Error("knowledge arrow should open the configured document URL");
if (aliPage.width > aliPage.viewport) throw new Error("Ali page horizontal overflow");
if (!aliResponse?.ok()) throw new Error("Ali SPA route should return the app shell");
if (!aliPage.pathname.endsWith("/ali")) throw new Error("Ali route should use the React Router SPA path");
if (aliPage.knowledgeItems !== 3) throw new Error("Ali page should include the employee departure SOP");
if (!aliPage.qrLoaded) throw new Error("Ali page QR image should load from the nested route");
if (desktop.bodySections.join(",") !== "insight,community,mutual-aid") throw new Error("page body should keep the three content sections after the top carousel");
if (desktop.carouselSlides !== 2 || desktop.carouselSections.join(",") !== "home,community-invites") throw new Error("hero and community invites should be the two top carousel slides");
if (desktop.hasLoginCopy) throw new Error("login copy should not appear");
if (errors.length) throw new Error("browser errors: " + errors.join(" | "));

console.log(JSON.stringify({ desktop, mobile, aliPage, stickyHeaderTop, errors }, null, 2));
await browser.close();
