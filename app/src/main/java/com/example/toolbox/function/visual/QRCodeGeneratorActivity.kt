package com.example.toolbox.function.visual

import android.app.Activity
import android.content.ContentValues
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.material.icons.filled.Save
import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.graphics.createBitmap
import com.example.toolbox.ui.theme.ToolBoxTheme
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.datamatrix.DataMatrixWriter
import com.google.zxing.oned.Code128Writer
import com.google.zxing.oned.Code39Writer
import com.google.zxing.oned.EAN13Writer
import com.google.zxing.oned.EAN8Writer
import com.google.zxing.pdf417.PDF417Writer
import com.google.zxing.aztec.AztecWriter
import java.util.EnumMap

class QRCodeGeneratorActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ToolBoxTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    QRCodeGeneratorScreen(
                        modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QRCodeGeneratorScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    data class BarcodeType(val name: String, val format: BarcodeFormat, val is1D: Boolean)
    
    val barcodeTypes = remember {
        listOf(
            BarcodeType("QR Code", BarcodeFormat.QR_CODE, false),
            BarcodeType("Data Matrix", BarcodeFormat.DATA_MATRIX, false),
            BarcodeType("Aztec", BarcodeFormat.AZTEC, false),
            BarcodeType("PDF 417", BarcodeFormat.PDF_417, false),
            BarcodeType("Code 128", BarcodeFormat.CODE_128, true),
            BarcodeType("Code 39", BarcodeFormat.CODE_39, true),
            BarcodeType("EAN-13", BarcodeFormat.EAN_13, true),
            BarcodeType("EAN-8", BarcodeFormat.EAN_8, true)
        )
    }

    var selectedTypeIndex by remember { mutableIntStateOf(0) }
    var inputText by remember { mutableStateOf("") }
    var qrCodeBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var errorMessage by remember { mutableStateOf("") }
    
    val currentType = barcodeTypes[selectedTypeIndex]

    fun generateBarcode() {
        errorMessage = ""
        if (inputText.isEmpty()) {
            errorMessage = "请输入要生成${currentType.name}的内容"
            return
        }

        try {
            val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java).apply {
                put(EncodeHintType.CHARACTER_SET, "UTF-8")
                put(EncodeHintType.MARGIN, 1)
            }

            val bitMatrix = when (currentType.format) {
                BarcodeFormat.QR_CODE -> {
                    QRCodeWriter().encode(inputText, BarcodeFormat.QR_CODE, 512, 512, hints)
                }
                BarcodeFormat.DATA_MATRIX -> {
                    DataMatrixWriter().encode(inputText, BarcodeFormat.DATA_MATRIX, 256, 256, hints)
                }
                BarcodeFormat.AZTEC -> {
                    AztecWriter().encode(inputText, BarcodeFormat.AZTEC, 256, 256, hints)
                }
                BarcodeFormat.PDF_417 -> {
                    PDF417Writer().encode(inputText, BarcodeFormat.PDF_417, 512, 256, hints)
                }
                BarcodeFormat.CODE_128 -> {
                    Code128Writer().encode(inputText, BarcodeFormat.CODE_128, 512, 100, hints)
                }
                BarcodeFormat.CODE_39 -> {
                    Code39Writer().encode(inputText, BarcodeFormat.CODE_39, 512, 100, hints)
                }
                BarcodeFormat.EAN_13 -> {
                    if (!inputText.matches(Regex("\\d{12,13}"))) {
                        throw IllegalArgumentException("EAN-13需要12或13位数字")
                    }
                    EAN13Writer().encode(inputText.take(12), BarcodeFormat.EAN_13, 512, 100, hints)
                }
                BarcodeFormat.EAN_8 -> {
                    if (!inputText.matches(Regex("\\d{7,8}"))) {
                        throw IllegalArgumentException("EAN-8需要7或8位数字")
                    }
                    EAN8Writer().encode(inputText.take(7), BarcodeFormat.EAN_8, 512, 100, hints)
                }
                else -> throw IllegalArgumentException("不支持的条码格式")
            }

            val width = bitMatrix.width
            val height = bitMatrix.height
            val pixels = IntArray(width * height)

            for (y in 0 until height) {
                for (x in 0 until width) {
                    pixels[y * width + x] = if (bitMatrix[x, y]) {
                        Color.BLACK
                    } else {
                        Color.WHITE
                    }
                }
            }

            qrCodeBitmap = createBitmap(width, height)
            qrCodeBitmap?.setPixels(pixels, 0, width, 0, 0, width, height)

        } catch (e: Exception) {
            errorMessage = "生成${currentType.name}失败: ${e.message}"
        }
    }

    fun saveImageToGallery() {
        val bitmap = qrCodeBitmap ?: return
        val barcodeType = currentType.name.replace(" ", "_")
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "${barcodeType}_${System.currentTimeMillis()}.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/ToolBox")
            }
        }

        try {
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                }
                Toast.makeText(context, "图片已保存至相册", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "保存失败", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "保存出错: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TopAppBar(
            title = { Text("${currentType.name}生成器") },
            navigationIcon = {
                FilledTonalIconButton(onClick = { (context as Activity).finish() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                }
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "选择条码类型",
                        style = MaterialTheme.typography.titleMedium
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(barcodeTypes.size) { index ->
                            FilterChip(
                                selected = selectedTypeIndex == index,
                                onClick = { selectedTypeIndex = index },
                                label = { Text(barcodeTypes[index].name) }
                            )
                        }
                    }

                    Text(
                        text = "输入内容",
                        style = MaterialTheme.typography.titleMedium
                    )
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { 
                            Text(when {
                                currentType.format == BarcodeFormat.EAN_13 -> "请输入12或13位数字"
                                currentType.format == BarcodeFormat.EAN_8 -> "请输入7或8位数字"
                                currentType.is1D -> "请输入文本（仅支持字母、数字和部分符号）"
                                else -> "请输入文本或链接"
                            })
                        },
                        singleLine = false,
                        maxLines = 5
                    )
                    if (currentType.is1D) {
                        Text(
                            text = "提示：一维码仅支持ASCII字符，不支持中文",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
            }

            if (errorMessage.isNotEmpty()) {
                Card(
                    elevation = CardDefaults.cardElevation(0.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Text(
                        text = errorMessage,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            Card(
                elevation = CardDefaults.cardElevation(0.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Button(
                        onClick = { generateBarcode() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.QrCode,
                                contentDescription = "生成${currentType.name}",
                                modifier = Modifier.size(ButtonDefaults.IconSize)
                            )
                            Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                            Text("生成${currentType.name}")
                        }
                    }
                }
            }

            if (qrCodeBitmap != null) {
                Card(
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "生成的${currentType.name}",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        Image(
                            bitmap = qrCodeBitmap!!.asImageBitmap(),
                            contentDescription = currentType.name,
                            modifier = Modifier
                                .size(256.dp)
                                .padding(8.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Button(
                                onClick = { saveImageToGallery() },
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Filled.Save,
                                        contentDescription = "保存图片",
                                        modifier = Modifier.size(ButtonDefaults.IconSize)
                                    )
                                    Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                                    Text("保存图片")
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}