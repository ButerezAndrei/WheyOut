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
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.toJavaDuration

const val TAG = "CaloriesTracker"
//TODO: Handle checking permissions
//TODO: Figure out why AOD doesnt work for this eventhough it works for HelloWorld Glyph
class CaloriesTracker(val context: Context) {
    val target = 1600.0

    suspend fun remaining(forTheLast: Duration): Double {
        return target - consumed(1.days)
    }

    suspend fun consumed(forTheLast: Duration): Double {
        val temporalAmount = forTheLast.toJavaDuration()
        val startOfPeriod = Instant.now().minus(temporalAmount)
        val interval = TimeRangeFilter.after(startOfPeriod)
        val nutritionRecords = fetchNutrition(interval)
        return nutritionRecords.records.sumOf { it.energy?.inKilocalories ?: 0.0 }
    }

    suspend fun fetchNutrition(forInterval: TimeRangeFilter): ReadRecordsResponse<NutritionRecord> {
        val healthConnectClient = getHealthConnectClient(context)
        return healthConnectClient.readRecords(
            ReadRecordsRequest(
                NutritionRecord::class,
                forInterval
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
