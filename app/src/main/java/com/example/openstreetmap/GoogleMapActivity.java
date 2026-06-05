package com.example.openstreetmap;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.util.Locale;

public class GoogleMapActivity extends AppCompatActivity {

    private MapView osmMapView;
    private RequestQueue networkRequestQueue;
    private final String serverShowUrl = "http://10.0.2.2/map_project/getPosition.php";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Configuration.getInstance().setUserAgentValue(getPackageName());
        Configuration.getInstance().load(getApplicationContext(), getSharedPreferences("osmdroid", MODE_PRIVATE));

        setContentView(R.layout.activity_google_map);

        osmMapView = findViewById(R.id.osmMapView);
        osmMapView.setTileSource(TileSourceFactory.MAPNIK);
        osmMapView.setMultiTouchControls(true);

        osmMapView.getController().setZoom(15.0);
        osmMapView.getController().setCenter(new GeoPoint(48.8584, 2.2945));

        networkRequestQueue = Volley.newRequestQueue(getApplicationContext());

        fetchSavedLocations();
    }

    private void fetchSavedLocations() {
        JsonObjectRequest jsonRequest = new JsonObjectRequest(
                Request.Method.POST,
                serverShowUrl,
                null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            JSONArray jsonPositions = response.getJSONArray("positions");
                            if (jsonPositions.length() > 0) {
                                for (int i = 0; i < jsonPositions.length(); i++) {
                                    JSONObject posData = jsonPositions.getJSONObject(i);
                                    double lat = posData.getDouble("latitude");
                                    double lng = posData.getDouble("longitude");

                                    Marker mapMarker = new Marker(osmMapView);
                                    GeoPoint geoPoint = new GeoPoint(lat, lng);
                                    mapMarker.setPosition(geoPoint);
                                    mapMarker.setTitle("Location " + (i + 1));

                                    Drawable iconDrawable = ResourcesCompat.getDrawable(getResources(), R.drawable.marker, null);
                                    if (iconDrawable != null) {
                                        Bitmap iconBitmap;
                                        if (iconDrawable instanceof BitmapDrawable) {
                                            iconBitmap = ((BitmapDrawable) iconDrawable).getBitmap();
                                        } else {
                                            iconBitmap = Bitmap.createBitmap(iconDrawable.getIntrinsicWidth(),
                                                    iconDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
                                            Canvas canvas = new Canvas(iconBitmap);
                                            iconDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                                            iconDrawable.draw(canvas);
                                        }
                                        Bitmap scaledIcon = Bitmap.createScaledBitmap(iconBitmap, 80, 80, false);
                                        mapMarker.setIcon(new BitmapDrawable(getResources(), scaledIcon));
                                    }

                                    mapMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                                    osmMapView.getOverlays().add(mapMarker);
                                    
                                    if (i == jsonPositions.length() - 1) {
                                        osmMapView.getController().animateTo(geoPoint);
                                    }
                                }
                                osmMapView.invalidate();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(GoogleMapActivity.this, "Server error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
        );

        networkRequestQueue.add(jsonRequest);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (osmMapView != null) osmMapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (osmMapView != null) osmMapView.onPause();
    }
}
