package com.velocimetro.android

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Looper
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.velocimetro.android.ui.theme.VelocimetroAndroidTheme
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var locationCallback: LocationCallback? = null
    private var speedListener: ((Float) -> Unit)? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            startLocationUpdates()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        setContent {
            VelocimetroAndroidTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    VelocimetroScreen(
                        onRegisterSpeedListener = { listener -> speedListener = listener }
                    )
                }
            }
        }

        requestLocationPermissions()
    }

    private fun hasLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermissions() {
        if (hasLocationPermission()) {
            startLocationUpdates()
        } else {
            requestPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        if (locationCallback != null) return

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            500L
        ).setMinUpdateIntervalMillis(200L).build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                val location = locationResult.lastLocation ?: return
                val speed = if (location.hasSpeed()) location.speed * 3.6f else 0f
                speedListener?.invoke(speed)
            }
        }
        locationCallback = callback
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            callback,
            Looper.getMainLooper()
        )
    }

    private fun stopLocationUpdates() {
        locationCallback?.let { fusedLocationClient.removeLocationUpdates(it) }
        locationCallback = null
    }

    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
    }

    override fun onResume() {
        super.onResume()
        if (hasLocationPermission()) {
            startLocationUpdates()
        }
    }
}

@Composable
fun VelocimetroScreen(
    onRegisterSpeedListener: ((Float) -> Unit) -> Unit
) {
    var speedKmh by remember { mutableFloatStateOf(0f) }
    var unit by remember { mutableIntStateOf(0) } // 0 = km/h, 1 = mph
    var showColorPicker by remember { mutableStateOf(false) }
    var displayColor by remember { mutableStateOf(Color.Red) }

    LaunchedEffect(Unit) {
        onRegisterSpeedListener { speed -> speedKmh = speed }
    }

    if (showColorPicker) {
        ColorPickerDialog(
            currentColor = displayColor,
            onColorSelected = { displayColor = it },
            onDismiss = { showColorPicker = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Velocímetro Android",
                color = Color.White,
                style = MaterialTheme.typography.titleLarge
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                SingleChoiceSegmentedButtonRow {
                    SegmentedButton(
                        selected = unit == 0,
                        onClick = { unit = 0 },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) {
                        Text("Km/h")
                    }
                    SegmentedButton(
                        selected = unit == 1,
                        onClick = { unit = 1 },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) {
                        Text("mph")
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Button(onClick = { showColorPicker = true }) {
                    Text("Color...")
                }
            }
        }

        val displaySpeed = if (unit == 0) {
            speedKmh
        } else {
            speedKmh * 2.23693629f
        }
        val displayText = String.format("%.1f", displaySpeed)

        AutoResizeText(
            text = displayText,
            color = displayColor,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f)
                .align(Alignment.CenterHorizontally)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorPickerDialog(
    currentColor: Color,
    onColorSelected: (Color) -> Unit,
    onDismiss: () -> Unit
) {
    var red by remember { mutableFloatStateOf(currentColor.red * 255) }
    var green by remember { mutableFloatStateOf(currentColor.green * 255) }
    var blue by remember { mutableFloatStateOf(currentColor.blue * 255) }

    LaunchedEffect(currentColor) {
        red = currentColor.red * 255
        green = currentColor.green * 255
        blue = currentColor.blue * 255
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Seleccionar color") },
        text = {
            Column {
                Text("Rojo: ${red.roundToInt()}")
                Slider(
                    value = red,
                    onValueChange = { red = it },
                    valueRange = 0f..255f
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("Verde: ${green.roundToInt()}")
                Slider(
                    value = green,
                    onValueChange = { green = it },
                    valueRange = 0f..255f
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("Azul: ${blue.roundToInt()}")
                Slider(
                    value = blue,
                    onValueChange = { blue = it },
                    valueRange = 0f..255f
                )
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .background(Color(red.roundToInt(), green.roundToInt(), blue.roundToInt()))
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onColorSelected(Color(red.roundToInt(), green.roundToInt(), blue.roundToInt()))
                onDismiss()
            }) {
                Text("Aceptar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun AutoResizeText(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    maxFontSize: TextUnit = 300.sp
) {
    val fontFamily = FontFamily.Monospace

    SubcomposeLayout(modifier = modifier) { constraints ->
        var fontSize = maxFontSize
        var readyToDraw = false

        while (!readyToDraw) {
            val textStyle = androidx.compose.ui.text.TextStyle(
                fontSize = fontSize,
                fontFamily = fontFamily,
                color = color,
                textAlign = TextAlign.Center
            )
            val textMeasurable = subcompose("text") {
                Text(
                    text = text,
                    style = textStyle,
                    maxLines = 1
                )
            }.first()
            val textSize = textMeasurable.measure(Constraints(maxWidth = Int.MAX_VALUE))

            if (textSize.width <= constraints.maxWidth && textSize.height <= constraints.maxHeight || fontSize.value <= 1f) {
                readyToDraw = true
            } else {
                fontSize = fontSize * 0.9f
            }
        }

        val finalTextStyle = androidx.compose.ui.text.TextStyle(
            fontSize = fontSize,
            fontFamily = fontFamily,
            color = color,
            textAlign = TextAlign.Center
        )
        val finalMeasurable = subcompose("finalText") {
            Text(
                text = text,
                style = finalTextStyle,
                maxLines = 1,
                modifier = Modifier.fillMaxSize()
            )
        }.first()
        val finalSize = finalMeasurable.measure(
            Constraints(
                minWidth = constraints.minWidth,
                maxWidth = constraints.maxWidth,
                minHeight = constraints.minHeight,
                maxHeight = constraints.maxHeight
            )
        )

        layout(finalSize.width, finalSize.height) {
            finalSize.placeRelative(0, 0)
        }
    }
}
