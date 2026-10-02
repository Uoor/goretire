import { Avatar } from "antd";
import { portalContent } from "../../data/portalContent";

export default function SiteFooter() {
  return (
    <footer className="site-footer">
      <div className="shell footer-inner">
        <div className="footer-topline">
          <a className="footer-brand" href="#home">
            <Avatar className="footer-brand-symbol" size={36} aria-hidden="true">{portalContent.brand.symbol}</Avatar>
            <span className="footer-wordmark">{portalContent.brand.name}</span>
          </a>
          <p>{portalContent.footer.tagline}</p>
        </div>
        <div className="filing-number">
          <a href={portalContent.footer.filing.url} target="_blank" rel="noopener noreferrer">{portalContent.footer.filing.label}</a>
        </div>
      </div>
    </footer>
  );
}
