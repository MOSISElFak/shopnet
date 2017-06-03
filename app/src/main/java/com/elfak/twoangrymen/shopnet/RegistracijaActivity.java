package com.elfak.twoangrymen.shopnet;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class RegistracijaActivity extends AppCompatActivity {
    private final int PICK_IMAGE_REQUEST = 1;
    private final int TAKE_IMAGE_REQUEST = 2;
    private String mCurrentPhotoPath = null;
    Button btnIzaberiSliku;
    Button btnKamera;
    Button btnRegistracija;
    Bitmap fotografija = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registracija);

        //event listeneri
        btnIzaberiSliku = (Button)findViewById(R.id.btn_galerija);
        btnIzaberiSliku.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent biranjeSlike = new Intent();
                biranjeSlike.setType("image/jpeg");
                biranjeSlike.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(biranjeSlike, "Odaberi sliku"), PICK_IMAGE_REQUEST);
            }
        });
        btnKamera = (Button)findViewById(R.id.btn_dodaj_sliku);
        btnKamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent slikanje = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if(slikanje.resolveActivity(getPackageManager()) != null ){
                    File photo = null;
                    try {
                        photo = createImageFile();
                        Log.e("PATH:", photo.getAbsolutePath());
                    } catch (IOException e) {
                        e.printStackTrace();
                        Toast.makeText(RegistracijaActivity.this, "Nije moguce kreirati privremeni fajl.", Toast.LENGTH_SHORT).show();
                    }
                    if(photo != null) {
                        Uri photoUri = FileProvider.getUriForFile(RegistracijaActivity.this, "com.elfak.twoangrymen.shopnet.fileprovider", photo);
                        slikanje.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                        startActivityForResult(slikanje, TAKE_IMAGE_REQUEST);
                    }
                }
            }
        });
        btnRegistracija = (Button) findViewById(R.id.btn_registracija);
        btnRegistracija.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String korime = ((EditText) findViewById(R.id.edit_korime_login)).getText().toString();
                String sifra = ((EditText) findViewById(R.id.edit_sifra_login)).getText().toString();
                String ime = ((EditText) findViewById(R.id.edit_ime)).getText().toString();
                String prezime = ((EditText) findViewById(R.id.edit_prezime)).getText().toString();
                String brtel = ((EditText) findViewById(R.id.edit_brtel)).getText().toString();

                String poruka = "";
                if(korime == null || korime.length() < 5) poruka +=  "Korisnicko ime mora biti duze od 4 karaktera!\n";
                if(sifra  == null || sifra.length() < 6) poruka +=  "Sifra mora biti duza od 5 karaktera!\n";
                if(ime == null || ime.length() < 3) poruka +=  "Ime mora biti duze od 2 karaktera!\n";
                if(prezime == null || prezime.length() < 3) poruka +=  "Prezime mora biti duze od 2 karaktera!\n";
                if(brtel == null || brtel.length() < 7) poruka +=  "Broj telefona nije validan!\n";
                if(fotografija == null) poruka +=  "Morate izabrati fotografiju!\n";

                if(poruka.length() > 0){
                    Toast.makeText(RegistracijaActivity.this, poruka, Toast.LENGTH_LONG).show();
                    return;
                }

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                fotografija.compress(Bitmap.CompressFormat.JPEG, 80, baos);
                byte[] nizBajtova = baos.toByteArray();
                String slika = Base64.encodeToString(nizBajtova, Base64.NO_WRAP);
                Log.e("SLIKA:", slika);
                JSONObject podaci = new JSONObject();
                try {
                    podaci.put("korime", korime);
                    podaci.put("sifra", sifra);
                    podaci.put("ime", ime);
                    podaci.put("prezime", prezime);
                    podaci.put("brtel", brtel);
                    podaci.put("slika", slika);
                    String JSONPodaci = podaci.toString();
                    new WebService(){
                        protected void onPostExecute(ServerResponse r){
                            super.onPostExecute(r);
                            if(r != null) {
                                Log.e("SERVER:", r.getResponse() + " code " + r.getResponseCode());
                                if(r.getResponseCode() == 400)
                                    Toast.makeText(RegistracijaActivity.this, "Korisnicko ime je vec zauzeto!", Toast.LENGTH_SHORT).show();
                                if(r.getResponseCode() == 200) {
                                    Toast.makeText(RegistracijaActivity.this, "Registracija uspesna!", Toast.LENGTH_SHORT).show();
                                    Intent nazadNaLogin = new Intent(RegistracijaActivity.this, MainActivity.class);
                                    nazadNaLogin.putExtra("KORIME", korime);
                                    startActivity(nazadNaLogin);
                                    finish();
                                }
                            }
                            else{
                                Toast.makeText(RegistracijaActivity.this, "Server nije dostupan.", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }.execute("/novikorisnik", "POST", JSONPodaci);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null ){
            Uri imgUri = data.getData();
            try{
                fotografija = android.provider.MediaStore.Images.Media.getBitmap(getContentResolver(), imgUri);
                Log.d("BITMAP:", String.valueOf(fotografija));
                ImageView iv = (ImageView) findViewById(R.id.imageView);
                iv.setImageBitmap(fotografija);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if(requestCode == TAKE_IMAGE_REQUEST && resultCode == RESULT_OK){
                fotografija = BitmapFactory.decodeFile(mCurrentPhotoPath);
                Log.d("BITMAP:", String.valueOf(fotografija));
                ImageView iv = (ImageView) findViewById(R.id.imageView);
                iv.setImageBitmap(fotografija);
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }
}
