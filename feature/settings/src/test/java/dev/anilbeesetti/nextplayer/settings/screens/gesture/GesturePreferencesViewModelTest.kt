package dev.anilbeesetti.nextplayer.settings.screens.gesture

import dev.anilbeesetti.nextplayer.core.data.repository.fake.FakePreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GesturePreferencesViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository = FakePreferencesRepository()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `long press background opacity is persisted within slider bounds`() =
        runTest(context = dispatcher) {
            val viewModel = GesturePreferencesViewModel(
                preferencesRepository = repository,
                GesturePreferencesViewModel.Output(navigateUp = {}),
            )

            listOf(
                -0.5f to 0f,
                0.42f to 0.42f,
                1.5f to 1f,
            ).forEach { (input, expected) ->
                viewModel.onAction(
                    GesturePreferencesUiEvent.UpdateLongPressBackgroundOpacity(input),
                )
                advanceUntilIdle()

                assertEquals(
                    expected,
                    repository.playerPreferences.value.longPressBackgroundOpacity,
                )
                assertEquals(
                    expected,
                    viewModel.state.value.preferences.longPressBackgroundOpacity,
                )
            }
        }
}
