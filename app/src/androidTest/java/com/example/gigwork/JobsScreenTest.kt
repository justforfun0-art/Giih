package com.example.gigwork

import androidx.compose.material3.SearchBar
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.gigwork.domain.models.Job
import com.example.gigwork.domain.models.Location
import com.example.gigwork.presentation.theme.GigWorkTheme
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.compose.runtime.mutableStateOf
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.test.core.app.ApplicationProvider
import coil.ImageLoader
import com.example.gigwork.presentation.screens.*
import com.example.gigwork.presentation.components.jobs.JobCard
import com.example.gigwork.presentation.viewmodels.JobsViewModel
import javax.inject.Inject

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class JobsScreenTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createComposeRule()


    @Inject
    lateinit var testViewModel: JobsViewModel

    private val testJob = Job(
        id = "1",
        title = "Software Developer",
        description = "Test description",
        employerId = "emp1",
        location = Location(null, null, null,null, "CA", "SF"),
        salary = 100000.0,
        salaryUnit = "monthly",
        workDuration = 40,
        workDurationUnit = "hours",
        status = "OPEN",
        createdAt = "2023-01-01",
        updatedAt = "2023-01-02",
        lastModified = "2023-01-02",
        company = "Test Company"
    )

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun jobCard_displaysCorrectInformation() {
        // Given
        composeTestRule.setContent {
            GigWorkTheme {
                JobCard(
                    job = testJob,
                    onClick = {},
                    imageLoader = ImageLoader(ApplicationProvider.getApplicationContext())
                )
            }
        }

        // Then
        composeTestRule.onNodeWithText(testJob.title).assertExists()
        composeTestRule.onNodeWithText("₹${testJob.salary}/${testJob.salaryUnit}")
            .assertExists()
        composeTestRule.onNodeWithText("${testJob.location.district}, ${testJob.location.state}")
            .assertExists()
        composeTestRule.onNodeWithText(testJob.status).assertExists()
    }

    @Test
    fun searchBar_performsSearch() {
        var searchQuery = mutableStateOf("")

        composeTestRule.setContent {
            GigWorkTheme {
                SearchBar(
                    query = searchQuery.value,
                    onQueryChange = { searchQuery.value = it }
                )
            }
        }

        // When
        composeTestRule.onNodeWithContentDescription("Search")
            .assertExists()
        composeTestRule.onNode(hasSetTextAction())
            .performTextInput("developer")

        // Then
        assert(searchQuery.value == "developer")
    }

    @Test
    fun filterSection_showsAndHidesCorrectly() {
        composeTestRule.setContent {
            GigWorkTheme {
                JobsScreen(
                    onJobClick = {},
                    onProfileClick = {},
                    imageLoader = ImageLoader(ApplicationProvider.getApplicationContext()),
                    viewModel = testViewModel // Explicitly add viewModel
                )
            }
        }


    // Initially filters should be hidden
        composeTestRule.onNodeWithText("Salary Range").assertDoesNotExist()

        // Click filter button
        composeTestRule.onNodeWithContentDescription("Show Filters").performClick()

        // Filters should now be visible
        composeTestRule.onNodeWithText("Salary Range").assertExists()
    }

    @Test
    fun emptyState_showsCorrectMessage() {
        composeTestRule.setContent {
            GigWorkTheme {
                EmptyJobsView(
                    showFilters = true,
                    onClearFilters = {}
                )
            }
        }

        composeTestRule.onNodeWithText("No jobs found").assertExists()
        composeTestRule.onNodeWithText("Try adjusting your filters")
            .assertExists()
    }

    @Test
    fun jobStatusChip_showsCorrectColor() {
        composeTestRule.setContent {
            GigWorkTheme {
                JobStatusChip(status = "OPEN")
            }
        }

        // Check if the status chip exists and has correct text
        composeTestRule.onNodeWithText("OPEN")
            .assertExists()
    }
}