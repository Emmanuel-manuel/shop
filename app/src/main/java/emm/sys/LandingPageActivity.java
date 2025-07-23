package emm.sys;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class LandingPageActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_landing_page);

        TextView welcomeText = findViewById(R.id.welcomeText);
        String role = getIntent().getStringExtra("USER_ROLE");

        if (role != null) {
            welcomeText.setText("Welcome, " + role + "!");
        } else {
            welcomeText.setText("Welcome!");
        }
    }
}