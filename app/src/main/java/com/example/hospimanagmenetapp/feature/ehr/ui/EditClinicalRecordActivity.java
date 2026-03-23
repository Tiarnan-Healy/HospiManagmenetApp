package com.example.hospimanagmenetapp.feature.ehr.ui;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.hospimanagmenetapp.R;
import com.example.hospimanagmenetapp.data.AppDatabase;
import com.example.hospimanagmenetapp.data.entities.ClinicalRecord;
import com.example.hospimanagmenetapp.util.ValidationUtils;
import com.example.hospimanagmenetapp.security.SecurityAgent;

import java.util.concurrent.Executors;


// Allows authorised staff to create or update a patient's clinical record.

public class EditClinicalRecordActivity extends AppCompatActivity {

    private EditText etProblems, etAllergies, etMedications;
    private String nhsNumber;
    private SecurityAgent securityAgent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_clinical_record);

        try {
            securityAgent = new SecurityAgent();
        } catch (Exception e) {
            android.util.Log.e("EditClinicalRecord", "SecurityAgent init failed.");
            Toast.makeText(this,
                    "Security initialisation failed. Cannot edit records.",
                    Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        etProblems    = findViewById(R.id.etProblems);
        etAllergies   = findViewById(R.id.etAllergies);
        etMedications = findViewById(R.id.etMedications);
        Button btnSave = findViewById(R.id.btnSaveRecord);

        nhsNumber = getIntent().getStringExtra("nhsNumber");

        if (nhsNumber == null || !ValidationUtils.validateNhsNumber(nhsNumber)) {
            Toast.makeText(this, "Invalid patient identifier.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Pre-populate fields if a record already exists
        loadExistingRecord();

        btnSave.setOnClickListener(v -> saveRecord());
    }

    // Loads any existing clinical record into the form fields.

    private void loadExistingRecord() {
        Executors.newSingleThreadExecutor().execute(() -> {
            ClinicalRecord existing = AppDatabase.getInstance(getApplicationContext())
                    .clinicalRecordDao().findByPatient(nhsNumber);

            runOnUiThread(() -> {
                if (existing != null) {
                    etProblems.setText(existing.problems);
                    etAllergies.setText(existing.allergies);
                    etMedications.setText(existing.medications);
                }
            });
        });
    }

    // Sanitises inputs and upserts the clinical record to Room.
    private void saveRecord() {
        // Sanitise all free-text inputs before storage
        String problems    = ValidationUtils.sanitiseInput(
                etProblems.getText().toString());
        String allergies   = ValidationUtils.sanitiseInput(
                etAllergies.getText().toString());
        String medications = ValidationUtils.sanitiseInput(
                etMedications.getText().toString());

        ClinicalRecord record = new ClinicalRecord();
        record.patientNhs  = nhsNumber;
        record.problems    = securityAgent.encrypt(problems);
        record.allergies   = securityAgent.encrypt(allergies);
        record.medications = securityAgent.encrypt(medications);
        record.updatedAt   = System.currentTimeMillis();

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                AppDatabase.getInstance(getApplicationContext())
                        .clinicalRecordDao().upsert(record);

                // SECURITY: Log operation result only — no clinical content
                android.util.Log.i("EditClinicalRecord", "Clinical record upserted.");

                runOnUiThread(() -> {
                    Toast.makeText(this,
                            "Clinical record saved.", Toast.LENGTH_SHORT).show();
                    finish(); // Return to PatientSummaryActivity
                });
            } catch (Exception e) {
                android.util.Log.e("EditClinicalRecord", "Upsert failed.");
                runOnUiThread(() ->
                        Toast.makeText(this,
                                "Error saving record. Please try again.",
                                Toast.LENGTH_LONG).show());
            }
        });
    }
}