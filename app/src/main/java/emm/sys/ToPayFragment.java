package emm.sys;

import android.app.DatePickerDialog;
import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class ToPayFragment extends Fragment {

    // UI Components
    private MaterialButton btnDateFilter;
    private TextView txtTotalPending, txtCustomerCount;
    private TextView txtResultCount;
    private MaterialButton btnClearSearch;
    private LinearLayout emptyState;
    private CardView bulkActionsBar;
    private TextView txtSelectedCount;
    private MaterialButton btnMarkAsPaid, btnCancelSelection;

    // Search
    private TextInputEditText editSearch;

    // RecyclerView
    private RecyclerView toPayRecyclerView;
    private ToPayAdapter toPayAdapter;
    private List<ToPayItem> allToPayItems = new ArrayList<>();
    private List<ToPayItem> filteredToPayItems = new ArrayList<>();

    // Database
    private DBHelper dbHelper;

    // Date handling
    private String selectedDate = "";
    private SimpleDateFormat displayFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
    private SimpleDateFormat dbDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private NumberFormat formatter = NumberFormat.getInstance();

    private Handler handler = new Handler(Looper.getMainLooper());
    private boolean isFragmentActive = true;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_to_pay, container, false);

        initializeViews(view);

        dbHelper = new DBHelper(getActivity());

        // Load today's data
        selectedDate = dbDateFormat.format(new Date());
        btnDateFilter.setText("📅 Today");
        loadToPayData(selectedDate);

        setupListeners();

        return view;
    }

    private void initializeViews(View view) {
        btnDateFilter = view.findViewById(R.id.btnDateFilter);
        txtTotalPending = view.findViewById(R.id.txtTotalPending);
        txtCustomerCount = view.findViewById(R.id.txtCustomerCount);

        editSearch = view.findViewById(R.id.editSearch);
        txtResultCount = view.findViewById(R.id.txtResultCount);
        btnClearSearch = view.findViewById(R.id.btnClearSearch);
        emptyState = view.findViewById(R.id.emptyState);

        bulkActionsBar = view.findViewById(R.id.bulkActionsBar);
        txtSelectedCount = view.findViewById(R.id.txtSelectedCount);
        btnMarkAsPaid = view.findViewById(R.id.btnMarkAsPaid);
        btnCancelSelection = view.findViewById(R.id.btnCancelSelection);

        toPayRecyclerView = view.findViewById(R.id.toPayRecyclerView);
        toPayRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        toPayAdapter = new ToPayAdapter(filteredToPayItems);
        toPayRecyclerView.setAdapter(toPayAdapter);
    }

    private void setupListeners() {
        btnDateFilter.setOnClickListener(v -> showDatePickerDialog());

        editSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterToPayItems(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnClearSearch.setOnClickListener(v -> {
            editSearch.setText("");
            btnClearSearch.setVisibility(View.GONE);
        });

        toPayAdapter.setOnItemSelectedListener(new ToPayAdapter.OnItemSelectedListener() {
            @Override
            public void onItemSelected(ToPayItem item, boolean isSelected) {
                updateSelectionUI();
            }

            @Override
            public void onSelectionModeChanged(boolean isActive) {
                if (isActive) {
                    bulkActionsBar.setVisibility(View.VISIBLE);
                }
                updateSelectionUI();
            }
        });

        btnMarkAsPaid.setOnClickListener(v -> showMarkAsPaidConfirmation());

        btnCancelSelection.setOnClickListener(v -> {
            toPayAdapter.setSelectionMode(false);
            bulkActionsBar.setVisibility(View.GONE);
        });
    }

    private void showDatePickerDialog() {
        Calendar calendar = Calendar.getInstance();
        try {
            if (!selectedDate.isEmpty()) {
                calendar.setTime(dbDateFormat.parse(selectedDate));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                getActivity(),
                (view, year, month, dayOfMonth) -> {
                    Calendar selectedCal = Calendar.getInstance();
                    selectedCal.set(year, month, dayOfMonth);
                    selectedDate = dbDateFormat.format(selectedCal.getTime());
                    String displayDate = displayFormat.format(selectedCal.getTime());
                    btnDateFilter.setText("📅 " + displayDate);
                    loadToPayData(selectedDate);

                    // Exit selection mode when date changes
                    toPayAdapter.setSelectionMode(false);
                    bulkActionsBar.setVisibility(View.GONE);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        datePickerDialog.show();
    }

    private void loadToPayData(String date) {
        new Thread(() -> {
            if (!isFragmentActive || dbHelper == null) return;

            try {
                List<ToPayItem> toPayItems = getToPayByDate(date);

                allToPayItems.clear();
                allToPayItems.addAll(toPayItems);

                // Sort by timestamp (newest first)
                Collections.sort(allToPayItems, (a, b) -> b.getTimestamp().compareTo(a.getTimestamp()));

                int totalBill = 0;
                int totalBalance = 0;
                Set<String> uniqueCustomers = new HashSet<>();

                for (ToPayItem item : toPayItems) {
                    totalBill += item.getBill(); // Sum of Bill column
                    totalBalance += item.getBalance();
                    uniqueCustomers.add(item.getCustName());
                }

                final int finalTotalBill = totalBill;
                final int finalCustomerCount = uniqueCustomers.size();

                if (handler != null && isFragmentActive) {
                    handler.post(() -> {
                        if (!isAdded()) return;

                        // Update summary with total Bill
                        txtTotalPending.setText("KES " + formatter.format(finalTotalBill));
                        txtCustomerCount.setText(String.valueOf(finalCustomerCount));

                        // Update list
                        filterToPayItems(editSearch.getText().toString());
                    });
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private List<ToPayItem> getToPayByDate(String date) {
        List<ToPayItem> toPayList = new ArrayList<>();

        try {
            android.database.Cursor cursor = dbHelper.getReadableDatabase().rawQuery(
                    "SELECT id, market, cust_name, product_name, selling_price, quantity, bill, payment_mode, total_bill, balance, timestamp " +
                            "FROM to_pay WHERE date(timestamp) = ? ORDER BY timestamp DESC",
                    new String[]{date}
            );

            if (cursor != null) {
                while (cursor.moveToNext()) {
                    ToPayItem item = new ToPayItem(
                            cursor.getInt(0),
                            cursor.getString(1),
                            cursor.getString(2),
                            cursor.getString(3),
                            cursor.getInt(4),
                            cursor.getInt(5),
                            cursor.getInt(6),
                            cursor.getString(7),
                            cursor.getInt(8),
                            cursor.getInt(9),
                            cursor.getString(10)
                    );
                    toPayList.add(item);
                }
                cursor.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return toPayList;
    }

    private void filterToPayItems(String query) {
        filteredToPayItems.clear();

        if (query.isEmpty()) {
            filteredToPayItems.addAll(allToPayItems);
            btnClearSearch.setVisibility(View.GONE);
        } else {
            String lowerQuery = query.toLowerCase(Locale.getDefault());

            for (ToPayItem item : allToPayItems) {
                if (item.getCustName().toLowerCase().contains(lowerQuery) ||
                        item.getMarket().toLowerCase().contains(lowerQuery) ||
                        item.getProductName().toLowerCase().contains(lowerQuery) ||
                        item.getPaymentMode().toLowerCase().contains(lowerQuery)) {
                    filteredToPayItems.add(item);
                }
            }
            btnClearSearch.setVisibility(View.VISIBLE);
        }

        // Calculate total bill from filtered items
        int filteredTotalBill = 0;
        for (ToPayItem item : filteredToPayItems) {
            filteredTotalBill += item.getBill();
        }

        // Update result count and total pending
        txtResultCount.setText(filteredToPayItems.size() + " pending payments");
        txtTotalPending.setText("KES " + formatter.format(filteredTotalBill));

        // Show/hide empty state
        if (filteredToPayItems.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
            toPayRecyclerView.setVisibility(View.GONE);
        } else {
            emptyState.setVisibility(View.GONE);
            toPayRecyclerView.setVisibility(View.VISIBLE);
        }

        toPayAdapter.updateList(filteredToPayItems);
    }

    private void updateSelectionUI() {
        List<ToPayItem> selectedItems = toPayAdapter.getSelectedItems();
        int count = selectedItems.size();

        if (count > 0) {
            // Calculate total bill of selected items
            int selectedTotalBill = 0;
            for (ToPayItem item : selectedItems) {
                selectedTotalBill += item.getBill();
            }
            txtSelectedCount.setText(count + " item(s) selected (KES " + formatter.format(selectedTotalBill) + ")");
            bulkActionsBar.setVisibility(View.VISIBLE);
        } else {
            bulkActionsBar.setVisibility(View.GONE);
            toPayAdapter.setSelectionMode(false);
        }
    }

    private void showMarkAsPaidConfirmation() {
        List<ToPayItem> selectedItems = toPayAdapter.getSelectedItems();

        if (selectedItems.isEmpty()) {
            Toast.makeText(getActivity(), "No items selected", Toast.LENGTH_SHORT).show();
            return;
        }

        int totalBill = 0;
        int totalBalance = 0;
        Set<String> customers = new HashSet<>();

        for (ToPayItem item : selectedItems) {
            totalBill += item.getBill();
            totalBalance += item.getBalance();
            customers.add(item.getCustName());
        }

        String message = "Mark " + selectedItems.size() + " item(s) as paid?\n" +
                "Total Bill: KES " + formatter.format(totalBill) + "\n" +
                "Outstanding Balance: KES " + formatter.format(totalBalance) + "\n" +
                "Customers: " + customers.size();

        new AlertDialog.Builder(getActivity())
                .setTitle("Confirm Payment")
                .setMessage(message)
                .setPositiveButton("Yes, Mark as Paid", (dialog, which) -> markSelectedAsPaid())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void markSelectedAsPaid() {
        List<ToPayItem> selectedItems = toPayAdapter.getSelectedItems();

        if (selectedItems.isEmpty()) {
            return;
        }

        new Thread(() -> {
            if (!isFragmentActive || dbHelper == null) return;

            SQLiteDatabase db = dbHelper.getWritableDatabase();
            db.beginTransaction();

            try {
                boolean allSuccess = true;

                for (ToPayItem item : selectedItems) {
                    // Insert into sales table
                    ContentValues salesValues = new ContentValues();
                    salesValues.put("market", item.getMarket());
                    salesValues.put("cust_name", item.getCustName());
                    salesValues.put("product_name", item.getProductName());
                    salesValues.put("selling_price", item.getSellingPrice());
                    salesValues.put("quantity", item.getQuantity());
                    salesValues.put("bill", item.getBill());
                    salesValues.put("payment_mode", "Cash"); // Mark as paid with Cash
                    salesValues.put("total_bill", item.getTotalBill());
                    salesValues.put("timestamp", item.getTimestamp());

                    long salesResult = db.insert("sales", null, salesValues);

                    if (salesResult != -1) {
                        // Delete from to_pay table
                        int deleteResult = db.delete("to_pay", "id = ?", new String[]{String.valueOf(item.getId())});

                        if (deleteResult <= 0) {
                            allSuccess = false;
                            break;
                        }
                    } else {
                        allSuccess = false;
                        break;
                    }
                }

                if (allSuccess) {
                    db.setTransactionSuccessful();

                    if (handler != null && isFragmentActive) {
                        handler.post(() -> {
                            Toast.makeText(getActivity(),
                                    selectedItems.size() + " item(s) marked as paid successfully",
                                    Toast.LENGTH_LONG).show();

                            // Exit selection mode
                            toPayAdapter.setSelectionMode(false);
                            bulkActionsBar.setVisibility(View.GONE);

                            // Reload data
                            loadToPayData(selectedDate);
                        });
                    }
                } else {
                    if (handler != null && isFragmentActive) {
                        handler.post(() ->
                                Toast.makeText(getActivity(), "Error marking items as paid", Toast.LENGTH_SHORT).show()
                        );
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                db.endTransaction();
            }
        }).start();
    }

    @Override
    public void onResume() {
        super.onResume();
        isFragmentActive = true;
        if (!selectedDate.isEmpty()) {
            loadToPayData(selectedDate);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        isFragmentActive = false;
        handler.removeCallbacksAndMessages(null);

        // Exit selection mode
        if (toPayAdapter != null) {
            toPayAdapter.setSelectionMode(false);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isFragmentActive = false;
        if (dbHelper != null) {
            dbHelper.close();
        }
    }
}