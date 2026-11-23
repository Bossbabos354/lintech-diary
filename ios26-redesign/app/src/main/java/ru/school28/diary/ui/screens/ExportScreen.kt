package ru.school28.diary.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.school28.diary.Prefs
import ru.school28.diary.api.ApiClient
import ru.school28.diary.export.ExportFiles
import ru.school28.diary.export.IcsExport
import ru.school28.diary.export.PdfExport
import ru.school28.diary.export.XlsxExport
import ru.school28.diary.glass.Glass
import ru.school28.diary.glass.GlassButton
import ru.school28.diary.glass.GlassButtonVariant
import ru.school28.diary.glass.GlassCard
import ru.school28.diary.glass.GlassChip
import ru.school28.diary.glass.GlassIconBubble
import ru.school28.diary.glass.GlassScreenScaffold
import ru.school28.diary.glass.SectionLabel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExportScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val colors = Glass.colors
    val scope = rememberCoroutineScope()
    val ru = remember { Locale("ru") }
    val shortFmt = remember { SimpleDateFormat("d MMM", ru) }
    val monthFmt = remember { SimpleDateFormat("LLLL yyyy", ru) }

    var periodIndex by remember { mutableIntStateOf(0) }
    var busy by remember { mutableStateOf<String?>(null) }
    var lastFile by remember { mutableStateOf<File?>(null) }

    fun weekStart(offset: Int): Calendar = Calendar.getInstance().apply {
        firstDayOfWeek = Calendar.MONDAY
        val day = if (get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) 7 else get(Calendar.DAY_OF_WEEK) - 1
        add(Calendar.DAY_OF_YEAR, -(day - 1) + offset * 7)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    fun period(): Triple<Date, Date, String> {
        return when (periodIndex) {
            1 -> {
                val start = weekStart(-1)
                val end = (start.clone() as Calendar).apply {
                    add(Calendar.DAY_OF_YEAR, 6)
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                }
                Triple(start.time, end.time, "${shortFmt.format(start.time)} — ${shortFmt.format(end.time)}")
            }
            2 -> {
                val start = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val end = (start.clone() as Calendar).apply {
                    set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                }
                val month = monthFmt.format(start.time)
                    .replaceFirstChar { if (it.isLowerCase()) it.titlecase(ru) else it.toString() }
                Triple(start.time, end.time, month)
            }
            else -> {
                val start = weekStart(0)
                val end = (start.clone() as Calendar).apply {
                    add(Calendar.DAY_OF_YEAR, 6)
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                }
                Triple(start.time, end.time, "${shortFmt.format(start.time)} — ${shortFmt.format(end.time)}")
            }
        }
    }

    val range = remember(periodIndex) { period() }

    suspend fun loadIds(): Triple<Int, Int, Int> = withContext(Dispatchers.IO) {
        val studentId = Prefs.getStudentId(ctx)
        if (studentId == -1) error("Сначала войди в аккаунт")
        val uchYear = Prefs.getUchYear(ctx)
        var classId = Prefs.getClassId(ctx)
        if (classId == -1) {
            classId = ApiClient.getStudentClass(studentId, uchYear)
            Prefs.setClassId(ctx, classId)
        }
        if (classId == -1) error("Не удалось определить класс")
        Triple(studentId, classId, uchYear)
    }

    fun export(kind: String) {
        if (busy != null) return
        busy = kind
        scope.launch {
            try {
                when (kind) {
                    "pdf" -> {
                        val (studentId, classId, uchYear) = loadIds()
                        val entries = withContext(Dispatchers.IO) {
                            ApiClient.getDairy(classId, studentId, range.first, range.second)
                        }
                        val subjects = withContext(Dispatchers.IO) {
                            ApiClient.getMarks(classId, studentId, uchYear)
                        }
                        val file = withContext(Dispatchers.IO) {
                            PdfExport.create(ctx, entries, subjects, range.third)
                        }
                        lastFile = file
                        ExportFiles.share(ctx, file, "application/pdf")
                    }
                    "xlsx" -> {
                        val (studentId, classId, uchYear) = loadIds()
                        val entries = withContext(Dispatchers.IO) {
                            ApiClient.getDairy(classId, studentId, range.first, range.second)
                        }
                        val subjects = withContext(Dispatchers.IO) {
                            ApiClient.getMarks(classId, studentId, uchYear)
                        }
                        val file = withContext(Dispatchers.IO) {
                            XlsxExport.create(ctx, entries, subjects, range.third)
                        }
                        lastFile = file
                        ExportFiles.share(
                            ctx,
                            file,
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                        )
                    }
                    "ics" -> {
                        val schedule = withContext(Dispatchers.IO) { ApiClient.getSchedule() }
                            ?: error("Сайт с расписанием недоступен")
                        val className = Prefs.getSelectedClass(ctx)?.takeIf { it in schedule.classes }
                            ?: schedule.classes.firstOrNull()
                            ?: error("Не удалось определить класс")
                        val file = withContext(Dispatchers.IO) {
                            IcsExport.create(ctx, schedule, className)
                        }
                        lastFile = file
                        ExportFiles.share(ctx, file, "text/calendar")
                    }
                }
                Toast.makeText(ctx, "Файл готов", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(ctx, e.message ?: "Не удалось экспортировать", Toast.LENGTH_LONG).show()
            } finally {
                busy = null
            }
        }
    }

    GlassScreenScaffold(
        title = "Экспорт",
        subtitle = "Дневник, оценки и расписание в файлы",
        onBack = onBack,
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(key = "intro") {
                GlassCard(tint = colors.accent, contentPadding = PaddingValues(15.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        GlassIconBubble(
                            icon = Icons.Rounded.FileDownload,
                            tint = colors.accent,
                            size = 44.dp,
                            iconSize = 22.dp,
                        )
                        Spacer(Modifier.width(11.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = "Данные в файл",
                                color = colors.text,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = "PDF и Excel с дневником и оценками, календарь .ics с расписанием",
                                color = colors.textSecondary,
                                fontSize = 11.sp,
                                lineHeight = 15.sp,
                            )
                        }
                    }
                }
            }

            item(key = "periodLabel") {
                SectionLabel("Период дневника")
            }

            item(key = "period") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    listOf("Текущая неделя", "Прошлая неделя", "Месяц").forEachIndexed { index, label ->
                        GlassChip(
                            text = label,
                            selected = periodIndex == index,
                            onClick = { periodIndex = index },
                            compact = true,
                        )
                    }
                }
            }

            item(key = "range") {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = range.third,
                        color = colors.textSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            item(key = "documentsLabel") {
                SectionLabel("Документы")
            }

            item(key = "documents") {
                GlassCard(contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)) {
                    ExportRow(
                        icon = Icons.Rounded.PictureAsPdf,
                        title = "Дневник и оценки · PDF",
                        subtitle = "Удобно читать и печатать",
                        buttonText = "PDF",
                        loading = busy == "pdf",
                        enabled = busy == null,
                        onClick = { export("pdf") },
                    )
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(start = 50.dp)
                            .height(1.dp)
                            .background(colors.separator)
                    )
                    ExportRow(
                        icon = Icons.Rounded.TableChart,
                        title = "Дневник и оценки · Excel",
                        subtitle = "Два листа: дневник и оценки",
                        buttonText = "XLSX",
                        loading = busy == "xlsx",
                        enabled = busy == null,
                        onClick = { export("xlsx") },
                    )
                }
            }

            item(key = "calendarLabel") {
                SectionLabel("Календарь")
            }

            item(key = "calendar") {
                GlassCard(contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)) {
                    ExportRow(
                        icon = Icons.Rounded.CalendarMonth,
                        title = "Расписание в календарь",
                        subtitle = "Файл .ics для Google и Apple Календаря",
                        buttonText = "ICS",
                        loading = busy == "ics",
                        enabled = busy == null,
                        onClick = { export("ics") },
                    )
                }
            }

            lastFile?.let { file ->
                item(key = "lastFile") {
                    GlassCard(tint = colors.success, contentPadding = PaddingValues(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = colors.success,
                                modifier = Modifier.size(22.dp),
                            )
                            Spacer(Modifier.width(9.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = "Файл готов",
                                    color = colors.text,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    text = file.name,
                                    color = colors.textSecondary,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            GlassButton(
                                text = "Поделиться",
                                onClick = {
                                    ExportFiles.share(ctx, file, mimeFor(file))
                                },
                                variant = GlassButtonVariant.Tinted,
                                icon = Icons.Rounded.Share,
                                height = 38.dp,
                            )
                        }
                    }
                }
            }

            item(key = "note") {
                Text(
                    text = "Файлы создаются в защищённой папке приложения и сразу открываются через «Поделиться»: можно сохранить в Downloads, отправить в мессенджер или открыть в календаре.",
                    color = colors.textTertiary,
                    fontSize = 10.sp,
                    lineHeight = 14.sp,
                    modifier = Modifier.padding(start = 6.dp, end = 6.dp, top = 2.dp),
                )
            }
        }
    }
}

@Composable
private fun ExportRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    buttonText: String,
    loading: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val colors = Glass.colors
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlassIconBubble(
            icon = icon,
            tint = colors.accent,
            size = 38.dp,
            iconSize = 19.dp,
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                color = colors.text,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = subtitle,
                color = colors.textSecondary,
                fontSize = 10.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        GlassButton(
            text = buttonText,
            onClick = onClick,
            variant = GlassButtonVariant.Tinted,
            enabled = enabled,
            loading = loading,
            modifier = Modifier.width(78.dp),
            height = 38.dp,
        )
    }
}

private fun mimeFor(file: File): String = when (file.extension.lowercase(Locale.US)) {
    "pdf" -> "application/pdf"
    "ics" -> "text/calendar"
    "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    else -> "*/*"
}
