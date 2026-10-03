import { Fragment, useEffect, useRef, useState } from "react";
import { Link } from "react-router-dom";
import { Avatar, Button, Card, Carousel, Image, List, Popover, Typography } from "antd";
import type { CarouselRef } from "antd/es/carousel";
import CommunityInviteSection, { buildCommunityInviteSlides } from "../components/site/CommunityInviteSection";
import HeroVisual from "../components/site/HeroVisual";
import SiteFooter from "../components/site/SiteFooter";
import SiteHeader from "../components/site/SiteHeader";
import { portalContent, type PortalVariant } from "../data/portalContent";
import assetUrl from "../utils/assetUrl";

function HeroSection() {
  return (
    <section className="hero section" id="home" aria-labelledby="hero-title">
      <div className="hero-glow" aria-hidden="true" />
      <div className="shell hero-grid">
        <div className="hero-copy reveal">
          <Typography.Title id="hero-title" level={1}>
            <span className="headline-line"><span className="headline-prefix">致力于</span><b>拉平信息差、</b><b>提升认知、</b></span>
            <span className="headline-line"><b>互助避坑、</b><b>善用金融工具，</b></span>
            <em>探索更自由人生</em>
          </Typography.Title>
        </div>
        <HeroVisual />
      </div>
    </section>
  );
}

function InsightSection({ variant }: { variant: PortalVariant }) {

    const [activeTopic, setActiveTopic] = useState<string>(portalContent.topics[0].label);
  const knowledge = variant === "ali"
      ? [...portalContent.knowledge.shared, portalContent.knowledge.ali]
      : [...portalContent.knowledge.shared];

  return (
    <section className="insight section" id="insight" aria-labelledby="insight-title">
      <div className="shell">
        <div className="section-head reveal">
          <div><p className="section-index">内容观点</p><h2 id="insight-title">帮你筛出<br />值得关注的信息</h2></div>
          <p>从股市到房市，从职场到生活，把零散信息变成体系化的判断依据</p>
        </div>
        <Card className="feature-panel insight-panel reveal" role="article" variant="outlined">
          <div className="insight-body">
            <div className="panel-block">
              <div className="block-title"><Typography.Text strong>每日bot推送</Typography.Text></div>
              <div className="topic-cloud" role="group" aria-label="选择每日bot推送主题">
                {portalContent.topics.map((topic) => {
                  const selected = activeTopic === topic.label;
                  return (
                    <Button
                      className={`topic-button${topic.accent ? " topic-accent" : ""}${selected ? " is-active" : ""}`}
                      key={topic.label}
                      aria-pressed={selected}
                      aria-controls="topic-preview"
                      onClick={() => setActiveTopic(topic.label)}
                    >
                      {topic.label}
                    </Button>
                  );
                })}
              </div>
              {(() => {
                const topic = portalContent.topics.find((item) => item.label === activeTopic) || portalContent.topics[0];
                return (
                  <div className="topic-preview" id="topic-preview" role="status">
                    <Image className="topic-preview-image" src={assetUrl(topic.image)} alt={topic.imageAlt} preview={false} />
                    <div className="topic-preview-copy">
                      <Typography.Text strong>{topic.label}</Typography.Text>
                      <Typography.Paragraph>{topic.intro}</Typography.Paragraph>
                    </div>
                  </div>
                );
              })()}
            </div>
            <div className="panel-block knowledge-block">
              <div className="block-title"><Typography.Text strong>知识库</Typography.Text></div>
              <List
                className="knowledge-list"
                split={false}
                dataSource={knowledge}
                renderItem={(item) => (
                  <List.Item className="knowledge-item" key={item.title}>
                    <Avatar className="knowledge-icon" shape="square" size={38} aria-hidden="true">{item.icon}</Avatar>
                    <div>
                      <Typography.Text strong>{item.title}</Typography.Text>
                      <Typography.Text className="knowledge-detail" type="secondary">{item.detail}</Typography.Text>
                    </div>
                    <Button
                      className="knowledge-open"
                      type="text"
                      aria-label={`打开${item.title}资料`}
                      onClick={() => window.open(item.url, "_blank", "noopener,noreferrer")}
                    >
                      <span aria-hidden="true">→</span>
                    </Button>
                  </List.Item>
                )}
              />
            </div>
          </div>
        </Card>
      </div>
    </section>
  );
}

