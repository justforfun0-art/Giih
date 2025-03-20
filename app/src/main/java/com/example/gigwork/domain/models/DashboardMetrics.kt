package com.example.gigwork.domain.models



data class DashboardMetrics(
    val openJobsCount: Int = 0,
    val closedJobsCount: Int = 0,
    val totalJobsCount: Int = 0,
    val activeApplicantsCount: Int = 0,
    val totalApplicantsCount: Int = 0,
    val hiredCount: Int = 0,
    val averageResponseRate: Double = 0.0,
    val recentJobs: List<Job> = emptyList(),
    // Add these properties to match the metrics used in getMetricsItems
    val activeJobs: Int = 0,
    val totalApplications: Int = 0,
    val pendingApplications: Int = 0,
    val hiredCandidates: Int = 0
)