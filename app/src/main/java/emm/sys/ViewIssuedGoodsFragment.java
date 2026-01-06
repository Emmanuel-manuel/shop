package emm.sys;

import android.app.DatePickerDialog;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ViewIssuedGoodsFragment extends Fragment implements IssuedGoodsAdapter.OnItemActionListener {
    private RecyclerView goodsRecyclerView;
    private IssuedGoodsAdapter adapter;
    private DBHelper dbHelper;
    private SearchView searchView;
    private TextView txtTotalIssued, txtTotalQuantity, txtDateRange, txtEmptyMessage;
    private MaterialButton btnDateFilter;
    private View emptyStateView;
    private com.google.android.material.floatingactionbutton.FloatingActionButton fabRefresh;

    private String selectedDate = "";
    private SimpleDateFormat displayFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
    private SimpleDateFormat dbDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_view_issued_goods, container, false);

        // Initialize views
        goodsRecyclerView = view.findViewById(R.id.goodsRecyclerView);
        searchView = view.findViewById(R.id.search);
        txtTotalIssued = view.findViewById(R.id.txtTotalIssued);
        txtTotalQuantity = view.findViewById(R.id.txtTotalQuantity);
        txtDateRange = view.findViewById(R.id.txtDateRange);
        txtEmptyMessage = view.findViewById(R.id.txtEmptyMessage);
        btnDateFilter = view.findViewById(R.id.btnDateFilter);
        emptyStateView = view.findViewById(R.id.emptyState);
        fabRefresh = view.findViewById(R.id.fabRefresh);

        dbHelper = new DBHelper(getActivity());

        // Setup RecyclerView
        goodsRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        adapter = new IssuedGoodsAdapter(new ArrayList<>(), this);
        goodsRecyclerView.setAdapter(adapter);

        // Load today's issued goods by default
        loadTodayIssuedGoods();

        // Setup search functionality
        setupSearchView();

        // Setup date filter button
        btnDateFilter.setOnClickListener(v -> showDatePickerDialog());

        // Setup refresh FAB
        fabRefresh.setOnClickListener(v -> {
            fabRefresh.animate().rotationBy(360).setDuration(500).start();
            if (selectedDate.isEmpty()) {
                loadTodayIssuedGoods();
            } else {
                loadIssuedGoodsByDate(selectedDate);
            }
        });

        return view;
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
                filterIssuedGoods(newText);
                return true;
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
                    loadIssuedGoodsByDate(selectedDate); // This now uses the database method
                },
                year, month, day
        );

        // Set max date to today
        datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        datePickerDialog.show();
    }

    private void loadTodayIssuedGoods() {
        selectedDate = "";
        btnDateFilter.setText("📅 Today");
        // Get today's date and load data
        String todayDate = dbDateFormat.format(new Date());
        loadIssuedGoodsByDate(todayDate);
    }

    private void loadIssuedGoodsByDate(String date) {
        new Thread(() -> {
            Cursor cursor;
            if (date.equals("today")) {
                // Get today's date in yyyy-MM-dd format
                String todayDate = dbDateFormat.format(new Date());
                cursor = dbHelper.getIssuedGoodsByDate(todayDate);
            } else {
                cursor = dbHelper.getIssuedGoodsByDate(date);
            }

            List<IssuedGoodsItem> items = new ArrayList<>();
            int totalItems = 0;
            int totalQuantity = 0;

            if (cursor != null) {
                try {
                    int idIndex = cursor.getColumnIndexOrThrow("id");
                    int assigneeIndex = cursor.getColumnIndexOrThrow("assignee");
                    int productNameIndex = cursor.getColumnIndexOrThrow("product_name");
                    int weightIndex = cursor.getColumnIndexOrThrow("weight");
                    int flavourIndex = cursor.getColumnIndexOrThrow("flavour");
                    int quantityIndex = cursor.getColumnIndexOrThrow("quantity");
                    int stationIndex = cursor.getColumnIndexOrThrow("station");
                    int timestampIndex = cursor.getColumnIndexOrThrow("timestamp");

                    if (cursor.moveToFirst()) {
                        do {
                            IssuedGoodsItem item = new IssuedGoodsItem(
                                    cursor.getInt(idIndex),
                                    cursor.getString(assigneeIndex),
                                    cursor.getString(productNameIndex),
                                    cursor.getString(weightIndex),
                                    cursor.getString(flavourIndex),
                                    cursor.getInt(quantityIndex),
                                    cursor.getString(stationIndex),
                                    cursor.getString(timestampIndex)
                            );
                            items.add(item);
                            totalItems++;
                            totalQuantity += cursor.getInt(quantityIndex);
                        } while (cursor.moveToNext());
                    }
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                    // Log the error for debugging
                    Log.e("DB_ERROR", "Error loading issued goods by date", e);
                } finally {
                    cursor.close();
                }
            }

            final List<IssuedGoodsItem> finalItems = items;
            final int finalTotalItems = totalItems;
            final int finalTotalQuantity = totalQuantity;
            final String displayDate = date.equals("today") ? "Today" : formatDateForDisplay(date);

            new Handler(Looper.getMainLooper()).post(() -> {
                adapter.updateList(finalItems);

                // Update statistics
                txtTotalIssued.setText(String.format(Locale.getDefault(),
                        "Total: %d items", finalTotalItems));
                txtTotalQuantity.setText(String.format(Locale.getDefault(),
                        "Qty: %d pieces", finalTotalQuantity));
                txtDateRange.setText(displayDate);

                // Show/hide empty state
                if (finalItems.isEmpty()) {
                    goodsRecyclerView.setVisibility(View.GONE);
                    emptyStateView.setVisibility(View.VISIBLE);
                    if (date.equals("today")) {
                        txtEmptyMessage.setText("No goods issued today");
                    } else {
                        txtEmptyMessage.setText(String.format("No goods issued on %s", displayDate));
                        Toast.makeText(getActivity(),
                                "No data for selected date", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    goodsRecyclerView.setVisibility(View.VISIBLE);
                    emptyStateView.setVisibility(View.GONE);
                }
            });
        }).start();
    }

    private Cursor filterCursorByDate(Cursor cursor, String targetDate) {
        if (cursor == null) return null;

        List<Object[]> rows = new ArrayList<>();
        SimpleDateFormat dbFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        try {
            int idIndex = cursor.getColumnIndexOrThrow("id");
            int assigneeIndex = cursor.getColumnIndexOrThrow("assignee");
            int productNameIndex = cursor.getColumnIndexOrThrow("product_name");
            int weightIndex = cursor.getColumnIndexOrThrow("weight");
            int flavourIndex = cursor.getColumnIndexOrThrow("flavour");
            int quantityIndex = cursor.getColumnIndexOrThrow("quantity");
            int stationIndex = cursor.getColumnIndexOrThrow("station");
            int timestampIndex = cursor.getColumnIndexOrThrow("timestamp");

            if (cursor.moveToFirst()) {
                do {
                    String timestamp = cursor.getString(timestampIndex);
                    String itemDate = extractDateFromTimestamp(timestamp);

                    if (itemDate.equals(targetDate)) {
                        rows.add(new Object[]{
                                cursor.getInt(idIndex),
                                cursor.getString(assigneeIndex),
                                cursor.getString(productNameIndex),
                                cursor.getString(weightIndex),
                                cursor.getString(flavourIndex),
                                cursor.getInt(quantityIndex),
                                cursor.getString(stationIndex),
                                timestamp
                        });
                    }
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return createMemoryCursor(rows);
    }

    private String extractDateFromTimestamp(String timestamp) {
        try {
            // Assuming timestamp format: "yyyy-MM-dd HH:mm:ss"
            return timestamp.split(" ")[0];
        } catch (Exception e) {
            return "";
        }
    }

    private String formatDateForDisplay(String dateStr) {
        try {
            Date date = dbDateFormat.parse(dateStr);
            return displayFormat.format(date);
        } catch (ParseException e) {
            return dateStr;
        }
    }

    private void filterIssuedGoods(String searchText) {
        new Thread(() -> {
            Cursor cursor;
            String targetDate;

            if (selectedDate.isEmpty()) {
                // Get today's date
                targetDate = dbDateFormat.format(new Date());
            } else {
                targetDate = selectedDate;
            }

            cursor = dbHelper.getIssuedGoodsByDate(targetDate);

            List<IssuedGoodsItem> filteredList = new ArrayList<>();

            if (cursor != null) {
                try {
                    int idIndex = cursor.getColumnIndexOrThrow("id");
                    int assigneeIndex = cursor.getColumnIndexOrThrow("assignee");
                    int productNameIndex = cursor.getColumnIndexOrThrow("product_name");
                    int weightIndex = cursor.getColumnIndexOrThrow("weight");
                    int flavourIndex = cursor.getColumnIndexOrThrow("flavour");
                    int quantityIndex = cursor.getColumnIndexOrThrow("quantity");
                    int stationIndex = cursor.getColumnIndexOrThrow("station");
                    int timestampIndex = cursor.getColumnIndexOrThrow("timestamp");

                    if (cursor.moveToFirst()) {
                        do {
                            String assignee = cursor.getString(assigneeIndex);
                            String productName = cursor.getString(productNameIndex);
                            String station = cursor.getString(stationIndex);
                            String flavour = cursor.getString(flavourIndex);
                            String weight = cursor.getString(weightIndex);

                            // Search in multiple fields
                            if (assignee.toLowerCase().contains(searchText.toLowerCase()) ||
                                    productName.toLowerCase().contains(searchText.toLowerCase()) ||
                                    station.toLowerCase().contains(searchText.toLowerCase()) ||
                                    flavour.toLowerCase().contains(searchText.toLowerCase()) ||
                                    weight.toLowerCase().contains(searchText.toLowerCase())) {

                                filteredList.add(new IssuedGoodsItem(
                                        cursor.getInt(idIndex),
                                        assignee,
                                        productName,
                                        weight,
                                        flavour,
                                        cursor.getInt(quantityIndex),
                                        station,
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

            final List<IssuedGoodsItem> finalFilteredList = filteredList;
            new Handler(Looper.getMainLooper()).post(() -> {
                adapter.updateList(finalFilteredList);

                if (!searchText.isEmpty()) {
                    txtTotalIssued.setText(String.format(Locale.getDefault(),
                            "Found: %d items", finalFilteredList.size()));
                } else {
                    // Reload data without filter
                    if (selectedDate.isEmpty()) {
                        loadTodayIssuedGoods();
                    } else {
                        loadIssuedGoodsByDate(selectedDate);
                    }
                }

                if (finalFilteredList.isEmpty() && !searchText.isEmpty()) {
                    Toast.makeText(getActivity(),
                            "No results found for: \"" + searchText + "\"",
                            Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    // Helper method to create in-memory cursor
    private android.database.MatrixCursor createMemoryCursor(List<Object[]> rows) {
        String[] columns = {"id", "assignee", "product_name", "weight", "flavour",
                "quantity", "station", "timestamp"};
        android.database.MatrixCursor cursor = new android.database.MatrixCursor(columns);

        for (Object[] row : rows) {
            cursor.addRow(row);
        }

        return cursor;
    }

    @Override
    public void onEditClicked(IssuedGoodsItem item) {
        Bundle bundle = new Bundle();
        bundle.putInt("EDIT_MODE", 1);
        bundle.putInt("ISSUED_ID", item.getId());
        bundle.putString("ASSIGNEE", item.getAssignee());
        bundle.putString("PRODUCT_NAME", item.getProductName());
        bundle.putString("WEIGHT", item.getWeight());
        bundle.putString("FLAVOUR", item.getFlavour());
        bundle.putInt("QUANTITY", item.getQuantity());
        bundle.putString("STATION", item.getStation());

        IssueGoodsFragment issueGoodsFragment = new IssueGoodsFragment();
        issueGoodsFragment.setArguments(bundle);

        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainer, issueGoodsFragment)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onDeleteClicked(IssuedGoodsItem item) {
        showDeleteConfirmationDialog(item);
    }

    private void showDeleteConfirmationDialog(IssuedGoodsItem item) {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(getActivity());
        builder.setTitle("Delete Issued Goods");
        builder.setMessage("Are you sure you want to delete this issued goods record?\n\n" +
                "Product: " + item.getProductName() + "\n" +
                "Assignee: " + item.getAssignee() + "\n" +
                "Quantity: " + item.getQuantity() + " pieces");

        builder.setPositiveButton("Delete", (dialog, which) -> {
            deleteIssuedGoods(item);
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> {
            dialog.dismiss();
        });

        builder.show();
    }

    private void deleteIssuedGoods(IssuedGoodsItem item) {
        boolean isDeleted = dbHelper.deleteIssuedGoods(item.getId(), item.getProductName(), item.getQuantity());

        if (isDeleted) {
            Toast.makeText(getActivity(), "Issued goods deleted successfully", Toast.LENGTH_SHORT).show();
            if (selectedDate.isEmpty()) {
                loadTodayIssuedGoods();
            } else {
                loadIssuedGoodsByDate(selectedDate);
            }
        } else {
            Toast.makeText(getActivity(), "Failed to delete issued goods", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (selectedDate.isEmpty()) {
            loadTodayIssuedGoods();
        } else {
            loadIssuedGoodsByDate(selectedDate);
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