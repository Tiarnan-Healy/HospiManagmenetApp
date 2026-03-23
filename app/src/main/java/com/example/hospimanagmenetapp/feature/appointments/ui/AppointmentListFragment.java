package com.example.hospimanagmenetapp.feature.appointments.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hospimanagmenetapp.R;
import com.example.hospimanagmenetapp.data.AppDatabase;
import com.example.hospimanagmenetapp.data.entities.Appointment;
import com.example.hospimanagmenetapp.domain.GetTodaysAppointmentsUseCase;
import com.example.hospimanagmenetapp.feature.appointments.ui.adapters.AppointmentAdapter;
import com.example.hospimanagmenetapp.feature.appointments.ui.adapters.SimpleAppointmentAdapter;

import java.util.Calendar;
import java.util.List;
import java.util.concurrent.Executors;

public class AppointmentListFragment extends Fragment {

    private Spinner spClinic, spSpecialty;
    private ProgressBar progress;
    private RecyclerView rv;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_appointment_list, container, false);

        spClinic    = v.findViewById(R.id.spClinic);
        spSpecialty = v.findViewById(R.id.spSpecialty);
        progress    = v.findViewById(R.id.progress);
        rv          = v.findViewById(R.id.rvAppointments);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));

        // Clinic options
        ArrayAdapter<String> clinicAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"All Clinics", "Surgery A", "Surgery B"});
        spClinic.setAdapter(clinicAdapter);

        // Specialty options — matches strings embedded in clinicianName field
        ArrayAdapter<String> specialtyAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"All Specialties", "Surgeon", "General"});
        spSpecialty.setAdapter(specialtyAdapter);

        Button btnRefresh = v.findViewById(R.id.btnRefresh);

        // Refresh re-seeds from mock and re-queries Room
        btnRefresh.setOnClickListener(b -> loadData());

        // Initial load
        loadData();
        return v;
    }

    private void loadData() {
        progress.setVisibility(View.VISIBLE);

        // Old method, pre pagination
//        // Build adapter with click handler navigating to BookingFragment
//        AppointmentAdapter adapter = new AppointmentAdapter(item -> {
//            BookingFragment f = BookingFragment.newInstance(item);
//            requireActivity().getSupportFragmentManager()
//                    .beginTransaction()
//                    .replace(R.id.appointmentContainer, f)
//                    .addToBackStack(null)
//                    .commit();
//        });
//
//        rv.setAdapter(adapter);
//
//        // Obtain ViewModel scoped to this fragment's lifecycle
//        AppointmentViewModel viewModel = new ViewModelProvider(this).get(
//                AppointmentViewModel.class);
//
//        // Observe LiveData — submitData feeds pages to the PagingDataAdapter
//        viewModel.pagedAppointments.observe(getViewLifecycleOwner(), pagingData -> {
//            progress.setVisibility(View.GONE);
//            adapter.submitData(getLifecycle(), pagingData);
//        });

        // New method, populate Room as appointment screen empty otherwise
        // Read filter selections on main thread before going background
        String clinicSelection    = spClinic.getSelectedItemPosition() == 0
                ? null : spClinic.getSelectedItem().toString();
        String specialtySelection = spSpecialty.getSelectedItemPosition() == 0
                ? null : spSpecialty.getSelectedItem().toString();

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                // Seed Room and return filtered results in one step
                new GetTodaysAppointmentsUseCase(requireContext())
                        .execute(clinicSelection, specialtySelection);
            } catch (Exception e) {
                android.util.Log.w("AppointmentList", "Seed step failed — using cached data.");
            }
            requireActivity().runOnUiThread(this::applyFilters);
        });
    }
    private void applyFilters() {
        // Read filter selections on the main thread before going background
        String clinicSelection    = spClinic.getSelectedItemPosition() == 0
                ? null : spClinic.getSelectedItem().toString();
        String specialtySelection = spSpecialty.getSelectedItemPosition() == 0
                ? null : spSpecialty.getSelectedItem().toString();

        progress.setVisibility(View.VISIBLE);

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                // Calculate today's midnight-to-midnight window
                Calendar cal = Calendar.getInstance();
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                long start = cal.getTimeInMillis();
                cal.add(Calendar.DAY_OF_MONTH, 1);
                long end = cal.getTimeInMillis();

                // DIAGNOSTIC — remove after fixing
                android.util.Log.d("ApptDebug", "Query window start: " + start);
                android.util.Log.d("ApptDebug", "Query window end:   " + end);
                android.util.Log.d("ApptDebug", "Clinic filter:      " + clinicSelection);
                android.util.Log.d("ApptDebug", "Specialty filter:   " + specialtySelection);

                // Query Room with both filters
                List<Appointment> filtered = AppDatabase
                        .getInstance(requireContext())
                        .appointmentDao()
                        .findBetweenFiltered(start, end,
                                clinicSelection, specialtySelection);

                // DIAGNOSTIC — remove after fixing
                android.util.Log.d("ApptDebug", "Results returned:   " + filtered.size());

                requireActivity().runOnUiThread(() -> {
                    progress.setVisibility(View.GONE);

                    if (filtered.isEmpty()) {
                        Toast.makeText(getContext(),
                                "No appointments found for the selected filters.",
                                Toast.LENGTH_SHORT).show();
                    }

                    rv.setAdapter(new SimpleAppointmentAdapter(filtered, item -> {
                        BookingFragment f = BookingFragment.newInstance(item);
                        requireActivity().getSupportFragmentManager()
                                .beginTransaction()
                                .replace(R.id.appointmentContainer, f)
                                .addToBackStack(null)
                                .commit();
                    }));
                });

            } catch (Exception e) {
                requireActivity().runOnUiThread(() -> {
                    progress.setVisibility(View.GONE);
                    Toast.makeText(getContext(),
                            "Failed to load appointments. Please retry.",
                            Toast.LENGTH_LONG).show();
                });
            }
        });
    }
}