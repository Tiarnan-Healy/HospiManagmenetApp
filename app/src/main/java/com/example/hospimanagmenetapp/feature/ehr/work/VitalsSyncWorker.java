package com.example.hospimanagmenetapp.feature.ehr.work;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.hospimanagmenetapp.data.AppDatabase;
import com.example.hospimanagmenetapp.data.entities.Vitals;

import java.util.List;


// WorkManager background worker responsible for syncing unsynced vitals
// records to the server once network connectivity is available.
//
// This worker is scheduled in MainActivity with a CONNECTED network
// constraint — it will only run when the device is online.
//
// OFFLINE DESIGN:
// VitalsActivity inserts records with synced=false.
// This worker picks up all synced=false records, simulates upload,
// then marks them synced=true. In a production system the TODO block
// would make a Retrofit call to the server API.
//
// SECURITY: Only record IDs and counts are logged — never NHS numbers
// or clinical values. PHI must never appear in system logs.
//
public class VitalsSyncWorker extends Worker {

    private static final String TAG = "VitalsSyncWorker";

    public VitalsSyncWorker(@NonNull Context ctx, @NonNull WorkerParameters params) {
        super(ctx, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            AppDatabase db = AppDatabase.getInstance(getApplicationContext());

            // Fetch all records pending upload
            List<Vitals> pending = db.vitalsDao().getPending();

            // SECURITY: Log count only — never log NHS or clinical values
            Log.i(TAG, "Vitals sync started. Pending records: " + pending.size());

            for (Vitals v : pending) {
                // TODO: Replace with real Retrofit API call in production
                // RetrofitClient.getInstance().vitalsApi().upload(v).execute();

                // Simulate successful upload — mark as synced
                db.vitalsDao().markSynced(v.id);

                // Log ID only — not the clinical content
                Log.d(TAG, "Vitals record synced. ID: " + v.id);
            }

            Log.i(TAG, "Vitals sync complete.");
            return Result.success();

        } catch (Exception e) {
            // SECURITY: Do not log exception detail — may contain PHI in stack trace
            Log.e(TAG, "Vitals sync failed — will retry.");
            return Result.retry();
        }
    }
}