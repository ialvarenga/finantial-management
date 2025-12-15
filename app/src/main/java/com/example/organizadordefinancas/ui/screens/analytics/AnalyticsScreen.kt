package com.example.organizadordefinancas.ui.screens.analytics

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.organizadordefinancas.data.model.AnalyticsPeriod
import com.example.organizadordefinancas.data.model.CategoryTotal
import com.example.organizadordefinancas.data.model.MerchantTotal
import com.example.organizadordefinancas.data.model.MonthlyTotal
import com.example.organizadordefinancas.ui.components.formatCurrency
import com.example.organizadordefinancas.ui.viewmodel.AnalyticsUiState
import com.example.organizadordefinancas.ui.viewmodel.AnalyticsViewModel
import java.util.Locale

// Chart colors palette
private val chartColors = listOf(
    Color(0xFFE91E63),
    Color(0xFF2196F3),
    Color(0xFF4CAF50),
    Color(0xFFFF9800),
    Color(0xFF9C27B0),
    Color(0xFF00BCD4),
    Color(0xFFFFEB3B),
    Color(0xFF795548),
    Color(0xFF607D8B),
    Color(0xFFF44336)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Análises") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Period Selector
            PeriodSelector(
                selectedPeriod = uiState.selectedPeriod,
                onPeriodSelected = { viewModel.selectPeriod(it) }
            )

            // Summary Cards
            SummaryCardsSection(uiState)

            // Tab Row for different charts
            TabRow(
                selectedTabIndex = selectedTabIndex,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text("Categorias") }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text("Histórico") }
                )
                Tab(
                    selected = selectedTabIndex == 2,
                    onClick = { selectedTabIndex = 2 },
                    text = { Text("Top Gastos") }
                )
            }

            // Chart content based on selected tab
            when (selectedTabIndex) {
                0 -> CategoryChartSection(uiState.categoryTotals)
                1 -> MonthlyChartSection(uiState.monthlyTotals)
                2 -> TopMerchantsSection(uiState.topMerchants)
            }
        }
    }
}

