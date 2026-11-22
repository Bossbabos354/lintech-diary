package ru.school28.diary.api

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.*
import ru.school28.diary.FileLog
import java.io.File
import java.io.IOException
import java.math.BigInteger
import java.nio.charset.Charset
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

data class DairyEntry(
    val date: Date,
    val lessonNumber: Int,
    val subjectId: Int,
    val subjectName: String,
    val theme: String,
    val homework: String,
    val mark: String,
    val teacherFio: String
)

data class LessonMarks(
    val lessonId: Int,
    val periodId: Int?,
    val date: Date?,
    val subjectName: String,
    val theme: String?,
    val homework: String?,
    val mark: String,
    val weight: Int?
)

data class SubjectMarks(
    val subjectId: Int,
    val subjectName: String,
    val teacherName: String?,
    val marks: List<LessonMarks>,
    val totalMarks: Map<Int, String>
)

data class PersonInfo(
    val fio: String,
    val uchName: String,
    val uchId: Int,
    val studentId: Int,
    val userId: Int = -1
)

/** Событие с сайта школы (events.json на Beget). */
data class SchoolEvent(
    val id: String,
    val date: String,
    val time: String? = null,
    val title: String,
    val text: String,
    val content: String? = null,
    val image: String? = null,
    val slug: String? = null,
    val link: String? = null
)

object ApiClient {
    private const val BASE_URL = "https://journal.school28-kirov.ru"
    private val gson = Gson()

    private var appContext: Context? = null

    /** Контекст нужен для дискового кэша при плохой сети. */
    fun init(context: Context) {
        appContext = context.applicationContext
    }

    private val offlineStamps = ConcurrentHashMap<String, Long>()

    /** Время офлайн-кэша для раздела ("marks", "dairy", "schedule"), если сеть не ответила. */
    fun offlineAt(kind: String): Long? = offlineStamps[kind]

    private fun diskFile(key: String): File? =
        appContext?.let { File(it.filesDir, "offline_$key.json") }

    private fun diskSave(key: String, json: String) {
        runCatching { diskFile(key)?.writeText(json) }
    }

    private fun diskLoad(key: String): String? = runCatching {
        diskFile(key)?.takeIf { it.exists() }?.readText()
    }.getOrNull()

    private fun cacheTime(key: String): Long = runCatching {
        diskFile(key)?.takeIf { it.exists() }?.lastModified() ?: 0L
    }.getOrDefault(0L)

    private inline fun <reified T> readCache(key: String): T? {
        val raw = diskLoad(key) ?: return null
        return runCatching {
            val type = object : TypeToken<T>() {}.type
            gson.fromJson<T>(raw, type)
        }.getOrNull()
    }

    // Сессия АВЕРС — client-side cookies Ext JS (ys- префикс)
    private var sessionUser: String? = null
    private var sessionPassword: String? = null
    private var sessionUserId: String? = null

    // Справочники меняются редко, но тянулись на каждый запрос (а NotifierWorker
    // опрашивает дневник каждые 15–1440 минут) — кэшируем в памяти.
    @Volatile
    private var cachedPersons: Map<Int?, String>? = null
    private val cachedSubjects = ConcurrentHashMap<Int, Map<Int?, String>>()

    // Короткоживущий кэш результатов экранов: заставка грузит данные заранее,
    // а экраны показывают их без повторного запроса к медленному серверу.
    private const val SCREEN_CACHE_TTL_MS = 60_000L

    private data class MarksCacheKey(val classId: Int, val studentId: Int, val uchYear: Int)
    private data class DairyCacheKey(
        val classId: Int, val studentId: Int, val begin: Long, val end: Long
    )

    @Volatile
    private var marksCache: Pair<MarksCacheKey, List<SubjectMarks>>? = null
    @Volatile
    private var marksCacheAt: Long = 0L

    @Volatile
    private var dairyCache: Pair<DairyCacheKey, List<DairyEntry>>? = null
    @Volatile
    private var dairyCacheAt: Long = 0L

    private fun clearCaches() {
        cachedPersons = null
        cachedSubjects.clear()
        marksCache = null
        dairyCache = null
        marksCacheAt = 0L
        dairyCacheAt = 0L
    }

