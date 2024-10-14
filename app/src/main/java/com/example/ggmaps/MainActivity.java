package com.example.ggmaps;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.SearchView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private final int FINE_PERMISSION_CODE = 1;
    private GoogleMap myMap;
    Location currentLocation;
    FusedLocationProviderClient fusedLocationProviderClient;
    SearchView searchView;
    Button btnExit ;
    // Location sharing
    LocationCallback locationCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setSupportActionBar(findViewById(R.id.toolbar));

        searchView = findViewById(R.id.search_view);
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        btnExit = findViewById(R.id.btnExit);
        btnExit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
                System.exit(0);
            }
        });
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                searchLocation(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });

        // GPS button
        FloatingActionButton gpsButton = findViewById(R.id.gpsButton);
        gpsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                moveCameraToCurrentLocation();
            }
        });

        getLastLocation();

        // Location sharing
        Button startSharingButton = findViewById(R.id.startSharing);
        Button stopSharingButton = findViewById(R.id.stopSharing);
        startSharingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startLocationSharing();
                stopSharingButton.setVisibility(View.VISIBLE);
                startSharingButton.setVisibility(View.GONE);
            }
        });

        stopSharingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopLocationSharing();
                stopSharingButton.setVisibility(View.GONE);
                startSharingButton.setVisibility(View.VISIBLE);
            }
        });
    }

    private void searchLocation(String locationName) {
        Geocoder geocoder = new Geocoder(MainActivity.this);
        List<Address> addressList = null;
        try {
            addressList = geocoder.getFromLocationName(locationName, 1);
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(MainActivity.this, "Error retrieving location", Toast.LENGTH_SHORT).show();
        }

        if (addressList != null && !addressList.isEmpty()) {
            Address selectedAddress = addressList.get(0);
            LatLng selectedLocation = new LatLng(selectedAddress.getLatitude(), selectedAddress.getLongitude());
            putRedMarkerAndMoveCamera(selectedLocation, selectedAddress.getAddressLine(0));
        } else {
            Toast.makeText(MainActivity.this, "Location not found", Toast.LENGTH_SHORT).show();
        }
    }

    private void getLastLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, FINE_PERMISSION_CODE);
            return;
        }
        Task<Location> locationTask = fusedLocationProviderClient.getLastLocation();
        locationTask.addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location != null) {
                    currentLocation = location;
                    if (myMap != null) {
                        LatLng currentLatLng = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
                        putRedMarkerAndMoveCamera(currentLatLng, "My Location");
                    }
                }
            }
        });
    }

    // Move the camera to the current location
    private void moveCameraToCurrentLocation() {
        if (currentLocation != null && myMap != null) {
            LatLng currentLatLng = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
            putRedMarkerAndMoveCamera(currentLatLng, "My Location");
        } else {
            Toast.makeText(MainActivity.this, "Current location is not available", Toast.LENGTH_SHORT).show();
        }
    }

    // General method to place a red marker and move the camera
    private void putRedMarkerAndMoveCamera(LatLng latLng, String title) {
        myMap.clear(); // Clear previous markers if necessary
        myMap.addMarker(new MarkerOptions()
                .position(latLng)
                .title(title)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
        myMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15), 2000, null);
    }

    private void startLocationSharing() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, FINE_PERMISSION_CODE);
            return;
        }

        myMap.setMyLocationEnabled(true);
        myMap.getUiSettings().setMyLocationButtonEnabled(false);

        // Set continuous location updates with compass bearing
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(1000);  // Update location every 1 second
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (locationResult == null) return;
                Location location = locationResult.getLastLocation();
                if (location != null && myMap != null) {
                    LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                    float bearing = location.getBearing();  // Device's direction

                    CameraPosition cameraPosition = new CameraPosition.Builder()
                            .target(currentLatLng)
                            .zoom(25)
                            .tilt(65)
                            .bearing(bearing)
                            .build();
                    myMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), 1000, null);
                }
            }
        };

        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, new Handler().getLooper());
    }

    // Stop location sharing
    private void stopLocationSharing() {
        // Disable location updates
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);

        // Disable the My Location layer
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            myMap.setMyLocationEnabled(false);
        }

        // Reset the camera to a normal 2D view
        if (myMap != null) {
            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()))  // Move back to current location
                    .zoom(15)
                    .tilt(0)
                    .bearing(0)
                    .build();
            myMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), 1000, null);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        myMap = googleMap;
        myMap.getUiSettings().setCompassEnabled(true); // Enable compass
        myMap.getUiSettings().setZoomControlsEnabled(true); //Enable zoom
        myMap.getUiSettings().setZoomGesturesEnabled(true); //Enable zoom gesture (2 fingers)
        getLastLocation();

        myMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                putRedMarkerAndMoveCamera(latLng, "Selected Location");
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == FINE_PERMISSION_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getLastLocation();
        } else {
            Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.mapNormal) {
            myMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        }
        if (id == R.id.mapHybird) {
            myMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        }
        if (id == R.id.mapSattelite) {
            myMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
        }
        if (id == R.id.mapTerrain) {
            myMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
        }
        return super.onOptionsItemSelected(item);
    }
}
