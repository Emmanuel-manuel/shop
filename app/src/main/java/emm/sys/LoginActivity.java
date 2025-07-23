package emm.sys;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;

public class LoginActivity extends AppCompatActivity {
    DBHelper DB;
    private String selectedRole;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        DB = new DBHelper(this);
        Spinner spinnerRoles = findViewById(R.id.spinnerRoles);
        TextInputEditText passwordEditText = findViewById(R.id.password);
        Button btnLogIn = findViewById(R.id.btnLogIn);
        TextView signUpText = findViewById(R.id.sign_in);

        // Setup role spinner
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.roles_array,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRoles.setAdapter(adapter);

        // Capture selected role
        spinnerRoles.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedRole = parent.getItemAtPosition(position).toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedRole = null;
            }
        });

        // Login button click
        btnLogIn.setOnClickListener(v -> {
            String password = passwordEditText.getText().toString().trim();

            // Validate inputs
            if (selectedRole == null || selectedRole.isEmpty()) {
                Toast.makeText(LoginActivity.this, "Please select a role", Toast.LENGTH_SHORT).show();
                return;
            }

            if (password.isEmpty()) {
                Toast.makeText(LoginActivity.this, "Please enter password", Toast.LENGTH_SHORT).show();
                return;
            }

            // Verify credentials
            if (DB.checkRolePassword(selectedRole, password)) {
                Toast.makeText(LoginActivity.this, "Login successful", Toast.LENGTH_SHORT).show();

                // Pass role to landing page
                Intent intent = new Intent(LoginActivity.this, LandingPageActivity.class);
                intent.putExtra("USER_ROLE", selectedRole);
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(LoginActivity.this, "Wrong Password", Toast.LENGTH_SHORT).show();
            }
        });

        // Sign Up redirect
        signUpText.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish();
        });
    }
}