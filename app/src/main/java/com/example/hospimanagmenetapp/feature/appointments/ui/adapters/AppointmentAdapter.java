package com.example.hospimanagmenetapp.feature.appointments.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.paging.PagingDataAdapter;
import androidx.recyclerview.widget.DiffUtil;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hospimanagmenetapp.R;
import com.example.hospimanagmenetapp.data.entities.Appointment;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


// RecyclerView adapter backed by the Paging library.

public class AppointmentAdapter
        extends PagingDataAdapter<Appointment, AppointmentAdapter.VH> {

    public interface Clicker { void onClick(Appointment a); }

    private final Clicker clicker;
    private final SimpleDateFormat sdf =
            new SimpleDateFormat("dd MMM yyyy HH:mm", Locale.UK);


//     DiffUtil callback — required by PagingDataAdapter.
    private static final DiffUtil.ItemCallback<Appointment> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<Appointment>() {
                @Override
                public boolean areItemsTheSame(
                        @NonNull Appointment oldItem, @NonNull Appointment newItem) {
                    return oldItem.id == newItem.id;
                }

                @Override
                public boolean areContentsTheSame(
                        @NonNull Appointment oldItem, @NonNull Appointment newItem) {
                    return oldItem.status.equals(newItem.status)
                            && oldItem.startTime == newItem.startTime
                            && oldItem.endTime == newItem.endTime;
                }
            };

    public AppointmentAdapter(Clicker clicker) {
        super(DIFF_CALLBACK);
        this.clicker = clicker;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_appointment, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        // getItem() is provided by PagingDataAdapter — may return null
        // while the next page is loading; guard against this
        Appointment a = getItem(position);
        if (a == null) return;

        h.tvPatient.setText("NHS: " + a.patientNhsNumber);
        h.tvClinician.setText(a.clinicianName + " — " + a.clinic);
        h.tvTime.setText(sdf.format(new Date(a.startTime))
                + " → " + sdf.format(new Date(a.endTime)));
        h.itemView.setOnClickListener(v -> clicker.onClick(a));
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvPatient, tvClinician, tvTime;

        VH(View item) {
            super(item);
            tvPatient   = item.findViewById(R.id.tvPatient);
            tvClinician = item.findViewById(R.id.tvClinician);
            tvTime      = item.findViewById(R.id.tvTime);
        }
    }
}