package com.example.toolbox.function.daily

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.toolbox.ui.theme.ToolBoxTheme

class BMIActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ToolBoxTheme {
                BMICalculatorScreen(
                    onBackClick = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BMICalculatorScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {}
) {
    var height by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var bmiResult by remember { mutableStateOf<Double?>(null) }
    var bmiCategory by remember { mutableStateOf("") }

    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("BMI计算器") },
                navigationIcon = {
                    FilledTonalIconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (bmiResult != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = when (bmiCategory) {
                            "正常范围" -> MaterialTheme.colorScheme.tertiaryContainer
                            "体重过轻" -> MaterialTheme.colorScheme.secondaryContainer
                            "超重" -> MaterialTheme.colorScheme.errorContainer
                            "肥胖" -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f)
                            "重度肥胖" -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f)
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    ),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(24.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "您的BMI指数",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = bmiResult.toString(),
                            fontSize = 48.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = when (bmiCategory) {
                                "正常范围" -> MaterialTheme.colorScheme.tertiary
                                "体重过轻" -> MaterialTheme.colorScheme.secondary
                                else -> MaterialTheme.colorScheme.error
                            }
                        )

                        Text(
                            text = bmiCategory,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = when (bmiCategory) {
                                "正常范围" -> MaterialTheme.colorScheme.tertiary
                                "体重过轻" -> MaterialTheme.colorScheme.secondary
                                else -> MaterialTheme.colorScheme.error
                            }
                        )
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "请输入您的身高和体重",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = height,
                        onValueChange = { newValue ->
                            if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d*$"))) {
                                height = newValue
                            }
                        },
                        label = { Text("身高 (厘米)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        suffix = { Text("cm") }
                    )

                    OutlinedTextField(
                        value = weight,
                        onValueChange = { newValue ->
                            if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d*$"))) {
                                weight = newValue
                            }
                        },
                        label = { Text("体重 (公斤)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        suffix = { Text("kg") }
                    )

                    Button(
                        onClick = {
                            val h = height.toDoubleOrNull()
                            val w = weight.toDoubleOrNull()

                            if (h != null && w != null && h > 0 && w > 0) {
                                val heightInMeters = h / 100
                                val bmi = w / (heightInMeters * heightInMeters)
                                bmiResult = String.format("%.2f", bmi).toDouble()

                                bmiCategory = when {
                                    bmi < 18.5 -> "体重过轻"
                                    bmi < 24 -> "正常范围"
                                    bmi < 28 -> "超重"
                                    bmi < 32 -> "肥胖"
                                    else -> "重度肥胖"
                                }
                            } else {
                                bmiResult = null
                                bmiCategory = "请输入有效的身高和体重"
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = height.isNotEmpty() && weight.isNotEmpty()
                    ) {
                        Text("计算BMI")
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "BMI分类标准",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    BmiCategoryRow("体重过轻", "< 18.5", MaterialTheme.colorScheme.secondary)
                    BmiCategoryRow("正常范围", "18.5 - 24", MaterialTheme.colorScheme.tertiary)
                    BmiCategoryRow("超重", "24 - 28", MaterialTheme.colorScheme.error)
                    BmiCategoryRow("肥胖", "28 - 32", MaterialTheme.colorScheme.error.copy(alpha = 0.8f))
                    BmiCategoryRow("重度肥胖", "≥ 32", MaterialTheme.colorScheme.error.copy(alpha = 0.9f))
                }
            }

            // 说明文字
            Text(
                text = "BMI（身体质量指数）是世界卫生组织推荐的国际统一使用的肥胖分型标准。",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
    }
}

@Composable
fun BmiCategoryRow(category: String, range: String, color: androidx.compose.ui.graphics.Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = color,
                modifier = Modifier.width(12.dp).height(12.dp)
            ) {}
            Text(category, fontWeight = FontWeight.Medium)
        }
        Text(range, fontWeight = FontWeight.Medium)
    }
}

@Preview(showBackground = true)
@Composable
fun BMICalculatorPreview() {
    ToolBoxTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            BMICalculatorScreen()
        }
    }
}