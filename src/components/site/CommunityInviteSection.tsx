import { Image, Tag, Typography } from "antd";
import { portalContent } from "../../data/portalContent";
import assetUrl from "../../utils/assetUrl";

export default function CommunityInviteSection() {
  return (
    <section className="community-invites section" id="community-invites" aria-labelledby="community-invites-title">
      <div className="shell">
        <div className="community-invites-heading">
          <div>
            {/* <p className="section-index">加入社群</p> */}
            <Typography.Title id="community-invites-title" level={2}>加入社群</Typography.Title>
          </div>
          {/* <Typography.Paragraph>选择适合你的入口，钉钉扫码加入一起提前退休社区</Typography.Paragraph> */}
        </div>
        <div className="community-invites-grid">
          {portalContent.footer.qrCodes.map((qr) => (
            <article className="community-invite-card" key={"qrImage" in qr ? qr.qrAlt : qr.label}>
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
