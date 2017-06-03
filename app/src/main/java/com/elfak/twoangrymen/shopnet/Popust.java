package com.elfak.twoangrymen.shopnet;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

/**
 * Created by Bencun on 6/2/2017.
 */

public class Popust implements Serializable{
    public int id;
    public int profil;
    public double loklat;
    public double loklng;
    public long vremedodavanja;
    public long trajedo;
    public int velicinapopusta;
    public int tippopusta;
    public String opispopusta;

    public Popust(JSONObject temp){
        try {
            id = temp.getInt("id");
            profil = temp.getInt("profil");
            loklat = temp.getDouble("loklat");
            loklng = temp.getDouble("loklng");
            vremedodavanja = temp.getLong("vremedodavanja");
            trajedo = temp.getLong("trajedo");
            velicinapopusta = temp.getInt("velicinapopusta");
            tippopusta = temp.getInt("tippopusta");
            opispopusta = temp.getString("opispopusta");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public JSONObject getJSON(){
        JSONObject temp = new JSONObject();
        try {
            temp.put("id", this.id);
            temp.put("profil", this.profil);
            temp.put("loklat", this.loklat);
            temp.put("loklng", this.loklng);
            temp.put("vremedodavanja", this.vremedodavanja);
            temp.put("trajedo", this.trajedo);
            temp.put("velicinapopusta", this.velicinapopusta);
            temp.put("tippopusta", this.tippopusta);
            temp.put("opispopusta", this.opispopusta);

        } catch (JSONException e) {
            temp = null;
            e.printStackTrace();
        }
        return temp;
    }
}
