package com.example.feature.map.map

/**
 * Builds the full Leaflet HTML page loaded into the Map screen's WebView.
 *
 * Supports inlined Leaflet CSS & JS from local assets for 100% offline, zero-latency
 * initialisation, with CDN fallback. Renders high-performance dark OpenStreetMap
 * raster tiles (CartoDB Dark Matter) with standard OSM fallback, custom pulsing user
 * location, vibrant glowing venue markers matching the mockup with name/distance pills,
 * friend pins, route drawing, heatmap, and Overpass API POI queries.
 */
internal fun buildLeafletHtml(
    venueMarkersScript: String,
    friendMarkersScript: String,
    inlinedCss: String = "",
    inlinedJs: String = ""
): String {
    val cssBlock = if (inlinedCss.isNotBlank()) {
        "<style>\n$inlinedCss\n</style>"
    } else {
        """
        <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
        <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/leaflet.css" />
        """.trimIndent()
    }

    val jsBlock = if (inlinedJs.isNotBlank()) {
        "<script>\n$inlinedJs\n</script>"
    } else {
        """
        <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
        <script>if (typeof L === 'undefined') { document.write('<script src="https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/leaflet.js"><\\/script>'); }</script>
        """.trimIndent()
    }

    return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8" />
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
            $cssBlock
            $jsBlock
            <style>
                body, html {
                    margin: 0;
                    padding: 0;
                    background-color: #0b0f19;
                    width: 100%;
                    height: 100%;
                    overflow: hidden;
                    -webkit-user-select: none;
                    user-select: none;
                }
                #map {
                    height: 100%;
                    width: 100%;
                    background-color: #0b0f19;
                }
                
                /* Dark premium luxury OSM tile enhancement */
                .leaflet-tile {
                    filter: brightness(92%) contrast(112%);
                }
                .leaflet-zoom-animated {
                    transition: transform 0.4s cubic-bezier(0.25, 1, 0.5, 1);
                }
                .leaflet-control-attribution, .leaflet-control-zoom {
                    display: none !important;
                }
                
                /* Glowing neon ring keyframes */
                @keyframes neon-glow-hot {
                    0% { transform: scale(0.96); box-shadow: 0 0 8px #FF2D55, inset 0 0 4px #FF2D55; }
                    100% { transform: scale(1.04); box-shadow: 0 0 18px #FF2D55, 0 0 25px #FF2D55; }
                }
                @keyframes neon-glow-trending {
                    0% { transform: scale(0.97); box-shadow: 0 0 6px #B026FF, inset 0 0 3px #B026FF; }
                    100% { transform: scale(1.03); box-shadow: 0 0 15px #B026FF, 0 0 22px #B026FF; }
                }
                @keyframes pulse-user {
                    0% { transform: scale(0.6); opacity: 0.9; }
                    100% { transform: scale(2.0); opacity: 0; }
                }
                
                .venue-marker-wrap, .friend-marker-wrap, .user-loc-pin {
                    background: transparent;
                    border: none;
                }
            </style>
        </head>
        <body>
            <div id="map"></div>
            <script>
                function initMap() {
                    if (typeof L === 'undefined') {
                        setTimeout(initMap, 50);
                        return;
                    }

                    var map = L.map('map', {
                        zoomControl: false,
                        attributionControl: false,
                        fadeAnimation: true,
                        markerZoomAnimation: true
                    }).setView([-26.145, 28.045], 14);

                    // Primary: CartoDB Dark Matter (native dark OSM raster tiles with clear street labels)
                    var darkLayer = L.tileLayer('https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png', {
                        subdomains: 'abcd',
                        maxZoom: 20
                    }).addTo(map);

                    // Fallback: Standard OpenStreetMap (inverted for dark theme)
                    var osmFallback = L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                        maxZoom: 19
                    });

                    darkLayer.on('tileerror', function() {
                        if (!map.hasLayer(osmFallback)) {
                            osmFallback.addTo(map);
                        }
                    });

                    window.map = map;
                    window.venueMarkers = {};
                    window.friendMarkers = {};
                    window.densityCircles = [];
                    window.routePolyline = null;

                    // User pulsating dot marker
                    var userMarker = L.marker([-26.147, 28.043], {
                        icon: L.divIcon({
                            className: 'user-loc-pin',
                            html: "<div style='position:relative; width:40px; height:40px; display:flex; align-items:center; justify-content:center;'><div style='position:absolute; width:14px; height:14px; background-color:#00E5FF; border-radius:50%; border:2.5px solid #fff; box-shadow:0 0 12px #00E5FF; z-index:3;'></div><div style='position:absolute; width:38px; height:38px; background-color:rgba(0,229,255,0.35); border-radius:50%; animation:pulse-user 2s infinite; z-index:2;'></div><div style='position:absolute; bottom:-14px; background:rgba(0,0,0,0.8); color:#00E5FF; font-size:8px; font-weight:800; padding:1px 5px; border-radius:4px; border:0.5px solid #00E5FF; white-space:nowrap; z-index:4;'>YOU</div></div>",
                            iconSize: [40, 54],
                            iconAnchor: [20, 20]
                        })
                    }).addTo(map);

                    // Professional addVenueMarker layout with badges, glowing rings & labels matching mockup
                    window.addVenueMarker = function(id, name, lat, lon, category, subcategory, rating, score, imageUrl, hasFlashDrop, isLive, hasEvent, friendsCount, isSponsored, isTrending, isHot, isClosingSoon) {
                        var borderStyle = "box-shadow: 0 0 10px #00E5FF, inset 0 0 5px #00E5FF; border: 2.5px solid #00E5FF;";
                        var animClass = "";
                        var ringColor = "#00E5FF";
                        
                        if (isHot) {
                            ringColor = "#FF2D55";
                            borderStyle = "border: 2.5px solid #FF2D55;";
                            animClass = "animation: neon-glow-hot 1.6s infinite alternate;";
                        } else if (isTrending) {
                            ringColor = "#B026FF";
                            borderStyle = "border: 2.5px solid #B026FF;";
                            animClass = "animation: neon-glow-trending 2.2s infinite alternate;";
                        } else if (isClosingSoon) {
                            ringColor = "#FF9500";
                            borderStyle = "border: 2.5px solid #FF9500; box-shadow: 0 0 10px #FF9500;";
                        }

                        var iconHtml = "<div style='display:flex; flex-direction:column; align-items:center; width:130px; cursor:pointer;'>";
                        
                        // Top circle with badge
                        iconHtml += "<div style='position:relative; width:44px; height:44px; display:flex; align-items:center; justify-content:center;'>";
                        iconHtml += "<div style='position:absolute; width:40px; height:40px; border-radius:50%; background:#000; " + borderStyle + " " + animClass + "'></div>";
                        iconHtml += "<div style='position:absolute; width:33px; height:33px; border-radius:50%; overflow:hidden; background:#151d30; display:flex; align-items:center; justify-content:center; z-index:2;'>";
                        if (imageUrl) {
                            iconHtml += "<img src='" + imageUrl + "' style='width:100%; height:100%; object-fit:cover;' onerror=\"this.style.display='none';\" />";
                        } else {
                            iconHtml += "<span style='color:white; font-size:11px;'>🎵</span>";
                        }
                        iconHtml += "</div>";

                        // Overlay badge setup
                        if (isLive) {
                            iconHtml += "<div style='position:absolute; top:-4px; right:-2px; background:#FF2D55; color:white; font-size:6px; font-weight:bold; padding:1.5px 3.5px; border-radius:5px; border:1px solid #000; box-shadow:0 1.5px 3px rgba(0,0,0,0.5); z-index:3;'>🔴 LIVE</div>";
                        } else if (hasFlashDrop) {
                            iconHtml += "<div style='position:absolute; top:-4px; right:-2px; background:#FF9500; font-size:9px; width:15px; height:15px; display:flex; align-items:center; justify-content:center; border-radius:50%; border:1px solid #000; box-shadow:0 1.5px 3px rgba(0,0,0,0.5); z-index:3;'>🎁</div>";
                        } else if (hasEvent) {
                            iconHtml += "<div style='position:absolute; top:-4px; right:-2px; background:#00E5FF; font-size:9px; width:15px; height:15px; display:flex; align-items:center; justify-content:center; border-radius:50%; border:1px solid #000; box-shadow:0 1.5px 3px rgba(0,0,0,0.5); z-index:3;'>🎫</div>";
                        } else if (friendsCount > 0) {
                            iconHtml += "<div style='position:absolute; top:-4px; right:-2px; background:#32D74B; color:black; font-size:7px; font-weight:bold; padding:1.5px 3.5px; border-radius:5px; border:1px solid #000; box-shadow:0 1.5px 3px rgba(0,0,0,0.5); z-index:3;'>👥 +" + friendsCount + "</div>";
                        } else if (isSponsored) {
                            iconHtml += "<div style='position:absolute; top:-4px; right:-2px; background:#FFD700; color:black; font-size:8px; font-weight:bold; width:14px; height:14px; display:flex; align-items:center; justify-content:center; border-radius:50%; border:1px solid #000; box-shadow:0 1.5px 3px rgba(0,0,0,0.5); z-index:3;'>⭐</div>";
                        }
                        iconHtml += "</div>";

                        // Downward pin pointer caret
                        iconHtml += "<div style='width:0; height:0; border-left:4px solid transparent; border-right:4px solid transparent; border-top:5px solid " + ringColor + "; margin-top:-1px;'></div>";

                        // Pill label with venue name & vibe score
                        var shortName = name && name.length > 13 ? name.substring(0, 12) + '…' : (name || 'Venue');
                        iconHtml += "<div style='margin-top:2px; display:inline-flex; align-items:center; background:rgba(13,17,29,0.94); border:1px solid " + ringColor + "; border-radius:12px; padding:2px 7px; box-shadow:0 3px 8px rgba(0,0,0,0.8); white-space:nowrap; max-width:125px;'>";
                        iconHtml += "<span style='color:#FFFFFF; font-size:10px; font-weight:700; font-family:-apple-system,BlinkMacSystemFont,Roboto,sans-serif;'>" + shortName + "</span>";
                        iconHtml += "<span style='color:" + ringColor + "; font-size:9px; font-weight:800; margin-left:4px;'>• " + score + "%</span>";
                        iconHtml += "</div>";

                        iconHtml += "</div>";

                        var venueIcon = L.divIcon({
                            className: 'venue-marker-wrap',
                            html: iconHtml,
                            iconSize: [130, 72],
                            iconAnchor: [65, 22]
                        });

                        var marker = L.marker([lat, lon], { icon: venueIcon }).addTo(window.map);
                        marker.on('click', function() {
                            if (window.AndroidBridge) window.AndroidBridge.onVenueClick(id);
                        });

                        window.venueMarkers[id] = { marker: marker, category: category, lat: lat, lon: lon, score: score, hasEvent: hasEvent };
                    };

                    window.addFriendMarker = function(id, name, lat, lon, avatarUrl, status, currentActivity, isCloseFriend) {
                        var color = status === "Online" ? "#32D74B" : "#8E8E93";
                        var iconHtml = "<div style='display:flex; flex-direction:column; align-items:center; width:100px; cursor:pointer;'>";
                        iconHtml += "<div style='position:relative; width:38px; height:38px; display:flex; align-items:center; justify-content:center;'>";
                        iconHtml += "<div style='position:absolute; width:34px; height:34px; border-radius:50%; border:2px solid " + color + "; background:#111; box-shadow:0 2px 6px rgba(0,0,0,0.5);'></div>";
                        iconHtml += "<div style='position:absolute; width:28px; height:28px; border-radius:50%; overflow:hidden; z-index:2;'>";
                        iconHtml += "<img src='" + avatarUrl + "' style='width:100%; height:100%; object-fit:cover;' onerror=\"this.style.display='none';\" />";
                        iconHtml += "</div>";
                        iconHtml += "<div style='position:absolute; bottom:1px; right:1px; width:9px; height:9px; border-radius:50%; background:" + color + "; border:1.5px solid #000; z-index:3;'></div>";
                        iconHtml += "</div>";
                        
                        var shortFriendName = name && name.length > 10 ? name.substring(0, 9) + '…' : (name || 'Friend');
                        iconHtml += "<div style='margin-top:2px; background:rgba(13,17,29,0.92); border:1px solid " + color + "; border-radius:8px; padding:1px 5px; box-shadow:0 2px 4px rgba(0,0,0,0.6); white-space:nowrap;'>";
                        iconHtml += "<span style='color:#FFFFFF; font-size:9px; font-weight:700; font-family:-apple-system,BlinkMacSystemFont,Roboto,sans-serif;'>" + shortFriendName + "</span>";
                        iconHtml += "</div>";
                        iconHtml += "</div>";

                        var friendIcon = L.divIcon({
                            className: 'friend-marker-wrap',
                            html: iconHtml,
                            iconSize: [100, 60],
                            iconAnchor: [50, 19]
                        });

                        var marker = L.marker([lat, lon], { icon: friendIcon }).addTo(window.map);
                        marker.on('click', function() {
                            if (window.AndroidBridge) window.AndroidBridge.onFriendClick(id);
                        });

                        window.friendMarkers[id] = { marker: marker, lat: lat, lon: lon };
                    };

                    window.centerOn = function(lat, lon, zoom) {
                        if (window.map) {
                            window.map.flyTo([lat, lon], zoom || 15, { animate: true, duration: 0.85 });
                        }
                    };

                    window.drawRoute = function(endLat, endLon) {
                        if (!window.map) return;
                        if (window.routePolyline) window.map.removeLayer(window.routePolyline);
                        var startLat = -26.147;
                        var startLon = 28.043;
                        window.routePolyline = L.polyline([[startLat, startLon], [endLat, endLon]], {
                            color: '#00E5FF',
                            weight: 5,
                            opacity: 0.9,
                            dashArray: '8, 12',
                            lineJoin: 'round'
                        }).addTo(window.map);
                        
                        var bounds = L.latLngBounds([[startLat, startLon], [endLat, endLon]]);
                        window.map.fitBounds(bounds, { padding: [80, 80] });
                    };

                    window.clearRoute = function() {
                        if (window.routePolyline && window.map) {
                            window.map.removeLayer(window.routePolyline);
                            window.routePolyline = null;
                        }
                    };

                    window.toggleHeatmap = function(show) {
                        if (!window.map) return;
                        window.densityCircles.forEach(function(c) { window.map.removeLayer(c); });
                        window.densityCircles = [];
                        if (show) {
                            for (var id in window.venueMarkers) {
                                var v = window.venueMarkers[id];
                                var col = v.score > 85 ? "rgba(255, 45, 85, 0.22)" : "rgba(139, 92, 246, 0.18)";
                                var rad = v.score > 85 ? 450 : 300;
                                var circle = L.circle([v.lat, v.lon], { color: 'transparent', fillColor: col, fillOpacity: 0.5, radius: rad }).addTo(window.map);
                                window.densityCircles.push(circle);
                            }
                        }
                    };

                    window.filterCategory = function(categoryName) {
                        window.clearRoute();
                        if (!window.map) return;
                        var clean = (categoryName || '').replace(/[^\w\s]/g, '').trim().toLowerCase();
                        if (clean === "wellness") clean = "recover";
                        
                        for (var id in window.venueMarkers) {
                            var item = window.venueMarkers[id];
                            var itemCat = (item.category || '').toLowerCase();
                            if (clean === "all" || !clean || itemCat === clean || (clean === "events" && item.hasEvent)) {
                                item.marker.addTo(window.map);
                            } else {
                                window.map.removeLayer(item.marker);
                            }
                        }
                    };

                    window.fetchOverpassPOIs = function(categoryName) {
                        try {
                            if (!window.map) return;
                            var bounds = window.map.getBounds();
                            var s = bounds.getSouth(), w = bounds.getWest(), n = bounds.getNorth(), e = bounds.getEast();
                            var cat = (categoryName || 'Nightlife').toLowerCase();
                            var amenityQuery = 'nightclub|bar|pub|restaurant|cafe|fast_food';
                            if (cat.indexOf('night') !== -1 || cat.indexOf('club') !== -1) {
                                amenityQuery = 'nightclub|bar|pub';
                            } else if (cat.indexOf('food') !== -1 || cat.indexOf('din') !== -1) {
                                amenityQuery = 'restaurant|fast_food|food_court';
                            } else if (cat.indexOf('well') !== -1 || cat.indexOf('recov') !== -1) {
                                amenityQuery = 'spa|gym|fitness_centre';
                            }
                            var query = '[out:json][timeout:15];node["amenity"~"' + amenityQuery + '"](' + s + ',' + w + ',' + n + ',' + e + ');out body 30;';
                            var url = 'https://overpass-api.de/api/interpreter?data=' + encodeURIComponent(query);
                            
                            fetch(url)
                              .then(function(res) { return res.json(); })
                              .then(function(data) {
                                  if (data && data.elements) {
                                      var added = 0;
                                      data.elements.forEach(function(el) {
                                          if (el.tags && el.tags.name) {
                                              var osmId = 'osm_' + el.id;
                                              if (!window.venueMarkers[osmId]) {
                                                  window.addVenueMarker(
                                                      osmId,
                                                      el.tags.name,
                                                      el.lat,
                                                      el.lon,
                                                      categoryName || 'Nightlife',
                                                      (el.tags.amenity || 'OSM POI').toUpperCase(),
                                                      4.7,
                                                      90,
                                                      'https://images.unsplash.com/photo-1514525253161-7a46d19cd819?q=80&w=200&auto=format&fit=crop',
                                                      false, false, false, 0, false, true, false, false
                                                  );
                                                  added++;
                                              }
                                          }
                                      });
                                      if (window.AndroidBridge) {
                                          window.AndroidBridge.onOverpassResult(added, categoryName || 'All');
                                      }
                                  }
                              })
                              .catch(function(err) {
                                  if (window.AndroidBridge) {
                                      window.AndroidBridge.onOverpassResult(0, categoryName || 'All');
                                  }
                              });
                        } catch(e) {
                            if (window.AndroidBridge) {
                                window.AndroidBridge.onOverpassResult(0, categoryName || 'All');
                            }
                        }
                    };

                    // Initial marker population scripts
                    $venueMarkersScript
                    $friendMarkersScript
                    window.toggleHeatmap(true);
                }

                initMap();
            </script>
        </body>
        </html>
    """.trimIndent()
}
