package com.elfak.twoangrymen.shopnet;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity{

    private String teststring = "nista";
    Button btnRegistracija;
    Button btnLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //iniciraj SPManager
        SharedPreferencesManager.init(MainActivity.this);
        Intent privIntent = getIntent();
        //zatrazi location
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 123);

        //ako je intent sa RegistracijaActivity popuni tekstualno polje
        String korime = privIntent.getStringExtra("KORIME");
        if(korime != null){
            EditText edit_korime = (EditText)findViewById(R.id.edit_korime_login);
            edit_korime.setText(korime);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
            //proveriToken();

        //listeneri za dugmad
        btnRegistracija = (Button)findViewById(R.id.btn_registracija);
        btnRegistracija.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent prelazakNaReg = new Intent(MainActivity.this, RegistracijaActivity.class);
                startActivity(prelazakNaReg);
            }
        });
        btnLogin = (Button) findViewById(R.id.btn_ulogujse);
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //sakupi podatke sa aktivitija
                String korime = ((EditText) findViewById(R.id.edit_korime_login)).getText().toString();
                String sifra = ((EditText) findViewById(R.id.edit_sifra_login)).getText().toString();
                if(korime.length() < 5) {
                    Toast.makeText(MainActivity.this, "Korisnicko ime mora imati barem 5 karaktera!", Toast.LENGTH_SHORT).show();
                    return;
                }
                if(sifra.length() < 6) {
                    Toast.makeText(MainActivity.this, "Sifra mora imati barem 6 karaktera!", Toast.LENGTH_SHORT).show();
                    return;
                }
                JSONObject zahtevJSON = new JSONObject();
                //salji zahtev
                try {
                    zahtevJSON.put("korime", korime);
                    zahtevJSON.put("sifra", sifra);
                    new WebService(){
                        protected void onPostExecute(ServerResponse r){
                            super.onPostExecute(r);
                            if(r != null) {
                                if(r.getResponseCode() == 200) {
                                    String primljeniToken = null;
                                    try {
                                        primljeniToken = new JSONObject(r.getResponse()).getString("token");
                                        Log.e("TOKEN", primljeniToken);
                                        //upisi token
                                        SharedPreferencesManager.setPreferenceString(SharedPreferencesManager.STR_CURRENT_TOKEN, primljeniToken);
                                        //predji na MapaActivity
                                        Intent prelazakNaMapu = new Intent(MainActivity.this, MapsActivity.class);
                                        startActivity(prelazakNaMapu);
                                        finish();
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                        Log.e("GRESKA", "Los JSON odgovor.");
                                    }
                                }
                                else if(r.getResponseCode() == 400){
                                    Toast.makeText(MainActivity.this, "Login podaci nisu ispravni!", Toast.LENGTH_LONG).show();
                                }
                            }else
                                Toast.makeText(MainActivity.this, "Server nije dostupan!", Toast.LENGTH_LONG).show();
                        }
                    }.execute("/login", "POST", zahtevJSON.toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                    Log.e("GRESKA", "Korisnicko ime ili sifra nisu u redu.");
                    return;
                }
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == 123) {
            if (permissions.length == 1 && permissions[0].compareTo(Manifest.permission.ACCESS_FINE_LOCATION) == 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    proveriToken();
                    Log.v("PERMLOC", "Dozvola okej.");
                }
            } else {
                Toast.makeText(MainActivity.this, "Morate dozvoliti pristup lokaciji.", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    private void proveriToken(){
        //proveri token
        String tokenIzSP = SharedPreferencesManager.getPreferenceString(SharedPreferencesManager.STR_CURRENT_TOKEN);
        if(tokenIzSP != null) {
            JSONObject tokenObjekat = new JSONObject();
            try {
                tokenObjekat.put("token", tokenIzSP);
                new WebService() {
                    protected void onPostExecute(ServerResponse r) {
                        super.onPostExecute(r);
                        if (r != null) {
                            if(r.getResponseCode() == 200){
                                Log.v("TOKEN", "Token je validan.");
                                Intent prelazakNaMapu = new Intent(MainActivity.this, MapsActivity.class);
                                startActivity(prelazakNaMapu);
                                finish();
                            }
                            else{
                                Log.e("TOKEN", "Token nije validan!");
                            }
                        } else
                            Toast.makeText(MainActivity.this, "Server nije dostupan!", Toast.LENGTH_LONG).show();
                    }
                }.execute("/checktoken", "POST", tokenObjekat.toString());
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
}

        /*new WebService(){
            protected void onPostExecute(String s){
                super.onPostExecute(s);
                Log.e("Odgovor MainActivity: ", "" + s);
                teststring = s;
            }
        }.execute("/test", "POST", "{\"test\":\"ABCČĆЋЖ\"}");*/