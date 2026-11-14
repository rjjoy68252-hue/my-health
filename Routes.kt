package com.example.navigation

import kotlinx.serialization.Serializable

@Serializable
object TodayRoute

@Serializable
object HealthRoute

@Serializable
object WorkoutsRoute

@Serializable
data class MetricDetailRoute(val metricId: String)

@Serializable
object ProfileSettingsRoute
