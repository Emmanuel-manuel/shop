package emm.sys;

import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ViewTodayInventoryFragment extends Fragment {
    private RecyclerView recyclerView;
    private InventoryAdapter adapter;
    private DBHelper dbHelper;
    private SearchView searchView;
    private TextView txtTotalItems, txtLastUpdated;
    private FloatingActionButton fabRefresh;
    private View emptyStateView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_view_today_inventory, container, false);

        // Initialize views
        recyclerView = view.findViewById(R.id.userRecyclerView);
        searchView = view.findViewById(R.id.search);
        txtTotalItems = view.findViewById(R.id.txtTotalItems);
        txtLastUpdated = view.findViewById(R.id.txtLastUpdated);
        emptyStateView = view.findViewById(R.id.emptyState);
        fabRefresh = view.findViewById(R.id.fabRefresh);

        dbHelper = new DBHelper(getActivity());

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        adapter = new InventoryAdapter(new ArrayList<>());
        recyclerView.setAdapter(adapter);

        // Load today's inventory
        loadTodayInventory();

        // Setup search functionality
        setupSearchView();

        // Setup refresh FAB
        fabRefresh.setOnClickListener(v -> {
            // Add rotation animation
            fabRefresh.animate().rotationBy(360).setDuration(500).start();
            loadTodayInventory();
        });

        return view;
    }

    private void setupSearchView() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                // Hide keyboard when search is submitted
                searchView.clearFocus();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filter(newText);
                return true;
            }
        });

        // Clear search when close button clicked
        searchView.setOnCloseListener(() -> {
            loadTodayInventory();
            return false;
        });
    }

    private void loadTodayInventory() {
        new Thread(() -> {
            Cursor cursor = dbHelper.getTodayInventory();
            List<InventoryItem> items = new ArrayList<>();
            int totalItems = 0;
            int totalDelivered = 0;
            int totalBalance = 0;

            if (cursor != null) {
                try {
                    int productNameIndex = cursor.getColumnIndexOrThrow("product_name");
                    int weightIndex = cursor.getColumnIndexOrThrow("weight");
                    int flavourIndex = cursor.getColumnIndexOrThrow("flavour");
                    int quantityIndex = cursor.getColumnIndexOrThrow("quantity");
                    int balanceIndex = cursor.getColumnIndexOrThrow("balance");
                    int timestampIndex = cursor.getColumnIndexOrThrow("timestamp");

                    if (cursor.moveToFirst()) {
                        do {
                            int delivered = cursor.getInt(quantityIndex);
                            int balance = cursor.getInt(balanceIndex);

                            InventoryItem item = new InventoryItem(
                                    cursor.getString(productNameIndex),
                                    cursor.getString(weightIndex),
                                    cursor.getString(flavourIndex),
                                    delivered,
                                    balance,
                                    cursor.getString(timestampIndex)
                            );
                            items.add(item);
                            totalItems++;
                            totalDelivered += delivered;
                            totalBalance += balance;
                        } while (cursor.moveToNext());
                    }
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                } finally {
                    cursor.close();
                }
            }

            final List<InventoryItem> finalItems = items;
            final int finalTotalItems = totalItems;
            final int finalTotalDelivered = totalDelivered;
            final int finalTotalBalance = totalBalance;

            new Handler(Looper.getMainLooper()).post(() -> {
                adapter.updateList(finalItems);

                // Update summary
                String time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
                txtTotalItems.setText(String.format(Locale.getDefault(),
                        "Items: %d | Delivered: %d", finalTotalItems, finalTotalDelivered));
                txtLastUpdated.setText(String.format(Locale.getDefault(),
                        "Balance: %d | %s", finalTotalBalance, time));

                // Show/hide empty state
                if (finalItems.isEmpty()) {
                    recyclerView.setVisibility(View.GONE);
                    emptyStateView.setVisibility(View.VISIBLE);
                } else {
                    recyclerView.setVisibility(View.VISIBLE);
                    emptyStateView.setVisibility(View.GONE);
                }
            });
        }).start();
    }

    private void filter(String text) {
        new Thread(() -> {
            List<InventoryItem> filteredList = new ArrayList<>();
            Cursor cursor = dbHelper.getTodayInventory();

            if (cursor != null) {
                try {
                    int productNameIndex = cursor.getColumnIndexOrThrow("product_name");
                    int weightIndex = cursor.getColumnIndexOrThrow("weight");
                    int flavourIndex = cursor.getColumnIndexOrThrow("flavour");
                    int quantityIndex = cursor.getColumnIndexOrThrow("quantity");
                    int balanceIndex = cursor.getColumnIndexOrThrow("balance");
                    int timestampIndex = cursor.getColumnIndexOrThrow("timestamp");

                    if (cursor.moveToFirst()) {
                        do {
                            String productName = cursor.getString(productNameIndex);
                            String flavour = cursor.getString(flavourIndex);
                            // Search in both product name and flavour
                            if (productName.toLowerCase().contains(text.toLowerCase()) ||
                                    flavour.toLowerCase().contains(text.toLowerCase())) {
                                filteredList.add(new InventoryItem(
                                        productName,
                                        cursor.getString(weightIndex),
                                        flavour,
                                        cursor.getInt(quantityIndex),
                                        cursor.getInt(balanceIndex),
                                        cursor.getString(timestampIndex)
                                ));
                            }
                        } while (cursor.moveToNext());
                    }
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                } finally {
                    cursor.close();
                }
            }

            final List<InventoryItem> finalFilteredList = filteredList;
            new Handler(Looper.getMainLooper()).post(() -> {
                adapter.updateList(finalFilteredList);

                // Update search results count
                if (!text.isEmpty()) {
                    txtTotalItems.setText(String.format(Locale.getDefault(),
                            "Found: %d items", finalFilteredList.size()));
                } else {
                    // Reload full list summary
                    loadTodayInventory();
                }

                // Show message if no items found
                if (finalFilteredList.isEmpty() && !text.isEmpty()) {
                    Toast.makeText(getActivity(),
                            "No items found for: \"" + text + "\"",
                            Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh data when fragment becomes visible again
        loadTodayInventory();
    }

    @Override
    public void onDestroy() {
        if (dbHelper != null) {
            dbHelper.close();
        }
        super.onDestroy();
    }
}