package com.example.gigwork.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.example.gigwork.data.api.SupabaseClient
import com.example.gigwork.data.mappers.toDomain
import com.example.gigwork.domain.models.Job
import io.github.jan.supabase.postgrest.query.FilterOperator
import javax.inject.Inject

class JobsPagingSource @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val state: String? = null,
    private val district: String? = null,
    private val minSalary: Double? = null,
    private val maxSalary: Double? = null,
    private val searchQuery: String? = null
) : PagingSource<Int, Job>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Job> {
        try {
            val page = params.key ?: 1
            val pageSize = params.loadSize

            val query = supabaseClient.client.postgrest["jobs"]
                .select {
                    // Apply filters
                    state?.let {
                        filter("location->>'state'", FilterOperator.EQ, it)
                    }
                    district?.let {
                        filter("location->>'district'", FilterOperator.EQ, it)
                    }
                    minSalary?.let {
                        filter("salary", FilterOperator.GTE, it)
                    }
                    maxSalary?.let {
                        filter("salary", FilterOperator.LTE, it)
                    }
                    searchQuery?.let {
                        or {
                            filter("title", FilterOperator.ILIKE, "%$it%")
                            filter("description", FilterOperator.ILIKE, "%$it%")
                        }
                    }

                    // Add pagination
                    limit(pageSize)
                    offset((page - 1) * pageSize)
                    order("created_at", ascending = false)
                }

            val jobs = query.decodeList<com.example.gigwork.data.models.JobDto>()
                .map { it.toDomain() }

            return LoadResult.Page(
                data = jobs,
                prevKey = if (page == 1) null else page - 1,
                nextKey = if (jobs.isEmpty()) null else page + 1
            )
        } catch (e: Exception) {
            return LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, Job>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }
}