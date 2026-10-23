package com.guotu.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.guotu.app.ui.theme.GuotuColors
import com.guotu.app.ui.theme.GuotuControlShape
import com.guotu.app.ui.theme.GuotuDimens

/**
 * Full-width segmented rail. Selected = primary soft fill.
 */
@Composable
fun <T> GuotuSegmentedRail(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
            .clip(GuotuControlShape)
            .border(GuotuDimens.Hairline, GuotuColors.Rule, GuotuControlShape)
            .background(GuotuColors.Canvas),
    ) {
        options.forEachIndexed { index, (value, label) ->
            val on = value == selected
            if (index > 0) {
                Box(
                    modifier = Modifier
                        .width(GuotuDimens.Hairline)
                        .fillMaxHeight()
                        .background(GuotuColors.Rule),
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(if (on) GuotuColors.SignalSoft else GuotuColors.Canvas)
                    .clickable { onSelect(value) }
                    .padding(horizontal = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (on) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (on) GuotuColors.Signal else GuotuColors.InkMute,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/**
 * Discrete 1–3 picker: numbers as equal cells, optional captions under.
 */
@Composable
fun GuotuStepPicker(
    value: Int,
    range: IntRange,
    onSelect: (Int) -> Unit,
    captions: Map<Int, String> = emptyMap(),
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(GuotuControlShape)
                .border(GuotuDimens.Hairline, GuotuColors.Rule, GuotuControlShape),
        ) {
            range.forEachIndexed { index, step ->
                val on = step == value
                if (index > 0) {
                    Box(
                        modifier = Modifier
                            .width(GuotuDimens.Hairline)
                            .fillMaxHeight()
                            .background(GuotuColors.Rule),
                    )
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(if (on) GuotuColors.SignalSoft else GuotuColors.Panel)
                        .clickable { onSelect(step) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = step.toString(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (on) GuotuColors.Signal else GuotuColors.Ink,
                    )
                }
            }
        }
        if (captions.isNotEmpty()) {
            Spacer(modifier = Modifier.height(GuotuDimens.Grid))
            Row(modifier = Modifier.fillMaxWidth()) {
                range.forEach { step ->
                    Text(
                        text = captions[step].orEmpty(),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (step == value) GuotuColors.Signal else GuotuColors.InkMute,
                        fontWeight = if (step == value) FontWeight.SemiBold else FontWeight.Normal,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
fun GuotuHairline(
    startInset: Boolean = true,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = if (startInset) GuotuDimens.Page else 0.dp)
            .height(GuotuDimens.Hairline)
            .background(GuotuColors.Rule),
    )
}
