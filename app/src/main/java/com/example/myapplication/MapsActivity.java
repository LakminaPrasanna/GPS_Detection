package com.example.myapplication;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Criteria;
import android.location.GpsSatellite;
import android.location.Location;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.location.GpsStatus;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import com.google.android.gms.location.LocationListener;

public class MapsActivity<MapActivity> extends FragmentActivity
        implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private LocationManager locationManager = null;
    private Criteria criteria = null;
    private android.location.LocationListener locationListener = null;
    private GpsStatus.NmeaListener nmeaListener = null;
    private GpsStatus.Listener gpsStatusListener = null;
    private TextView txtGPS_Quality = null;
    private TextView txtGPS_Location = null;
    private TextView txtGPS_Satellites = null;
    private GoogleMap mMap;
    ArrayList<LatLng> markerPoints;
    GoogleApiClient mGoogleApiClient;
    Location mLastLocation;
    Marker mCurrLocationMarker;
    LocationRequest mLocationRequest;
    private Handler mHandler = null;
    private GpsStatus.NmeaListener gpsListener2;
    private final int  PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 9003;
    FireBaseConnection fireBaseConnection;
    private MarkerOptions place1, place2;
    Button getDirection;
    private Polyline currentPolyline;
    List<String> gpsvList = new ArrayList();

    int noOfSatelites = 0;
    double avgSnr = 0.0;
    int gps_quality = 0;
    Double hdop = 0.0;
    Double currentLat = 0.0;
    Double currentLot = 0.0;
    TextView routeTag;
    TextView error;
    Button logData;
    protected class gpsListener2 implements GpsStatus.NmeaListener {


        @Override
        public void onNmeaReceived(long l, String s) {
            Toast.makeText(MapsActivity.this,"nuwan",Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        markerPoints = new ArrayList<LatLng>();
        fireBaseConnection = new FireBaseConnection();
        registerHandler();
        registerListener();
       // try {
            locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

           }
        getLocationPermission();
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 0, locationListener);

            locationManager.addNmeaListener(nmeaListener);

            routeTag = findViewById(R.id.txt_routeTag);
            error = findViewById(R.id.txt_accuracy);
            logData = (Button) findViewById(R.id.btn_logData);
            logData.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    try{
                        String tag = String.valueOf(routeTag.getText());
                        Double errorForAccuracy = Double.valueOf(String.valueOf(routeTag.getText()));
                        fireBaseConnection.saveLocationParameters(gps_quality,noOfSatelites,currentLat,currentLot,avgSnr,hdop,tag,errorForAccuracy);
                    }
                    catch (Exception ex){
                        Log.i("logData",ex+"");
                    }

                }
            });



    }


    private void getLocationPermission() {
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {

        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                buildGoogleApiClient();
                mMap.setMyLocationEnabled(true);
            }
        }
        else {
            buildGoogleApiClient();
            mMap.setMyLocationEnabled(true);
        }
        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                if (markerPoints.size() > 1) {
                    markerPoints.clear();
                    mMap.clear();
                }

                markerPoints.add(latLng);

                MarkerOptions options = new MarkerOptions();
                options.position(latLng);

                if (markerPoints.size() == 1) {
                    options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
                } else if (markerPoints.size() == 2) {
                    options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
                }

                mMap.addMarker(options);

                if (markerPoints.size() >= 2) {
                    LatLng origin = markerPoints.get(0);
                    LatLng dest = markerPoints.get(1);
                    LatLng origin1 = new LatLng(6.7951,79.9008);
                    LatLng dest1 = new LatLng(6.7132,79.9026);
                    // Getting URL to the Google Directions API
                    String url = getUrl(origin, dest);
                    Log.d("onMapClick", url.toString());
                    FetchUrl FetchUrl = new FetchUrl();

                    // Start downloading json data from Google Directions API
                    FetchUrl.execute(url);
                    //move map camera
                    mMap.moveCamera(CameraUpdateFactory.newLatLng(origin1));
                    mMap.animateCamera(CameraUpdateFactory.zoomTo(11));
                }
            }
        });

    }
    // Fetches data from url passed
    private class FetchUrl extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... url) {

            // For storing data from web service
            String data = "";

            try {
                // Fetching the data from web service
                data = downloadUrl(url[0]);

            } catch (Exception e) {
                Log.d("Background Task", e.toString());
            }
            return data;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            ParserTask parserTask = new ParserTask();

            // Invokes the thread for parsing the JSON data
            parserTask.execute(result);

        }
    }
        private String downloadUrl(String strUrl) throws IOException {
            String data = "";
            InputStream iStream = null;
            HttpURLConnection urlConnection = null;
            try {
                URL url = new URL(strUrl);

                // Creating an http connection to communicate with url
                urlConnection = (HttpURLConnection) url.openConnection();

                // Connecting to url
                urlConnection.connect();

                // Reading data from url
                iStream = urlConnection.getInputStream();

                BufferedReader br = new BufferedReader(new InputStreamReader(iStream));

                StringBuffer sb = new StringBuffer();

                String line = "";
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }

                data = sb.toString();
                Log.d("downloadUrl", data.toString());
                br.close();

            } catch (Exception e) {
                Log.d("Exception", e.toString());
            } finally {
                iStream.close();
                urlConnection.disconnect();
            }
            return data;
        }


        private String getUrl(LatLng origin, LatLng dest) {

        // Origin of route
        String str_origin = "origin=" + origin.latitude + "," + origin.longitude;

        // Destination of route
        String str_dest = "destination=" + dest.latitude + "," + dest.longitude;


        // Sensor enabled
        String sensor = "sensor=false";

        // Building the parameters to the web service
        String parameters = str_origin + "&" + str_dest + "&" + sensor;

        // Output format
        String output = "json";

        // Building the url to the web service
        String url = "https://maps.googleapis.com/maps/api/directions/" + output +
                "?" + parameters+"&key="+ "AIzaSyDn5EjjaGy0vuGhsroCF-bvlt4j1kVROUI";


        return url;
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }

    private class ParserTask extends AsyncTask<String, Integer, List<List<HashMap<String, String>>>> {

        // Parsing the data in non-ui thread
        @Override
        protected List<List<HashMap<String, String>>> doInBackground(String... jsonData) {

            JSONObject jObject;
            List<List<HashMap<String, String>>> routes = null;

            try {
                jObject = new JSONObject(jsonData[0]);
                Log.d("ParserTask",jsonData[0].toString());
                DataParser parser = new DataParser();
                Log.d("ParserTask", parser.toString());

                // Starts parsing data
                routes = parser.parse(jObject);
                Log.d("ParserTask","Executing routes");
                Log.d("ParserTask",routes.toString());

            } catch (Exception e) {
                Log.d("ParserTask",e.toString());
                e.printStackTrace();
            }
            return routes;
        }

        // Executes in UI thread, after the parsing process
        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> result) {
            ArrayList<LatLng> points;
            PolylineOptions lineOptions = null;

            // Traversing through all the routes
            for (int i = 0; i < result.size(); i++) {
                points = new ArrayList<>();
                lineOptions = new PolylineOptions();

                // Fetching i-th route
                List<HashMap<String, String>> path = result.get(i);

                // Fetching all the points in i-th route
                for (int j = 0; j < path.size(); j++) {
                    HashMap<String, String> point = path.get(j);

                    double lat = Double.parseDouble(point.get("lat"));
                    double lng = Double.parseDouble(point.get("lng"));
                    LatLng position = new LatLng(lat, lng);

                    points.add(position);
                }

                // Adding all the points in the route to LineOptions
                lineOptions.addAll(points);
                lineOptions.width(14);
                lineOptions.color(Color.BLUE);

                Log.d("onPostExecute","onPostExecute lineoptions decoded");

            }

            // Drawing polyline in the Google Map for the i-th route
            if(lineOptions != null) {
                mMap.addPolyline(lineOptions);
            }
            else {
                Log.i("onPostExecute","without Polylines drawn");
            }
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {

        mLastLocation = location;
        if (mCurrLocationMarker != null) {
            mCurrLocationMarker.remove();
        }

        Log.i("location_accuracy",location.getAccuracy()+"");

        //Place current location marker
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(latLng);
        markerOptions.title("Current Position");
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA));
        mCurrLocationMarker = mMap.addMarker(markerOptions);

        //move map camera
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(11));

        //stop location updates
        if (mGoogleApiClient != null) {
        //    LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        }

    }

    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;
    public boolean checkLocationPermission(){
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Asking user if explanation is needed
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

                //Prompt the user once explanation has been shown
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);


            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);
            }
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted. Do the
                    // contacts-related task you need to do.
                    if (ContextCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {

                        if (mGoogleApiClient == null) {
                            buildGoogleApiClient();
                        }
                        mMap.setMyLocationEnabled(true);
                    }

                } else {

                    // Permission denied, Disable the functionality that depends on this permission.
                    Toast.makeText(this, "permission denied", Toast.LENGTH_LONG).show();
                }
                return;
            }

            // other 'case' lines to check for other permissions this app might request.
            // You can add here other case statements according to your requirement.
        }
    }

    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
        locationManager.removeUpdates(locationListener);
        locationManager.removeNmeaListener(nmeaListener);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {


        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();
        //if (id == R.id.action_settings) {
        //	return true;
        //}
        return super.onOptionsItemSelected(item);
    }

    private void registerListener() {
        locationListener = new android.location.LocationListener() {

            @Override
            public void onLocationChanged(Location loc) {

//                Log.d("GPS-NMEA", loc.getLatitude() + "," + loc.getLongitude());
            }

            @Override
            public void onProviderDisabled(String provider) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
                // TODO Auto-generated method stub
                Log.d("GPS-NMEA", provider + "");

                switch (status) {
                    case LocationProvider.OUT_OF_SERVICE:
                        Log.d("GPS-NMEA", "OUT_OF_SERVICE");
                        break;
                    case LocationProvider.TEMPORARILY_UNAVAILABLE:
                        Log.d("GPS-NMEA", " TEMPORARILY_UNAVAILABLE");
                        break;
                    case LocationProvider.AVAILABLE:
                        Log.d("GPS-NMEA", "" + provider + "");

                        break;
                }

            }

        };
