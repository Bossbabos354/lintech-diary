package ru.school28.diary.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.school28.diary.Prefs
import ru.school28.diary.api.LessonMarks
import ru.school28.diary.api.SubjectMarks
import ru.school28.diary.glass.Glass
import ru.school28.diary.glass.GlassButton
import ru.school28.diary.glass.GlassButtonVariant
import ru.school28.diary.glass.GlassCard
import ru.school28.diary.glass.GlassIconButton
import ru.school28.diary.glass.GlassScreenScaffold
import ru.school28.diary.glass.GlassTextField
import ru.school28.diary.glass.MarkBadge
import ru.school28.diary.ui.GoalMath
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Все оценки по предмету: дата, повод и возможность дописать «за что»,
 * если учитель комментарий не оставил.
 */
@Composable
fun SubjectMarksScreen(
    subject: SubjectMarks,
    onBack: () -> Unit,
    onOpenGoal: (SubjectMarks) -> Unit,
) {
    val ctx = LocalContext.current
    val colors = Glass.colors
    var notesVersion by remember { mutableIntStateOf(0) }
    var editing by remember { mutableStateOf<LessonMarks?>(null) }
    var editText by remember { mutableStateOf("") }

    val marks = remember(subject.marks) {
        subject.marks.filter { it.mark.isNotBlank() }.sortedByDescending { it.date }
    }
    val avg = remember(subject.marks) { GoalMath.currentPeriodAverage(subject) }
    val dayFmt = remember { SimpleDateFormat("d MMM", Locale("ru")) }

    GlassScreenScaffold(
        title = subject.subjectName,
        subtitle = subject.teacherName ?: "Все оценки по предмету",
        onBack = onBack,
    ) {
        Column(Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item(key = "summary") {
                    GlassCard(tint = colors.accent, contentPadding = PaddingValues(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = "Средний балл за период",
                                    color = colors.textSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = avg?.let { String.format(Locale("ru"), "%.2f", it) } ?: "—",
                                    color = colors.text,
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                )
                            }
                            Text(
                                text = "${marks.size} оценок",
                                color = colors.textSecondary,
                                fontSize = 12.sp,
                            )
                        }
                    }
                }

                if (marks.isEmpty()) {
                    item(key = "empty") {
                        GlassCard {
                            Text(
                                text = "Оценок по предмету пока нет",
                                color = colors.textSecondary,
                                fontSize = 13.sp,
                            )
                        }
                    }
                }

                items(marks, key = { "${it.lessonId}" }) { mark ->
                    // «За что»: комментарий учителя, иначе своя подпись.
                    // Тема урока поводом не считается — она показывается отдельно.
                    val teacherNote = mark.comment?.takeIf { it.isNotBlank() }
                    val ownNote = remember(mark.lessonId, notesVersion) {
                        Prefs.getMarkNote(ctx, subject.subjectId, mark.lessonId)
                    }
                    MarkDetailRow(
                        mark = mark,
                        dayText = dayFmt.format(mark.date ?: java.util.Date(0L)),
                        note = teacherNote ?: ownNote,
                        editable = teacherNote == null,
                        onEdit = {
                            editing = mark
                            editText = ownNote.orEmpty()
                        },
                    )
                }
            }

            GlassButton(
                text = "Поставить цель",
                onClick = { onOpenGoal(subject) },
                icon = Icons.Rounded.Flag,
                variant = GlassButtonVariant.Primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            )
        }
    }

    editing?.let { mark ->
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable { editing = null },
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.padding(24.dp)) {
                // Непрозрачная карточка: текст и поле ввода не просвечивают сквозь список.
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(colors.background)
                        .border(1.dp, colors.glassBorderTop, RoundedCornerShape(24.dp))
                        .padding(18.dp),
                ) {
                    Text(
                        text = "За что оценка",
                        color = colors.text,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "${mark.subjectName} · ${mark.mark.removePrefix("!")} · ${dayFmt.format(mark.date ?: java.util.Date(0L))}",
                        color = colors.textSecondary,
                        fontSize = 12.sp,
                    )
                    Spacer(Modifier.height(12.dp))
                    GlassTextField(
                        value = editText,
                        onValueChange = { editText = it },
                        label = "Комментарий",
                        placeholder = "Например: контрольная работа",
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GlassButton(
                            text = "Отмена",
                            onClick = { editing = null },
                            variant = GlassButtonVariant.Plain,
                            modifier = Modifier.weight(1f),
                        )
                        GlassButton(
                            text = "Сохранить",
                            onClick = {
                                Prefs.setMarkNote(ctx, subject.subjectId, mark.lessonId, editText)
                                editing = null
                                notesVersion++
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MarkDetailRow(
    mark: LessonMarks,
    dayText: String,
    note: String?,
    editable: Boolean,
    onEdit: () -> Unit,
) {
    val colors = Glass.colors
    GlassCard(elevated = false, contentPadding = PaddingValues(12.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            MarkBadge(mark = mark.mark, size = 40.dp)
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = dayText,
                        color = colors.text,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    if (!mark.theme.isNullOrBlank()) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = mark.theme,
                            color = colors.textSecondary,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (note.isNullOrBlank()) "Нет информации" else "За что: $note",
                    color = if (note.isNullOrBlank()) colors.textTertiary else colors.textSecondary,
                    fontSize = 12.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (editable) {
                Spacer(Modifier.width(6.dp))
                GlassIconButton(
                    icon = Icons.Rounded.Edit,
                    contentDescription = "Изменить, за что оценка",
                    onClick = onEdit,
                    size = 34.dp,
                )
            }
        }
    }
}
