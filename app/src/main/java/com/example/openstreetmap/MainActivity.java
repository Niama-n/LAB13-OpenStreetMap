package com.example.openstreetmap;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private Button openMapBtn;
    private double currentLat;
    private double currentLng;
    private double currentAlt;
    private float locationAccuracy;
    private RequestQueue networkRequestQueue;
    private final String serverInsertUrl = "http://10.0.2.2/map_project/createPosition.php";
    private LocationManager gpsLocationManager;

    private static final int LOCATION_PERMISSION_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        networkRequestQueue = Volley.newRequestQueue(getApplicationContext());
        gpsLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        
        openMapBtn = findViewById(R.id.openMapBtn);
        openMapBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, GoogleMapActivity.class));
            }
        });

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.READ_PHONE_STATE
                    }, LOCATION_PERMISSION_CODE);
        } else {
            initLocationUpdates();
        }
    }

    private void initLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        gpsLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 60000, 150, new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                currentLat = location.getLatitude();
                currentLng = location.getLongitude();
                currentAlt = location.getAltitude();
                locationAccuracy = location.getAccuracy();

                String infoMsg = String.format(
                        getResources().getString(R.string.new_location), currentLat,
                        currentLng, currentAlt, locationAccuracy);

                pushLocationToServer(currentLat, currentLng);

                Toast.makeText(getApplicationContext(), infoMsg, Toast.LENGTH_LONG).show();
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            @Override
            public void onProviderEnabled(@NonNull String provider) {
            }

            @Override
            public void onProviderDisabled(@NonNull String provider) {
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initLocationUpdates();
            } else {
                Toast.makeText(this, "Permission denied. The app cannot function.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void pushLocationToServer(final double latitude, final double longitude) {
        StringRequest postRequest = new StringRequest(Request.Method.POST,
                serverInsertUrl, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
            }
        }) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                HashMap<String, String> requestParams = new HashMap<>();
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

                requestParams.put("latitude", String.valueOf(latitude));
                requestParams.put("longitude", String.valueOf(longitude));
                requestParams.put("date", dateFormat.format(new Date()));

                String deviceId = Settings.Secure.getString(
                        getContentResolver(),
                        Settings.Secure.ANDROID_ID
                    );

                requestParams.put("imei", deviceId);

                return requestParams;
            }
        };
        networkRequestQueue.add(postRequest);
    }
}