function CommunitySection({ variant }: { variant: PortalVariant }) {
  const [activeQr, setActiveQr] = useState<string | null>(null);
  const isHome = variant === "home";
  const isMobile = window.matchMedia("(max-width: 700px)").matches;

  return (
    <section className="community section" id="community" aria-labelledby="community-title">
      <div className="shell">
        <div className="section-head reveal">
          <div><p className="section-index">子群服务</p><Typography.Title id="community-title" level={2}>找到同频的人<br />一起交流心得</Typography.Title></div>
          <Typography.Paragraph>在投资、金融工具、工作生活的细分圈子里，让信息交流都变成真实的连接</Typography.Paragraph>
        </div>
        <div className="service-groups service-panel reveal">
          {portalContent.communityGroups.map((group) => (
            <Card className="service-group" role="group" aria-label={group.title} key={group.title} variant="outlined">
              <div className="service-heading"><div><Typography.Title level={5}>{group.title}</Typography.Title><Typography.Text type="secondary">{group.subtitle}</Typography.Text></div></div>
              <div className="community-links">
                {group.links.map((link) => {
                  if (isHome) {
                    return (
                      <div className="community-tag community-tag-static" key={link.label}>
                        <Typography.Text strong>{link.label}</Typography.Text>
                      </div>
                    );
                  }
                  const hasQr = "qrImage" in link && Boolean(link.qrImage);
                  const button = (
                    <Button
                      className="community-tag"
                      aria-label={hasQr ? `${link.label}，查看进群二维码` : `${link.label}，打开详情页`}
                      onClick={link.url ? () => window.open(link.url, "_blank", "noopener,noreferrer") : undefined}
                    >
                      <Typography.Text strong>{link.label}</Typography.Text>
                      <span aria-hidden="true">↗</span>
                    </Button>
                  );
                  if (!hasQr || isMobile) {
                    return <Fragment key={link.label}>{button}</Fragment>;
                  }
                  return (
                    <Popover
                      key={link.label}
                      open={activeQr === link.label}
                      onOpenChange={(open) => {
                        if (open) {
                          setActiveQr(link.label);
                        } else {
                          setActiveQr((current) => current === link.label ? null : current);
                        }
                      }}
                      trigger={["hover"]}
                      placement="top"
                      mouseEnterDelay={0.12}
                      content={(
                        <div className="community-popover-content">
                          <Image src={assetUrl(link.qrImage)} alt={link.qrAlt} preview={false} />
                          <Typography.Text>钉钉扫码加入{link.label}</Typography.Text>
                        </div>
                      )}
                    >
                      {button}
                    </Popover>
                  );
                })}
              </div>
            </Card>
          ))}
        </div>
        <p className="risk-note reveal">市场及金融相关内容仅供信息交流与学习参考，不构成任何投资、税务或金融建议</p>
      </div>
    </section>
  );
}

function CompanyNetworkSection() {
  const autoplayEnabled = !window.matchMedia("(prefers-reduced-motion: reduce)").matches;
  const showAll = portalContent.companyNetworksShowAll;
  const companies = showAll
    ? portalContent.companyNetworks
    : portalContent.companyNetworks.filter((company) => company.id === "ali");
  const pages: (typeof companies)[number][][] = [];
  for (let i = 0; i < companies.length; i += 3) {
    pages.push(companies.slice(i, i + 3));
  }

  const renderCard = (company: (typeof companies)[number]) => (
    <Card className="company-card" key={company.id} variant="outlined">
      <div className="company-copy">
        <Typography.Text className="company-name" strong>{company.name}</Typography.Text>
        <Typography.Paragraph className="company-desc">{company.description}</Typography.Paragraph>
        {"link" in company && company.link ? (
          <Link className="company-link" to={company.link}>
            {company.linkLabel}
            <span aria-hidden="true">→</span>
          </Link>
        ) : null}
      </div>
      <div className="company-qr">
        <img src={assetUrl(company.qrImage)} alt={company.qrAlt} />
        <Typography.Text type="secondary">扫码加入总群</Typography.Text>
      </div>
    </Card>
  );

  return (
    <section className="company-networks section" id="company-networks" aria-labelledby="company-networks-title">
      <div className="shell">
        <div className="section-head reveal">
          <div>
            <p className="section-index">大厂社群</p>
            <Typography.Title id="company-networks-title" level={2}>{showAll ? <>按公司找到组织<br />专属圈子持续扩展</> : <>按公司找到组织<br />阿里校友专属圈子</>}</Typography.Title>
          </div>
          <Typography.Paragraph>{showAll ? "目前已覆盖阿里巴巴/字节/携程/得物/小米/海康... 更多大厂的专属社群正在路上" : "阿里巴巴专属社群入口，6000+ 校友在这里互助同行"}</Typography.Paragraph>
        </div>
        {pages.length > 1 ? (
          <Carousel
            className="company-carousel reveal"
            aria-label="大厂社群卡片轮播"
            arrows
            dots
            autoplay={autoplayEnabled}
            autoplaySpeed={3000}
            pauseOnHover
            adaptiveHeight
            accessibility
          >
            {pages.map((page, index) => (
              <div className="company-slide" key={index}>
                <div className="company-grid">
                  {page.map((company, cardIndex) => (
                    cardIndex === page.length - 1 ? (
                      <div className="company-mask" key={company.id}>
                        {renderCard(company)}
                        <div className="company-mask-tip" aria-hidden="true">滑动查看更多 →</div>
                      </div>
                    ) : renderCard(company)
                  ))}
                </div>
              </div>
            ))}
          </Carousel>
        ) : (
          <div className={`company-grid reveal${showAll ? "" : " is-single"}`}>
            {companies.map(renderCard)}
          </div>
        )}
      </div>
    </section>
  );
}

