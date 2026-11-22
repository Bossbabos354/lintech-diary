package ru.school28.diary.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalScrollbarStyle
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/** Плавная прокрутка колесом: вместо шага — короткая анимация. */
@OptIn(ExperimentalComposeUiApi::class)
fun Modifier.smoothScroll(state: ScrollableState): Modifier = composed {
    val scope = rememberCoroutineScope()
    val step = with(LocalDensity.current) { 120.dp.toPx() }
    onPointerEvent(PointerEventType.Scroll, pass = PointerEventPass.Initial) { event ->
        val delta = event.changes.firstOrNull()?.scrollDelta?.y ?: 0f
        if (delta != 0f) {
            event.changes.forEach { it.consume() }
            scope.launch {
                state.animateScrollBy(delta * step, tween(190, easing = FastOutSlowInEasing))
            }
        }
    }
}

@Composable
fun SmoothLazyColumn(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: LazyListScope.() -> Unit,
) {
    val listState = rememberLazyListState()
    Box(modifier) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().smoothScroll(listState),
            state = listState,
            contentPadding = contentPadding,
            verticalArrangement = verticalArrangement,
            content = content,
        )
        VerticalScrollbar(
            adapter = rememberScrollbarAdapter(listState),
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().padding(vertical = 8.dp),
            style = LocalScrollbarStyle.current.copy(
                unhoverColor = AppTheme.colors.primary.copy(alpha = 0.25f),
                hoverColor = AppTheme.colors.primary.copy(alpha = 0.6f),
            ),
        )
    }
}
