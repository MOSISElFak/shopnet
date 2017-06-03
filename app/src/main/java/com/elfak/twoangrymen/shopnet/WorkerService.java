package com.elfak.twoangrymen.shopnet;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class WorkerService extends Service implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener  {

    private GoogleApiClient mGAPIClient;
    private LocationRequest mLocationRequest = new LocationRequest().setFastestInterval(5000).setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY).setInterval(5000);
    private Location mLastKnownLocation;
    ArrayList<Korisnik> korisnici = new ArrayList<>();
    ArrayList<Popust> popusti = new ArrayList<>();
    //na svakih N promena lokacije trazimo nove podatke sa servera o popustima
    //na svakih N promena lokacije osvezavamo i prijatelje
    private long locationChangedCounter = 0;
    private final int popustFrequency = 2;
    private final int prijateljFrequency = 3;
    private boolean sendNotifications = true;


    public WorkerService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public int onStartCommand(Intent aIntent, int flags, int startId){
        super.onStartCommand(aIntent, flags, startId);

        if(aIntent != null) {
            //Toast.makeText(this, "Ne saljem notifikacije.", Toast.LENGTH_SHORT).show();
            sendNotifications = false;
            if (aIntent.getAction() == MapsActivity.INTENT_OSVEZI_POPUSTE) {
                osveziPopuste();
                //Toast.makeText(this, "Stigao intent da se osveze markeri.", Toast.LENGTH_SHORT).show();
            }
            if (aIntent.getAction() == MapsActivity.INTENT_OSVEZI_PRIJATELJE) {
                //Toast.makeText(this, "Stigao intent da se osveze prijatelji.", Toast.LENGTH_SHORT).show();
                osveziPrijatelje();
            }
        }
        if(aIntent == null
                || (flags & START_FLAG_REDELIVERY) != 0
                || aIntent.getAction() == MapsActivity.INTENT_AKTIVIRAJ_NOTIFIKACIJE){
            //Toast.makeText(this, "Saljem notifikacije.", Toast.LENGTH_SHORT).show();
            sendNotifications = true;
        }

        mGAPIClient.connect();
        return Service.START_STICKY;
    }

    public void onCreate(){
        Toast.makeText(this, "Pozadinski servis pokrenut.", Toast.LENGTH_SHORT).show();
        /*
        Intent testBroadcast = new Intent();
        testBroadcast.setAction(MapsActivity.BROADCAST_TEST);
        sendBroadcast(testBroadcast);
        */

        //povezujemo se na GAPI
        if(mGAPIClient == null){
            mGAPIClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
    }

    public void onDestroy(){
        Toast.makeText(this, "Pozadinski servis zaustavljen.", Toast.LENGTH_SHORT).show();
        handlerPeriodicnogOsvezavanja.removeCallbacks(periodicnoOsvezavanje);
        mGAPIClient.disconnect();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        //premesti kameru na trenutnu lokaciju korisnika
        Location lastLocation = LocationServices.FusedLocationApi.getLastLocation(mGAPIClient);
        if (lastLocation != null) {
            Intent testBroadcast = new Intent();
            testBroadcast.setAction(MapsActivity.BROADCAST_TEST);
            testBroadcast.putExtra("INITLOCATION", lastLocation);
            sendBroadcast(testBroadcast);
        }else{
            Toast.makeText(this, "Trenutna lokacija nije dostupna. Proverite dozvole i ukljucite lokaciju.", Toast.LENGTH_SHORT).show();
        }
        //aktiviramo pracenje lokacije
        LocationServices.FusedLocationApi.requestLocationUpdates(mGAPIClient, mLocationRequest, this);
        handlerPeriodicnogOsvezavanja.post(periodicnoOsvezavanje);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Toast.makeText(this, "Google Play Location servis nije dostupan.", Toast.LENGTH_SHORT).show();
        //TODO STOP APPLICATION
    }

    @Override
    public void onLocationChanged(Location location) {
        if(location == null) return;
        mLastKnownLocation = location;
        //broadcast lokacije
        Intent testBroadcast = new Intent();
        testBroadcast.setAction(MapsActivity.BROADCAST_TEST);
        testBroadcast.putExtra("NEWLOCATION", location);
        sendBroadcast(testBroadcast);

        //salji serveru lokaciju
        JSONObject tokenObjekat = new JSONObject();
        try {
            tokenObjekat.put("token", SharedPreferencesManager.getPreferenceString(SharedPreferencesManager.STR_CURRENT_TOKEN));
            tokenObjekat.put("loklat", location.getLatitude());
            tokenObjekat.put("loklng", location.getLongitude());
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }
        new WebService().execute("/updateuser", "POST", tokenObjekat.toString());
    }

    private void osveziPopuste(){
        JSONObject tokenObjekat = new JSONObject();
        try {
            SharedPreferencesManager.init(WorkerService.this);
            tokenObjekat.put("token", SharedPreferencesManager.getPreferenceString(SharedPreferencesManager.STR_CURRENT_TOKEN));
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }
        new WebService(){
            protected void onPostExecute(ServerResponse r){
                super.onPostExecute(r);
                if(r != null) {
                    if(r.getResponseCode() == 200) {
                        try {
                            popusti.clear();
                            JSONArray nizPopusta = new JSONArray(r.getResponse());
                            for(int i=0; i < nizPopusta.length(); i++){
                                JSONObject privremeni = nizPopusta.getJSONObject(i);
                                Popust tempP = new Popust(privremeni);
                                popusti.add(tempP);
                            }//for
                            //send intent containing popusti
                            Intent testBroadcast = new Intent();
                            testBroadcast.setAction(MapsActivity.BROADCAST_TEST);
                            testBroadcast.putExtra("NEWPOPUSTI", popusti);
                            sendBroadcast(testBroadcast);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                    else if(r.getResponseCode() == 400){
                        Toast.makeText(WorkerService.this, "Server nije odgovorio!", Toast.LENGTH_LONG).show();
                    }
                }else
                    Log.e("GRESKA", "Server nije odgovorio.");
            }
        }.execute("/popusti", "POST", tokenObjekat.toString());
    }

    private void osveziPrijatelje(){
        JSONObject tokenObjekat = new JSONObject();
        try {
            tokenObjekat.put("token", SharedPreferencesManager.getPreferenceString(SharedPreferencesManager.STR_CURRENT_TOKEN));
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }
        new WebService(){
            protected void onPostExecute(ServerResponse r){
                super.onPostExecute(r);
                if(r != null) {
                    if(r.getResponseCode() == 200) {
                        try {
                            korisnici.clear();
                            JSONArray nizPopusta = new JSONArray(r.getResponse());
                            for(int i=0; i < nizPopusta.length(); i++){
                                JSONObject privremeni = nizPopusta.getJSONObject(i);
                                Korisnik tempK = new Korisnik(privremeni);
                                korisnici.add(tempK);
                            }//for
                            //send intent containing popusti
                            Intent testBroadcast = new Intent();
                            testBroadcast.setAction(MapsActivity.BROADCAST_TEST);
                            testBroadcast.putExtra("NEWKORISNICI", korisnici);
                            sendBroadcast(testBroadcast);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                    else if(r.getResponseCode() == 400){
                        Toast.makeText(WorkerService.this, "Server nije odgovorio!", Toast.LENGTH_LONG).show();
                    }
                }else
                    Log.e("GRESKA", "Server nije odgovorio.");
            }
        }.execute("/prijatelji", "POST", tokenObjekat.toString());
    }


    Handler handlerPeriodicnogOsvezavanja = new Handler();
    private Runnable periodicnoOsvezavanje = new Runnable() {
        @Override
        public void run() {
            osveziPopuste();
            osveziPrijatelje();

            if(korisnici != null && korisnici.size() > 0){
                for(Korisnik k : korisnici){
                    if(mLastKnownLocation != null && ((System.currentTimeMillis()/1000) - k.lokvreme) <= 120 ) {
                        float[] distanca = new float[1];
                        Location.distanceBetween(k.loklat, k.loklng, mLastKnownLocation.getLatitude(), mLastKnownLocation.getLongitude(), distanca);
                        if(distanca[0] <= 370 && sendNotifications){
                            Intent targetIntent = new Intent(WorkerService.this, MainActivity.class);
                            PendingIntent pIntent = PendingIntent.getActivity(WorkerService.this, 0, targetIntent, 0);
                            Notification ntfc = new Notification.Builder(WorkerService.this)
                                    .setContentTitle("Prijatelj je blizu vas!")
                                    .setContentText(k.korime)
                                    .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                                    .setContentIntent(pIntent)
                                    .build();
                            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                            nm.notify(k.id, ntfc);
                        }
                    }
                }
            }
            if(popusti != null && popusti.size() > 0){
                for(Popust p : popusti){
                    if(mLastKnownLocation != null) {
                        float[] distanca = new float[1];
                        Location.distanceBetween(p.loklat, p.loklng, mLastKnownLocation.getLatitude(), mLastKnownLocation.getLongitude(), distanca);
                        if(distanca[0] <= 500 && sendNotifications){
                            Intent targetIntent = new Intent(WorkerService.this, MainActivity.class);
                            PendingIntent pIntent = PendingIntent.getActivity(WorkerService.this, 0, targetIntent, 0);
                            Notification ntfc = new Notification.Builder(WorkerService.this)
                                    .setContentTitle("Popust od " + p.velicinapopusta + "% je blizu vas!")
                                    .setContentText(p.opispopusta)
                                    .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                                    .setContentIntent(pIntent)
                                    .build();
                            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                            nm.notify(p.id, ntfc);
                        }
                    }
                }
            }

            handlerPeriodicnogOsvezavanja.postDelayed(periodicnoOsvezavanje, 5000);
        }
    };

}
