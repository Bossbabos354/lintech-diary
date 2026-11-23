package ru.school28.diary.ui.screens

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.school28.diary.Prefs
import ru.school28.diary.api.ApiClient
import ru.school28.diary.api.SubjectMarks
import ru.school28.diary.glass.Glass
import ru.school28.diary.glass.GlassBadge
import ru.school28.diary.glass.GlassCard
import ru.school28.diary.glass.GlassEmptyState
import ru.school28.diary.glass.GlassErrorState
import ru.school28.diary.glass.GlassScreenScaffold
import ru.school28.diary.glass.GlassSpinner
import ru.school28.diary.glass.GradientAvatar
import ru.school28.diary.glass.StaggeredAppear
import ru.school28.diary.ui.CriticalSubject
import ru.school28.diary.ui.CriticalSubjects

@Composable
fun CriticalSubjectsScreen(
    onBack: () -> Unit,
    onPick: (SubjectMarks) -> Unit,
) {
    val ctx = LocalContext.current
    val colors = Glass.colors
    var subjects by remember { mutableStateOf<List<SubjectMarks>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    val seenAppear = remember { mutableSetOf<Any>() }
    var error by remember { mutableStateOf<String?>(null) }
    var refresh by remember { mutableStateOf(0) }

    LaunchedEffect(refresh) {
        loading = true
        error = null
        try {
            val studentId = Prefs.getStudentId(ctx)
            val uchYear = Prefs.getUchYear(ctx)
            if (studentId != -1) {
                var classId = Prefs.getClassId(ctx)
                if (classId == -1) {
                    classId = withContext(Dispatchers.IO) {
                        ApiClient.getStudentClass(studentId, uchYear)
                    }
                    Prefs.setClassId(ctx, classId)
                }
                subjects = withContext(Dispatchers.IO) {
                    ApiClient.getMarks(classId, studentId, uchYear)
                }
            }
        } catch (e: Exception) {
            error = e.message
        }
        loading = false
    }

    val critical = remember(subjects) { CriticalSubjects.find(subjects) }
    val byId = remember(subjects) { subjects.associateBy { it.subjectId } }

    GlassScreenScaffold(
        title = "Критические предметы",
        subtitle = if (critical.isEmpty()) "Всё под контролем" else "${critical.size} требуют внимания",
        onBack = onBack,
    ) {
        when {
            loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                GlassSpinner()
            }

            error != null -> LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
            ) {
                item {
                    GlassCard { GlassErrorState(message = error, onRetry = { refresh++ }) }
                }
            }

            critical.isEmpty() -> LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
            ) {
                item {
                    GlassCard {
                        GlassEmptyState(
                            icon = Icons.Rounded.CheckCircle,
                            title = "Критических предметов нет",
                            message = "Предмет попадает сюда, если средний балл ниже " +
                                CriticalSubjects.AVG_LIMIT.toString().replace('.', ',') +
                                " или набралось " + CriticalSubjects.TWOS_LIMIT + " и больше «2».",
                        )
                    }
                }
            }

            else -> LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                item(key = "intro") {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        GlassBadge("${critical.size} предметов", tint = colors.danger)
                        Spacer(Modifier.weight(1f))
                    }
                }
                itemsIndexed(critical, key = { _, item -> item.subjectId }) { index, item ->
                    StaggeredAppear(
                            index = index,
                            appearKey = item.subjectId,
                            seen = seenAppear,
                        ) {
                        CriticalRow(
                            item = item,
                            onClick = { byId[item.subjectId]?.let(onPick) },
                        )
                    }
                }
                item(key = "hint") {
                    Text(
                        text = "Нажми на предмет, чтобы сразу поставить цель и понять, сколько оценок нужно.",
                        color = colors.textSecondary,
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        modifier = Modifier.padding(start = 6.dp, end = 6.dp, top = 2.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun CriticalRow(item: CriticalSubject, onClick: () -> Unit) {
    val colors = Glass.colors
    GlassCard(onClick = onClick, contentPadding = PaddingValues(14.dp), elevated = false) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GradientAvatar(
                text = item.subjectName.take(2),
                subjectId = item.subjectId,
                size = 42.dp,
            )
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = item.subjectName,
                    color = colors.text,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = item.reasons.joinToString(" · "),
                    color = colors.danger,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "всего оценок: ${item.totalMarks}",
                    color = colors.textTertiary,
                    fontSize = 10.sp,
                )
            }
            Spacer(Modifier.width(8.dp))
            GlassBadge("Цель →", tint = colors.accent)
            Spacer(Modifier.width(2.dp))
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = colors.textTertiary,
                modifier = Modifier.size(17.dp),
            )
        }
    }
}
