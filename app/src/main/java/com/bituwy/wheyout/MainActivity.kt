package com.bituwy.wheyout

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.response.ReadRecordsResponse
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.lifecycle.lifecycleScope
import com.bituwy.wheyout.model.getHealthConnectClient
import com.bituwy.wheyout.ui.theme.WheyOutTheme
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.temporal.ChronoUnit

val PERMISSIONS = setOf(
    HealthPermission.getReadPermission(NutritionRecord::class),
    HealthPermission.PERMISSION_READ_HEALTH_DATA_HISTORY,
    HealthPermission.PERMISSION_READ_HEALTH_DATA_IN_BACKGROUND
)
val TAG = "MainActivity"
class MainActivity : ComponentActivity() {
    private lateinit var healthConnectClient: HealthConnectClient
    private lateinit var requestPermission: ActivityResultLauncher<Set<String>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WheyOutTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    floatingActionButton = {
                        FloatingActionButton(
                            onClick = {
                                Log.i(TAG, "Button clicked")
                                lifecycleScope.launch {
                                    checkPermissionsAndRun()
                                }
                            }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Permissions")
                        }
                    }
                ) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )

                }
            }
        }

        healthConnectClient = getHealthConnectClient(this)

        val requestPermissionActivityContract = PermissionController.createRequestPermissionResultContract()
        requestPermission = registerForActivityResult(requestPermissionActivityContract) { granted ->
            if (granted.containsAll(PERMISSIONS)) {
                logHealthData()
            } else {
                Log.i(TAG, "Lack of required permissions")
            }
        }
   }

    suspend fun fetchHealthData(): ReadRecordsResponse<NutritionRecord> {
        val instant = Instant.now().minus(10, ChronoUnit.DAYS)
        val timeRange = TimeRangeFilter.after(instant)
        val response = healthConnectClient.readRecords(
            ReadRecordsRequest(
                NutritionRecord::class,
                timeRangeFilter = timeRange
            )
        )
        return response
    }

    fun logHealthData() {
        lifecycleScope.launch {
            val healthData = fetchHealthData()
            for (record in healthData.records) {
                Log.i(TAG, "Name: ${record.name.toString()} Calories: ${record.energy?.inKilocalories.toString()}")
            }
        }
    }

    suspend fun checkPermissionsAndRun() {
        val granted = healthConnectClient.permissionController.getGrantedPermissions()
        Log.i(TAG, granted.toString())
        if (granted.containsAll(PERMISSIONS)) {
            // Permissions already granted; proceed with inserting or reading data
            Log.i(TAG, "Permissions already granted")
            logHealthData()
        } else {
            Log.i(TAG, "Asking for permissions")
            requestPermission.launch(PERMISSIONS)
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    WheyOutTheme {
        Greeting("Android")
    }
}