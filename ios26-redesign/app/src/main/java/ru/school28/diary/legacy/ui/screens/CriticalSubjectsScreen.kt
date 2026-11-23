package ru.school28.diary.legacy.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.school28.diary.Prefs
import ru.school28.diary.api.ApiClient
import ru.school28.diary.api.SubjectMarks
import ru.school28.diary.legacy.ui.CriticalSubject
import ru.school28.diary.legacy.ui.CriticalSubjects
import ru.school28.diary.legacy.ui.subjectColor
import ru.school28.diary.ui_components.AppTheme
import ru.school28.diary.ui_components.components.Text
import ru.school28.diary.ui_components.components.progressindicators.CircularProgressIndicator

/**
 * Экран «Критические предметы»: предметы с низким средним баллом
 * или большим числом «2». Тап по предмету — сразу поставить цель.
 */
@Composable
fun CriticalSubjectsScreen(
    onBack: () -> Unit,
    onPick: (SubjectMarks) -> Unit
) {
    val ctx = LocalContext.current
    var subjects by remember { mutableStateOf<List<SubjectMarks>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
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

    Column(Modifier.fillMaxSize().background(AppTheme.colors.background)) {
        // Шапка экрана
        Row(
            Modifier
                .fillMaxWidth()
                .background(AppTheme.colors.primary)
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Назад",
                tint = AppTheme.colors.onPrimary,
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onBack() }
            )
            Spacer(Modifier.width(12.dp))
            Text(
                "Критические предметы",
                color = AppTheme.colors.onPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }

        when {
            loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            critical.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = AppTheme.colors.primary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Критических предметов нет",
                        color = AppTheme.colors.text,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Предмет попадает сюда, если средний балл ниже " +
                            CriticalSubjects.AVG_LIMIT.toString().replace('.', ',') +
                            " или набралось " + CriticalSubjects.TWOS_LIMIT + " и больше «2»",
                        color = AppTheme.colors.textSecondary,
                        fontSize = 13.sp
                    )
                }
            }

            else -> LazyColumn(Modifier.fillMaxSize().padding(12.dp)) {
                item {
                    Text(
                        "Стоит взяться в первую очередь: низкий средний балл или много «2».",
                        color = AppTheme.colors.textSecondary,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                    )
                }
                items(critical, key = { it.subjectId }) { item ->
                    CriticalRow(
                        item = item,
                        onClick = { byId[item.subjectId]?.let(onPick) }
                    )
                }
            }
        }

        if (error != null) {
            Text(
                "Не удалось загрузить оценки: $error",
                color = AppTheme.colors.error,
                fontSize = 12.sp,
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}

@Composable
private fun CriticalRow(item: CriticalSubject, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(AppTheme.colors.surface)
            .clickable { onClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(subjectColor(item.subjectId)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                item.subjectName.take(2),
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 14.sp
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                item.subjectName,
                color = AppTheme.colors.text,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                maxLines = 1
            )
            Spacer(Modifier.height(2.dp))
            Text(
                item.reasons.joinToString(" · "),
                color = AppTheme.colors.error,
                fontSize = 12.sp
            )
            Text(
                "всего оценок: ${item.totalMarks}",
                color = AppTheme.colors.textSecondary,
                fontSize = 11.sp
            )
        }
        Text(
            "Цель →",
            color = AppTheme.colors.primary,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp
        )
    }
}
