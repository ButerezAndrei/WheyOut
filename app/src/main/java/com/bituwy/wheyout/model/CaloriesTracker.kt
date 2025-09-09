package com.bituwy.wheyout.model

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.response.ReadRecordsResponse
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.LocalDateTime

const val TAG = "CaloriesTracker"
//TODO: Change catching the SecurityException when permission missing and have granular user info on the screen
//TODO: Figure out why AOD doesn't work for this even though it works for HelloWorld Glyph
class CaloriesTracker(val context: Context) {
    val target = 1600.0

    suspend fun remaining(from: LocalDateTime): Double {
        val timeRage = TimeRangeFilter.after(from)

        return target - consumed(timeRage)
    }

    suspend fun percentConsumed(from: LocalDateTime): Double {
        return consumed(from)/target
    }

    suspend fun consumed(from: LocalDateTime): Double {
        val timeRange = TimeRangeFilter.after(from)

        return consumed(timeRange)
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

    public companion object {
        val PERMISSIONS = setOf(
            HealthPermission.getReadPermission(NutritionRecord::class),
            HealthPermission.PERMISSION_READ_HEALTH_DATA_HISTORY,
            HealthPermission.PERMISSION_READ_HEALTH_DATA_IN_BACKGROUND
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
