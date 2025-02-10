// data/models/LocationResponses.kt
package com.example.gigwork.data.models

import com.google.gson.annotations.SerializedName

/**
 * Response model for states API
 */
data class StatesResponse(
    @SerializedName("states")
    val states: List<State>,
    @SerializedName("total")
    val total: Int,
    @SerializedName("success")
    val success: Boolean
)

data class State(
    @SerializedName("id")
    val id: Int,
    @SerializedName("name")
    val name: String,
    @SerializedName("capital")
    val capital: String?,
    @SerializedName("zone")
    val zone: String?
)

/**
 * Response model for districts API
 */
data class DistrictsResponse(
    @SerializedName("districts")
    val districts: List<District>,
    @SerializedName("state")
    val state: String,
    @SerializedName("total")
    val total: Int,
    @SerializedName("success")
    val success: Boolean
)

data class District(
    @SerializedName("id")
    val id: Int,
    @SerializedName("name")
    val name: String,
    @SerializedName("population")
    val population: Int?
)