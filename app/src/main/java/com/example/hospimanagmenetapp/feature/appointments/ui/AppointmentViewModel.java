package com.example.hospimanagmenetapp.feature.appointments.ui;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.paging.Pager;
import androidx.paging.PagingConfig;
import androidx.paging.PagingData;
import androidx.paging.PagingLiveData;

import com.example.hospimanagmenetapp.data.AppDatabase;
import com.example.hospimanagmenetapp.data.entities.Appointment;

// ViewModel for the appointment list screen.

public class AppointmentViewModel extends AndroidViewModel {

    private static final int PAGE_SIZE = 4;

    public final LiveData<PagingData<Appointment>> pagedAppointments;

    // Assignment must be inside the constructor, not loose in the class body
    public AppointmentViewModel(Application application) {
        super(application);

        pagedAppointments = PagingLiveData.getLiveData(new Pager<>(
                new PagingConfig(
                        PAGE_SIZE,
                        2,
                        false
                ),
                () -> AppDatabase.getInstance(application)
                        .appointmentDao()
                        .getAllPaged()
        ));
    }
}