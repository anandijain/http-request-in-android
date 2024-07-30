package com.example.send_angle2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApp()
        }
    }
}

@Composable
fun MyApp() {
    var angle1 by remember { mutableStateOf("") }
    var angle2 by remember { mutableStateOf("") }
    var responseText by remember { mutableStateOf("Press the button to set servo angles") }

    MaterialTheme {
        Surface {
            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = angle1,
                    onValueChange = { angle1 = it },
                    label = { Text("Angle 1") },
                    modifier = Modifier.padding(bottom = 8.dp),
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = angle2,
                    onValueChange = { angle2 = it },
                    label = { Text("Angle 2") },
                    modifier = Modifier.padding(bottom = 8.dp),
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number)
                )
                Button(onClick = {
                    val angle1Int = angle1.toIntOrNull()
                    val angle2Int = angle2.toIntOrNull()
                    if (angle1Int != null && angle2Int != null && angle1Int in 0..180 && angle2Int in 0..180) {
                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                val response = RetrofitClient.apiService.setServoAngles(angle1Int, angle2Int).string()
                                withContext(Dispatchers.Main) {
                                    responseText = response
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    responseText = e.message ?: "Error"
                                }
                            }
                        }
                    } else {
                        responseText = "Invalid angles. Must be between 0 and 180."
                    }
                }) {
                    Text("Set Servo Angles")
                }
                Text(text = responseText, modifier = Modifier.padding(top = 16.dp))
            }
        }
    }
}
