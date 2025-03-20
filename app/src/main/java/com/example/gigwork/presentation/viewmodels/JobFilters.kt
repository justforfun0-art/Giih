package com.example.gigwork.presentation.viewmodels

data class JobFilters(
    val status: String? = null,
    val minSalary: Double? = null,
    val maxSalary: Double? = null,
    val location: String? = null
)