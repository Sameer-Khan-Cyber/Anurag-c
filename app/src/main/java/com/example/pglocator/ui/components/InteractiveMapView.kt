package com.example.pglocator.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.BrandPrimary

/**
 * JS Interface bridge to relay pin tap/drag coordinate events back to Jetpack Compose
 */
class MapLocationBridge(
    private val onLocationChanged: ((Double, Double) -> Unit)?
) {
    private val mainHandler = Handler(Looper.getMainLooper())

    @JavascriptInterface
    fun onPinMoved(lat: Double, lng: Double) {
        mainHandler.post {
            onLocationChanged?.invoke(lat, lng)
        }
    }
}

/**
 * InteractiveMapView:
 * High-performance embedded interactive Map component using OpenStreetMap & Leaflet.
 * Eliminates Google Play Services API key authorization failures while providing
 * smooth panning, zooming, interactive pin placement, and 1-tap navigation launch
 * to the Google Maps application.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun InteractiveMapView(
    latitude: Double,
    longitude: Double,
    modifier: Modifier = Modifier,
    isInteractive: Boolean = false,
    title: String = "PG Location",
    onLocationChanged: ((Double, Double) -> Unit)? = null
) {
    val context = LocalContext.current
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    val currentOnLocationChanged by rememberUpdatedState(onLocationChanged)

    // Keep camera and marker synchronized when coordinates change from Compose
    LaunchedEffect(latitude, longitude) {
        webViewRef?.evaluateJavascript(
            "if (typeof updatePosition === 'function') { updatePosition($latitude, $longitude); }",
            null
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .testTag("interactive_map_card"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column {
            // Header bar with coordinates and External Google Maps launch button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BrandPrimary)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Map Pin",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Map View",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = Color.White.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = String.format("%.4f, %.4f", latitude, longitude),
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Button to launch external Google Maps App for turn-by-turn navigation
                    IconButton(
                        onClick = {
                            val uri = Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude(${Uri.encode(title)})")
                            val mapIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                                setPackage("com.google.android.apps.maps")
                            }
                            try {
                                context.startActivity(mapIntent)
                            } catch (_: Exception) {
                                // Fallback without package restriction or to browser Google Maps
                                val fallbackUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=$latitude,$longitude")
                                val browserIntent = Intent(Intent.ACTION_VIEW, fallbackUri)
                                try { context.startActivity(browserIntent) } catch (_: Exception) {}
                            }
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = "Open in Google Maps",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Interactive Map View Container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(230.dp)
            ) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        WebView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.loadWithOverviewMode = true
                            settings.useWideViewPort = true
                            webChromeClient = WebChromeClient()
                            webViewClient = WebViewClient()

                            val bridge = MapLocationBridge { lat, lng ->
                                currentOnLocationChanged?.invoke(lat, lng)
                            }
                            addJavascriptInterface(bridge, "AndroidBridge")

                            val escapedTitle = title.replace("'", "\\'").replace("\"", "\\\"")
                            val html = generateLeafletHtml(latitude, longitude, isInteractive, escapedTitle)
                            loadDataWithBaseURL("https://www.openstreetmap.org", html, "text/html", "UTF-8", null)
                            webViewRef = this
                        }
                    },
                    update = {
                        // The LaunchedEffect handles updatePosition calls dynamically
                    }
                )

                // Interactive mode hint overlay
                if (isInteractive) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.72f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MyLocation,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Tap map or drag pin to choose PG location",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Quick Preset City Buttons for fast location jumps
            if (isInteractive && onLocationChanged != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Quick Jump:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    listOf(
                        "Bengaluru" to Pair(12.9716, 77.5946),
                        "Hyderabad" to Pair(17.3850, 78.4867),
                        "Pune" to Pair(18.5204, 73.8567),
                        "Delhi" to Pair(28.7041, 77.1025)
                    ).forEach { (name, coords) ->
                        SuggestionChip(
                            onClick = {
                                onLocationChanged(coords.first, coords.second)
                            },
                            label = { Text(name, fontSize = 10.sp) },
                            modifier = Modifier.height(28.dp)
                        )
                    }
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            webViewRef?.destroy()
            webViewRef = null
        }
    }
}

private fun generateLeafletHtml(
    lat: Double,
    lng: Double,
    isInteractive: Boolean,
    title: String
): String {
    return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8" />
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
            <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
            <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
            <style>
                html, body, #map {
                    margin: 0;
                    padding: 0;
                    width: 100%;
                    height: 100%;
                    background: #f0f2f5;
                }
                .leaflet-control-attribution {
                    display: none !important;
                }
                .custom-pin {
                    filter: drop-shadow(0 2px 4px rgba(0,0,0,0.3));
                }
            </style>
        </head>
        <body>
            <div id="map"></div>
            <script>
                var map = L.map('map', {
                    center: [$lat, $lng],
                    zoom: 15,
                    zoomControl: true,
                    attributionControl: false
                });

                L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
                    maxZoom: 19
                }).addTo(map);

                var marker = L.marker([$lat, $lng], {
                    draggable: $isInteractive
                }).addTo(map);

                if ("$title".length > 0) {
                    marker.bindPopup("<b>$title</b><br/>Lat: $lat, Lng: $lng").openPopup();
                }

                if ($isInteractive) {
                    map.on('click', function(e) {
                        marker.setLatLng(e.latlng);
                        if (window.AndroidBridge) {
                            window.AndroidBridge.onPinMoved(e.latlng.lat, e.latlng.lng);
                        }
                    });

                    marker.on('dragend', function(e) {
                        var pos = marker.getLatLng();
                        if (window.AndroidBridge) {
                            window.AndroidBridge.onPinMoved(pos.lat, pos.lng);
                        }
                    });
                }

                function updatePosition(newLat, newLng) {
                    map.setView([newLat, newLng], 15);
                    marker.setLatLng([newLat, newLng]);
                }
            </script>
        </body>
        </html>
    """.trimIndent()
}
