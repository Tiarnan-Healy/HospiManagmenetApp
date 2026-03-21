package com.example.hospimanagmenetapp.feature.appointments.ui;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.lifecycle.ViewModelProvider;

import com.example.hospimanagmenetapp.R;
import com.example.hospimanagmenetapp.data.entities.Appointment;
import com.example.hospimanagmenetapp.domain.GetTodaysAppointmentsUseCase;
import com.example.hospimanagmenetapp.feature.appointments.ui.adapters.AppointmentAdapter;

import java.util.List;
import java.util.concurrent.Executors;

public class AppointmentListFragment extends Fragment {

    private Spinner spClinic;
    private ProgressBar progress;
    private androidx.recyclerview.widget.RecyclerView rv;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_appointment_list, container, false);

        spClinic = v.findViewById(R.id.spClinic);
        progress = v.findViewById(R.id.progress);
        rv       = v.findViewById(R.id.rvAppointments);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));

        ArrayAdapter<String> clinics = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"All Clinics", "Surgery A", "Surgery B"});
        spClinic.setAdapter(clinics);

        v.findViewById(R.id.btnRefresh).setOnClickListener(b -> loadData());

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
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                new GetTodaysAppointmentsUseCase(requireContext()).execute(null);
            } catch (Exception e) {
                // Seeding failed — Room may already have data from a previous
                // session so continue anyway rather than blocking the UI
                android.util.Log.w("AppointmentList", "Seed step failed — using cached data.");
            }

            // Step 2 — observe paged data from ViewModel on the main thread
            requireActivity().runOnUiThread(() -> observePagedData());
        });
    }
    private void observePagedData() {
        AppointmentAdapter adapter = new AppointmentAdapter(item -> {
            BookingFragment f = BookingFragment.newInstance(item);
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.appointmentContainer, f)
                    .addToBackStack(null)
                    .commit();
        });

        rv.setAdapter(adapter);

        AppointmentViewModel viewModel = new ViewModelProvider(this)
                .get(AppointmentViewModel.class);

        viewModel.pagedAppointments.observe(getViewLifecycleOwner(), pagingData -> {
            progress.setVisibility(View.GONE);
            adapter.submitData(getLifecycle(), pagingData);
        });
    }
}