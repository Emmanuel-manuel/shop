package emm.sys;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
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

import java.io.IOException;
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
    private FloatingActionButton fabRefresh, fabQuickActions;
    private TextView serverStatusText;

    private String selectedDate = "";
    private SimpleDateFormat displayFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
    private SimpleDateFormat dbDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    // Share server
    private ProductShareServer shareServer;
    private boolean isDeviceConnected = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_view_issued_goods, container, false);

        initializeViews(view);
        setupRecyclerView();
        setupSearchView();
        setupDateFilter();
        setupFABs();
        loadTodayIssuedGoods();
        startShareServer();

        return view;
    }

    private void initializeViews(View view) {
        goodsRecyclerView = view.findViewById(R.id.goodsRecyclerView);
        searchView = view.findViewById(R.id.search);
        txtTotalIssued = view.findViewById(R.id.txtTotalIssued);
        txtTotalQuantity = view.findViewById(R.id.txtTotalQuantity);
        txtDateRange = view.findViewById(R.id.txtDateRange);
        txtEmptyMessage = view.findViewById(R.id.txtEmptyMessage);
        btnDateFilter = view.findViewById(R.id.btnDateFilter);
        emptyStateView = view.findViewById(R.id.emptyState);
        fabRefresh = view.findViewById(R.id.fabRefresh);
        fabQuickActions = view.findViewById(R.id.fabQuickActions);
        serverStatusText = view.findViewById(R.id.serverStatusText);

        dbHelper = new DBHelper(getActivity());
    }

    private void setupRecyclerView() {
        goodsRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        adapter = new IssuedGoodsAdapter(new ArrayList<>(), this);
        goodsRecyclerView.setAdapter(adapter);
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

    private void setupDateFilter() {
        btnDateFilter.setOnClickListener(v -> showDatePickerDialog());
    }

    private void setupFABs() {
        fabRefresh.setOnClickListener(v -> {
            fabRefresh.animate().rotationBy(360).setDuration(500).start();
            if (selectedDate.isEmpty()) {
                loadTodayIssuedGoods();
            } else {
                loadIssuedGoodsByDate(selectedDate);
            }
        });

        fabQuickActions.setOnClickListener(v -> showQuickActionsMenu());
    }

    // ---------------------------------------------------------------
    // Share Server
    // ---------------------------------------------------------------
    private void startShareServer() {
        if (shareServer != null) {
            updateServerStatusIndicator(isDeviceConnected);
            return;
        }

        try {
            shareServer = new ProductShareServer(requireContext());

            shareServer.setConnectionListener(new ProductShareServer.ConnectionListener() {
                @Override
                public void onDeviceConnected() {
                    isDeviceConnected = true;
                    requireActivity().runOnUiThread(() -> {
                        updateServerStatusIndicator(true);
                        Toast.makeText(getActivity(), "✓ Device connected!", Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onDataReceived(int itemCount) {
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(getActivity(), "✓ Received " + itemCount + " issued goods record(s)!", Toast.LENGTH_LONG).show();
                        loadTodayIssuedGoods();
                    });
                }
            });

            shareServer.start();
            Log.d("ViewIssuedGoods", "Share server started on port " + ProductShareServer.PORT);

            isDeviceConnected = false;
            updateServerStatusIndicator(false);

            if (isAdded() && getActivity() != null) {
                Toast.makeText(getActivity(), "Ready to receive issued goods", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            Log.e("ViewIssuedGoods", "Could not start share server: " + e.getMessage());
            shareServer = null;
            updateServerStatusIndicator(false);
        }
    }

    private void updateServerStatusIndicator(boolean isConnected) {
        if (getView() != null) {
            TextView statusText = getView().findViewById(R.id.serverStatusText);
            if (statusText != null) {
                statusText.setText("🟢");
                if (isConnected) {
                    statusText.setTextColor(Color.parseColor("#4CAF50"));
                } else {
                    statusText.setTextColor(Color.parseColor("#9E9E9E"));
                }
                statusText.setVisibility(View.VISIBLE);
            }
        }
    }

    // ---------------------------------------------------------------
    // Quick Actions Menu
    // ---------------------------------------------------------------
    private void showQuickActionsMenu() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Quick Actions");

        String[] actions = {"Refresh List", "Share Issued Goods", "Check Server Status"};

        builder.setItems(actions, (dialog, which) -> {
            switch (which) {
                case 0:
                    if (selectedDate.isEmpty()) {
                        loadTodayIssuedGoods();
                    } else {
                        loadIssuedGoodsByDate(selectedDate);
                    }
                    Toast.makeText(getActivity(), "List refreshed", Toast.LENGTH_SHORT).show();
                    break;
                case 1:
                    initiateShare();
                    break;
                case 2:
                    checkServerStatus();
                    break;
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void checkServerStatus() {
        if (shareServer != null) {
            Toast.makeText(getActivity(),
                    "✓ Server is running on port " + ProductShareServer.PORT,
                    Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getActivity(),
                    "✗ Server is NOT running",
                    Toast.LENGTH_SHORT).show();
        }
    }

    // ---------------------------------------------------------------
    // Share - WiFi transfer for Issued Goods
    // ---------------------------------------------------------------
    private void initiateShare() {
        List<IssuedGoodsItem> currentList = adapter.getCurrentList();
        if (currentList == null || currentList.isEmpty()) {
            Toast.makeText(getActivity(), "No issued goods to share", Toast.LENGTH_SHORT).show();
            return;
        }

        final List<IssuedGoodsItem> snapshot = new ArrayList<>(currentList);

        AlertDialog progressDialog = new AlertDialog.Builder(getActivity())
                .setTitle("Sharing Issued Goods")
                .setMessage("Connecting to nearby device...")
                .setCancelable(false)
                .create();
        progressDialog.show();

        new Thread(() -> {
            ProductShareClient.ShareResponse response =
                    ProductShareClient.shareIssuedGoods(requireContext(), snapshot);

            new Handler(Looper.getMainLooper()).post(() -> {
                progressDialog.dismiss();

                if (response.result == ProductShareClient.ShareResult.SUCCESS) {
                    String msg = "✓ Transfer complete!\n" + response.inserted + " record(s) sent";
                    if (response.skipped > 0) {
                        msg += "\n" + response.skipped + " skipped (already exist)";
                    }
                    new AlertDialog.Builder(getActivity())
                            .setTitle("Share Successful")
                            .setMessage(msg)
                            .setPositiveButton("OK", null)
                            .show();
                } else {
                    new AlertDialog.Builder(getActivity())
                            .setTitle("Share Failed")
                            .setMessage("Could not connect to receiving device.\n\nMake sure:\n1. Both devices have the app open\n2. Receiving device is on this screen\n3. Both are on the same WiFi/Hotspot")
                            .setPositiveButton("OK", null)
                            .show();
                }
            });
        }).start();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (shareServer == null) {
            startShareServer();
        }
        if (selectedDate.isEmpty()) {
            loadTodayIssuedGoods();
        } else {
            loadIssuedGoodsByDate(selectedDate);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (shareServer != null) {
            shareServer.stop();
            shareServer = null;
        }
    }

    @Override
    public void onDestroy() {
        if (dbHelper != null) {
            dbHelper.close();
        }
        if (shareServer != null) {
            shareServer.stop();
        }
        super.onDestroy();
    }

    // ---------------------------------------------------------------
    // Data Loading Methods
    // ---------------------------------------------------------------
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
                    loadIssuedGoodsByDate(selectedDate);
                },
                year, month, day
        );

        datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        datePickerDialog.show();
    }

    private void loadTodayIssuedGoods() {
        selectedDate = "";
        btnDateFilter.setText("📅 Today");
        String todayDate = dbDateFormat.format(new Date());
        loadIssuedGoodsByDate(todayDate);
    }

    private void loadIssuedGoodsByDate(String date) {
        new Thread(() -> {
            Cursor cursor = dbHelper.getIssuedGoodsByDate(date);

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
                txtTotalIssued.setText(String.format(Locale.getDefault(), "Total: %d items", finalTotalItems));
                txtTotalQuantity.setText(String.format(Locale.getDefault(), "Qty: %d pieces", finalTotalQuantity));
                txtDateRange.setText(displayDate);

                if (finalItems.isEmpty()) {
                    goodsRecyclerView.setVisibility(View.GONE);
                    emptyStateView.setVisibility(View.VISIBLE);
                    if (date.equals("today")) {
                        txtEmptyMessage.setText("No goods issued today");
                    } else {
                        txtEmptyMessage.setText(String.format("No goods issued on %s", displayDate));
                    }
                } else {
                    goodsRecyclerView.setVisibility(View.VISIBLE);
                    emptyStateView.setVisibility(View.GONE);
                }
            });
        }).start();
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
            String targetDate = selectedDate.isEmpty() ? dbDateFormat.format(new Date()) : selectedDate;
            Cursor cursor = dbHelper.getIssuedGoodsByDate(targetDate);

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
                if (finalFilteredList.isEmpty() && !searchText.isEmpty()) {
                    Toast.makeText(getActivity(),
                            "No results found for: \"" + searchText + "\"",
                            Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    // ---------------------------------------------------------------
    // Item Actions
    // ---------------------------------------------------------------
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
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Delete Issued Goods");
        builder.setMessage("Are you sure you want to delete this issued goods record?\n\n" +
                "Product: " + item.getProductName() + "\n" +
                "Assignee: " + item.getAssignee() + "\n" +
                "Quantity: " + item.getQuantity() + " pieces");

        builder.setPositiveButton("Delete", (dialog, which) -> deleteIssuedGoods(item));
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
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
}