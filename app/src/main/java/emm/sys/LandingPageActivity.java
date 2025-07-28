package emm.sys;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.ImageButton;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;

import java.util.Calendar;

public class LandingPageActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle drawerToggle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_landing_page);

        TextView welcomeText = findViewById(R.id.welcomeText);
        TextView welcomeTime = findViewById(R.id.welcomeTime);

        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        // Set the toolbar as action bar
        setSupportActionBar(toolbar);

        // Enable home button for navigation drawer
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
        }

        // Setup navigation drawer
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(drawerToggle);
        toggle.syncState();

// ............... WELCOME MESSAGES............................................................................
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
        // ..........................................................................

        // Handle navigation item clicks
        navigationView.setNavigationItemSelectedListener(item -> {
            // Handle menu item selection
            int id = item.getItemId();

            if (id == R.id.logout) {
                // Handle logout
                finish();
            }
            // Add other cases

            // Close drawer after selection
            drawerLayout.closeDrawers();
            return true;
        });
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred
        if (drawerToggle != null) {
            drawerToggle.syncState();
        }
    }
}