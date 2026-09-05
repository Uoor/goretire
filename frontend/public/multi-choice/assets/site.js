(() => {
  const content = window.SITE_CONTENT || {};

  const escapeHTML = (value = "") => String(value).replace(/[&<>"]/g, (character) => ({
    "&": "&amp;",
    "<": "&lt;",
    ">": "&gt;",
    '"': "&quot;"
  })[character]);

  const renderStories = () => {
    const stories = content.stories || [];
    const feed = document.querySelector("#story-feed");
    const updates = document.querySelector("#hero-updates");

    if (feed && stories.length) {
      feed.innerHTML = stories.map((story) =>
        '<article class="story-card">' +
          '<time datetime="2026-' + escapeHTML(story.date.replace(".", "-")) + '">' + escapeHTML(story.date) + '</time>' +
          '<div><small>' + escapeHTML(story.type) + '</small><h3>' + escapeHTML(story.title) + '</h3><p>' + escapeHTML(story.summary) + '</p></div>' +
          '<a href="' + escapeHTML(story.url) + '" aria-label="继续阅读：' + escapeHTML(story.title) + '">继续看 <span aria-hidden="true">↓</span></a>' +
        '</article>'
      ).join("");
    }

    if (updates && stories.length) {
      updates.innerHTML = stories.map((story) =>
        '<a href="' + escapeHTML(story.url) + '">' +
          '<time>' + escapeHTML(story.date) + '</time><b>' + escapeHTML(story.title) + '</b><em aria-hidden="true">↓</em>' +
        '</a>'
      ).join("");
    }
  };

  const renderNotes = () => {
    const notes = content.notes || [];
    const grid = document.querySelector("#notes-grid");
    if (!grid || !notes.length) return;

    grid.innerHTML = notes.map((note, index) => {
      const image = note.image
        ? '<img src="' + escapeHTML(note.image) + '" alt="' + escapeHTML(note.title) + '封面" />'
        : "";
      const source = note.url
        ? '<a href="' + escapeHTML(note.url) + '" target="_blank" rel="noreferrer">' + escapeHTML(note.source || "查看来源") + ' <span aria-hidden="true">↗</span></a>'
        : "";
      return '<article class="note-card ' + (index === 0 ? "featured" : "") + '" id="' + escapeHTML(note.id) + '">' +
        image +
        '<div><small>' + escapeHTML(note.type) + ' · ' + escapeHTML(note.date) + '</small>' +
        '<h3>' + escapeHTML(note.title) + '</h3><p>' + escapeHTML(note.summary) + '</p>' + source + '</div>' +
      '</article>';
    }).join("");
  };

  const bindChecklist = ({ selector, countSelector, resultSelector, noteSelector, messages }) => {
    const checks = [...document.querySelectorAll(selector)];
    const count = document.querySelector(countSelector);
    const result = document.querySelector(resultSelector);
    const note = document.querySelector(noteSelector);
    if (!checks.length || !count || !result || !note) return;

    const update = () => {
      const done = checks.filter((item) => item.checked).length;
      const message = messages.find((item) => done <= item.max) || messages[messages.length - 1];
      count.textContent = done + " / " + checks.length;
      result.textContent = message.title;
      note.textContent = message.note;
    };

    checks.forEach((item) => item.addEventListener("change", update));
    update();
  };

  const bindDeparture = () => {
    const buttons = [...document.querySelectorAll("[data-phase]")];
    const panel = document.querySelector("[data-phase-panel]");
    const progress = document.querySelector("[data-departure-progress]");
    const groups = content.departure || {};
    const storageKey = "multiple-choice-departure-v1";
    let saved = {};

    try {
      saved = JSON.parse(localStorage.getItem(storageKey) || "{}");
    } catch {
      saved = {};
    }

    const updateProgress = () => {
      const total = Object.values(groups).flat().length;
      const done = Object.keys(saved).filter((id) => saved[id]).length;
      if (progress) progress.textContent = done + " / " + total;
    };

    const renderPhase = (phase) => {
      if (!panel) return;
      panel.innerHTML = (groups[phase] || []).map((item) =>
        '<label><input type="checkbox" data-departure-id="' + escapeHTML(item.id) + '" ' + (saved[item.id] ? "checked" : "") + ' />' +
        '<span><b>' + escapeHTML(item.title) + '</b><small>' + escapeHTML(item.note) + '</small></span></label>'
      ).join("");

      panel.querySelectorAll("[data-departure-id]").forEach((input) => {
        input.addEventListener("change", () => {
          saved[input.dataset.departureId] = input.checked;
          try {
            localStorage.setItem(storageKey, JSON.stringify(saved));
          } catch {
            // The checklist still works for this visit when storage is unavailable.
          }
          updateProgress();
        });
      });
    };

    buttons.forEach((button) => {
      button.addEventListener("click", () => {
        buttons.forEach((item) => {
          const active = item === button;
          item.classList.toggle("active", active);
          item.setAttribute("aria-selected", String(active));
        });
        renderPhase(button.dataset.phase);
      });
    });

    renderPhase("before");
    updateProgress();
  };

  const bindTemplates = () => {
    const templates = {
      career: "你好，我是___。我做过___。现在关注___。如果你也在做相关事情，欢迎___。",
      project: "我在做___，目前已经___。接下来想解决___，也希望认识正在关注___的人。"
    };
    const buttons = [...document.querySelectorAll("[data-template]")];
    const preview = document.querySelector("[data-template-preview]");
    const copy = document.querySelector("[data-copy-template]");
    if (!preview || !copy) return;

    buttons.forEach((button) => {
      button.addEventListener("click", () => {
        buttons.forEach((item) => {
          const active = item === button;
          item.classList.toggle("active", active);
          item.setAttribute("aria-selected", String(active));
        });
        preview.textContent = templates[button.dataset.template];
      });
    });

    copy.addEventListener("click", async () => {
      try {
        await navigator.clipboard.writeText(preview.textContent);
        copy.textContent = "已复制";
      } catch {
        copy.textContent = "请手动复制";
      }
      window.setTimeout(() => { copy.textContent = "复制模板"; }, 1600);
    });
  };

  const bindActiveNavigation = () => {
    if (!("IntersectionObserver" in window)) return;
    const links = [...document.querySelectorAll("nav a")];
    const sections = links.map((link) => document.querySelector(link.getAttribute("href"))).filter(Boolean);
    const observer = new IntersectionObserver((entries) => {
      entries.forEach((entry) => {
        if (!entry.isIntersecting) return;
        links.forEach((link) => {
          const active = link.getAttribute("href") === "#" + entry.target.id;
          link.classList.toggle("active", active);
          if (active) link.setAttribute("aria-current", "location");
          else link.removeAttribute("aria-current");
        });
      });
    }, { rootMargin: "-30% 0px -60%", threshold: 0 });
    sections.forEach((section) => observer.observe(section));
  };

  renderStories();
  renderNotes();
  bindDeparture();
  bindTemplates();
  bindChecklist({
    selector: "[data-decision]",
    countSelector: "[data-decision-count]",
    resultSelector: "[data-decision-result]",
    noteSelector: "[data-decision-note]",
    messages: [
      { max: 1, title: "先从事实开始", note: "把已知和猜测分开，问题通常会小一点。" },
      { max: 3, title: "问题正在变清楚", note: "再找一个成本很低、可以验证判断的动作。" },
      { max: 4, title: "可以迈出最小一步", note: "不用一次解决全部，先让事情向前移动。" }
    ]
  });
  bindActiveNavigation();
})();