//
        nmeaListener = new GpsStatus.NmeaListener() {
            public void onNmeaReceived(long timestamp, String nmea) {
                //check nmea's checksum
                if (isValidForNmea(nmea)) {
                    nmeaProgress(nmea);
                    Log.d("GPS-NMEA", nmea);
                }

            }
        };
//
        gpsStatusListener = new GpsStatus.Listener() {
            @SuppressLint("MissingPermission")
            public void onGpsStatusChanged(int event) {
                // TODO Auto-generated method stub
                GpsStatus gpsStatus;
                if (ActivityCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
                gpsStatus = locationManager.getGpsStatus(null);

                switch(event)
                {
                    case GpsStatus.GPS_EVENT_FIRST_FIX:
                        //
                        gpsStatus.getTimeToFirstFix();
                        Log.d("GPS-NMEA","GPS_EVENT_FIRST_FIX");
                        break;
                    case GpsStatus.GPS_EVENT_SATELLITE_STATUS:

                        Iterable<GpsSatellite> allSatellites = gpsStatus.getSatellites();
                        Iterator<GpsSatellite> it=allSatellites.iterator();

                        int count = 0;
                        while(it.hasNext())
                        {
                            GpsSatellite gsl=(GpsSatellite)it.next();

                            if (gsl.getSnr()>0.0){
                                count++;
                            }

                        }


                        break;
                    case GpsStatus.GPS_EVENT_STARTED:
                        //Event sent when the GPS system has started.
                        Log.d("GPS-NMEA","GPS_EVENT_STARTED");
                        break;
                    case GpsStatus.GPS_EVENT_STOPPED:
                        //Event sent when the GPS system has stopped.
                        Log.d("GPS-NMEA","GPS_EVENT_STOPPED");
                        break;
                    default :
                        break;
                }
            }

        };

    }

    private void registerHandler(){

        mHandler = new Handler(Looper.getMainLooper()) {
            public void handleMessage(Message msg) {

                String str = (String) msg.obj;
                String[] rawNmeaSplit = str.split(",");

//                fireBaseConnection.saveLocationParameters(Integer.parseInt(rawNmeaSplit[6]),Integer.parseInt(rawNmeaSplit[7]),
//                        Double.parseDouble(rawNmeaSplit[2]),Double.parseDouble(rawNmeaSplit[4]));
//                txtGPS_Quality.setText(rawNmeaSplit[6]);// rawNmeaSplit[6]
//                txtGPS_Location.setText(rawNmeaSplit[2] + " " + rawNmeaSplit[3] + "," + rawNmeaSplit[4] + " " + rawNmeaSplit[5]);
//                txtGPS_Satellites.setText(rawNmeaSplit[7]);
                hdop = Double.parseDouble(rawNmeaSplit[9]);
                gps_quality = Integer.parseInt(rawNmeaSplit[6]);
                noOfSatelites = Integer.parseInt(rawNmeaSplit[7]);
                currentLat = Double.parseDouble(rawNmeaSplit[2]);
                currentLot = Double.parseDouble(rawNmeaSplit[4]);
             // fireBaseConnection.saveLocationParameters(gps_quality,noOfSatelites,currentLat,currentLot,avgSnr,hdop,"",12.0);
              Log.i("nuwan",gps_quality+" hhhh");

            }
        };

    }

    private void nmeaProgress(String rawNmea){

        String[] rawNmeaSplit = rawNmea.split(",");

        if (rawNmeaSplit[0].equalsIgnoreCase("$GPGGA")){
            //send GGA nmea data to handler
            Message msg = new Message();
            String x="$GPGGA,173825.969,0647.6217,N,07954.0079,E,0,0,,6.9,M,-96.7,M,,*67\n";
            msg.obj = rawNmea;
            mHandler.sendMessage(msg);
        }
        if (rawNmeaSplit[0].equalsIgnoreCase("$GPGSV")) {

        }

        if (rawNmeaSplit[0].equalsIgnoreCase("$GPGSV")) {
            Log.i("nmeaProgress", rawNmea);
          PrintGPGSV(rawNmea);
        }

    }


    private boolean isValidForNmea(String rawNmea){
        boolean valid = true;
        byte[] bytes = rawNmea.getBytes();
        int checksumIndex = rawNmea.indexOf("*");

        byte checksumCalcValue = 0;
        int checksumValue;


        if ((rawNmea.charAt(0) != '$') || (checksumIndex==-1)){
            valid = false;
        }
        //
        if (valid){
            String val = rawNmea.substring(checksumIndex + 1, rawNmea.length()).trim();
            checksumValue = Integer.parseInt(val, 16);
            for (int i = 1; i < checksumIndex; i++){
                checksumCalcValue = (byte) (checksumCalcValue ^ bytes[i]);
            }
            if (checksumValue != checksumCalcValue){
                valid = false;
            }
        }
        return valid;
    }
    void PrintGPGSV(String rawNmea){
        String[] rawNmeaSplit =rawNmea.split(",");
        addToArryList(rawNmeaSplit);

    }
    void addToArryList(String[] rawNmeaSplit){
        double sum = 0;
        int noOfSat = 0;
        if(gpsvList.size()== 60 || gpsvList.size() == 68){
            if(!gpsvList.get(6).isEmpty()){
                sum = sum + Double.valueOf(gpsvList.get(6));
                noOfSat++;
            }
            if(!gpsvList.get(10).isEmpty()){
                sum = sum + Double.valueOf(gpsvList.get(10));
                noOfSat++;
            }
            if(!gpsvList.get(14).isEmpty()){
                sum = sum + Double.valueOf(gpsvList.get(14));
                noOfSat++;
            }
            if(!gpsvList.get(18).isEmpty()){
                sum = sum + Double.valueOf(gpsvList.get(18));
                noOfSat++;
            }
            if(!gpsvList.get(26).isEmpty()){
                sum = sum + Double.valueOf(gpsvList.get(26));
                noOfSat++;
            }
            if(!gpsvList.get(30).isEmpty()){
                sum = sum + Double.valueOf(gpsvList.get(30));
                noOfSat++;
            }
            if(!gpsvList.get(34).isEmpty()){
                sum = sum + Double.valueOf(gpsvList.get(34));
                noOfSat++;
            }
            if(!gpsvList.get(38).isEmpty()){
                sum = sum + Double.valueOf(gpsvList.get(38));
                noOfSat++;
            }
            if(!gpsvList.get(46).isEmpty()){
                sum = sum + Double.valueOf(gpsvList.get(46));
                noOfSat++;
            }
            if(!gpsvList.get(50).isEmpty()){
                sum = sum + Double.valueOf(gpsvList.get(50));
                noOfSat++;
            }
            if(!gpsvList.get(54).isEmpty()){
                sum = sum + Double.valueOf(gpsvList.get(54));
                noOfSat++;
            }
            if(!gpsvList.get(58).isEmpty()){
                sum = sum + Double.valueOf(gpsvList.get(58));
                noOfSat++;
            }
            if( gpsvList.size()>60 )
                if(!gpsvList.get(66).isEmpty() ){
                    sum = sum + Double.valueOf(gpsvList.get(66));
                    noOfSat++;
                }
            Log.i("nmeaProgress","summation= "+sum+" noOfSat ="+noOfSat+"average= "+ sum/noOfSat);

            avgSnr = sum/noOfSat;
        }
        for(String el :rawNmeaSplit){
            if(gpsvList.size() == 69){
                gpsvList.clear();
            }else{
                // Log.i("nmeaProgress",gpsvList+"");
                gpsvList.add(el);
            }
        }

    }
}


