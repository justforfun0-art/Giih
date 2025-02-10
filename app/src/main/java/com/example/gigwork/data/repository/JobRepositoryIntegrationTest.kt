package com.example.gigwork.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.example.gigwork.data.api.SupabaseClient
import com.example.gigwork.data.database.AppDatabase
import com.example.gigwork.data.database.dao.JobDao
import com.example.gigwork.data.models.JobDto
import com.example.gigwork.domain.models.Job
import com.example.gigwork.domain.models.Location
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject
import com.google.common.truth.Truth.assertThat

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
class JobRepositoryIntegrationTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var supabaseClient: SupabaseClient

    private lateinit var database: AppDatabase
    private lateinit var jobDao: JobDao
    private lateinit var repository: JobRepositoryImpl

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
        hiltRule.inject()

        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            AppDatabase::class.java
        ).allowMainThreadQueries().build()

        jobDao = database.jobDao()
        repository = JobRepositoryImpl(supabaseClient, jobDao, context)
    }

    @After
    fun cleanup() {
        database.close()
    }

    @Test
    fun getJobs_withNetworkSuccess_shouldCacheAndReturnJobs() = runTest {
        // Given
        val jobs = listOf(testJob)

        // When
        repository.getJobs().test {
            // Then
            val loadingState = awaitItem()
            assertThat(loadingState).isInstanceOf(Result.Loading::class.java)

            val successState = awaitItem()
            assertThat(successState).isInstanceOf(Result.Success::class.java)
            assertThat((successState as Result.Success).data).hasSize(1)
            assertThat(successState.data.first().id).isEqualTo(testJob.id)

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun searchJobs_shouldReturnFilteredResults() = runTest {
        // Given
        val searchQuery = "developer"

        // When
        repository.searchJobs(searchQuery).test {
            // Then
            val loadingState = awaitItem()
            assertThat(loadingState).isInstanceOf(Result.Loading::class.java)

            val successState = awaitItem()
            assertThat(successState).isInstanceOf(Result.Success::class.java)
            val jobs = (successState as Result.Success).data
            assertThat(jobs.all {
                it.title.contains(searchQuery, ignoreCase = true) ||
                        it.description.contains(searchQuery, ignoreCase = true)
            }).isTrue()

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun getJobById_whenJobExists_shouldReturnJob() = runTest {
        // Given
        val jobId = testJob.id

        // When
        repository.getJobById(jobId).test {
            // Then
            val loadingState = awaitItem()
            assertThat(loadingState).isInstanceOf(Result.Loading::class.java)

            val successState = awaitItem()
            assertThat(successState).isInstanceOf(Result.Success::class.java)
            assertThat((successState as Result.Success).data.id).isEqualTo(jobId)

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun getJobById_whenJobDoesNotExist_shouldReturnError() = runTest {
        // Given
        val nonExistentJobId = "invalid_id"

        // When
        repository.getJobById(nonExistentJobId).test {
            // Then
            val loadingState = awaitItem()
            assertThat(loadingState).isInstanceOf(Result.Loading::class.java)

            val errorState = awaitItem()
            assertThat(errorState).isInstanceOf(Result.Error::class.java)

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun createJob_shouldSaveAndReturnJob() = runTest {
        // When
        repository.createJob(testJob).test {
            // Then
            val loadingState = awaitItem()
            assertThat(loadingState).isInstanceOf(Result.Loading::class.java)

            val successState = awaitItem()
            assertThat(successState).isInstanceOf(Result.Success::class.java)
            assertThat((successState as Result.Success).data.title).isEqualTo(testJob.title)

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun updateJobStatus_shouldUpdateStatus() = runTest {
        // Given
        val newStatus = "CLOSED"

        // When
        repository.updateJobStatus(testJob.id, newStatus).test {
            // Then
            val loadingState = awaitItem()
            assertThat(loadingState).isInstanceOf(Result.Loading::class.java)

            val successState = awaitItem()
            assertThat(successState).isInstanceOf(Result.Success::class.java)
            assertThat((successState as Result.Success).data.status).isEqualTo(newStatus)

            cancelAndConsumeRemainingEvents()
        }
    }
}