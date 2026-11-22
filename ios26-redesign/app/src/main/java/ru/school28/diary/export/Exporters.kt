package ru.school28.diary.export

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import ru.school28.diary.api.DairyEntry
import ru.school28.diary.api.SchoolSchedule
import ru.school28.diary.api.SubjectMarks
import ru.school28.diary.ui.GoalMath
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object ExportFiles {

    fun directory(context: Context): File {
        val dir = File(context.filesDir, "exports")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun file(context: Context, name: String): File = File(directory(context), name)

    fun stamp(): String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())

    fun share(context: Context, file: File, mime: String) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, file.name)
            clipData = ClipData.newRawUri("export", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(send, "Поделиться файлом")
        if (context !is android.app.Activity) {
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}

private const val INK = 0xFF16171D.toInt()
private const val INK_SOFT = 0xFF6B7080.toInt()
private const val INK_LIGHT = 0xFF9AA0AE.toInt()
private const val ACCENT = 0xFF5A5AF2.toInt()
private const val LINE = 0xFFE2E5EE.toInt()
private const val HEADER_FILL = 0xFFF0F2F8.toInt()
private const val DAY_FILL = 0xFFEDEDFB.toInt()

object PdfExport {

    fun create(
        context: Context,
        entries: List<DairyEntry>,
        subjects: List<SubjectMarks>,
        periodLabel: String,
    ): File {
        val file = ExportFiles.file(context, "lintech_diary_${ExportFiles.stamp()}.pdf")
        val document = PdfDocument()
        val writer = PdfPageWriter(document)

        writer.text("ЛинТех 28 · электронный дневник", 40f, 10f, paint(8.5f, false, INK_SOFT))
        writer.advance(16f)
        writer.text("Дневник и оценки", 40f, 26f, paint(22f, true, INK))
        writer.advance(30f)
        writer.text("Период: $periodLabel", 40f, 12f, paint(10f, false, INK_SOFT))
        writer.advance(16f)
        val generated = SimpleDateFormat("d MMMM yyyy, HH:mm", Locale("ru")).format(Date())
        writer.text("Сформировано: $generated", 40f, 10f, paint(8.5f, false, INK_LIGHT))
        writer.advance(16f)
        writer.rule()
        writer.advance(10f)

        writer.section("Дневник")
        val headers = listOf("№", "Предмет", "Тема урока", "Домашнее задание", "Оценка")
        val widths = floatArrayOf(24f, 102f, 148f, 180f, 61f)
        val paints = listOf(
            paint(9f, false, INK),
            paint(9f, false, INK),
            paint(9f, false, INK_SOFT),
            paint(9f, false, INK_SOFT),
            paint(9.5f, true, INK),
        )
        val headerPaint = paint(9f, true, INK)

        if (entries.isEmpty()) {
            writer.text("За выбранный период записей нет", 44f, 14f, paint(10f, false, INK_SOFT))
            writer.advance(20f)
        } else {
            writer.tableHeader(headers, widths, headerPaint)
            val dayFormat = SimpleDateFormat("EEEE, d MMMM", Locale("ru"))
            val grouped = entries.sortedBy { it.date }.groupBy { dayKey(it.date) }.toSortedMap()
            grouped.values.forEach { dayEntries ->
                val first = dayEntries.first().date
                if (writer.ensure(20f + writer.rowHeight(listOf("x"), floatArrayOf(300f), listOf(paint(9f, false, INK))))) {
                    writer.tableHeader(headers, widths, headerPaint)
                }
                writer.fillRow(20f, DAY_FILL)
                writer.text(
                    dayFormat.format(first).replaceFirstChar {
                        if (it.isLowerCase()) it.titlecase(Locale("ru")) else it.toString()
                    },
                    writer.margin + 6f,
                    14f,
                    paint(9.5f, true, INK),
                )
                writer.advance(20f)
                dayEntries.forEach { entry ->
                    val cells = listOf(
                        entry.lessonNumber.toString(),
                        entry.subjectName,
                        entry.theme.ifBlank { "—" },
                        entry.homework.ifBlank { "—" },
                        entry.mark.removePrefix("!").ifBlank { "—" },
                    )
                    val rowHeight = writer.rowHeight(cells, widths, paints)
                    if (writer.ensure(rowHeight)) {
                        writer.tableHeader(headers, widths, headerPaint)
                    }
                    writer.row(cells, widths, paints, rowHeight)
                }
            }
        }

        writer.advance(10f)
        writer.section("Оценки")

        val markHeaders = listOf("Предмет", "Учитель", "Средний", "Оценок", "Итоги")
        val markWidths = floatArrayOf(168f, 138f, 52f, 48f, 109f)
        val markPaints = listOf(
            paint(9f, false, INK),
            paint(9f, false, INK_SOFT),
            paint(9f, true, INK),
            paint(9f, false, INK_SOFT),
            paint(8.5f, false, INK_SOFT),
        )

        if (subjects.isEmpty()) {
            writer.text("Данных об оценках нет", 44f, 14f, paint(10f, false, INK_SOFT))
            writer.advance(18f)
        } else {
            writer.tableHeader(markHeaders, markWidths, headerPaint)
            subjects.sortedBy { it.subjectName }.forEach { subject ->
                val numeric = subject.marks.mapNotNull { it.mark.toIntOrNull() }
                val average = GoalMath.currentPeriodAverage(subject)?.let {
                    String.format(Locale("ru"), "%.2f", it)
                } ?: "—"
                val totals = subject.totalMarks.entries.sortedBy { it.key }
                    .joinToString(", ") { (period, mark) -> "${periodLabel(period)}: $mark" }
                val cells = listOf(
                    subject.subjectName,
                    subject.teacherName.orEmpty().ifBlank { "—" },
                    average,
                    numeric.size.toString(),
                    totals.ifBlank { "—" },
                )
                val rowHeight = writer.rowHeight(cells, markWidths, markPaints)
                if (writer.ensure(rowHeight)) {
                    writer.tableHeader(markHeaders, markWidths, headerPaint)
                }
                writer.row(cells, markWidths, markPaints, rowHeight)
            }
        }

        writer.finish()
        FileOutputStream(file).use { document.writeTo(it) }
        document.close()
        return file
    }

    private fun dayKey(date: Date): Int {
        val calendar = Calendar.getInstance().apply { time = date }
        return calendar.get(Calendar.YEAR) * 1000 + calendar.get(Calendar.DAY_OF_YEAR)
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

private class PdfPageWriter(private val document: PdfDocument) {
    private val pageWidth = 595
    private val pageHeight = 842
    val margin = 40f
    private var pageNumber = 0
    private var page: PdfDocument.Page? = null
    private var canvas: Canvas? = null
    var y = margin
        private set

    init {
        newPage()
    }

    fun newPage() {
        page?.let {
            footer(it.canvas)
            document.finishPage(it)
        }
        pageNumber++
        val created = document.startPage(
            PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        )
        page = created
        canvas = created.canvas
        y = margin
    }

    fun finish() {
        page?.let {
            footer(it.canvas)
            document.finishPage(it)
        }
        page = null
    }

    fun advance(height: Float) {
        y += height
    }

    fun ensure(height: Float): Boolean {
        if (y + height > pageHeight - margin - 20f) {
            newPage()
            return true
        }
        return false
    }

    fun text(value: String, x: Float, baseline: Float, paint: Paint) {
        canvas?.drawText(value, x, y + baseline, paint)
    }

    fun rule() {
        canvas?.drawLine(
            margin,
            y,
            pageWidth - margin,
            y,
            Paint().apply {
                color = LINE
                strokeWidth = 0.7f
                isAntiAlias = true
            },
        )
    }

    fun fillRow(height: Float, color: Int) {
        canvas?.drawRect(
            margin,
            y,
            pageWidth - margin,
            y + height,
            Paint().apply {
                this.color = color
                style = Paint.Style.FILL
            },
        )
    }

    fun section(title: String) {
        ensure(30f)
        text(title, margin, 16f, paint(15f, true, INK))
        advance(22f)
        rule()
        advance(7f)
    }

    fun tableHeader(cells: List<String>, widths: FloatArray, paint: Paint) {
        val height = 22f
        fillRow(height, HEADER_FILL)
        var x = margin
        cells.forEachIndexed { index, value ->
            text(value, x + 4f, 15f, paint)
            x += widths[index]
        }
        advance(height)
        rule()
    }

    fun rowHeight(cells: List<String>, widths: FloatArray, paints: List<Paint>): Float {
        val lines = cells.mapIndexed { index, value ->
            wrap(value, paints[index], widths[index] - 8f).size.coerceAtLeast(1)
        }.maxOrNull() ?: 1
        return lines * 12.5f + 9f
    }

    fun row(cells: List<String>, widths: FloatArray, paints: List<Paint>, height: Float) {
        var x = margin
        cells.forEachIndexed { index, value ->
            val lines = wrap(value, paints[index], widths[index] - 8f)
            var baseline = 13f
            lines.forEach { line ->
                text(line, x + 4f, baseline, paints[index])
                baseline += 12.5f
            }
            x += widths[index]
        }
        advance(height)
        rule()
    }

    private fun wrap(value: String, paint: Paint, maxWidth: Float): List<String> {
        val normalized = value.replace('\n', ' ').trim()
        if (normalized.isEmpty()) return listOf("")
        val result = mutableListOf<String>()
        var remaining = normalized
        while (remaining.isNotEmpty()) {
            var count = paint.breakText(remaining, true, maxWidth, null)
            if (count <= 0) count = 1
            if (count < remaining.length) {
                val lastSpace = remaining.substring(0, count).lastIndexOf(' ')
                if (lastSpace > 0) count = lastSpace + 1
            }
            result += remaining.substring(0, count).trim()
            remaining = remaining.substring(count).trimStart()
        }
        return result.ifEmpty { listOf("") }
    }

    private fun footer(canvas: Canvas) {
        val paint = paint(8f, false, INK_LIGHT)
        canvas.drawLine(
            margin,
            pageHeight - 28f,
            pageWidth - margin,
            pageHeight - 28f,
            Paint().apply {
                color = LINE
                strokeWidth = 0.6f
                isAntiAlias = true
            },
        )
        canvas.drawText("ЛинТех 28 · экспорт", margin, pageHeight - 16f, paint)
        val number = "стр. $pageNumber"
        canvas.drawText(
            number,
            pageWidth - margin - paint.measureText(number),
            pageHeight - 16f,
            paint,
        )
    }
}

private fun paint(size: Float, bold: Boolean, color: Int): Paint = Paint().apply {
    isAntiAlias = true
    textSize = size
    this.color = color
    typeface = Typeface.create("sans-serif", if (bold) Typeface.BOLD else Typeface.NORMAL)
}

object XlsxExport {

    fun create(
        context: Context,
        entries: List<DairyEntry>,
        subjects: List<SubjectMarks>,
        periodLabel: String,
    ): File {
        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale("ru"))
        val diaryRows = entries.sortedBy { it.date }.map { entry ->
            listOf(
                dateFormat.format(entry.date),
                entry.lessonNumber.toString(),
                entry.subjectName,
                entry.theme.ifBlank { "—" },
                entry.homework.ifBlank { "—" },
                entry.mark.removePrefix("!").ifBlank { "—" },
            )
        }
        val markRows = subjects.sortedBy { it.subjectName }.map { subject ->
            val numeric = subject.marks.mapNotNull { it.mark.toIntOrNull() }
            val average = if (numeric.isEmpty()) "—" else String.format(Locale("ru"), "%.2f", numeric.sum().toDouble() / numeric.size)
            val totals = subject.totalMarks.entries.sortedBy { it.key }
                .joinToString(", ") { (period, mark) -> "${xlsxPeriodLabel(period)}: $mark" }
            listOf(
                subject.subjectName,
                subject.teacherName.orEmpty().ifBlank { "—" },
                average,
                numeric.size.toString(),
                totals.ifBlank { "—" },
            )
        }

        val diarySheet = Sheet(
            name = "Дневник",
            headers = listOf("Дата", "Урок", "Предмет", "Тема урока", "Домашнее задание", "Оценка"),
            rows = diaryRows,
            widths = listOf(12, 7, 24, 34, 44, 10),
        )
        val marksSheet = Sheet(
            name = "Оценки",
            headers = listOf("Предмет", "Учитель", "Средний", "Оценок", "Итоги"),
            rows = markRows,
            widths = listOf(28, 26, 11, 10, 40),
        )

        val file = ExportFiles.file(context, "lintech_marks_${ExportFiles.stamp()}.xlsx")
        ZipOutputStream(FileOutputStream(file)).use { zip ->
            writeEntry(zip, "[Content_Types].xml", contentTypes())
            writeEntry(zip, "_rels/.rels", rootRels())
            writeEntry(zip, "xl/workbook.xml", workbook(periodLabel))
            writeEntry(zip, "xl/_rels/workbook.xml.rels", workbookRels())
            writeEntry(zip, "xl/styles.xml", styles())
            writeEntry(zip, "xl/worksheets/sheet1.xml", worksheet(diarySheet))
            writeEntry(zip, "xl/worksheets/sheet2.xml", worksheet(marksSheet))
        }
        return file
    }

    private data class Sheet(
        val name: String,
        val headers: List<String>,
        val rows: List<List<String>>,
        val widths: List<Int>,
    )

    private fun writeEntry(zip: ZipOutputStream, name: String, content: String) {
        zip.putNextEntry(ZipEntry(name))
        zip.write(content.toByteArray(Charsets.UTF_8))
        zip.closeEntry()
    }

    private fun contentTypes(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
<Default Extension="xml" ContentType="application/xml"/>
<Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
<Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
<Override PartName="/xl/worksheets/sheet2.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
<Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
</Types>"""

    private fun rootRels(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>"""

    private fun workbook(periodLabel: String): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
<bookViews><workbookView activeTab="0"/></bookViews>
<sheets>
<sheet name="Дневник" sheetId="1" r:id="rId1"/>
<sheet name="Оценки" sheetId="2" r:id="rId2"/>
</sheets>
</workbook>"""

    private fun workbookRels(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
<Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet2.xml"/>
<Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
</Relationships>"""

    private fun styles(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
<fonts count="2">
<font><sz val="11"/><color rgb="FF16171D"/><name val="Calibri"/><family val="2"/></font>
<font><b/><sz val="11"/><color rgb="FF16171D"/><name val="Calibri"/><family val="2"/></font>
</fonts>
<fills count="2">
<fill><patternFill patternType="none"/></fill>
<fill><patternFill patternType="gray125"/></fill>
</fills>
<borders count="1"><border><left/><right/><top/><bottom/><diagonal/></border></borders>
<cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>
<cellXfs count="2">
<xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>
<xf numFmtId="0" fontId="1" fillId="0" borderId="0" xfId="0" applyFont="1"/>
</cellXfs>
<cellStyles count="1"><cellStyle name="Normal" xfId="0" builtinId="0"/></cellStyles>
</styleSheet>"""

    private fun worksheet(sheet: Sheet): String {
        val columns = buildString {
            append("<cols>")
            sheet.widths.forEachIndexed { index, width ->
                append("""<col min="${index + 1}" max="${index + 1}" width="$width" customWidth="1"/>""")
            }
            append("</cols>")
        }
        val rowsXml = buildString {
            append("<row r=\"1\">")
            sheet.headers.forEachIndexed { index, header ->
                append(cell("${columnName(index)}1", header, header = true))
            }
            append("</row>")
            sheet.rows.forEachIndexed { rowIndex, row ->
                val number = rowIndex + 2
                append("<row r=\"$number\">")
                row.forEachIndexed { index, value ->
                    append(cell("${columnName(index)}$number", value, header = false))
                }
                append("</row>")
            }
        }
        val lastColumn = columnName(sheet.headers.lastIndex)
        val lastRow = sheet.rows.size + 1
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
<dimension ref="A1:$lastColumn$lastRow"/>
<sheetViews><sheetView tabSelected="1" workbookViewId="0"><pane ySplit="1" topLeftCell="A2" activePane="bottomLeft" state="frozen"/></sheetView></sheetViews>
<sheetFormatPr defaultRowHeight="15"/>
$columns
<sheetData>$rowsXml</sheetData>
<pageMargins left="0.7" right="0.7" top="0.75" bottom="0.75" header="0.3" footer="0.3"/>
</worksheet>"""
    }

    private fun cell(reference: String, value: String, header: Boolean): String {
        val style = if (header) " s=\"1\"" else ""
        return """<c r="$reference"$style t="inlineStr"><is><t xml:space="preserve">${escapeXml(value)}</t></is></c>"""
    }

    private fun columnName(index: Int): String {
        var value = index
        val result = StringBuilder()
        do {
            result.insert(0, ('A' + value % 26))
            value = value / 26 - 1
        } while (value >= 0)
        return result.toString()
    }

    private fun escapeXml(value: String): String = buildString {
        value.forEach { char ->
            when (char) {
                '&' -> append("&amp;")
                '<' -> append("&lt;")
                '>' -> append("&gt;")
                '"' -> append("&quot;")
                '\'' -> append("&apos;")
                else -> if (char.code >= 0x20 || char == '\n' || char == '\t') append(char)
            }
        }
    }

    private fun xlsxPeriodLabel(periodId: Int): String = when (periodId) {
        360000 -> "1 четв."
        360001 -> "2 четв."
        360002 -> "3 четв."
        360003 -> "4 четв."
        360011 -> "1 полуг."
        360012 -> "2 полуг."
        else -> "Пер. $periodId"
    }
}

object IcsExport {

    fun create(context: Context, schedule: SchoolSchedule, className: String): File {
        val lessonsByDay = listOf(
            "mon" to Calendar.MONDAY,
            "tue" to Calendar.TUESDAY,
            "wed" to Calendar.WEDNESDAY,
            "thu" to Calendar.THURSDAY,
            "fri" to Calendar.FRIDAY,
            "sat" to Calendar.SATURDAY,
        )
        val monday = Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.MONDAY
            val day = if (get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) 7 else get(Calendar.DAY_OF_WEEK) - 1
            add(Calendar.DAY_OF_YEAR, -(day - 1))
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val dtStamp = SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date())
        val dtFormat = SimpleDateFormat("yyyyMMdd'T'HHmmss", Locale.US)
        val lines = mutableListOf(
            "BEGIN:VCALENDAR",
            "VERSION:2.0",
            "PRODID:-//LinTech 28//School Diary//RU",
            "CALSCALE:GREGORIAN",
            "METHOD:PUBLISH",
            "X-WR-CALNAME:ЛинТех 28 — $className",
        )

        var events = 0
        lessonsByDay.forEach dayLoop@ { (key, _) ->
            val dayLessons = schedule.lessonsFor(className, key)
            if (dayLessons.isEmpty()) return@dayLoop
            val bells = schedule.bellsFor(key)
            val calendar = (monday.clone() as Calendar).apply {
                add(Calendar.DAY_OF_YEAR, dayIndex(key))
            }
            dayLessons.forEach lessonLoop@ { lesson ->
                val bell = bells.firstOrNull { it.n == lesson.n } ?: return@lessonLoop
                val start = dateTime(calendar, bell.start) ?: return@lessonLoop
                val end = dateTime(calendar, bell.end) ?: return@lessonLoop
                events++
                lines += "BEGIN:VEVENT"
                lines += "UID:lintech28-${dtFormat.format(calendar.time)}-${lesson.n}@school28"
                lines += "DTSTAMP:$dtStamp"
                lines += "DTSTART:${dtFormat.format(start.time)}"
                lines += "DTEND:${dtFormat.format(end.time)}"
                lines += "SUMMARY:${escapeIcs(lesson.subject)}"
                lesson.room?.takeIf { it.isNotBlank() }?.let {
                    lines += "LOCATION:${escapeIcs("каб. $it")}"
                }
                lesson.teacher?.takeIf { it.isNotBlank() }?.let {
                    lines += "DESCRIPTION:${escapeIcs(it)}"
                }
                lines += "END:VEVENT"
            }
        }

        if (events == 0) {
            throw IllegalStateException("На эту неделю нет уроков с расписанием")
        }
        lines += "END:VCALENDAR"

        val file = ExportFiles.file(context, "lintech_schedule_${ExportFiles.stamp()}.ics")
        file.writeText(lines.joinToString("\r\n") { foldIcs(it) } + "\r\n", Charsets.UTF_8)
        return file
    }

    private fun dayIndex(key: String): Int = when (key) {
        "mon" -> 0
        "tue" -> 1
        "wed" -> 2
        "thu" -> 3
        "fri" -> 4
        "sat" -> 5
        else -> 0
    }

    private fun dateTime(day: Calendar, time: String): Calendar? {
        val parts = time.split(":")
        if (parts.size != 2) return null
        val hour = parts[0].trim().toIntOrNull() ?: return null
        val minute = parts[1].trim().toIntOrNull() ?: return null
        return (day.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    private fun escapeIcs(value: String): String = value
        .replace("\\", "\\\\")
        .replace(";", "\\;")
        .replace(",", "\\,")
        .replace("\n", "\\n")

    private fun foldIcs(line: String): String {
        val bytes = line.toByteArray(Charsets.UTF_8)
        if (bytes.size <= 75) return line
        val result = StringBuilder()
        val current = StringBuilder()
        var currentBytes = 0
        line.forEach { char ->
            val charBytes = char.toString().toByteArray(Charsets.UTF_8).size
            if (currentBytes + charBytes > 73) {
                result.append(current).append("\r\n ")
                current.clear()
                currentBytes = 0
            }
            current.append(char)
            currentBytes += charBytes
        }
        result.append(current)
        return result.toString()
    }
}

private fun dayKey(date: Date): Int {
    val calendar = Calendar.getInstance().apply { time = date }
    return calendar.get(Calendar.YEAR) * 1000 + calendar.get(Calendar.DAY_OF_YEAR)
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
