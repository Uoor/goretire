import { useEffect, useState } from "react";
import { Avatar, Menu, Typography } from "antd";
import { Link } from "react-router-dom";
import { portalContent, type PortalVariant } from "../../data/portalContent";

export default function SiteHeader({ variant }: { variant: PortalVariant }) {
  const [activeSection, setActiveSection] = useState("#home");

  useEffect(() => {
    const links = [...document.querySelectorAll<HTMLAnchorElement>(".top-nav a")];
    const sections = links
      .map((link) => document.querySelector<HTMLElement>(link.getAttribute("href") || ""))
      .filter((section): section is HTMLElement => Boolean(section));
    let ticking = false;

    const updateActiveNav = () => {
      const scrollY = window.scrollY;
      const viewportHeight = window.innerHeight;
      let activeIndex = 0;
      sections.forEach((section, index) => {
        const rect = section.getBoundingClientRect();
        const sectionTop = rect.top + scrollY;
        if (scrollY + viewportHeight * 0.3 >= sectionTop && scrollY < sectionTop + rect.height) activeIndex = index;
      });
      setActiveSection(links[activeIndex]?.getAttribute("href") || "#home");
      ticking = false;
    };

    const onScroll = () => {
      if (ticking) return;
      window.requestAnimationFrame(updateActiveNav);
      ticking = true;
    };

    window.addEventListener("scroll", onScroll, { passive: true });
    updateActiveNav();
    return () => window.removeEventListener("scroll", onScroll);
  }, []);

  const brand = (
    <>
      <Avatar className="brand-symbol" size={38} aria-hidden="true">{portalContent.brand.symbol}</Avatar>
      <Typography.Text className="brand-name" strong>{portalContent.brand.name}</Typography.Text>
      <Typography.Text className="brand-caption">{portalContent.brand.caption}</Typography.Text>
    </>
  );

  return (
    <header className="site-header" data-header>
      <div className="shell header-inner">
        {variant === "ali" ? (
          <Link className="nav-brand" to="/" aria-label={`${portalContent.brand.name} ${portalContent.brand.caption}，回到主页`}>{brand}</Link>
        ) : (
          <a className="nav-brand" href="#home" aria-label={`${portalContent.brand.name} ${portalContent.brand.caption}，回到页面顶部`}>{brand}</a>
        )}
        <Menu
          className="top-nav"
          aria-label="主要导航"
          mode="horizontal"
          selectedKeys={[activeSection]}
          items={portalContent.navigation.map((item) => ({ key: item.href, label: <a href={item.href}>{item.label}</a> }))}
          onClick={({ key }) => setActiveSection(key)}
        />
      </div>
    </header>
  );
}
