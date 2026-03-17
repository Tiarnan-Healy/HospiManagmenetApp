package com.example.hospimanagmenetapp.data.entities; // Entity lives in the data.entities package

import androidx.annotation.NonNull;   // Annotation to mark fields that must not be null
import androidx.room.Entity;          // Marks this class as a Room table
import androidx.room.Index;           // Allows creating DB indices for faster lookups/uniqueness
import androidx.room.PrimaryKey;      // Identifies the primary key column

@Entity(tableName = "staff")    // Marks this class as a Room entity mapped to the 'staff' table
public class Staff {              // Defines the Staff model class representing a table row

    public enum Role { ADMIN, CLINICIAN, RECEPTION }          // Enum defining possible staff roles within the system

    @PrimaryKey(autoGenerate = true) public long id;  // Primary key; Room auto-generates a unique ID for each staff member

    public String fullName;      // Full name of the staff member (optional field)

    @NonNull public String email;     // Email address; must not be null — used to identify a staff member

    @NonNull public Role role;      // Role field using the Role enum; must not be null

    public String adminPin;       // Optional PIN used only for admin authentication
}                                  // End of Staff entity class
