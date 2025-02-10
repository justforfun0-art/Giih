package com.example.gigwork.data.api

sealed class LocationApiException(message: String) : Exception(message) {
    class RateLimitException : LocationApiException("Rate limit exceeded. Please try again later.")
    class NetworkException(message: String) : LocationApiException(message)
    class CacheException(message: String) : LocationApiException(message)
    class InvalidStateException(state: String) : LocationApiException("Invalid state: $state")
    class ServiceUnavailableException : LocationApiException("Location service is temporarily unavailable")
    class NoDataException : LocationApiException("No data available")
}