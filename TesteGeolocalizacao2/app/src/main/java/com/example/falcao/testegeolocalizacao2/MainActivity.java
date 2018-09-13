/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.falcao.testegeolocalizacao2;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;

import java.text.DateFormat;


/**
 * The only activity in this sample. Displays UI widgets for requesting and removing location
 * updates, and for the batched location updates that are reported.
 *
 * Location updates requested through this activity continue even when the activity is not in the
 * foreground. Note: apps running on "O" devices (regardless of targetSdkVersion) may receive
 * updates less frequently than the interval specified in the {@link LocationRequest} when the app
 * is no longer in the foreground.
 */
public class MainActivity extends AppCompatActivity implements LocationListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    final String TAG = "GPS";
    private long TEMPO_ATUALIZACAO = 5 * 1000;  /* 5 secs */
    private long INTERVALO_RAPIDO = 2*1000; /* 2 sec */
    static final int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    //private SphericalUtil sphericalUtil = new SphericalUtil();

    GoogleApiClient gac;
    LocationRequest locationRequest;
    TextView tvLatitude, tvLongitude, tvTime, tvLatitude2, tvLongitude2, tvResultado, tvArea;

    private void buildGoogleApiClient() {
        int resultCode = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this);
        switch (resultCode) {
            case ConnectionResult.SUCCESS:
                Log.d(TAG, "Google Play Services is ready to go!");
                break;
            default:
                showPlayServicesError(resultCode);
                return;
        }
    }

    private void showPlayServicesError(int errorCode) {
        GoogleApiAvailability.getInstance().showErrorDialogFragment(this, errorCode, 10,
                new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialogInterface) {
                        finish();
                    }
                });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tvLatitude = (TextView) findViewById(R.id.tvLatitude);
        tvLongitude = (TextView) findViewById(R.id.tvLongitude);
        tvTime = (TextView) findViewById(R.id.tvTime);
        tvLatitude2 = (TextView) findViewById(R.id.latitude2);
        tvLongitude2 = (TextView) findViewById(R.id.longitude2);
        tvResultado = (TextView) findViewById(R.id.distanciaResultante);
        tvArea = (TextView) findViewById(R.id.areaTeste);

        isGooglePlayServicesAvailable();

        if(!isLocationEnabled())
            showAlert();

        locationRequest = new LocationRequest();
        locationRequest.setInterval(TEMPO_ATUALIZACAO);
        locationRequest.setFastestInterval(INTERVALO_RAPIDO);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        gac = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    @Override
    protected void onStart() {
        gac.connect();
        super.onStart();
    }

    @Override
    protected void onStop() {
        gac.disconnect();
        super.onStop();
    }

    @Override
    public void onLocationChanged(Location location) {
        if (location != null) {
            updateUI(location);
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);

            return;
        }
        Log.d(TAG, "onConnected");

        Location ll = LocationServices.FusedLocationApi.getLastLocation(gac);
        Log.d(TAG, "LastLocation: " + (ll == null ? "NO LastLocation" : ll.toString()));

        LocationServices.FusedLocationApi.requestLocationUpdates(gac, locationRequest, this);
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(MainActivity.this, "Permission was granted!", Toast.LENGTH_LONG).show();

                    try{
                        LocationServices.FusedLocationApi.requestLocationUpdates(
                                gac, locationRequest, this);
                    } catch (SecurityException e) {
                        Toast.makeText(MainActivity.this, "SecurityException:\n" + e.toString(),
                                Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(MainActivity.this, "Permission denied!", Toast.LENGTH_LONG).show();
                }
                return;
            }
        }
    }

    @Override
    public void onConnectionSuspended(int i) {}

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Toast.makeText(MainActivity.this, "onConnectionFailed: \n" + connectionResult.toString(),
                Toast.LENGTH_LONG).show();
        Log.d("DDD", connectionResult.toString());
    }

    private void updateUI(Location loc) {
        Regiao regiao = new Regiao();
        regiao.setRaio(10);
        float [] result = new float[3];
        Log.d(TAG, "updateUI");
        LatLng latlng1 = new LatLng(loc.getLatitude(), loc.getLongitude());
        LatLng latlng2 = new LatLng(-8.03494,-35.000000 );
        CircleOptions circle = new CircleOptions ();
        circle.center(latlng2);
        circle.radius(10000);
        //DecimalFormat decimalFormat = new DecimalFormat("#.##");
        //String aux1 = String.valueOf(SphericalUtil.computeDistanceBetween(latlng1, latlng2));
        //regiao.setArea(SphericalUtil.interpolate(latlng1, latlng2, regiao.getRaio()));
        //result = distanceCalculator.distance_Between_LatLong(loc.getLatitude(), loc.getLongitude(),-8.2000000,-35.000000);
        //regiao.setArea(distanceCalculator.greatCircleInFeet(latlng2, latlng1));
        loc.distanceBetween(latlng1.latitude, latlng1.longitude, latlng2.latitude, latlng2.longitude, result);
        tvLatitude.setText(Double.toString(loc.getLatitude()));
        tvLongitude.setText(Double.toString(loc.getLongitude()));
        tvLatitude2.setText(Double.toString(-8.100000));
        tvLongitude2.setText(Double.toString(-35.000000));
        //tvArea.setText(Double.toString(regiao.getArea()));
        tvResultado.setText(Double.toString(result[0]));
        tvTime.setText(DateFormat.getTimeInstance().format(loc.getTime()));
       /* boolean flipflop = isCircleContains(circle, latlng1);
        if(flipflop == true){
            showPoupUp();
        }*/
    }

    private boolean isLocationEnabled() {
        LocationManager locationManager =
                (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    private boolean isGooglePlayServicesAvailable() {
        final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                apiAvailability.getErrorDialog(this, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST)
                        .show();
            } else {
                Log.d(TAG, "This device is not supported.");
                finish();
            }
            return false;
        }
        Log.d(TAG, "This device is supported.");
        return true;
    }

    private void showAlert() {
        final AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle("Enable Location")
                .setMessage("Your Locations Settings is set to 'Off'.\nPlease Enable Location to " +
                        "use this app")
                .setPositiveButton("Location Settings", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface paramDialogInterface, int paramInt) {

                        Intent myIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivity(myIntent);
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface paramDialogInterface, int paramInt) {

                    }
                });
        dialog.show();
    }

    private void showPoupUp(){
        final AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle("Vai subir poupup")
                .setMessage("Entrou no raio")
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface paramDialogInterface, int paramInt) {

                    }
                });
        dialog.show();
    }

    /*private boolean isCircleContains(CircleOptions circle, LatLng point) {
        double r = circle.getRadius();
        LatLng center = circle.getCenter();
        double cX = center.latitude;
        double cY = center.longitude;
        double pX = point.latitude;
        double pY = point.longitude;

        float[] results = new float[1];

        Location.distanceBetween(cX, cY, pX, pY, results);

        if(results[0] < r) {
            return true;
        } else {
            return false;
        }
    }
    */
}