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

import java.util.concurrent.Executors;

/**
 * Allows authorised staff to create or update a patient's clinical record.
 *
 * Uses upsert (REPLACE strategy) so creating and editing are the same
 * operation — if a record exists for this NHS number it is updated,
 * otherwise a new one is created.
 *
 * SECURITY:
 * - NHS number comes from Intent only — never user input
 * - All free-text fields sanitised with ValidationUtils.sanitiseInput()
 *   before being written to Room — defence-in-depth against injection
 * - No field values are logged at any point
 *
 * RBAC NOTE:
 * This screen should only be accessible to CLINICIAN and ADMIN roles.
 * A full RBAC gate using RbacPolicyEvaluator is the recommended
 * enhancement for Lab 4.
 */
public class EditClinicalRecordActivity extends AppCompatActivity {

    private EditText etProblems, etAllergies, etMedications;
    private String nhsNumber;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_clinical_record);

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

    /**
     * Loads any existing clinical record into the form fields.
     * If no record exists the fields remain empty — saving will create one.
     */
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

    /**
     * Sanitises inputs and upserts the clinical record to Room.
     *
     * SECURITY: sanitiseInput() strips injection characters from all
     * free-text clinical fields before they reach the database.
     * Room's parameterised queries provide the primary injection defence —
     * sanitiseInput() is the secondary layer.
     */
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
        record.problems    = problems;
        record.allergies   = allergies;
        record.medications = medications;
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