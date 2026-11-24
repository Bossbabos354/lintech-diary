/* ============================================================================
   Полоса-«линейка» — порт scrollbar'а getartcraft.com.
   • нативная полоса скрыта, справа фиксированная панель 60px;
   • шкала 0–100%: мелкие деления каждые 1%, длинные с цифрами — каждые 5%;
   • ghost-линия под курсором показывает, куда прыгнет клик;
   • темы разделов написаны вдоль линейки, а буквы заголовков по одной
     улетают и «перетекают» в свою тему на линейке.
   Подключается на index.html и developers.html.
   ============================================================================ */
(function () {
  'use strict';

  // ---- геометрия и тайминги (значения из их «tunables») ----
  var RAIL_W = 60;         // railW
  var NAV_H = 48;
  var EDGE_PAD = 12;       // edgePad
  var MINOR_STEP = 0.01;   // деление каждые 1%
  var MAJOR_STEP = 0.05;   // цифра каждые 5%
  var FLIP_VH = 0.12;      // линия отрыва (12% высоты окна)
  var DETACH_ZONE = 240;   // путь, за который буква долетает
  var STAGGER = 0.55;      // буквы летят «по одной»
  var ARC = 28;            // изгиб траектории к странице
  var LABEL_PX = 9.5;      // размер буквы в линейке (совпадает с .ruler-label)
  var LABEL_LINE = 11;     // шаг стопки букв вдоль линейки
  var HANDOFF_START = 0.72;// с какого момента буква растворяется в подписи

  var mqOK = window.matchMedia('(hover: hover) and (pointer: fine) and (min-width: 1100px)');
  var mqReduce = window.matchMedia('(prefers-reduced-motion: reduce)');

  var ruler = null, railEl = null, cursor = null, ghost = null;
  var groups = [];
  var geom = { vw: 0, vh: 0, docH: 1, maxScroll: 1, top: 0, span: 1 };
  var raf = 0, dragging = false, dragMoved = 0, dragStartY = 0;

  function clamp01(v) { return v < 0 ? 0 : v > 1 ? 1 : v; }
  function easeCubic(t) { return t < 0.5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2; }

  function labelFor(sec) {
    var eb = sec.querySelector('.eyebrow');
    if (eb) {
      var t = (eb.textContent || '').split('·')[0].replace(/^\s*\d+\s*[\/·]\s*/, '').trim();
      if (t) return t.slice(0, 16);
    }
    if (sec.id === 'hero' || sec.id === 'top') return 'Начало';
    return sec.id;
  }

  // ---------- разбиение заголовка на буквы (слова не рвутся) ----------
  function splitTextNode(node) {
    var parts = node.nodeValue.split(/(\s+)/);
    var frag = document.createDocumentFragment();
    for (var i = 0; i < parts.length; i++) {
      var piece = parts[i];
      if (!piece) continue;
      if (/^\s+$/.test(piece)) { frag.appendChild(document.createTextNode(piece)); continue; }
      var word = document.createElement('span');
      word.className = 'rl-w';
      for (var c = 0; c < piece.length; c++) {
        var ch = document.createElement('span');
        ch.className = 'rl-ch';
        ch.textContent = piece.charAt(c);
        word.appendChild(ch);
      }
      frag.appendChild(word);
    }
    node.parentNode.replaceChild(frag, node);
  }

  function splitHeading(heading) {
    if (heading.getAttribute('data-split') === '1') return;
    Array.prototype.slice.call(heading.childNodes).forEach(function (n) {
      if (n.nodeType === 3) splitTextNode(n);
      else if (n.nodeType === 1 && !n.classList.contains('rl-w')) {
        Array.prototype.slice.call(n.childNodes).forEach(function (k) {
          if (k.nodeType === 3) splitTextNode(k);
        });
      }
    });
    heading.setAttribute('data-split', '1');
  }

  function collect() {
    groups = [];
    var sections = Array.prototype.slice.call(document.querySelectorAll('section[id]'));
    sections.forEach(function (sec) {
      var heading = sec.querySelector('h2') || sec.querySelector('h1');
      if (!heading) return;
      splitHeading(heading);
      var letters = Array.prototype.slice.call(heading.querySelectorAll('.rl-ch'));
      if (!letters.length) return;
      groups.push({
        sec: sec,
        heading: heading,
        letters: letters,
        orig: [],
        headingPageTop: 0,
        slotY: 0,
        fs: parseFloat(window.getComputedStyle(heading).fontSize) || 24,
        labelEl: null,
        labelCX: 0,
        labelCY: 0,
        lastP: -1
      });
    });
  }

  // layout-координаты относительно документа (не зависят от CSS-трансформов)
  function pageOffset(el) {
    var x = 0, y = 0, n = el;
    while (n) { x += n.offsetLeft; y += n.offsetTop; n = n.offsetParent; }
    return { x: x, y: y };
  }

  function yFor(frac) { return geom.top + clamp01(frac) * geom.span; }
  function fracForScroll(y) { return clamp01(y / geom.maxScroll); }
  function fracForY(clientY) { return clamp01((clientY - geom.top) / geom.span); }

  function measure() {
    geom.vw = window.innerWidth;
    geom.vh = window.innerHeight;
    geom.docH = Math.max(document.documentElement.scrollHeight, document.body.scrollHeight);
    geom.maxScroll = Math.max(1, geom.docH - geom.vh);
    geom.top = NAV_H + EDGE_PAD;
    geom.span = Math.max(1, (geom.vh - EDGE_PAD) - geom.top);

    groups.forEach(function (g) {
      g.orig = g.letters.map(function (l) {
        var o = pageOffset(l);
        return { x: o.x, y: o.y };
      });
      g.headingPageTop = pageOffset(g.heading).y;
      g.slotY = yFor(g.sec.offsetTop / geom.maxScroll);
      g.lastP = -1;
    });
  }

  // ---------- отрисовка рейки: деления, цифры и темы разделов ----------
  function buildRail() {
    railEl.innerHTML = '';
    for (var f = 0; f <= 1.0001; f += MINOR_STEP) {
      var minor = document.createElement('span');
      minor.className = 'ruler-tick minor';
      minor.style.top = yFor(f).toFixed(1) + 'px';
      railEl.appendChild(minor);
    }
    for (var m = 0; m <= 1.0001; m += MAJOR_STEP) {
      var y = yFor(m);
      var major = document.createElement('span');
      major.className = 'ruler-tick major';
      major.style.top = y.toFixed(1) + 'px';
      railEl.appendChild(major);
      var num = document.createElement('span');
      num.className = 'ruler-num';
      num.style.top = y.toFixed(1) + 'px';
      num.textContent = Math.round(m * 100);
      railEl.appendChild(num);
    }
    groups.forEach(function (g) {
      var lab = document.createElement('span');
      lab.className = 'ruler-label';
      lab.style.top = g.slotY.toFixed(1) + 'px';
      lab.textContent = labelFor(g.sec);
      railEl.appendChild(lab);
      g.labelEl = lab;
    });
  }

  function measureLabels() {
    groups.forEach(function (g) {
      if (!g.labelEl) return;
      var r = g.labelEl.getBoundingClientRect();
      g.labelCX = r.left + r.width / 2;
      g.labelCY = r.top + r.height / 2;
    });
  }

  // ---------- анимация ----------
  function render() {
    raf = 0;
    if (!ruler) return;
    var scrollY = window.pageYOffset;
    var flipLine = geom.vh * FLIP_VH;

    cursor.style.top = yFor(fracForScroll(scrollY)).toFixed(1) + 'px';
    ruler.setAttribute('aria-valuenow', String(Math.round(fracForScroll(scrollY) * 100)));

    groups.forEach(function (g) {
      var p = clamp01((flipLine - (g.headingPageTop - scrollY)) / DETACH_ZONE);

      if (p <= 0) {
        if (g.lastP !== 0) {
          g.letters.forEach(function (l) { l.style.transform = ''; l.style.opacity = ''; });
          if (g.labelEl) g.labelEl.style.opacity = '0';
          g.lastP = 0;
        }
        return;
      }
      g.lastP = p;

      // «перетекание»: буквы растворяются, подпись-тема проявляется
      var hand = easeCubic(clamp01((p - HANDOFF_START) / (1 - HANDOFF_START)));
      var active = (g.sec.offsetTop <= scrollY + flipLine);
      var base = active ? 1 : 0.45;
      if (g.labelEl) g.labelEl.style.opacity = (base * hand).toFixed(3);
      var letterOpacity = 1 - hand;

      var n = g.letters.length;
      var scaleEnd = LABEL_PX / g.fs;
      var cx = g.labelCX, cy = g.labelCY, half = (n - 1) / 2;

      for (var j = 0; j < n; j++) {
        var l = g.letters[j];
        var frac = n > 1 ? j / (n - 1) : 0;
        var pj = clamp01((p - frac * STAGGER) / (1 - STAGGER));
        var e = easeCubic(pj);

        var ox = g.orig[j].x;
        var oy = g.orig[j].y - scrollY;
        var tx = cx;
        var ty = cy + (j - half) * LABEL_LINE;

        var dx = (tx - ox) * e - ARC * Math.sin(Math.PI * e);
        var dy = (ty - oy) * e;
        var sc = 1 + (scaleEnd - 1) * e;

        l.style.transform = 'translate(' + dx.toFixed(2) + 'px,' + dy.toFixed(2) + 'px) scale(' + sc.toFixed(3) + ')';
        l.style.opacity = letterOpacity < 0.999 ? letterOpacity.toFixed(3) : '';
      }
    });
  }

  function onScroll() { if (!raf) raf = window.requestAnimationFrame(render); }
  function onResize() { measure(); buildRail(); measureLabels(); render(); }

  // ---------- сборка / разборка ----------
  function build() {
    if (ruler) return;

    ruler = document.createElement('div');
    ruler.className = 'ruler';
    ruler.setAttribute('role', 'scrollbar');
    ruler.setAttribute('aria-controls', 'main');
    ruler.setAttribute('aria-orientation', 'vertical');
    ruler.setAttribute('aria-valuemin', '0');
    ruler.setAttribute('aria-valuemax', '100');
    ruler.setAttribute('aria-label', 'Позиция страницы');

    railEl = document.createElement('div');
    railEl.className = 'ruler-rail';
    ruler.appendChild(railEl);

    cursor = document.createElement('div');
    cursor.className = 'ruler-cursor';
    ruler.appendChild(cursor);

    ghost = document.createElement('div');
    ghost.className = 'ruler-ghost';
    ruler.appendChild(ghost);

    ruler.addEventListener('pointermove', function (e) {
      ghost.style.top = (e.clientY - 1.5) + 'px';
      ghost.style.opacity = '1';
      if (dragging) {
        dragMoved = Math.max(dragMoved, Math.abs(e.clientY - dragStartY));
        window.scrollTo(0, fracForY(e.clientY) * geom.maxScroll);
      }
    });
    ruler.addEventListener('pointerleave', function () {
      if (!dragging) ghost.style.opacity = '0';
    });

    ruler.addEventListener('click', function (e) {
      if (dragMoved > 4) { dragMoved = 0; return; }
      window.scrollTo({ top: fracForY(e.clientY) * geom.maxScroll, behavior: 'smooth' });
    });

    // перетаскивание — мгновенно (иначе CSS scroll-behavior:smooth «дёргает»)
    ruler.addEventListener('pointerdown', function (e) {
      dragging = true; dragMoved = 0; dragStartY = e.clientY;
      document.documentElement.style.scrollBehavior = 'auto';
      try { ruler.setPointerCapture(e.pointerId); } catch (err) {}
      window.scrollTo(0, fracForY(e.clientY) * geom.maxScroll);
    });
    function endDrag(e) {
      if (!dragging) return;
      dragging = false;
      document.documentElement.style.scrollBehavior = '';
      try { ruler.releasePointerCapture(e.pointerId); } catch (err) {}
      ghost.style.opacity = '0';
    }
    ruler.addEventListener('pointerup', endDrag);
    ruler.addEventListener('pointercancel', endDrag);

    document.body.appendChild(ruler);
    document.documentElement.classList.add('ruler-on');

    collect();
    measure();
    buildRail();
    measureLabels();
    render();

    window.addEventListener('scroll', onScroll, { passive: true });
    window.addEventListener('resize', onResize);
    window.addEventListener('load', onResize);
    if (document.fonts && document.fonts.ready) document.fonts.ready.then(onResize);
  }

  function destroy() {
    if (!ruler) return;
    window.removeEventListener('scroll', onScroll);
    window.removeEventListener('resize', onResize);
    window.removeEventListener('load', onResize);
    groups.forEach(function (g) {
      g.letters.forEach(function (l) { l.style.transform = ''; l.style.opacity = ''; });
    });
    ruler.remove();
    ruler = railEl = cursor = ghost = null;
    document.documentElement.classList.remove('ruler-on');
    document.documentElement.style.scrollBehavior = '';
  }

  function sync() {
    if (mqOK.matches && !mqReduce.matches) build();
    else destroy();
  }

  if (mqOK.addEventListener) {
    mqOK.addEventListener('change', sync);
    mqReduce.addEventListener('change', sync);
  } else if (mqOK.addListener) {
    mqOK.addListener(sync);
    mqReduce.addListener(sync);
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', sync);
  } else {
    sync();
  }
})();
