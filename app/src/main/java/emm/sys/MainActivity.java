package emm.sys;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;

public class MainActivity extends AppCompatActivity {
    DBHelper DB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        DB = new DBHelper(this);
        Spinner spinnerRoles = findViewById(R.id.spinnerRoles);
        TextInputEditText emailEditText = findViewById(R.id.email);
        TextInputEditText passwordEditText = findViewById(R.id.password);
        TextInputEditText rePasswordEditText = findViewById(R.id.re_password);
        Button btnSignUp = findViewById(R.id.btnSignUp);
        TextView signInText = findViewById(R.id.sign_in);

        // Sign Up Button Click
        btnSignUp.setOnClickListener(v -> {
            String role = spinnerRoles.getSelectedItem().toString();
            String email = emailEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();
            String rePassword = rePasswordEditText.getText().toString().trim();

            // Validate passwords match
            if (!password.equals(rePassword)) {
                Toast.makeText(MainActivity.this, "Password and Confirm Password don't match", Toast.LENGTH_SHORT).show();
                return;
            }

            // Check if user exists
            if (DB.checkusername(email)) {
                Toast.makeText(MainActivity.this, "User already exists!", Toast.LENGTH_SHORT).show();
                return;
            }

            // Insert new user
            if (DB.insertData(role, email, password)) {
                Toast.makeText(MainActivity.this, "Registered successfully", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(MainActivity.this, LoginActivity.class));
                finish();  // Close current activity
            } else {
                Toast.makeText(MainActivity.this, "Registration failed", Toast.LENGTH_SHORT).show();
            }
        });

        // Already have account? Login redirect
        signInText.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
        });
    }
}