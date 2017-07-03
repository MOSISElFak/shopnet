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
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Array;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MapsActivity extends AppCompatActivity
        implements OnMapReadyCallback, GoogleMap.OnInfoWindowClickListener, GoogleMap.OnMapClickListener {

    public static final int INTENT_DODAJ_POPUST = 1;
    public static final String INTENT_OSVEZI_POPUSTE = "REFRESH_POPUSTI";
    public static final String INTENT_OSVEZI_PRIJATELJE = "REFRESH_PRIJATELJI";
    public static final String INTENT_AKTIVIRAJ_NOTIFIKACIJE = "AKTIVIRAJ_NOTIFIKACIJE";
    public static final String INTENT_PRETRAZI = "PRETRAZI";
    public static final String BROADCAST_TEST = "com.elfak.twoangrymen.shopnet.BROADCAST_TEST";


    private GoogleMap mMap;
    private Location mLastLocation;
    private ArrayList<Marker> mMarkersPopusti = new ArrayList<>();
    private ArrayList<Popust> mArrayPopusti = new ArrayList<>();
    private ArrayList<Marker> mMarkersPrijatelji = new ArrayList<>();
    private ArrayList<Korisnik> mArrayKorisnici = new ArrayList<>();
    private ArrayList<Marker> mMarkersPretraga = new ArrayList<>();
    private ArrayList<Popust> mArrayPretraga = new ArrayList<>();
    private ListView listaView;
    private List<String> stavkeListe;
    private ArrayAdapter<String> adapterListe;
    private Button btnDodajPopust;
    private boolean mServisAktivan = true;
    private ConstraintLayout mSlidingLayout;

    private boolean mModPretrage = false;
    public static int mFilterTipa = 0;
    private int mRadius = 1000;


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
                if(mModPretrage) return;
                //prekini ako je aktivan mod pretrage
                ArrayList<Popust> p = mArrayPopusti = (ArrayList<Popust>) intent.getSerializableExtra("NEWPOPUSTI");
                osveziMarkerePopusta(p);
            }
            if(intent.hasExtra("PRETRAGAPOPUSTI")){
                ArrayList<Popust> p = mArrayPretraga = (ArrayList<Popust>) intent.getSerializableExtra("PRETRAGAPOPUSTI");
                if(p.size() > 0) {
                    aktivirajPretragu();
                    osveziMarkerePretrage(p);
                }
                else{
                    Toast.makeText(MapsActivity.this, "Nema rezultata!", Toast.LENGTH_LONG).show();
                }
            }
            if(intent.hasExtra("NEWKORISNICI")){
                ArrayList<Korisnik> k = mArrayKorisnici = (ArrayList<Korisnik>) intent.getSerializableExtra("NEWKORISNICI");
                osveziMarkereKorisnika(k);
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

        final ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, TipoviPopusta.tipoviPopusta);
        ((Spinner) findViewById(R.id.spinner_tipovi_filter)).setAdapter(spinnerAdapter);

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
        //pretraga
        Button btnTrazi = (Button) findViewById(R.id.btn_trazi);
        btnTrazi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mModPretrage)
                    deaktivirajPretragu();
                else{
                    EditText poljePretrage = (EditText) findViewById(R.id.edit_pretraga);
                    String upit = poljePretrage.getText().toString().trim();
                    if(upit.length() > 2){
                        Intent pokreniPretragu = new Intent(MapsActivity.this, WorkerService.class);
                        pokreniPretragu.setAction(MapsActivity.INTENT_PRETRAZI);
                        pokreniPretragu.putExtra("upit", upit);
                        pokreniPretragu.putExtra("radius", mRadius);
                        startService(pokreniPretragu);
                    }
                    else{
                        Toast.makeText(MapsActivity.this, "Upit je prekratak!", Toast.LENGTH_LONG).show();
                    }
                }
            }
        });
        //radius
        EditText txtRadius = (EditText) findViewById(R.id.edit_radius);
        txtRadius.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(!hasFocus){
                    EditText txtRadius = (EditText) findViewById(R.id.edit_radius);
                    try {
                        String ulaz = txtRadius.getText().toString();
                        int newRadius = Integer.parseInt(ulaz);
                        mRadius = newRadius;
                    }
                    catch (NumberFormatException e){
                        Toast.makeText(MapsActivity.this, "Unesite pozitivan ceo broj!", Toast.LENGTH_SHORT).show();
                        txtRadius.setText("1000");
                        mRadius = 1000;
                    }
                }
            }
        });
        //tipovi popusta filter
        Spinner filterSpinner = (Spinner) findViewById(R.id.spinner_tipovi_filter);
        filterSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mFilterTipa = position;
                osveziMarkerePopusta(mArrayPopusti);
                osveziMarkerePretrage(mArrayPretraga);
                //Toast.makeText(MapsActivity.this, "Podesavam ID filtera na " + position, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                mFilterTipa = 0;
                osveziMarkerePopusta(mArrayPopusti);
                osveziMarkerePretrage(mArrayPretraga);
                //Toast.makeText(MapsActivity.this, "Podesavam ID filtera na -1", Toast.LENGTH_SHORT).show();
            }
        });
        //liste
        listaView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //aktiviranje/deaktiviranje servisa
                if(position == 0){
                    Spinner tempSpinner = new Spinner(MapsActivity.this, null, android.R.style.Widget_Spinner, Spinner.MODE_DIALOG);
                    List<String> listaPrij = new ArrayList<String>();
                    Collections.sort(mArrayKorisnici, new Comparator<Korisnik>(){
                        public int compare(Korisnik k1, Korisnik k2){
                            if(k1.poeni == k2.poeni)
                                return 0;
                            return k1.poeni > k2.poeni ? -1 : 1;
                        }
                    });
                    for(Korisnik k : mArrayKorisnici){
                        listaPrij.add(k.ime + " " + k.prezime + ", " + k.poeni + " bod(ova)");
                    }
                    ArrayAdapter<String> aad = new ArrayAdapter<String>(MapsActivity.this, android.R.layout.simple_spinner_dropdown_item, listaPrij);
                    tempSpinner.setAdapter(aad);
                    tempSpinner.performClick();
                }
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
                                Intent zaustaviServis = new Intent(MapsActivity.this, WorkerService.class);
                                stopService(zaustaviServis);
                                Intent vratiSeNaLogin = new Intent(MapsActivity.this, MainActivity.class);
                                startActivity(vratiSeNaLogin);
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
            ((Button) findViewById(R.id.button_like)).setVisibility(View.GONE);
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
            final Popust p = (Popust)o;
            Button lajkDugme = (Button) findViewById(R.id.button_like);
            lajkDugme.setVisibility(View.VISIBLE);
            lajkDugme.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //ucitaj informacije o korisniku
                    JSONObject tokenJSON = new JSONObject();
                    try {
                        tokenJSON.put("token", SharedPreferencesManager.getPreferenceString(SharedPreferencesManager.STR_CURRENT_TOKEN));
                        tokenJSON.put("id", p.id);
                        new WebService(){
                            protected void onPostExecute(ServerResponse r){
                                super.onPostExecute(r);
                                if(r != null) {
                                    if(r.getResponseCode() == 200) {
                                        Toast.makeText(MapsActivity.this, "Lajkovali ste ovaj popust!", Toast.LENGTH_SHORT).show();
                                    }
                                    else if(r.getResponseCode() == 400){
                                        Toast.makeText(MapsActivity.this, "Greska, pokusajte ponovo.", Toast.LENGTH_LONG).show();
                                    }
                                }else
                                    Log.e("GRESKA", "Server nije odgovorio.");
                            }
                        }.execute("/lajk", "POST", tokenJSON.toString());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });

            ((ImageView) findViewById(R.id.image_profilna_sliding)).setImageDrawable(getResources().getDrawable(android.R.drawable.ic_menu_mylocation));
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

    private void aktivirajPretragu(){
        mModPretrage = true;
        Button btnTrazi = (Button) findViewById(R.id.btn_trazi);
        btnTrazi.setText("Nazad");
        EditText txtPretraga = (EditText) findViewById(R.id.edit_pretraga);
        txtPretraga.setEnabled(false);
    }
    private void deaktivirajPretragu(){
        mModPretrage = false;
        Button btnTrazi = (Button) findViewById(R.id.btn_trazi);
        btnTrazi.setText("Traži popuste");
        EditText txtPretraga = (EditText) findViewById(R.id.edit_pretraga);
        txtPretraga.setEnabled(true);
        osveziMarkerePopusta(mArrayPopusti);
    }

    private void osveziMarkerePopusta(ArrayList<Popust> p){
        if(p.size() > 0){
            for(Marker m : mMarkersPopusti){
                m.remove();
            }
            for(Marker m : mMarkersPretraga){
                m.remove();
            }
            for(int i=0; i < p.size(); i++){
                Popust pop = p.get(i);
                //proveri radius
                if(pop != null && mLastLocation != null){
                    float[] distanca = new float[1];
                    Location.distanceBetween(pop.loklat, pop.loklng, mLastLocation.getLatitude(), mLastLocation.getLongitude(), distanca);
                    if(distanca[0] > mRadius) continue;
                }
                //proveri tip
                if(mFilterTipa > 0 && (pop.tippopusta != mFilterTipa)) continue;
                //nastavi
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

    private void osveziMarkerePretrage(ArrayList<Popust> p){
        if(p.size() > 0){
            for(Marker m : mMarkersPretraga){
                m.remove();
            }
            for(Marker m : mMarkersPopusti){
                m.remove();
            }
            LatLngBounds.Builder llBuilder = new LatLngBounds.Builder();
            for(int i=0; i < p.size(); i++){
                Popust pop = p.get(i);
                //proveri tip
                if(mFilterTipa > 0 && (pop.tippopusta != mFilterTipa)) continue;
                Marker tempMarker = mMap.addMarker(new MarkerOptions()
                        .position( new LatLng(pop.loklat, pop.loklng))
                        .title("Popust: " + pop.velicinapopusta + "%; " + TipoviPopusta.tipoviPopusta.get(pop.tippopusta))
                        .snippet("(dodirni za još informacija)")
                );
                llBuilder.include(tempMarker.getPosition());
                tempMarker.setTag(pop);
                mMarkersPretraga.add(tempMarker);
            }
            LatLngBounds bounds = llBuilder.build();
            int padding = 10;
            CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, padding);
            mMap.animateCamera(cu);
        }
    }
    private void osveziMarkereKorisnika(ArrayList<Korisnik> k){
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
}