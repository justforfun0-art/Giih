package com.example.gigwork.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.example.gigwork.data.api.SupabaseClient
import com.example.gigwork.data.mappers.toDomain
import com.example.gigwork.domain.models.Job
import io.github.jan.supabase.postgrest.postgrest
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
            val offset = (page - 1) * pageSize

            // Create query with select
            val query = supabaseClient.client.postgrest
                .from("jobs")
                .select {
                    // Apply filters inside the select block
                    filter {
                        state?.let { eq("location_state", it) }
                        district?.let { eq("location_district", it) }
                        minSalary?.let { gte("salary", it) }
                        maxSalary?.let { lte("salary", it) }
                    }

                    // Apply search query if provided
                    searchQuery?.let { query ->
                        filter {
                            or {
                                ilike("title", "%$query%")
                                ilike("description", "%$query%")
                            }
                        }
                    }

                    // Pagination and ordering inside select block
                    //limit(pageSize.toLong())
                    range(offset.toLong(), (offset + pageSize).toLong() - 1)
                    order("created_at", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                }

            // Execute the query and map the results
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