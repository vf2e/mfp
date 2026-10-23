package com.guotu.app.ui.swipe

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.guotu.app.R
import com.guotu.app.domain.model.MarkStatus
import com.guotu.app.ui.theme.GuotuButtonShape
import com.guotu.app.ui.theme.GuotuColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val KeepHeart = Color(0xFFFF2D55)
private val ChromeInk = Color.White
private val ChromeMute = Color.White.copy(alpha = 0.62f)
private val ControlFill = Color.Black.copy(alpha = 0.42f)

@Composable
fun SwipeIndexBar(
    modifier: Modifier = Modifier,
    indexLabel: String,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.55f),
                        Color.Transparent,
                    ),
                ),
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = indexLabel,
            color = ChromeMute,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(ControlFill)
                .padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}

@Composable
fun SwipeBottomChrome(
    modifier: Modifier = Modifier,
    fileName: String,
    markStatus: MarkStatus,
    keepBurstEpoch: Int = 0,
    onBack: () -> Unit,
    onKeep: () -> Unit,
    onPending: () -> Unit,
    onCut: () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.55f),
                        Color.Black.copy(alpha = 0.88f),
                    ),
                ),
            )
            .padding(start = 14.dp, end = 14.dp, top = 18.dp, bottom = 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            CircularChromeButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.action_back),
                onClick = onBack,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = fileName.ifBlank { "—" },
                    color = ChromeInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            MarkStatusChip(status = markStatus)
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MarkActionButton(
                icon = if (markStatus == MarkStatus.KEEP) {
                    Icons.Filled.Favorite
                } else {
                    Icons.Outlined.FavoriteBorder
                },
                label = stringResource(R.string.mark_keep),
                tint = if (markStatus == MarkStatus.KEEP) KeepHeart else ChromeInk,
                active = markStatus == MarkStatus.KEEP,
                activeBackground = KeepHeart.copy(alpha = 0.18f),
                burstEpoch = keepBurstEpoch,
                onClick = onKeep,
                modifier = Modifier.weight(1f),
            )
            MarkActionButton(
                icon = Icons.Outlined.Flag,
                label = stringResource(R.string.mark_pending),
                tint = if (markStatus == MarkStatus.PENDING) GuotuColors.Signal else ChromeInk,
                active = markStatus == MarkStatus.PENDING,
                activeBackground = GuotuColors.Signal.copy(alpha = 0.18f),
                onClick = onPending,
                modifier = Modifier.weight(1f),
            )
            MarkActionButton(
                icon = Icons.Filled.ContentCut,
                label = stringResource(R.string.action_cut),
                tint = ChromeInk,
                active = false,
                activeBackground = ControlFill,
                onClick = onCut,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun MarkStatusChip(status: MarkStatus) {
    when (status) {
        MarkStatus.KEEP -> {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(GuotuButtonShape)
                    .background(Color.Black.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = stringResource(R.string.mark_keep),
                    tint = KeepHeart,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        MarkStatus.PENDING -> StatusPill(
            text = stringResource(R.string.mark_pending),
            background = GuotuColors.SignalSoft,
            foreground = GuotuColors.Slate,
        )
        MarkStatus.MOVED -> StatusPill(
            text = stringResource(R.string.mark_moved),
            background = GuotuColors.Slate,
            foreground = Color.White,
        )
        MarkStatus.TO_DELETE, MarkStatus.UNSORTED -> StatusPill(
            text = stringResource(R.string.mark_to_delete),
            background = GuotuColors.Danger,
            foreground = Color.White,
        )
    }
}

@Composable
private fun StatusPill(
    text: String,
    background: Color,
    foreground: Color,
) {
    Text(
        text = text,
        modifier = Modifier
            .clip(GuotuButtonShape)
            .background(background)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        style = MaterialTheme.typography.labelSmall,
        color = foreground,
        fontWeight = FontWeight.Medium,
    )
}

@Composable
private fun MarkActionButton(
    icon: ImageVector,
    label: String,
    tint: Color,
    active: Boolean,
    activeBackground: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    burstEpoch: Int = 0,
) {
    val scale = remember { Animatable(1f) }
    LaunchedEffect(burstEpoch) {
        if (burstEpoch <= 0) return@LaunchedEffect
        scale.snapTo(1f)
        scale.animateTo(
            1.12f,
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium,
            ),
        )
        scale.animateTo(1f, tween(120))
    }
    Column(
        modifier = modifier
            .scale(scale.value)
            .clip(RoundedCornerShape(12.dp))
            .background(if (active) activeBackground else ControlFill)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(22.dp),
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = if (active) tint else ChromeMute,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
        )
    }
}

@Composable
private fun CircularChromeButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(ControlFill)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = ChromeInk,
            modifier = Modifier.size(20.dp),
        )
    }
}

/**
 * 双击/点收藏时的中心心形特效：放大弹入 → 停顿 → 淡出。
 */
@Composable
fun KeepBurstOverlay(
    epoch: Int,
    kept: Boolean,
    modifier: Modifier = Modifier,
) {
    if (epoch <= 0) return
    key(epoch) {
        val scale = remember { Animatable(0.35f) }
        val alpha = remember { Animatable(0f) }
        LaunchedEffect(epoch) {
            alpha.snapTo(0f)
            scale.snapTo(0.35f)
            launch {
                alpha.animateTo(1f, tween(90))
            }
            scale.animateTo(
                1.18f,
                spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                ),
            )
            scale.animateTo(1f, tween(120, easing = FastOutSlowInEasing))
            delay(520)
            alpha.animateTo(0f, tween(280, easing = FastOutSlowInEasing))
        }
        Column(
            modifier = modifier
                .scale(scale.value)
                .alpha(alpha.value),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.42f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (kept) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = null,
                    tint = if (kept) KeepHeart else Color.White.copy(alpha = 0.92f),
                    modifier = Modifier.size(52.dp),
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(
                    if (kept) R.string.keep_burst_on else R.string.keep_burst_off,
                ),
                color = Color.White,
                style = MaterialTheme.typography.titleMedium.copy(
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.55f),
                        blurRadius = 8f,
                    ),
                ),
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
