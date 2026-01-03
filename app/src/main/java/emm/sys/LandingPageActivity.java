package emm.sys;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.ImageButton;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Calendar;

public class LandingPageActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle drawerToggle;
    private FloatingActionButton fab;
    private BottomNavigationView bottomNavView;
    private NavigationView navigationView;
    private ImageView profileIcon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_landing_page);

        TextView welcomeText = findViewById(R.id.welcomeText);
        TextView welcomeTime = findViewById(R.id.welcomeTime);
        profileIcon = findViewById(R.id.profileIcon);

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
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // Setup profile icon click listener
        setupProfileIcon();

// .......................... WELCOME MESSAGES...................................
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
            welcomeTime.setText("Good Morning!");
        } else if (hour >= 12 && hour < 17) {
            welcomeTime.setText("Good Afternoon!");
        } else {
            welcomeTime.setText("Good Evening!");
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
                    return true;


                case R.id.add_products:
                    // Navigate to ReceiveInventoryFragment
                    handleAddProductDetailsNavigation();
                    return true;

                case R.id.receive_inventory:
                    // Navigate to ReceiveInventoryFragment
                    handleReceiveInventoryNavigation();
                    return true;

                case R.id.logout:
                    // Handle logout - go to LoginActivity and clear back stack
                    performLogout();
                    return true;

                // Add other cases for additional menu items
                case R.id.settings:
                    // Handle settings
                    return true;


                case R.id.productDetails:
                    // Handle View Today's inventory details
                    handleViewProductDetailsNavigation();
                    return true;

                case R.id.inventoryDetails:
                    // Handle View Today's inventory details
                    handleViewTodayInventoryNavigation();
                    return true;

                case R.id.issue_goods:
                    transition();
                    handleIssueGoodsNavigation();
                    return true;

                case R.id.viewIssuedDetails:
                    transition();
                    handleViewIssuedGoodsNavigation();
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
            }
        });

        // ............. Working with Fragments of Bottom Navigation Buttons ..............
        // BEGINNING OF BOTTOM NAVIGATION VIEW
        replaceFragment(new HomeFragment());

        bottomNavView = findViewById(R.id.bottom_navView);

//        bottomNavView.setBackground(null);
        bottomNavView.setOnItemSelectedListener(item -> {


            switch (item.getItemId()) {
                case R.id.home:
                    replaceFragment(new HomeFragment());
                    break;
                case R.id.issue_goods:
                    transition();
                    replaceFragment(new IssueGoodsFragment());
                    break;
                case R.id.to_pay:
                    replaceFragment(new ToPayFragment());
                    break;
                case R.id.announcements:
                    replaceFragment(new AnnouncementFragment());
                    break;
            }
            return true;
        });

        // Set default selection
        bottomNavView.setSelectedItemId(R.id.home);

        // Initialize FAB and the bottom navigation View
        fab = findViewById(R.id.fab);
        fab.setOnClickListener(view -> {
            handleReceiveInventoryNavigation();
        });
    }

    // Setup profile icon functionality
    private void setupProfileIcon() {
        if (profileIcon != null) {
            profileIcon.setOnClickListener(view -> {
                showProfileMenu(view);
            });
        }
    }

    // Show profile menu popup
    private void showProfileMenu(View anchorView) {
        PopupMenu popupMenu = new PopupMenu(this, anchorView);
        popupMenu.getMenuInflater().inflate(R.menu.profile_menu, popupMenu.getMenu());


        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                int id = item.getItemId();

                if (id == R.id.settings) {
                    // Handle settings action
                    handleSettingsNavigation();
                    return true;
                } else if (id == R.id.logout) {
                    // Handle logout action
                    performLogout();
                    return true;
                }
                return false;
            }
        });

        popupMenu.show();
    }

    // Handle settings navigation
    private void handleSettingsNavigation() {
        // Navigate to SettingsFragment or SettingsActivity
        // Example: replaceFragment(new SettingsFragment());
        // Toast.makeText(this, "Settings clicked", Toast.LENGTH_SHORT).show();

        // Clear BottomNavigationView selection
        bottomNavView.getMenu().setGroupCheckable(0, false, true);
        bottomNavView.getMenu().setGroupCheckable(0, true, true);
        bottomNavView.setSelectedItemId(0);

        // If you have a SettingsFragment, uncomment below:
         getSupportFragmentManager().beginTransaction()
                 .setCustomAnimations(
                         R.anim.fragment_enter,
                         R.anim.fragment_exit,
                         R.anim.fragment_enter,
                         R.anim.fragment_exit)
                 .replace(R.id.fragmentContainer, new SettingsFragment())
                 .addToBackStack(null)
                 .commit();
    }

    // Perform logout
    private void performLogout() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
    // .........................................................................



    //    OUTSIDE "onCreate"
    private  void replaceFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.setCustomAnimations(
                R.anim.fragment_enter,
                R.anim.fragment_exit,
                R.anim.fragment_enter,
                R.anim.fragment_exit
        );

        transaction.replace(R.id.fragmentContainer, fragment);
        // Only add to back stack if not the home fragment
        if (!(fragment instanceof HomeFragment)) {
            transaction.addToBackStack(null);
        }
        transaction.commit();
    }

    // .............. HELPER METHODS FOR DRAWER NAVIGATION TRANSITIONING ..........
    // Helper method for Receive inventory navigation
    void handleAddProductDetailsNavigation() {

        // Clear BottomNavigationView selection
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
                .replace(R.id.fragmentContainer, new ProductDetailsFragment())
                .addToBackStack(null)
                .commit();
    }
    // Helper method for Receive inventory navigation
    private void handleReceiveInventoryNavigation() {

        // Clear BottomNavigationView selection
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
    // Helper method for View Product Details navigation
    public void handleViewProductDetailsNavigation() {

        // Clear BottomNavigationView selection
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
                .replace(R.id.fragmentContainer, new ViewProductsDetailsFragment())
                .addToBackStack(null)
                .commit();
    }
    // Helper method for View Today's inventory navigation
    private void handleViewTodayInventoryNavigation() {

        // Clear BottomNavigationView selection
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
                .replace(R.id.fragmentContainer, new ViewTodayInventoryFragment())
                .addToBackStack(null)
                .commit();
    }
    // Helper method for View Issued Goods/ inventory navigation
    private void handleIssueGoodsNavigation() {

        // Clear BottomNavigationView selection
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
                .replace(R.id.fragmentContainer, new IssueGoodsFragment())
                .addToBackStack(null)
                .commit();
    }
    // Helper method for View Today's inventory navigation
    private void handleViewIssuedGoodsNavigation() {

        // Clear BottomNavigationView selection
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
                .replace(R.id.fragmentContainer, new ViewIssuedGoodsFragment())
                .addToBackStack(null)
                .commit();
    }
    // method for smooth transition
    private void transition(){
        getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(
                        R.anim.fragment_enter,
                        R.anim.fragment_exit,
                        R.anim.fragment_enter,
                        R.anim.fragment_exit);
    }

    // .............. END OF HELPER METHODS FOR DRAWER NAVIGATION TRANSITIONING ............

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