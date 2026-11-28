/* Веб-клиент «Электронного дневника» — порт ApiClient.kt на JavaScript.
 * Сессия АВЕРС: Ext JS cookies (ys-user, ys-password, ys-userId) с префиксами
 * s:/n: и JS escape()-кодированием. Браузер не может ставить Cookie вручную,
 * поэтому все запросы идут через proxy.php, который подставляет заголовок.
 */
const Diary = (() => {
  const BASE_URL = 'https://journal.school28-kirov.ru';
  // Адрес прокси: proxy.php на Beget или Worker на Cloudflare
  const PROXY = window.LINTECH_PROXY || 'proxy.php';

  // ---- JS escape() — Ext JS CookieProvider encodeValue ----
  function jsEscape(s) {
    let out = '';
    for (const ch of s) {
      const code = ch.codePointAt(0);
      if (code < 256) {
        out += /[A-Za-z0-9*_\-.@/]/.test(ch) ? ch : '%' + code.toString(16).toUpperCase().padStart(2, '0');
      } else {
        out += '%u' + code.toString(16).toUpperCase().padStart(4, '0');
      }
    }
    return out;
  }
  const encodeExtStr = (v) => jsEscape('s:' + v);
  const encodeExtNum = (v) => jsEscape('n:' + v);

  // ---- SHA-1. WebCrypto доступен только в HTTPS/localhost; на Beget по http
  // мы на обычном http, поэтому fallback на чисто-JS реализацию.
  function sha1js(input) {
    const enc = new TextEncoder();
    const bytes = enc.encode(input);
    const ml = bytes.length;
    const blocks = (((ml + 8) >> 6) + 1) * 16;
    const words = new Array(blocks).fill(0);
    for (let i = 0; i < ml; i++) words[i >> 2] |= (bytes[i] & 0xff) << (24 - (i % 4) * 8);
    words[ml >> 2] |= 0x80 << (24 - (ml % 4) * 8);
    words[blocks - 1] = ml * 8;

    let a = 0x67452301, b = 0xEFCDAB89, c = 0x98BADCFE, d = 0x10325476, e = 0xC3D2E1F0;
    const w = new Array(80);
    for (let i = 0; i < words.length; i += 16) {
      for (let j = 0; j < 16; j++) w[j] = words[i + j];
      for (let j = 16; j < 80; j++) {
        const n = w[j - 3] ^ w[j - 8] ^ w[j - 14] ^ w[j - 16];
        w[j] = (n << 1 | n >>> 31) >>> 0;
      }
      let t = 0;
      let A = a, B = b, C = c, D = d, E = e;
      for (let j = 0; j < 80; j++) {
        let f, k;
        if (j < 20) { f = (B & C) | (~B & D); k = 0x5A827999; }
        else if (j < 40) { f = B ^ C ^ D; k = 0x6ED9EBA1; }
        else if (j < 60) { f = (B & C) | (B & D) | (C & D); k = 0x8F1BBCDC; }
        else { f = B ^ C ^ D; k = 0xCA62C1D6; }
        const temp = (((A << 5 | A >>> 27) + f + E + k + w[j]) >>> 0);
        E = D; D = C; C = (B << 30 | B >>> 2) >>> 0; B = A; A = temp;
      }
      a = (a + A) >>> 0; b = (b + B) >>> 0; c = (c + C) >>> 0; d = (d + D) >>> 0; e = (e + E) >>> 0;
    }
    return [a, b, c, d, e].map(x => x.toString(16).padStart(8, '0')).join('');
  }

  async function sha1hex(input) {
    try {
      if (crypto && crypto.subtle && typeof crypto.subtle.digest === 'function') {
        const buf = await crypto.subtle.digest('SHA-1', new TextEncoder().encode(input));
        return Array.from(new Uint8Array(buf)).map(b => b.toString(16).padStart(2, '0')).join('');
      }
    } catch (e) { /* падаем в JS-реализацию */ }
    return sha1js(input);
  }

  // ---- сессионные куки ----
  let session = { user: null, password: null, userId: null };
  function buildCookie(endpoint) {
    if (!endpoint.includes('/act/')) return '';
    const parts = [];
    if (session.user !== null) parts.push('ys-user=' + session.user);
    if (session.password !== null) parts.push('ys-password=' + session.password);
    if (session.userId !== null) parts.push('ys-userId=' + session.userId);
    return parts.join('; ');
  }

  // ---- HTTP через прокси ----
  async function request(endpoint, params, isGet) {
    const cookie = buildCookie(endpoint);
    const ctrl = new AbortController();
    // Активность сервера АВЕРС: таймаут на весь запрос как в приложении (120 c).
    const timer = setTimeout(() => ctrl.abort(), 120000);
    let resp;
    try {
      resp = await fetch(PROXY, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ e: endpoint, p: params || {}, cookie, get: isGet }),
        signal: ctrl.signal,
      });
    } catch (e) {
      clearTimeout(timer);
      if (e.name === 'AbortError') throw new Error('Сервер журнала не отвечает (таймаут). Попробуйте ещё раз.');
      throw e;
    } finally {
      clearTimeout(timer);
    }
    const data = await resp.json();
    if (!data.ok) throw new Error(data.error || 'Ошибка прокси');
    return data.text;
  }
  const post = (ep, p) => request(ep, p, false);
  const get = (ep) => request(ep, {}, true);

  // ---- разбор ответа: new Date(...) -> строка дд.мм.гггг, потом JSON ----
  function parseArray(json) {
    let clean = json;
    try {
      clean = json.replace(/new\s+Date\((\d{4}),(\d{1,2}),(\d{1,2})[^)]*\)/g, (m, y, mo, d) => {
        const dt = new Date(+y, +mo, Math.max(+d, 1));
        const dd = String(dt.getDate()).padStart(2, '0');
        const mm = String(dt.getMonth() + 1).padStart(2, '0');
        return '"' + dd + '.' + mm + '.' + y + '"';
      });
    } catch (e) { /* невалидный JSON — оставляем как есть */ }
    try {
      const arr = JSON.parse(clean);
      return Array.isArray(arr) ? arr : [];
    } catch (e) {
      return [];
    }
  }

  // ---- утилиты для дат ----
  const sdf = (d) => {
    const dd = String(d.getDate()).padStart(2, '0');
    const mm = String(d.getMonth() + 1).padStart(2, '0');
    return dd + '.' + mm + '.' + d.getFullYear();
  };
  const parseJsDate = (v) => {
    if (typeof v === 'string') {
      const parts = v.split('.');
      if (parts.length === 3) {
        return new Date(+parts[2], +parts[1] - 1, +parts[0]);
      }
    }
    return null;
  };

  // начало недели (понедельник), сдвиг weekOffset недель
  function weekRange(weekOffset) {
    const now = new Date();
    const monday = new Date(now.getFullYear(), now.getMonth(), now.getDate());
    monday.setTime(monday.getTime() + weekOffset * 7 * 86400000);
    let dow = monday.getDay(); // 0=вс..6=сб
    if (dow === 0) dow = 7;
    monday.setDate(monday.getDate() - (dow - 1));
    monday.setHours(0, 0, 0, 0);
    const sunday = new Date(monday);
    sunday.setDate(sunday.getDate() + 6);
    sunday.setHours(23, 59, 59, 999);
    return { monday, sunday };
  }

  // ---- ПУБЛИЧНЫЕ МЕТОДЫ ----
  async function login(login, password) {
    const hashed = await sha1hex(password);
    const resp = await post('/login', { l: login, p: hashed });
    if (resp.trim() === '[]' || resp.trim() === '[error_symbol]') return null;

    const data = parseArray(resp);
    if (!data.length) return null;

    const row = data[0];
    const userId = Number(row[0]);
    const fio = row[5] || '';
    const studentId = Number(row[row.length - 1]) || userId;

    session.user = encodeExtStr(login);
    session.password = encodeExtStr(hashed);
    session.userId = encodeExtNum(userId);

    // Подтверждение сессии не ждём: куки уже выставлены, экран не должен простаивать.
    post('/auth', { uId: String(userId), act: '1' }).catch(() => {});

    return { fio, uchName: '', uchId: 1, studentId, userId };
  }

  // Асинхронна: сессия готова только после расчёта SHA-1.
  // Вызывающий ОБЯЗАН дождаться (await), иначе первые запросы уйдут без cookie.
  async function restoreSession(savedLogin, savedPassword) {
    if (!savedLogin || !savedPassword) {
      session = { user: null, password: null, userId: null };
      return;
    }
    const h = await sha1hex(savedPassword);
    session.user = encodeExtStr(savedLogin);
    session.password = encodeExtStr(h);
    session.userId = encodeExtNum(-1);
  }

  async function getUserData() {
    const data = parseArray(await post('/act/get_user_data', {}));
    if (!data.length) throw new Error('Нет данных пользователя');
    const row = data[0];
    const fio = row[0] + ' ' + ((row[1] || '').charAt(0) || '') + '. ' + ((row[2] || '').charAt(0) || '') + '.';
    const uchName = row[3] || '';
    const uchId = Number(row[4]) || 0;
    return { fio, uchName, uchId, studentId: 0 };
  }

  async function getUchYear() {
    const data = parseArray(await post('/act/get_uch_year', { currentDate: sdf(new Date()) }));
    if (data.length) {
      const y = parseInt(data[0][0], 10);
      if (!isNaN(y)) return y;
    }
    return new Date().getFullYear();
  }

  async function getStudentClass(studentId, uchYear) {
    const data = parseArray(await post('/act/GET_STUDENT_CLASS', {
      currentDate: sdf(new Date()), student: String(studentId),
      uchYear: String(uchYear), uchId: '1',
    }));
    if (!data.length) throw new Error('Нет данных класса');
    return Number(data[0][0]);
  }

  async function getDairy(classId, studentId, beginDate, endDate) {
    const [subjRaw, dairyRaw, personsRaw] = await Promise.all([
      post('/act/GET_DAIRY_CLASS_SUBJECTS', { pClassesIds: '', cls: String(classId) }),
      post('/act/GET_STUDENT_DAIRY', {
        pClassesIds: '', student: String(studentId), cls: String(classId),
        begin_dt: sdf(beginDate), end_dt: sdf(endDate),
      }),
      post('/act/GET_PERSON_STORE_DATA', { uchId: '1' }),
    ]);

    const subjData = parseArray(subjRaw);
    const subjectMap = {};
    subjData.forEach(r => { subjectMap[Number(r[0])] = r[1] || ''; });

    const dairyData = parseArray(dairyRaw);
    const personsData = parseArray(personsRaw);
    const personMap = {};
    personsData.forEach(r => { personMap[Number(r[0])] = r[1] || ''; });

    const entries = [];
    for (const row of dairyData) {
      const date = parseJsDate(row[0]);
      if (!date) continue;
      const lessonNum = Number(row[1]);
      const subjectId = Number(row[2]);
      const theme = row[3] || '';
      const homework = row[4] || '';
      const mark = row[5] || '';
      const teacherId = Number(row[9]) || 0;
      entries.push({
        date, lessonNumber: lessonNum, subjectId,
        subjectName: subjectMap[subjectId] || ('Предмет ' + subjectId),
        theme, homework, mark,
        teacherFio: personMap[teacherId] || '',
      });
    }
    return entries.sort((a, b) => a.date - b.date);
  }

  async function getSchedule(classId) {
    const data = parseArray(await get('/act/GET_TIMETABLE'));
    const names = parseArray(await post('/act/GET_SBJ_NAMES_STORE_DATA', {}));
    const nameMap = {};
    names.forEach(r => { nameMap[Number(r[0])] = r[1] || ''; });
    const persons = parseArray(await post('/act/GET_PERSON_STORE_DATA', { uchId: '1' }));
    const personMap = {};
    persons.forEach(r => { personMap[Number(r[0])] = r[1] || ''; });

    const csp = parseArray(await post('/act/GET_CLASS_SUBJECT', { cls: String(classId) }));
    const studentGroups = new Set();
    csp.forEach(row => {
      const sbj = Number(row[1]); const grp = Number(row[3]) || 1;
      if (!isNaN(sbj)) studentGroups.add(sbj + '.' + grp);
    });

    const result = [];
    data.forEach(row => {
      const sbj = Number(row[2]);
      if (isNaN(sbj)) return;
      const ngroup = Number(row[3]) || 1;
      if (!studentGroups.has(sbj + '.' + ngroup)) return;
      const weekday = Number(row[5]);
      const inday = Number(row[6]);
      if (isNaN(weekday) || isNaN(inday)) return;
      const person = Number(row[4]);
      result.push({
        subjectId: sbj, subjectName: nameMap[sbj] || ('Предмет ' + sbj),
        weekday, lessonOrder: inday, room: row[8] || null,
        teacherFio: !isNaN(person) ? (personMap[person] || null) : null,
      });
    });
    return result.sort((a, b) => (a.weekday * 10 + a.lessonOrder) - (b.weekday * 10 + b.lessonOrder));
  }

  async function getMarks(classId, studentId, uchYear) {
    const now = new Date();
    let yearStart = uchYear;
    const sep = new Date(now.getFullYear(), 8, 1); // 1 сентября
    if (now < sep) yearStart = uchYear - 1;
    const yearEnd = yearStart + 1;

    const [lessonsRaw, marksRaw, dirRaw, subjectsRaw, teachersRaw] = await Promise.all([
      post('/act/GET_STUDENT_LESSONS', {
        cls: String(classId), parallelClasses: '',
        period_begin: '01.09.' + yearStart, period_end: '31.08.' + yearEnd,
        student: String(studentId), uchYear: String(uchYear),
      }),
      post('/act/GET_STUDENT_JOURNAL_DATA', {
        cls: String(classId), parallelClasses: '', student: String(studentId),
      }),
      post('/act/GET_STUDENT_DIRECTOR_DATA', {
        cls: String(classId), parallelClasses: '', student: String(studentId),
      }),
      post('/act/GET_STUDENT_SUBJECTS', {
        student: String(studentId), uchYear: String(uchYear), cls: String(classId),
      }),
      post('/act/GET_CLASS_TEACHERS_INFO', {
        student: String(studentId), uchYear: String(uchYear), cls: String(classId),
      }),
    ]);

    const lessonsData = parseArray(lessonsRaw);
    const marksData = parseArray(marksRaw);
    const dirData = parseArray(dirRaw);
    const subjectsData = parseArray(subjectsRaw);
    const teachersData = parseArray(teachersRaw);

    // Периоды приходят в тех же строках: [5] — id, [6] — начало, [7] — конец.
    // Урок привязываем к самому узкому периоду по дате, чтобы годовая строка
    // не перетягивала на себя оценки (как в приложении).
    const periodRanges = lessonsData.map(l => {
      const id = Number(l[5]);
      const begin = parseJsDate(l[6]);
      const end = parseJsDate(l[7]);
      if (!id || !begin || !end) return null;
      return { id, begin: begin.getTime(), end: end.getTime() };
    }).filter(Boolean).sort((a, b) => (a.end - a.begin) - (b.end - b.begin));

    const numOrNull = (v) => {
      if (v === null || v === undefined || v === '') return null;
      const n = Number(v);
      return isNaN(n) ? null : n;
    };

    const results = [];
    for (const srow of subjectsData) {
      const subjId = Number(srow[0]);
      const subjName = srow[1];
      if (isNaN(subjId) || !subjName) continue;

      const teacherRow = teachersData.find(t => Number(t[0]) === subjId);
      const teacher = teacherRow ? teacherRow[1] : null;

      const marks = lessonsData
        .filter(l => Number(l[14]) === subjId)
        .map(l => {
          const lessonId = Number(l[0]);
          const date = parseJsDate(l[7]);
          const range = date
            ? periodRanges.find(p => date.getTime() >= p.begin && date.getTime() <= p.end)
            : null;
          const markRec = marksData.find(m =>
            Number(m[4]) === subjId && Number(m[0]) === lessonId);
          return {
            lessonId,
            periodId: range ? range.id : null,
            date,
            subjectName: subjName, theme: l[4] || null, homework: l[8] || null,
            mark: markRec ? (markRec[2] || '') : '',
            comment: markRec ? (markRec[8] || '') : '',
            // Журнал берёт вес из [9] (может быть дробным), [3] — запасной вариант.
            weight: numOrNull(l[9]) !== null ? numOrNull(l[9]) : numOrNull(l[3]),
          };
        });

      const totals = {};
      dirData.filter(d => Number(d[4]) === subjId).forEach(d => {
        totals[Number(d[1]) || 0] = d[3] || '';
      });

      results.push({ subjectId: subjId, subjectName: subjName, teacherName: teacher, marks, totalMarks: totals });
    }
    return results;
  }


  // ---- Расчёты оценок (порт GoalMath из Android) ----
  function weightOf(m) {
    const w = Number(m && m.weight);
    return w > 0 ? w : 1;
  }

  function weightedAverage(list) {
    let sum = 0, total = 0;
    (list || []).forEach(m => {
      const n = parseInt(m.mark, 10);
      if (isNaN(n)) return;
      const w = weightOf(m);
      sum += n * w;
      total += w;
    });
    return total ? sum / total : null;
  }

  function currentPeriodLessons(subject) {
    const all = (subject.marks || []).filter(m => !isNaN(parseInt(m.mark, 10)));
    const id = currentPeriodId(subject);
    if (id == null) return all;
    const inPeriod = all.filter(m => m.periodId === id);
    return inPeriod.length ? inPeriod : all;
  }

  function currentPeriodAverage(subject) {
    return weightedAverage(currentPeriodLessons(subject));
  }

  function roundedMark(avg) {
    if (avg == null) return null;
    if (avg >= 4.5) return 5;
    if (avg >= 3.5) return 4;
    if (avg >= 2.5) return 3;
    return 2;
  }

  // Периоды как в приложении: четверти, полугодия, год.
  const periods = [
    { key: 'Q1', label: '1 четверть', ids: [360000] },
    { key: 'Q2', label: '2 четверть', ids: [360001] },
    { key: 'Q3', label: '3 четверть', ids: [360002] },
    { key: 'Q4', label: '4 четверть', ids: [360003] },
    { key: 'H1', label: '1 полугодие', ids: [360000, 360001] },
    { key: 'H2', label: '2 полугодие', ids: [360002, 360003] },
    { key: 'YEAR', label: 'Учебный год', ids: [] }
  ];

  function period(key) {
    return periods.find(p => p.key === key) || periods[periods.length - 1];
  }

  function currentPeriodId(subject) {
    const withPeriod = (subject.marks || []).filter(m => m.periodId != null);
    if (!withPeriod.length) return null;
    return withPeriod.reduce((a, b) => ((b.date || 0) >= (a.date || 0) ? b : a)).periodId;
  }

  // Оценки предмета за выбранный период; если привязки к периодам нет — все.
  function marksForPeriod(subject, key) {
    const p = period(key);
    const all = subject.marks || [];
    const withPeriod = all.filter(m => m.periodId != null);
    let filtered = all;
    if (p.ids.length && withPeriod.length) {
      const inPeriod = all.filter(m => p.ids.indexOf(m.periodId) >= 0);
      filtered = inPeriod.length ? inPeriod : all;
    }
    return filtered.filter(m => !isNaN(parseInt(m.mark, 10)));
  }

  function threshold(target) {
    if (target === 5) return 4.5;
    if (target === 4) return 3.5;
    if (target === 3) return 2.5;
    return 1.5;
  }

  function average(list) {
    const nums = (list || []).filter(n => typeof n === 'number' && !isNaN(n));
    return nums.length ? nums.reduce((a, b) => a + b, 0) / nums.length : null;
  }

  // Сколько оценок нужно до цели. Новая оценка идёт с весом 1, как в приложении.
  function needed(lessons, target, allowed) {
    if (!allowed || !allowed.length) return null;
    const t = threshold(target);
    let sum = 0, total = 0;
    (lessons || []).forEach(m => {
      const n = parseInt(m.mark, 10);
      if (isNaN(n)) return;
      const w = weightOf(m);
      sum += n * w;
      total += w;
    });
    if (total > 0 && sum / total >= t) return 0;
    if (total === 0 && t <= 0) return 0;
    const best = Math.max.apply(null, allowed);
    if (best < t) return null;
    return Math.max(1, Math.ceil((t * total - sum) / (best - t)));
  }

  function hint(lessons, goal) {
    const need = needed(lessons, goal.targetMark, goal.allowedMarks);
    const avg = weightedAverage(lessons);
    const avgText = avg == null ? 'нет оценок' : avg.toFixed(2).replace('.', ',');
    if (need == null) return 'С набором ' + goal.allowedMarks.join('/') + ' цели не достичь';
    if (need === 0) return 'Цель достигнута! Средний: ' + avgText;
    return 'Нужно ещё ' + need + ' × «' + Math.max.apply(null, goal.allowedMarks) + '». Сейчас средний: ' + avgText;
  }

  return {
    login, restoreSession, getUserData, getUchYear,
    getStudentClass, getDairy, getSchedule, getMarks,
    weightedAverage, currentPeriodAverage, currentPeriodLessons, roundedMark,
    periods, period, currentPeriodId, marksForPeriod,
    threshold, average, needed, hint,
    weekRange, parseArray,
  };
})();