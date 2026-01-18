package emm.sys;

import android.app.DatePickerDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class HomeFragment extends Fragment implements DatePicker.OnDateChangedListener {

    private MaterialButton btnDateFilter;
    private TextView txtTotalInventory, txtTotalIssued, txtRemainingBalance, txtDateRange;
    private TextView txtInventoryNote, txtTopProductsNote, txtDistributionNote;
    private LinearLayout inventoryProgressContainer, topProductsContainer, distributionContainer;

    private DBHelper dbHelper;
    private String selectedDate = "";
    private SimpleDateFormat displayFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
    private SimpleDateFormat dbDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    private Handler handler;
    private boolean isFragmentActive = true;

    // Store dates with data for highlighting
    private Set<String> datesWithData = new HashSet<>();
    private HighlightedDatePickerDialog datePickerDialog;

    private void showDateSelectionDialog() {
        if (!isAdded() || getActivity() == null) {
            return;
        }

        // Get all dates with data
        new Thread(() -> {
            if (!isFragmentActive || dbHelper == null) return;

            try {
                Set<String> dates = dbHelper.getAllDatesWithData();
                List<String> dateList = new ArrayList<>(dates);

                // Sort dates in descending order (newest first)
                Collections.sort(dateList, Collections.reverseOrder());

                // Convert to display format
                final List<String> displayDates = new ArrayList<>();
                for (String date : dateList) {
                    try {
                        Date parsedDate = dbDateFormat.parse(date);
                        displayDates.add(displayFormat.format(parsedDate));
                    } catch (Exception e) {
                        displayDates.add(date);
                    }
                }

                // Add "Today" option
                final List<String> finalDateList = dateList;

                if (handler != null && isFragmentActive) {
                    handler.post(() -> {
                        if (!isAdded() || getActivity() == null) return;

                        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                        builder.setTitle("📅 Select Date with Data");

                        // Add "Today" option at the top
                        final List<String> options = new ArrayList<>();
                        options.add("📊 Today");
                        options.addAll(displayDates);

                        builder.setItems(options.toArray(new String[0]), (dialog, which) -> {
                            if (which == 0) {
                                // Today selected
                                loadTodayAnalytics();
                            } else {
                                // Other date selected
                                int dataIndex = which - 1;
                                if (dataIndex < finalDateList.size()) {
                                    String selectedDbDate = finalDateList.get(dataIndex);
                                    String selectedDisplayDate = options.get(which);

                                    selectedDate = selectedDbDate;
                                    btnDateFilter.setText("📅 " + selectedDisplayDate + " 📊");
                                    loadAnalyticsByDate(selectedDate);
                                }
                            }
                        });

                        builder.setNegativeButton("Cancel", null);

                        AlertDialog dialog = builder.create();
                        dialog.show();
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        initializeViews(view);
        handler = new Handler(Looper.getMainLooper());

        if (dbHelper == null) {
            dbHelper = new DBHelper(getActivity());
        }

        // Load dates with data in background
        loadDatesWithData();

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

    private void loadDatesWithData() {
        new Thread(() -> {
            if (!isFragmentActive || dbHelper == null) return;

            try {
                datesWithData = dbHelper.getAllDatesWithData();

                // Log for debugging
                if (!datesWithData.isEmpty()) {
                    System.out.println("Dates with data: " + datesWithData.size());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void showDatePickerDialog() {
        if (!isAdded() || getActivity() == null) {
            return;
        }

        final Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        // Create custom date picker dialog
        datePickerDialog = new HighlightedDatePickerDialog(getActivity(),
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    Calendar selectedCalendar = Calendar.getInstance();
                    selectedCalendar.set(selectedYear, selectedMonth, selectedDay);

                    selectedDate = dbDateFormat.format(selectedCalendar.getTime());
                    String displayDate = displayFormat.format(selectedCalendar.getTime());

                    btnDateFilter.setText("📅 " + displayDate);
                    loadAnalyticsByDate(selectedDate);

                    // Update the button text to show it's selected
                    updateButtonForSelectedDate(displayDate, selectedDate);
                },
                year, month, day);

        // Set max date to today
        datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());

        // Set date changed listener to update highlights when month changes
        datePickerDialog.getDatePicker().init(year, month, day, this);

        // Load and set highlighted days for current month
        updateHighlightedDays(year, month);

        // Customize dialog appearance
        customizeDatePickerDialog();

        datePickerDialog.show();
    }

    private void updateButtonForSelectedDate(String displayDate, String dbDate) {
        // Check if the selected date has data
        boolean hasData = datesWithData.contains(dbDate);

        if (hasData) {
            btnDateFilter.setText("📅 " + displayDate + " 📊");
            btnDateFilter.setBackgroundColor(Color.parseColor("#E8F5E9")); // Light green
        } else {
            btnDateFilter.setText("📅 " + displayDate);
            btnDateFilter.setBackgroundColor(Color.TRANSPARENT);
        }
    }

    private void customizeDatePickerDialog() {
        try {
            // Set dialog title
            datePickerDialog.setTitle("📅 Select Date with Data");

            // Customize positive button
            datePickerDialog.getButton(DatePickerDialog.BUTTON_POSITIVE).setText("Select");
            datePickerDialog.getButton(DatePickerDialog.BUTTON_NEGATIVE).setText("Cancel");

            // Add info text about highlighted dates
            TextView infoText = new TextView(getActivity());
            infoText.setText("• Green dates have data\n• Today's date is auto-selected");
            infoText.setTextSize(12);
            infoText.setTextColor(Color.parseColor("#757575"));
            infoText.setPadding(24, 16, 24, 8);

            // Get the dialog's main layout and add info text
            ViewGroup dialogLayout = (ViewGroup) datePickerDialog.findViewById(android.R.id.content);
            if (dialogLayout != null && dialogLayout.getChildAt(0) instanceof ViewGroup) {
                ViewGroup contentLayout = (ViewGroup) dialogLayout.getChildAt(0);
                contentLayout.addView(infoText, 0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDateChanged(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
        // Update highlighted days when month changes
        updateHighlightedDays(year, monthOfYear);
    }

    private void updateHighlightedDays(int year, int month) {
        new Thread(() -> {
            if (!isFragmentActive || dbHelper == null || datePickerDialog == null) return;

            try {
                Set<Integer> highlightedDays = dbHelper.getHighlightedDaysForMonth(year, month);

                // Update UI on main thread
                if (handler != null && isFragmentActive) {
                    handler.post(() -> {
                        if (datePickerDialog != null) {
                            datePickerDialog.setHighlightedDays(highlightedDays);
                        }
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void loadTodayAnalytics() {
        selectedDate = "";
        String todayDate = dbDateFormat.format(new Date());
        String displayDate = "Today";

        // Check if today has data
        boolean todayHasData = datesWithData.contains(todayDate);
        if (todayHasData) {
            btnDateFilter.setText("📅 " + displayDate + " 📊");
        } else {
            btnDateFilter.setText("📅 " + displayDate);
        }

        loadAnalyticsByDate(todayDate);
    }

    private void loadAnalyticsByDate(String date) {
        if (!isAdded() || getActivity() == null) {
            return;
        }

        new Thread(() -> {
            try {
                if (!isFragmentActive) {
                    return;
                }

                Map<String, Integer> inventoryMap = getInventoryByDate(date);
                Map<String, Integer> issuedMap = getIssuedGoodsByDate(date);

                int totalInventory = 0;
                int totalIssued = 0;

                for (Map.Entry<String, Integer> entry : inventoryMap.entrySet()) {
                    totalInventory += entry.getValue();
                }

                for (Map.Entry<String, Integer> entry : issuedMap.entrySet()) {
                    totalIssued += entry.getValue();
                }

                int remainingBalance = totalInventory - totalIssued;
                List<Map.Entry<String, Integer>> topProducts = getTopProducts(issuedMap, 5);

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
                final String displayDate = date.equals(dbDateFormat.format(new Date())) ?
                        "Today" : formatDateForDisplay(date);

                if (handler != null && isFragmentActive) {
                    handler.post(() -> {
                        if (!isAdded() || getActivity() == null) {
                            return;
                        }

                        txtTotalInventory.setText(String.valueOf(finalTotalInventory));
                        txtTotalIssued.setText(String.valueOf(finalTotalIssued));
                        txtRemainingBalance.setText(String.valueOf(finalRemainingBalance));

                        // Update date range text with data indicator
                        String dateText = displayDate + " Analytics";
                        if (!finalInventoryMap.isEmpty() || !finalIssuedMap.isEmpty()) {
                            dateText += " 📊";
                        }
                        txtDateRange.setText(dateText);

                        updateInventoryProgress(finalInventoryMap, finalIssuedMap, finalMaxInventory);
                        updateTopProducts(finalTopProducts);
                        updateDistributionView(finalInventoryMap, finalIssuedMap);

                        txtInventoryNote.setText(finalInventoryMap.size() + " products in inventory");
                        txtTopProductsNote.setText("Top " + Math.min(5, finalTopProducts.size()) + " issued products");
                        txtDistributionNote.setText("Comparing " + finalInventoryMap.size() + " products");

                        if (finalInventoryMap.isEmpty() && finalIssuedMap.isEmpty()) {
                            Toast.makeText(getActivity(),
                                    "No data for selected date", Toast.LENGTH_SHORT).show();
                        }
                    });
                }

            } catch (Exception e) {
                e.printStackTrace();
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
            android.database.Cursor cursor = dbHelper.getReadableDatabase().rawQuery(
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
            android.database.Cursor cursor = dbHelper.getReadableDatabase().rawQuery(
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
        inventoryProgressContainer.removeAllViews();

        if (!isAdded() || getActivity() == null) {
            return;
        }

        if (inventoryMap.isEmpty()) {
            TextView noDataText = new TextView(getActivity());
            noDataText.setText("No inventory data available");
            noDataText.setTextSize(14);
            noDataText.setTextColor(getResources().getColor(android.R.color.darker_gray));
            noDataText.setGravity(android.view.Gravity.CENTER);
            noDataText.setPadding(0, 20, 0, 20);
            inventoryProgressContainer.addView(noDataText);
            return;
        }

        android.view.LayoutInflater inflater = android.view.LayoutInflater.from(getActivity());

        List<Map.Entry<String, Integer>> sortedProducts = new ArrayList<>(inventoryMap.entrySet());

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

            String displayName = productName.length() > 20 ?
                    productName.substring(0, 20) + "..." : productName;
            txtProductName.setText(displayName);

            txtProductValue.setText(String.valueOf(inventoryQty));
            txtIssued.setText("Issued: " + issuedQty);

            if (maxInventory > 0) {
                int progress = (inventoryQty * 100) / maxInventory;
                progressBar.setProgress(progress);

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
        topProductsContainer.removeAllViews();

        if (!isAdded() || getActivity() == null) {
            return;
        }

        if (topProducts.isEmpty()) {
            TextView noDataText = new TextView(getActivity());
            noDataText.setText("No issued goods data");
            noDataText.setTextSize(14);
            noDataText.setTextColor(getResources().getColor(android.R.color.darker_gray));
            noDataText.setGravity(android.view.Gravity.CENTER);
            noDataText.setPadding(0, 20, 0, 20);
            topProductsContainer.addView(noDataText);
            return;
        }

        android.view.LayoutInflater inflater = android.view.LayoutInflater.from(getActivity());

        for (int i = 0; i < topProducts.size(); i++) {
            Map.Entry<String, Integer> entry = topProducts.get(i);

            View topProductItem = inflater.inflate(R.layout.top_product_item, topProductsContainer, false);

            TextView txtRank = topProductItem.findViewById(R.id.txtRank);
            TextView txtProductName = topProductItem.findViewById(R.id.txtProductName);
            TextView txtProductDetails = topProductItem.findViewById(R.id.txtProductDetails);
            TextView txtQuantity = topProductItem.findViewById(R.id.txtQuantity);

            txtRank.setText(String.valueOf(i + 1));

            if (i == 0) {
                txtRank.setBackgroundColor(Color.parseColor("#FFD700"));
            } else if (i == 1) {
                txtRank.setBackgroundColor(Color.parseColor("#C0C0C0"));
            } else if (i == 2) {
                txtRank.setBackgroundColor(Color.parseColor("#CD7F32"));
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
        distributionContainer.removeAllViews();

        if (!isAdded() || getActivity() == null) {
            return;
        }

        if (inventoryMap.isEmpty() && issuedMap.isEmpty()) {
            TextView noDataText = new TextView(getActivity());
            noDataText.setText("No distribution data available");
            noDataText.setTextSize(14);
            noDataText.setTextColor(getResources().getColor(android.R.color.darker_gray));
            noDataText.setGravity(android.view.Gravity.CENTER);
            noDataText.setPadding(0, 20, 0, 20);
            distributionContainer.addView(noDataText);
            return;
        }

        android.view.LayoutInflater inflater = android.view.LayoutInflater.from(getActivity());

        List<String> allProducts = new ArrayList<>();
        allProducts.addAll(inventoryMap.keySet());
        allProducts.addAll(issuedMap.keySet());

        List<String> uniqueProducts = new ArrayList<>();
        for (String product : allProducts) {
            if (!uniqueProducts.contains(product)) {
                uniqueProducts.add(product);
            }
        }

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

        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }

        // Dismiss date picker if shown
        if (datePickerDialog != null && datePickerDialog.isShowing()) {
            datePickerDialog.dismiss();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isFragmentActive = false;

        if (dbHelper != null) {
            dbHelper.close();
            dbHelper = null;
        }

        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
            handler = null;
        }
    }
}