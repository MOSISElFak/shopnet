package com.elfak.twoangrymen.shopnet;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

/**
 * Created by Bencun on 5/30/2017.
 */

public class Korisnik implements Serializable{
    public int id;
    public String korime;
    public String ime;
    public String prezime;
    public String slika;
    public int poeni;
    public double loklat;
    public double loklng;
    public long lokvreme;

    public Korisnik(JSONObject temp){
        try {
            id = temp.getInt("id");
            korime = temp.getString("korime");
            ime = temp.getString("ime");
            prezime = temp.getString("prezime");
            slika = temp.getString("slika");
            poeni = temp.getInt("poeni");
            loklat = temp.getDouble("loklat");
            loklng = temp.getDouble("loklng");
            lokvreme = temp.getLong("lokvreme");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public JSONObject getJSON(){
        JSONObject temp = new JSONObject();
        try {
            temp.put("id", this.id);
            temp.put("korime", this.korime);
            temp.put("ime", this.ime);
            temp.put("prezime", this.prezime);
            temp.put("slika", this.slika);
            temp.put("poeni", this.poeni);
            temp.put("loklat", this.loklat);
            temp.put("loklng", this.loklng);
            temp.put("lokvreme", this.lokvreme);

        } catch (JSONException e) {
            temp = null;
            e.printStackTrace();
        }
        return temp;
    }

    public Bitmap getProfileBitmap(){
        String receivedBase64 = slika.replaceAll("\\s", "+");
        byte[] imageBytes = Base64.decode(receivedBase64, Base64.DEFAULT);
        Bitmap thumb = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
        return thumb;
    }
}
