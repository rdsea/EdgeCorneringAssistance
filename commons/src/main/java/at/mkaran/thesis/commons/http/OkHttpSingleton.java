package at.mkaran.thesis.commons.http;

import com.squareup.okhttp.OkHttpClient;

/**
 * Created by matthias on 13.11.17.
 */
public class OkHttpSingleton {

    private static OkHttpClient instance;

    private OkHttpSingleton() {}

    public static OkHttpClient getInstance () {
        if (OkHttpSingleton.instance == null) {
            OkHttpSingleton.instance = new OkHttpClient();
        }
        return OkHttpSingleton.instance;
    }
}