function MutualAidSection() {
  return (
    <section className="mutual-aid section" id="mutual-aid" aria-labelledby="mutual-title">
      <div className="shell mutual-card reveal">
        <div className="mutual-copy">
          <p className="section-index section-index-light">资源互助</p>
          <h2 id="mutual-title">把好资源<br />带给更多校友</h2>
          <p>如果你身边有优质的资源或服务，并愿意给校友提供专属福利，我们非常乐意帮忙推广，对接相关人脉资源</p>
        </div>
        <Card className="mutual-action" variant="outlined">
          <Typography.Text className="action-label">资源合作</Typography.Text>
          <Typography.Paragraph>欢迎微信联系： <span className="contact-id">antfin2018</span></Typography.Paragraph>
          <div className="action-line" aria-hidden="true"><span /></div>
          <Typography.Text className="mutual-action-note" type="secondary">大家一起互助互利，早日实现提前退休</Typography.Text>
        </Card>
      </div>
    </section>
  );
}

export default function PortalPage({ variant }: { variant: PortalVariant }) {
  const carouselRef = useRef<CarouselRef>(null);
  const autoplayEnabled = !window.matchMedia("(prefers-reduced-motion: reduce)").matches;
  const [isMobile, setIsMobile] = useState(() => window.matchMedia("(max-width: 700px)").matches);

  useEffect(() => {
    const mq = window.matchMedia("(max-width: 700px)");
    const onChange = (event: MediaQueryListEvent) => setIsMobile(event.matches);
    mq.addEventListener("change", onChange);
    return () => mq.removeEventListener("change", onChange);
  }, []);

  const heroSlideIndex = isMobile ? 0 : 1;
  const inviteStartIndex = isMobile ? 1 : 0;

  useEffect(() => {
    const reveals = [...document.querySelectorAll<HTMLElement>(".reveal")];
    const reduceMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
    let revealObserver: IntersectionObserver | undefined;

    if (reduceMotion || !("IntersectionObserver" in window)) {
      reveals.forEach((item) => item.classList.add("is-visible"));
    } else {
      revealObserver = new IntersectionObserver((entries, activeObserver) => {
        entries.forEach((entry) => {
          if (entry.target.classList.contains("hero-visual")) {
            entry.target.classList.toggle("is-in-view", entry.isIntersecting);
            if (entry.isIntersecting) entry.target.classList.add("is-visible");
            return;
          }
          if (!entry.isIntersecting) return;
          entry.target.classList.add("is-visible");
          activeObserver.unobserve(entry.target);
        });
      }, { threshold: 0.12 });
      reveals.forEach((item) => revealObserver?.observe(item));
    }

    return () => {
      revealObserver?.disconnect();
    };
  }, []);

  useEffect(() => {
    const syncCarouselToHash = () => {
      if (window.location.hash === "#community-invites") carouselRef.current?.goTo(inviteStartIndex);
      if (window.location.hash === "#home") carouselRef.current?.goTo(heroSlideIndex);
    };

    window.addEventListener("hashchange", syncCarouselToHash);
    syncCarouselToHash();
    return () => window.removeEventListener("hashchange", syncCarouselToHash);
  }, [heroSlideIndex, inviteStartIndex]);

  return (
    <>
      <a className="skip-link" href="#main">跳到主要内容</a>
      <SiteHeader variant={variant} />
      <main id="main">
        <Carousel
          ref={carouselRef}
          className="portal-top-carousel"
          aria-label="品牌介绍与加入社群轮播"
          arrows
          dots
          autoplay={autoplayEnabled}
          pauseOnHover
          adaptiveHeight={!isMobile}
          accessibility
        >
          {isMobile ? (
            [
              <div className="portal-carousel-slide" key="hero-mobile"><HeroSection /></div>,
              ...buildCommunityInviteSlides(variant)
            ]
          ) : (
            [
              <div className="portal-carousel-slide" key="invite"><CommunityInviteSection variant={variant} /></div>,
              <div className="portal-carousel-slide" key="hero"><HeroSection /></div>
            ]
          )}
        </Carousel>
        {variant === "home" && <CompanyNetworkSection />}
        <InsightSection variant={variant} />
        <CommunitySection variant={variant} />
        <MutualAidSection />
      </main>
      <SiteFooter />
    </>
  );
}