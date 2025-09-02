package com.bituwy.wheyout.model

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.response.ReadRecordsResponse
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Instant
import java.time.LocalDateTime
import kotlin.time.Duration
import kotlin.time.toJavaDuration

const val TAG = "CaloriesTracker"
//TODO: Handle checking permissions
//TODO: Figure out why AOD doesn't work for this even though it works for HelloWorld Glyph
class CaloriesTracker(val context: Context) {
    val target = 1600.0

    suspend fun remaining(from: LocalDateTime): Double {
        val timeRage = TimeRangeFilter.after(from)

        return target - consumed(timeRage)
    }
    suspend fun remaining(forTheLast: Duration): Double {
        val temporalAmount = forTheLast.toJavaDuration()
        val startOfPeriod = Instant.now().minus(temporalAmount)
        val timeRange = TimeRangeFilter.after(startOfPeriod)

        return target - consumed(timeRange)
    }

    suspend fun consumed(between: TimeRangeFilter): Double {
        val nutritionRecords = fetchNutrition(between)
        return nutritionRecords.records.sumOf { it.energy?.inKilocalories ?: 0.0 }
    }

    suspend fun fetchNutrition(between: TimeRangeFilter): ReadRecordsResponse<NutritionRecord> {
        val healthConnectClient = getHealthConnectClient(context)
        return healthConnectClient.readRecords(
            ReadRecordsRequest(
                NutritionRecord::class,
                between
            )
        )
    }
}
fun getHealthConnectClient(context: Context): HealthConnectClient {
    val availabilityStatus = HealthConnectClient.getSdkStatus(context)

    if (availabilityStatus == HealthConnectClient.SDK_UNAVAILABLE) {
        Log.i(TAG, "SDK UNAVAILABLE")
    }

    val providerPackageName = "com.google.android.apps.healthdata"
    if (availabilityStatus == HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED) {
        val uriString = "market://details?id=$providerPackageName&url=healthconnect%3A%2F%2Fonboarding"
        context.startActivity(
            Intent(Intent.ACTION_VIEW).apply {
                setPackage("com.android.vending")
                data = Uri.parse(uriString)
                putExtra("overlay", true)
                putExtra("callerId", context.packageName)
            }
        )
    }

    return HealthConnectClient.getOrCreate(context)
}
