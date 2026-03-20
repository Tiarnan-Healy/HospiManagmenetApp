package com.example.hospimanagmenetapp.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.hospimanagmenetapp.data.entities.Vitals;

import java.util.List;

@Dao
public interface VitalsDao {
    @Query("SELECT * FROM vitals WHERE synced = 0")
    List<Vitals> getPending();

    /**
     * Insert a new vitals reading. Returns the new row ID
     */
    @Insert
    long insert(Vitals v);

    /**
     * Mark a vitals record as successfully synced
     *
     * SECURITY: :id is parameterised — no injection risk.
     */
    @Query("UPDATE vitals SET synced = 1 WHERE id = :id")
    void markSynced(long id);
}