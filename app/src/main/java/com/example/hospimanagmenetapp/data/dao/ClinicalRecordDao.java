package com.example.hospimanagmenetapp.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.hospimanagmenetapp.data.entities.ClinicalRecord;

@Dao
public interface ClinicalRecordDao {
    @Query("SELECT * FROM clinical_records WHERE patientNhs = :nhs LIMIT 1")
    ClinicalRecord findByPatient(String nhs);

    /**
     * Insert or update a clinical record.
     * REPLACE means calling this with an existing NHS number
     * updates the record instead of creating a duplicate.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(ClinicalRecord record);
}