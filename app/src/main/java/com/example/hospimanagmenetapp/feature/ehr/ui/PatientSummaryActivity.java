package com.example.hospimanagmenetapp.feature.ehr.ui;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.hospimanagmenetapp.R;
import com.example.hospimanagmenetapp.data.AppDatabase;
import com.example.hospimanagmenetapp.data.entities.ClinicalRecord;
import com.example.hospimanagmenetapp.util.ValidationUtils;

import java.util.concurrent.Executors;

/**
 * Displays the EHR summary for a single patient, identified by NHS number.
 *
 * Navigated to from:
 *   - BarcodeScannerActivity (scan wristband → NHS number → this screen)
 *   - PatientRegistrationActivity (view record button)
 *
 * SECURITY:
 * - NHS number is received via Intent extra, never from user text input
 * - Clinical data is only displayed — never logged
 * - RBAC check should be added here in Lab 4 to restrict to CLINICIAN/ADMIN
 *
 * ACCESSIBILITY:
 * - All TextViews have contentDescription set in the layout
 * - TalkBack will read clinical field labels and values correctly
 */
public class PatientSummaryActivity extends AppCompatActivity {

    private TextView tvHeader, tvProblems, tvAllergies, tvMedications;
    private String nhsNumber;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient_summary);

        tvHeader      = findViewById(R.id.tvPatientHeader);
        tvProblems    = findViewById(R.id.tvProblems);
        tvAllergies   = findViewById(R.id.tvAllergies);
        tvMedications = findViewById(R.id.tvMedications);
        Button btnVitals     = findViewById(R.id.btnRecordVitals);
        Button btnEditRecord = findViewById(R.id.btnEditRecord);

        // Receive NHS number from Intent — validate before any DB use
        nhsNumber = getIntent().getStringExtra("nhsNumber");

        if (nhsNumber == null || !ValidationUtils.validateNhsNumber(nhsNumber)) {
            // SECURITY: Don't echo the invalid value back — just reject it
            Toast.makeText(this, "Invalid patient identifier.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // SECURITY: Show only last 4 digits in the header — avoid full NHS in UI
        tvHeader.setText("Patient record — NHS ending "
                + nhsNumber.substring(nhsNumber.length() - 4));

        loadRecord();

        btnVitals.setOnClickListener(v -> {
            Intent i = new Intent(this, VitalsActivity.class);
            i.putExtra("nhsNumber", nhsNumber);
            startActivity(i);
        });

        btnEditRecord.setOnClickListener(v -> {
            Intent i = new Intent(this, EditClinicalRecordActivity.class);
            i.putExtra("nhsNumber", nhsNumber);
            startActivity(i);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload on return from edit screen so changes appear immediately
        loadRecord();
    }

    // Fetches the clinical record from Room on a background thread.
    // SECURITY: Results are displayed only — never logged.
    private void loadRecord() {
        Executors.newSingleThreadExecutor().execute(() -> {
            ClinicalRecord record = AppDatabase.getInstance(getApplicationContext())
                    .clinicalRecordDao().findByPatient(nhsNumber);

            runOnUiThread(() -> {
                if (record != null) {
                    tvProblems.setText("Problems: " + record.problems);
                    tvAllergies.setText("Allergies: " + record.allergies);
                    tvMedications.setText("Medications: " + record.medications);
                } else {
                    tvProblems.setText("No clinical record found.");
                    tvAllergies.setText("");
                    tvMedications.setText("");
                }
            });
        });
    }
}