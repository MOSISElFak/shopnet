package com.elfak.twoangrymen.shopnet;

/**
 * Created by Bencun on 5/31/2017.
 */

public class ServerResponse {
    private int responseCode;
    private String response;
    public ServerResponse(int responseCode, String response){
        this.responseCode = responseCode;
        this.response = response;
    }

    public int getResponseCode(){
        return responseCode;
    }

    public String getResponse(){
        return response;
    }
}
