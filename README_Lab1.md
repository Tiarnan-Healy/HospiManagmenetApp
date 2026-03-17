## Lab 1 Complete

### Overview

This is an Android hospital management application developed as part of Secure 
Development and Deployment. Lab 1 establishes the technical foundation for all 
subsequent labs, covering patient registration, staff management, and role-based
access control.

---

### What This Lab Demonstrates

- Clean layered architecture (UI → Utility → Data)
- Validated patient registration with NHS Mod 11 checksum
- Secure local data persistence using Room (SQLite ORM)
- Role-Based Access Control (RBAC) restricting admin features
- Session management via SharedPreferences
- First-run bootstrap flow to create the initial Admin account

---

### Package Structure

- data/entities/ - Room entity classes (Patient, Staff)
- data/dao/ - Data Access Objects (PatientDao, StaffDao)
- data/AppDatabase.java - Room database singleton
- ui/adapters/ - RecyclerView adapter (StaffAdapter)
- ui/ - AdminLoginActivity, AdminPortalActivity, PatientRegistrationActivity
- util/ - SessionManager, ValidationUtils
- MainActivity.java - Entry point and navigation hub

---

### Features

- **Patient Registration** - captures NHS number, full name, date of birth, phone, and 
email; validates the NHS number using Mod 11; prevents duplicate records; stores data 
in Room.
- **Admin Portal** - staff registration with roles (admin, clinician, reception). 
PIN required for admin role. Staff list displayed in a RecyclerView. Access restricted
to authenticated Admins.
- **Admin Login** - Email and PIN authentication verified against the Room database.
- **Session Management** - Clear Session button acts as logout

---

### Security

- NHS number integrity enforced via Mod 11 checksum in `ValidationUtils`.
- Admin PINs hashed using SHA-256 before storage.
- Unique database index on `nhsNumber` prevents duplicate patient records.
- RBAC role check in `AdminPortalActivity` blocks unauthorised access.
- `countAdmins()` query gates the first-run setup button.

---

### First Launch

- Device/Emulator running Android 7.0+
- Open the app - no user is signed in
- Tap **Admin Portal** → setup button is enabled (no admin exists yet)
- Register the first Admin with an email and PIN
- Return to the main screen and log in with those credentials