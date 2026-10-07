package com.lumora.iptv.ui.account

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lumora.iptv.data.iptv.IptvRepository
import com.lumora.iptv.ui.theme.GoldyColors
import kotlinx.coroutines.launch

@Composable
fun AccountScreen(
    repository: IptvRepository,
    onBack: () -> Unit,
    onLoggedOut: () -> Unit
) {
    BackHandler { onBack() }

    val scope = rememberCoroutineScope()
    val account by repository.accountFlow.collectAsState(initial = null)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GoldyColors.BgDark)
            .padding(16.dp)
            .testTag("account_screen")
    ) {
        // Top Bar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(GoldyColors.PanelDark1)
                    .testTag("account_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = GoldyColors.CyanLight
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = "إدارة الحساب (Account)",
                color = GoldyColors.TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Account Details Card (Section 37)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(GoldyColors.PanelDark1)
                .border(1.dp, GoldyColors.Cyan.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                .padding(24.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(GoldyColors.BtnGradient1)
                    .border(2.dp, GoldyColors.Cyan, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = null,
                    tint = GoldyColors.CyanLight,
                    modifier = Modifier.size(54.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = account?.username ?: "Guest User",
                color = GoldyColors.TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = account?.serverName ?: "IPTV Server",
                color = GoldyColors.CyanLight,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            AccountInfoRow(label = "حالة الاشتراك", value = account?.status ?: "نشط")
            AccountInfoRow(label = "تاريخ انتهاء الصلاحية", value = account?.expiryDate ?: "غير محدد")
            AccountInfoRow(label = "السيرفر", value = account?.host ?: "غير محدد")

            Spacer(modifier = Modifier.height(32.dp))

            // Logout Button (Section 37)
            Button(
                onClick = {
                    scope.launch {
                        repository.logout()
                        onLoggedOut()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().testTag("logout_button")
            ) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Logout", tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "تسجيل الخروج ومسح الجلسة", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun AccountInfoRow(label: String, value: String) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(text = label, color = GoldyColors.TextMuted, fontSize = 13.sp)
        Text(text = value, color = GoldyColors.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}
