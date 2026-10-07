package com.lumora.iptv

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.lumora.iptv.data.iptv.IptvRepository
import com.lumora.iptv.ui.navigation.AppNavHost
import com.lumora.iptv.ui.theme.GoldyColors
import com.lumora.iptv.ui.theme.LumoraIPTVTheme

class MainActivity : ComponentActivity() {

    private lateinit var repository: IptvRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        repository = IptvRepository(applicationContext)

        setContent {
            LumoraIPTVTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = GoldyColors.BgDark
                ) {
                    AppNavHost(repository = repository)
                }
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        // Support Android TV Remote Shortcuts
        when (keyCode) {
            KeyEvent.KEYCODE_PROG_YELLOW, KeyEvent.KEYCODE_F3 -> {
                // TV shortcut button
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }
}
