package com.example.hospitalmanagement;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    // Firebase Authentication
    private FirebaseAuth mAuth;

    // Firebase Database
    private FirebaseDatabase database;
    private DatabaseReference patientsRef;

    // UI Elements
    private CardView loginCard, patientCard;
    private TextInputEditText emailField, passwordField, confirmPasswordField;
    private TextInputLayout confirmPasswordLayout;
    private ProgressBar progressBar;
    private Button loginBtn, registerBtn, cancelBtn, logoutBtn, addPatientBtn, viewPatientsBtn;
    private TextView formTitle;

    // Patient fields
    private TextInputEditText patientName, patientId, patientEmail, patientAge, patientDisease;
    private RecyclerView patientRecyclerView;
    private TextView patientRecordsTitle;

    // Patient data
    private PatientAdapter patientAdapter;
    private List<Patient> patientList = new ArrayList<>();

    // Track registration state
    private boolean isRegistering = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();
        patientsRef = database.getReference("patients");

        // Initialize UI elements
        initializeUI();

        // Set button listeners
        setButtonListeners();

        // Add password matching validation
        setupPasswordValidation();

        // Check if user is already logged in
        checkCurrentUser();
    }

    private void initializeUI() {
        // Authentication UI
        loginCard = findViewById(R.id.loginCard);
        patientCard = findViewById(R.id.patientCard);
        emailField = findViewById(R.id.emailField);
        passwordField = findViewById(R.id.passwordField);
        confirmPasswordField = findViewById(R.id.confirmPasswordField);
        confirmPasswordLayout = findViewById(R.id.confirmPasswordLayout);
        progressBar = findViewById(R.id.progressBar);
        loginBtn = findViewById(R.id.loginBtn);
        registerBtn = findViewById(R.id.registerBtn);
        cancelBtn = findViewById(R.id.cancelBtn);
        formTitle = findViewById(R.id.formTitle);

        // Patient management UI
        logoutBtn = findViewById(R.id.logoutBtn);
        addPatientBtn = findViewById(R.id.addPatientBtn);
        viewPatientsBtn = findViewById(R.id.viewPatientsBtn);

        // Patient fields
        patientName = findViewById(R.id.patientName);
        patientId = findViewById(R.id.patientId);
        patientEmail = findViewById(R.id.patientEmail);
        patientAge = findViewById(R.id.patientAge);
        patientDisease = findViewById(R.id.patientDisease);

        // Patient records UI
        patientRecordsTitle = findViewById(R.id.patientRecordsTitle);
        patientRecyclerView = findViewById(R.id.patientRecyclerView);

        // Setup RecyclerView
        patientRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        patientAdapter = new PatientAdapter(patientList);
        patientRecyclerView.setAdapter(patientAdapter);
    }

    private void setButtonListeners() {
        // Authentication buttons
        loginBtn.setOnClickListener(v -> {
            if (isRegistering) {
                cancelRegistration();
            } else {
                loginUser();
            }
        });

        registerBtn.setOnClickListener(v -> {
            if (isRegistering) {
                registerUser();
            } else {
                showRegistrationForm();
            }
        });

        cancelBtn.setOnClickListener(v -> cancelRegistration());

        // Patient management buttons
        logoutBtn.setOnClickListener(v -> logoutUser());
        addPatientBtn.setOnClickListener(v -> addPatient());
        viewPatientsBtn.setOnClickListener(v -> togglePatientRecords());
    }

    private void setupPasswordValidation() {
        // Add text watcher for real-time password matching validation
        TextWatcher passwordWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                validatePasswordMatch();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };

        passwordField.addTextChangedListener(passwordWatcher);
        confirmPasswordField.addTextChangedListener(passwordWatcher);
    }

    private void validatePasswordMatch() {
        String password = passwordField.getText().toString().trim();
        String confirmPassword = confirmPasswordField.getText().toString().trim();

        if (!password.isEmpty() && !confirmPassword.isEmpty()) {
            if (!password.equals(confirmPassword)) {
                confirmPasswordLayout.setError("Passwords do not match");
            } else {
                confirmPasswordLayout.setError(null);
            }
        } else {
            confirmPasswordLayout.setError(null);
        }
    }

    private boolean passwordsMatch() {
        String password = passwordField.getText().toString().trim();
        String confirmPassword = confirmPasswordField.getText().toString().trim();
        return password.equals(confirmPassword);
    }

    private void showRegistrationForm() {
        isRegistering = true;
        confirmPasswordLayout.setVisibility(View.VISIBLE);
        cancelBtn.setVisibility(View.VISIBLE);
        registerBtn.setText("Create Account");
        loginBtn.setText("Cancel");
        formTitle.setText("Create Admin Account");
    }

    private void cancelRegistration() {
        isRegistering = false;
        confirmPasswordLayout.setVisibility(View.GONE);
        cancelBtn.setVisibility(View.GONE);
        registerBtn.setText("Register");
        loginBtn.setText("Login");
        formTitle.setText("Hospital Admin Portal");
        confirmPasswordLayout.setError(null);

        // Clear fields
        emailField.setText("");
        passwordField.setText("");
        confirmPasswordField.setText("");
    }

    private void checkCurrentUser() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // User is logged in, show patient management
            showPatientManagement();
        } else {
            // No user logged in, show login form
            showLoginForm();
        }
    }

    private void showLoginForm() {
        loginCard.setVisibility(View.VISIBLE);
        patientCard.setVisibility(View.GONE);
        cancelRegistration(); // Reset form state
    }

    private void showPatientManagement() {
        loginCard.setVisibility(View.GONE);
        patientCard.setVisibility(View.VISIBLE);

        // Clear any previous patient data
        clearPatientForm();
        patientList.clear();
        patientAdapter.notifyDataSetChanged();

        // Hide patient records by default
        patientRecyclerView.setVisibility(View.GONE);
        patientRecordsTitle.setVisibility(View.GONE);
        viewPatientsBtn.setText("View Patient Records");
    }

    private void loginUser() {
        String email = emailField.getText().toString().trim();
        String password = passwordField.getText().toString().trim();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    progressBar.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        Toast.makeText(MainActivity.this, "Login successful", Toast.LENGTH_SHORT).show();
                        showPatientManagement();
                    } else {
                        Toast.makeText(MainActivity.this, "Authentication failed: " +
                                task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void registerUser() {
        String email = emailField.getText().toString().trim();
        String password = passwordField.getText().toString().trim();
        String confirmPassword = confirmPasswordField.getText().toString().trim();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password) || TextUtils.isEmpty(confirmPassword)) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate password match
        if (!passwordsMatch()) {
            confirmPasswordLayout.setError("Passwords do not match");
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    progressBar.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        Toast.makeText(MainActivity.this, "Registration successful", Toast.LENGTH_SHORT).show();
                        showPatientManagement();
                    } else {
                        Toast.makeText(MainActivity.this, "Registration failed: " +
                                task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void logoutUser() {
        mAuth.signOut();
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
        showLoginForm();
    }

    private void togglePatientRecords() {
        if (patientRecyclerView.getVisibility() == View.VISIBLE) {
            // Hide patient records
            patientRecyclerView.setVisibility(View.GONE);
            patientRecordsTitle.setVisibility(View.GONE);
            viewPatientsBtn.setText("View Patient Records");
        } else {
            // Show patient records
            patientRecordsTitle.setVisibility(View.VISIBLE);
            patientRecyclerView.setVisibility(View.VISIBLE);
            viewPatientsBtn.setText("Hide Patient Records");
            loadPatients();
        }
    }

    private void addPatient() {
        String name = patientName.getText().toString().trim();
        String id = patientId.getText().toString().trim();
        String email = patientEmail.getText().toString().trim();
        String age = patientAge.getText().toString().trim();
        String disease = patientDisease.getText().toString().trim();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(id) ||
                TextUtils.isEmpty(email) || TextUtils.isEmpty(age) || TextUtils.isEmpty(disease)) {
            Toast.makeText(this, "Please fill all patient fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create patient object
        Patient patient = new Patient(name, email, age, id, disease);

        // Save to Firebase
        patientsRef.child(id).setValue(patient)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(MainActivity.this, "Patient added successfully", Toast.LENGTH_SHORT).show();
                    clearPatientForm();

                    // Show patient records after adding
                    patientRecordsTitle.setVisibility(View.VISIBLE);
                    patientRecyclerView.setVisibility(View.VISIBLE);
                    viewPatientsBtn.setText("Hide Patient Records");
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(MainActivity.this, "Failed to add patient: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    Log.e("Firebase", "Error adding patient", e);
                });
    }

    private void clearPatientForm() {
        patientName.setText("");
        patientId.setText("");
        patientEmail.setText("");
        patientAge.setText("");
        patientDisease.setText("");
    }

    private void loadPatients() {
        patientsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                patientList.clear();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Patient patient = dataSnapshot.getValue(Patient.class);
                    if (patient != null) {
                        patientList.add(patient);
                    }
                }
                patientAdapter.notifyDataSetChanged();

                if (patientList.isEmpty()) {
                    Toast.makeText(MainActivity.this, "No patients found", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MainActivity.this, "Failed to load patients: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("Firebase", "Database error", error.toException());
            }
        });
    }

    // Patient model class
    public static class Patient {
        public String name;
        public String email;
        public String age;
        public String id;
        public String disease;

        public Patient() {
            // Default constructor required for Firebase
        }

        public Patient(String name, String email, String age, String id, String disease) {
            this.name = name;
            this.email = email;
            this.age = age;
            this.id = id;
            this.disease = disease;
        }
    }

    // Patient Adapter
    private class PatientAdapter extends RecyclerView.Adapter<PatientAdapter.PatientViewHolder> {

        private List<Patient> patients;

        public PatientAdapter(List<Patient> patients) {
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
            Patient patient = patients.get(position);
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