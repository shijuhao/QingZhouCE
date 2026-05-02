package com.example.toolbox.function.visual

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.ceil
import kotlin.math.sqrt
import kotlin.random.Random
import androidx.core.graphics.createBitmap
import com.example.toolbox.ui.theme.ToolBoxTheme
import java.io.File
import androidx.core.graphics.scale

enum class HighlightScope {
    NONE, LETTER_ONLY, ORIGINAL_ONLY
}

data class HighlightConfig(
    val scope: HighlightScope = HighlightScope.ORIGINAL_ONLY,
    val textColor: Color = Color.Red,
    val bgColor: Color = Color.Yellow,
    val bold: Boolean = true
)

class AntiOCRGenerator(private val context: Context) {

    private val disturbChars = "的一是在不了有和人这中大为上个国我以要他时来用们生到作地于出就分对成会可主发年动同工也能下过子说产种面而方后多定行学法所民得经十三之进着等部度家电力里如水化高自二理起小物现实加量都两体制机当使点从业本去把性好应开它合还因由其些然前外天政四日那社义事平形相全表间样与关各重新线内数正心反你明看原又么利比或但质气第向道命此变条只没结解问意建月公无系军很情者最立代想已通并提直题党程展五果料象员革位入常文总次品式活设及管特件长求老头基资边流路级少图山统接知较将组见计别她手角期根论运农指几九区强放决西被干做必战先回则任取据处队南给色光门即保治北造百规热领七海口东导器压志世金增争济阶油思术极交受联什认六共权收证改清己美再采转更单风切打白教速花带安场身车例真务具万每目至达走积示议声报斗完类八离华名确才科张信马节话米整空元况今集温传土许步群广石记需段研界拉林律叫且究观越织装影算低持音众书布复容儿须际商非验连断深难近矿千周委素技备半办青省列习响约支般史感劳便团往酸历市克何除消构府称太准精值号率族维划选标写存候毛亲快效斯院查江型眼王按格养易置派层片始却专状育厂京识适属圆包火住调满县局照参红细引听该铁价严".toList()
    private val alphabetPool = ('a'..'z') + ('A'..'Z')

    private data class CharItem(
        val letter: Char,
        val char: Char,
        val isOriginal: Boolean,
        val isOriginalLetter: Boolean
    )

    private fun getPaint(fontSizeSp: Float): Paint {
        return Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = fontSizeSp.spToPx(context).toFloat()
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
    }

    private fun insertDisturbanceChars(originalText: String): Pair<MutableList<CharItem>, MutableList<Char>> {
        var alphabetIdx = 0
        val seq = mutableListOf<CharItem>()
        val letterMap = mutableListOf<Char>()

        repeat(Random.nextInt(0, 4)) {
            val dChar = disturbChars.random()
            val letter = alphabetPool[alphabetIdx % alphabetPool.size]
            alphabetIdx++
            seq.add(CharItem(letter, dChar, false, false))
        }

        originalText.forEach { char ->
            val letter = alphabetPool[alphabetIdx % alphabetPool.size]
            alphabetIdx++
            letterMap.add(letter)
            seq.add(CharItem(letter, char, true, true))

            val count = Random.nextInt(3, 5)
            repeat(count) {
                val dChar = disturbChars.random()
                val dLetter = alphabetPool[alphabetIdx % alphabetPool.size]
                alphabetIdx++
                seq.add(CharItem(dLetter, dChar, false, false))
            }
        }
        return seq to letterMap
    }

    private fun calculateLayout(totalItems: Int, userRows: Int?, userCols: Int?): Pair<Int, Int> {
        if (userRows != null && userCols != null) {
            var rows = userRows
            if (rows * userCols < totalItems) {
                rows = ceil(totalItems.toDouble() / userCols).toInt()
            }
            return rows to userCols
        }
        var cols = maxOf(3, ceil(sqrt(totalItems * 1.2)).toInt())
        var rows = ceil(totalItems.toDouble() / cols).toInt()
        if (rows < 5) {
            rows = 5
            cols = maxOf(3, ceil(totalItems.toDouble() / rows).toInt())
        }
        return rows to cols
    }

