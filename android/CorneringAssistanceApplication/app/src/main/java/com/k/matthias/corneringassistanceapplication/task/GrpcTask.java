package com.k.matthias.corneringassistanceapplication.task;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;

import com.k.matthias.corneringassistanceapplication.SettingsActivity;
import com.k.matthias.corneringassistanceapplication.db.LocalCurveCache;
import com.k.matthias.corneringassistanceapplication.grpc.RecommendationGrpc;
import com.k.matthias.corneringassistanceapplication.grpc.RecommendationListener;
import com.k.matthias.corneringassistanceapplication.grpc.RecommendationResponseCode;
import com.k.matthias.corneringassistanceapplication.grpc.RequestDTO;
import com.k.matthias.corneringassistanceapplication.grpc.ResponseDTO;

import java.util.concurrent.TimeUnit;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

/**
 * Background task to receive recommendation results
 */

public class GrpcTask extends AsyncTask<Void, Void, Boolean> {

    private static final String TAG = GrpcTask.class.getName();
    private static final String STATIC_REC_SERVER_ADDRESS = "104.155.88.1"; // TODO: consider using a service registry instead
    private static final long POLL_DELAY = 3000;
    private final boolean pollServer;
    private final Context context;
    private final RecommendationListener listener;
    private final SharedPreferences prefs;
    private ManagedChannel mChannel;
    private RequestDTO requestDTO;
    private static final int MAX_POLLS = 3;
    private static int POLLS = 0;
    private ResponseDTO cachedResult = null;

    public GrpcTask(RecommendationListener listener, RequestDTO requestDTO, boolean pollServer, Context context) {
        this.prefs = PreferenceManager.getDefaultSharedPreferences(context);
        this.requestDTO = requestDTO;
        this.pollServer = pollServer;
        this.context = context;
        this.listener = listener;
    }

    @Override
    protected Boolean doInBackground(Void... nothing) {
        try {
            mChannel = ManagedChannelBuilder.forAddress(STATIC_REC_SERVER_ADDRESS, 50051)
                    .usePlaintext(true)
                    .build();
            RecommendationGrpc.RecommendationBlockingStub stub = RecommendationGrpc.newBlockingStub(mChannel);
            ResponseDTO response;
            if (pollServer) {
                response = stub.pollDatabase(requestDTO);
            } else {
                POLLS = 0;
                response = stub.requestRecommendation(requestDTO);
            }

            Log.d(TAG, "Received recommendation response code: " + response.getResponseCode());

            if (response.getResponseCode() == RecommendationResponseCode.CURVES_DETECTED) {
                cachedResult = response;
                boolean clearCache = prefs.getBoolean(SettingsActivity.KEY_PREF_CLEAR_CACHE, false);
                if (clearCache) {
                    LocalCurveCache.getInstance(context).removeAllCurves();
                }
                LocalCurveCache.getInstance(context).insertCurves(response.getCurveList());
                Log.d(TAG, "Inserted " + response.getCurveList().getCurveRecommondationsList().size() + " curves to cache");
                return false;
            } else if (response.getResponseCode() == RecommendationResponseCode.DETECTION_SERVICE_BUSY) {
                if (POLLS < MAX_POLLS) {
                    return true;
                }
            }
        } catch (Exception e) {
            Log.d(TAG, e.getMessage());
        }
        return false;
    }

    @Override
    protected void onPostExecute(Boolean poll) {
        try {
            mChannel.shutdown().awaitTermination(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        if (poll) {
            poll();
        } else {
            if (listener != null) {
                listener.onRecommendationResult();
            }
        }

    }

    private void poll() {
        Log.d(TAG, "Sending " + POLLS + ". poll request");
        POLLS++;
        final GrpcTask task = new GrpcTask(listener, requestDTO, true, context);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                task.execute();
            }
        }, POLL_DELAY);
    }
}