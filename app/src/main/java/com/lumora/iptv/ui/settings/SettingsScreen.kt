package com.lumora.iptv.ui.settings

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lumora.iptv.data.iptv.IptvRepository
import com.lumora.iptv.ui.theme.GoldyColors
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    repository: IptvRepository,
    onBack: () -> Unit,
    onNavigateToAccount: () -> Unit,
    onSyncComplete: () -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val syncState by repository.syncState.collectAsState()
    val account by repository.accountFlow.collectAsState(initial = null)

    var serverUrl by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var m3uUrl by remember { mutableStateOf("") }
    var selectedSource by remember { mutableStateOf("xtream") } // "xtream" or "m3u"

    androidx.compose.runtime.LaunchedEffect(Unit) {
        val creds = repository.getCredentials()
        if (creds != null) {
            serverUrl = creds.serverUrl
            username = creds.username
            password = creds.password
            m3uUrl = creds.m3uUrl ?: ""
            selectedSource = creds.sourceType
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GoldyColors.BgDark)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
            .testTag("settings_screen")
    ) {
        // Top Bar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(GoldyColors.PanelDark1)
                    .testTag("settings_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = GoldyColors.CyanLight
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = "الإعدادات (Settings)",
                color = GoldyColors.TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Section 1: Account Info Card
        SectionCard(title = "الحساب (Account)", icon = Icons.Default.AccountCircle) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = "المستخدم: ${account?.username ?: "غير مسجل"}",
                        color = GoldyColors.TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "تاريخ الانتهاء: ${account?.expiryDate ?: "غير محدد"}",
                        color = GoldyColors.CyanLight,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "الحالة: ${account?.status ?: "جاهز"}",
                        color = GoldyColors.TextMuted,
                        fontSize = 12.sp
                    )
                }

                Button(
                    onClick = onNavigateToAccount,
                    colors = ButtonDefaults.buttonColors(containerColor = GoldyColors.BtnGradient1),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("manage_account_button")
                ) {
                    Text("إدارة الحساب", color = GoldyColors.CyanLight, fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Section 2: IPTV Connection
        SectionCard(title = "بيانات سيرفر IPTV", icon = Icons.Default.LiveTv) {
            // Source selector
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("xtream" to "Xtream Codes API", "m3u" to "M3U Playlist").forEach { (type, label) ->
                    val isSel = selectedSource == type
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSel) GoldyColors.BtnActive1 else GoldyColors.PanelDark2)
                            .border(1.dp, if (isSel) GoldyColors.Cyan else Color.Transparent, RoundedCornerShape(8.dp))
                            .clickable { selectedSource = type }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                            .focusable()
                    ) {
                        Text(
                            text = label,
                            color = if (isSel) Color.White else GoldyColors.TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (selectedSource == "xtream") {
                OutlinedTextField(
                    value = serverUrl,
                    onValueChange = { serverUrl = it },
                    label = { Text("Server URL (رابط السيرفر)") },
                    placeholder = { Text("http://example.com:8080") },
                    colors = textFieldColors(),
                    modifier = Modifier.fillMaxWidth().testTag("server_url_input")
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username (اسم المستخدم)") },
                    colors = textFieldColors(),
                    modifier = Modifier.fillMaxWidth().testTag("username_input")
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password (كلمة المرور)") },
                    visualTransformation = PasswordVisualTransformation(),
                    colors = textFieldColors(),
                    modifier = Modifier.fillMaxWidth().testTag("password_input")
                )
            } else {
                OutlinedTextField(
                    value = m3uUrl,
                    onValueChange = { m3uUrl = it },
                    label = { Text("رابط قائمة M3U أو المسار") },
                    placeholder = { Text("http://example.com/playlist.m3u") },
                    colors = textFieldColors(),
                    modifier = Modifier.fillMaxWidth().testTag("m3u_url_input")
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = {
                    scope.launch {
                        if (selectedSource == "xtream") {
                            if (serverUrl.isBlank() || username.isBlank() || password.isBlank()) {
                                Toast.makeText(context, "يرجى ملء جميع حقول Xtream", Toast.LENGTH_SHORT).show()
                                return@launch
                            }
                            repository.syncXtream(serverUrl, username, password).onSuccess { onSyncComplete() }
                        } else {
                            if (m3uUrl.isBlank()) {
                                Toast.makeText(context, "يرجى إدخال رابط M3U", Toast.LENGTH_SHORT).show()
                                return@launch
                            }
                            repository.syncM3u(m3uUrl).onSuccess { onSyncComplete() }
                        }
                    }
                },
                enabled = !syncState.isSyncing,
                colors = ButtonDefaults.buttonColors(containerColor = GoldyColors.BtnActive2),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().testTag("sync_iptv_button")
            ) {
                if (syncState.isSyncing) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(syncState.message, color = Color.White)
                } else {
                    Icon(imageVector = Icons.Default.Sync, contentDescription = "Sync", tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("حفظ ومزامنة القنوات والمحتوى", color = Color.White)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Section 3: Player & Subtitles & Audio
        SectionCard(title = "المشغّل والترجمة والصوت", icon = Icons.Default.PlayCircle) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "• دعم كامل للمسارات الصوتية وتغيير لغة المعلق داخل المشغّل",
                    color = GoldyColors.TextSecondary,
                    fontSize = 13.sp
                )
                Text(
                    text = "• دعم ترجمات WebVTT و SRT مع التحكم في تأخير الترجمة (Delay offset)",
                    color = GoldyColors.TextSecondary,
                    fontSize = 13.sp
                )
                Text(
                    text = "• محرك تشغيل Media3 ExoPlayer متوافق مع Android TV و D-Pad",
                    color = GoldyColors.TextSecondary,
                    fontSize = 13.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Section 4: About
        SectionCard(title = "حول التطبيق (About)", icon = Icons.Default.Info) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = "Lumora IPTV Player", color = GoldyColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(text = "الإصدار: 1.0 (Production Build)", color = GoldyColors.CyanLight, fontSize = 12.sp)
                Text(text = "الهندسة: Hybrid Architecture (Goldy HTML + Native Compose + Media3)", color = GoldyColors.TextMuted, fontSize = 11.sp)
            }
        }
    }
}

@Composable
fun SectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(GoldyColors.PanelDark1)
            .border(1.dp, GoldyColors.Cyan.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 10.dp)) {
            Icon(imageVector = icon, contentDescription = null, tint = GoldyColors.Cyan, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = title, color = GoldyColors.CyanLight, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
        content()
    }
}

@Composable
fun textFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = GoldyColors.TextPrimary,
    unfocusedTextColor = GoldyColors.TextPrimary,
    focusedBorderColor = GoldyColors.Cyan,
    unfocusedBorderColor = GoldyColors.Cyan.copy(alpha = 0.3f),
    focusedLabelColor = GoldyColors.Cyan,
    unfocusedLabelColor = GoldyColors.TextMuted,
    focusedPlaceholderColor = GoldyColors.TextMuted,
    unfocusedPlaceholderColor = GoldyColors.TextMuted
)
