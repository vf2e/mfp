package com.guotu.app.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.guotu.app.core.util.StorageAccessHelper
import com.guotu.app.ui.theme.GuotuButtonShape
import com.guotu.app.ui.theme.GuotuCardShape
import com.guotu.app.ui.theme.GuotuColors
import com.guotu.app.ui.theme.GuotuDimens
import java.io.File

/**
 * 全张文件访问权限下的本地目录浏览：可进入 Android/ 等 DocumentsUI 通常拦掉的路径。
 */
@Composable
fun FileFolderPickerDialog(
    onDismiss: () -> Unit,
    onConfirm: (absolutePath: String) -> Unit,
    initialDir: File = StorageAccessHelper.androidDir().takeIf { it.exists() }
        ?: StorageAccessHelper.primaryStorageDir(),
) {
    var current by remember { mutableStateOf(initialDir.canonicalFile) }
    var children by remember { mutableStateOf<List<File>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(current) {
        val listed = runCatching {
            current.listFiles()
                ?.filter { it.isDirectory && !it.name.startsWith(".") }
                ?.sortedBy { it.name.lowercase() }
                .orEmpty()
        }.getOrElse {
            error = "无法读取此目录"
            emptyList()
        }
        children = listed
        error = if (!current.canRead()) "无法读取此目录" else null
    }

    val storageRoot = remember { StorageAccessHelper.primaryStorageDir().canonicalFile }
    val canGoUp = current.absolutePath != storageRoot.absolutePath &&
        current.absolutePath.startsWith(storageRoot.absolutePath)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.82f)
                .clip(GuotuCardShape)
                .background(GuotuColors.Panel)
                .padding(GuotuDimens.Module),
        ) {
            Text(
                text = "选择扫描目录",
                style = MaterialTheme.typography.titleLarge,
                color = GuotuColors.Ink,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = StorageAccessHelper.displayPathForFile(current),
                style = MaterialTheme.typography.bodySmall,
                color = GuotuColors.InkMute,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(GuotuDimens.ListGap))

            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                StorageAccessHelper.shortcutDirs().forEach { (label, dir) ->
                    val selected = dir.canonicalFile == current
                    Text(
                        text = label,
                        Modifier
                            .clip(GuotuButtonShape)
                            .background(
                                if (selected) GuotuColors.SignalSoft else GuotuColors.Canvas,
                            )
                            .clickable { current = dir.canonicalFile }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (selected) GuotuColors.Signal else GuotuColors.Ink,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    )
                }
            }

            Spacer(Modifier.height(GuotuDimens.ListGap))

            if (canGoUp) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(GuotuButtonShape)
                        .clickable {
                            current.parentFile?.canonicalFile?.let { parent ->
                                if (parent.absolutePath.startsWith(storageRoot.absolutePath)) {
                                    current = parent
                                }
                            }
                        }
                        .padding(vertical = 10.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.FolderOpen,
                        contentDescription = null,
                        tint = GuotuColors.InkMute,
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "上级目录",
                        style = MaterialTheme.typography.bodyLarge,
                        color = GuotuColors.Ink,
                    )
                }
            }

            when {
                error != null -> {
                    Text(
                        text = error.orEmpty(),
                        Modifier
                            .weight(1f)
                            .padding(vertical = GuotuDimens.Module),
                        style = MaterialTheme.typography.bodyMedium,
                        color = GuotuColors.Danger,
                    )
                }
                children.isEmpty() -> {
                    Text(
                        text = "此目录下没有子文件夹，可直接选用当前目录",
                        Modifier
                            .weight(1f)
                            .padding(vertical = GuotuDimens.Module),
                        style = MaterialTheme.typography.bodyMedium,
                        color = GuotuColors.InkMute,
                    )
                }
                else -> {
                    LazyColumn(
                        Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = 4.dp),
                    ) {
                        items(children, key = { it.absolutePath }) { dir ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(GuotuButtonShape)
                                    .clickable { current = dir.canonicalFile }
                                    .padding(vertical = 12.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Folder,
                                    contentDescription = null,
                                    tint = GuotuColors.Signal,
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = dir.name,
                                    Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = GuotuColors.Ink,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Icon(
                                    imageVector = Icons.Outlined.ChevronRight,
                                    contentDescription = null,
                                    tint = GuotuColors.InkMute,
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(GuotuDimens.ListGap))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "取消",
                    Modifier
                        .weight(1f)
                        .clip(GuotuButtonShape)
                        .background(GuotuColors.Canvas)
                        .clickable(onClick = onDismiss)
                        .padding(vertical = 14.dp),
                    style = MaterialTheme.typography.titleMedium,
                    color = GuotuColors.Ink,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = "使用此目录",
                    Modifier
                        .weight(1.4f)
                        .clip(GuotuButtonShape)
                        .background(GuotuColors.Signal)
                        .clickable { onConfirm(current.absolutePath) }
                        .padding(vertical = 14.dp),
                    style = MaterialTheme.typography.titleMedium,
                    color = GuotuColors.OnSignal,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
