package com.example.hospimanagmenetapp.feature.appointments.ui;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.hospimanagmenetapp.R;
import com.example.hospimanagmenetapp.data.entities.Appointment;
import com.example.hospimanagmenetapp.domain.BookOrRescheduleAppointmentUseCase;
import com.example.hospimanagmenetapp.domain.DetectScheduleConflictsUseCase;
import com.example.hospimanagmenetapp.security.auth.RbacPolicyEvaluator;
import com.example.hospimanagmenetapp.util.SessionManager;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.Executors;

public class BookingFragment extends Fragment {

    private static final String ARG_CLINICIAN_ID = "clinicianId";
    private static final String ARG_CLINICIAN_NAME = "clinicianName";
    private static final String ARG_PATIENT_NHS = "patientNhs";
    private static final String ARG_START = "start";
    private static final String ARG_END = "end";
    private static final String ARG_CLINIC = "clinic";

    private final SimpleDateFormat displayFormat =
            new SimpleDateFormat("dd MMM yyyy HH:mm", Locale.UK);

    // Internal epoch values set by the pickers, used for DB storage
    private long startMillis = 0;
    private long endMillis   = 0;

    // Picker state - date is set first, then time is applied to it
    private final Calendar startCal = Calendar.getInstance();
    private final Calendar endCal   = Calendar.getInstance();

    private TextView tvClinician, tvStartDisplay, tvEndDisplay;
    private EditText etNhs;
    private Button btnPickStartDate, btnPickStartTime;
    private Button btnPickEndDate, btnPickEndTime;
    private Button btnConfirm;

    public static BookingFragment newInstance(Appointment a) {
        Bundle b = new Bundle();
        b.putLong(ARG_CLINICIAN_ID, a.clinicianId);
        b.putString(ARG_CLINICIAN_NAME, a.clinicianName);
        b.putString(ARG_PATIENT_NHS, a.patientNhsNumber);
        b.putLong(ARG_START, a.startTime);
        b.putLong(ARG_END, a.endTime);
        b.putString(ARG_CLINIC, a.clinic);
        BookingFragment f = new BookingFragment();
        f.setArguments(b);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_booking, container, false);

        tvClinician    = v.findViewById(R.id.tvClinician);
        tvStartDisplay = v.findViewById(R.id.tvStartDisplay);
        tvEndDisplay   = v.findViewById(R.id.tvEndDisplay);
        etNhs          = v.findViewById(R.id.etNhsBooking);
        btnPickStartDate = v.findViewById(R.id.btnPickStartDate);
        btnPickStartTime = v.findViewById(R.id.btnPickStartTime);
        btnPickEndDate   = v.findViewById(R.id.btnPickEndDate);
        btnPickEndTime   = v.findViewById(R.id.btnPickEndTime);
        btnConfirm       = v.findViewById(R.id.btnConfirmBooking);

        Bundle args = getArguments();
        if (args != null) {
            tvClinician.setText("Clinician: " + args.getString(ARG_CLINICIAN_NAME, ""));
            etNhs.setText(args.getString(ARG_PATIENT_NHS, ""));

            // Pre-fill pickers from existing appointment times
            startMillis = args.getLong(ARG_START);
            endMillis   = args.getLong(ARG_END);
            startCal.setTimeInMillis(startMillis);
            endCal.setTimeInMillis(endMillis);
            updateDisplays();
        }

        btnPickStartDate.setOnClickListener(x -> showDatePicker(true));
        btnPickStartTime.setOnClickListener(x -> showTimePicker(true));
        btnPickEndDate.setOnClickListener(x -> showDatePicker(false));
        btnPickEndTime.setOnClickListener(x -> showTimePicker(false));
        btnConfirm.setOnClickListener(x -> confirm());

        return v;
    }

    private void showDatePicker(boolean isStart) {
        Calendar cal = isStart ? startCal : endCal;
        new DatePickerDialog(
                requireContext(),
                (picker, year, month, day) -> {
                    cal.set(Calendar.YEAR, year);
                    cal.set(Calendar.MONTH, month);
                    cal.set(Calendar.DAY_OF_MONTH, day);
                    if (isStart) {
                        startMillis = cal.getTimeInMillis();
                    } else {
                        endMillis = cal.getTimeInMillis();
                    }
                    updateDisplays();
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    private void showTimePicker(boolean isStart) {
        Calendar cal = isStart ? startCal : endCal;
        new TimePickerDialog(
                requireContext(),
                (picker, hour, minute) -> {
                    cal.set(Calendar.HOUR_OF_DAY, hour);
                    cal.set(Calendar.MINUTE, minute);
                    cal.set(Calendar.SECOND, 0);
                    cal.set(Calendar.MILLISECOND, 0);
                    if (isStart) {
                        startMillis = cal.getTimeInMillis();
                    } else {
                        endMillis = cal.getTimeInMillis();
                    }
                    updateDisplays();
                },
                cal.get(Calendar.HOUR_OF_DAY),
                cal.get(Calendar.MINUTE),
                true // 24-hour format
        ).show();
    }

    private void updateDisplays() {
        tvStartDisplay.setText(startMillis > 0
                ? displayFormat.format(new Date(startMillis))
                : "Not selected");
        tvEndDisplay.setText(endMillis > 0
                ? displayFormat.format(new Date(endMillis))
                : "Not selected");
    }

    private void confirm() {
        // RBAC check - main thread, no DB access
        if (!RbacPolicyEvaluator.canBookOrReschedule(requireContext())) {
            android.util.Log.d("BookingFragment", "RBAC block - role: "
                    + SessionManager.getCurrentRole(requireContext()));
            Toast.makeText(getContext(),
                    "You do not have permission to book appointments.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        String nhs = etNhs.getText().toString().trim();
        if (TextUtils.isEmpty(nhs)) {
            Toast.makeText(getContext(),
                    "NHS number is required.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate both times have been selected
        if (startMillis == 0 || endMillis == 0) {
            Toast.makeText(getContext(),
                    "Please select both a start and end date and time.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // End must be after start
        if (endMillis <= startMillis) {
            Toast.makeText(getContext(),
                    "End time must be after start time.", Toast.LENGTH_SHORT).show();
            return;
        }

        Bundle args      = getArguments();
        long clinicianId = args.getLong(ARG_CLINICIAN_ID);
        String clinic    = args.getString(ARG_CLINIC);

        // All DB work on background thread
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                // Conflict detection - must be off main thread
                boolean conflict = new DetectScheduleConflictsUseCase(requireContext())
                        .hasConflict(clinicianId, startMillis, endMillis);

                if (conflict) {
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(getContext(),
                                    "Time conflict detected. Please choose another slot.",
                                    Toast.LENGTH_LONG).show());
                    return;
                }

                Appointment a      = new Appointment();
                a.patientNhsNumber = nhs;
                a.clinicianId      = clinicianId;
                a.clinicianName    = args.getString(ARG_CLINICIAN_NAME);
                a.startTime        = startMillis;
                a.endTime          = endMillis;
                a.clinic           = clinic;
                a.status           = "BOOKED";

                new BookOrRescheduleAppointmentUseCase(requireContext()).execute(a);

                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(),
                            "Appointment confirmed for "
                                    + displayFormat.format(new Date(startMillis)),
                            Toast.LENGTH_LONG).show();
                    requireActivity().getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.appointmentContainer, new AppointmentListFragment())
                            .commit();
                });

            } catch (Exception e) {
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(getContext(),
                                "Booking failed. Please try again.",
                                Toast.LENGTH_LONG).show());
            }
        });
    }
}
