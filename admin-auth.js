/* ============================================================================
   ЛинТех 28 — общий вход в админку.

   Пароль сохраняется один раз в localStorage и подставляется во все
   инструменты (расписание, события). Пароль проверяется на сервере
   (Yandex Cloud Function), GitHub-токен в браузер не попадает.
   Кнопка «Выйти» забывает пароль на этом устройстве.

   Важно: пароль хранится в браузере как есть — это удобно, но означает,
   что любой, у кого есть доступ к этому браузеру, попадёт в админку.
   Кнопка «Выйти» очищает сохранённое значение.
   ============================================================================ */
(function () {
  'use strict';

  var KEY = 'lt28_admin_pass';
  var PROXY = 'https://functions.yandexcloud.net/d4eojimh0r45jrrtadm0';

  var AdminAuth = {
    get: function () {
      try { return localStorage.getItem(KEY) || ''; } catch (e) { return ''; }
    },
    has: function () {
      return !!AdminAuth.get();
    },
    set: function (value) {
      var v = String(value || '');
      if (!v) return;
      try { localStorage.setItem(KEY, v); } catch (e) {}
    },
    clear: function () {
      try { localStorage.removeItem(KEY); } catch (e) {}
    },
    /* Подставляет сохранённый пароль в поле и вешает «Выйти».
       Возвращает сохранённый пароль (или ''). */
    init: function (opts) {
      opts = opts || {};

      var input = opts.input ? document.getElementById(opts.input) : null;
      var saved = AdminAuth.get();

      if (input) {
        if (saved) input.value = saved;
        input.addEventListener('input', function () {
          if (input.value) AdminAuth.set(input.value);
        });
      }

      var logout = opts.logout ? document.getElementById(opts.logout) : null;
      if (logout) {
        logout.addEventListener('click', function (e) {
          e.preventDefault();
          AdminAuth.clear();
          location.reload();
        });
      }

      return saved;
    },
    /* Запрос к GitHub через серверную функцию: чтение и запись web/data/*.json */
    request: function (body) {
      var payload = Object.assign({ action: 'github' }, body);
      payload.password = AdminAuth.get();
      return fetch(PROXY, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
      }).then(function (r) {
        return r.json().catch(function () { return { ok: false, error: 'HTTP ' + r.status }; });
      }).then(function (data) {
        if (!data.ok && /парол/i.test(String(data.error || ''))) AdminAuth.clear();
        return data;
      });
    },
    /* Обработка ответа сервера: если пароль неверный — забываем его,
       чтобы следующий вход снова спросил пароль. */
    checkResponse: function (data) {
      var text = data && (data.message || data.error || '');
      if (!text) return data;
      if (/парол/i.test(text) && /(невер|не подходит|ошиб|неверн)/i.test(text)) {
        AdminAuth.clear();
      }
      return data;
    }
  };

  window.AdminAuth = AdminAuth;
})();
