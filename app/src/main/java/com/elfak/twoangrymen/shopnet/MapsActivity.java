package com.elfak.twoangrymen.shopnet;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.ActivityCompat;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MapsActivity extends AppCompatActivity
        implements OnMapReadyCallback, GoogleMap.OnInfoWindowClickListener, GoogleMap.OnMapClickListener {

    public static final int INTENT_DODAJ_POPUST = 1;
    public static final String INTENT_OSVEZI_POPUSTE = "REFRESH_POPUSTI";
    public static final String INTENT_OSVEZI_PRIJATELJE = "REFRESH_PRIJATELJI";
    public static final String INTENT_AKTIVIRAJ_NOTIFIKACIJE = "AKTIVIRAJ_NOTIFIKACIJE";
    public static final String BROADCAST_TEST = "com.elfak.twoangrymen.shopnet.BROADCAST_TEST";


    private GoogleMap mMap;
    private Location mLastLocation;
    private ArrayList<Marker> mMarkersPopusti = new ArrayList<>();
    private ArrayList<Marker> mMarkersPrijatelji = new ArrayList<>();
    private ListView listaView;
    private List<String> stavkeListe;
    private ArrayAdapter<String> adapterListe;
    private Button btnDodajPopust;
    private boolean mServisAktivan = true;
    private ConstraintLayout mSlidingLayout;

    private boolean mModPretrage = false;
    public static int mFilterTipa = -1;

    private BroadcastReceiver bcReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.hasExtra("INITLOCATION")){
                mLastLocation = intent.getParcelableExtra("INITLOCATION");
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude()), 15));
            }
            if(intent.hasExtra("NEWLOCATION")){
                mLastLocation = intent.getParcelableExtra("NEWLOCATION");
            }
            if(intent.hasExtra("NEWPOPUSTI")){
                ArrayList<Popust> p = (ArrayList<Popust>) intent.getSerializableExtra("NEWPOPUSTI");
                if(p.size() > 0){
                    for(Marker m : mMarkersPopusti){
                        m.remove();
                    }
                    for(int i=0; i < p.size(); i++){
                        Popust pop = p.get(i);
                        Marker tempMarker = mMap.addMarker(new MarkerOptions()
                                .position( new LatLng(pop.loklat, pop.loklng))
                                .title("Popust: " + pop.velicinapopusta + "%; " + TipoviPopusta.tipoviPopusta.get(pop.tippopusta))
                                .snippet("(dodirni za još informacija)")
                        );
                        tempMarker.setTag(pop);
                        mMarkersPopusti.add(tempMarker);
                    }
                }
            }
            if(intent.hasExtra("NEWKORISNICI")){
                ArrayList<Korisnik> k = (ArrayList<Korisnik>) intent.getSerializableExtra("NEWKORISNICI");
                if(k.size() > 0){
                    for(Marker m : mMarkersPrijatelji){
                        m.remove();
                    }
                    for(int i=0; i < k.size(); i++){
                        Korisnik kor = k.get(i);
                        String receivedBase64 = kor.slika;
                        receivedBase64 = receivedBase64.replaceAll("\\s", "+");
                        byte[] imageBytes = Base64.decode(receivedBase64, Base64.DEFAULT);
                        Bitmap thumb = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
                        Marker tempMarker = mMap.addMarker(new MarkerOptions()
                                .position( new LatLng(kor.loklat, kor.loklng))
                                .title(kor.korime)
                                .icon(BitmapDescriptorFactory.fromBitmap(thumb))
                                .anchor(0.5f, 0.5f)
                                .snippet("(dodirni za još informacija)")
                        );
                        tempMarker.setTag(kor);
                        mMarkersPrijatelji.add(tempMarker);
                    }
                }
            }
        }//onReceive
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        //set mSlidingLayout
        mSlidingLayout = (ConstraintLayout)findViewById(R.id.sliding_layout);

        //popuni listu
        stavkeListe = new ArrayList<String>();
        stavkeListe.add("Prijatelji");
        stavkeListe.add("Pretraga");
        if(mServisAktivan)
            stavkeListe.add("Deaktiviraj servis");
        else
            stavkeListe.add("Aktiviraj servis");
        stavkeListe.add("Izloguj se");

        adapterListe = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, stavkeListe);
        listaView = (ListView)findViewById(R.id.list_view_drawer);
        listaView.setAdapter(adapterListe);

        //ucitaj tipove popusta
        TipoviPopusta.ucitajTipove();

        //ucitaj informacije o korisniku
        JSONObject tokenJSON = new JSONObject();
        try {
            tokenJSON.put("token", SharedPreferencesManager.getPreferenceString(SharedPreferencesManager.STR_CURRENT_TOKEN));
            new WebService(){
                protected void onPostExecute(ServerResponse r){
                    super.onPostExecute(r);
                    if(r != null) {
                        if(r.getResponseCode() == 200) {
                            try {
                                JSONObject primljeniJSON = new JSONObject(r.getResponse());
                                //korisnicko ime
                                String primljenoKorime = primljeniJSON.getString("korime");
                                ((TextView) findViewById(R.id.text_korime_display)).setText(primljenoKorime);
                                SharedPreferencesManager.setPreferenceString(SharedPreferencesManager.STR_KORIME, primljenoKorime);
                                //slika
                                String receivedBase64 = primljeniJSON.getString("slika");
                                receivedBase64 = receivedBase64.replaceAll("\\s", "+");
                                byte[] imageBytes = Base64.decode(receivedBase64, Base64.DEFAULT);
                                Bitmap imageBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
                                ImageView iv = (ImageView) findViewById(R.id.image_view_profilna);
                                iv.setImageBitmap(imageBitmap);
                            } catch (JSONException e) {
                                e.printStackTrace();
                                Log.e("GRESKA", "Los JSON odgovor.");
                            }
                        }
                        else if(r.getResponseCode() == 400){
                            Toast.makeText(MapsActivity.this, "Login podaci nisu ispravni!", Toast.LENGTH_LONG).show();
                        }
                    }else
                        Log.e("GRESKA", "Server nije odgovorio.");
                }
            }.execute("/userinfo", "POST", tokenJSON.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        //click handleri
        //dugme za dodavanje popusta
        btnDodajPopust = (Button) findViewById(R.id.btn_dodaj_popust);
        btnDodajPopust.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                    if (mLastLocation != null) {
                        Intent dodavanjePopusta = new Intent(MapsActivity.this, DodajPopustActivity.class);
                        dodavanjePopusta.putExtra("LAT", mLastLocation.getLatitude());
                        dodavanjePopusta.putExtra("LNG", mLastLocation.getLongitude());
                        startActivityForResult(dodavanjePopusta, INTENT_DODAJ_POPUST);
                    }
                    else{
                        Toast.makeText(MapsActivity.this, "Trenutna lokacija nije dostupna. Proverite dozvole i ukljucite lokaciju.", Toast.LENGTH_SHORT).show();
                    }
                }
        });
        listaView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //aktiviranje/deaktiviranje servisa
                if(position == 2){
                    mServisAktivan = !mServisAktivan;
                    if(mServisAktivan)
                        stavkeListe.set(position, "Deaktiviraj servis");
                    else
                        stavkeListe.set(position, "Aktiviraj servis");
                    //apdejtuj dataset jer menjamo listu iz drugog threada
                    adapterListe.notifyDataSetChanged();
                }
                if(position == 3){
                    JSONObject tokenJSON = new JSONObject();
                    try {
                        tokenJSON.put("token", SharedPreferencesManager.getPreferenceString(SharedPreferencesManager.STR_CURRENT_TOKEN));
                        new WebService() {
                            protected void onPostExecute(ServerResponse r) {
                                SharedPreferencesManager.setPreferenceString("token", "");
                                Intent vratiSeNaLogin = new Intent(MapsActivity.this, MainActivity.class);
                                startActivity(vratiSeNaLogin);
                                Intent gasiServis = new Intent(MapsActivity.this, WorkerService.class);
                                stopService(gasiServis);
                                finish();
                            }
                        }.execute("/logout", "POST", tokenJSON.toString());
                    }
                    catch (JSONException e){
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        if(requestCode == INTENT_DODAJ_POPUST){
            if(resultCode == Activity.RESULT_OK){
                //Toast.makeText(this, "Dodavanje ok.", Toast.LENGTH_SHORT).show();
                Intent requestRefresh = new Intent(MapsActivity.this, WorkerService.class);
                requestRefresh.setAction(MapsActivity.INTENT_OSVEZI_POPUSTE);
                startService(requestRefresh);
            }
        }
        else{
            Toast.makeText(this, "Dodavanje otkazano.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
            //aktiviramo click listener za title na markerima
            mMap.setOnInfoWindowClickListener(this);
            mMap.setOnMapClickListener(this);
            //startujemo servis
            Intent pokreniServis = new Intent(MapsActivity.this, WorkerService.class);
            startService(pokreniServis);
            Log.v("PERMLOC", "Dozvola je vec ok.");
        } else {
            Log.e("PERMLOC", "Dozvola odbijena u onMapReady.");
        }
    }

    @Override
    public void onStart(){
        //bcrec filter podesavanje
        IntentFilter filterZaBrotkast = new IntentFilter();
        filterZaBrotkast.addAction(MapsActivity.BROADCAST_TEST);
        registerReceiver(bcReceiver, filterZaBrotkast);
        super.onStart();
    }

    @Override
    public void onStop(){
        unregisterReceiver(bcReceiver);
        super.onStop();
    }

    @Override
    public void onDestroy(){
        if(!mServisAktivan) {
            Intent zaustaviServis = new Intent(MapsActivity.this, WorkerService.class);
            stopService(zaustaviServis);
        }else{
            Intent obavestiServis = new Intent(MapsActivity.this, WorkerService.class);
            obavestiServis.setAction(MapsActivity.INTENT_AKTIVIRAJ_NOTIFIKACIJE);
            startService(obavestiServis);
        }
        super.onDestroy();
    }


    @Override
    public void onMapClick(LatLng latLng) {
        ObjectAnimator oa = ObjectAnimator.ofFloat(mSlidingLayout, "translationY", 260 * Resources.getSystem().getDisplayMetrics().density);
        oa.setDuration(200);
        oa.start();
    }

    @Override
    public void onInfoWindowClick(Marker marker) {
        Object o = marker.getTag();
        if(o instanceof Korisnik){
            Korisnik k = (Korisnik)o;
            ((ImageView) findViewById(R.id.image_profilna_sliding)).setImageBitmap(k.getProfileBitmap());
            ((TextView) findViewById(R.id.text_naslov)).setText(k.korime);
            ((TextView) findViewById(R.id.text_poeni)).setText("Poena: " + Integer.toString(k.poeni));
            ((TextView) findViewById(R.id.text_prvi_lab)).setText("Ime i prezime");
            ((TextView) findViewById(R.id.text_prvi)).setText(k.ime + " " + k.prezime);
            ((TextView) findViewById(R.id.text_drugi_lab)).setText("Poslednji put viđen");
            ((TextView) findViewById(R.id.text_drugi))
                    .setText(new SimpleDateFormat("dd/MM/yyyy hh:mm", Locale.US).format(new Date(k.lokvreme * 1000)));
            ((TextView) findViewById(R.id.text_opis)).setText("");
        }
        if(o instanceof Popust){
            Popust p = (Popust)o;
            ((ImageView) findViewById(R.id.image_profilna_sliding))
                    .setImageDrawable(getResources().getDrawable(android.R.drawable.ic_menu_mylocation));
            ((TextView) findViewById(R.id.text_naslov)).setText("Popust od " + p.velicinapopusta + "%");
            ((TextView) findViewById(R.id.text_poeni)).setText("");
            ((TextView) findViewById(R.id.text_prvi_lab)).setText("Popust dodat");
            ((TextView) findViewById(R.id.text_prvi))
                    .setText(new SimpleDateFormat("dd/MM/yyyy", Locale.US).format(new Date(p.vremedodavanja* 1000)));
            ((TextView) findViewById(R.id.text_drugi_lab)).setText("Traje do");
            ((TextView) findViewById(R.id.text_drugi))
                    .setText(new SimpleDateFormat("dd/MM/yyyy", Locale.US).format(new Date(p.trajedo * 1000)));
            ((TextView) findViewById(R.id.text_opis)).setText(p.opispopusta);
        }

        ObjectAnimator oa = ObjectAnimator.ofFloat(mSlidingLayout, "translationY", 0);
        oa.setDuration(200);
        oa.start();
    }
}


/*


Marker tempMarker = mMap.addMarker(new MarkerOptions()
                                        .position( new LatLng(privremeni.getDouble("loklat"), privremeni.getDouble("loklng")))
                                        .title(privremeni.getString("opispopusta"))
                                        .snippet("(dodirni za još informacija)")
                                );
                                tempMarker.setTag(tempP);


String receivedBase64 = privremeni.getString("slika");
                                receivedBase64 = receivedBase64.replaceAll("\\s", "+");
                                byte[] imageBytes = Base64.decode(receivedBase64, Base64.DEFAULT);
                                Bitmap thumb = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
                                Marker tempMarker = mMap.addMarker(new MarkerOptions()
                                        .position( new LatLng(privremeni.getDouble("loklat"), privremeni.getDouble("loklng")))
                                        .title(privremeni.getString("korime"))
                                        .icon(BitmapDescriptorFactory.fromBitmap(thumb))
                                        .anchor(0.5f, 0.5f)
                                        .snippet("(dodirni za još informacija)")
                                );
                                tempMarker.setTag(tempK);
 */