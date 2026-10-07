package com.lumora.iptv

import com.lumora.iptv.bridge.GoldyBridge
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
class GoldyBridgeTest {

    @Test
    fun testOnNavigateSanitization_stripsQuotes() {
        var navigatedKey: String? = null

        val bridge = GoldyBridge(
            onOpenAction = {},
            onNavigateAction = { key ->
                navigatedKey = key
            },
            onMenuAction = {},
            onAppsAction = {}
        )

        // goldy.html sends JSON.stringify("movies") -> "\"movies\""
        bridge.onNavigate("\"movies\"")
        ShadowLooper.idleMainLooper()

        assertEquals("movies", navigatedKey)
    }

    @Test
    fun testOnNavigateSanitization_blocksUnpermittedKeys() {
        var navigatedKey: String? = null

        val bridge = GoldyBridge(
            onOpenAction = {},
            onNavigateAction = { key ->
                navigatedKey = key
            },
            onMenuAction = {},
            onAppsAction = {}
        )

        bridge.onNavigate("\"hack_admin\"")
        ShadowLooper.idleMainLooper()

        assertNull(navigatedKey)
    }

    @Test
    fun testOnMenu_acceptsStringPayloadWithoutCrashing() {
        var menuCalled = false

        val bridge = GoldyBridge(
            onOpenAction = {},
            onNavigateAction = {},
            onMenuAction = {
                menuCalled = true
            },
            onAppsAction = {}
        )

        // goldy.html sends JSON.stringify({}) = "{}"
        bridge.onMenu("{}")
        ShadowLooper.idleMainLooper()

        assertEquals(true, menuCalled)
    }

    @Test
    fun testOnApps_acceptsStringPayloadWithoutCrashing() {
        var appsCalled = false

        val bridge = GoldyBridge(
            onOpenAction = {},
            onNavigateAction = {},
            onMenuAction = {},
            onAppsAction = {
                appsCalled = true
            }
        )

        bridge.onApps("{}")
        ShadowLooper.idleMainLooper()

        assertEquals(true, appsCalled)
    }

    @Test
    fun testOnOpen_extractsIdSuccessfully() {
        var openedId: String? = null

        val bridge = GoldyBridge(
            onOpenAction = { id ->
                openedId = id
            },
            onNavigateAction = {},
            onMenuAction = {},
            onAppsAction = {}
        )

        val json = """{"id":"4582","title":"Inception","img":"http://example.com/poster.jpg"}"""
        bridge.onOpen(json)
        ShadowLooper.idleMainLooper()

        assertEquals("4582", openedId)
    }
}
