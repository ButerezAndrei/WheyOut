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

class CaloriesTracker private constructor (val nutritionRecords: ReadRecordsResponse<NutritionRecord>) {

    //TODO: Make the target configurable from the main activity
    val target = 1600.0

    fun percentConsumed(): Double {
        return consumed()/target
    }

    fun remaining(): Double {
        return target - consumed()
    }

    fun consumed(): Double {
        return nutritionRecords.records.sumOf { it.energy?.inKilocalories ?: 0.0 }
    }

    fun nutritionalValues(): Map<String, Double> {
        // TODO: Nutrition Records may be paginated, check the page limits and handle the case
        val nutrition = mutableMapOf("proteins" to 0.0, "carbs" to 0.0, "fats" to 0.0)
        nutritionRecords.records.forEach {
            nutrition["proteins"] = nutrition["proteins"]!! + (it.protein?.inGrams ?: 0.0)
            nutrition["carbs"] = nutrition["carbs"]!! + (it.totalCarbohydrate?.inGrams ?: 0.0)
            nutrition["fats"] = nutrition["fats"]!! + (it.totalFat?.inGrams ?: 0.0)
        }
        return nutrition
    }

    companion object {
        const val TAG = "CaloriesTracker"
        val PERMISSIONS = mapOf<String, String>(
            "Nutrition" to HealthPermission.getReadPermission(NutritionRecord::class),
            "Health History" to HealthPermission.PERMISSION_READ_HEALTH_DATA_HISTORY,
            "Health Background" to HealthPermission.PERMISSION_READ_HEALTH_DATA_IN_BACKGROUND
        )

        suspend fun create(context: Context, from: LocalDateTime): CaloriesTracker {
            val nutritionRecords = fetchNutrition(context,TimeRangeFilter.after(from))
            return CaloriesTracker(nutritionRecords)
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

        suspend fun fetchNutrition(context: Context, between: TimeRangeFilter): ReadRecordsResponse<NutritionRecord> {
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
}
