package com.example.feature.map.map

/**
 * Builds the full Leaflet HTML page loaded into the Map screen's WebView.
 *
 * This is the exact HTML/CSS/JS payload `MapScreen.kt` used to construct
 * inline before the module split - the premium dark map tile filter, the
 * neon marker glow keyframes, the pulsating user location dot, and every
 * exposed JS function (`addVenueMarker`, `addFriendMarker`, `centerOn`,
 * `drawRoute`, `clearRoute`, `toggleHeatmap`, `filterCategory`,
 * `fetchOverpassPOIs`) are unchanged. Only the Kotlin string-template splice
 * points for the initial venue/friend marker scripts became explicit
 * function parameters instead of captured local variables.
 *
 * Do NOT change the Leaflet engine or JS behaviour here as part of the
 * architecture split - see `docs/MAP_ARCHITECTURE.md` for the planned
 * Organic Maps migration, which is a separate, deliberate future step.
 */
internal fun buildLeafletHtml(
    venueMarkersScript: String,
    friendMarkersScript: String
): String {
    return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
            <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
            <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
            <style>
                body, html { margin: 0; padding: 0; background-color: #0b0f19; width: 100%; height: 100%; overflow: hidden; }
                #map { height: 100%; width: 100%; background-color: #0b0f19; }
                
                /* Dark premium luxury OSM layout filters */
                .leaflet-tile {
                    filter: invert(100%) hue-rotate(190deg) brightness(50%) contrast(125%) saturate(130%);
                }
                .leaflet-zoom-animated {
                    transition: transform 0.45s cubic-bezier(0.25, 1, 0.5, 1);
                }
                .leaflet-control-attribution { display: none !important; }
                
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
                    100% { transform: scale(1.8); opacity: 0; }
                }
            </style>
        </head>
        <body>
            <div id="map"></div>
            <script>
                var map = L.map('map', {
                    zoomControl: false,
                    attributionControl: false
                }).setView([-26.115, 28.055], 13);

                L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                    maxZoom: 19
                }).addTo(map);

                var venueMarkers = {};
                var friendMarkers = {};
                var densityCircles = [];
                var routePolyline = null;

                // User pulsating dot marker
                var userMarker = L.marker([-26.147, 28.043], {
                    icon: L.divIcon({
                        className: 'user-loc',
                        html: "<div style='position:relative; width:34px; height:34px; display:flex; align-items:center; justify-content:center;'><div style='position:absolute; width:14px; height:14px; background-color:#00E5FF; border-radius:50%; border:2.5px solid #fff; box-shadow:0 0 10px #00E5FF; z-index:2;'></div><div style='position:absolute; width:32px; height:32px; background-color:rgba(0,229,255,0.35); border-radius:50%; animation:pulse-user 2s infinite; z-index:1;'></div></div>",
                        iconSize: [34, 34],
                        iconAnchor: [17, 17]
                    })
                }).addTo(map);

                // Professional addVenueMarker layout with badges & glowing rings
                function addVenueMarker(id, name, lat, lon, category, subcategory, rating, score, imageUrl, hasFlashDrop, isLive, hasEvent, friendsCount, isSponsored, isTrending, isHot, isClosingSoon) {
                    var borderStyle = "box-shadow: 0 0 10px #00E5FF, inset 0 0 5px #00E5FF; border: 2.5px solid #00E5FF;";
                    var animClass = "";
                    
                    if (isHot) {
                        borderStyle = "border: 2.5px solid #FF2D55;";
                        animClass = "animation: neon-glow-hot 1.6s infinite alternate;";
                    } else if (isTrending) {
                        borderStyle = "border: 2.5px solid #B026FF;";
                        animClass = "animation: neon-glow-trending 2.2s infinite alternate;";
                    } else if (isClosingSoon) {
                        borderStyle = "border: 2.5px solid #FF9500; box-shadow: 0 0 10px #FF9500;";
                    }

                    var iconHtml = "<div style='position:relative; width:52px; height:52px; display:flex; align-items:center; justify-content:center;'>";
                    iconHtml += "<div style='position:absolute; width:40px; height:40px; border-radius:50%; background:#000; " + borderStyle + " " + animClass + "'></div>";
                    iconHtml += "<div style='position:absolute; width:33px; height:33px; border-radius:50%; overflow:hidden; background:#151d30; display:flex; align-items:center; justify-content:center; z-index:1;'>";
                    if (imageUrl) {
                        iconHtml += "<img src='" + imageUrl + "' style='width:100%; height:100%; object-fit:cover;' />";
                    } else {
                        iconHtml += "<span style='color:white; font-size:10px;'>🎵</span>";
                    }
                    iconHtml += "</div>";

                    // Overlay badge setup
                    if (isLive) {
                        iconHtml += "<div style='position:absolute; top:-4px; right:-2px; background:#FF2D55; color:white; font-size:6px; font-weight:bold; padding:1.5px 3.5px; border-radius:5px; border:1px solid #000; box-shadow:0 1.5px 3px rgba(0,0,0,0.5); z-index:2;'>🔴 LIVE</div>";
                    } else if (hasFlashDrop) {
                        iconHtml += "<div style='position:absolute; top:-4px; right:-2px; background:#FF9500; font-size:9px; width:15px; height:15px; display:flex; align-items:center; justify-content:center; border-radius:50%; border:1px solid #000; box-shadow:0 1.5px 3px rgba(0,0,0,0.5); z-index:2;'>🎁</div>";
                    } else if (hasEvent) {
                        iconHtml += "<div style='position:absolute; top:-4px; right:-2px; background:#00E5FF; font-size:9px; width:15px; height:15px; display:flex; align-items:center; justify-content:center; border-radius:50%; border:1px solid #000; box-shadow:0 1.5px 3px rgba(0,0,0,0.5); z-index:2;'>🎫</div>";
                    } else if (friendsCount > 0) {
                        iconHtml += "<div style='position:absolute; top:-4px; right:-2px; background:#32D74B; color:black; font-size:7px; font-weight:bold; padding:1.5px 3.5px; border-radius:5px; border:1px solid #000; box-shadow:0 1.5px 3px rgba(0,0,0,0.5); z-index:2;'>👥 +" + friendsCount + "</div>";
                    } else if (isSponsored) {
                        iconHtml += "<div style='position:absolute; top:-4px; right:-2px; background:#FFD700; color:black; font-size:8px; font-weight:bold; width:14px; height:14px; display:flex; align-items:center; justify-content:center; border-radius:50%; border:1px solid #000; box-shadow:0 1.5px 3px rgba(0,0,0,0.5); z-index:2;'>⭐</div>";
                    }
                    iconHtml += "</div>";

                    var venueIcon = L.divIcon({
                        className: 'venue-' + id,
                        html: iconHtml,
                        iconSize: [52, 52],
                        iconAnchor: [26, 26]
                    });

                    var marker = L.marker([lat, lon], { icon: venueIcon }).addTo(map);
                    marker.on('click', function() {
                        if (window.AndroidBridge) window.AndroidBridge.onVenueClick(id);
                    });

                    venueMarkers[id] = { marker: marker, category: category, lat: lat, lon: lon, score: score, hasEvent: hasEvent };
                }

                function addFriendMarker(id, name, lat, lon, avatarUrl, status, currentActivity, isCloseFriend) {
                    var color = status === "Online" ? "#32D74B" : "#8E8E93";
                    var iconHtml = "<div style='position:relative; width:44px; height:44px; display:flex; align-items:center; justify-content:center;'>";
                    iconHtml += "<div style='position:absolute; width:36px; height:36px; border-radius:50%; border:2.5px solid " + color + "; background:#111; box-shadow:0 3px 6px rgba(0,0,0,0.4);'></div>";
                    iconHtml += "<div style='position:absolute; width:29px; height:29px; border-radius:50%; overflow:hidden;'>";
                    iconHtml += "<img src='" + avatarUrl + "' style='width:100%; height:100%; object-fit:cover;' />";
                    iconHtml += "</div></div>";

                    var friendIcon = L.divIcon({
                        className: 'friend-' + id,
                        html: iconHtml,
                        iconSize: [44, 44],
                        iconAnchor: [22, 22]
                    });

                    var marker = L.marker([lat, lon], { icon: friendIcon }).addTo(map);
                    marker.on('click', function() {
                        if (window.AndroidBridge) window.AndroidBridge.onFriendClick(id);
                    });

                    friendMarkers[id] = { marker: marker, lat: lat, lon: lon };
                }

                function centerOn(lat, lon, zoom) {
                    map.flyTo([lat, lon], zoom || 15, { animate: true, duration: 0.85 });
                }

                function drawRoute(endLat, endLon) {
                    if (routePolyline) map.removeLayer(routePolyline);
                    var startLat = -26.147;
                    var startLon = 28.043;
                    routePolyline = L.polyline([[startLat, startLon], [endLat, endLon]], {
                        color: '#00E5FF',
                        weight: 4.5,
                        opacity: 0.85,
                        dashArray: '8, 12',
                        lineJoin: 'round'
                    }).addTo(map);
                    
                    var bounds = L.latLngBounds([[startLat, startLon], [endLat, endLon]]);
                    map.fitBounds(bounds, { padding: [60, 60] });
                }

                function clearRoute() {
                    if (routePolyline) { map.removeLayer(routePolyline); routePolyline = null; }
                }

                function toggleHeatmap(show) {
                    densityCircles.forEach(function(c) { map.removeLayer(c); });
                    densityCircles = [];
                    if (show) {
                        for (var id in venueMarkers) {
                            var v = venueMarkers[id];
                            var col = v.score > 85 ? "rgba(255, 45, 85, 0.22)" : "rgba(0, 229, 255, 0.15)";
                            var rad = v.score > 85 ? 420 : 280;
                            var circle = L.circle([v.lat, v.lon], { color: 'transparent', fillColor: col, fillOpacity: 0.45, radius: rad }).addTo(map);
                            densityCircles.push(circle);
                        }
                    }
                }

                function filterCategory(categoryName) {
                    clearRoute();
                    var clean = categoryName.replace(/[^\w\s]/g, '').trim().toLowerCase();
                    if (clean === "wellness") clean = "recover";
                    
                    for (var id in venueMarkers) {
                        var item = venueMarkers[id];
                        var itemCat = item.category.toLowerCase();
                        if (clean === "all" || itemCat === clean || (clean === "events" && item.hasEvent)) {
                            item.marker.addTo(map);
                        } else {
                            map.removeLayer(item.marker);
                        }
                    }
                }

                function fetchOverpassPOIs(categoryName) {
                    try {
                        var bounds = map.getBounds();
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
                                          if (!venueMarkers[osmId]) {
                                              addVenueMarker(
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
                }

                $venueMarkersScript
                $friendMarkersScript
                toggleHeatmap(true);
            </script>
        </body>
        </html>
    """.trimIndent()
}
