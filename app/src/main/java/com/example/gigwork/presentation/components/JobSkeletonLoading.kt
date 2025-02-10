package com.example.gigwork.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun JobSkeletonList(
    itemCount: Int = 3,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        repeat(itemCount) {
            JobSkeletonItem()
        }
    }
}

@Composable
fun JobSkeletonItem(
    modifier: Modifier = Modifier
) {
    val shimmerBrush = rememberShimmerBrush()

    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Title skeleton
            ShimmerBox(
                brush = shimmerBrush,
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(24.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Salary skeleton with icon
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Salary icon skeleton
                ShimmerBox(
                    brush = shimmerBrush,
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Salary amount skeleton
                ShimmerBox(
                    brush = shimmerBrush,
                    modifier = Modifier
                        .width(100.dp)
                        .height(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Location and duration row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Location with icon
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    ShimmerBox(
                        brush = shimmerBrush,
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    ShimmerBox(
                        brush = shimmerBrush,
                        modifier = Modifier
                            .width(120.dp)
                            .height(16.dp)
                    )
                }

                // Duration skeleton
                ShimmerBox(
                    brush = shimmerBrush,
                    modifier = Modifier
                        .width(80.dp)
                        .height(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Status chip skeleton
            ShimmerBox(
                brush = shimmerBrush,
                modifier = Modifier
                    .width(60.dp)
                    .height(24.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
        }
    }
}

@Composable
private fun ShimmerBox(
    brush: Brush,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(brush)
    )
}

@Composable
private fun rememberShimmerBrush(
    shimmerColors: List<Color> = listOf(
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    ),
    animationSpec: InfiniteRepeatableSpec<Float> = infiniteRepeatable(
        animation = tween(
            durationMillis = 1200,
            easing = FastOutSlowInEasing,
            delayMillis = 300
        ),
        repeatMode = RepeatMode.Restart
    )
): Brush {
    val transition = rememberInfiniteTransition()
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = animationSpec
    )

    return Brush.linearGradient(
        colors = shimmerColors,
        start = Offset.Zero,
        end = Offset(x = translateAnim, y = translateAnim)
    )
}

@Composable
fun JobDetailsSkeleton(
    modifier: Modifier = Modifier
) {
    val shimmerBrush = rememberShimmerBrush()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Title skeleton
        ShimmerBox(
            brush = shimmerBrush,
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .height(32.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Company info skeleton
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            ShimmerBox(
                brush = shimmerBrush,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                ShimmerBox(
                    brush = shimmerBrush,
                    modifier = Modifier
                        .width(150.dp)
                        .height(20.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                ShimmerBox(
                    brush = shimmerBrush,
                    modifier = Modifier
                        .width(100.dp)
                        .height(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Job details skeleton
        repeat(3) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                ShimmerBox(
                    brush = shimmerBrush,
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                )

                Spacer(modifier = Modifier.width(16.dp))

                ShimmerBox(
                    brush = shimmerBrush,
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Description skeleton
        repeat(4) {
            ShimmerBox(
                brush = shimmerBrush,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp)
                    .padding(vertical = 4.dp)
            )
        }
    }
}