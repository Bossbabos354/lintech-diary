package ru.school28.diary.api

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import ru.school28.diary.FileLog
import java.io.IOException
import java.math.BigInteger
import java.nio.charset.Charset
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*
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

data class ScheduleEntry(
    val subjectId: Int,
    val subjectName: String,
    val weekday: Int,
    val lessonOrder: Int,
    val room: String?,
    val teacherFio: String?
)

data class PersonInfo(
    val fio: String,
    val uchName: String,
    val uchId: Int,
    val studentId: Int,
    val userId: Int = -1
)

object ApiClient {
    private const val BASE_URL = "https://journal.school28-kirov.ru"
    private val gson = Gson()

    private var sessionUser: String? = null
    private var sessionPassword: String? = null
    private var sessionUserId: String? = null

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
            val cookieParts = mutableListOf<String>()
            sessionUser?.let { cookieParts.add("ys-user=$it") }
            sessionPassword?.let { cookieParts.add("ys-password=$it") }
            sessionUserId?.let { cookieParts.add("ys-userId=$it") }
            if (orig.url.encodedPath.contains("/act/") && cookieParts.isNotEmpty()) {
                builder.header("Cookie", cookieParts.joinToString("; "))
            }
            chain.proceed(builder.build())
        }
        .build()

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

    private suspend fun get(endpoint: String): String =
        withContext(Dispatchers.IO) {
            val url = "$BASE_URL$endpoint"
            val request = Request.Builder().url(url).get().build()
            try {
                val response = client.newCall(request).execute()
                val body = response.body ?: throw IOException("Empty response")
                val text = decode(body.bytes())
                if (!response.isSuccessful) FileLog.log("HTTP GET $endpoint -> ${response.code}")
                text
            } catch (e: Exception) {
                FileLog.logException("HTTP GET $endpoint", e)
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
        val clean = json.replace(Regex("""new\s+Date\((\d{4}),(\d{1,2}),(\d{1,2})[^)]*\)""")) { m ->
            val y = m.groupValues[1]; val mo = m.groupValues[2]; val d = m.groupValues[3]
            val cal = Calendar.getInstance()
            cal.set(y.toInt(), mo.toInt(), d.toInt().coerceAtLeast(1))
            val sdf = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
            "\"${sdf.format(cal.time)}\""
        }
        return try {
            gson.fromJson(clean, object : TypeToken<List<List<Any?>>>() {}.type) ?: emptyList()
        } catch (e: Exception) { emptyList() }
    }

    private fun parseJsDate(value: Any?): Date? {
        if (value is String) {
            val sdf = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
            return try { sdf.parse(value) } catch (e: Exception) { null }
        }
        return null
    }

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

        setSession(login, hashed, userId)
        FileLog.log("login: cookies set, userId=$userId studentId=$studentId")

        try { post("/auth", mapOf("uId" to userId.toString(), "act" to "1")) } catch (_: Exception) {}

        FileLog.log("login: SUCCESS")
        return PersonInfo(fio = fio, uchName = "", uchId = 1, studentId = studentId, userId = userId)
    }

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

    suspend fun getUserData(userId: Int): PersonInfo {
        val raw = post("/act/get_user_data", emptyMap())
        val data = parseArray(raw)
        if (data.isEmpty()) throw IOException("No user data")
        val row = data[0]
        val fio = "${row[0]} ${(row[1] as? String)?.take(1) ?: ""}." +
                " ${(row[2] as? String)?.take(1) ?: ""}."
        val uchName = row[3] as? String ?: ""
        val uchId = (row[4] as? Double)?.toInt() ?: 0
        return PersonInfo(fio, uchName, uchId, 0)
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

    suspend fun getDairy(
        classId: Int, studentId: Int, beginDate: Date, endDate: Date
    ): List<DairyEntry> {
        val sdf = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

        val rawSubjects = post("/act/GET_DAIRY_CLASS_SUBJECTS",
            mapOf("pClassesIds" to "", "cls" to classId.toString()))
        val subjects = parseArray(rawSubjects)
        val subjectMap = subjects.associate {
            (it[0] as? Double)?.toInt() to (it[1] as? String ?: "")
        }

        val rawDairy = post("/act/GET_STUDENT_DAIRY", mapOf(
            "pClassesIds" to "", "student" to studentId.toString(),
            "cls" to classId.toString(),
            "begin_dt" to sdf.format(beginDate),
            "end_dt" to sdf.format(endDate)
        ))
        val dairyData = parseArray(rawDairy)

        val rawPersons = post("/act/GET_PERSON_STORE_DATA", mapOf("uchId" to "1"))
        val persons = parseArray(rawPersons)
        val personMap = persons.associate {
            (it[0] as? Double)?.toInt() to (it[1] as? String ?: "")
        }

        val entries = mutableListOf<DairyEntry>()
        val jsDateSdf = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

        for (row in dairyData) {
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
                subjectName = subjectMap[subjectId] ?: "Предмет $subjectId",
                theme = theme, homework = homework, mark = mark,
                teacherFio = personMap[teacherId] ?: ""
            ))
        }
        return entries.sortedBy { it.date }
    }

    suspend fun getSchedule(classId: Int, studentId: Int): List<ScheduleEntry> {
        val raw = get("/act/GET_TIMETABLE")
        val data = parseArray(raw)

        val rawNames = post("/act/GET_SBJ_NAMES_STORE_DATA", emptyMap())
        val names = parseArray(rawNames)
        val nameMap = names.associate { (it[0] as? Double)?.toInt() to (it[1] as? String ?: "") }

        val rawPersons = post("/act/GET_PERSON_STORE_DATA", mapOf("uchId" to "1"))
        val persons = parseArray(rawPersons)
        val personMap = persons.associate { (it[0] as? Double)?.toInt() to (it[1] as? String ?: "") }

        val rawCsp = post("/act/GET_CLASS_SUBJECT", mapOf("cls" to classId.toString()))
        val cspData = parseArray(rawCsp)

        val studentGroups = mutableSetOf<String>()
        for (row in cspData) {
            val sbj = (row[1] as? Double)?.toInt() ?: continue
            val grp = (row[3] as? Double)?.toInt() ?: 1
            studentGroups.add("$sbj.$grp")
        }

        return data.mapNotNull { row ->
            val sbj = (row[2] as? Double)?.toInt() ?: return@mapNotNull null
            val ngroup = (row[3] as? Double)?.toInt() ?: 1
            if ("$sbj.$ngroup" !in studentGroups) return@mapNotNull null
            val weekday = (row[5] as? Double)?.toInt() ?: return@mapNotNull null
            val inday = (row[6] as? Double)?.toInt() ?: return@mapNotNull null
            val person = (row[4] as? Double)?.toInt()
            ScheduleEntry(
                subjectId = sbj, subjectName = nameMap[sbj] ?: "Предмет $sbj",
                weekday = weekday, lessonOrder = inday,
                room = row[8] as? String,
                teacherFio = person?.let { personMap[it] }
            )
        }.sortedBy { it.weekday * 10 + it.lessonOrder }
    }

    suspend fun getMarks(
        classId: Int, studentId: Int, uchYear: Int
    ): List<SubjectMarks> {
        val sdf = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

        val now = Calendar.getInstance()
        val sep = Calendar.getInstance().apply {
            set(Calendar.MONTH, Calendar.SEPTEMBER); set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0)
        }
        val yearStart = if (now.before(sep)) uchYear - 1 else uchYear
        val yearEnd = yearStart + 1
        val rawLessons = post("/act/GET_STUDENT_LESSONS", mapOf(
            "cls" to classId.toString(),
            "parallelClasses" to "",
            "period_begin" to "01.09.$yearStart",
            "period_end" to "31.08.$yearEnd",
            "student" to studentId.toString(),
            "uchYear" to uchYear.toString()
        ))
        FileLog.log("getMarks: GET_STUDENT_LESSONS period 01.09.$yearStart-31.08.$yearEnd")
        val lessonsData = parseArray(rawLessons)

        val rawMarks = post("/act/GET_STUDENT_JOURNAL_DATA", mapOf(
            "cls" to classId.toString(),
            "parallelClasses" to "",
            "student" to studentId.toString()
        ))
        val marksData = parseArray(rawMarks)

        val rawDirMarks = post("/act/GET_STUDENT_DIRECTOR_DATA", mapOf(
            "cls" to classId.toString(),
            "parallelClasses" to "",
            "student" to studentId.toString()
        ))
        val dirMarksData = parseArray(rawDirMarks)

        val rawSubjects = post("/act/GET_STUDENT_SUBJECTS", mapOf(
            "student" to studentId.toString(),
            "uchYear" to uchYear.toString(),
            "cls" to classId.toString()
        ))
        val subjectsData = parseArray(rawSubjects)

        val rawTeachers = post("/act/GET_CLASS_TEACHERS_INFO", mapOf(
            "student" to studentId.toString(),
            "uchYear" to uchYear.toString(),
            "cls" to classId.toString()
        ))
        val teachersData = parseArray(rawTeachers)

        val results = mutableListOf<SubjectMarks>()

        for (subjRow in subjectsData) {
            val subjId = (subjRow[0] as? Double)?.toInt() ?: continue
            val subjName = subjRow[1] as? String ?: continue

            val teacher = teachersData.firstOrNull { t ->
                (t[0] as? Double)?.toInt() == subjId
            }?.let { it[1] as? String }

            val marks = lessonsData.filter { l ->
                (l[14] as? Double)?.toInt() == subjId
            }.mapNotNull { l ->
                val lessonId = (l[0] as? Double)?.toInt() ?: return@mapNotNull null
                val periodId = (l[5] as? Double)?.toInt()
                val dateStr = l[7] as? String
                LessonMarks(
                    lessonId = lessonId, periodId = periodId,
                    date = dateStr?.let { sdf.parse(it) },
                    subjectName = subjName,
                    theme = l[4] as? String, homework = l[8] as? String,
                    mark = marksData.firstOrNull { m ->
                        (m[4] as? Double)?.toInt() == subjId &&
                        (m[0] as? Double)?.toInt() == lessonId
                    }?.let { it[2] as? String } ?: "",
                    weight = (l[3] as? Double)?.toInt()
                )
            }

            val totals = dirMarksData.filter { d ->
                (d[4] as? Double)?.toInt() == subjId
            }.associate { d ->
                ((d[1] as? Double)?.toInt() ?: 0) to (d[3] as? String ?: "")
            }

            results.add(SubjectMarks(subjId, subjName, teacher, marks, totals))
        }
        return results
    }
}