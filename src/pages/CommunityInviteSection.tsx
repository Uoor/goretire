import { Image, Tag, Typography } from "antd";
import { portalContent, type PortalVariant } from "../../data/portalContent";
import assetUrl from "../../utils/assetUrl";

export default function CommunityInviteSection({ variant }: { variant: PortalVariant }) {
  const isHome = variant === "home";
  const cards = portalContent.footer.qrCodes.filter((qr) => !isHome || "qrImage" in qr);
  return (
    <section className="community-invites section" id="community-invites" aria-labelledby="community-invites-title">
      <div className="shell">
        <div className="community-invites-heading">
          <div>
            {/* <p className="section-index"></p> */}
            <Typography.Title id="community-invites-title" level={2}></Typography.Title>
          </div>
          {/* <Typography.Paragraph>选择适合你的入口，钉钉扫码加入一起提前退休社区</Typography.Paragraph> */}
        </div>
        <div className={`community-invites-grid${cards.length === 1 ? " is-single" : ""}`}>
          {cards.map((qr) => (
            <article className={`community-invite-card${"qrImage" in qr ? " invite-wechat-card" : ""}`} key={"qrImage" in qr ? qr.qrAlt : qr.label}>
              {"qrImage" in qr ? (
                <div className="invite-wechat">
                  <img className="invite-wechat-qr" src={assetUrl(qr.qrImage)} alt={qr.qrAlt} />
                  <img className="invite-wechat-search" src={assetUrl(qr.searchImage)} alt={qr.searchAlt} />
                </div>
              ) : (
                <>
                  <img src={assetUrl(qr.image)} alt={qr.alt} />
                  <div className="invite-copy">
                    <div className="invite-label-row">
                      <Typography.Text strong>{qr.label}</Typography.Text>
                      {"badge" in qr && qr.badge ? <Tag className="invite-badge" color="orange">{qr.badge}</Tag> : null}
                    </div>
                    {"notes" in qr && qr.notes ? (
                      <ul className="invite-notes">
                        {qr.notes.map((note) => (
                          <li key={note}>{note}</li>
                        ))}
                      </ul>
                    ) : null}
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
