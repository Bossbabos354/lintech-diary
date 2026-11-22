package ru.school28.diary.export

import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDType0Font
import ru.school28.diary.AppLog
import ru.school28.diary.AppPaths
import ru.school28.diary.api.DairyEntry
import ru.school28.diary.api.SchoolSchedule
import ru.school28.diary.api.SubjectMarks
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Экспорт данных: PDF дневника и оценок, XLSX с оценками, ICS с расписанием,
 * плюс ZIP со всем сразу. Файлы кладутся в каталог экспорта приложения.
 */
object Exporter {

    data class Result(val dir: File, val files: List<File>)

    fun exportAll(
        marks: List<SubjectMarks>,
        diary: List<DairyEntry>,
        schedule: SchoolSchedule?,
        className: String?,
    ): Result {
        val dir = AppPaths.exportDir.also { it.mkdirs() }
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val pdf = File(dir, "lintech_diary_$stamp.pdf")
        val xlsx = File(dir, "lintech_marks_$stamp.xlsx")
        val ics = File(dir, "lintech_schedule_$stamp.ics")

        writePdf(diary, marks, pdf)
        writeXlsx(marks, xlsx)
        writeIcs(schedule, className, ics)

        val zip = File(dir, "lintech_export_$stamp.zip")
        zipFiles(listOf(pdf, xlsx, ics), zip)
        return Result(dir, listOf(pdf, xlsx, ics, zip))
    }

    // ---- PDF ----

