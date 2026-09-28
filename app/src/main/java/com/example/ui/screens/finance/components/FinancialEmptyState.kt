package com.example.ui.screens.finance.components

import androidx.compose.foundation.Image
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.EmeraldPrimaryLight
import com.example.ui.theme.RadiusMD

@Composable
fun FinancialEmptyState(
    onAddTransaction: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp, horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 3D Empty Wallet Illustration
        Box(
            modifier = Modifier
                .size(110.dp)
                .shadow(8.dp, CircleShape, spotColor = EmeraldPrimaryLight.copy(alpha = 0.3f))
                .clip(CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_3d_empty_wallet),
                contentDescription = "کیف پول خالی",
                modifier = Modifier.size(100.dp),
                contentScale = ContentScale.Fit
            )
        }

        Text(
            text = "هنوز تراکنشی در این بازه ثبت نشده",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = "اولین هزینه یا درآمد خود را ثبت کنید تا نمودار و گزارشات شما فعال شود.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))

        Button(
            onClick = onAddTransaction,
            shape = RoundedCornerShape(RadiusMD),
            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimaryLight),
            modifier = Modifier.testTag("empty_state_add_tx_button")
        ) {
            Text(
                text = "+ ثبت اولین تراکنش",
                style = MaterialTheme.typography.titleSmall,
                color = androidx.compose.ui.graphics.Color.White
            )
        }
    }
}
