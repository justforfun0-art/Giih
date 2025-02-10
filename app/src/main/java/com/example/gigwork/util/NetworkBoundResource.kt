// util/NetworkBoundResource.kt
package com.example.gigwork.util

import kotlinx.coroutines.flow.*

inline fun <ResultType, RequestType> networkBoundResource(
    crossinline query: () -> Flow<ResultType>,
    crossinline fetch: suspend () -> RequestType,
    crossinline saveFetchResult: suspend (RequestType) -> Unit,
    crossinline shouldFetch: (ResultType) -> Boolean = { true },
    crossinline onFetchError: (Throwable) -> Unit = { }
) = flow {
    val data = query().first()

    val flow = if (shouldFetch(data)) {
        emit(Result.Loading)

        try {
            val fetchedData = fetch()
            saveFetchResult(fetchedData)
            query().map { Result.Success(it) }
        } catch (t: Throwable) {
            onFetchError(t)
            query().map { Result.Success(it) }
        }
    } else {
        query().map { Result.Success(it) }
    }

    emitAll(flow)
}