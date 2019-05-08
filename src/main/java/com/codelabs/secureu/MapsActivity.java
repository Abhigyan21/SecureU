package com.codelabs.secureu;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.SharedPreferencesCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.location.LocationListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback, View.OnClickListener, LocationListener {

    private GoogleMap mMap;
    private Button getLoc, help, emergency;
    private Toolbar toolbar;
    private UpdateDB dbHelper;
    private double latitude, longitude;
    private LocationManager locationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        setUpToolbar();

        try {
            initializeMap();
        } catch (Exception e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }

        dbHelper = new UpdateDB(this);

        getLoc = (Button) findViewById(R.id.get_cur_loc);
        help = (Button) findViewById(R.id.help);
        emergency = (Button) findViewById(R.id.emergency);

        getLoc.setOnClickListener(this);
        help.setOnClickListener(this);
        emergency.setOnClickListener(this);

    }

    private void setUpToolbar() {
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setLogo(R.mipmap.ic_launcher);
    }

    private void initializeMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if (mMap != null) {
            mMap.getUiSettings().setZoomGesturesEnabled(true);
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                Intent intent = new Intent();
                intent.setAction(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                return;
            }
            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(true);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.get_cur_loc:
                setCurrentLocation();

                if(latitude == 0 && longitude == 0)
                    Toast.makeText(MapsActivity.this, "No location found! Try again", Toast.LENGTH_SHORT).show();
                else {
                    Toast.makeText(MapsActivity.this, "Location: " + latitude + ", " + longitude, Toast.LENGTH_SHORT).show();
                    mMap.addMarker(new MarkerOptions().position(new LatLng(latitude, longitude))).setTitle("My Location");
                }
                break;
            case R.id.help:
                MapsActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        getHelp();
                    }
                });
                break;
            case R.id.emergency:
                showEmergency();
                break;
        }
    }

    private void showEmergency() {
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.contact_list);
        dialog.setTitle("Emergency");

        ListView contactList = (ListView) dialog.findViewById(R.id.contacts_list);
        ArrayList<String> list = new ArrayList();
        dialog.show();
        String[] data = {"Ambulance", "Hospital", "Police", "Fire Services"};
        for (String temp : data)
            list.add(temp);
        ArrayAdapter<String> adapter = new ArrayAdapter(MapsActivity.this,
                R.layout.activity_listview,
                list);

        contactList.setAdapter(adapter);
        adapter.notifyDataSetChanged();
        contactList.setOnItemClickListener(new ListClick());
    }

    private void getHelp() {
        setCurrentLocation();

        List<Address> addressList = null;
        String addressString;

        Geocoder geocoder = new Geocoder(MapsActivity.this, Locale.getDefault());
        try {
            if(latitude == 0 && longitude == 0){
                Toast.makeText(MapsActivity.this, "Unable to get address!", Toast.LENGTH_SHORT).show();
                Thread.sleep(Toast.LENGTH_SHORT);
                return;
            }
            addressList = geocoder.getFromLocation(latitude, longitude, 1);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

        if(addressList == null){
            Toast.makeText(MapsActivity.this, "Unable to retrieve address", Toast.LENGTH_SHORT).show();
            return;
        }
        Address address = addressList.get(0);
        addressString = address.getAddressLine(0);

        LatLng latLng = new LatLng(address.getLatitude(), address.getLongitude());
        mMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));

        Toast.makeText(MapsActivity.this, "Address: " + addressString, Toast.LENGTH_SHORT).show();
        sendMessage(String.valueOf(latitude), String.valueOf(longitude), addressString);

    }

    public void sendMessage(String latitudeString, String longitudeString, String addressString) {
        final String address = addressString;
        final String latitude = latitudeString;
        final String longitude = longitudeString;


        /*Thread smsSender = new Thread(new Runnable() {
            @Override
            public void run() {*/
                try {
                    SmsManager sms = SmsManager.getDefault();
                    String message = "HELP!My address is: " + address + "\n" + "My address link is " + "https://maps.google.com/?q=" + latitude + "," + longitude + "\nMessage send from SecureU.";
                    ArrayList<String> contactsList = dbHelper.retrieve();

                    if (contactsList.size() <= 0) {
                        Toast.makeText(MapsActivity.this, "Unable to send message! No contacts found.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    for (String num : contactsList) {
                        switch (num.length()) {
                            case 10:
                                sms.sendTextMessage(num, null, message, null, null);
                                Toast.makeText(MapsActivity.this, "Message Sent", Toast.LENGTH_SHORT).show();
                                break;
                            case 11:
                                sms.sendTextMessage(num.substring(1, num.length()), null, message, null, null);
                                Toast.makeText(MapsActivity.this, "Message Sent", Toast.LENGTH_SHORT).show();
                                break;
                            case 12:
                                sms.sendTextMessage(num.substring(2, num.length()), null, message, null, null);
                                Toast.makeText(MapsActivity.this, "Message Sent", Toast.LENGTH_SHORT).show();
                                break;
                            case 13:
                                sms.sendTextMessage(num.substring(3, num.length()), null, message, null, null);
                                Toast.makeText(MapsActivity.this, "Message Sent", Toast.LENGTH_SHORT).show();
                                break;
                            default:
                                break;
                        }
                    }
                    Thread.sleep(Toast.LENGTH_SHORT);
                } catch (Exception e) {
                    //Toast.makeText(MapsActivity.this, e.toString(), Toast.LENGTH_SHORT).show();
                    Log.e("SMS sender",e.getMessage());
                }
            //}
        /*});
        smsSender.start();*/
    }


    private void setCurrentLocation() {
        locationManager = (LocationManager) MapsActivity.this.getSystemService(Context.LOCATION_SERVICE);
        boolean enabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

        AlertDialog dialog = new AlertDialog.Builder(MapsActivity.this).
                setTitle("GPS Off").
                setMessage("Please enable gps.").
                setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent();
                        intent.setAction(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivity(intent);
                    }
                }).create();

        if (enabled == false)
            dialog.show();


        if (ActivityCompat.checkSelfPermission(
                this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(
                        this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(MapsActivity.this, "Location permissions not granted!", Toast.LENGTH_SHORT).show();

            try {
                Thread.sleep(Toast.LENGTH_SHORT);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        if (location != null) {
            Log.e("TAG", "GPS is on");
            latitude = location.getLatitude();
            longitude = location.getLongitude();
        } else
            Toast.makeText(MapsActivity.this, "Location not found", Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.settings:
                Intent settingsIntent = new Intent(MapsActivity.this, Settings.class);
                startActivity(settingsIntent);
                break;
            case R.id.exit:
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onLocationChanged(Location location) {
        latitude = location.getLatitude();
        longitude = location.getLongitude();
        mMap.addMarker(new MarkerOptions().position(new LatLng(latitude, longitude)));
    }

    private class ListClick implements AdapterView.OnItemClickListener {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            try {
                String item = (String) parent.getItemAtPosition(position);
                switch (item) {
                    case "Police":
                        startActivity(new Intent("android.intent.action.CALL", Uri.parse("tel:100")));
                        break;
                    case "Hospital":
                        startActivity(new Intent("android.intent.action.CALL", Uri.parse("tel:102")));
                        break;
                    case "Ambulance":
                        startActivity(new Intent("android.intent.action.CALL", Uri.parse("tel:108")));
                        break;
                    case "Fire Services":
                        startActivity(new Intent("android.intent.action.CALL", Uri.parse("tel:101")));
                        break;
                }

            } catch (Exception e) {
                Toast.makeText(MapsActivity.this, e.toString(), Toast.LENGTH_LONG).show();
            }
        }
    }
}
