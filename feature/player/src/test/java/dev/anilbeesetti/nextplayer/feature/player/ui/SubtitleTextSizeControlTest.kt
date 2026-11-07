package dev.anilbeesetti.nextplayer.feature.player.ui

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import dev.anilbeesetti.nextplayer.core.model.PlayerPreferences
import dev.anilbeesetti.nextplayer.core.ui.R
import dev.anilbeesetti.nextplayer.core.ui.theme.NextPlayerTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SubtitleTextSizeControlTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun sliderShowsCurrentSizeAndUsesTheSubtitlePreferenceRange() {
        var value by mutableIntStateOf(PlayerPreferences.DEFAULT_SUBTITLE_TEXT_SIZE)
        val minimumSize = PlayerPreferences.MIN_SUBTITLE_TEXT_SIZE.toFloat()
        val maximumSize = PlayerPreferences.MAX_SUBTITLE_TEXT_SIZE.toFloat()
        composeRule.setContent {
            NextPlayerTheme {
                SubtitleTextSizeControl(
                    value = value,
                    enabled = true,
                    onValueChange = { value = it },
                )
            }
        }

        composeRule.onNodeWithText("20").assertExists()
        val slider = composeRule.onNode(
            SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo),
        )
        val rangeInfo = slider.fetchSemanticsNode().config[
            SemanticsProperties.ProgressBarRangeInfo,
        ]
        assertEquals(
            ProgressBarRangeInfo(
                current = 20f,
                range = minimumSize..maximumSize,
                steps = PlayerPreferences.MAX_SUBTITLE_TEXT_SIZE -
                    PlayerPreferences.MIN_SUBTITLE_TEXT_SIZE - 1,
            ),
            rangeInfo,
        )

        slider.performSemanticsAction(SemanticsActions.SetProgress) { setProgress ->
            assertTrue(setProgress(42f))
        }
        composeRule.runOnIdle {
            assertEquals(
                42,
                value,
            )
        }
    }

    @Test
    fun resetButtonRestoresTheDefaultSubtitleTextSize() {
        var value by mutableIntStateOf(37)
        composeRule.setContent {
            NextPlayerTheme {
                SubtitleTextSizeControl(
                    value = value,
                    enabled = true,
                    onValueChange = { value = it },
                )
            }
        }

        composeRule.onNodeWithContentDescription(
            composeRule.activity.getString(R.string.reset_subtitle_text_size),
        ).performClick()

        composeRule.runOnIdle {
            assertEquals(
                PlayerPreferences.DEFAULT_SUBTITLE_TEXT_SIZE,
                value,
            )
        }
    }

    @Test
    fun systemCaptionStyleDisablesSizeSliderAndResetButton() {
        composeRule.setContent {
            NextPlayerTheme {
                SubtitleTextSizeControl(
                    value = PlayerPreferences.DEFAULT_SUBTITLE_TEXT_SIZE,
                    enabled = false,
                    onValueChange = {},
                )
            }
        }

        composeRule.onNode(
            SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo),
        ).assertIsNotEnabled()
        composeRule.onNodeWithContentDescription(
            composeRule.activity.getString(R.string.reset_subtitle_text_size),
        ).assertIsNotEnabled()
    }
}
