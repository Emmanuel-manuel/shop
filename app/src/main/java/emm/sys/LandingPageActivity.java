package emm.sys;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Calendar;

public class LandingPageActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_landing_page);

        TextView welcomeText = findViewById(R.id.welcomeText);
        TextView welcomeTime = findViewById(R.id.welcomeTime);
//        String role = getIntent().getStringExtra("USER_ROLE");
//
//        if (role != null) {
//            welcomeText.setText("Welcome, " + role + "!");
//        } else {
//            welcomeText.setText("Welcome!");
//        }
        String email = getIntent().getStringExtra("USER_EMAIL");

        if (email != null) {
            welcomeText.setText("Welcome, " + email + "!");
        } else {
            welcomeText.setText("Welcome!");
        }

        // Set time-based greeting
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);

        if (hour >= 0 && hour < 12) {
            welcomeTime.setText("Good Morning");
        } else if (hour >= 12 && hour < 17) {
            welcomeTime.setText("Good Afternoon");
        } else {
            welcomeTime.setText("Good Evening");
        }
    }
}