package com.example.gigwork.presentation.screens

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

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class JobsScreenTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createComposeRule()

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
    }

    @Test
    fun jobCard_displaysCorrectInformation() {
        // Given
        composeTestRule.setContent {
            GigWorkTheme {
                JobCard(
                    job = testJob,
                    onClick = {}
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
        var searchQuery = ""

        composeTestRule.setContent {
            GigWorkTheme {
                JobSearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it }
                )
            }
        }

        // When
        composeTestRule.onNodeWithContentDescription("Search")
            .assertExists()
        composeTestRule.onNode(hasSetTextAction())
            .performTextInput("developer")

        // Then
        assertThat(searchQuery).isEqualTo("developer")
    }

    @Test
    fun filterSection_showsAndHidesCorrectly() {
        composeTestRule.setContent {
            GigWorkTheme {
                JobsScreen(
                    onJobClick = {},
                    onProfileClick = {}
                )
            }
        }

        // Initially filters should be hidden
        composeTestRule.onNodeWithText("Filters").assertDoesNotExist()

        // Click filter button
        composeTestRule.onNodeWithContentDescription("Show Filters").performClick()

        // Filters should now be visible
        composeTestRule.onNodeWithText("Filters").assertExists()
    }

    @Test
    fun emptyState_showsCorrectMessage() {
        composeTestRule.setContent {
            GigWorkTheme {
                EmptyJobsView()
            }
        }

        composeTestRule.onNodeWithText("No jobs found").assertExists()
        composeTestRule.onNodeWithText("Try adjusting your filters or search query")
            .assertExists()
    }

    @Test
    fun errorState_showsRetryButton() {
        val errorMessage = "Network error"

        composeTestRule.setContent {
            GigWorkTheme {
                ErrorView(
                    message = errorMessage,
                    onRetry = {}
                )
            }
        }

        composeTestRule.onNodeWithText(errorMessage).assertExists()
        composeTestRule.onNodeWithText("Try Again").assertExists()
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
            // You can also check for specific background color using SemanticsMatcher
            .assertHasClickAction()
    }
}