    private fun measureText(paint: Paint, text: String): Pair<Float, Float> {
        val bounds = Rect()
        paint.getTextBounds(text, 0, text.length, bounds)
        return bounds.width().toFloat() to bounds.height().toFloat()
    }

    private fun getFontHeight(paint: Paint): Float {
        return paint.fontMetrics.descent - paint.fontMetrics.ascent
    }

    suspend fun generate(
        originalText: String,
        highlightConfig: HighlightConfig = HighlightConfig(),
        rows: Int? = null,
        cols: Int? = null,
        rotateAngle: Float? = null,
        watermarkOpacity: Float = 0.35f,
        fontSizeSp: Float = 20f
    ): Bitmap = withContext(Dispatchers.Default) {
        val paint = getPaint(fontSizeSp)
        val (seq, letterMap) = insertDisturbanceChars(originalText)
        val (finalRows, finalCols) = calculateLayout(seq.size, rows, cols)

        var maxLetterWidth = 0f
        alphabetPool.forEach { letter ->
            val (w, _) = measureText(paint, letter.toString())
            if (w > maxLetterWidth) maxLetterWidth = w
        }

        var maxCharWidth = 0f
        seq.forEach { item ->
            val (w, _) = measureText(paint, item.char.toString())
            if (w > maxCharWidth) maxCharWidth = w
        }

        val padding = 10f
        val cellWidth = maxLetterWidth + 4f + maxCharWidth + padding * 2
        val fontHeight = getFontHeight(paint)
        val cellHeight = fontHeight + padding * 2

        val imgWidth = (finalCols * cellWidth).toInt()
        val imgHeight = (finalRows * cellHeight + 80).toInt()

        val bitmap = createBitmap(imgWidth, imgHeight)
        val canvas = Canvas(bitmap)
        canvas.drawColor(AndroidColor.WHITE)

        val decodeStr = "==${letterMap.joinToString("")}=="
        paint.color = AndroidColor.BLACK
        paint.typeface = Typeface.DEFAULT
        val decodeWidth = paint.measureText(decodeStr)
        canvas.drawText(decodeStr, (imgWidth - decodeWidth) / 2f, 40f - paint.fontMetrics.ascent, paint)

        val gridTop = 90f
        seq.forEachIndexed { idx, item ->
            val row = idx / finalCols
            val col = idx % finalCols
            if (row >= finalRows) return@forEachIndexed

            val baseX = col * cellWidth + padding
            val baseY = gridTop + row * cellHeight + padding
            val baseline = baseY - paint.fontMetrics.ascent

            val letterStyle = when (highlightConfig.scope) {
                HighlightScope.NONE -> null
                HighlightScope.LETTER_ONLY -> if (item.isOriginalLetter) highlightConfig else null
                HighlightScope.ORIGINAL_ONLY -> null
            }

            drawStyledChar(canvas, baseX, baseline, item.letter, letterStyle, paint)

            val charX = baseX + maxLetterWidth + 4f
            val charStyle = when (highlightConfig.scope) {
                HighlightScope.NONE -> null
                HighlightScope.LETTER_ONLY -> null
                HighlightScope.ORIGINAL_ONLY -> if (item.isOriginal) highlightConfig else null
            }

            drawStyledChar(canvas, charX, baseline, item.char, charStyle, paint)
        }

        var resultBitmap = bitmap

        val margin = 40
        val withMargin = createBitmap(resultBitmap.width + margin * 2, resultBitmap.height + margin * 2)
        Canvas(withMargin).apply {
            drawColor(AndroidColor.WHITE)
            drawBitmap(resultBitmap, margin.toFloat(), margin.toFloat(), null)
        }
        resultBitmap = withMargin

        val angle = rotateAngle ?: Random.nextDouble(-15.0, 15.0).toFloat()
        resultBitmap = rotateBitmap(resultBitmap, angle)

        resultBitmap = addWatermark(resultBitmap, fontSizeSp * 0.8f, watermarkOpacity)

        resultBitmap
    }

