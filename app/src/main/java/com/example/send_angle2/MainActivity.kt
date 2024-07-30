package com.example.send_angle2

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import java.nio.FloatBuffer


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
    var resultsText by remember { mutableStateOf("Results will be shown here") }

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
                                    val response = RetrofitClient.apiService.setServoAngles(
                                        angle1Int,
                                        angle2Int
                                    ).string()
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
                    Text(
                        text = ortSession?.toString() ?: "ORT Session not created yet",
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
                item {
                    Text(
                        text = ortSession?.inputInfo.toString() ?: "ORT Session not created yet",
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
                    Text(
                        text = ortSession?.outputInfo.toString() ?: "ORT Session not created yet",
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
                item {
                    val imageBitmap = ImageBitmap.imageResource(id = R.raw.me)

                    Box(
                        modifier = Modifier
                            .width(320.dp)
                            .height(240.dp)
                    ) {
                        Image(
                            bitmap = imageBitmap,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val centerX = size.width / 2
                            val centerY = size.height / 2
                            drawRect(
                                color = Color.Red,
                                topLeft = androidx.compose.ui.geometry.Offset(
                                    centerX - 50,
                                    centerY - 50
                                ),
                                size = androidx.compose.ui.geometry.Size(100f, 100f),
                                style = Stroke(width = 4.dp.toPx())
                            )
                        }
                    }
                    // Convert imageBitmap to tensor
                    val tensor = remember {
                        createTensorFromImage(imageBitmap)
                    }

                    // Use tensor with the ORT session
                    LaunchedEffect(tensor) {
                        if (ortSession != null) {
                            val inputName = ortSession.inputNames.iterator().next()
                            val feeds = mapOf(inputName to tensor)
                            val results = ortSession.run(feeds)
//                            val rawOutput = results?.get(0)?.value
                            val rawOutputBoxes = ((results.get(1)?.value) as Array<Array<FloatArray>>)[0]
                            Log.d("ORTOutput", "Raw Output: ${rawOutputBoxes.toString()}")
//                            Log.d("ORTOutputBoxes", "Raw Output Boxes: ${rawOutputBoxes.joinToString()}")

                            resultsText = "Results: " + results.toString()
//                            resultsText += results.get(0).info
                            resultsText += results.get("scores").get().info
                            resultsText += results.get("boxes").get().info
                            resultsText += "\n"
//                            val subArray = rawOutputBoxes[0].sliceArray(0..3)
                            val foo = listOf(rawOutputBoxes[0][0], rawOutputBoxes[0][1], rawOutputBoxes[0][2], rawOutputBoxes[0][3])
                            resultsText += foo

//                            resultsText += results.get(1).info
                        }

                    }
                }

                item {
                    Text(text = resultsText, modifier = Modifier.padding(top = 16.dp))
                }
            }
            //
        }
    }
}


private fun createTensorFromImage(imageBitmap: androidx.compose.ui.graphics.ImageBitmap): OnnxTensor {
    val width = 320
    val height = 240
    val input = FloatArray(1 * 3 * height * width)

    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    canvas.drawBitmap(
        imageBitmap.asAndroidBitmap(),
        null,
        android.graphics.Rect(0, 0, width, height),
        null
    )

    val pixels = IntArray(width * height)
    bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

    for (i in 0 until height) {
        for (j in 0 until width) {
            val idx = i * width + j
            val pixel = pixels[idx]
            val r = (pixel shr 16 and 0xFF) / 255.0f
            val g = (pixel shr 8 and 0xFF) / 255.0f
            val b = (pixel and 0xFF) / 255.0f
            input[(0 * height + i) * width + j] = r
            input[(1 * height + i) * width + j] = g
            input[(2 * height + i) * width + j] = b
        }
    }

    val env = OrtEnvironment.getEnvironment()
    val shape = longArrayOf(1, 3, height.toLong(), width.toLong())
    return OnnxTensor.createTensor(env, FloatBuffer.wrap(input), shape)
}

data class Box(val x1: Float, val y1: Float, val x2: Float, val y2: Float, val score: Float)

fun intersectionOverUnion(boxA: Box, boxB: Box): Float {
    val xA = maxOf(boxA.x1, boxB.x1)
    val yA = maxOf(boxA.y1, boxB.y1)
    val xB = minOf(boxA.x2, boxB.x2)
    val yB = minOf(boxA.y2, boxB.y2)

    val interArea = maxOf(0f, xB - xA) * maxOf(0f, yB - yA)
    val boxAArea = (boxA.x2 - boxA.x1) * (boxA.y2 - boxA.y1)
    val boxBArea = (boxB.x2 - boxB.x1) * (boxB.y2 - boxB.y1)

    return interArea / (boxAArea + boxBArea - interArea)
}

fun nonMaxSuppression(boxes: List<Box>, scoreThreshold: Float, iouThreshold: Float): List<Box> {
    val filteredBoxes = mutableListOf<Box>()

    // Filter out boxes with low scores
    val candidates = boxes.filter { it.score > scoreThreshold }.sortedByDescending { it.score }.toMutableList()

    // Perform Non-Maximum Suppression
    while (candidates.isNotEmpty()) {
        val bestBox = candidates.removeAt(0)
        filteredBoxes.add(bestBox)

        val iterator = candidates.iterator()
        while (iterator.hasNext()) {
            val box = iterator.next()
            if (intersectionOverUnion(bestBox, box) >= iouThreshold) {
                iterator.remove()
            }
        }
    }

    return filteredBoxes
}
