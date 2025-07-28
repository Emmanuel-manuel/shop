package emm.sys;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.ImageButton;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;

import java.util.Calendar;

public class LandingPageActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle drawerToggle;
    private FloatingActionButton fab;
    private BottomNavigationView bottomNavView;
    private NavigationView navigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_landing_page);

        TextView welcomeText = findViewById(R.id.welcomeText);
        TextView welcomeTime = findViewById(R.id.welcomeTime);

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
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


        // .................. Handle Drawer Menu navigation item clicks .................
        navigationView.setNavigationItemSelectedListener(item -> {
            // Handle menu item selection
            int id = item.getItemId();
            drawerLayout.closeDrawer(GravityCompat.START); // Close drawer after selection

            switch (id) {
                case R.id.home:
                    // Navigate to HomeFragment
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragmentContainer, new HomeFragment())
                            .commit();
//                    resetFabToWhite(); // Reset FAB color
                    return true;

                case R.id.receive_inventory:
                    // Navigate to ReceiveInventoryFragment
                    handleReceiveInventoryNavigation();
                    return true;

                case R.id.logout:
                    // Handle logout - go to LoginActivity and clear back stack
                    Intent intent = new Intent(this, LoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish(); // Finish current activity
                    return true;

                // Add other cases for additional menu items
                case R.id.settings:
                    // Handle settings
                    return true;

                case R.id.inventoryDetails:
                    // Handle inventory details
                    return true;

                case R.id.to_pay:
                    // Handle to pay
                    return true;

                case R.id.email:
                    // Handle email
                    return true;

                case R.id.contact:
                    // Handle contact
                    return true;

                default:
                    return false;
            }        });

        // ............. Working with Fragments of Bottom Navigation Buttons ..............
        // Initialize FAB and the bottom navigation View
        bottomNavView = findViewById(R.id.bottom_navView);

        fab = findViewById(R.id.fab);
        fab.setOnClickListener(view -> {

            handleReceiveInventoryNavigation();
        });




        // .........................................................................


    }


    // Helper method for inventory navigation
    private void handleReceiveInventoryNavigation() {
        // Change FAB color to blue
//        fab.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.blue));

        // Clear BottomNavigationView selection
        // 2. Clear BottomNavigationView selection
        bottomNavView.getMenu().setGroupCheckable(0, false, true); // Temporarily disable checking
        bottomNavView.getMenu().setGroupCheckable(0, true, true); // Re-enable checking
        bottomNavView.setSelectedItemId(0); // Clear selection

        // Show ReceiveInventoryFragment
        getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(
                        R.anim.fragment_enter,
                        R.anim.fragment_exit,
                        R.anim.fragment_enter,
                        R.anim.fragment_exit)
                .replace(R.id.fragmentContainer, new ReceiveInventoryFragment())
                .addToBackStack(null)
                .commit();
    }

    // Helper method to reset FAB color
//    private void resetFabToWhite() {
//        fab.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.white));
//    }



    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred
        if (drawerToggle != null) {
            drawerToggle.syncState();
        }
    }

    // ................ Device's Back Button Pressed Action Listener .................
    @Override
    public void onBackPressed() {
        // Close drawer if open
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        }
        // If not on HomeFragment, go to HomeFragment
        else if (!(getSupportFragmentManager().findFragmentById(R.id.fragmentContainer) instanceof HomeFragment)) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainer, new HomeFragment())
                    .commit();
        }
        // If already on HomeFragment, minimize app
        else {
            moveTaskToBack(true);
        }
    }


}