package com.example.send_angle2

import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp


class MainActivity : ComponentActivity() {

    private val backgroundExecutor: ExecutorService by lazy { Executors.newSingleThreadExecutor() }

    private val scope = CoroutineScope(Job() + Dispatchers.Main)
    private var ortEnv: OrtEnvironment? = null
    private var imageCapture: ImageCapture? = null
    private var imageAnalysis: ImageAnalysis? = null

    private suspend fun readModel(): ByteArray = withContext(Dispatchers.IO) {
        val modelID = R.raw.face_detector
        resources.openRawResource(modelID).readBytes()
    }

    // Create a new ORT session in background
    private suspend fun createOrtSession(): OrtSession? = withContext(Dispatchers.Default) {
        ortEnv?.createSession(readModel())
    }

    private fun readLabels(): List<String> {
        return resources.openRawResource(R.raw.imagenet_classes).bufferedReader().readLines()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ortEnv = OrtEnvironment.getEnvironment()

        val sessionState = mutableStateOf<OrtSession?>(null)

        scope.launch {
            sessionState.value = createOrtSession()
        }

        setContent {
            MyApp(sessionState.value)
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        public const val TAG = "ORTImageClassifier"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
}

@Composable
fun MyApp(ortSession: OrtSession?) {
    var angle1 by remember { mutableStateOf("") }
    var angle2 by remember { mutableStateOf("") }
    var responseText by remember { mutableStateOf("Press the button to set servo angles") }

    MaterialTheme {
        Surface {
            LazyColumn(modifier = Modifier.padding(16.dp)) {
                item {
                    OutlinedTextField(
                        value = angle1,
                        onValueChange = { angle1 = it },
                        label = { Text("Angle 1") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number)
                    )
                }
                item {
                    OutlinedTextField(
                        value = angle2,
                        onValueChange = { angle2 = it },
                        label = { Text("Angle 2") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number)
                    )
                }
                item {
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
                }
                item {
                    Text(text = responseText, modifier = Modifier.padding(top = 16.dp))
                }
                item {
                    Text(text = ortSession?.toString() ?: "ORT Session not created yet", modifier = Modifier.padding(top = 16.dp))
                }
                item {
                    Text(text = ortSession?.inputInfo.toString() ?: "ORT Session not created yet", modifier = Modifier.padding(top = 16.dp))

                }
                item {
                    Text(
                        text = ortSession?.outputInfo.toString() ?: "ORT Session not created yet",
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
                item {
                    Text(
                        text = ortSession?.outputInfo.toString() ?: "ORT Session not created yet",
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
                item {
                    Text(
                        text = ortSession?.outputInfo.toString() ?: "ORT Session not created yet",
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
                    item {
                    val imageBitmap = ImageBitmap.imageResource(id = R.raw.me)
                    Image(
                        bitmap = imageBitmap,
                        contentDescription = null,
                        modifier = Modifier
                            .width(320.dp)
                            .height(240.dp),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        }
    }
}