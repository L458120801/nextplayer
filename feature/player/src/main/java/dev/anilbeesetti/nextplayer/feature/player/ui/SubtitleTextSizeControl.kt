package dev.anilbeesetti.nextplayer.feature.player.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.anilbeesetti.nextplayer.core.model.PlayerPreferences
import dev.anilbeesetti.nextplayer.core.ui.R
import kotlin.math.roundToInt

@Composable
internal fun SubtitleTextSizeControl(value: Int, enabled: Boolean, onValueChange: (Int) -> Unit) {
    val hapticFeedback = LocalHapticFeedback.current
    val minimumSize = PlayerPreferences.MIN_SUBTITLE_TEXT_SIZE.toFloat()
    val maximumSize = PlayerPreferences.MAX_SUBTITLE_TEXT_SIZE.toFloat()
    val valueRange = minimumSize..maximumSize

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.subtitle_text_size),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.titleMedium,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Slider(
                value = value.toFloat(),
                valueRange = valueRange,
                steps = PlayerPreferences.MAX_SUBTITLE_TEXT_SIZE -
                    PlayerPreferences.MIN_SUBTITLE_TEXT_SIZE - 1,
                onValueChange = { newValue ->
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onValueChange(
                        newValue.roundToInt().coerceIn(
                            PlayerPreferences.MIN_SUBTITLE_TEXT_SIZE,
                            PlayerPreferences.MAX_SUBTITLE_TEXT_SIZE,
                        ),
                    )
                },
                enabled = enabled,
                modifier = Modifier.weight(1f),
            )
            IconButton(
                onClick = {
                    onValueChange(PlayerPreferences.DEFAULT_SUBTITLE_TEXT_SIZE)
                },
                enabled = enabled,
                modifier = Modifier.padding(start = 8.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_reset),
                    contentDescription = stringResource(R.string.reset_subtitle_text_size),
                )
            }
        }
    }
}
