package com.example.gigwork.presentation.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.paging.PagingData
import app.cash.turbine.test
import com.example.gigwork.domain.models.Job
import com.example.gigwork.domain.repository.JobRepository
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class JobsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var viewModel: JobsViewModel
    private lateinit var repository: JobRepository
    private lateinit var savedStateHandle: SavedStateHandle

    private val testJob = Job(
        id = "1",
        title = "Software Developer",
        description = "Test description",
        employerId = "emp1",
        location = Location("CA", "SF"),
        salary = 100000.0,
        salaryUnit = "monthly",
        workDuration = 40,
        workDurationUnit = "hours",
        status = "OPEN",
        createdAt = "2023-01-01"
    )

    @Before
    fun setup() {
        repository = mockk(relaxed = true)
        savedStateHandle = SavedStateHandle()
        viewModel = JobsViewModel(repository, savedStateHandle)
    }

    @Test
    fun `initial state should be correct`() = runTest {
        viewModel.uiState.test {
            val initialState = awaitItem()
            assertThat(initialState.isLoading).isFalse()
            assertThat(initialState.error).isNull()
            assertThat(initialState.isRefreshing).isFalse()
        }
    }

    @Test
    fun `search query update should trigger new search`() = runTest {
        // Given
        val searchQuery = "developer"

        // When
        viewModel.updateSearchQuery(searchQuery)

        // Then
        assertThat(savedStateHandle.get<String>("searchQuery")).isEqualTo(searchQuery)
        viewModel.events.test {
            assertThat(awaitItem()).isInstanceOf(JobsEvent.ScrollToTop::class.java)
        }
    }

    @Test
    fun `applying filters should update state and trigger reload`() = runTest {
        // Given
        val state = "California"
        val district = "San Francisco"
        val minSalary = 100000.0

        // When
        viewModel.applyFilters(
            state = state,
            district = district,
            minSalary = minSalary
        )

        // Then
        assertThat(savedStateHandle.get<String>("selectedState")).isEqualTo(state)
        assertThat(savedStateHandle.get<String>("selectedDistrict")).isEqualTo(district)
        assertThat(savedStateHandle.get<Double>("minSalary")).isEqualTo(minSalary)
    }

    @Test
    fun `clearing filters should reset all filter values`() = runTest {
        // Given - Set some filters
        viewModel.applyFilters(
            state = "California",
            district = "SF",
            minSalary = 100000.0
        )

        // When
        viewModel.clearFilters()

        // Then
        assertThat(savedStateHandle.get<String>("selectedState")).isNull()
        assertThat(savedStateHandle.get<String>("selectedDistrict")).isNull()
        assertThat(savedStateHandle.get<Double>("minSalary")).isNull()
        assertThat(savedStateHandle.get<String>("searchQuery")).isEmpty()
    }

    @Test
    fun `refresh should update loading state`() = runTest {
        // When
        viewModel.refresh()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state.isRefreshing).isTrue()
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `error should emit snackbar event`() = runTest {
        // Given
        val errorMessage = "Test error"

        // When
        viewModel.showError(errorMessage)

        // Then
        viewModel.events.test {
            val event = awaitItem()
            assertThat(event).isInstanceOf(JobsEvent.ShowSnackbar::class.java)
            assertThat((event as JobsEvent.ShowSnackbar).message).isEqualTo(errorMessage)
        }
    }

    @Test
    fun `job click should emit navigation event`() = runTest {
        // Given
        val jobId = "test_id"

        // When
        viewModel.onJobClicked(jobId)

        // Then
        viewModel.events.test {
            val event = awaitItem()
            assertThat(event).isInstanceOf(JobsEvent.NavigateToJobDetail::class.java)
            assertThat((event as JobsEvent.NavigateToJobDetail).jobId).isEqualTo(jobId)
        }
    }
}