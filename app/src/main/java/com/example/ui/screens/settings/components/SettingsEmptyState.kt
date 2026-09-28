package com.example.ui.screens.settings.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ButtonShape
import com.example.ui.theme.RadiusLG

@Composable
fun SettingsEmptyState(
    title: String,
    description: String,
    @DrawableRes iconRes: Int? = null,
    iconEmoji: String? = null,
    actionButtonText: String? = null,
    onActionClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(90.dp)
                .shadow(
                    elevation = if (isDark) 8.dp else 4.dp,
                    shape = RoundedCornerShape(RadiusLG),
                    ambientColor = Color.Black.copy(alpha = 0.2f),
                    spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                )
                .clip(RoundedCornerShape(RadiusLG))
                .background(
                    if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color.White
                ),
            contentAlignment = Alignment.Center
        ) {
            when {
                iconRes != null -> {
                    Image(
                        painter = painterResource(id = iconRes),
                        contentDescription = title,
                        modifier = Modifier.size(72.dp),
                        contentScale = ContentScale.Crop
                    )
                }
                iconEmoji != null -> {
                    Text(text = iconEmoji, fontSize = 42.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            ),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 12.5.sp,
                lineHeight = 18.sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        if (actionButtonText != null) {
            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = onActionClick,
                shape = ButtonShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.testTag("empty_state_action_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.size(6.dp))
                Text(
                    text = actionButtonText,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                )
            }
        }
    }
}
