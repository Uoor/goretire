const year = document.querySelector("[data-year]");
if (year) year.textContent = new Date().getFullYear();

const reveals = [...document.querySelectorAll(".reveal")];
const reduceMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches;

if (reduceMotion || !("IntersectionObserver" in window)) {
  reveals.forEach((item) => item.classList.add("is-visible"));
} else {
  const revealObserver = new IntersectionObserver((entries, observer) => {
    entries.forEach((entry) => {
      if (!entry.isIntersecting) return;
      entry.target.classList.add("is-visible");
      observer.unobserve(entry.target);
    });
  }, { threshold: 0.12 });
  reveals.forEach((item) => revealObserver.observe(item));
}

const links = [...document.querySelectorAll(".top-nav a")];
const sections = links.map((link) => document.querySelector(link.getAttribute("href"))).filter(Boolean);

if (sections.length) {
  let ticking = false;
  const updateActiveNav = () => {
    const scrollY = window.scrollY;
    const viewportHeight = window.innerHeight;
    let activeIndex = 0;
    sections.forEach((section, index) => {
      const rect = section.getBoundingClientRect();
      const sectionTop = rect.top + scrollY;
      const sectionBottom = sectionTop + rect.height;
      if (scrollY + viewportHeight * 0.3 >= sectionTop && scrollY < sectionBottom) {
        activeIndex = index;
      }
    });
    links.forEach((link, index) => link.classList.toggle("is-active", index === activeIndex));
    ticking = false;
  };
  window.addEventListener("scroll", () => {
    if (!ticking) {
      requestAnimationFrame(updateActiveNav);
      ticking = true;
    }
  });
  updateActiveNav();
}
