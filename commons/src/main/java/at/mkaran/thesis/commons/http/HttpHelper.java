package at.mkaran.thesis.commons.http;

import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.IOException;

/**
 * Created by matthias on 13.11.17.
 */
public class HttpHelper {

    public static String okHttp(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .build();

        Response response = OkHttpSingleton.getInstance().newCall(request).execute();
        return response.body().string();
    }
}
