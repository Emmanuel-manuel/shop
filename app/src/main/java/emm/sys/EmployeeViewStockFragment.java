package emm.sys;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.database.Cursor;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class EmployeeViewStockFragment extends Fragment implements EmployeeReceivedGoodsAdapter.OnItemClickListener {

    private RecyclerView recyclerView;
    private EmployeeReceivedGoodsAdapter adapter;
    private DBHelper dbHelper;
    private TextView txtTotalItems, txtLastUpdated;
    private MaterialButton btnDateFilter;
    private SearchView searchView;
    private View emptyStateView;
    private FloatingActionButton fabRefresh;

    private String assigneeEmail;
    private String selectedDate = "";
    private SimpleDateFormat displayFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
    private SimpleDateFormat dbDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_employee_view_stock, container, false);

        // Initialize views
        recyclerView = view.findViewById(R.id.inventoryRecyclerView);
        txtTotalItems = view.findViewById(R.id.txtTotalItems);
        txtLastUpdated = view.findViewById(R.id.txtLastUpdated);
        btnDateFilter = view.findViewById(R.id.btnDateFilter);
        searchView = view.findViewById(R.id.search);
        emptyStateView = view.findViewById(R.id.emptyState);
        fabRefresh = view.findViewById(R.id.fabRefresh);

        dbHelper = new DBHelper(getActivity());
        assigneeEmail = LandingPageEmployeeActivity.getCurrentUserEmail();

        setupRecyclerView();
        setupSearchView();
        setupDateFilter();
        setupRefreshButton();
        loadReceivedGoods();

        return view;
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        adapter = new EmployeeReceivedGoodsAdapter(new ArrayList<>());
        adapter.setOnItemClickListener(this);
        recyclerView.setAdapter(adapter);
    }

    private void setupSearchView() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                searchView.clearFocus();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterItems(newText);
                return true;
            }
        });
    }

    private void setupDateFilter() {
        btnDateFilter.setOnClickListener(v -> showDatePickerDialog());
    }

    private void setupRefreshButton() {
        fabRefresh.setOnClickListener(v -> {
            fabRefresh.animate().rotationBy(360).setDuration(500).start();
            if (selectedDate.isEmpty()) {
                loadReceivedGoods();
            } else {
                loadReceivedGoodsByDate(selectedDate);
            }
        });
    }

    private void showDatePickerDialog() {
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
                    loadReceivedGoodsByDate(selectedDate);
                },
                year, month, day
        );

        datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        datePickerDialog.show();
    }

    private void loadReceivedGoods() {
        selectedDate = "";
        btnDateFilter.setText("📅 Today");
        String todayDate = dbDateFormat.format(new Date());
        loadReceivedGoodsByDate(todayDate);
    }

    private void loadReceivedGoodsByDate(String date) {
        if (TextUtils.isEmpty(assigneeEmail)) {
            Toast.makeText(getActivity(), "User email not found. Please login again.", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            Cursor cursor = dbHelper.getEmployeeReceivedGoodsByAssigneeAndDate(assigneeEmail, date);
            List<EmployeeReceivedGoodsItem> items = new ArrayList<>();
            int totalQuantity = 0;

            if (cursor != null && cursor.moveToFirst()) {
                try {
                    int idIndex = cursor.getColumnIndexOrThrow("id");
                    int productNameIndex = cursor.getColumnIndexOrThrow("product_name");
                    int weightIndex = cursor.getColumnIndexOrThrow("weight");
                    int flavourIndex = cursor.getColumnIndexOrThrow("flavour");
                    int quantityIndex = cursor.getColumnIndexOrThrow("quantity");
                    int stationIndex = cursor.getColumnIndexOrThrow("station");
                    int timestampIndex = cursor.getColumnIndexOrThrow("timestamp");

                    do {
                        int quantity = cursor.getInt(quantityIndex);
                        EmployeeReceivedGoodsItem item = new EmployeeReceivedGoodsItem(
                                cursor.getInt(idIndex),
                                cursor.getString(productNameIndex),
                                cursor.getString(weightIndex),
                                cursor.getString(flavourIndex),
                                quantity,
                                cursor.getString(stationIndex),
                                cursor.getString(timestampIndex)
                        );
                        items.add(item);
                        totalQuantity += quantity;

                    } while (cursor.moveToNext());
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    cursor.close();
                }
            }

            final List<EmployeeReceivedGoodsItem> finalItems = items;
            final int finalTotalQuantity = totalQuantity;
            final String displayDate = date.equals(dbDateFormat.format(new Date())) ? "Today" : formatDateForDisplay(date);

            requireActivity().runOnUiThread(() -> {
                if (adapter != null) {
                    adapter.updateList(finalItems);
                }

                if (txtTotalItems != null) {
                    txtTotalItems.setText("Total: " + finalTotalQuantity + " pieces");
                }

                if (txtLastUpdated != null) {
                    txtLastUpdated.setText("Updated: " + displayDate);
                }

                if (finalItems.isEmpty()) {
                    if (recyclerView != null) {
                        recyclerView.setVisibility(View.GONE);
                    }
                    if (emptyStateView != null) {
                        emptyStateView.setVisibility(View.VISIBLE);
                        // Safely update empty message
                        TextView emptyMessage = emptyStateView.findViewById(R.id.txtEmptyMessage);
                        if (emptyMessage != null) {
                            if (date.equals(dbDateFormat.format(new Date()))) {
                                emptyMessage.setText("No goods received today");
                            } else {
                                emptyMessage.setText("No goods received on " + displayDate);
                            }
                        }
                    }
                } else {
                    if (recyclerView != null) {
                        recyclerView.setVisibility(View.VISIBLE);
                    }
                    if (emptyStateView != null) {
                        emptyStateView.setVisibility(View.GONE);
                    }
                }
            });
        }).start();
    }

    private void filterItems(String searchText) {
        if (TextUtils.isEmpty(searchText)) {
            if (selectedDate.isEmpty()) {
                loadReceivedGoods();
            } else {
                loadReceivedGoodsByDate(selectedDate);
            }
            return;
        }

        List<EmployeeReceivedGoodsItem> currentList = adapter.getCurrentList();
        List<EmployeeReceivedGoodsItem> filteredList = new ArrayList<>();

        for (EmployeeReceivedGoodsItem item : currentList) {
            if (item.getProductName().toLowerCase().contains(searchText.toLowerCase()) ||
                    item.getWeight().toLowerCase().contains(searchText.toLowerCase()) ||
                    item.getFlavour().toLowerCase().contains(searchText.toLowerCase()) ||
                    item.getStation().toLowerCase().contains(searchText.toLowerCase())) {
                filteredList.add(item);
            }
        }

        adapter.updateList(filteredList);
        txtTotalItems.setText("Found: " + filteredList.size() + " items");

        if (filteredList.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyStateView.setVisibility(View.VISIBLE);
            // Update the empty message
            TextView emptyMessage = emptyStateView.findViewById(R.id.txtEmptyMessage);
            if (emptyMessage != null) {
                emptyMessage.setText("No results found for: \"" + searchText + "\"");
            }
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyStateView.setVisibility(View.GONE);
        }
    }

    private String formatDateForDisplay(String dateStr) {
        try {
            Date date = dbDateFormat.parse(dateStr);
            return displayFormat.format(date);
        } catch (Exception e) {
            return dateStr;
        }
    }

    // ---------------------------------------------------------------
    // OnItemClickListener implementation
    // ---------------------------------------------------------------
    @Override
    public void onItemClick(EmployeeReceivedGoodsItem item) {
        // Show item details in a dialog
        String details = "Product: " + item.getProductName() + "\n" +
                "Weight: " + item.getWeight() + "\n" +
                "Flavour: " + item.getFlavour() + "\n" +
                "Quantity: " + item.getQuantity() + " pieces\n" +
                "Station: " + item.getStation() + "\n" +
                "Received: " + formatTimestamp(item.getTimestamp());

        new AlertDialog.Builder(getActivity())
                .setTitle("Stock Details")
                .setMessage(details)
                .setPositiveButton("OK", null)
                .show();
    }

    @Override
    public void onDeleteClick(EmployeeReceivedGoodsItem item) {
        new AlertDialog.Builder(getActivity())
                .setTitle("Delete Record")
                .setMessage("Are you sure you want to delete this record?\n\n" +
                        "Product: " + item.getProductName() + "\n" +
                        "Quantity: " + item.getQuantity() + " pieces")
                .setPositiveButton("Delete", (dialog, which) -> deleteItem(item))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteItem(EmployeeReceivedGoodsItem item) {
        boolean isDeleted = dbHelper.deleteEmployeeReceivedGoods(item.getId());

        if (isDeleted) {
            Toast.makeText(getActivity(), "Record deleted successfully", Toast.LENGTH_SHORT).show();
            if (selectedDate.isEmpty()) {
                loadReceivedGoods();
            } else {
                loadReceivedGoodsByDate(selectedDate);
            }
        } else {
            Toast.makeText(getActivity(), "Failed to delete record", Toast.LENGTH_SHORT).show();
        }
    }

    private String formatTimestamp(String timestamp) {
        if (TextUtils.isEmpty(timestamp)) return "";
        try {
            SimpleDateFormat in = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            SimpleDateFormat out = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
            return out.format(in.parse(timestamp));
        } catch (Exception e) {
            return timestamp;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (selectedDate.isEmpty()) {
            loadReceivedGoods();
        } else {
            loadReceivedGoodsByDate(selectedDate);
        }
    }

    @Override
    public void onDestroy() {
        if (dbHelper != null) {
            dbHelper.close();
        }
        super.onDestroy();
    }
}