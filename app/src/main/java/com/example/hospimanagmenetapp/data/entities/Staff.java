package com.example.hospimanagmenetapp.data.entities; // Entity lives in the data.entities package

import androidx.annotation.NonNull;   // Annotation to mark fields that must not be null
import androidx.room.Entity;          // Marks this class as a Room table
import androidx.room.Index;           // Allows creating DB indices for faster lookups/uniqueness
import androidx.room.PrimaryKey;      // Identifies the primary key column

@Entity(
        tableName = "staff",                              // Actual SQLite table name
        indices = {@Index(value = {"email"}, unique = true)} // Each staff email must be unique
)
public class Staff {

    @PrimaryKey(autoGenerate = true) // Auto-incremented surrogate key
    public long id;                  // Local DB identifier

    @NonNull
    public String fullName;          // Staff member’s full name

    @NonNull
    public String email;             // Staff login email (unique)

    public String adminPin;          // PIN used for Admin authentication

    @NonNull
    public Role role;                // Staff role used for RBAC

    public long createdAt;           // Unix epoch millis when account was created
    public long updatedAt;           // Unix epoch millis when account was last updated


    // Enum defining allowed staff roles
    public enum Role {
        ADMIN,
        CLINICIAN,
        RECEPTIONIST
    }
}