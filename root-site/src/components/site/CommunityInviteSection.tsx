import type { ReactNode } from "react";
import { Tag, Typography } from "antd";
import { portalContent, type PortalVariant } from "../../data/portalContent";
import assetUrl from "../../utils/assetUrl";
import HeroVisual from "./HeroVisual";

const MOBILE_QUERY = "(max-width: 700px)";

type QrCodeItem = (typeof portalContent.footer.qrCodes)[number];
type GroupCard = Extract<QrCodeItem, { label: string }>;
type WechatCard = Extract<QrCodeItem, { qrImage: string }>;

function getInviteCards(variant: PortalVariant) {
  const isHome = variant === "home";
  return portalContent.footer.qrCodes.filter((qr) => !isHome || "qrImage" in qr);
}

/** 移动端拆分后的 slide 数量（每张卡一个 slide），供外层轮播计算 Hero 的页码 */
export function getInviteSlideCount(variant: PortalVariant) {
  return getInviteCards(variant).length;
}

function renderLabelRow(qr: GroupCard) {
  return (
    <div className="invite-label-row">
      <Typography.Text strong>{qr.label}</Typography.Text>
      {"badge" in qr && qr.badge ? <Tag className="invite-badge" color="orange">{qr.badge}</Tag> : null}
    </div>
  );
}

function renderNotes(qr: GroupCard) {
  return "notes" in qr && qr.notes ? (
    <ul className="invite-notes">
      {qr.notes.map((note) => (
        <li key={note}>{note}</li>
      ))}
    </ul>
  ) : null;
}

function renderWechatContent(qr: WechatCard) {
  return (
    <div className="invite-wechat">
      <img className="invite-wechat-qr" src={assetUrl(qr.qrImage)} alt={qr.qrAlt} />
      <img className="invite-wechat-search" src={assetUrl(qr.searchImage)} alt={qr.searchAlt} />
    </div>
  );
}

/** 移动端：把社群内容拆成多张卡，每张卡作为外层轮播的一个独立 slide */
export function buildCommunityInviteSlides(variant: PortalVariant): ReactNode[] {
  const groupHeading = variant === "home" ? "关注我们" : "加入社群";
  const wechatHeading = "关注我们";
  const cards = getInviteCards(variant);
  const groupCards = cards.filter((qr): qr is GroupCard => !("qrImage" in qr));
  const wechatCards = cards.filter((qr): qr is WechatCard => "qrImage" in qr);
  const slides: ReactNode[] = [];

  groupCards.forEach((qr, index) => {
    slides.push(
      <div className="portal-carousel-slide" key={`${qr.label}-slide`}>
        <section className="community-invites invite-slide-content" id={index === 0 ? "community-invites" : undefined} aria-labelledby={index === 0 ? "community-invites-title" : undefined}>
          <div className="shell">
            <div className="community-invites-heading">
              <Typography.Title id={index === 0 ? "community-invites-title" : undefined} level={2}>{groupHeading}</Typography.Title>
            </div>
            <article className="community-invite-card">
              <img src={assetUrl(qr.image)} alt={qr.alt} />
              <div className="invite-copy">
                {renderLabelRow(qr)}
                {renderNotes(qr)}
              </div>
            </article>
          </div>
        </section>
      </div>
    );
  });

  wechatCards.forEach((qr) => {
    slides.push(
      <div className="portal-carousel-slide" key={`${qr.qrAlt}-slide`}>
        <section className="community-invites invite-slide-content">
          <div className="shell">
            <div className="community-invites-heading">
              <Typography.Title level={2}>{wechatHeading}</Typography.Title>
            </div>
            <article className="community-invite-card invite-wechat-card">
              <div className="invite-wechat">
                <img className="invite-wechat-qr" src={assetUrl(qr.qrImage)} alt={qr.qrAlt} />
                <img className="invite-wechat-search invite-wechat-search-long" src={assetUrl("assets/wechat-long.png")} alt={qr.searchAlt} />
              </div>
            </article>
          </div>
        </section>
      </div>
    );
  });

  return slides;
}

export default function CommunityInviteSection({ variant }: { variant: PortalVariant }) {
  const cards = getInviteCards(variant);
  const wechatCard = cards.find((qr): qr is WechatCard => "qrImage" in qr);

  // 首页：完全参考 Hero 的排版 —— 左侧「关注我们」+ 二维码 + 微信搜一搜，右侧轨道视觉，底色/动效沿用 .hero
  if (variant === "home" && wechatCard) {
    return (
      <section className="hero section follow-hero" id="community-invites" aria-labelledby="community-invites-title">
        <div className="hero-glow" aria-hidden="true" />
        <div className="shell hero-grid">
          <div className="hero-copy reveal">
            <Typography.Title id="community-invites-title" level={1}>关注我们</Typography.Title>
            <div className="follow-media">
              <img className="follow-qr" src={assetUrl(wechatCard.qrImage)} alt={wechatCard.qrAlt} />
              <img className="follow-search follow-search-long" src={assetUrl("assets/wechat-long.png")} alt={wechatCard.searchAlt} />
            </div>
          </div>
          <HeroVisual />
        </div>
      </section>
    );
  }

  return (
    <section className="community-invites section" id="community-invites" aria-labelledby="community-invites-title">
      <div className="shell">
        <div className="community-invites-heading">
          <div>
            {/* <p className="section-index">加入社群</p> */}
            <Typography.Title id="community-invites-title" level={2}>{variant === "home" ? "关注我们" : "加入社群"}</Typography.Title>
          </div>
          {/* <Typography.Paragraph>选择适合你的入口，钉钉扫码加入一起提前退休社区</Typography.Paragraph> */}
        </div>
        <div className={`community-invites-grid${cards.length === 1 ? " is-single" : ""}`}>
          {cards.map((qr) => (
            <article className={`community-invite-card${"qrImage" in qr ? " invite-wechat-card" : ""}`} key={"qrImage" in qr ? qr.qrAlt : qr.label}>
              {"qrImage" in qr ? (
                renderWechatContent(qr)
              ) : (
                <>
                  <img src={assetUrl(qr.image)} alt={qr.alt} />
                  <div className="invite-copy">
                    {renderLabelRow(qr)}
                    {renderNotes(qr)}
                  </div>
                </>
              )}
            </article>
          ))}
        </div>
      </div>
    </section>
  );
}
