/* ============================================================================
   ЛинТех 28 — общий вход в админку.

   Пароль сохраняется один раз в localStorage и подставляется во все
   инструменты (загрузка APK, статьи, расписание). Кнопка «Выйти» забывает
   пароль на этом устройстве.

   Важно: пароль хранится в браузере как есть — это удобно, но означает,
   что любой, у кого есть доступ к этому браузеру, попадёт в админку.
   Кнопка «Выйти» очищает сохранённое значение.
   ============================================================================ */
(function () {
  'use strict';

  var KEY = 'lt28_admin_pass';

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
