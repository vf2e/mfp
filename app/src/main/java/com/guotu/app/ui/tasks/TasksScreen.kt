package com.guotu.app.ui.tasks

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DriveFileMove
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.guotu.app.R
import com.guotu.app.data.db.TaskEntity
import com.guotu.app.domain.model.TaskState
import com.guotu.app.domain.model.TaskType
import com.guotu.app.ui.components.GuotuEmptyState
import com.guotu.app.ui.components.GuotuPageHeader
import com.guotu.app.ui.theme.GuotuColors
import com.guotu.app.ui.theme.GuotuDimens
import com.guotu.app.ui.theme.guotuCard
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@Composable
fun TasksScreen(
    viewModel: TasksViewModel = hiltViewModel(),
) {
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    val active = remember(tasks) {
        tasks.filter {
            it.state == TaskState.RUNNING.name || it.state == TaskState.PENDING.name
        }
    }
    val history = remember(tasks) {
        tasks.filter {
            it.state != TaskState.RUNNING.name && it.state != TaskState.PENDING.name
        }
    }
    val runningCount = active.count { it.state == TaskState.RUNNING.name }
    val pendingCount = active.count { it.state == TaskState.PENDING.name }
    val successCount = history.count { it.state == TaskState.SUCCESS.name }
    val failedCount = history.count { it.state == TaskState.FAILED.name }
    val canClear = history.isNotEmpty()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GuotuColors.Canvas),
    ) {
        GuotuPageHeader(
            title = stringResource(R.string.nav_tasks),
            subtitle = stringResource(R.string.tasks_subtitle),
            actions = {
                if (canClear) {
                    TextButton(onClick = viewModel::clearFinished) {
                        Text(
                            text = stringResource(R.string.tasks_clear_finished),
                            color = GuotuColors.Signal,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            },
        )

        if (tasks.isEmpty()) {
            GuotuEmptyState(
                title = stringResource(R.string.tasks_empty_title),
                body = stringResource(R.string.tasks_empty),
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    horizontal = GuotuDimens.Page,
                    vertical = GuotuDimens.Page,
                ),
                verticalArrangement = Arrangement.spacedBy(GuotuDimens.ListGap),
            ) {
                item {
                    TaskSummaryRow(
                        running = runningCount,
                        pending = pendingCount,
                        success = successCount,
                        failed = failedCount,
                    )
                }
                if (active.isNotEmpty()) {
                    item {
                        SectionLabel(text = stringResource(R.string.tasks_section_active))
                    }
                    items(active, key = { "a-${it.id}" }) { task ->
                        TaskCard(task = task)
                    }
                }
                if (history.isNotEmpty()) {
                    item {
                        SectionLabel(text = stringResource(R.string.tasks_section_history))
                    }
                    items(history, key = { "h-${it.id}" }) { task ->
                        TaskCard(task = task)
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = GuotuColors.Ink,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(
            top = GuotuDimens.Grid,
            start = GuotuDimens.Grid,
            bottom = 4.dp,
        ),
    )
}

@Composable
private fun TaskSummaryRow(
    running: Int,
    pending: Int,
    success: Int,
    failed: Int,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .guotuCard()
            .padding(vertical = GuotuDimens.Page),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        SummaryCell(
            value = running.toString(),
            label = stringResource(R.string.task_state_running),
            accent = GuotuColors.Signal,
        )
        SummaryCell(
            value = pending.toString(),
            label = stringResource(R.string.task_state_pending),
            accent = GuotuColors.InkMute,
        )
        SummaryCell(
            value = success.toString(),
            label = stringResource(R.string.task_state_success),
            accent = GuotuColors.Ink,
        )
        SummaryCell(
            value = failed.toString(),
            label = stringResource(R.string.task_state_failed),
            accent = GuotuColors.Danger,
        )
    }
}

@Composable
private fun SummaryCell(
    value: String,
    label: String,
    accent: Color,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            color = accent,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = GuotuColors.InkMute,
        )
    }
}

@Composable
private fun TaskCard(task: TaskEntity) {
    val typeLabel = when (task.type) {
        TaskType.MOVE.name -> stringResource(R.string.task_type_move)
        TaskType.DELETE.name -> stringResource(R.string.task_type_delete)
        else -> task.type
    }
    val stateLabel = when (task.state) {
        TaskState.PENDING.name -> stringResource(R.string.task_state_pending)
        TaskState.RUNNING.name -> stringResource(R.string.task_state_running)
        TaskState.SUCCESS.name -> stringResource(R.string.task_state_success)
        TaskState.FAILED.name -> stringResource(R.string.task_state_failed)
        TaskState.CANCELLED.name -> stringResource(R.string.task_state_cancelled)
        else -> task.state
    }
    val stateColor = when (task.state) {
        TaskState.RUNNING.name -> GuotuColors.Signal
        TaskState.PENDING.name -> GuotuColors.InkMute
        TaskState.FAILED.name -> GuotuColors.Danger
        TaskState.SUCCESS.name -> GuotuColors.Ink
        else -> GuotuColors.InkMute
    }
    val typeIcon = when (task.type) {
        TaskType.DELETE.name -> Icons.Outlined.Delete
        else -> Icons.AutoMirrored.Outlined.DriveFileMove
    }
    val stateIcon: ImageVector = when (task.state) {
        TaskState.RUNNING.name -> Icons.Outlined.HourglassEmpty
        TaskState.PENDING.name -> Icons.Outlined.HourglassEmpty
        TaskState.SUCCESS.name -> Icons.Outlined.CheckCircle
        TaskState.FAILED.name -> Icons.Outlined.ErrorOutline
        else -> Icons.Outlined.CheckCircle
    }
    val progress = if (task.progressTotal > 0) {
        task.progressCurrent.toFloat() / task.progressTotal.toFloat()
    } else {
        0f
    }
    val isActive = task.state == TaskState.RUNNING.name || task.state == TaskState.PENDING.name
    val timeText = remember(task.createdAt, task.finishedAt) {
        formatRelativeTime(task.finishedAt ?: task.createdAt)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .guotuCard()
            .padding(GuotuDimens.Page),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(GuotuColors.Canvas),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = typeIcon,
                    contentDescription = null,
                    tint = GuotuColors.Signal,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(modifier = Modifier.width(GuotuDimens.ListGap))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = typeLabel,
                    style = MaterialTheme.typography.titleMedium,
                    color = GuotuColors.Ink,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(
                        R.string.task_progress,
                        task.progressCurrent,
                        task.progressTotal,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = GuotuColors.InkMute,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = stateIcon,
                    contentDescription = null,
                    tint = stateColor,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = stateLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = stateColor,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        if (isActive) {
            Spacer(modifier = Modifier.height(GuotuDimens.ListGap))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(GuotuColors.Canvas),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress.coerceIn(0.04f, 1f))
                        .height(6.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(GuotuColors.Signal),
                )
            }
        }

        task.errorSummary?.takeIf { it.isNotBlank() }?.let { summary ->
            Spacer(modifier = Modifier.height(GuotuDimens.Grid))
            Text(
                text = summary,
                style = MaterialTheme.typography.bodySmall,
                color = when (task.state) {
                    TaskState.FAILED.name -> GuotuColors.Danger
                    else -> GuotuColors.InkMute
                },
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(modifier = Modifier.height(GuotuDimens.Grid))
        Text(
            text = timeText,
            style = MaterialTheme.typography.labelSmall,
            color = GuotuColors.InkMute,
        )
    }
}

private fun formatRelativeTime(epochMs: Long): String {
    val now = System.currentTimeMillis()
    val diff = (now - epochMs).coerceAtLeast(0L)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
    val hours = TimeUnit.MILLISECONDS.toHours(diff)
    val days = TimeUnit.MILLISECONDS.toDays(diff)
    return when {
        minutes < 1 -> "刚刚"
        minutes < 60 -> "${minutes} 分钟前"
        hours < 24 -> "${hours} 小时前"
        days < 7 -> "${days} 天前"
        else -> SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(epochMs))
    }
}