    private fun drawStyledChar(canvas: Canvas, x: Float, baseline: Float, char: Char, style: HighlightConfig?, paint: Paint) {
        val text = char.toString()
        val textWidth = paint.measureText(text)
        val fontMetrics = paint.fontMetrics

        if (style?.bgColor != null) {
            paint.color = style.bgColor.toArgb()
            paint.style = Paint.Style.FILL
            canvas.drawRect(
                x,
                baseline + fontMetrics.ascent,
                x + textWidth,
                baseline + fontMetrics.descent,
                paint
            )
        }

        paint.color = style?.textColor?.toArgb() ?: AndroidColor.BLACK

        paint.typeface = if (style?.bold == true) {
            Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        } else {
            Typeface.DEFAULT
        }

        canvas.drawText(text, x, baseline, paint)
    }

    private fun rotateBitmap(src: Bitmap, angle: Float): Bitmap {
        val matrix = Matrix().apply { postRotate(angle) }
        val rotated = Bitmap.createBitmap(src, 0, 0, src.width, src.height, matrix, true)
        val result = createBitmap(rotated.width, rotated.height)
        Canvas(result).apply {
            drawColor(AndroidColor.WHITE)
            drawBitmap(rotated, 0f, 0f, null)
        }
        return result
    }

    private fun addWatermark(src: Bitmap, fontSizeSp: Float, opacity: Float): Bitmap {
        val result = src.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.argb((255 * opacity).toInt(), 128, 128, 128)
            textSize = fontSizeSp.spToPx(context).toFloat()
            typeface = Typeface.DEFAULT
        }
        val text = "防止ocr识别"
        val textWidth = paint.measureText(text)
        val spacingY = paint.textSize * 2.5f
        var y = paint.textSize + 30f
        while (y < result.height) {
            var x = -textWidth / 2f
            while (x < result.width + textWidth) {
                canvas.drawText(text, x, y, paint)
                x += textWidth + 60f
            }
            y += spacingY
        }
        return result
    }

    private fun Float.spToPx(context: Context): Int = (this * context.resources.displayMetrics.scaledDensity).toInt()
    private fun Color.toArgb(): Int = AndroidColor.argb((alpha * 255).toInt(), (red * 255).toInt(), (green * 255).toInt(), (blue * 255).toInt())
}

suspend fun saveBitmapToGallery(context: Context, bitmap: Bitmap) = withContext(Dispatchers.IO) {
    val filename = "antiocr_${System.currentTimeMillis()}.png"
    val contentValues = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, filename)
        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
        } else {
            val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            if (!picturesDir.exists()) picturesDir.mkdirs()
            val file = File(picturesDir, filename)
            put(MediaStore.Images.Media.DATA, file.absolutePath)
        }
    }

    val resolver = context.contentResolver
    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
    uri?.let {
        resolver.openOutputStream(it)?.use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            out.flush()
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "图片已保存到相册", Toast.LENGTH_SHORT).show()
            }
        } ?: run {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "保存失败：无法打开输出流", Toast.LENGTH_SHORT).show()
            }
        }
    } ?: withContext(Dispatchers.Main) {
        Toast.makeText(context, "保存失败：无法创建媒体记录", Toast.LENGTH_SHORT).show()
    }
}

class AntiOCRActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ToolBoxTheme {
                AntiOCRScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AntiOCRScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val generator = remember { AntiOCRGenerator(context) }
    val scrollState = rememberScrollState()

    var originalText by remember { mutableStateOf("这是文本") }
    var generatedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isGenerating by remember { mutableStateOf(false) }

    var scopeOption by remember { mutableStateOf(HighlightScope.LETTER_ONLY) }
    var bold by remember { mutableStateOf(true) }
    var rotateAngle by remember { mutableStateOf<Float?>(null) }
    var watermarkOpacity by remember { mutableFloatStateOf(0.3f) }
    var fontSize by remember { mutableFloatStateOf(22f) }
    var rows by remember { mutableStateOf<Int?>(null) }
    var cols by remember { mutableStateOf<Int?>(null) }

