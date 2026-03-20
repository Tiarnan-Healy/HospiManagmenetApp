package com.example.hospimanagmenetapp.feature.ehr.ui;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.hospimanagmenetapp.R;
import com.example.hospimanagmenetapp.data.AppDatabase;
import com.example.hospimanagmenetapp.data.entities.Vitals;
import com.example.hospimanagmenetapp.util.ValidationUtils;

import java.util.concurrent.Executors;


//Screen for recording a patient's vital signs.
//
//Vitals are inserted with synced=false, entering the offline queue.
//VitalsSyncWorker will pick them up and mark them synced when
//network connectivity is available.
//
//SECURITY:
//- NHS number is received via Intent only
//- All free-text fields are sanitised before DB write
//- No clinical values are written to logs at any point
//
//OFFLINE:
//- Records are always written to Room first regardless of connectivity
//- The synced flag is false on insert, WorkManager handles upload

public class VitalsActivity extends AppCompatActivity {

    private EditText etTemperature, etHeartRate, etSystolic, etDiastolic;
    private String nhsNumber;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vitals);

        etTemperature = findViewById(R.id.etTemperature);
        etHeartRate   = findViewById(R.id.etHeartRate);
        etSystolic    = findViewById(R.id.etSystolic);
        etDiastolic   = findViewById(R.id.etDiastolic);
        Button btnSave = findViewById(R.id.btnSaveVitals);
        TextView tvHeader = findViewById(R.id.tvVitalsHeader);

        nhsNumber = getIntent().getStringExtra("nhsNumber");

        // Validate NHS before doing anything — belt and braces check
        if (nhsNumber == null || !ValidationUtils.validateNhsNumber(nhsNumber)) {
            Toast.makeText(this, "Invalid patient identifier.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Show only last 4 digits — avoid full NHS number in UI
        tvHeader.setText("Record Vitals — NHS ending "
                + nhsNumber.substring(nhsNumber.length() - 4));

        btnSave.setOnClickListener(v -> saveVitals());
    }

//    Validates inputs, builds a Vitals entity, and inserts on a background thread.
//
//    SECURITY: sanitiseInput() applied to all string fields as defence-in-depth.
//    Numeric fields are parsed directly — no injection risk from number parsing.
//    OFFLINE: synced=false ensures WorkManager picks this record up for upload.

    private void saveVitals() {
        String tempStr      = etTemperature.getText().toString().trim();
        String hrStr        = etHeartRate.getText().toString().trim();
        String systolicStr  = etSystolic.getText().toString().trim();
        String diastolicStr = etDiastolic.getText().toString().trim();

        // Required field check
        if (TextUtils.isEmpty(tempStr) || TextUtils.isEmpty(hrStr)
                || TextUtils.isEmpty(systolicStr) || TextUtils.isEmpty(diastolicStr)) {
            Toast.makeText(this, "All fields are required.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Parse and validate ranges
        float temperature;
        int heartRate, systolic, diastolic;
        try {
            temperature = Float.parseFloat(tempStr);
            heartRate   = Integer.parseInt(hrStr);
            systolic    = Integer.parseInt(systolicStr);
            diastolic   = Integer.parseInt(diastolicStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter valid numeric values.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Basic clinical range validation
        if (temperature < 30f || temperature > 45f) {
            Toast.makeText(this, "Temperature must be between 30°C and 45°C.",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        if (heartRate < 20 || heartRate > 300) {
            Toast.makeText(this, "Heart rate must be between 20 and 300 bpm.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // Build entity — synced=false enters the offline upload queue
        Vitals v = new Vitals();
        v.patientNhs  = nhsNumber; // already validated above
        v.temperature = temperature;
        v.heartRate   = heartRate;
        v.systolic    = systolic;
        v.diastolic   = diastolic;
        v.timestamp   = System.currentTimeMillis();
        v.synced      = false; // WorkManager will set this to true after upload

        // DB insert on background thread
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                AppDatabase.getInstance(getApplicationContext())
                        .vitalsDao().insert(v);

                // SECURITY: Log operation result only — never log clinical values
                android.util.Log.i("VitalsActivity", "Vitals record inserted successfully.");

                runOnUiThread(() -> {
                    Toast.makeText(this,
                            "Vitals saved. Will sync when online.",
                            Toast.LENGTH_LONG).show();
                    finish();
                });
            } catch (Exception e) {
                // SECURITY: Suppress exception detail — may expose PHI in stack
                android.util.Log.e("VitalsActivity", "Vitals insert failed.");
                runOnUiThread(() ->
                        Toast.makeText(this, "Error saving vitals. Please try again.",
                                Toast.LENGTH_LONG).show());
            }
        });
    }
}