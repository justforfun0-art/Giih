package com.example.gigwork.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.example.gigwork.domain.models.Job
import com.example.gigwork.domain.repository.JobRepository

class JobsPagingSource(
    private val jobRepository: JobRepository,
    private val searchQuery: String? = null,
    private val state: String? = null,
    private val district: String? = null,
    private val minSalary: Double? = null,
    private val maxSalary: Double? = null,
    private val sortOrder: String = "newest"
) : PagingSource<Int, Job>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Job> {
        try {
            val page = params.key ?: 1
            val response = if (searchQuery != null) {
                // Handle search
                jobRepository.searchJobs(searchQuery)
            } else {
                // Handle regular job loading with filters
                jobRepository.getJobs(
                    page = page,
                    pageSize = params.loadSize,
                    state = state,
                    district = district,
                    minSalary = minSalary,
                    maxSalary = maxSalary
                )
            }

            return when (val result = response.first()) {
                is Result.Success -> {
                    val jobs = result.data
                    LoadResult.Page(
                        data = jobs,
                        prevKey = if (page == 1) null else page - 1,
                        nextKey = if (jobs.isEmpty()) null else page + 1
                    )
                }
                is Result.Error -> {
                    LoadResult.Error(result.exception)
                }
                is Result.Loading -> {
                    LoadResult.Error(Exception("Unexpected loading state"))
                }
            }
        } catch (e: Exception) {
            return LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, Job>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
        }
    }
}