    var pendingBitmap by remember { mutableStateOf<Bitmap?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            if (granted) {
                pendingBitmap?.let { bitmap ->
                    scope.launch {
                        saveBitmapToGallery(context, bitmap)
                        pendingBitmap = null
                    }
                }
            } else {
                Toast.makeText(context, "需要存储权限才能保存图片", Toast.LENGTH_SHORT).show()
                pendingBitmap = null
            }
        }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("防OCR图片生成器") },
                navigationIcon = {
                    FilledTonalIconButton(onClick = { (context as? ComponentActivity)?.finish() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(
                value = originalText,
                onValueChange = { originalText = it },
                label = { Text("输入原文") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                FilterChip(
                    selected = scopeOption == HighlightScope.NONE,
                    onClick = { scopeOption = HighlightScope.NONE },
                    label = { Text("无高亮") }
                )
                FilterChip(
                    selected = scopeOption == HighlightScope.LETTER_ONLY,
                    onClick = { scopeOption = HighlightScope.LETTER_ONLY },
                    label = { Text("仅字母") }
                )
                FilterChip(
                    selected = scopeOption == HighlightScope.ORIGINAL_ONLY,
                    onClick = { scopeOption = HighlightScope.ORIGINAL_ONLY },
                    label = { Text("仅原文") }
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(checked = bold, onCheckedChange = { bold = it })
                Text("加粗")
            }

            Slider(
                value = watermarkOpacity,
                onValueChange = { watermarkOpacity = it },
                valueRange = 0.1f..0.6f,
                steps = 5,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            Text("水印不透明度: ${(watermarkOpacity * 100).toInt()}%", fontSize = 12.sp)

            Button(
                onClick = {
                    scope.launch {
                        isGenerating = true
                        val config = HighlightConfig(
                            scope = scopeOption,
                            textColor = Color.Red,
                            bgColor = Color.Yellow,
                            bold = bold,
                        )
                        val bitmap = generator.generate(
                            originalText = originalText,
                            highlightConfig = config,
                            rows = rows,
                            cols = cols,
                            rotateAngle = rotateAngle,
                            watermarkOpacity = watermarkOpacity,
                            fontSizeSp = fontSize
                        )
                        generatedBitmap = bitmap
                        isGenerating = false
                    }
                },
                enabled = originalText.isNotBlank() && !isGenerating,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isGenerating) "生成中..." else "生成图片")
            }

            Spacer(modifier = Modifier.height(16.dp))

            generatedBitmap?.let { bitmap ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "防OCR图片",
                            modifier = Modifier
                                .wrapContentSize()
                                .clip(RoundedCornerShape(8.dp))
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = {
                                    scope.launch {
                                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                                            val permission = Manifest.permission.WRITE_EXTERNAL_STORAGE
                                            if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
                                                saveBitmapToGallery(context, bitmap)
                                            } else {
                                                pendingBitmap = bitmap
                                                permissionLauncher.launch(permission)
                                            }
                                        } else {
                                            saveBitmapToGallery(context, bitmap)
                                        }
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("保存到相册")
                            }

                            Button(
                                onClick = {
                                    rotateAngle = Random.nextFloat() * 30 - 15
                                    scope.launch {
                                        isGenerating = true
                                        val config = HighlightConfig(
                                            scope = scopeOption,
                                            textColor = Color.Red,
                                            bgColor = Color.Yellow,
                                            bold = bold,
                                        )
                                        val newBitmap = generator.generate(
                                            originalText = originalText,
                                            highlightConfig = config,
                                            rows = rows,
                                            cols = cols,
                                            rotateAngle = rotateAngle,
                                            watermarkOpacity = watermarkOpacity,
                                            fontSizeSp = fontSize
                                        )
                                        generatedBitmap = newBitmap
                                        isGenerating = false
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("重新生成")
                            }
                        }
                    }
                }
            }
        }
    }
}