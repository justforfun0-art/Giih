package com.example.gigwork.data.repository

import app.cash.turbine.test
import com.example.gigwork.core.error.model.AppError
import com.example.gigwork.core.result.ApiResult
import com.example.gigwork.data.api.LocationService
import com.example.gigwork.data.api.LocationApiException
import com.example.gigwork.di.IoDispatcher
import com.example.gigwork.util.MainDispatcherRule
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject
import com.google.common.truth.Truth.assertThat

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
class LocationRepositoryTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val mainDispatcherRule = MainDispatcherRule()

    @Inject
    @IoDispatcher
    lateinit var ioDispatcher: CoroutineDispatcher

    private lateinit var locationService: LocationService
    private lateinit var repository: LocationRepositoryImpl
    private val testDispatcher: TestDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        hiltRule.inject()
        locationService = mockk()
        repository = LocationRepositoryImpl(locationService)
    }

    @Test
    fun getStatesReturnsSuccessWithSortedStates() = runTest {
        // Given
        val states = listOf("New York", "California", "Texas")
        val sortedStates = states.sorted()
        coEvery { locationService.getStates() } returns flowOf(states)

        // When
        repository.getStates().test {
            // Then
            val loading = awaitItem()
            assertThat(loading).isInstanceOf(ApiResult.Loading::class.java)

            val success = awaitItem()
            assertThat(success).isInstanceOf(ApiResult.Success::class.java)
            assertThat((success as ApiResult.Success<List<String>>).data).isEqualTo(sortedStates)

            awaitComplete()
        }

        // Verify service was called
        coVerify { locationService.getStates() }
    }

    @Test
    fun getStatesReturnsErrorOnServiceException() = runTest {
        // Given
        val exception = LocationApiException.RateLimitException()
        coEvery { locationService.getStates() } throws exception

        // When
        repository.getStates().test {
            // Then
            val loading = awaitItem()
            assertThat(loading).isInstanceOf(ApiResult.Loading::class.java)

            val error = awaitItem()
            assertThat(error).isInstanceOf(ApiResult.Error::class.java)
            assertThat((error as ApiResult.Error).error.message)
                .contains("Rate limit exceeded")

            awaitComplete()
        }

        coVerify { locationService.getStates() }
    }

    @Test
    fun getDistrictsReturnsSuccessWithSortedDistricts() = runTest {
        // Given
        val state = "California"
        val districts = listOf("San Francisco", "Los Angeles", "San Diego")
        val sortedDistricts = districts.sorted()
        coEvery { locationService.getDistricts(state) } returns flowOf(districts)

        // When
        repository.getDistricts(state).test {
            // Then
            val loading = awaitItem()
            assertThat(loading).isInstanceOf(ApiResult.Loading::class.java)

            val success = awaitItem()
            assertThat(success).isInstanceOf(ApiResult.Success::class.java)
            assertThat((success as ApiResult.Success<List<String>>).data).isEqualTo(sortedDistricts)

            awaitComplete()
        }

        coVerify { locationService.getDistricts(state) }
    }

    @Test
    fun getDistrictsReturnsErrorForInvalidState() = runTest {
        // Given
        val invalidState = "Invalid"
        val exception = LocationApiException.InvalidStateException(invalidState)
        coEvery { locationService.getDistricts(invalidState) } throws exception

        // When
        repository.getDistricts(invalidState).test {
            // Then
            val loading = awaitItem()
            assertThat(loading).isInstanceOf(ApiResult.Loading::class.java)

            val error = awaitItem()
            assertThat(error).isInstanceOf(ApiResult.Error::class.java)
            assertThat((error as ApiResult.Error).error.message)
                .contains("Invalid state")

            awaitComplete()
        }

        coVerify { locationService.getDistricts(invalidState) }
    }
}