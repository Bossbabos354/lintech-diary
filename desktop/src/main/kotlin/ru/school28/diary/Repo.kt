package ru.school28.diary

import ru.school28.diary.api.ApiClient
import ru.school28.diary.api.DairyEntry
import ru.school28.diary.api.SchoolEvent
import ru.school28.diary.api.SchoolSchedule
import ru.school28.diary.api.SubjectMarks
import java.util.Date

/** Общие загрузки данных: класс, оценки, дневник, расписание, события. */
object Repo {

    /** Класс ученика; при первом обращении запрашивается у журнала и кэшируется. */
    suspend fun ensureClass(): Int? {
        val studentId = Settings.studentId
        if (studentId == -1) return null
        val cached = Settings.classId
        if (cached != -1) return cached
        val classId = ApiClient.getStudentClass(studentId, Settings.uchYear)
        Settings.classId = classId
        return classId
    }

    suspend fun marks(): List<SubjectMarks> {
        val classId = ensureClass() ?: return emptyList()
        return ApiClient.getMarks(classId, Settings.studentId, Settings.uchYear)
    }

    suspend fun dairy(begin: Date, end: Date): List<DairyEntry> {
        val classId = ensureClass() ?: return emptyList()
        return ApiClient.getDairy(classId, Settings.studentId, begin, end)
    }

    suspend fun schedule(): SchoolSchedule? = ApiClient.getSchedule()

    suspend fun events(): List<SchoolEvent> = ApiClient.getEvents()
}
