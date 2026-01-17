package com.bituwy.wheyout

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Button
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.response.ReadRecordsResponse
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.lifecycle.lifecycleScope
import com.bituwy.wheyout.model.CaloriesTracker
import com.bituwy.wheyout.model.CaloriesTracker.Companion.getHealthConnectClient
import com.bituwy.wheyout.ui.theme.WheyOutTheme
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.toString


class MainActivity : ComponentActivity() {
    companion object {
        val TAG = "MainActivity"
    }
    private lateinit var healthConnectClient: HealthConnectClient
    private lateinit var requestPermission: ActivityResultLauncher<Set<String>>

    private var isPermissionGranted by mutableStateOf(false)
    val ndotFontFamily = FontFamily(
        Font(R.font.ndot55_regular)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        healthConnectClient = getHealthConnectClient(this)

        val requestPermissionActivityContract = PermissionController.createRequestPermissionResultContract()

        requestPermission =
            registerForActivityResult(requestPermissionActivityContract) { granted ->
                isPermissionGranted = granted.containsAll(CaloriesTracker.PERMISSIONS.values.toSet())
                if (isPermissionGranted) {
                    logHealthData()
                } else {
                    Log.d(TAG, "Lack of required permissions")
                }
            }

        enableEdgeToEdge()
        setContent {
            // TODO: Fix the text and background to follow phone theming (i.e dark mode) to be consistent with the ListItem behaviour
            WheyOutTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // TODO: [BUG] The buttons still show if the user manually enables Permissions, not vice-vers because Android forces a reset when permissions are disabled
                    LaunchedEffect(Unit) {
                        isPermissionGranted = hasPermissions()
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .safeContentPadding(),
                        verticalArrangement = Arrangement.Top,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("WheyOut Glyphs", fontSize = 35.sp, fontFamily = ndotFontFamily, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                        ListItem(
                            headlineContent = { Text("Calories Glyph", fontFamily = ndotFontFamily, fontWeight = FontWeight.Bold) },
                            leadingContent = {
                                GlyphPreview(R.drawable.glyph_matrix_calories_preview)
                            },
                            trailingContent = {
                                IconButton(
                                    onClick = {
                                        Log.d(TAG, "Permissions requested for Calories Glyph")
                                        if(!isPermissionGranted){
                                            requestPermissions()
                                        }
                                    }
                                ) {
                                    if (!isPermissionGranted) {
                                        Icon(
                                            Icons.Default.Error,
                                            "Need Permissions",
                                            tint = Color.Red
                                        )
                                    }
                                }
                            }
                        )
                    }
                }
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

    suspend fun hasPermissions(): Boolean {
        val granted = healthConnectClient.permissionController.getGrantedPermissions()
        Log.d(TAG, granted.toString())
        return granted.containsAll(CaloriesTracker.PERMISSIONS.values.toSet())
    }
    fun requestPermissions() {
        Log.d(TAG, "Asking for permissions")
        requestPermission.launch(CaloriesTracker.PERMISSIONS.values.toSet())
    }
}
@Composable
fun GlyphPreview(drawable: Int) {
    val imageModifier = Modifier
        .size(75.dp)
    Image(
        painter = painterResource(drawable),
        contentDescription = "Calories Glyph Preview",
        contentScale = ContentScale.Fit,
        modifier = imageModifier,
    )
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