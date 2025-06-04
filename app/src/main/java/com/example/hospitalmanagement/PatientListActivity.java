package com.example.hospitalmanagement;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class PatientListActivity extends AppCompatActivity {

    private RecyclerView patientRecyclerView;
    private PatientAdapter patientAdapter;
    private List<MainActivity.Patient> patientList = new ArrayList<>();
    private DatabaseReference patientsRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient_list);

        // Initialize Firebase
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        patientsRef = database.getReference("patients");

        // Initialize RecyclerView
        patientRecyclerView = findViewById(R.id.patientRecyclerView);
        patientRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        patientAdapter = new PatientAdapter(patientList);
        patientRecyclerView.setAdapter(patientAdapter);

        // Load patients
        loadPatients();
    }

    private void loadPatients() {
        patientsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                patientList.clear();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    MainActivity.Patient patient = dataSnapshot.getValue(MainActivity.Patient.class);
                    if (patient != null) {
                        patientList.add(patient);
                    }
                }
                patientAdapter.notifyDataSetChanged();

                if (patientList.isEmpty()) {
                    Toast.makeText(PatientListActivity.this, "No patients found", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(PatientListActivity.this, "Failed to load patients: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("Firebase", "Database error", error.toException());
            }
        });
    }

    private class PatientAdapter extends RecyclerView.Adapter<PatientAdapter.PatientViewHolder> {

        private List<MainActivity.Patient> patients;

        public PatientAdapter(List<MainActivity.Patient> patients) {
            this.patients = patients;
        }

        @NonNull
        @Override
        public PatientViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.patient_item, parent, false);
            return new PatientViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull PatientViewHolder holder, int position) {
            MainActivity.Patient patient = patients.get(position);
            holder.nameTextView.setText("Name: " + patient.name);
            holder.emailTextView.setText("Email: " + patient.email);
            holder.ageTextView.setText("Age: " + patient.age);
            holder.idTextView.setText("ID: " + patient.id);
            holder.diseaseTextView.setText("Disease: " + patient.disease);
        }

        @Override
        public int getItemCount() {
            return patients.size();
        }

        class PatientViewHolder extends RecyclerView.ViewHolder {
            TextView nameTextView, emailTextView, ageTextView, idTextView, diseaseTextView;

            public PatientViewHolder(@NonNull View itemView) {
                super(itemView);
                nameTextView = itemView.findViewById(R.id.nameTextView);
                emailTextView = itemView.findViewById(R.id.emailTextView);
                ageTextView = itemView.findViewById(R.id.ageTextView);
                idTextView = itemView.findViewById(R.id.idTextView);
                diseaseTextView = itemView.findViewById(R.id.diseaseTextView);
            }
        }
    }
}