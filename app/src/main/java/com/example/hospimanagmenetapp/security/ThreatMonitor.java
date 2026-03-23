package com.example.hospimanagmenetapp.security;

import android.util.Log;
import com.example.hospimanagmenetapp.data.AppDatabase;
import android.content.Context;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class ThreatMonitor {

    private static final String TAG = "ThreatMonitor";
    private static final int LOGIN_FAILURE_THRESHOLD = 3;

    private static ThreatMonitor INSTANCE;
    private final AtomicInteger failedLoginCount = new AtomicInteger(0);
    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor();
    private boolean accountLocked = false;

    private ThreatMonitor() {}

    public static synchronized ThreatMonitor getInstance() {
        if (INSTANCE == null) INSTANCE = new ThreatMonitor();
        return INSTANCE;
    }

    // Call from AdminLoginActivity on every failed login attempt.
    // SECURITY: Only the count is logged — never the attempted credentials.

    public void recordFailedLogin() {
        int count = failedLoginCount.incrementAndGet();
        Log.w(TAG, "Failed login attempt #" + count);
        if (count >= LOGIN_FAILURE_THRESHOLD) {
            accountLocked = true;
            // AGENTIC RESPONSE: Automatically lock account without human intervention
            Log.w(TAG, "THREAT DETECTED: threshold exceeded — account locked.");
        }
    }

//    Call from AdminLoginActivity on successful login.
//    Resets the failure counter so legitimate users aren't permanently locked.

    public void recordSuccessfulLogin() {
        failedLoginCount.set(0);
        accountLocked = false;
        Log.i(TAG, "Successful login — failure counter reset.");
    }

//    Returns true if account is locked due to excessive failures.
//    AdminLoginActivity checks this before processing any login attempt.
    public boolean isAccountLocked() {
        return accountLocked;
    }

//     Starts a background integrity check running every 60 seconds.
//     Checks that at least one ADMIN account exists — if all admins are
//     removed it could indicate tampering or privilege escalation.
//     AGENTIC: Runs automatically on a schedule with no user trigger.
//     SECURITY: Logs counts only
    public void startPeriodicIntegrityCheck(Context appContext) {
        scheduler.scheduleWithFixedDelay(() -> {
            try {
                int adminCount = AppDatabase.getInstance(appContext)
                        .staffDao().countAdmins();
                if (adminCount == 0) {
                    Log.e(TAG, "INTEGRITY ALERT: No admin accounts found — possible tampering.");
                } else {
                    Log.d(TAG, "Integrity check passed. Admin count: " + adminCount);
                }
            } catch (Exception e) {
                Log.e(TAG, "Integrity check failed — DB unreachable.");
            }
        }, 0, 60, TimeUnit.SECONDS);
    }

    // Shut down the scheduler cleanly when the app terminates
    public void shutdown() {
        scheduler.shutdownNow();
    }
}