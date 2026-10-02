import assert from "node:assert/strict";
import { readFile, readdir } from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const read = (name) => readFile(path.join(root, name), "utf8");
const outputFiles = await readdir(path.join(root, "dist"));
const [html, notFoundHtml, app, homePage, aliPage, page, siteHeader, siteFooter, inviteSection, content, styles, entry] = await Promise.all([
  read("dist/index.html"),
  read("dist/404.html"),
  read("src/app/App.tsx"),
  read("src/pages/HomePage.tsx"),
  read("src/pages/AliPage.tsx"),
  read("src/pages/PortalPage.tsx"),
  read("src/components/site/SiteHeader.tsx"),
  read("src/components/site/SiteFooter.tsx"),
  read("src/components/site/CommunityInviteSection.tsx"),
  read("src/data/portalContent.ts"),
  read("assets/site.scss"),
  read("src/main.tsx")
]);

for (const output of [html, notFoundHtml]) {
  assert.match(output, /<title>一起提前退休/);
  assert.match(output, /id=["']?root["']?/);
  assert.match(output, /main\.[\w-]+\.js/);
  assert.match(output, /main\.[\w-]+\.css/);
}

assert.match(app, /<Route path="\/" element=\{<HomePage \/>\} \/>/);
assert.match(app, /<Route path="\/ali" element=\{<AliPage \/>\} \/>/);
assert.match(homePage, /variant="home"/);
assert.match(aliPage, /variant="ali"/);
assert.match(entry, /<BrowserRouter[\s\S]*basename=\{__APP_BASE_PATH__\}/);
assert.match(siteFooter, /portalContent\.footer\.filing/);
assert.doesNotMatch(siteFooter, /qrCodes|footer-qrcode/);
assert.match(inviteSection, /portalContent\.footer\.qrCodes\.map/);
assert.match(page, /<CommunityInviteSection \/>/);
assert.match(page, /<Carousel[\s\S]*<HeroSection \/>[\s\S]*<CommunityInviteSection \/>[\s\S]*<\/Carousel>/);
assert.match(page, /carouselRef\.current\?\.goTo\(1\)/);
assert.doesNotMatch(page, /window\.location\.pathname/);
assert.match(html, /src=["']?\/assets\/main\.[\w-]+\.js/);
assert.match(html, /href=["']?\/assets\/main\.[\w-]+\.css/);
assert.match(notFoundHtml, /src=["']?\/assets\/main\.[\w-]+\.js/);
assert.ok(!outputFiles.includes("ali"), "SPA build should not emit a route-specific folder");
assert.ok(outputFiles.includes("index.html") && outputFiles.includes("404.html"));
assert.match(siteHeader, /Link className="nav-brand" to="\/"/);
assert.match(content, /离职员工 SOP/);
assert.match(content, /社群服务/);
assert.match(content, /qrImage:/);
assert.match(content, /qrAlt:/);
assert.match(content, /imageAlt:/);
assert.match(content, /url: "https:\/\/alidocs\.dingtalk\.com/);
assert.match(page, /trigger=\{\["hover", "click"\]\}/);
assert.match(page, /group\.links\.map\(\(link\)/);
assert.match(page, /window\.open\(item\.url/);
assert.match(page, /topic-preview/);
assert.match(styles, /\.community-invites\s*\{/);
assert.match(styles, /\.portal-top-carousel/);
assert.match(entry, /site\.scss/);
for (const id of ["home", "insight", "community", "community-invites", "mutual-aid"]) {
  assert.ok(page.includes(`id="${id}"`), `missing section: ${id}`);
}

for (const copy of [
  "致力于", "拉平信息差、", "提升认知、", "互助避坑、", "善用金融工具，", "探索更自由人生",
  "内容观点", "A股行情", "黄金行情", "港美股行情", "二手房价格推送",
  "房产拐点知识库", "AI 每日日报", "离职员工 SOP", "社群服务", "A股交流", "港美股交流",
  "银行咨询", "融资服务", "节税专区", "香港港险", "校友租房", "招聘内推", "香港身份 DIY",
  "别墅轰趴", "资源互助", "antfin2018", "一起提前退休"
]) assert.ok(`${page}${content}${siteHeader}`.includes(copy), `missing copy: ${copy}`);

assert.match(siteHeader, /className="brand-symbol"/);
assert.match(page, /一起<br\s*\/>提前退休/);
assert.match(page, /IntersectionObserver/);
assert.match(page, /prefers-reduced-motion/);
assert.match(styles, /--orange:\s*#ff6a00/);
assert.match(styles, /--text-title:\s*34px/);
assert.match(styles, /\.community\s*\{[^}]*linear-gradient/);
assert.match(styles, /\.site-header\s*\{[^}]*position:\s*sticky/);
assert.match(styles, /@media \(max-width: 700px\)/);
assert.match(styles, /prefers-reduced-motion/);

console.log("React SPA static checks passed");
