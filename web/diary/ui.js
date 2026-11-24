/* ЛинТех 28 — веб-версия, повторяет интерфейс приложения (ios26). */
(() => {
'use strict';

// ---------------- утилиты ----------------
const $ = (id) => document.getElementById(id);
function el(tag, cls, html) {
  const e = document.createElement(tag);
  if (cls) e.className = cls;
  if (html != null) e.innerHTML = html;
  return e;
}
const esc = (s) => String(s == null ? '' : s).replace(/[&<>"']/g, (c) =>
  ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
const fmt2 = (v) => (v == null ? '—' : v.toFixed(2).replace('.', ','));

const SUBJ_COLORS = ['#7C4DFF', '#FF4081', '#00BFA6', '#FF9800', '#42A5F5',
  '#AB47BC', '#26A69A', '#EF5350', '#5C6BC0', '#8D6E63', '#66BB6A', '#EC407A'];
const subjColor = (id) => SUBJ_COLORS[((id % SUBJ_COLORS.length) + SUBJ_COLORS.length) % SUBJ_COLORS.length];

const DAYS_FULL = ['Понедельник', 'Вторник', 'Среда', 'Четверг', 'Пятница', 'Суббота'];
const DAYS_SHORT = ['Пн', 'Вт', 'Ср', 'Чт', 'Пт', 'Сб'];
const MONTHS = ['января', 'февраля', 'марта', 'апреля', 'мая', 'июня',
  'июля', 'августа', 'сентября', 'октября', 'ноября', 'декабря'];

function dayKey(d) {
  const x = new Date(d);
  return x.getFullYear() + '-' + (x.getMonth() + 1) + '-' + x.getDate();
}
function fmtDM(d) { return d.getDate() + ' ' + MONTHS[d.getMonth()].slice(0, 3); }
function fmtFull(d) { return DAYS_FULL[(d.getDay() + 6) % 7] + ', ' + d.getDate() + ' ' + MONTHS[d.getMonth()]; }
function markClass(mark) {
  const n = parseInt(mark, 10);
  return isNaN(n) ? '' : (n >= 5 ? 'm5' : n >= 4 ? 'm4' : n >= 3 ? 'm3' : 'm2');
}
function markColor(mark) {
  const n = parseInt(mark, 10);
  return isNaN(n) ? 'var(--text3)' : (n >= 5 ? 'var(--success)' : n >= 4 ? 'var(--accent)' : n >= 3 ? 'var(--warning)' : 'var(--danger)');
}
function gradeColor(avg) {
  if (avg == null) return 'var(--text3)';
  return avg >= 4.5 ? 'var(--success)' : avg >= 3.5 ? 'var(--warning)' : 'var(--danger)';
}
let toastTimer = null;
function toast(msg) {
  const t = $('toast');
  t.textContent = msg;
  t.classList.add('show');
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => t.classList.remove('show'), 2200);
}

// ---------------- хранилище ----------------
const Prefs = {
  get(k, d) { try { const v = localStorage.getItem(k); return v == null ? d : v; } catch (e) { return d; } },
  set(k, v) { try { localStorage.setItem(k, v); } catch (e) {} },
  del(k) { try { localStorage.removeItem(k); } catch (e) {} },
  login: () => Prefs.get('diary_login', ''),
  password: () => Prefs.get('diary_password', ''),
  studentId: () => Number(Prefs.get('diary_student_id', -1)),
  setStudentId: (v) => Prefs.set('diary_student_id', String(v)),
  uchYear: () => Number(Prefs.get('diary_uch_year', 0)),
  setUchYear: (v) => Prefs.set('diary_uch_year', String(v)),
  classId: () => Number(Prefs.get('diary_class_id', -1)),
  setClassId: (v) => Prefs.set('diary_class_id', String(v)),
};

// ---------------- тема ----------------
const THEME_KEY = 'diary_theme';
function applyTheme() {
  let t = Prefs.get(THEME_KEY, 'system');
  if (t === 'system') {
    t = window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
  }
  document.documentElement.dataset.theme = t;
  const bg = $('bg');
  if (bg) bg.classList.toggle('zhan', t === 'zhan');
  document.querySelectorAll('.segment [data-theme]').forEach((b) =>
    b.classList.toggle('on', b.dataset.theme === Prefs.get(THEME_KEY, 'system')));
}
function setTheme(t) {
  Prefs.set(THEME_KEY, t);
  applyTheme();
  toast('Тема: ' + ({ system: 'системная', light: 'светлая', dark: 'тёмная', zhan: 'книжная' }[t] || t));
}
if (window.matchMedia) {
  try {
    window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', () => {
      if (Prefs.get(THEME_KEY, 'system') === 'system') applyTheme();
    });
  } catch (e) {}
}

// ---------------- состояние ----------------
const S = {
  route: { screen: 'dairy' },
  weekOffset: 0,
  subjects: null,
  subjectsAt: 0,
  schedule: null,
  scheduleAt: 0,
  bells: null,
  diary: new Map(),
  user: null,
  loading: false,
};
const TABS = ['dairy', 'schedule', 'marks', 'goals', 'events'];

function go(route) {
  S.route = route;
  window.scrollTo({ top: 0 });
  render();
}
function back() {
  if (S.route.back) return go(S.route.back);
  go({ screen: S.route.tab || 'dairy' });
}

// ---------------- данные ----------------
async function ensureClass() {
  let cid = Prefs.classId();
  if (cid === -1) {
    cid = await Diary.getStudentClass(Prefs.studentId(), Prefs.uchYear());
    Prefs.setClassId(cid);
  }
  return cid;
}

async function loadSubjects(force) {
  const fresh = Date.now() - S.subjectsAt < 60_000;
  if (S.subjects && fresh && !force) return S.subjects;
  const cid = await ensureClass();
  S.subjects = await Diary.getMarks(cid, Prefs.studentId(), Prefs.uchYear());
  S.subjectsAt = Date.now();
  return S.subjects;
}

async function loadWeek(offset, force) {
  const { monday, sunday } = Diary.weekRange(offset);
  const key = dayKey(monday) + (force ? '' : '');
  if (S.diary.has(key) && !force) return S.diary.get(key);
  const cid = await ensureClass();
  const entries = await Diary.getDairy(cid, Prefs.studentId(), monday, sunday);
  S.diary.set(key, entries);
  return entries;
}

async function loadSchedule(force) {
  if (S.schedule && Date.now() - S.scheduleAt < 60_000 && !force) return S.schedule;
  const cid = await ensureClass();
  S.schedule = await Diary.getSchedule(cid);
  S.scheduleAt = Date.now();
  return S.schedule;
}

async function loadBells() {
  if (S.bells) return S.bells;
  for (const url of ['../data/schedule.json', 'data/schedule.json']) {
    try {
      const r = await fetch(url, { cache: 'no-store' });
      if (!r.ok) continue;
      const j = await r.json();
      if (j && j.bells) { S.bells = j.bells; return S.bells; }
    } catch (e) {}
  }
  S.bells = { weekday: [], saturday: [] };
  return S.bells;
}

async function loadEvents() {
  for (const url of ['../data/events.json', 'data/events.json']) {
    try {
      const r = await fetch(url, { cache: 'no-store' });
      if (!r.ok) continue;
      const j = await r.json();
      const list = Array.isArray(j) ? j : (j.events || []);
      return list.slice().sort((a, b) => String(a.date || '').localeCompare(String(b.date || '')));
    } catch (e) {}
  }
  return [];
}

// ---------------- цели ----------------
const GOALS_KEY = 'diary_goals';
function loadGoals() {
  try { const a = JSON.parse(Prefs.get(GOALS_KEY, '[]')); return Array.isArray(a) ? a : []; } catch (e) { return []; }
}
function saveGoals(list) { Prefs.set(GOALS_KEY, JSON.stringify(list)); }
function findGoal(subjectId, periodKey) {
  return loadGoals().find((g) => g.subjectId === subjectId && g.periodKey === periodKey) || null;
}
function upsertGoal(goal) {
  saveGoals(loadGoals().filter((g) => !(g.subjectId === goal.subjectId && g.periodKey === goal.periodKey)).concat([goal]));
}
function removeGoal(subjectId, periodKey) {
  saveGoals(loadGoals().filter((g) => !(g.subjectId === subjectId && g.periodKey === periodKey)));
}
function goalPlural(n) {
  const m10 = n % 10, m100 = n % 100;
  if (m10 === 1 && m100 !== 11) return 'ку';
  if (m10 >= 2 && m10 <= 4 && (m100 < 12 || m100 > 14)) return 'ки';
  return 'ок';
}

// ---------------- домашка ----------------
function hwKey(e) { return dayKey(e.date) + '_' + e.lessonNumber + '_' + e.subjectId; }
function loadHw() {
  try { const a = JSON.parse(Prefs.get('diary_hw', '{}')); return a && typeof a === 'object' ? a : {}; } catch (e) { return {}; }
}
function isHwDone(e) { return !!loadHw()[hwKey(e)]; }
function toggleHw(e) {
  const all = loadHw();
  if (all[hwKey(e)]) delete all[hwKey(e)]; else all[hwKey(e)] = 1;
  Prefs.set('diary_hw', JSON.stringify(all));
}

// ---------------- шторка ----------------
function openSheet(html, onMount) {
  const ov = $('overlay');
  const sheet = $('sheet');
  sheet.innerHTML = '<div class="grab"></div>' + html;
  ov.classList.remove('hidden');
  requestAnimationFrame(() => ov.classList.add('show'));
  ov.onclick = (ev) => { if (ev.target === ov) closeSheet(); };
  if (onMount) onMount(sheet);
}
function closeSheet() {
  const ov = $('overlay');
  ov.classList.remove('show');
  setTimeout(() => { ov.classList.add('hidden'); $('sheet').innerHTML = ''; }, 240);
  ov.onclick = null;
}
document.addEventListener('keydown', (e) => { if (e.key === 'Escape') closeSheet(); });

// ---------------- шапка ----------------
const ICON_PATHS = {
  dairy: '<path d="M4 5.5A2.5 2.5 0 0 1 6.5 3H11v18H6.5A2.5 2.5 0 0 1 4 18.5z"/><path d="M20 5.5A2.5 2.5 0 0 0 17.5 3H13v18h4.5A2.5 2.5 0 0 0 20 18.5z"/>',
  schedule: '<rect x="3" y="5" width="18" height="16" rx="3"/><path d="M8 3v4M16 3v4M3 10h18"/>',
  marks: '<path d="m12 3 2.6 5.6 6.1.7-4.5 4.2 1.2 6-5.4-3-5.4 3 1.2-6L3.3 9.3l6.1-.7z"/>',
  goals: '<path d="M5 21V4"/><path d="M5 4h11l-2 3 2 3H5"/>',
  events: '<rect x="3" y="5" width="18" height="16" rx="3"/><path d="M8 3v4M16 3v4M3 10h18"/><path d="M8.5 15h7"/>',
  search: '<circle cx="11" cy="11" r="7"/><path d="m20 20-3.5-3.5"/>',
  settings: '<circle cx="12" cy="12" r="3"/><path d="M19 12a7 7 0 0 0-.1-1l2-1.5-2-3.4-2.3 1a7 7 0 0 0-1.7-1L14.5 3h-4l-.4 2.5a7 7 0 0 0-1.7 1l-2.3-1-2 3.4L6 10.5a7 7 0 0 0 0 2L4 14l2 3.4 2.3-1a7 7 0 0 0 1.7 1l.4 2.6h4l.4-2.6a7 7 0 0 0 1.7-1l2.3 1 2-3.4-2-1.5a7 7 0 0 0 .1-1z"/>',
  back: '<path d="m15 5-7 7 7 7"/>',
  left: '<path d="m14 6-6 6 6 6"/>',
  right: '<path d="m10 6 6 6-6 6"/>',
  homework: '<path d="M5 4h9l5 5v11H5z"/><path d="M14 4v5h5"/>',
};
function icon(name, size) {
  return '<svg class="icon" viewBox="0 0 24 24" width="' + (size || 22) + '" height="' + (size || 22) +
    '" aria-hidden="true">' + (ICON_PATHS[name] || '') + '</svg>';
}
function renderHeader() {
  const h = $('header');
  const r = S.route;
  const isTab = TABS.indexOf(r.screen) >= 0;
  const titles = {
    dairy: ['Дневник', 'учебная неделя'],
    schedule: ['Расписание', ''],
    marks: ['Оценки', ''],
    goals: ['Цели', 'личные цели по предметам'],
    events: ['События', 'афиша школы'],
    settings: ['Настройки', 'аккаунт и оформление'],
    subject: [r.title || 'Предмет', 'все оценки по предмету'],
    week: ['Итоги недели', 'с понедельника по воскресенье'],
    day: [r.title || 'Итоги дня', ''],
    critical: ['Критические предметы', ''],
  };
  const [title, sub] = titles[r.screen] || ['ЛинТех 28', ''];
  const actions = [];
  if (!isTab) actions.push('<button class="icon-btn" data-act="back" aria-label="Назад">' + icon('back', 20) + '</button>');
  if (isTab) actions.push('<button class="icon-btn" data-act="search" aria-label="Поиск">' + icon('search', 20) + '</button>');
  actions.push('<button class="icon-btn" data-act="settings" aria-label="Настройки">' + icon('settings', 20) + '</button>');
  h.innerHTML =
    '<div style="flex:1;min-width:0"><h1>' + esc(title) + '</h1>' +
      (sub ? '<div class="sub" id="h-sub">' + esc(sub) + '</div>' : '<div class="sub" id="h-sub"></div>') +
    '</div><div class="h-actions">' + actions.join('') + '</div>';
  h.querySelectorAll('[data-act]').forEach((b) => {
    b.onclick = () => {
      const a = b.dataset.act;
      if (a === 'back') back();
      if (a === 'settings') go({ screen: 'settings', tab: r.tab || r.screen });
      if (a === 'search') openSearch();
    };
  });
}
function setSub(text) {
  const s = $('h-sub');
  if (s) s.textContent = text;
}

// ---------------- навигация ----------------
function renderTabbar() {
  const bar = $('tabbar');
  const tab = S.route.tab || (TABS.indexOf(S.route.screen) >= 0 ? S.route.screen : 'dairy');
  const visible = TABS.indexOf(S.route.screen) >= 0;
  bar.style.display = visible ? 'flex' : 'none';
  bar.innerHTML = TABS.map((t) => {
    const names = { dairy: 'Дневник', schedule: 'Расписание', marks: 'Оценки', goals: 'Цели', events: 'События' };
    return '<button class="tab' + (t === tab ? ' on' : '') + '" data-tab="' + t + '">' +
      '<span class="ico">' + icon(t, 21) + '</span><span>' + names[t] + '</span></button>';
  }).join('');
  bar.querySelectorAll('[data-tab]').forEach((b) => {
    b.onclick = () => { S.weekOffset = S.weekOffset || 0; go({ screen: b.dataset.tab, tab: b.dataset.tab }); };
  });
}

function render() {
  renderHeader();
  renderTabbar();
  const content = $('content');
  content.innerHTML = '';
  const root = el('div', 'screen');
  content.appendChild(root);
  const r = S.route;
  const map = {
    dairy: screenDairy, schedule: screenSchedule, marks: screenMarks,
    goals: screenGoals, events: screenEvents, settings: screenSettings,
    subject: screenSubject, week: screenWeek, day: screenDay, critical: screenCritical,
  };
  (map[r.screen] || screenDairy)(root);
}

// ---------------- дневник ----------------
function screenDairy(root) {
  const offset = S.weekOffset || 0;
  const { monday, sunday } = Diary.weekRange(offset);
  const bar = el('div', 'weekbar');
  bar.innerHTML =
    '<button class="icon-btn" data-w="-1" aria-label="Предыдущая неделя">' + icon('left', 18) + '</button>' +
    '<div class="label">' + fmtDM(monday) + ' — ' + fmtDM(sunday) + '</div>' +
    (offset !== 0 ? '<button class="pill-btn" data-w="0">Сегодня</button>' : '') +
    '<button class="icon-btn" data-w="1" aria-label="Следующая неделя">' + icon('right', 18) + '</button>';
  root.appendChild(bar);
  bar.querySelectorAll('[data-w]').forEach((b) => {
    b.onclick = () => { S.weekOffset = Number(b.dataset.w) === 0 ? 0 : offset + Number(b.dataset.w); render(); };
  });

  const list = el('div');
  root.appendChild(list);
  list.innerHTML = '<div class="skeleton"></div><div class="skeleton"></div>';

  (async () => {
    try {
      const entries = await loadWeek(offset);
      const byDay = new Map();
      for (let i = 0; i < 6; i++) {
        const d = new Date(monday);
        d.setDate(monday.getDate() + i);
        byDay.set(dayKey(d), { date: d, items: [] });
      }
      entries.forEach((e) => {
        const k = dayKey(e.date);
        if (byDay.has(k)) byDay.get(k).items.push(e);
      });
      let any = false;
      list.innerHTML = '';
      for (const { date, items } of byDay.values()) {
        if (!items.length) continue;
        any = true;
        const label = el('div', 'day-label');
        label.innerHTML = '<b>' + esc(DAYS_FULL[(date.getDay() + 6) % 7]) + '</b>' +
          '<span>' + date.getDate() + ' ' + MONTHS[date.getMonth()] + '</span>';
        const summary = el('button', 'pill-btn', 'Итоги');
        summary.style.marginLeft = 'auto';
        summary.onclick = () => go({ screen: 'day', date: date.getTime(), tab: 'dairy', back: { screen: 'dairy', tab: 'dairy' } });
        label.appendChild(summary);
        list.appendChild(label);
        const card = el('div', 'card');
        items.sort((a, b) => a.lessonNumber - b.lessonNumber);
        items.forEach((it) => {
          const row = el('div', 'lesson tap');
          row.innerHTML =
            '<div class="lnum">' + it.lessonNumber + '</div>' +
            '<div class="lbody"><div class="lsubj">' + esc(it.subjectName) + '</div>' +
              (it.theme ? '<div class="ltheme">' + esc(it.theme) + '</div>' : '') +
              (it.homework ? '<div class="lhw">ДЗ · ' + esc(it.homework) + '</div>' : '') +
            '</div>' +
            (it.mark ? '<div class="mark ' + markClass(it.mark) + '">' + esc(it.mark) + '</div>' : '');
          row.onclick = () => openLessonSheet(it);
          card.appendChild(row);
        });
        list.appendChild(card);
      }
      if (!any) list.innerHTML = '<div class="empty">На этой неделе уроков нет</div>';
      setSub('неделя ' + fmtDM(monday) + ' — ' + fmtDM(sunday));
    } catch (e) {
      list.innerHTML = '<div class="card empty">Не удалось загрузить неделю<br><span class="muted">' + esc(e.message) + '</span></div>';
    }
  })();
}

function openLessonSheet(e) {
  const done = isHwDone(e);
  openSheet(
    '<h3>' + esc(e.subjectName) + '</h3>' +
    '<div class="sheet-sub">' + esc(fmtFull(new Date(e.date))) + ' · ' + e.lessonNumber + ' урок</div>' +
    '<div class="sheet-block"><div class="lbl">Тема урока</div><div class="txt">' + esc(e.theme || 'Тема не указана') + '</div></div>' +
    (e.mark ? '<div class="sheet-block"><div class="lbl">Оценка за урок</div><div class="txt"><span class="mark ' + markClass(e.mark) + '" style="display:inline-grid">' + esc(e.mark) + '</span></div></div>' : '') +
    '<div class="sheet-block"><div class="lbl">Домашнее задание</div><div class="txt">' + esc(e.homework || 'Задано не было') + '</div>' +
      (e.homework ? '<label class="row-item" style="margin-top:10px;padding:12px 14px;border-radius:16px;background:rgba(120,126,160,.12)">' +
        '<input type="checkbox" id="hwDone" ' + (done ? 'checked' : '') + ' style="width:18px;height:18px;accent-color:var(--accent)">' +
        '<span class="ri-title">Домашка сделана</span></label>' : '') +
    '</div>' +
    '<div class="btn secondary" id="lsClose" style="margin-top:20px">Закрыть</div>',
    (sheet) => {
      $('lsClose').onclick = closeSheet;
      const cb = $('hwDone');
      if (cb) cb.onchange = () => { toggleHw(e); toast(cb.checked ? 'Домашка отмечена' : 'Отметка снята'); };
    }
  );
}

// ---------------- расписание ----------------
function screenSchedule(root) {
  const todayIdx = (new Date().getDay() + 6) % 7;
  const day = S.schedDay == null ? (todayIdx < 6 ? todayIdx : 0) : S.schedDay;
  const days = el('div', 'days');
  DAYS_SHORT.forEach((d, i) => {
    const b = el('button', 'day-tab' + (i === day ? ' on' : ''), d);
    b.onclick = () => { S.schedDay = i; render(); };
    days.appendChild(b);
  });
  root.appendChild(days);

  const list = el('div');
  root.appendChild(list);
  list.innerHTML = '<div class="skeleton"></div>';

  (async () => {
    try {
      const [schedule, bells] = await Promise.all([loadSchedule(false), loadBells()]);
      const weekdayApi = day + 1; // 1 = понедельник
      const items = schedule.filter((x) => x.weekday === weekdayApi)
        .sort((a, b) => a.lessonOrder - b.lessonOrder);
      const table = (day === 5 ? bells.saturday : bells.weekday) || [];
      list.innerHTML = '';
      if (!items.length) {
        list.innerHTML = '<div class="empty">На ' + DAYS_FULL[day].toLowerCase() + ' уроков нет</div>';
        setSub(DAYS_FULL[day]);
        return;
      }
      const card = el('div', 'card');
      items.forEach((x) => {
        const bell = table.find((b) => Number(b.n) === x.lessonOrder);
        const row = el('div', 'sched-row');
        row.innerHTML =
          '<div class="time"><b>' + (bell ? esc(bell.start) + '–' + esc(bell.end) : x.lessonOrder + ' урок') + '</b>' +
            (bell ? x.lessonOrder + '-й урок' : '') + '</div>' +
          '<div style="flex:1;min-width:0"><div class="sched-subj">' + esc(x.subjectName) + '</div>' +
            '<div class="sched-meta">' + [x.teacherFio, x.room ? 'каб. ' + x.room : ''].filter(Boolean).map(esc).join(' · ') + '</div></div>';
        card.appendChild(row);
      });
      list.appendChild(card);
      setSub(DAYS_FULL[day]);
    } catch (e) {
      list.innerHTML = '<div class="card empty">Сайт с расписанием недоступен<br><span class="muted">' + esc(e.message) + '</span></div>';
    }
  })();
}

// ---------------- оценки ----------------
function screenMarks(root) {
  const load = el('div');
  root.appendChild(load);
  load.innerHTML = '<div class="skeleton"></div><div class="skeleton"></div>';

  (async () => {
    try {
      const subjects = await loadSubjects(false);
      const periodMarks = subjects.flatMap((s) => Diary.currentPeriodLessons(s));
      const overall = Diary.weightedAverage(periodMarks);
      load.innerHTML = '';

      const hero = el('div', 'card hero');
      hero.innerHTML = '<div class="caption">Общий средний балл</div>' +
        '<div class="value" style="color:' + gradeColor(overall) + '">' + fmt2(overall) + '</div>' +
        '<div class="note">' + subjects.length + ' предметов · ' + periodMarks.length + ' оценок</div>';
      load.appendChild(hero);

      load.appendChild(criticalBlock(subjects));

      const weekPill = el('button', 'pill-btn', 'Итоги недели →');
      weekPill.style.margin = '4px 0 14px';
      weekPill.onclick = () => go({ screen: 'week', tab: 'marks', back: { screen: 'marks', tab: 'marks' } });
      load.appendChild(weekPill);

      subjects.slice().sort((a, b) => a.subjectName.localeCompare(b.subjectName, 'ru'))
        .forEach((s) => load.appendChild(subjectCard(s)));
      setSub(subjects.length + ' предметов · ' + periodMarks.length + ' оценок');
    } catch (e) {
      load.innerHTML = '<div class="card empty">Оценки не загрузились<br><span class="muted">' + esc(e.message) + '</span></div>';
    }
  })();
}

function criticalBlock(subjects) {
  const weak = subjects.map((s) => {
    const lessons = Diary.currentPeriodLessons(s);
    const twos = lessons.filter((m) => parseInt(m.mark, 10) <= 2).length;
    return { s, avg: Diary.weightedAverage(lessons), twos };
  }).filter((x) => (x.avg != null && x.avg < 3.5) || x.twos >= 3)
    .sort((a, b) => (a.avg == null ? 99 : a.avg) - (b.avg == null ? 99 : b.avg));

  const box = el('div', 'card critical');
  if (!weak.length) {
    box.innerHTML = '<div class="t">Всё под контролем</div><div class="s">Нет предметов со средним ниже 3,5</div>';
    return box;
  }
  box.innerHTML = '<div class="t">Требуют внимания</div><div class="s">' + weak.length + ' предмет(а) проседают</div>';
  const list = el('div', 'critical-list');
  weak.slice(0, 5).forEach(({ s, avg, twos }) => {
    const chip = el('div', 'critical-chip');
    chip.innerHTML = '<span>' + esc(s.subjectName) + '</span><b>' +
      (avg == null ? 'нет оценок' : fmt2(avg)) + (twos >= 3 ? ' · ' + twos + ' «2»' : '') + '</b>';
    chip.onclick = () => go({ screen: 'subject', id: s.subjectId, tab: 'marks', back: { screen: 'marks', tab: 'marks' } });
    list.appendChild(chip);
  });
  box.appendChild(list);
  if (weak.length > 5) {
    const more = el('button', 'pill-btn', 'Все критические →');
    more.style.marginTop = '10px';
    more.onclick = () => go({ screen: 'critical', tab: 'marks', back: { screen: 'marks', tab: 'marks' } });
    box.appendChild(more);
  }
  return box;
}

function subjectCard(s) {
  const card = el('div', 'card tap subject-card');
  const avg = Diary.currentPeriodAverage(s);
  const recent = s.marks.filter((m) => m.mark).sort((a, b) => a.date - b.date).slice(-20);
  let html =
    '<div class="subject-head">' +
      '<div class="avatar" style="background:' + subjColor(s.subjectId) + '">' + esc(s.subjectName.slice(0, 2)) + '</div>' +
      '<div style="min-width:0"><div class="subject-name">' + esc(s.subjectName) + '</div>' +
        (s.teacherName ? '<div class="teacher">' + esc(s.teacherName) + '</div>' : '') + '</div>' +
      (avg != null ? '<div class="avg-chip" style="color:' + gradeColor(avg) + ';background:rgba(120,126,160,.12)">' + fmt2(avg) + '</div>' : '') +
    '</div>';
  if (recent.length) {
    html += '<div class="chips">' + recent.map((m) =>
      '<span class="chip" style="color:' + markColor(m.mark) + ';background:rgba(120,126,160,.14)">' + esc(m.mark) + '</span>').join('') + '</div>';
  }
  html += '<div class="subj-avg">' + (avg == null ? 'Нет оценок за период' : 'Средний за период: <b style="color:' + gradeColor(avg) + '">' + fmt2(avg) + '</b>') + '</div>';
  card.innerHTML = html;
  card.onclick = () => go({ screen: 'subject', id: s.subjectId, tab: 'marks', back: { screen: 'marks', tab: 'marks' } });
  return card;
}

// ---------------- экран предмета ----------------
function screenSubject(root) {
  const s = (S.subjects || []).find((x) => x.subjectId === S.route.id);
  if (!s) {
    root.innerHTML = '<div class="card empty">Предмет не найден</div>';
    return;
  }
  S.route.title = s.subjectName;
  const avg = Diary.currentPeriodAverage(s);
  const card = el('div', 'card hero');
  card.innerHTML = '<div class="caption">Средний балл за период</div>' +
    '<div class="value" style="color:' + gradeColor(avg) + '">' + fmt2(avg) + '</div>' +
    '<div class="note">' + esc(s.teacherName || '') + '</div>';
  root.appendChild(card);

  const goalBtn = el('button', 'btn secondary', 'Поставить цель');
  goalBtn.style.marginBottom = '14px';
  goalBtn.onclick = () => openGoalSheet(s, 'YEAR');
  root.appendChild(goalBtn);

  const marks = s.marks.filter((m) => m.mark).sort((a, b) => a.date - b.date);
  if (!marks.length) {
    root.appendChild(el('div', 'card empty', 'Оценок по предмету пока нет'));
  } else {
    const card2 = el('div', 'card');
    marks.slice().reverse().forEach((m) => {
      const row = el('div', 'lesson');
      const d = m.date ? new Date(m.date) : null;
      row.innerHTML =
        '<div class="lbody"><div class="lsubj">' + esc(d ? d.getDate() + ' ' + MONTHS[d.getMonth()].slice(0, 3) : '') + '</div>' +
          '<div class="ltheme">' + esc(m.theme || '') + '</div></div>' +
        '<div class="mark ' + markClass(m.mark) + '">' + esc(m.mark) + '</div>';
      card2.appendChild(row);
    });
    root.appendChild(card2);
  }

  const totals = Object.entries(s.totalMarks || {}).sort((a, b) => a[0] - b[0]);
  if (totals.length) {
    const labels = { '360000': '1 четв.', '360001': '2 четв.', '360002': '3 четв.', '360003': '4 четв.', '360011': '1 полуг.', '360012': '2 полуг.' };
    root.appendChild(el('div', 'card', '<div class="muted">' +
      totals.map(([p, m]) => esc(labels[p] || ('Пер. ' + p)) + ': ' + esc(m)).join(' · ') + '</div>'));
  }
  setSub(s.subjectName);
}

// ---------------- цели ----------------
function screenGoals(root) {
  const load = el('div');
  root.appendChild(load);

  (async () => {
    const goals = loadGoals();
    let subjects = [];
    try { subjects = await loadSubjects(false); } catch (e) {}
    const byId = {};
    subjects.forEach((s) => { byId[s.subjectId] = s; });

    load.innerHTML = '';
    load.appendChild(criticalBlock(subjects));

    if (!goals.length) {
      load.innerHTML += '<div class="card empty"><div class="flag">🚩</div><b>Целей пока нет</b>' +
        '<div style="margin-top:6px">Открой «Оценки» → предмет → «Поставить цель»</div></div>';
      setSub('личные цели по предметам');
      return;
    }
    goals.sort((a, b) => a.subjectName.localeCompare(b.subjectName, 'ru')).forEach((g) => {
      const subject = byId[g.subjectId];
      const lessons = subject ? Diary.marksForPeriod(subject, g.periodKey) : [];
      const hint = subject ? Diary.hint(lessons, g) : 'Нет данных об оценках';
      const achieved = subject ? Diary.needed(lessons, g.targetMark, g.allowedMarks) === 0 : false;

      const card = el('div', 'card tap');
      card.innerHTML =
        '<div class="subject-head">' +
          '<div class="avatar" style="background:' + subjColor(g.subjectId) + '">' + esc(g.subjectName.slice(0, 2)) + '</div>' +
          '<div><div class="subject-name">' + esc(g.subjectName) + '</div>' +
            '<div class="goal-meta">' + esc(Diary.period(g.periodKey).label) + ' · цель «' + g.targetMark + '»</div></div>' +
          '<div class="goal-target" style="color:' + markColor(g.targetMark) + ';background:rgba(120,126,160,.14)">' + g.targetMark + '</div>' +
        '</div>' +
        '<div class="goal-hint' + (achieved ? ' ok' : '') + '"><span>' + esc(hint) + '</span><b>Изменить</b></div>';
      card.onclick = () => subject ? openGoalSheet(subject, g.periodKey) : go({ screen: 'marks', tab: 'goals' });
      load.appendChild(card);
    });
    setSub(goals.length + ' целей · следи за прогрессом');
  })();
}

function openGoalSheet(subject, initialPeriodKey) {
  const existing = findGoal(subject.subjectId, initialPeriodKey || 'YEAR');
  const gs = {
    periodKey: initialPeriodKey || 'YEAR',
    targetMark: existing ? existing.targetMark : 5,
    allowed: new Set(existing ? existing.allowedMarks : [5]),
  };

  function paint() {
    const lessons = Diary.marksForPeriod(subject, gs.periodKey);
    const avg = Diary.weightedAverage(lessons);
    const allowed = Array.from(gs.allowed).sort((a, b) => b - a);
    const need = Diary.needed(lessons, gs.targetMark, allowed);
    const chips = lessons.length
      ? lessons.map((m) => '<span class="chip" style="color:' + markColor(m.mark) + ';background:rgba(120,126,160,.14)">' + esc(m.mark) + '</span>').join('')
      : '<span class="muted">Оценок за этот период нет</span>';
    let result, cls;
    if (!allowed.length) { result = 'Выбери хотя бы одну оценку'; cls = 'err'; }
    else if (need == null) { result = 'С такими оценками цель недостижима'; cls = 'err'; }
    else if (need === 0) { result = 'Цель уже достигнута'; cls = 'ok'; }
    else { result = 'Нужно ещё ' + need + ' оцен' + goalPlural(need) + ' «' + Math.max.apply(null, allowed) + '»'; cls = 'todo'; }

    openSheet(
      '<h3>Цель по предмету</h3>' +
      '<div class="sheet-sub">' + esc(subject.subjectName) + '</div>' +
      '<div class="sheet-block"><div class="lbl">Текущие оценки</div><div class="chips">' + chips + '</div>' +
        (lessons.length ? '<div class="subj-avg">Средний: <b>' + fmt2(avg) + '</b></div>' : '') + '</div>' +
      '<div class="sheet-block"><div class="lbl">За какой период</div>' +
        '<select class="field" id="gsPeriod">' + Diary.periods.map((p) =>
          '<option value="' + p.key + '"' + (p.key === gs.periodKey ? ' selected' : '') + '>' + esc(p.label) + '</option>').join('') + '</select></div>' +
      '<div class="sheet-block"><div class="lbl">Какую оценку хочешь</div><div class="row2">' +
        [5, 4, 3].map((m) => '<button class="pill-btn' + (gs.targetMark === m ? ' accent' : '') + '" data-target="' + m + '">' + m + '</button>').join('') +
      '</div></div>' +
      '<div class="sheet-block"><div class="lbl">Какими оценками набирать</div><div class="row2">' +
        [5, 4, 3, 2].map((m) => '<button class="pill-btn' + (gs.allowed.has(m) ? ' accent' : '') + '" data-allowed="' + m + '">' + m + '</button>').join('') +
      '</div><div class="muted" style="margin-top:6px">Можно выбрать несколько — посчитаем по лучшей</div></div>' +
      '<div class="card" style="margin-top:16px;text-align:center;font-weight:700;color:' +
        (cls === 'ok' ? 'var(--success)' : cls === 'err' ? 'var(--danger)' : 'var(--text)') + '">' + esc(result) + '</div>' +
      '<button class="btn" id="gsSave"' + (allowed.length ? '' : ' disabled') + '>Установить цель</button>' +
      (existing ? '<button class="btn danger" id="gsDelete" style="margin-top:10px">Удалить цель</button>' : ''),
      (sheet) => {
        $('gsPeriod').onchange = (ev) => { gs.periodKey = ev.target.value; paint(); };
        sheet.querySelectorAll('[data-target]').forEach((b) => {
          b.onclick = () => { gs.targetMark = Number(b.dataset.target); paint(); };
        });
        sheet.querySelectorAll('[data-allowed]').forEach((b) => {
          b.onclick = () => {
            const v = Number(b.dataset.allowed);
            if (gs.allowed.has(v)) gs.allowed.delete(v); else gs.allowed.add(v);
            paint();
          };
        });
        $('gsSave').onclick = () => {
          if (!gs.allowed.size) return;
          upsertGoal({
            subjectId: subject.subjectId,
            subjectName: subject.subjectName,
            periodKey: gs.periodKey,
            targetMark: gs.targetMark,
            allowedMarks: Array.from(gs.allowed).sort((a, b) => b - a),
          });
          closeSheet();
          toast('Цель сохранена');
          render();
        };
        const del = $('gsDelete');
        if (del) del.onclick = () => {
          removeGoal(subject.subjectId, gs.periodKey);
          closeSheet();
          toast('Цель удалена');
          render();
        };
      }
    );
  }
  paint();
}

// ---------------- итоги недели ----------------
function screenWeek(root) {
  const load = el('div');
  root.appendChild(load);
  load.innerHTML = '<div class="skeleton"></div>';

  (async () => {
    try {
      const subjects = await loadSubjects(false);
      const { monday, sunday } = Diary.weekRange(0);
      const from = monday.getTime(), to = sunday.getTime() + 86399999;
      const marks = [];
      subjects.forEach((s) => s.marks.forEach((m) => {
        const t = m.date ? new Date(m.date).getTime() : 0;
        if (m.mark && t >= from && t <= to) marks.push({ ...m, subjectName: s.subjectName, subjectId: s.subjectId });
      }));
      load.innerHTML = '';
      const avg = Diary.weightedAverage(marks);
      const hero = el('div', 'card hero');
      hero.innerHTML = '<div class="caption">Средний балл за неделю</div>' +
        '<div class="value" style="color:' + gradeColor(avg) + '">' + fmt2(avg) + '</div>' +
        '<div class="note">' + marks.length + ' оценок</div>';
      load.appendChild(hero);

      if (!marks.length) {
        load.appendChild(el('div', 'card empty', 'На этой неделе оценок не было'));
      } else {
        const best = {}, worst = {};
        marks.forEach((m) => {
          const n = parseInt(m.mark, 10);
          (best[m.subjectId] = best[m.subjectId] || []).push(n);
          (worst[m.subjectId] = worst[m.subjectId] || []).push(n);
        });
        const avgOf = (arr) => arr.reduce((a, b) => a + b, 0) / arr.length;
        const ids = Object.keys(best);
        const bestId = ids.sort((a, b) => avgOf(best[b]) - avgOf(best[a]))[0];
        const worstId = ids.slice().sort((a, b) => avgOf(worst[a]) - avgOf(worst[b]))[0];
        const nameOf = (id) => (subjects.find((s) => String(s.subjectId) === String(id)) || {}).subjectName || '';
        const tiles = el('div', 'card');
        tiles.innerHTML =
          '<div class="lesson"><div class="lbody"><div class="lsubj">Лучший предмет</div>' +
            '<div class="ltheme">' + esc(nameOf(bestId)) + '</div></div>' +
            '<div class="mark m5">' + fmt2(avgOf(best[bestId])) + '</div></div>' +
          (worstId !== bestId ? '<div class="lesson"><div class="lbody"><div class="lsubj">Просел</div>' +
            '<div class="ltheme">' + esc(nameOf(worstId)) + '</div></div>' +
            '<div class="mark m2">' + fmt2(avgOf(worst[worstId])) + '</div></div>' : '');
        load.appendChild(tiles);

        const card = el('div', 'card');
        marks.sort((a, b) => new Date(a.date) - new Date(b.date)).forEach((m) => {
          const d = new Date(m.date);
          const row = el('div', 'lesson');
          row.innerHTML = '<div class="lbody"><div class="lsubj">' + esc(m.subjectName) + '</div>' +
            '<div class="ltheme">' + d.getDate() + ' ' + MONTHS[d.getMonth()].slice(0, 3) + '</div></div>' +
            '<div class="mark ' + markClass(m.mark) + '">' + esc(m.mark) + '</div>';
          card.appendChild(row);
        });
        load.appendChild(card);
      }
      setSub(fmtDM(monday) + ' — ' + fmtDM(sunday));
    } catch (e) {
      load.innerHTML = '<div class="card empty">Не удалось загрузить: ' + esc(e.message) + '</div>';
    }
  })();
}

// ---------------- итоги дня ----------------
function screenDay(root) {
  const date = new Date(S.route.date || Date.now());
  const { monday } = Diary.weekRange(0);
  const dayOffset = Math.round((new Date(date.getFullYear(), date.getMonth(), date.getDate()) - new Date(monday.getFullYear(), monday.getMonth(), monday.getDate())) / 86400000);
  const weekOffset = dayOffset >= 0 && dayOffset <= 6 ? 0 : Math.floor(dayOffset / 7);

  const load = el('div');
  root.appendChild(load);
  load.innerHTML = '<div class="skeleton"></div>';

  (async () => {
    try {
      const entries = (await loadWeek(weekOffset)).filter((e) => dayKey(e.date) === dayKey(date));
      load.innerHTML = '';
      const marks = entries.filter((e) => e.mark);
      const avg = Diary.average(marks.map((e) => parseInt(e.mark, 10)));
      const hero = el('div', 'card hero');
      hero.innerHTML = '<div class="caption">Средний балл за день</div>' +
        '<div class="value" style="color:' + gradeColor(avg) + '">' + fmt2(avg) + '</div>' +
        '<div class="note">' + fmtFull(date) + ' · ' + marks.length + ' оценок</div>';
      load.appendChild(hero);

      if (!marks.length) load.appendChild(el('div', 'card empty', 'Сегодня оценок не было'));

      const hw = entries.filter((e) => e.homework);
      const done = hw.filter(isHwDone).length;
      if (hw.length) {
        const card = el('div', 'card');
        card.innerHTML = '<div class="critical"><div class="t">Домашнее задание</div>' +
          '<div class="s">' + done + ' из ' + hw.length + ' выполнено</div></div>';
        hw.forEach((e) => {
          const row = el('label', 'lesson');
          row.style.cursor = 'pointer';
          row.innerHTML = '<input type="checkbox" ' + (isHwDone(e) ? 'checked' : '') + ' style="width:18px;height:18px;accent-color:var(--accent)">' +
            '<div class="lbody"><div class="lsubj">' + esc(e.subjectName) + '</div>' +
            '<div class="ltheme">' + esc(e.homework) + '</div></div>';
          row.querySelector('input').onchange = (ev) => {
            if (ev.target.checked !== isHwDone(e)) toggleHw(e);
            render();
          };
          card.appendChild(row);
        });
        load.appendChild(card);
      }
      setSub(fmtFull(date));
    } catch (e) {
      load.innerHTML = '<div class="card empty">Не удалось загрузить: ' + esc(e.message) + '</div>';
    }
  })();
}

// ---------------- критические предметы ----------------
function screenCritical(root) {
  const load = el('div');
  root.appendChild(load);
  load.innerHTML = '<div class="skeleton"></div>';

  (async () => {
    try {
      const subjects = await loadSubjects(false);
      const weak = subjects.map((s) => {
        const lessons = Diary.currentPeriodLessons(s);
        return {
          s,
          avg: Diary.weightedAverage(lessons),
          twos: lessons.filter((m) => parseInt(m.mark, 10) <= 2).length,
          total: lessons.length,
        };
      }).filter((x) => (x.avg != null && x.avg < 3.5) || x.twos >= 3)
        .sort((a, b) => (a.avg == null ? 99 : a.avg) - (b.avg == null ? 99 : b.avg));
      load.innerHTML = '';
      if (!weak.length) {
        load.appendChild(el('div', 'card empty', 'Критических предметов нет. Всё под контролем.'));
        return;
      }
      weak.forEach(({ s, avg, twos, total }) => {
        const reasons = [];
        if (avg != null && avg < 3.5) reasons.push('средний балл ' + fmt2(avg));
        if (twos >= 3) reasons.push(twos + ' «2»');
        const card = el('div', 'card tap');
        card.innerHTML = '<div class="subject-head">' +
          '<div class="avatar" style="background:' + subjColor(s.subjectId) + '">' + esc(s.subjectName.slice(0, 2)) + '</div>' +
          '<div><div class="subject-name">' + esc(s.subjectName) + '</div>' +
          '<div class="teacher">' + esc(reasons.join(' · ')) + ' · всего оценок: ' + total + '</div></div>' +
          '<div class="avg-chip" style="color:var(--danger);background:rgba(255,90,90,.12)">' + fmt2(avg) + '</div></div>' +
          '<div class="goal-hint"><span>Поставь цель и держи балл</span><b>Цель →</b></div>';
        card.onclick = () => openGoalSheet(s, 'YEAR');
        load.appendChild(card);
      });
      setSub(weak.length + ' требуют внимания');
    } catch (e) {
      load.innerHTML = '<div class="card empty">Не удалось загрузить: ' + esc(e.message) + '</div>';
    }
  })();
}

// ---------------- события ----------------
function screenEvents(root) {
  const load = el('div');
  root.appendChild(load);
  load.innerHTML = '<div class="skeleton"></div>';

  (async () => {
    try {
      const events = await loadEvents();
      load.innerHTML = '';
      if (!events.length) {
        load.innerHTML = '<div class="card empty"><div class="flag">🎈</div>Событий пока нет</div>';
        setSub('афиша школы');
        return;
      }
      events.forEach((ev) => {
        const card = el('div', 'card');
        const d = ev.date ? new Date(ev.date + 'T00:00:00') : null;
        card.innerHTML =
          '<div class="event-date">' + (d ? d.getDate() + ' ' + MONTHS[d.getMonth()] : esc(ev.date || '')) +
            (ev.time ? ' · ' + esc(ev.time) : '') + '</div>' +
          '<div class="event-title">' + esc(ev.title || '') + '</div>' +
          '<div class="event-text">' + (ev.text ? esc(ev.text) : (ev.content || '')) + '</div>' +
          (ev.url ? '<a class="event-link" href="' + esc(ev.url) + '" target="_blank" rel="noopener">Открыть →</a>' : '');
        load.appendChild(card);
      });
      setSub(events.length + ' событий');
    } catch (e) {
      load.innerHTML = '<div class="card empty">События не загрузились: ' + esc(e.message) + '</div>';
    }
  })();
}

// ---------------- настройки ----------------
function screenSettings(root) {
  const theme = Prefs.get(THEME_KEY, 'system');
  root.appendChild(el('div', 'section-title', 'Аккаунт'));
  const prof = el('div', 'card');
  const u = S.user;
  prof.innerHTML = '<div class="subject-head">' +
    '<div class="avatar" style="background:linear-gradient(140deg,var(--accent),var(--accent2))">' + esc((u && u.fio || 'Л').slice(0, 1)) + '</div>' +
    '<div><div class="subject-name">' + esc(u ? u.fio : 'Ученик') + '</div>' +
    '<div class="teacher">' + esc(u && u.uchName ? u.uchName : 'ЛинТех 28') + '</div></div></div>';
  root.appendChild(prof);

  root.appendChild(el('div', 'section-title', 'Оформление'));
  const themeCard = el('div', 'card');
  themeCard.innerHTML = '<div class="ri-title" style="margin-bottom:10px">Тема</div>' +
    '<div class="segment">' +
      [['system', 'Системная'], ['light', 'Светлая'], ['dark', 'Тёмная'], ['zhan', 'Книжная']]
        .map(([k, l]) => '<button data-theme="' + k + '" class="' + (theme === k ? 'on' : '') + '">' + l + '</button>').join('') +
    '</div><div class="muted" style="margin-top:10px">Книжная — фон с фотографией школы.</div>';
  root.appendChild(themeCard);
  themeCard.querySelectorAll('[data-theme]').forEach((b) => {
    b.onclick = () => { setTheme(b.dataset.theme); screenSettingsRefresh(root); };
  });
  attachThemeButtons();

  root.appendChild(el('div', 'section-title', 'Данные'));
  const dataCard = el('div', 'card');
  dataCard.innerHTML =
    row('🧹', 'Очистить кэш', 'Загрузить данные заново', 'Очистить', 'clear') +
    row('🔄', 'Обновить оценки и расписание', 'Сбросить сохранённые данные', '', 'refresh');
  root.appendChild(dataCard);
  dataCard.querySelector('[data-act="clear"]').onclick = () => {
    S.diary.clear(); S.subjects = null; S.schedule = null;
    toast('Кэш очищен');
  };
  dataCard.querySelector('[data-act="refresh"]').onclick = () => {
    S.subjects = null; S.schedule = null; S.subjectsAt = 0; S.scheduleAt = 0;
    toast('Данные обновятся при открытии экранов');
  };

  root.appendChild(el('div', 'section-title', 'О приложении'));
  const about = el('div', 'card');
  about.innerHTML =
    row('📱', 'Версия', '3.0.4 · ios26', '', '') +
    '<div class="row-item" data-act="releases" style="cursor:pointer"><div class="ri-ico">⬇</div>' +
      '<div class="ri-body"><div class="ri-title">Релизы и APK</div>' +
      '<div class="ri-sub">github.com/Bossbabos354/lintech-diary</div></div><div class="ri-value">↗</div></div>' +
    '<div class="row-item" data-act="logout" style="cursor:pointer"><div class="ri-ico">🚪</div>' +
      '<div class="ri-body"><div class="ri-title" style="color:var(--danger)">Выйти из аккаунта</div></div></div>';
  root.appendChild(about);
  about.querySelector('[data-act="releases"]').onclick = () =>
    window.open('https://github.com/Bossbabos354/lintech-diary/releases', '_blank');
  about.querySelector('[data-act="logout"]').onclick = logout;

  setSub(u ? u.fio : 'аккаунт и оформление');
}

function row(ico, title, sub, value, act) {
  return '<div class="row-item"' + (act ? ' data-act="' + act + '"' : '') + '>' +
    '<div class="ri-ico">' + ico + '</div>' +
    '<div class="ri-body"><div class="ri-title">' + esc(title) + '</div>' +
      (sub ? '<div class="ri-sub">' + esc(sub) + '</div>' : '') + '</div>' +
    (value ? '<div class="ri-value">' + esc(value) + '</div>' : '') + '</div>';
}

function screenSettingsRefresh(root) {
  root.innerHTML = '';
  screenSettings(root);
}

function attachThemeButtons() {
  document.querySelectorAll('.segment [data-theme]').forEach((b) => {
    b.classList.toggle('on', b.dataset.theme === Prefs.get(THEME_KEY, 'system'));
  });
}

// ---------------- поиск ----------------
function openSearch() {
  openSheet(
    '<h3>Поиск</h3><div class="sheet-sub">Предмет, тема урока или домашнее задание</div>' +
    '<div class="sheet-block"><input class="field" id="q" placeholder="Например: алгебра" autocomplete="off"></div>' +
    '<div id="qres" style="margin-top:12px"></div>',
    () => {
      const input = $('q');
      const res = $('qres');
      let timer = null;
      const run = async () => {
        const q = input.value.trim().toLowerCase();
        if (q.length < 2) { res.innerHTML = '<div class="muted">Введи хотя бы два символа</div>'; return; }
        res.innerHTML = '<div class="spinner" style="margin:16px auto"></div>';
        const out = [];
        try {
          const subjects = await loadSubjects(false);
          subjects.forEach((s) => {
            if (s.subjectName.toLowerCase().indexOf(q) >= 0) out.push({ type: 'subj', s });
          });
        } catch (e) {}
        try {
          const entries = await loadWeek(S.weekOffset || 0);
          entries.forEach((e) => {
            const hay = [e.subjectName, e.theme, e.homework].join(' ').toLowerCase();
            if (hay.indexOf(q) >= 0) out.push({ type: 'lesson', e });
          });
        } catch (e) {}
        res.innerHTML = '';
        if (!out.length) { res.innerHTML = '<div class="muted">Ничего не найдено</div>'; return; }
        out.slice(0, 20).forEach((x) => {
          const b = el('div', 'lesson tap');
          if (x.type === 'subj') {
            b.innerHTML = '<div class="lnum">★</div><div class="lbody"><div class="lsubj">' +
              esc(x.s.subjectName) + '</div><div class="ltheme">предмет</div></div>';
            b.onclick = () => { closeSheet(); go({ screen: 'subject', id: x.s.subjectId, tab: 'marks', back: { screen: 'marks', tab: 'marks' } }); };
          } else {
            b.innerHTML = '<div class="lnum">' + x.e.lessonNumber + '</div><div class="lbody"><div class="lsubj">' +
              esc(x.e.subjectName) + '</div><div class="ltheme">' + esc(x.e.theme || x.e.homework || '') + '</div></div>';
            b.onclick = () => { closeSheet(); openLessonSheet(x.e); };
          }
          res.appendChild(b);
        });
      };
      input.addEventListener('input', () => { clearTimeout(timer); timer = setTimeout(run, 250); });
      input.focus();
    }
  );
}

// ---------------- вход и запуск ----------------
function showLogin() {
  $('app').hidden = true;
  $('login').hidden = false;
  $('login').innerHTML =
    '<div class="logo-badge">28</div>' +
    '<h1>ЛинТех 28</h1>' +
    '<div class="sub">Электронный дневник школы №28</div>' +
    '<div class="card" style="padding:18px">' +
      '<label class="lbl" style="font-size:11px;font-weight:800;letter-spacing:.6px;text-transform:uppercase;color:var(--text3)">Логин</label>' +
      '<input class="field" id="lgLogin" autocapitalize="none" autocomplete="username" placeholder="Логин">' +
      '<label class="lbl" style="display:block;margin-top:12px;font-size:11px;font-weight:800;letter-spacing:.6px;text-transform:uppercase;color:var(--text3)">Пароль</label>' +
      '<input class="field" id="lgPass" type="password" autocomplete="current-password" placeholder="Пароль">' +
      '<button class="btn" id="lgBtn" style="margin-top:16px">Войти</button>' +
      '<div class="login-error" id="lgErr"></div>' +
    '</div>' +
    '<div class="muted" style="text-align:center;margin-top:14px">Данные берутся с сайта журнала через прокси</div>';

  const doLogin = async () => {
    const login = $('lgLogin').value.trim();
    const pass = $('lgPass').value;
    const err = $('lgErr');
    const btn = $('lgBtn');
    err.textContent = '';
    if (!login || !pass) { err.textContent = 'Введите логин и пароль'; return; }
    btn.disabled = true;
    btn.textContent = 'Вхожу…';
    try {
      const res = await Diary.login(login, pass);
      if (!res) throw new Error('Неверный логин или пароль');
      Prefs.set('diary_login', login);
      Prefs.set('diary_password', pass);
      Prefs.setStudentId(res.studentId);
      Prefs.set('diary_user_id', String(res.userId));
      try { Prefs.setUchYear(await Diary.getUchYear()); } catch (e) {}
      S.user = { fio: res.fio, uchName: '' };
      try { const u = await Diary.getUserData(); S.user = u; } catch (e) {}
      showApp();
    } catch (e) {
      err.textContent = e.message || 'Не удалось войти';
    } finally {
      btn.disabled = false;
      btn.textContent = 'Войти';
    }
  };
  $('lgBtn').onclick = doLogin;
  $('lgPass').addEventListener('keydown', (e) => { if (e.key === 'Enter') doLogin(); });
}

function logout() {
  ['diary_login', 'diary_password', 'diary_student_id', 'diary_uch_year', 'diary_user_id', 'diary_class_id']
    .forEach(Prefs.del);
  S.subjects = null; S.schedule = null; S.diary.clear(); S.user = null;
  showLogin();
}

function showApp() {
  $('login').hidden = true;
  $('app').hidden = false;
  S.route = { screen: 'dairy', tab: 'dairy' };
  S.weekOffset = 0;
  render();
  // Расписание грузим в фоне, чтобы вкладка открывалась мгновенно.
  setTimeout(() => { loadSchedule(false).catch(() => {}); }, 700);
}

async function boot() {
  applyTheme();
  const started = Date.now();
  const splash = $('splash');
  const hideSplash = () => {
    const left = Math.max(0, 1000 - (Date.now() - started));
    setTimeout(() => {
      splash.classList.add('out');
      setTimeout(() => splash.remove(), 420);
    }, left);
  };

  const login = Prefs.login();
  const pass = Prefs.password();
  if (!login || !pass) { hideSplash(); showLogin(); return; }

  // Сессия восстанавливается локально, экран показываем сразу —
  // сетевые данные догружаются под скелетонами.
  await Diary.restoreSession(login, pass);
  showApp();
  hideSplash();

  if (!Prefs.uchYear()) {
    Diary.getUchYear().then((y) => Prefs.setUchYear(y)).catch(() => {});
  }
  Diary.getUserData().then((u) => {
    S.user = u;
    if (S.route.screen === 'settings') render();
  }).catch(() => {});
}

window.LT28 = {
  state: S, go, render, showApp, showLogin,
  setUser: (u) => { S.user = u; },
};

boot();
})();
