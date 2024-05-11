package com.example.taller_2;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import org.json.JSONException;
import android.app.UiModeManager;
import org.json.JSONObject;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.File;
import java.io.Writer;
import java.util.Date;
import java.io.IOException;
import org.osmdroid.bonuspack.routing.OSRMRoadManager;
import org.osmdroid.bonuspack.routing.Road;
import org.osmdroid.bonuspack.routing.RoadManager;
import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.TilesOverlay;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MapActivity extends AppCompatActivity {
    public static final int LOCATION_PERMISSION_ID = 7;
    public static final int WRITE_PERMISSION_ID = 8;
    public static final String LOCATION_PERMISSION_NAME = Manifest.permission.ACCESS_FINE_LOCATION;
    public static final String WRITE_PERMISSION_NAME = Manifest.permission.WRITE_EXTERNAL_STORAGE;
    public static final double RADIUS_OF_EARTH_KM = 6371.01;

    private FusedLocationProviderClient mFusedLocationClient;
    private LocationRequest mLocationRequest;
    private LocationCallback mLocationCallback;

    MapView map;
    EditText address;
    Marker longPressedMarker, mainMarker, searchMarker;
    double latitude;
    double longitude;
    GeoPoint startPoint;
    Geocoder geocoder;
    RoadManager roadManager;
    Polyline roadOverlay;

    SensorManager sensorManager;
    Sensor lightSensor;
    SensorEventListener sensorEventListener;

    private Set<String> mapStyles;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        roadManager = new OSRMRoadManager(this, "ANDROID");
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        sensorEventListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
                if (map != null)
                    if (sensorEvent.values[0] < 2000)
                        map.getOverlayManager().getTilesOverlay().setColorFilter(TilesOverlay.INVERT_COLORS);
                    else
                        map.getOverlayManager().getTilesOverlay().setColorFilter(new ColorFilter());
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {

            }
        };
        sensorManager.registerListener(sensorEventListener, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);

        requestPermission(this, LOCATION_PERMISSION_NAME, "Can we access your location?", LOCATION_PERMISSION_ID);
        requestPermission(this, WRITE_PERMISSION_NAME, "Can we write in your files?", WRITE_PERMISSION_ID);
        geocoder = new Geocoder(getBaseContext());
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mLocationRequest = createLocationRequest();
        afterPermission();
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult.getLastLocation() != null) {
                    if (distance(latitude, longitude, locationResult.getLastLocation().getLatitude(), locationResult.getLastLocation().getLongitude()) > 0.03)
                        writeJSONObject();
                    latitude = locationResult.getLastLocation().getLatitude();
                    longitude = locationResult.getLastLocation().getLongitude();
                    startPoint = new GeoPoint(latitude, longitude);
                    map.getController().setCenter(startPoint);
                    createMarker(startPoint, "location", null, R.drawable.red_marker);
                }
            }
        };
        Configuration.getInstance().load(getApplicationContext(), PreferenceManager.getDefaultSharedPreferences(getApplicationContext()));
        map = findViewById(R.id.map);
        address = findViewById(R.id.etxtAddress);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);
        map.getOverlays().add(createOverlayEvents());
        address.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (!address.getText().toString().isEmpty()) {
                    try {
                        List<Address> addresses = geocoder.getFromLocationName(address.getText().toString(), 2);
                        if (addresses != null && !addresses.isEmpty()) {
                            if (map != null) {
                                map.getController().setZoom(18.0);
                                map.getController().setCenter(new GeoPoint(addresses.get(0).getLatitude(), addresses.get(0).getLongitude()));
                                searchMarker = createMarker(new GeoPoint(addresses.get(0).getLatitude(), addresses.get(0).getLongitude()), address.getText().toString(), null, R.drawable.red_marker);
                                map.getOverlays().add(searchMarker);
                                Toast.makeText(MapActivity.this,"Distancia: " + String.valueOf(distance(latitude, longitude, addresses.get(0).getLatitude(), addresses.get(0).getLongitude()) * 100), Toast.LENGTH_SHORT).show();
                                drawRoute(new GeoPoint(latitude, longitude), new GeoPoint(addresses.get(0).getLatitude(), addresses.get(0).getLongitude()));
                            }
                        } else {
                            Toast.makeText(MapActivity.this, "Couldn't find address", Toast.LENGTH_SHORT).show();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    Toast.makeText(MapActivity.this, "Empty address", Toast.LENGTH_SHORT).show();
                }
                return false;
            }
        });

        // Inicializar el conjunto de estilos del mapa
        mapStyles = loadMapStyles();
    }

    private Set<String> loadMapStyles() {
        Set<String> styles = new HashSet<>();
        try {
            InputStream is = getResources().openRawResource(R.raw.mapstyle);
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String line;
            StringBuilder jsonContent = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                jsonContent.append(line).append('\n');
            }
            reader.close();
            is.close();

            // Agregar el contenido del archivo al conjunto de estilos
            styles.add(jsonContent.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return styles;
    }

    @Override
    protected void onResume() {
        super.onResume();
        map.onResume();
        map.getController().setZoom(18.0);
        startLocationUpdates();
        UiModeManager uiManager = (UiModeManager) getSystemService(Context.UI_MODE_SERVICE);
        if(uiManager.getNightMode() == UiModeManager.MODE_NIGHT_YES )
            map.getOverlayManager().getTilesOverlay().setColorFilter(TilesOverlay.INVERT_COLORS);
    }

    @Override
    protected void onPause() {
        super.onPause();
        map.onPause();
        stopLocationUpdates();

    }

    private void requestPermission(Activity context, String permission, String justification, int code) {
        if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(context, permission))
                Toast.makeText(context, justification, Toast.LENGTH_SHORT).show();
            ActivityCompat.requestPermissions(context, new String[]{permission}, code);
        }
    }

    private void afterPermission() {
        if (ContextCompat.checkSelfPermission(this, LOCATION_PERMISSION_NAME) == PackageManager.PERMISSION_GRANTED)
            mFusedLocationClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    if (location != null) {
                        longitude = location.getLongitude();
                        latitude = location.getLatitude();
                        map.getController().setCenter(new GeoPoint(latitude, longitude));
                        moveMainMarker(new GeoPoint(latitude, longitude));
                    }
                }
            });
    }

    private LocationRequest createLocationRequest() {
        return LocationRequest.create().setInterval(10000).setFastestInterval(5000).setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    private void startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(MapActivity.this, LOCATION_PERMISSION_NAME) == PackageManager.PERMISSION_GRANTED)
            mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, null);
    }

    private void stopLocationUpdates() {
        mFusedLocationClient.removeLocationUpdates(mLocationCallback);
    }

    private Marker createMarker(GeoPoint p, String title, String desc, int iconID) {
        Marker marker = null;
        if (map != null) {
            marker = new Marker(map);
            if (title != null) marker.setTitle(title);
            if (desc != null) marker.setSubDescription(desc);
            if (iconID != 0) {
                Drawable myIcon = getResources().getDrawable(iconID, this.getTheme());
                marker.setIcon(myIcon);
            }
            marker.setPosition(p);
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        }
        return marker;
    }


    private MapEventsOverlay createOverlayEvents() {
        MapEventsOverlay overlayEventos = new MapEventsOverlay(new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {
                return false;
            }

            @Override
            public boolean longPressHelper(GeoPoint p) {
                longPressOnMap(p);
                return true;
            }
        });
        return overlayEventos;
    }

    private void longPressOnMap(GeoPoint p) {
        if (longPressedMarker != null) map.getOverlays().remove(longPressedMarker);
        longPressedMarker = createMarker(p, "location", null, R.drawable.red_marker);
        map.getOverlays().add(longPressedMarker);
        Toast.makeText(this, "Distancia: " + String.valueOf(distance(latitude, longitude, p.getLatitude(), p.getLongitude()) * 100), Toast.LENGTH_SHORT).show();
        drawRoute(new GeoPoint(latitude, longitude), new GeoPoint(p.getLatitude(), p.getLongitude()));
    }

    private void moveMainMarker(GeoPoint p) {
        if (mainMarker != null) map.getOverlays().remove(mainMarker);
        mainMarker = createMarker(p, "location", null, R.drawable.red_marker);
        map.getOverlays().add(mainMarker);
    }

    public double distance(double lat1, double long1, double lat2, double long2) {
        double latDistance = Math.toRadians(lat1 - lat2);
        double lngDistance = Math.toRadians(long1 - long2);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lngDistance / 2) * Math.sin(lngDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double result = RADIUS_OF_EARTH_KM * c;
        return Math.round(result * 100.0) / 100.0;
    }

    public JSONObject toJSON() {
        JSONObject obj = new JSONObject();
        try {
            obj.put("latitud", latitude);
            obj.put("longitud", longitude);
            obj.put("date", new Date(System.currentTimeMillis()));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return obj;
    }

    private void writeJSONObject() {
        Writer output = null;
        String filename = "locations.json";
        try {
            File file = new File(getBaseContext().getExternalFilesDir(null), filename);
            output = new BufferedWriter(new FileWriter(file));
            output.write(toJSON().toString());
            output.close();
            Toast.makeText(getApplicationContext(), "Location saved",
                    Toast.LENGTH_LONG).show();
        } catch (Exception e) {}
    }

    private void drawRoute(GeoPoint start, GeoPoint finish){
        ArrayList<GeoPoint> routePoints = new ArrayList<>();
        routePoints.add(start);
        routePoints.add(finish);
        Road road = roadManager.getRoad(routePoints);
        if(map!=null){
            if(roadOverlay!=null){
                map.getOverlays().remove(roadOverlay);
            }
            roadOverlay = RoadManager.buildRoadOverlay(road);
            roadOverlay.getOutlinePaint().setColor(Color.RED);
            roadOverlay.getOutlinePaint().setStrokeWidth(10);
            map.getOverlays().add(roadOverlay);
        }
    }
}