    private fun writePdf(diary: List<DairyEntry>, marks: List<SubjectMarks>, file: File) {
        PDDocument().use { doc ->
            val font = Exporter::class.java.getResourceAsStream("/fonts/LiberationSans-Regular.ttf")
                ?.use { PDType0Font.load(doc, it) }
                ?: run {
                    AppLog.log("export: нет шрифта для PDF")
                    return
                }
            val writer = PdfWriter(doc, font)
            writer.text("ЛинТех 28 — дневник", 20f, gap = 6f)
            writer.text(
                "Выгружено " + SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("ru")).format(Date()),
                10f, gap = 14f,
            )

            if (diary.isNotEmpty()) {
                writer.text("Дневник", 15f, gap = 8f)
                val dayFmt = SimpleDateFormat("dd.MM.yyyy", Locale("ru"))
                diary.groupBy { dayFmt.format(it.date) }.toSortedMap().forEach { (day, rows) ->
                    writer.text(day, 12f, gap = 3f)
                    rows.sortedBy { it.lessonNumber }.forEach { e ->
                        val mark = if (e.mark.isBlank()) "" else " · оценка ${e.mark.removePrefix("!")}"
                        val hw = if (e.homework.isBlank()) "" else " · ДЗ: ${e.homework}"
                        writer.text("${e.lessonNumber}. ${e.subjectName}$mark$hw", 10f, gap = 3f)
                    }
                    writer.space(4f)
                }
            }

            if (marks.isNotEmpty()) {
                writer.text("Оценки", 15f, gap = 8f)
                marks.sortedBy { it.subjectName }.forEach { s ->
                    val numeric = s.marks.mapNotNull { it.mark.toIntOrNull() }
                    val avg = if (numeric.isEmpty()) "—"
                    else String.format(Locale("ru"), "%.2f", numeric.sum().toDouble() / numeric.size)
                    writer.text("${s.subjectName} — средний $avg", 11f, gap = 3f)
                    val totals = s.totalMarks.entries.sortedBy { it.key }
                        .joinToString(", ") { (p, m) -> "${periodLabel(p)}: $m" }
                    if (totals.isNotEmpty()) writer.text(totals, 9f, gap = 3f)
                    val list = s.marks.filter { it.mark.isNotBlank() }
                        .joinToString(" ") { it.mark.removePrefix("!") }
                    if (list.isNotEmpty()) writer.text("Оценки: $list", 9f, gap = 6f)
                }
            }
            writer.close()
            doc.save(file)
        }
    }

    private class PdfWriter(private val doc: PDDocument, private val font: PDType0Font) {
        private val margin = 42f
        private val usableWidth = PDRectangle.A4.width - margin * 2
        private var stream: PDPageContentStream? = null
        private var y = 0f

        init {
            newPage()
        }

        private fun newPage() {
            stream?.close()
            val page = PDPage(PDRectangle.A4)
            doc.addPage(page)
            stream = PDPageContentStream(doc, page)
            y = PDRectangle.A4.height - margin
        }

        fun text(value: String, size: Float = 11f, gap: Float = 5f) {
            for (line in wrap(sanitize(value), size)) {
                if (y < margin + size + 4f) newPage()
                stream?.apply {
                    beginText()
                    setFont(font, size)
                    newLineAtOffset(margin, y)
                    showText(line)
                    endText()
                }
                y -= size + gap
            }
        }

        fun space(height: Float) {
            y -= height
        }

        fun close() {
            stream?.close()
            stream = null
        }

        private fun wrap(text: String, size: Float): List<String> {
            if (text.isBlank()) return listOf("")
            val words = text.replace('\n', ' ').split(' ')
            val lines = mutableListOf<String>()
            var current = StringBuilder()
            for (word in words) {
                val candidate = if (current.isEmpty()) word else "$current $word"
                val width = runCatching { font.getStringWidth(candidate) / 1000f * size }.getOrDefault(0f)
                if (width > usableWidth && current.isNotEmpty()) {
                    lines += current.toString()
                    current = StringBuilder(word)
                } else {
                    current = StringBuilder(candidate)
                }
            }
            if (current.isNotEmpty()) lines += current.toString()
            return lines.ifEmpty { listOf("") }
        }

        /** Выкидывает символы, которых нет в шрифте (эмодзи и т.п.). */
        private fun sanitize(value: String): String =
            value.filter { ch -> runCatching { font.getStringWidth(ch.toString()) }.isSuccess }
    }

    // ---- XLSX ----

    private fun writeXlsx(marks: List<SubjectMarks>, file: File) {
        ZipOutputStream(file.outputStream()).use { zip ->
            put(zip, "[Content_Types].xml", contentTypes())
            put(zip, "_rels/.rels", rootRels())
            put(zip, "xl/workbook.xml", workbook())
            put(zip, "xl/_rels/workbook.xml.rels", workbookRels())
            put(zip, "xl/worksheets/sheet1.xml", sheetXml(marks))
        }
    }

    private fun sheetXml(marks: List<SubjectMarks>): String {
        val body = StringBuilder()
        var row = 1

        fun addRow(values: List<String>) {
            body.append("<row r=\"$row\">")
            values.forEachIndexed { index, value ->
                val ref = ('A' + index).toString() + row
                body.append("<c r=\"$ref\" t=\"inlineStr\"><is><t xml:space=\"preserve\">")
                body.append(escapeXml(value))
                body.append("</t></is></c>")
            }
            body.append("</row>")
            row++
        }

        addRow(listOf("Предмет", "Средний балл", "Итоговые", "Оценки"))
        marks.sortedBy { it.subjectName }.forEach { s ->
            val numeric = s.marks.mapNotNull { it.mark.toIntOrNull() }
            val avg = if (numeric.isEmpty()) "—"
            else String.format(Locale("ru"), "%.2f", numeric.sum().toDouble() / numeric.size)
            val totals = s.totalMarks.entries.sortedBy { it.key }
                .joinToString(", ") { (p, m) -> "${periodLabel(p)}: $m" }
            val list = s.marks.filter { it.mark.isNotBlank() }
                .joinToString(" ") { it.mark.removePrefix("!") }
            addRow(listOf(s.subjectName, avg, totals, list))
        }

        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
<sheetData>$body</sheetData></worksheet>"""
    }

    private fun contentTypes() = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
<Default Extension="xml" ContentType="application/xml"/>
<Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
<Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
</Types>"""

    private fun rootRels() = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>"""

    private fun workbook() = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"
 xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
<sheets><sheet name="Оценки" sheetId="1" r:id="rId1"/></sheets></workbook>"""

    private fun workbookRels() = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
</Relationships>"""

    private fun put(zip: ZipOutputStream, name: String, content: String) {
        zip.putNextEntry(ZipEntry(name))
        zip.write(content.toByteArray(Charsets.UTF_8))
        zip.closeEntry()
    }

    private fun escapeXml(value: String): String = value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")

    // ---- ICS ----

    private fun writeIcs(schedule: SchoolSchedule?, className: String?, file: File) {
        val out = StringBuilder()
        out.append("BEGIN:VCALENDAR\r\n")
        out.append("VERSION:2.0\r\n")
        out.append("PRODID:-//Linteh28//Diary//RU\r\n")
        out.append("CALSCALE:GREGORIAN\r\n")
        out.append("METHOD:PUBLISH\r\n")

        if (schedule != null && !className.isNullOrBlank()) {
            out.append("X-WR-CALNAME:").append(icsEscape("ЛинТех 28 · $className")).append("\r\n")
            val monday = Calendar.getInstance().apply {
                val dow = get(Calendar.DAY_OF_WEEK)
                add(Calendar.DAY_OF_YEAR, if (dow == Calendar.SUNDAY) -6 else Calendar.MONDAY - dow)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val keys = listOf("mon", "tue", "wed", "thu", "fri", "sat", "sun")
            val dt = SimpleDateFormat("yyyyMMdd'T'HHmmss", Locale.US)
            val stamp = SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }.format(Date())

            for (i in 0 until 7) {
                val day = (monday.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, i) }
                val key = keys[i]
                val lessons = schedule.lessonsFor(className, key)
                val bells = schedule.bellsFor(key)
                lessons.forEach { lesson ->
                    val bell = bells.firstOrNull { it.n == lesson.n } ?: return@forEach
                    val start = eventTime(day, bell.start) ?: return@forEach
                    val end = eventTime(day, bell.end) ?: return@forEach
                    out.append("BEGIN:VEVENT\r\n")
                    out.append("UID:lintech28-$i-${lesson.n}@lintech28\r\n")
                    out.append("DTSTAMP:").append(stamp).append("\r\n")
                    out.append("DTSTART:").append(dt.format(start)).append("\r\n")
                    out.append("DTEND:").append(dt.format(end)).append("\r\n")
                    out.append("SUMMARY:").append(icsEscape(lesson.subject)).append("\r\n")
                    lesson.room?.takeIf { it.isNotBlank() }?.let {
                        out.append("LOCATION:").append(icsEscape("каб. $it")).append("\r\n")
                    }
                    lesson.teacher?.takeIf { it.isNotBlank() }?.let {
                        out.append("DESCRIPTION:").append(icsEscape(it)).append("\r\n")
                    }
                    out.append("END:VEVENT\r\n")
                }
            }
        }

        out.append("END:VCALENDAR\r\n")
        file.writeText(out.toString())
    }

    private fun eventTime(day: Calendar, hhmm: String): Date? {
        val parts = hhmm.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: return null
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: return null
        return (day.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time
    }

    private fun icsEscape(value: String): String = value
        .replace("\\", "\\\\")
        .replace(";", "\\;")
        .replace(",", "\\,")
        .replace("\n", "\\n")

    // ---- общее ----

    private fun zipFiles(files: List<File>, zip: File) {
        ZipOutputStream(zip.outputStream()).use { out ->
            files.filter { it.exists() }.forEach { file ->
                out.putNextEntry(ZipEntry(file.name))
                file.inputStream().use { it.copyTo(out) }
                out.closeEntry()
            }
        }
    }

    private fun periodLabel(periodId: Int): String = when (periodId) {
        360000 -> "1 четв."
        360001 -> "2 четв."
        360002 -> "3 четв."
        360003 -> "4 четв."
        360011 -> "1 полуг."
        360012 -> "2 полуг."
        else -> "Пер. $periodId"
    }
}