    private val jsDateRegex = Regex("""new\s+Date\((\d{4}),(\d{1,2}),(\d{1,2})[^)]*\)""")

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val orig = chain.request()
            val builder = orig.newBuilder()
                .header("User-Agent", "Mozilla/5.0 (Android 14; Mobile) SchoolDiary/1.0")
                .header("X-Requested-With", "XMLHttpRequest")
                .header("Referer", "$BASE_URL/")
            // Добавляем Ext JS cookies для авторизации
            val cookieParts = mutableListOf<String>()
            sessionUser?.let { cookieParts.add("ys-user=$it") }
            sessionPassword?.let { cookieParts.add("ys-password=$it") }
            sessionUserId?.let { cookieParts.add("ys-userId=$it") }
            // Только для act/* запросов
            if (orig.url.encodedPath.contains("/act/") && cookieParts.isNotEmpty()) {
                builder.header("Cookie", cookieParts.joinToString("; "))
            }
            chain.proceed(builder.build())
        }
        .build()

    /**
     * JavaScript escape() — Ext JS CookieProvider encodeValue использует именно его.
     * Для ASCII: alphanumeric и *-_.@ остаются, прочее -> %XX.
     * Для не-ASCII: %uXXXX.
     */
    private fun jsEscape(s: String): String {
        val sb = StringBuilder()
        for (ch in s) {
            val o = ch.code
            when {
                o < 256 -> {
                    if (ch.isLetterOrDigit() || ch in "*-_./@") sb.append(ch)
                    else sb.append("%${o.toString(16).uppercase().padStart(2, '0')}")
                }
                else -> sb.append("%u${o.toString(16).uppercase().padStart(4, '0')}")
            }
        }
        return sb.toString()
    }

    private fun encodeExtStr(v: String) = jsEscape("s:$v")
    private fun encodeExtNum(v: Int) = jsEscape("n:$v")

    private suspend fun post(endpoint: String, params: Map<String, String>): String =
        withContext(Dispatchers.IO) {
            val url = "$BASE_URL$endpoint"
            val formBody = FormBody.Builder(Charsets.UTF_8)
            for ((k, v) in params) formBody.add(k, v)
            val request = Request.Builder().url(url).post(formBody.build()).build()
            try {
                val response = client.newCall(request).execute()
                val body = response.body ?: throw IOException("Empty response")
                val text = decode(body.bytes())
                if (!response.isSuccessful) FileLog.log("HTTP POST $endpoint -> ${response.code}")
                text
            } catch (e: Exception) {
                FileLog.logException("HTTP POST $endpoint", e)
                throw e
            }
        }

    private fun decode(bytes: ByteArray): String {
        val utf8 = try {
            Charsets.UTF_8.newDecoder()
                .onMalformedInput(java.nio.charset.CodingErrorAction.REPORT)
                .onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPORT)
                .decode(java.nio.ByteBuffer.wrap(bytes))
                .toString()
        } catch (e: Exception) { null }
        if (utf8 != null && !utf8.contains('\uFFFD')) return utf8
        return try {
            String(bytes, Charset.forName("windows-1251"))
        } catch (e: Exception) {
            String(bytes, Charsets.ISO_8859_1)
        }
    }

    private fun sha1(input: String): String {
        val md = MessageDigest.getInstance("SHA-1")
        return BigInteger(1, md.digest(input.toByteArray(Charsets.UTF_8)))
            .toString(16).padStart(40, '0')
    }

    private fun parseArray(json: String): List<List<Any?>> {
        // Сервер АВЕРС возвращает new Date(год,мес,день,...) — невалидный JSON.
        // Нормализуем в строку "дд.мм.гггг".
        // NB: Calendar создаём локально ВНУТРИ лямбды — здесь раньше был общий экземпляр,
        // а parseArray теперь вызывается из параллельных корутин (async в getDairy/getMarks),
        // Calendar не потокобезопасен → гонка данных портила даты уроков.
        val sdf = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        val clean = jsDateRegex.replace(json) { m ->
            // JS Date: месяц 0-индексный
            val cal = Calendar.getInstance()
            cal.set(
                m.groupValues[1].toInt(),
                m.groupValues[2].toInt(),
                m.groupValues[3].toInt().coerceAtLeast(1)
            )
            "\"${sdf.format(cal.time)}\""
        }
        return try {
            gson.fromJson(clean, object : TypeToken<List<List<Any?>>>() {}.type) ?: emptyList()
        } catch (e: Exception) { emptyList() }
    }

    // Ключ "предмет + урок" для быстрого поиска оценки
    private fun markKey(subjectId: Int, lessonId: Int): Long =
        (subjectId.toLong() shl 32) xor (lessonId.toLong() and 0xFFFFFFFFL)

    // ---- ПУБЛИЧНЫЕ МЕТОДЫ ----

    suspend fun login(login: String, password: String): PersonInfo? {
        FileLog.log("login: start, login=$login")
        val hashed = sha1(password)
        val resp = try {
            post("/login", mapOf("l" to login, "p" to hashed))
        } catch (e: Exception) {
            FileLog.logException("login/post", e)
            throw e
        }
        if (resp.trim() in listOf("[]", "[error_symbol]")) {
            FileLog.log("login: пустой ответ (bad creds)")
            return null
        }

        val data = try { parseArray(resp) } catch (e: Exception) { FileLog.logException("login/parse", e); emptyList() }
        if (data.isEmpty()) return null

        val userId = (data[0][0] as? Double)?.toInt() ?: return null
        val fio = data[0][5] as? String ?: ""
        val studentId = (data[0][data[0].size - 1] as? Double)?.toInt() ?: userId

        // Устанавливаем сессионные куки в Ext JS формате
        clearCaches()
        setSession(login, hashed, userId)
        FileLog.log("login: cookies set, userId=$userId studentId=$studentId")

        // Подтверждение авторизации
        try { post("/auth", mapOf("uId" to userId.toString(), "act" to "1")) } catch (_: Exception) {}

        FileLog.log("login: SUCCESS")
        return PersonInfo(fio = fio, uchName = "", uchId = 1, studentId = studentId, userId = userId)
    }

    /**
     * Восстанавливает сессию из сохранённых учётных данных (без вызова /login).
     * Используется при холодном старте (автологин).
     */
    fun restoreSession(login: String, password: String) {
        val hashed = sha1(password)
        FileLog.log("restoreSession: login=$login")
        setSession(login, hashed, -1)
    }

    private fun setSession(login: String, hashed: String, userId: Int) {
        sessionUser = encodeExtStr(login)
        sessionPassword = encodeExtStr(hashed)
        sessionUserId = encodeExtNum(userId)
        FileLog.log("setSession: ys-user=${sessionUser?.take(30)}... ys-userId=$sessionUserId")
    }

    suspend fun getUchYear(studentId: Int): Int {
        val sdf = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        val raw = post("/act/get_uch_year", mapOf("currentDate" to sdf.format(Date())))
        val data = parseArray(raw)
        if (data.isNotEmpty()) {
            return (data[0][0] as? String)?.toIntOrNull()
                ?: Calendar.getInstance().get(Calendar.YEAR)
        }
        return Calendar.getInstance().get(Calendar.YEAR)
    }

    suspend fun getStudentClass(studentId: Int, uchYear: Int): Int {
        val sdf = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        val raw = post("/act/GET_STUDENT_CLASS", mapOf(
            "currentDate" to sdf.format(Date()),
            "student" to studentId.toString(),
            "uchYear" to uchYear.toString(),
            "uchId" to "1"
        ))
        val data = parseArray(raw)
        if (data.isEmpty()) throw IOException("No student class")
        return (data[0][0] as? Double)?.toInt() ?: throw IOException("Bad class data")
    }

    /** Справочник учителей (общий для школы) — кэшируется за сессию. */
    private suspend fun personMap(): Map<Int?, String> {
        cachedPersons?.let { return it }
        val raw = post("/act/GET_PERSON_STORE_DATA", mapOf("uchId" to "1"))
        val map = parseArray(raw).associate {
            (it[0] as? Double)?.toInt() to (it[1] as? String ?: "")
        }
        cachedPersons = map
        return map
    }

    /** Справочник предметов класса — кэшируется по классу. */
    private suspend fun subjectMap(classId: Int): Map<Int?, String> {
        cachedSubjects[classId]?.let { return it }
        val raw = post("/act/GET_DAIRY_CLASS_SUBJECTS",
            mapOf("pClassesIds" to "", "cls" to classId.toString()))
        val map = parseArray(raw).associate {
            (it[0] as? Double)?.toInt() to (it[1] as? String ?: "")
        }
        cachedSubjects[classId] = map
        return map
    }

    /** Дневник: при сбое сети отдаём последний сохранённый кэш. */
    suspend fun getDairy(
        classId: Int, studentId: Int, beginDate: Date, endDate: Date
    ): List<DairyEntry> {
        val key = "dairy_${classId}_${studentId}_${beginDate.time}_${endDate.time}"
        return try {
            val online = getDairyOnline(classId, studentId, beginDate, endDate)
            offlineStamps.remove("dairy")
            diskSave(key, gson.toJson(online))
            online
        } catch (e: Exception) {
            val cached = readCache<List<DairyEntry>>(key) ?: throw e
            offlineStamps["dairy"] = cacheTime(key)
            FileLog.log("getDairy: отдаю офлайн-кэш")
            cached
        }
    }

    /** Оценки: при сбое сети отдаём последний сохранённый кэш. */
    suspend fun getMarks(
        classId: Int, studentId: Int, uchYear: Int
    ): List<SubjectMarks> {
        val key = "marks_${classId}_${studentId}_$uchYear"
        return try {
            val online = getMarksOnline(classId, studentId, uchYear)
            offlineStamps.remove("marks")
            diskSave(key, gson.toJson(online))
            online
        } catch (e: Exception) {
            val cached = readCache<List<SubjectMarks>>(key) ?: throw e
            offlineStamps["marks"] = cacheTime(key)
            FileLog.log("getMarks: отдаю офлайн-кэш")
            cached
        }
    }

    /** Расписание: если сайт недоступен — последний кэш. */
    suspend fun getSchedule(): SchoolSchedule? {
        val online = getScheduleOnline()
        if (online != null) {
            offlineStamps.remove("schedule")
            diskSave("schedule", gson.toJson(online))
            return online
        }
        val cached = readCache<SchoolSchedule>("schedule") ?: return null
        offlineStamps["schedule"] = cacheTime("schedule")
        FileLog.log("getSchedule: отдаю офлайн-кэш")
        return cached
    }

    private suspend fun getDairyOnline(
        classId: Int, studentId: Int, beginDate: Date, endDate: Date
    ): List<DairyEntry> {
        val cacheKey = DairyCacheKey(classId, studentId, beginDate.time, endDate.time)
        val now = System.currentTimeMillis()
        dairyCache?.let { (k, v) ->
            if (k == cacheKey && now - dairyCacheAt < SCREEN_CACHE_TTL_MS) return v
        }
        val result = coroutineScope {
        val sdf = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        val beginStr = sdf.format(beginDate)
        val endStr = sdf.format(endDate)
        val cls = classId.toString()

        // Справочники (из кэша) и сам дневник запрашиваем параллельно
        val subjectsDeferred = async { subjectMap(classId) }
        val personsDeferred = async { personMap() }
        val dairyDeferred = async {
            post("/act/GET_STUDENT_DAIRY", mapOf(
                "pClassesIds" to "", "student" to studentId.toString(),
                "cls" to cls,
                "begin_dt" to beginStr,
                "end_dt" to endStr
            ))
        }

        val subjects = subjectsDeferred.await()
        val persons = personsDeferred.await()
        val dairyData = parseArray(dairyDeferred.await())

        val entries = mutableListOf<DairyEntry>()
        val jsDateSdf = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

        for (row in dairyData) {
            // Формат: [new Date(2026,8,14,...), lessonKey, subjectId, theme, homework, mark, group, ...]
            val rawDate = row[0]
            val date = if (rawDate is String) {
                jsDateSdf.parse(rawDate) ?: continue
            } else continue

            val lessonNum = (row[1] as? Double)?.toInt() ?: continue
            val subjectId = (row[2] as? Double)?.toInt() ?: continue
            val theme = row[3] as? String ?: ""
            val homework = row[4] as? String ?: ""
            val mark = row[5] as? String ?: ""
            val teacherId = (row[9] as? Double)?.toInt() ?: 0

            entries.add(DairyEntry(
                date = date, lessonNumber = lessonNum,
                subjectId = subjectId,
                subjectName = subjects[subjectId] ?: "Предмет $subjectId",
                theme = theme, homework = homework, mark = mark,
                teacherFio = persons[teacherId] ?: ""
            ))
        }
        entries.sortedBy { it.date }
        }
        dairyCache = cacheKey to result
        dairyCacheAt = System.currentTimeMillis()
        return result
    }

    private suspend fun getMarksOnline(
        classId: Int, studentId: Int, uchYear: Int
    ): List<SubjectMarks> {
        val cacheKey = MarksCacheKey(classId, studentId, uchYear)
        val now = System.currentTimeMillis()
        marksCache?.let { (k, v) ->
            if (k == cacheKey && now - marksCacheAt < SCREEN_CACHE_TTL_MS) return v
        }
        val result = coroutineScope {
        val sdf = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

        // Уроки студента за текущий учебный год:
        // если сейчас вторая половина календарного года (сентябрь-декабрь),
        // учебный год начался в этом же году; если первая половина (январь-май) —
        // учебный год начался в прошлом году.
        val now = Calendar.getInstance()
        val sep = Calendar.getInstance().apply {
            set(Calendar.MONTH, Calendar.SEPTEMBER); set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0)
        }
        val yearStart = if (now.before(sep)) uchYear - 1 else uchYear
        val yearEnd = yearStart + 1

        val cls = classId.toString()
        val student = studentId.toString()
        val uch = uchYear.toString()
        FileLog.log("getMarks: GET_STUDENT_LESSONS period 01.09.$yearStart-31.08.$yearEnd")

        // Все пять запросов независимы — идём параллельно (сервер медленный)
        val lessonsDeferred = async {
            post("/act/GET_STUDENT_LESSONS", mapOf(
                "cls" to cls,
                "parallelClasses" to "",
                "period_begin" to "01.09.$yearStart",
                "period_end" to "31.08.$yearEnd",
                "student" to student,
                "uchYear" to uch
            ))
        }
        // Оценки
        val marksDeferred = async {
            post("/act/GET_STUDENT_JOURNAL_DATA", mapOf(
                "cls" to cls,
                "parallelClasses" to "",
                "student" to student
            ))
        }
        // Итоговые оценки (директорские)
        val dirMarksDeferred = async {
            post("/act/GET_STUDENT_DIRECTOR_DATA", mapOf(
                "cls" to cls,
                "parallelClasses" to "",
                "student" to student
            ))
        }
        // Предметы — нужны параметры student, uchYear, cls
        val subjectsDeferred = async {
            post("/act/GET_STUDENT_SUBJECTS", mapOf(
                "student" to student,
                "uchYear" to uch,
                "cls" to cls
            ))
        }
        // Учителя
        val teachersDeferred = async {
            post("/act/GET_CLASS_TEACHERS_INFO", mapOf(
                "student" to student,
                "uchYear" to uch,
                "cls" to cls
            ))
        }

        val lessonsData = parseArray(lessonsDeferred.await())
        val marksData = parseArray(marksDeferred.await())
        val dirMarksData = parseArray(dirMarksDeferred.await())
        val subjectsData = parseArray(subjectsDeferred.await())
        val teachersData = parseArray(teachersDeferred.await())

        // Индексы вместо линейного поиска внутри циклов (было O(предметы × уроки × оценки))
        val marksByKey = HashMap<Long, String>(marksData.size * 2)
        for (m in marksData) {
            val subjId = (m[4] as? Double)?.toInt() ?: continue
            val lessonId = (m[0] as? Double)?.toInt() ?: continue
            marksByKey[markKey(subjId, lessonId)] = m[2] as? String ?: ""
        }

        val lessonsBySubject = lessonsData.groupBy { (it[14] as? Double)?.toInt() }
        val totalsBySubject = dirMarksData.groupBy { (it[4] as? Double)?.toInt() }

        val teachersBySubject = HashMap<Int, String>()
        for (t in teachersData) {
            val subjId = (t[0] as? Double)?.toInt() ?: continue
            // как и раньше — берём первого найденного учителя
            if (subjId !in teachersBySubject) teachersBySubject[subjId] = t[1] as? String ?: ""
        }

        subjectsData.mapNotNull { subjRow ->
            val subjId = (subjRow[0] as? Double)?.toInt() ?: return@mapNotNull null
            val subjName = subjRow[1] as? String ?: return@mapNotNull null

            val marks = lessonsBySubject[subjId].orEmpty().mapNotNull { l ->
                val lessonId = (l[0] as? Double)?.toInt() ?: return@mapNotNull null
                val periodId = (l[5] as? Double)?.toInt()
                val dateStr = l[7] as? String
                LessonMarks(
                    lessonId = lessonId, periodId = periodId,
                    date = dateStr?.let { sdf.parse(it) },
                    subjectName = subjName,
                    theme = l[4] as? String, homework = l[8] as? String,
                    mark = marksByKey[markKey(subjId, lessonId)] ?: "",
                    weight = (l[3] as? Double)?.toInt()
                )
            }

            val totals = totalsBySubject[subjId].orEmpty().associate { d ->
                ((d[1] as? Double)?.toInt() ?: 0) to (d[3] as? String ?: "")
            }

            SubjectMarks(subjId, subjName, teachersBySubject[subjId], marks, totals)
        }
        }
        marksCache = cacheKey to result
        marksCacheAt = System.currentTimeMillis()
        return result
    }

    // ---- СОБЫТИЯ (синхронизация с сайтом Beget) ----

    /** URL файла событий на сайте (без учёта авторизации дневника). */
    private const val EVENTS_URL = "http://d91950hv.beget.tech/events.json"

    /** Скачивает список событий с сайта школы. Пустой список при ошибке/отсутствии. */
    suspend fun getEvents(): List<SchoolEvent> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(EVENTS_URL).get()
                .header("User-Agent", "Mozilla/5.0 (Android) SchoolDiary/1.0")
                .build()
            val response = client.newCall(request).execute()
            val body = response.body ?: return@withContext emptyList()
            val text = decode(body.bytes())
            if (!response.isSuccessful) {
                FileLog.log("getEvents: HTTP ${response.code}")
                return@withContext emptyList()
            }
            val type = object : TypeToken<Map<String, List<SchoolEvent>>>() {}.type
            val parsed: Map<String, List<SchoolEvent>>? = gson.fromJson(text, type)
            parsed?.get("events") ?: emptyList()
        } catch (e: Exception) {
            FileLog.logException("getEvents", e)
            emptyList()
        }
    }

    // ---- РАСПИСАНИЕ (сайт школы) ----

    private const val SCHEDULE_URL = "http://d91950hv.beget.tech/schedule.json"
    private const val SCHEDULE_TTL_MS = 5 * 60_000L

    @Volatile
    private var scheduleCache: Pair<Long, SchoolSchedule>? = null

    /** Скачивает расписание с сайта. Кэш 5 минут, чтобы не дёргать сервер. */
    private suspend fun getScheduleOnline(): SchoolSchedule? {
        val now = System.currentTimeMillis()
        scheduleCache?.let { (at, data) ->
            if (now - at < SCHEDULE_TTL_MS) return data
        }
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder().url(SCHEDULE_URL).get()
                    .header("User-Agent", "Mozilla/5.0 (Android) SchoolDiary/1.0")
                    .build()
                val response = client.newCall(request).execute()
                val body = response.body ?: return@withContext null
                val text = decode(body.bytes())
                if (!response.isSuccessful) {
                    FileLog.log("getSchedule: HTTP ${response.code}")
                    return@withContext null
                }
                val parsed = gson.fromJson(text, SchoolSchedule::class.java) ?: return@withContext null
                scheduleCache = System.currentTimeMillis() to parsed
                parsed
            } catch (e: Exception) {
                FileLog.logException("getSchedule", e)
                scheduleCache?.second
            }
        }
    }
}