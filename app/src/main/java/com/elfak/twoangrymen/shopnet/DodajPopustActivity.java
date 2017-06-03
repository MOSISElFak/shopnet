package com.elfak.twoangrymen.shopnet;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Intent;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

public class DodajPopustActivity extends AppCompatActivity {
    private double mLat;
    private double mLng;
    private Button btnPosalji;
    private EditText editTrajanje;
    private Calendar izabraniDatum = Calendar.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dodaj_popust);

        //obradi intent
        Intent pristiglo = getIntent();
        if(pristiglo.hasExtra("LAT") && pristiglo.hasExtra("LNG")){
            mLat = pristiglo.getDoubleExtra("LAT", 0);
            mLng = pristiglo.getDoubleExtra("LNG", 0);
        }
        else{
            Intent vratiInformacije = new Intent();
            setResult(Activity.RESULT_CANCELED, vratiInformacije);
            finish();
        }

        //tipovi popusta
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, TipoviPopusta.tipoviPopusta);
        ((Spinner) findViewById(R.id.spinner_tipovi)).setAdapter(spinnerAdapter);

        //listener za DatePickerDialog nakon odabira datuma
        final DatePickerDialog.OnDateSetListener dateListener = new DatePickerDialog.OnDateSetListener(){

            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                izabraniDatum.set(Calendar.YEAR, year);
                izabraniDatum.set(Calendar.MONTH, month);
                izabraniDatum.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                //apdejt viewa
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.US);
                editTrajanje.setText(sdf.format(izabraniDatum.getTime()));
            }
        };

        //click handleri
        btnPosalji = (Button) findViewById(R.id.btn_posalji);
        btnPosalji.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //sakupi podatke
                int velicinaPopusta = 0;
                try{
                    velicinaPopusta = Integer.parseInt(((EditText) findViewById(R.id.edit_popust)).getText().toString());
                }
                catch (NumberFormatException e){}
                String tipPopusta = ((Spinner) findViewById(R.id.spinner_tipovi)).getSelectedItem().toString();
                long trajanjePopusta = izabraniDatum.getTimeInMillis() / 1000;
                String opisPopusta = ((EditText) findViewById(R.id.edit_opis)).getText().toString();

                String poruka = "";
                if(velicinaPopusta <= 0) poruka +=  "Unesite popust veci od nule!\n";
                if(tipPopusta.length() == 0) poruka +=  "Morate izabrati tip popusta!\n";
                if(trajanjePopusta == 0) poruka +=  "Morate izabrati trajanje popusta!\n";
                if(opisPopusta.length() < 10) poruka +=  "Morate uneti opis popusta!\n";

                if(poruka.length() > 0){
                    Toast.makeText(DodajPopustActivity.this, poruka, Toast.LENGTH_LONG).show();
                    return;
                }

                JSONObject podaci = new JSONObject();
                try {
                    podaci.put("lat", mLat);
                    podaci.put("lng", mLng);
                    podaci.put("trajedo", trajanjePopusta);
                    podaci.put("tippopusta", tipPopusta);
                    podaci.put("velicinapopusta", velicinaPopusta);
                    podaci.put("opispopusta", opisPopusta);
                    podaci.put("token", SharedPreferencesManager.getPreferenceString(SharedPreferencesManager.STR_CURRENT_TOKEN));
                    new WebService(){
                        @Override
                        protected void onPostExecute(ServerResponse r) {
                            super.onPostExecute(r);
                            if (r != null) {
                                Toast.makeText(DodajPopustActivity.this, "Popust je dodat!", Toast.LENGTH_LONG).show();
                                Intent vratiSeNaMapu = new Intent();
                                setResult(Activity.RESULT_OK, vratiSeNaMapu);
                                finish();
                            } else
                                Log.e("GRESKA", "Server nije odgovorio.");
                        }
                    }.execute("/dodajpopust", "POST", podaci.toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                    Toast.makeText(DodajPopustActivity.this, "Podaci nisu validni!", Toast.LENGTH_LONG).show();
                }
            }
        });
        editTrajanje = (EditText) findViewById(R.id.edit_trajanje);
        //apdejt viewa datuma
        SimpleDateFormat datFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.US);
        editTrajanje.setText(datFormat.format(izabraniDatum.getTime()));
        //click handler
        editTrajanje.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new DatePickerDialog(DodajPopustActivity.this, dateListener,
                        izabraniDatum.get(Calendar.YEAR),
                        izabraniDatum.get(Calendar.MONTH),
                        izabraniDatum.get(Calendar.DAY_OF_MONTH)).show();
            }
        });
    }
}
