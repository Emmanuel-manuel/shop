package emm.sys;

import android.app.DatePickerDialog;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HomeFragment extends Fragment {

    private MaterialButton btnDateFilter;
    private TextView txtTotalInventory, txtTotalIssued, txtRemainingBalance, txtDateRange;
    private TextView txtInventoryNote, txtTopProductsNote, txtDistributionNote;
    private LinearLayout inventoryProgressContainer, topProductsContainer, distributionContainer;

    private DBHelper dbHelper;
    private String selectedDate = "";
    private SimpleDateFormat displayFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
    private SimpleDateFormat dbDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    // Add a handler to manage background tasks
    private Handler handler;
    private boolean isFragmentActive = true;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        initializeViews(view);
        handler = new Handler(Looper.getMainLooper());

        // Initialize DBHelper only if needed
        if (dbHelper == null) {
            dbHelper = new DBHelper(getActivity());
        }

        loadTodayAnalytics();

        btnDateFilter.setOnClickListener(v -> showDatePickerDialog());

        return view;
    }

    private void initializeViews(View view) {
        btnDateFilter = view.findViewById(R.id.btnDateFilter);
        txtTotalInventory = view.findViewById(R.id.txtTotalInventory);
        txtTotalIssued = view.findViewById(R.id.txtTotalIssued);
        txtRemainingBalance = view.findViewById(R.id.txtRemainingBalance);
        txtDateRange = view.findViewById(R.id.txtDateRange);

        txtInventoryNote = view.findViewById(R.id.txtInventoryNote);
        txtTopProductsNote = view.findViewById(R.id.txtTopProductsNote);
        txtDistributionNote = view.findViewById(R.id.txtDistributionNote);

        inventoryProgressContainer = view.findViewById(R.id.inventoryProgressContainer);
        topProductsContainer = view.findViewById(R.id.topProductsContainer);
        distributionContainer = view.findViewById(R.id.distributionContainer);
    }

    private void showDatePickerDialog() {
        // Check if fragment is attached to activity
        if (!isAdded() || getActivity() == null) {
            return;
        }

        final Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                getActivity(),
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    Calendar selectedCalendar = Calendar.getInstance();
                    selectedCalendar.set(selectedYear, selectedMonth, selectedDay);

                    selectedDate = dbDateFormat.format(selectedCalendar.getTime());
                    String displayDate = displayFormat.format(selectedCalendar.getTime());

                    btnDateFilter.setText("📅 " + displayDate);
                    loadAnalyticsByDate(selectedDate);
                },
                year, month, day
        );

        datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        datePickerDialog.show();
    }

    private void loadTodayAnalytics() {
        selectedDate = "";
        btnDateFilter.setText("📅 Today");
        String todayDate = dbDateFormat.format(new Date());
        loadAnalyticsByDate(todayDate);
    }

    private void loadAnalyticsByDate(String date) {
        // Check if fragment is still attached
        if (!isAdded() || getActivity() == null) {
            return;
        }

        new Thread(() -> {
            try {
                // Check if fragment is still active
                if (!isFragmentActive) {
                    return;
                }

                // Get data from database
                Map<String, Integer> inventoryMap = getInventoryByDate(date);
                Map<String, Integer> issuedMap = getIssuedGoodsByDate(date);

                // Calculate totals
                int totalInventory = 0;
                int totalIssued = 0;

                for (Map.Entry<String, Integer> entry : inventoryMap.entrySet()) {
                    totalInventory += entry.getValue();
                }

                for (Map.Entry<String, Integer> entry : issuedMap.entrySet()) {
                    totalIssued += entry.getValue();
                }

                int remainingBalance = totalInventory - totalIssued;

                // Get top products (sorted by issued quantity)
                List<Map.Entry<String, Integer>> topProducts = getTopProducts(issuedMap, 5);

                // Find max inventory for progress bar scaling
                int maxInventory = 0;
                for (Map.Entry<String, Integer> entry : inventoryMap.entrySet()) {
                    int quantity = entry.getValue();
                    if (quantity > maxInventory) {
                        maxInventory = quantity;
                    }
                }

                final int finalTotalInventory = totalInventory;
                final int finalTotalIssued = totalIssued;
                final int finalRemainingBalance = remainingBalance;
                final int finalMaxInventory = maxInventory;
                final Map<String, Integer> finalInventoryMap = inventoryMap;
                final Map<String, Integer> finalIssuedMap = issuedMap;
                final List<Map.Entry<String, Integer>> finalTopProducts = topProducts;
                final String displayDate = date.equals(dbDateFormat.format(new Date())) ? "Today" : formatDateForDisplay(date);

                // Post UI updates to main thread
                if (handler != null && isFragmentActive) {
                    handler.post(() -> {
                        // Check if fragment is still attached
                        if (!isAdded() || getActivity() == null) {
                            return;
                        }

                        // Update summary stats
                        txtTotalInventory.setText(String.valueOf(finalTotalInventory));
                        txtTotalIssued.setText(String.valueOf(finalTotalIssued));
                        txtRemainingBalance.setText(String.valueOf(finalRemainingBalance));
                        txtDateRange.setText(displayDate + " Analytics");

                        // Update inventory progress bars
                        updateInventoryProgress(finalInventoryMap, finalIssuedMap, finalMaxInventory);

                        // Update top products
                        updateTopProducts(finalTopProducts);

                        // Update distribution view
                        updateDistributionView(finalInventoryMap, finalIssuedMap);

                        // Update notes
                        txtInventoryNote.setText(finalInventoryMap.size() + " products in inventory");
                        txtTopProductsNote.setText("Top " + Math.min(5, finalTopProducts.size()) + " issued products");
                        txtDistributionNote.setText("Comparing " + finalInventoryMap.size() + " products");

                        // Show message if no data
                        if (finalInventoryMap.isEmpty() && finalIssuedMap.isEmpty()) {
                            Toast.makeText(getActivity(),
                                    "No data for selected date", Toast.LENGTH_SHORT).show();
                        }
                    });
                }

            } catch (Exception e) {
                e.printStackTrace();
                // Log error but don't crash
                if (handler != null && isFragmentActive && isAdded() && getActivity() != null) {
                    handler.post(() -> {
                        Toast.makeText(getActivity(),
                                "Error loading analytics", Toast.LENGTH_SHORT).show();
                    });
                }
            }
        }).start();
    }

    private Map<String, Integer> getInventoryByDate(String date) {
        Map<String, Integer> inventoryMap = new HashMap<>();

        try {
            // Use DBHelper method instead of direct SQLiteDatabase access
            Cursor cursor = dbHelper.getReadableDatabase().rawQuery(
                    "SELECT product_name, SUM(quantity) as total_quantity " +
                            "FROM inventory WHERE date(timestamp) = ? " +
                            "GROUP BY product_name",
                    new String[]{date}
            );

            if (cursor != null) {
                while (cursor.moveToNext()) {
                    String productName = cursor.getString(0);
                    int quantity = cursor.getInt(1);
                    inventoryMap.put(productName, quantity);
                }
                cursor.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return inventoryMap;
    }

    private Map<String, Integer> getIssuedGoodsByDate(String date) {
        Map<String, Integer> issuedMap = new HashMap<>();

        try {
            Cursor cursor = dbHelper.getReadableDatabase().rawQuery(
                    "SELECT product_name, SUM(quantity) as total_quantity " +
                            "FROM issue_goods WHERE date(timestamp) = ? " +
                            "GROUP BY product_name",
                    new String[]{date}
            );

            if (cursor != null) {
                while (cursor.moveToNext()) {
                    String productName = cursor.getString(0);
                    int quantity = cursor.getInt(1);
                    issuedMap.put(productName, quantity);
                }
                cursor.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return issuedMap;
    }

    private List<Map.Entry<String, Integer>> getTopProducts(Map<String, Integer> issuedMap, int limit) {
        List<Map.Entry<String, Integer>> sortedEntries = new ArrayList<>(issuedMap.entrySet());

        // Use Collections.sort() instead of List.sort() for API level 23 compatibility
        Collections.sort(sortedEntries, new Comparator<Map.Entry<String, Integer>>() {
            @Override
            public int compare(Map.Entry<String, Integer> a, Map.Entry<String, Integer> b) {
                return b.getValue().compareTo(a.getValue());
            }
        });

        return sortedEntries.subList(0, Math.min(limit, sortedEntries.size()));
    }

    private void updateInventoryProgress(Map<String, Integer> inventoryMap,
                                         Map<String, Integer> issuedMap,
                                         int maxInventory) {
        // Clear existing views
        inventoryProgressContainer.removeAllViews();

        // Check if fragment is still attached
        if (!isAdded() || getActivity() == null) {
            return;
        }

        if (inventoryMap.isEmpty()) {
            TextView noDataText = new TextView(getActivity()); // Use getActivity() context
            noDataText.setText("No inventory data available");
            noDataText.setTextSize(14);
            noDataText.setTextColor(getResources().getColor(android.R.color.darker_gray));
            noDataText.setGravity(android.view.Gravity.CENTER);
            noDataText.setPadding(0, 20, 0, 20);
            inventoryProgressContainer.addView(noDataText);
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(getActivity());

        // Sort products by inventory quantity (descending)
        List<Map.Entry<String, Integer>> sortedProducts = new ArrayList<>(inventoryMap.entrySet());

        // Use Collections.sort() instead of List.sort() for API level 23 compatibility
        Collections.sort(sortedProducts, new Comparator<Map.Entry<String, Integer>>() {
            @Override
            public int compare(Map.Entry<String, Integer> a, Map.Entry<String, Integer> b) {
                return b.getValue().compareTo(a.getValue());
            }
        });

        for (Map.Entry<String, Integer> entry : sortedProducts) {
            String productName = entry.getKey();
            int inventoryQty = entry.getValue();
            int issuedQty = getIssuedQuantity(issuedMap, productName);

            View progressItem = inflater.inflate(R.layout.progress_bar_item, inventoryProgressContainer, false);

            TextView txtProductName = progressItem.findViewById(R.id.txtProductName);
            TextView txtProductValue = progressItem.findViewById(R.id.txtProductValue);
            ProgressBar progressBar = progressItem.findViewById(R.id.progressBar);
            TextView txtIssued = progressItem.findViewById(R.id.txtIssued);

            // Set product name (truncate if too long)
            String displayName = productName.length() > 20 ?
                    productName.substring(0, 20) + "..." : productName;
            txtProductName.setText(displayName);

            txtProductValue.setText(String.valueOf(inventoryQty));
            txtIssued.setText("Issued: " + issuedQty);

            // Set progress (scale to max inventory)
            if (maxInventory > 0) {
                int progress = (inventoryQty * 100) / maxInventory;
                progressBar.setProgress(progress);

                // Set color based on inventory level
                if (inventoryQty <= 10) {
                    progressBar.getProgressDrawable().setColorFilter(
                            Color.parseColor("#F44336"), android.graphics.PorterDuff.Mode.SRC_IN);
                } else if (inventoryQty <= 50) {
                    progressBar.getProgressDrawable().setColorFilter(
                            Color.parseColor("#FF9800"), android.graphics.PorterDuff.Mode.SRC_IN);
                } else {
                    progressBar.getProgressDrawable().setColorFilter(
                            Color.parseColor("#4CAF50"), android.graphics.PorterDuff.Mode.SRC_IN);
                }
            }

            inventoryProgressContainer.addView(progressItem);
        }
    }

    private int getIssuedQuantity(Map<String, Integer> issuedMap, String productName) {
        Integer quantity = issuedMap.get(productName);
        return quantity != null ? quantity : 0;
    }

    private void updateTopProducts(List<Map.Entry<String, Integer>> topProducts) {
        // Clear existing views
        topProductsContainer.removeAllViews();

        // Check if fragment is still attached
        if (!isAdded() || getActivity() == null) {
            return;
        }

        if (topProducts.isEmpty()) {
            TextView noDataText = new TextView(getActivity()); // Use getActivity() context
            noDataText.setText("No issued goods data");
            noDataText.setTextSize(14);
            noDataText.setTextColor(getResources().getColor(android.R.color.darker_gray));
            noDataText.setGravity(android.view.Gravity.CENTER);
            noDataText.setPadding(0, 20, 0, 20);
            topProductsContainer.addView(noDataText);
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(getActivity());

        for (int i = 0; i < topProducts.size(); i++) {
            Map.Entry<String, Integer> entry = topProducts.get(i);

            View topProductItem = inflater.inflate(R.layout.top_product_item, topProductsContainer, false);

            TextView txtRank = topProductItem.findViewById(R.id.txtRank);
            TextView txtProductName = topProductItem.findViewById(R.id.txtProductName);
            TextView txtProductDetails = topProductItem.findViewById(R.id.txtProductDetails);
            TextView txtQuantity = topProductItem.findViewById(R.id.txtQuantity);

            txtRank.setText(String.valueOf(i + 1));

            // Set different colors for top 3
            if (i == 0) {
                txtRank.setBackgroundColor(Color.parseColor("#FFD700")); // Gold
            } else if (i == 1) {
                txtRank.setBackgroundColor(Color.parseColor("#C0C0C0")); // Silver
            } else if (i == 2) {
                txtRank.setBackgroundColor(Color.parseColor("#CD7F32")); // Bronze
            }

            String displayName = entry.getKey().length() > 20 ?
                    entry.getKey().substring(0, 20) + "..." : entry.getKey();
            txtProductName.setText(displayName);
            txtProductDetails.setText("Issued today");
            txtQuantity.setText(entry.getValue() + " pcs");

            topProductsContainer.addView(topProductItem);
        }
    }

    private void updateDistributionView(Map<String, Integer> inventoryMap,
                                        Map<String, Integer> issuedMap) {
        // Clear existing views
        distributionContainer.removeAllViews();

        // Check if fragment is still attached
        if (!isAdded() || getActivity() == null) {
            return;
        }

        if (inventoryMap.isEmpty() && issuedMap.isEmpty()) {
            TextView noDataText = new TextView(getActivity()); // Use getActivity() context
            noDataText.setText("No distribution data available");
            noDataText.setTextSize(14);
            noDataText.setTextColor(getResources().getColor(android.R.color.darker_gray));
            noDataText.setGravity(android.view.Gravity.CENTER);
            noDataText.setPadding(0, 20, 0, 20);
            distributionContainer.addView(noDataText);
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(getActivity());

        // Get all unique products
        List<String> allProducts = new ArrayList<>();
        allProducts.addAll(inventoryMap.keySet());
        allProducts.addAll(issuedMap.keySet());

        // Remove duplicates
        List<String> uniqueProducts = new ArrayList<>();
        for (String product : allProducts) {
            if (!uniqueProducts.contains(product)) {
                uniqueProducts.add(product);
            }
        }

        // Sort by inventory quantity (descending)
        Collections.sort(uniqueProducts, new Comparator<String>() {
            @Override
            public int compare(String a, String b) {
                int invA = getInventoryQuantity(inventoryMap, a);
                int invB = getInventoryQuantity(inventoryMap, b);
                return Integer.compare(invB, invA);
            }
        });

        for (String product : uniqueProducts) {
            int inventoryQty = getInventoryQuantity(inventoryMap, product);
            int issuedQty = getIssuedQuantity(issuedMap, product);

            if (inventoryQty == 0 && issuedQty == 0) continue;

            View distributionItem = inflater.inflate(R.layout.distribution_item, distributionContainer, false);

            TextView txtProductName = distributionItem.findViewById(R.id.txtProductName);
            TextView txtInventory = distributionItem.findViewById(R.id.txtInventory);
            TextView txtIssued = distributionItem.findViewById(R.id.txtIssued);

            String displayName = product.length() > 20 ?
                    product.substring(0, 20) + "..." : product;
            txtProductName.setText(displayName);
            txtInventory.setText("Inv: " + inventoryQty);
            txtIssued.setText("Iss: " + issuedQty);

            distributionContainer.addView(distributionItem);
        }
    }

    private int getInventoryQuantity(Map<String, Integer> inventoryMap, String productName) {
        Integer quantity = inventoryMap.get(productName);
        return quantity != null ? quantity : 0;
    }

    private String formatDateForDisplay(String dateStr) {
        try {
            Date date = dbDateFormat.parse(dateStr);
            return displayFormat.format(date);
        } catch (Exception e) {
            return dateStr;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        isFragmentActive = true;

        // Refresh data when fragment resumes
        if (selectedDate.isEmpty()) {
            loadTodayAnalytics();
        } else {
            loadAnalyticsByDate(selectedDate);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        isFragmentActive = false;

        // Remove any pending callbacks
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isFragmentActive = false;

        // Clean up database connection
        if (dbHelper != null) {
            dbHelper.close();
            dbHelper = null;
        }

        // Remove any pending callbacks
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
            handler = null;
        }
    }
}