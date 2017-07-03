package com.elfak.twoangrymen.shopnet;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class TipoviPopusta {
    public static ArrayList<String> tipoviPopusta = null;
    public static void ucitajTipove(){
        //preuzmi tipove popusta
        new WebService() {
            protected void onPostExecute(ServerResponse r) {
                super.onPostExecute(r);
                if (r != null) {
                    Log.e("TipoviPopusta: ", "" + r.getResponse());
                    try {
                        //parsuj niz u objekat
                        TipoviPopusta.tipoviPopusta = new ArrayList<>();    //ovde kreiramo objekat u suprotnom je null
                        JSONArray niz = new JSONArray(r.getResponse());
                        TipoviPopusta.tipoviPopusta.add("Svi popusti");
                        for (int i = 0; i < niz.length(); i++) {
                            //parsuj svaki objekat iz niza u JSONObject
                            JSONObject privremeni = niz.getJSONObject(i);
                            TipoviPopusta.tipoviPopusta.add(privremeni.getString("naziv"));
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    for (int i = 0; i < TipoviPopusta.tipoviPopusta.size(); i++) {
                        Log.e("Stampam tip:", TipoviPopusta.tipoviPopusta.get(i));
                    }
                } else
                    Log.e("GRESKA", "Server nije odgovorio.");
            }
        }.execute("/tipovi", "GET");
    }
}
