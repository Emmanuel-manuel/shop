package emm.sys;

import android.app.DatePickerDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.text.NumberFormat;
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

public class ViewSalesFragment extends Fragment {

    // UI Components
    private MaterialButton btnDateFilter;
    private TextView txtTotalSales, txtPaidAmount, txtToPayAmount;
    private TextView txtPieChartNote, txtBarChartNote, txtLineChartNote;
    private TextView txtResultCount;
    private MaterialButton btnClearSearch;
    private LinearLayout emptyState;

    // Charts
    private PieChart pieChart;
    private BarChart barChart;
    private LineChart lineChart;

    // Search
    private TextInputEditText editSearch;

    // RecyclerView
    private RecyclerView transactionsRecyclerView;
    private TransactionAdapter transactionAdapter;
    private List<TransactionItem> allTransactions = new ArrayList<>();
    private List<TransactionItem> filteredTransactions = new ArrayList<>();

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
        View view = inflater.inflate(R.layout.fragment_view_sales, container, false);

        initializeViews(view);
        setupCharts();

        dbHelper = new DBHelper(getActivity());

        // Load today's data
        selectedDate = dbDateFormat.format(new Date());
        btnDateFilter.setText("📅 Today");
        loadSalesData(selectedDate);

        setupListeners();

        return view;
    }

    private void initializeViews(View view) {
        btnDateFilter = view.findViewById(R.id.btnDateFilter);
        txtTotalSales = view.findViewById(R.id.txtTotalSales);
        txtPaidAmount = view.findViewById(R.id.txtPaidAmount);
        txtToPayAmount = view.findViewById(R.id.txtToPayAmount);

        pieChart = view.findViewById(R.id.pieChart);
        barChart = view.findViewById(R.id.barChart);
        lineChart = view.findViewById(R.id.lineChart);
        txtPieChartNote = view.findViewById(R.id.txtPieChartNote);
        txtBarChartNote = view.findViewById(R.id.txtBarChartNote);
        txtLineChartNote = view.findViewById(R.id.txtLineChartNote);

        editSearch = view.findViewById(R.id.editSearch);
        txtResultCount = view.findViewById(R.id.txtResultCount);
        btnClearSearch = view.findViewById(R.id.btnClearSearch);
        emptyState = view.findViewById(R.id.emptyState);

        transactionsRecyclerView = view.findViewById(R.id.transactionsRecyclerView);
        transactionsRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        transactionAdapter = new TransactionAdapter(filteredTransactions);
        transactionsRecyclerView.setAdapter(transactionAdapter);
    }

    private void setupCharts() {
        // Setup Pie Chart
        pieChart.setUsePercentValues(true);
        pieChart.getDescription().setEnabled(false);
        pieChart.setExtraOffsets(5, 10, 5, 5);
        pieChart.setDragDecelerationFrictionCoef(0.95f);
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleColor(Color.WHITE);
        pieChart.setHoleRadius(35f);
        pieChart.setTransparentCircleRadius(40f);
        pieChart.setDrawCenterText(true);
        pieChart.setCenterText("Payment\nModes");
        pieChart.setCenterTextSize(12f);
        pieChart.setCenterTextColor(Color.parseColor("#2196F3"));
        pieChart.setRotationAngle(0);
        pieChart.setRotationEnabled(true);

        Legend pieLegend = pieChart.getLegend();
        pieLegend.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        pieLegend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        pieLegend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        pieLegend.setTextSize(10f);

        // Setup Bar Chart
        barChart.getDescription().setEnabled(false);
        barChart.setDrawBarShadow(false);
        barChart.setDrawValueAboveBar(true);
        barChart.setMaxVisibleValueCount(60);
        barChart.setPinchZoom(false);
        barChart.setDrawGridBackground(false);

        XAxis barXAxis = barChart.getXAxis();
        barXAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        barXAxis.setDrawGridLines(false);
        barXAxis.setGranularity(1f);
        barXAxis.setTextSize(10f);

        YAxis barLeftAxis = barChart.getAxisLeft();
        barLeftAxis.setLabelCount(8, false);
        barLeftAxis.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART);
        barLeftAxis.setSpaceTop(15f);
        barLeftAxis.setAxisMinimum(0f);
        barLeftAxis.setTextSize(10f);

        YAxis barRightAxis = barChart.getAxisRight();
        barRightAxis.setEnabled(false);

        // Setup Line Chart
        lineChart.getDescription().setEnabled(false);
        lineChart.setDrawGridBackground(false);
        lineChart.setDragEnabled(true);
        lineChart.setScaleEnabled(true);
        lineChart.setPinchZoom(true);

        XAxis lineXAxis = lineChart.getXAxis();
        lineXAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        lineXAxis.setDrawGridLines(false);
        lineXAxis.setGranularity(1f);
        lineXAxis.setTextSize(10f);

        YAxis lineLeftAxis = lineChart.getAxisLeft();
        lineLeftAxis.setDrawGridLines(true);
        lineLeftAxis.setAxisMinimum(0f);
        lineLeftAxis.setTextSize(10f);

        YAxis lineRightAxis = lineChart.getAxisRight();
        lineRightAxis.setEnabled(false);
    }

    private void setupListeners() {
        btnDateFilter.setOnClickListener(v -> showDatePickerDialog());

        editSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterTransactions(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnClearSearch.setOnClickListener(v -> {
            editSearch.setText("");
            btnClearSearch.setVisibility(View.GONE);
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
                    loadSalesData(selectedDate);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        datePickerDialog.show();
    }

    private void loadSalesData(String date) {
        new Thread(() -> {
            if (!isFragmentActive || dbHelper == null) return;

            try {
                List<TransactionItem> sales = getSalesByDate(date);
                List<TransactionItem> toPay = getToPayByDate(date);

                allTransactions.clear();
                allTransactions.addAll(sales);
                allTransactions.addAll(toPay);

                // Sort by timestamp (newest first)
                Collections.sort(allTransactions, (a, b) -> b.getTimestamp().compareTo(a.getTimestamp()));

                int totalSales = 0;
                int totalPaid = 0;
                int totalToPay = 0;

                for (TransactionItem item : sales) {
                    totalSales += item.getBill();
                    totalPaid += item.getBill();
                }

                for (TransactionItem item : toPay) {
                    totalSales += item.getBill();
                    totalToPay += item.getBill();
                }

                final int finalTotalSales = totalSales;
                final int finalTotalPaid = totalPaid;
                final int finalTotalToPay = totalToPay;
                final List<TransactionItem> finalSales = sales;
                final List<TransactionItem> finalToPay = toPay;

                if (handler != null && isFragmentActive) {
                    handler.post(() -> {
                        if (!isAdded()) return;

                        // Update summary
                        txtTotalSales.setText("KES " + formatter.format(finalTotalSales));
                        txtPaidAmount.setText("KES " + formatter.format(finalTotalPaid));
                        txtToPayAmount.setText("KES " + formatter.format(finalTotalToPay));

                        // Update charts
                        updatePieChart(finalSales, finalToPay);
                        updateBarChart(finalSales, finalToPay);
                        updateLineChart(date);

                        // Update transaction list
                        filterTransactions(editSearch.getText().toString());

                        // Update notes
                        txtPieChartNote.setText("Payment distribution - " + (finalSales.size() + finalToPay.size()) + " transactions");
                        txtBarChartNote.setText("Top products - " + getTopProducts(finalSales, finalToPay, 5).size() + " products");
                        txtLineChartNote.setText("7-day sales trend ending " + displayFormat.format(new Date()));
                    });
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private List<TransactionItem> getSalesByDate(String date) {
        List<TransactionItem> salesList = new ArrayList<>();

        try {
            android.database.Cursor cursor = dbHelper.getReadableDatabase().rawQuery(
                    "SELECT id, market, cust_name, product_name, selling_price, quantity, bill, payment_mode, total_bill, timestamp " +
                            "FROM sales WHERE date(timestamp) = ? ORDER BY timestamp DESC",
                    new String[]{date}
            );

            if (cursor != null) {
                while (cursor.moveToNext()) {
                    TransactionItem item = new TransactionItem(
                            cursor.getInt(0),
                            cursor.getString(1),
                            cursor.getString(2),
                            cursor.getString(3),
                            cursor.getInt(4),
                            cursor.getInt(5),
                            cursor.getInt(6),
                            cursor.getString(7),
                            cursor.getInt(8),
                            cursor.getString(9)
                    );
                    salesList.add(item);
                }
                cursor.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return salesList;
    }

    private List<TransactionItem> getToPayByDate(String date) {
        List<TransactionItem> toPayList = new ArrayList<>();

        try {
            android.database.Cursor cursor = dbHelper.getReadableDatabase().rawQuery(
                    "SELECT id, market, cust_name, product_name, selling_price, quantity, bill, payment_mode, total_bill, balance, timestamp " +
                            "FROM to_pay WHERE date(timestamp) = ? ORDER BY timestamp DESC",
                    new String[]{date}
            );

            if (cursor != null) {
                while (cursor.moveToNext()) {
                    TransactionItem item = new TransactionItem(
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

    private void filterTransactions(String query) {
        filteredTransactions.clear();

        if (query.isEmpty()) {
            filteredTransactions.addAll(allTransactions);
            btnClearSearch.setVisibility(View.GONE);
        } else {
            String lowerQuery = query.toLowerCase(Locale.getDefault());

            for (TransactionItem item : allTransactions) {
                if (item.getCustName().toLowerCase().contains(lowerQuery) ||
                        item.getMarket().toLowerCase().contains(lowerQuery) ||
                        item.getProductName().toLowerCase().contains(lowerQuery) ||
                        item.getPaymentMode().toLowerCase().contains(lowerQuery)) {
                    filteredTransactions.add(item);
                }
            }
            btnClearSearch.setVisibility(View.VISIBLE);
        }

        // Update result count
        txtResultCount.setText(filteredTransactions.size() + " transactions");

        // Show/hide empty state
        if (filteredTransactions.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
            transactionsRecyclerView.setVisibility(View.GONE);
        } else {
            emptyState.setVisibility(View.GONE);
            transactionsRecyclerView.setVisibility(View.VISIBLE);
        }

        transactionAdapter.updateList(filteredTransactions);
    }

    private void updatePieChart(List<TransactionItem> sales, List<TransactionItem> toPay) {
        List<PieEntry> entries = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();

        // Calculate totals by payment mode
        Map<String, Integer> paymentTotals = new HashMap<>();

        for (TransactionItem item : sales) {
            String mode = item.getPaymentMode();
            int currentTotal = paymentTotals.containsKey(mode) ? paymentTotals.get(mode) : 0;
            paymentTotals.put(mode, currentTotal + item.getBill());
        }

        for (TransactionItem item : toPay) {
            String mode = "To-Pay";
            int currentTotal = paymentTotals.containsKey(mode) ? paymentTotals.get(mode) : 0;
            paymentTotals.put(mode, currentTotal + item.getBill());
        }

        if (paymentTotals.isEmpty()) {
            entries.add(new PieEntry(100f, "No Data"));
            colors.add(Color.parseColor("#CCCCCC"));
        } else {
            int colorIndex = 0;
            for (Map.Entry<String, Integer> entry : paymentTotals.entrySet()) {
                entries.add(new PieEntry(entry.getValue(), entry.getKey()));
                colors.add(getChartColor(colorIndex++));
            }
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(colors);
        dataSet.setSliceSpace(2f);
        dataSet.setSelectionShift(5f);
        dataSet.setValueTextSize(10f);
        dataSet.setValueTextColor(Color.WHITE);

        PieData data = new PieData(dataSet);
        data.setValueFormatter(new PercentFormatter());

        pieChart.setData(data);
        pieChart.animateY(1000);
        pieChart.invalidate();
    }

    private void updateBarChart(List<TransactionItem> sales, List<TransactionItem> toPay) {
        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        // Get top 5 products by quantity
        List<Map.Entry<String, Integer>> topProducts = getTopProducts(sales, toPay, 5);

        if (topProducts.isEmpty()) {
            entries.add(new BarEntry(0, 0));
            labels.add("No Data");
        } else {
            for (int i = 0; i < topProducts.size(); i++) {
                Map.Entry<String, Integer> product = topProducts.get(i);
                entries.add(new BarEntry(i, product.getValue()));

                String displayName = product.getKey().length() > 8 ?
                        product.getKey().substring(0, 8) + "..." : product.getKey();
                labels.add(displayName);
            }
        }

        BarDataSet dataSet = new BarDataSet(entries, "Quantity Sold");
        dataSet.setColor(Color.parseColor("#2196F3"));
        dataSet.setValueTextSize(8f);

        BarData data = new BarData(dataSet);
        data.setBarWidth(0.5f);

        barChart.setData(data);
        barChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        barChart.animateY(1000);
        barChart.invalidate();
    }

    private List<Map.Entry<String, Integer>> getTopProducts(List<TransactionItem> sales, List<TransactionItem> toPay, int limit) {
        Map<String, Integer> productQuantities = new HashMap<>();

        for (TransactionItem item : sales) {
            String product = item.getProductName();
            int currentQty = productQuantities.containsKey(product) ? productQuantities.get(product) : 0;
            productQuantities.put(product, currentQty + item.getQuantity());
        }

        for (TransactionItem item : toPay) {
            String product = item.getProductName();
            int currentQty = productQuantities.containsKey(product) ? productQuantities.get(product) : 0;
            productQuantities.put(product, currentQty + item.getQuantity());
        }

        List<Map.Entry<String, Integer>> sortedEntries = new ArrayList<>(productQuantities.entrySet());
        Collections.sort(sortedEntries, (a, b) -> b.getValue().compareTo(a.getValue()));

        return sortedEntries.subList(0, Math.min(limit, sortedEntries.size()));
    }

    private void updateLineChart(String currentDate) {
        List<Entry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        try {
            Calendar cal = Calendar.getInstance();
            cal.setTime(dbDateFormat.parse(currentDate));

            for (int i = 6; i >= 0; i--) {
                Calendar tempCal = (Calendar) cal.clone();
                tempCal.add(Calendar.DAY_OF_MONTH, -i);

                String dateStr = dbDateFormat.format(tempCal.getTime());
                String labelStr = new SimpleDateFormat("dd/MM", Locale.getDefault()).format(tempCal.getTime());

                List<TransactionItem> daySales = getSalesByDate(dateStr);
                List<TransactionItem> dayToPay = getToPayByDate(dateStr);

                int total = 0;
                for (TransactionItem item : daySales) total += item.getBill();
                for (TransactionItem item : dayToPay) total += item.getBill();

                entries.add(new Entry(6 - i, total));
                labels.add(labelStr);
            }
        } catch (Exception e) {
            for (int i = 0; i < 7; i++) {
                entries.add(new Entry(i, 0));
                labels.add("Day " + (i + 1));
            }
        }

        LineDataSet dataSet = new LineDataSet(entries, "Sales Amount");
        dataSet.setColor(Color.parseColor("#4CAF50"));
        dataSet.setCircleColor(Color.parseColor("#4CAF50"));
        dataSet.setLineWidth(2f);
        dataSet.setCircleRadius(4f);
        dataSet.setValueTextSize(8f);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);

        LineData data = new LineData(dataSet);

        lineChart.setData(data);
        lineChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        lineChart.animateX(1000);
        lineChart.invalidate();
    }

    private int getChartColor(int index) {
        int[] colors = {
                Color.parseColor("#2196F3"), // Blue
                Color.parseColor("#4CAF50"), // Green
                Color.parseColor("#FF9800"), // Orange
                Color.parseColor("#9C27B0"), // Purple
                Color.parseColor("#F44336"), // Red
                Color.parseColor("#00BCD4"), // Cyan
                Color.parseColor("#8BC34A")  // Light Green
        };
        return colors[index % colors.length];
    }

    @Override
    public void onResume() {
        super.onResume();
        isFragmentActive = true;
        if (!selectedDate.isEmpty()) {
            loadSalesData(selectedDate);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        isFragmentActive = false;
        handler.removeCallbacksAndMessages(null);
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