@Composable
private fun PeriodSelector(
    selectedPeriod: AnalyticsPeriod,
    onPeriodSelected: (AnalyticsPeriod) -> Unit
) {
    SingleChoiceSegmentedButtonRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        AnalyticsPeriod.entries.forEachIndexed { index, period ->
            SegmentedButton(
                selected = selectedPeriod == period,
                onClick = { onPeriodSelected(period) },
                shape = SegmentedButtonDefaults.itemShape(
                    index = index,
                    count = AnalyticsPeriod.entries.size
                )
            ) {
                Text(
                    text = period.displayName,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@Composable
private fun SummaryCardsSection(uiState: AnalyticsUiState) {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Resumo do Período",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AnalyticsSummaryCard(
                title = "Total Gasto",
                value = formatCurrency(uiState.totalSpending),
                icon = Icons.Default.AttachMoney,
                containerColor = Color(0xFFE91E63),
                modifier = Modifier.weight(1f)
            )
            AnalyticsSummaryCard(
                title = "Transações",
                value = uiState.transactionCount.toString(),
                icon = Icons.Default.Receipt,
                containerColor = Color(0xFF2196F3),
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AnalyticsSummaryCard(
                title = "Média",
                value = formatCurrency(uiState.averageSpending),
                icon = Icons.AutoMirrored.Filled.TrendingUp,
                containerColor = Color(0xFF4CAF50),
                modifier = Modifier.weight(1f)
            )
            AnalyticsSummaryCard(
                title = "Maior Gasto",
                value = formatCurrency(uiState.maxSpending),
                icon = Icons.Default.ShoppingCart,
                containerColor = Color(0xFFFF9800),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun AnalyticsSummaryCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    containerColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.8f)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun CategoryChartSection(categoryTotals: List<CategoryTotal>) {
    Column(
        modifier = Modifier.padding(16.dp)
    ) {
        Text(
            text = "Gastos por Categoria",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (categoryTotals.isEmpty()) {
            EmptyChartMessage("Nenhum gasto registrado no período")
        } else {
            // Pie Chart
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    PieChart(
                        data = categoryTotals,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Category breakdown list with bar representation
            CategoryBreakdownList(categoryTotals)
        }
    }
}

@Composable
private fun PieChart(
    data: List<CategoryTotal>,
    modifier: Modifier = Modifier
) {
    val total = data.sumOf { it.total }.toFloat()
    if (total == 0f) return

    var animationProgress by remember { mutableFloatStateOf(0f) }
    val animatedProgress by animateFloatAsState(
        targetValue = animationProgress,
        animationSpec = tween(durationMillis = 1000),
        label = "pie_animation"
    )

    LaunchedEffect(data) {
        animationProgress = 1f
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Pie chart
        Canvas(
            modifier = Modifier
                .size(150.dp)
                .padding(8.dp)
        ) {
            var startAngle = -90f
            data.forEachIndexed { index, category ->
                val sweepAngle = (category.total.toFloat() / total) * 360f * animatedProgress
                val color = chartColors[index % chartColors.size]

                drawArc(
                    color = color,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = true,
                    size = Size(size.minDimension, size.minDimension),
                    topLeft = Offset(
                        (size.width - size.minDimension) / 2,
                        (size.height - size.minDimension) / 2
                    )
                )
                startAngle += sweepAngle
            }
        }

        // Legend
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            data.take(6).forEachIndexed { index, category ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(chartColors[index % chartColors.size])
                    )
                    Text(
                        text = category.category,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            if (data.size > 6) {
                Text(
                    text = "+${data.size - 6} mais",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CategoryBreakdownList(categoryTotals: List<CategoryTotal>) {
    val total = categoryTotals.sumOf { it.total }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Detalhamento",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            categoryTotals.forEachIndexed { index, category ->
                val percentage = if (total > 0) (category.total / total * 100) else 0.0
                CategoryBreakdownItem(
                    category = category.category,
                    amount = category.total,
                    percentage = percentage,
                    color = chartColors[index % chartColors.size]
                )
            }
        }
    }
}

@Composable
private fun CategoryBreakdownItem(
    category: String,
    amount: Double,
    percentage: Double,
    color: Color
) {
    var animationProgress by remember { mutableFloatStateOf(0f) }
    val animatedProgress by animateFloatAsState(
        targetValue = animationProgress,
        animationSpec = tween(durationMillis = 800),
        label = "bar_animation"
    )

    LaunchedEffect(percentage) {
        animationProgress = 1f
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(color)
                )
                Text(
                    text = category,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth((percentage / 100 * animatedProgress).toFloat())
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(3.dp))
                        .background(color)
                )
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = formatCurrency(amount),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = String.format(Locale.getDefault(), "%.1f%%", percentage),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MonthlyChartSection(monthlyTotals: List<MonthlyTotal>) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = "Histórico Mensal",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (monthlyTotals.isEmpty()) {
            EmptyChartMessage("Nenhum dado disponível")
        } else {
            // Line Chart
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    LineChart(
                        data = monthlyTotals,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Monthly breakdown
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Detalhamento Mensal",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    monthlyTotals.reversed().forEach { monthly ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = formatMonthLabel(monthly.month),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = formatCurrency(monthly.total),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LineChart(
    data: List<MonthlyTotal>,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) return

    val maxValue = data.maxOfOrNull { it.total } ?: 0.0
    if (maxValue == 0.0) return

    var animationProgress by remember { mutableFloatStateOf(0f) }
    val animatedProgress by animateFloatAsState(
        targetValue = animationProgress,
        animationSpec = tween(durationMillis = 1000),
        label = "line_animation"
    )

    LaunchedEffect(data) {
        animationProgress = 1f
    }

    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant

    Canvas(modifier = modifier.padding(start = 40.dp, end = 8.dp, top = 8.dp, bottom = 24.dp)) {
        val chartWidth = size.width
        val chartHeight = size.height
        val pointSpacing = if (data.size > 1) chartWidth / (data.size - 1) else chartWidth

        // Draw horizontal grid lines
        for (i in 0..4) {
            val y = chartHeight * i / 4
            drawLine(
                color = gridColor,
                start = Offset(0f, y),
                end = Offset(chartWidth, y),
                strokeWidth = 1f
            )
        }

        // Draw line and points
        val path = Path()
        data.forEachIndexed { index, monthly ->
            val x = index * pointSpacing
            val y = chartHeight - (monthly.total.toFloat() / maxValue.toFloat() * chartHeight * animatedProgress)

            if (index == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }

            // Draw point
            drawCircle(
                color = lineColor,
                radius = 6f,
                center = Offset(x, y)
            )
        }

        // Draw line
        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 3f, cap = StrokeCap.Round)
        )

        // Draw labels
        data.forEachIndexed { index, monthly ->
            val x = index * pointSpacing
            drawContext.canvas.nativeCanvas.apply {
                drawText(
                    formatMonthLabel(monthly.month),
                    x,
                    chartHeight + 40f,
                    android.graphics.Paint().apply {
                        color = android.graphics.Color.GRAY
                        textSize = 24f
                        textAlign = android.graphics.Paint.Align.CENTER
                    }
                )
            }
        }
    }
}

@Composable
private fun TopMerchantsSection(topMerchants: List<MerchantTotal>) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = "Maiores Gastos por Estabelecimento",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (topMerchants.isEmpty()) {
            EmptyChartMessage("Nenhum gasto registrado no período")
        } else {
            // Horizontal Bar Chart
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    HorizontalBarChart(
                        data = topMerchants.take(5),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Merchant ranking list
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Ranking de Estabelecimentos",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    topMerchants.forEachIndexed { index, merchant ->
                        MerchantRankingItem(rank = index + 1, merchant = merchant)
                    }
                }
            }
        }
    }
}

@Composable
private fun HorizontalBarChart(
    data: List<MerchantTotal>,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) return

    val maxValue = data.maxOfOrNull { it.total } ?: 0.0
    if (maxValue == 0.0) return

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        data.forEachIndexed { index, merchant ->
            var animationProgress by remember { mutableFloatStateOf(0f) }
            val animatedProgress by animateFloatAsState(
                targetValue = animationProgress,
                animationSpec = tween(durationMillis = 800, delayMillis = index * 100),
                label = "bar_animation_$index"
            )

            LaunchedEffect(merchant) {
                animationProgress = 1f
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = merchant.description.take(12),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.width(80.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(20.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth((merchant.total / maxValue * animatedProgress).toFloat())
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(4.dp))
                            .background(chartColors[index % chartColors.size])
                    )
                }
                Text(
                    text = formatCurrency(merchant.total),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(start = 8.dp),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun MerchantRankingItem(rank: Int, merchant: MerchantTotal) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Rank badge
        Surface(
            shape = MaterialTheme.shapes.small,
            color = when (rank) {
                1 -> Color(0xFFFFD700)
                2 -> Color(0xFFC0C0C0)
                3 -> Color(0xFFCD7F32)
                else -> MaterialTheme.colorScheme.surfaceVariant
            },
            modifier = Modifier.size(28.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = rank.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (rank <= 3) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = merchant.description,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${merchant.count} transações",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Text(
            text = formatCurrency(merchant.total),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun EmptyChartMessage(message: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatMonthLabel(yearMonth: String): String {
    return try {
        val parts = yearMonth.split("-")
        val year = parts[0].takeLast(2)
        val month = when (parts[1]) {
            "01" -> "Jan"
            "02" -> "Fev"
            "03" -> "Mar"
            "04" -> "Abr"
            "05" -> "Mai"
            "06" -> "Jun"
            "07" -> "Jul"
            "08" -> "Ago"
            "09" -> "Set"
            "10" -> "Out"
            "11" -> "Nov"
            "12" -> "Dez"
            else -> parts[1]
        }
        "$month/$year"
    } catch (_: Exception) {
        yearMonth
    }
}

