package com.example.gigwork.data.repository

import android.content.Context
import app.cash.turbine.test
import com.example.gigwork.data.api.LocationService
import com.example.gigwork.data.api.exceptions.LocationApiException
import com.example.gigwork.data.cache.LocationCache
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject
import com.google.common.truth.Truth.assertThat

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
class LocationRepositoryTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    private lateinit var locationService: LocationService
    private lateinit var locationCache: LocationCache
    private lateinit var repository: LocationRepositoryImpl

    @Before
    fun setup() {
        locationService = mockk()
        locationCache = mockk()
        repository = LocationRepositoryImpl(locationService)
    }

    @Test
    fun getStates_withCacheHit_returnsFromCache() = runTest {
        // Given
        val cachedStates = listOf("California", "New York")
        coEvery { locationCache.get("states") } returns cachedStates

        // When
        repository.getStates().test {
            // Then
            val loadingState = awaitItem()
            assertThat(loadingState).isInstanceOf(Result.Loading::class.java)

            val successState = awaitItem()
            assertThat(successState).isInstanceOf(Result.Success::class.java)
            assertThat((successState as Result.Success).data).isEqualTo(cachedStates)

            cancelAndConsumeRemainingEvents()
        }

        // Verify cache was checked
        coVerify { locationCache.get("states") }
        // Verify API was not called
        coVerify(exactly = 0) { locationService.getStatesRaw() }
    }

    @Test
    fun getStates_withCacheMissAndApiSuccess_cachesAndReturns() = runTest {
        // Given
        val states = listOf("California", "New York")
        coEvery { locationCache.get("states") } returns null
        coEvery { locationService.getStatesRaw() } returns mockk {
            every { isSuccessful } returns true
            every { body() } returns states
        }
        coEvery { locationCache.put("states", states) } returns Unit

        // When
        repository.getStates().test {
            // Then
            val loadingState = awaitItem()
            assertThat(loadingState).isInstanceOf(Result.Loading::class.java)

            val successState = awaitItem()
            assertThat(successState).isInstanceOf(Result.Success::class.java)
            assertThat((successState as Result.Success).data).isEqualTo(states)

            cancelAndConsumeRemainingEvents()
        }

        // Verify cache was checked and updated
        coVerify { locationCache.get("states") }
        coVerify { locationCache.put("states", states) }
    }

    @Test
    fun getDistricts_withInvalidState_returnsError() = runTest {
        // Given
        val invalidState = "Invalid"
        coEvery { locationService.getDistrictsRaw(invalidState) } throws
                LocationApiException.InvalidStateException(invalidState)

        // When
        repository.getDistricts(invalidState).test {
            // Then
            val loadingState = awaitItem()
            assertThat(loadingState).isInstanceOf(Result.Loading::class.java)

            val errorState = awaitItem()
            assertThat(errorState).isInstanceOf(Result.Error::class.java)
            assertThat((errorState as Result.Error).exception)
                .isInstanceOf(LocationApiException.InvalidStateException::class.java)

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun getDistricts_withRateLimit_returnsError() = runTest {
        // Given
        val state = "California"
        coEvery { locationService.getDistrictsRaw(state) } throws
                LocationApiException.RateLimitException()

        // When
        repository.getDistricts(state).test {
            // Then
            val loadingState = awaitItem()
            assertThat(loadingState).isInstanceOf(Result.Loading::class.java)

            val errorState = awaitItem()
            assertThat(errorState).isInstanceOf(Result.Error::class.java)
            assertThat((errorState as Result.Error).exception)
                .isInstanceOf(LocationApiException.RateLimitException::class.java)

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun getDistricts_withSuccess_returnsSortedDistricts() = runTest {
        // Given
        val state = "California"
        val districts = listOf("San Francisco", "Los Angeles", "San Diego")
        val sortedDistricts = districts.sorted()

        coEvery { locationService.getDistrictsRaw(state) } returns mockk {
            every { isSuccessful } returns true
            every { body() } returns districts
        }

        // When
        repository.getDistricts(state).test {
            // Then
            val loadingState = awaitItem()
            assertThat(loadingState).isInstanceOf(Result.Loading::class.java)

            val successState = awaitItem()
            assertThat(successState).isInstanceOf(Result.Success::class.java)
            assertThat((successState as Result.Success).data).isEqualTo(sortedDistricts)

            cancelAndConsumeRemainingEvents()
        }
    }
}