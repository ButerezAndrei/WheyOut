package com.bituwy.wheyout.model

import android.app.RecoverableSecurityException
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
class CaloriesTracker(val context: Context) {
    companion object {
        val PERMISSIONS = mapOf<String, String>(
            "Nutrition" to HealthPermission.getReadPermission(NutritionRecord::class),
            "Health History" to HealthPermission.PERMISSION_READ_HEALTH_DATA_HISTORY,
            "Health Background" to HealthPermission.PERMISSION_READ_HEALTH_DATA_IN_BACKGROUND
        )
    }
    //TODO: Make the target configurable from the main activity
    val target = 1600.0

    suspend fun percentConsumed(from: LocalDateTime): Double {
        return consumed(from)/target
    }

    suspend fun remaining(from: LocalDateTime): Double {
        val timeRage = TimeRangeFilter.after(from)

        return target - consumed(timeRage)
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
        val grantedPermissions = healthConnectClient.permissionController.getGrantedPermissions()
        val missingPermissionsMessages = PERMISSIONS.filterValues { !grantedPermissions.contains(it) }.keys
        if (missingPermissionsMessages.any()) {
            throw SecurityException(missingPermissionsMessages.joinToString(prefix = "Missing Permissions: "))
        }
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
