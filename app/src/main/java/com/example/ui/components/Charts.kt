package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.util.CurrencyHelper

val ChartPalette = listOf(
    Color(0xFFE57373),
    Color(0xFFFFB74D),
    Color(0xFF81C784),
    Color(0xFF4DD0E1),
    Color(0xFFBA68C8),
    Color(0xFF7986CB),
    Color(0xFFFFD54F),
    Color(0xFFF06292),
    Color(0xFF4DB6AC),
    Color(0xFFAED581)
)

@Composable
fun CategoryDonutChart(
    categoryBreakdown: List<Pair<String, Long>>,
    totalExpenseMinor: Long,
    currencyCode: String,
    modifier: Modifier = Modifier
) {
    if (categoryBreakdown.isEmpty() || totalExpenseMinor <= 0) {
        Box(
            modifier = modifier.fillMaxWidth().height(180.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("No expense data for this period", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    val animatedProgress = remember { Animatable(0f) }
    LaunchedEffect(categoryBreakdown) {
        animatedProgress.animateTo(1f, animationSpec = tween(durationMillis = 800))
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(140.dp)
        ) {
            Canvas(modifier = Modifier.size(130.dp)) {
                var startAngle = -90f
                val strokeWidth = 28.dp.toPx()
                val radius = (size.minDimension - strokeWidth) / 2
                val center = Offset(size.width / 2, size.height / 2)

                for ((index, item) in categoryBreakdown.withIndex()) {
                    val sweep = (item.second.toFloat() / totalExpenseMinor.toFloat()) * 360f * animatedProgress.value
                    val color = ChartPalette[index % ChartPalette.size]

                    drawArc(
                        color = color,
                        startAngle = startAngle,
                        sweepAngle = sweep,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Butt),
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = Size(radius * 2, radius * 2)
                    )
                    startAngle += sweep
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Total",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = CurrencyHelper.formatMinorUnits(totalExpenseMinor, currencyCode),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Legend
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            categoryBreakdown.take(5).forEachIndexed { index, (catName, amount) ->
                val percentage = if (totalExpenseMinor > 0) (amount.toDouble() / totalExpenseMinor * 100).toInt() else 0
                val color = ChartPalette[index % ChartPalette.size]

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(color, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = catName,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f),
                        maxLines = 1
                    )
                    Text(
                        text = "$percentage%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun IncomeVsExpenseBarChart(
    totalIncomeMinor: Long,
    totalExpenseMinor: Long,
    currencyCode: String,
    modifier: Modifier = Modifier
) {
    val maxVal = Math.max(1L, Math.max(totalIncomeMinor, totalExpenseMinor)).toFloat()
    val animatedProgress = remember { Animatable(0f) }
    LaunchedEffect(totalIncomeMinor, totalExpenseMinor) {
        animatedProgress.animateTo(1f, animationSpec = tween(durationMillis = 800))
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Income vs Expense",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Income Bar
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Income", style = MaterialTheme.typography.bodySmall, color = Color(0xFF10B981))
                    Text(
                        CurrencyHelper.formatMinorUnits(totalIncomeMinor, currencyCode),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF10B981)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(14.dp)
                ) {
                    drawRoundRect(
                        color = Color(0xFFE2E8F0),
                        cornerRadius = CornerRadius(8f, 8f),
                        size = size
                    )
                    val width = (totalIncomeMinor / maxVal) * size.width * animatedProgress.value
                    drawRoundRect(
                        color = Color(0xFF10B981),
                        cornerRadius = CornerRadius(8f, 8f),
                        size = Size(width, size.height)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Expense Bar
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Expense", style = MaterialTheme.typography.bodySmall, color = Color(0xFFEF4444))
                    Text(
                        CurrencyHelper.formatMinorUnits(totalExpenseMinor, currencyCode),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFEF4444)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(14.dp)
                ) {
                    drawRoundRect(
                        color = Color(0xFFE2E8F0),
                        cornerRadius = CornerRadius(8f, 8f),
                        size = size
                    )
                    val width = (totalExpenseMinor / maxVal) * size.width * animatedProgress.value
                    drawRoundRect(
                        color = Color(0xFFEF4444),
                        cornerRadius = CornerRadius(8f, 8f),
                        size = Size(width, size.height)
                    )
                }
            }
        }
    }
}
