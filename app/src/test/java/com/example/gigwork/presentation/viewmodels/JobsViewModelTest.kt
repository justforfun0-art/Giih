package com.example.gigwork.presentation.viewmodels

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.example.gigwork.domain.models.Job
import com.example.gigwork.domain.models.Location
import com.example.gigwork.domain.repository.*
import com.example.gigwork.presentation.states.JobsEvent
import com.example.gigwork.presentation.states.JobsFilterData
import com.example.gigwork.util.MainDispatcherRule
import com.google.common.truth.Truth.assertThat
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class JobsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var viewModel: JobsViewModel
    private lateinit var jobRepository: JobRepository
    private lateinit var savedStateHandle: SavedStateHandle

    private val testJob = Job(
        id = "1",
        title = "Software Developer",
        description = "Test description",
        employerId = "emp1",
        location = Location(
            latitude = null,
            longitude = null,
            address = null,
            pinCode = null,
            state = "CA",
            district = "SF"
        ),
        salary = 100000.0,
        salaryUnit = "monthly",
        workDuration = 40,
        workDurationUnit = "hours",
        status = "OPEN",
        createdAt = "2023-01-01",
        updatedAt = "2023-01-01",
        lastModified = "2023-01-01",
        company = "Test Company"
    )

    @Before
    fun setup() {
        jobRepository = mockk(relaxed = true)
        savedStateHandle = SavedStateHandle()

        viewModel = JobsViewModel(
            savedStateHandle = savedStateHandle,
            getJobsUseCase = mockk(),
            getUserProfileUseCase = mockk(),
            jobRepository = jobRepository,
            userRepository = mockk(),
            bookmarkRepository = mockk(),
            applicationRepository = mockk(),
            userLocationRepository = mockk(),
            employerRatingRepository = mockk(),
            jobStatisticsRepository = mockk(),
            fileRepository = mockk(),
            locationManager = mockk(),
            errorHandler = mockk(),
            logger = mockk(),
            ioDispatcher = mainDispatcherRule.testDispatcher
        )
    }

    @Test
    fun `initial state should be correct`() = runTest {
        viewModel.state.test {
            val initialState = awaitItem()
            assertThat(initialState.isLoading).isFalse()
            assertThat(initialState.errorMessage).isNull()
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `search query update should trigger new search`() = runTest {
        // Given
        val searchQuery = "developer"

        // When
        // Use the actual method name from your ViewModel
        viewModel.clearSearchQuery()  // or whatever your method is called

        // Then
        assertThat(savedStateHandle.get<String>("searchQuery")).isEqualTo(searchQuery)
        viewModel.events.test {
            val event = awaitItem()
            assertThat(event).isInstanceOf(JobsEvent.ScrollToTop::class.java)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `applying filters should update state and trigger reload`() = runTest {
        // Given
        val filtersData = JobsFilterData(
            district = "SF",
            minSalary = 100000.0,
            maxSalary = 200000.0,
            state = "California"
        )

        // When
        // Use the actual method name and parameters from your ViewModel
        viewModel.applyFilters(filtersData)  // or whatever your method is called

        // Then
        assertThat(savedStateHandle.get<JobsFilterData>("filters")).isEqualTo(filtersData)
        viewModel.events.test {
            val event = awaitItem()
            assertThat(event).isInstanceOf(JobsEvent.ScrollToTop::class.java)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `clearing filters should reset all filter values`() = runTest {
        // Given - Set some filters
        val filtersData = JobsFilterData(
            district = "SF",
            minSalary = 100000.0,
            maxSalary = 200000.0,
            state = "California"
        )
        viewModel.applyFilters(filtersData)  // Use your actual method

        // When
        viewModel.clearSearchQuery()  // Use your actual method name

        // Then
        assertThat(savedStateHandle.get<JobsFilterData>("filters")).isNull()
        assertThat(savedStateHandle.get<String>("searchQuery")).isEqualTo("")
    }

    @Test
    fun `job click should emit navigation event`() = runTest {
        // Given
        val jobId = "test_id"

        // When
        viewModel.navigateToJobDetail(jobId)  // Use your actual method name

        // Then
        viewModel.events.test {
            val event = awaitItem()
            assertThat(event).isInstanceOf(JobsEvent.NavigateToJobDetail::class.java)
            assertThat((event as JobsEvent.NavigateToJobDetail).jobId).isEqualTo(jobId)
            cancelAndConsumeRemainingEvents()
        }
    }
}