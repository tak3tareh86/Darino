package com.example.ui.screens.home.components

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.ui.components.shimmerBrush
import com.example.ui.theme.RadiusLG
import com.example.ui.theme.RadiusMD

@Composable
fun HomeSkeletonLoading(
    modifier: Modifier = Modifier
) {
    val shimmer = shimmerBrush()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Summary Card Skeleton
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(RadiusLG))
                .background(shimmer)
        )

        // Quick Actions Skeleton
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            repeat(4) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(72.dp)
                        .clip(RoundedCornerShape(RadiusMD))
                        .background(shimmer)
                )
            }
        }

        // 2x2 Grid Skeleton
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            repeat(2) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(140.dp)
                            .clip(RoundedCornerShape(RadiusLG))
                            .background(shimmer)
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(140.dp)
                            .clip(RoundedCornerShape(RadiusLG))
                            .background(shimmer)
                    )
                }
            }
        }

        // Upcoming rows skeleton
        repeat(2) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp)
                    .clip(RoundedCornerShape(RadiusMD))
                    .background(shimmer)
            )
        }
    }
}
