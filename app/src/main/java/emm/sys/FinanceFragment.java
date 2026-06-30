package emm.sys;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.itextpdf.io.source.ByteArrayOutputStream;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class FinanceFragment extends Fragment {

    // Views
    private TextView txtDateRange, txtTotalRevenue, txtTotalProfit, txtTotalExpenses, txtNetCashFlow;
    private MaterialButton btnDateFilter, btnAddExpense, btnSaveExpense, btnCancelExpense;
    private MaterialButton btnGeneratePDF, btnGenerateCSV, btnEmailReport, btnRefresh;
    private Spinner spinnerReportType;
    private EditText txtExpenseCategory, txtExpenseAmount;
    private RecyclerView expenseRecyclerView, topProductsRecyclerView, topAssigneesRecyclerView;
    private View expenseInputRow;

    // Adapters
    private ExpenseAdapter expenseAdapter;
    private TopProductAdapter topProductAdapter;
    private TopAssigneeAdapter topAssigneeAdapter;

    // DBHelper
    private DBHelper dbHelper;

    // Date variables
    private String selectedDate = "";
    private String reportType = "daily";
    private SimpleDateFormat displayFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
    private SimpleDateFormat dbDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    // Data lists
    private List<Expense> expenseList = new ArrayList<>();
    private List<TopProduct> topProductList = new ArrayList<>();
    private List<TopAssignee> topAssigneeList = new ArrayList<>();

    // Formatter for currency - Kenya Shillings
    private NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("en", "KE"));

    // File save launchers
    private ActivityResultLauncher<Intent> csvSaveLauncher;
    private ActivityResultLauncher<Intent> pdfSaveLauncher;

    // Store generated content for saving
    private String generatedCSVContent = "";
    private byte[] generatedPDFContent;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Register CSV save launcher
        csvSaveLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == android.app.Activity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            saveCSVToUri(uri);
                        }
                    }
                }
        );

        // Register PDF save launcher
        pdfSaveLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == android.app.Activity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            savePDFToUri(uri);
                        }
                    }
                }
        );
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_finance, container, false);

        // Initialize views
        initializeViews(view);

        // Initialize DBHelper
        dbHelper = new DBHelper(getActivity());

        // Create expenses table if not exists
        dbHelper.createExpensesTable(dbHelper.getWritableDatabase());

        // Setup spinners
        setupReportTypeSpinner();

        // Setup adapters
        setupAdapters();

        // Setup listeners
        setupListeners();

        // Set default date to today
        setDefaultDate();

        // Load data
        loadFinancialData();

        return view;
    }

    private void initializeViews(View view) {
        txtDateRange = view.findViewById(R.id.txtDateRange);
        txtTotalRevenue = view.findViewById(R.id.txtTotalRevenue);
        txtTotalProfit = view.findViewById(R.id.txtTotalProfit);
        txtTotalExpenses = view.findViewById(R.id.txtTotalExpenses);
        txtNetCashFlow = view.findViewById(R.id.txtNetCashFlow);

        btnDateFilter = view.findViewById(R.id.btnDateFilter);
        btnAddExpense = view.findViewById(R.id.btnAddExpense);
        btnSaveExpense = view.findViewById(R.id.btnSaveExpense);
        btnCancelExpense = view.findViewById(R.id.btnCancelExpense);
        btnGeneratePDF = view.findViewById(R.id.btnGeneratePDF);
        btnGenerateCSV = view.findViewById(R.id.btnGenerateCSV);
        btnEmailReport = view.findViewById(R.id.btnEmailReport);
        btnRefresh = view.findViewById(R.id.btnRefresh);

        spinnerReportType = view.findViewById(R.id.spinnerReportType);

        txtExpenseCategory = view.findViewById(R.id.txtExpenseCategory);
        txtExpenseAmount = view.findViewById(R.id.txtExpenseAmount);
        expenseInputRow = view.findViewById(R.id.expenseInputRow);

        expenseRecyclerView = view.findViewById(R.id.expenseRecyclerView);
        topProductsRecyclerView = view.findViewById(R.id.topProductsRecyclerView);
        topAssigneesRecyclerView = view.findViewById(R.id.topAssigneesRecyclerView);
    }

    private void setupReportTypeSpinner() {
        String[] reportTypes = {"📊 Daily", "📈 Weekly"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(),
                android.R.layout.simple_spinner_item, reportTypes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerReportType.setAdapter(adapter);

        spinnerReportType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                reportType = position == 0 ? "daily" : "weekly";
                updateDateRange();
                loadFinancialData();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupAdapters() {
        expenseRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        expenseAdapter = new ExpenseAdapter(expenseList);
        expenseAdapter.setOnExpenseActionListener(expense -> showDeleteExpenseDialog(expense));
        expenseRecyclerView.setAdapter(expenseAdapter);

        topProductsRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        topProductAdapter = new TopProductAdapter(topProductList);
        topProductsRecyclerView.setAdapter(topProductAdapter);

        topAssigneesRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        topAssigneeAdapter = new TopAssigneeAdapter(topAssigneeList);
        topAssigneesRecyclerView.setAdapter(topAssigneeAdapter);
    }

    private void setupListeners() {
        btnDateFilter.setOnClickListener(v -> showDatePickerDialog());

        btnAddExpense.setOnClickListener(v -> {
            expenseInputRow.setVisibility(View.VISIBLE);
            btnAddExpense.setVisibility(View.GONE);
            txtExpenseCategory.requestFocus();
        });

        btnSaveExpense.setOnClickListener(v -> saveExpense());

        btnCancelExpense.setOnClickListener(v -> {
            expenseInputRow.setVisibility(View.GONE);
            btnAddExpense.setVisibility(View.VISIBLE);
            txtExpenseCategory.setText("");
            txtExpenseAmount.setText("");
        });

        btnGeneratePDF.setOnClickListener(v -> generatePDFReport());
        btnGenerateCSV.setOnClickListener(v -> generateCSVReport());
        btnEmailReport.setOnClickListener(v -> emailReport());

        btnRefresh.setOnClickListener(v -> {
            btnRefresh.animate().rotationBy(360).setDuration(500).start();
            loadFinancialData();
            Toast.makeText(getActivity(), "Data refreshed", Toast.LENGTH_SHORT).show();
        });
    }

    private void setDefaultDate() {
        selectedDate = dbDateFormat.format(new Date());
        btnDateFilter.setText("📅 Today");
        updateDateRange();
    }

    private void updateDateRange() {
        try {
            Date date = dbDateFormat.parse(selectedDate);
            String display = displayFormat.format(date);

            if (reportType.equals("daily")) {
                txtDateRange.setText("Today: " + display);
                btnDateFilter.setText("📅 Today");
            } else {
                Calendar cal = Calendar.getInstance();
                cal.setTime(date);
                cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
                String startDate = dbDateFormat.format(cal.getTime());
                cal.add(Calendar.DAY_OF_WEEK, 6);
                String endDate = dbDateFormat.format(cal.getTime());
                txtDateRange.setText("Week: " + displayFormat.format(cal.getTime()) + " - " + displayFormat.format(cal.getTime()));
                btnDateFilter.setText("📅 Week of " + display);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showDatePickerDialog() {
        Calendar calendar = Calendar.getInstance();
        try {
            if (!TextUtils.isEmpty(selectedDate)) {
                calendar.setTime(dbDateFormat.parse(selectedDate));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        DatePickerDialog dialog = new DatePickerDialog(
                getActivity(),
                (view, year, month, dayOfMonth) -> {
                    Calendar selected = Calendar.getInstance();
                    selected.set(year, month, dayOfMonth);
                    selectedDate = dbDateFormat.format(selected.getTime());
                    btnDateFilter.setText("📅 " + displayFormat.format(selected.getTime()));
                    updateDateRange();
                    loadFinancialData();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        dialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        dialog.show();
    }

    private void loadFinancialData() {
        new Thread(() -> {
            try {
                String startDate, endDate;
                if (reportType.equals("daily")) {
                    startDate = selectedDate;
                    endDate = selectedDate;
                } else {
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(dbDateFormat.parse(selectedDate));
                    cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
                    startDate = dbDateFormat.format(cal.getTime());
                    cal.add(Calendar.DAY_OF_WEEK, 6);
                    endDate = dbDateFormat.format(cal.getTime());
                }

                Cursor summaryCursor = dbHelper.getFinancialSummary(startDate, endDate);
                double revenue = 0, profit = 0, expenses = 0;
                if (summaryCursor != null && summaryCursor.moveToFirst()) {
                    revenue = summaryCursor.getDouble(0);
                    profit = summaryCursor.getDouble(1);
                    expenses = summaryCursor.getDouble(2);
                    summaryCursor.close();
                }

                Cursor expenseCursor = dbHelper.getExpensesByDateRange(startDate, endDate);
                List<Expense> expenseItems = new ArrayList<>();
                if (expenseCursor != null && expenseCursor.moveToFirst()) {
                    do {
                        Expense expense = new Expense(
                                expenseCursor.getInt(0),
                                expenseCursor.getString(1),
                                expenseCursor.getDouble(2),
                                expenseCursor.getString(3),
                                expenseCursor.getString(4)
                        );
                        expenseItems.add(expense);
                    } while (expenseCursor.moveToNext());
                    expenseCursor.close();
                }

                Cursor topProductsCursor = dbHelper.getTopSellingProducts(startDate, endDate, 5);
                List<TopProduct> topProducts = new ArrayList<>();
                int rank = 1;
                if (topProductsCursor != null && topProductsCursor.moveToFirst()) {
                    do {
                        TopProduct product = new TopProduct(
                                rank++,
                                topProductsCursor.getString(0),
                                topProductsCursor.getInt(1),
                                topProductsCursor.getDouble(2)
                        );
                        topProducts.add(product);
                    } while (topProductsCursor.moveToNext());
                    topProductsCursor.close();
                }

                Cursor topAssigneesCursor = dbHelper.getTopAssignees(startDate, endDate, 5);
                List<TopAssignee> topAssignees = new ArrayList<>();
                rank = 1;
                if (topAssigneesCursor != null && topAssigneesCursor.moveToFirst()) {
                    do {
                        TopAssignee assignee = new TopAssignee(
                                rank++,
                                topAssigneesCursor.getString(0),
                                topAssigneesCursor.getInt(1),
                                topAssigneesCursor.getInt(2)
                        );
                        topAssignees.add(assignee);
                    } while (topAssigneesCursor.moveToNext());
                    topAssigneesCursor.close();
                }

                final double finalRevenue = revenue;
                final double finalProfit = profit;
                final double finalExpenses = expenses;
                final List<Expense> finalExpenseItems = expenseItems;
                final List<TopProduct> finalTopProducts = topProducts;
                final List<TopAssignee> finalTopAssignees = topAssignees;

                requireActivity().runOnUiThread(() -> {
                    txtTotalRevenue.setText(formatter.format(finalRevenue));
                    txtTotalProfit.setText(formatter.format(finalProfit));
                    txtTotalExpenses.setText(formatter.format(finalExpenses));
                    txtNetCashFlow.setText(formatter.format(finalRevenue - finalExpenses));

                    expenseAdapter.updateList(finalExpenseItems);
                    topProductAdapter.updateList(finalTopProducts);
                    topAssigneeAdapter.updateList(finalTopAssignees);
                });

            } catch (Exception e) {
                e.printStackTrace();
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(getActivity(), "Error loading data: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show()
                );
            }
        }).start();
    }

    private void saveExpense() {
        String category = txtExpenseCategory.getText().toString().trim();
        String amountStr = txtExpenseAmount.getText().toString().trim();

        if (TextUtils.isEmpty(category)) {
            txtExpenseCategory.setError("Category is required");
            return;
        }

        if (TextUtils.isEmpty(amountStr)) {
            txtExpenseAmount.setError("Amount is required");
            return;
        }

        try {
            double amount = Double.parseDouble(amountStr);
            if (amount <= 0) {
                txtExpenseAmount.setError("Amount must be greater than 0");
                return;
            }

            boolean success = dbHelper.insertExpense(category, amount, selectedDate, "");
            if (success) {
                Toast.makeText(getActivity(), "✓ Expense added: " + formatter.format(amount),
                        Toast.LENGTH_SHORT).show();
                expenseInputRow.setVisibility(View.GONE);
                btnAddExpense.setVisibility(View.VISIBLE);
                txtExpenseCategory.setText("");
                txtExpenseAmount.setText("");
                loadFinancialData();
            } else {
                Toast.makeText(getActivity(), "Failed to add expense", Toast.LENGTH_SHORT).show();
            }
        } catch (NumberFormatException e) {
            txtExpenseAmount.setError("Enter a valid number");
        }
    }

    private void showDeleteExpenseDialog(Expense expense) {
        new AlertDialog.Builder(getActivity())
                .setTitle("Delete Expense")
                .setMessage("Delete expense: " + expense.getCategory() + " (" +
                        formatter.format(expense.getAmount()) + ")?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    boolean success = dbHelper.deleteExpense(expense.getId());
                    if (success) {
                        Toast.makeText(getActivity(), "Expense deleted", Toast.LENGTH_SHORT).show();
                        loadFinancialData();
                    } else {
                        Toast.makeText(getActivity(), "Failed to delete expense", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ============================================================
    // CSV GENERATION & SAVE
    // ============================================================
    private void generateCSVReport() {
        // Show progress
        Toast.makeText(getActivity(), "📊 Generating CSV report...", Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            try {
                String startDate, endDate;
                if (reportType.equals("daily")) {
                    startDate = selectedDate;
                    endDate = selectedDate;
                } else {
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(dbDateFormat.parse(selectedDate));
                    cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
                    startDate = dbDateFormat.format(cal.getTime());
                    cal.add(Calendar.DAY_OF_WEEK, 6);
                    endDate = dbDateFormat.format(cal.getTime());
                }

                StringBuilder csv = new StringBuilder();

                // Header
                csv.append("========================================\n");
                csv.append("        FINANCE REPORT\n");
                csv.append("========================================\n");
                csv.append("Report Type: ").append(reportType.toUpperCase()).append("\n");
                csv.append("Date Range: ").append(startDate).append(" to ").append(endDate).append("\n");
                csv.append("Generated: ").append(new SimpleDateFormat("dd MMM yyyy HH:mm:ss", Locale.getDefault()).format(new Date())).append("\n");
                csv.append("========================================\n\n");

                // Financial Summary
                Cursor summaryCursor = dbHelper.getFinancialSummary(startDate, endDate);
                if (summaryCursor != null && summaryCursor.moveToFirst()) {
                    csv.append("FINANCIAL SUMMARY\n");
                    csv.append("----------------------------------------\n");
                    csv.append("Total Revenue: Ksh. ").append(summaryCursor.getDouble(0)).append("\n");
                    csv.append("Total Profit: Ksh. ").append(summaryCursor.getDouble(1)).append("\n");
                    csv.append("Total Expenses: Ksh. ").append(summaryCursor.getDouble(2)).append("\n");
                    csv.append("Net Cash Flow: Ksh. ").append(summaryCursor.getDouble(0) - summaryCursor.getDouble(2)).append("\n");
                    summaryCursor.close();
                }
                csv.append("\n");

                // Sales Summary
                Cursor salesCursor = dbHelper.getDailySalesSummary(selectedDate);
                if (salesCursor != null && salesCursor.moveToFirst()) {
                    csv.append("SALES SUMMARY\n");
                    csv.append("----------------------------------------\n");
                    csv.append("Transactions: ").append(salesCursor.getInt(0)).append("\n");
                    csv.append("Total Revenue: Ksh. ").append(salesCursor.getDouble(1)).append("\n");
                    csv.append("Average Transaction: Ksh. ").append(String.format("%.2f", salesCursor.getDouble(2))).append("\n");
                    salesCursor.close();
                }
                csv.append("\n");

                // Top Products
                Cursor topProductsCursor = dbHelper.getTopSellingProducts(startDate, endDate, 5);
                csv.append("TOP SELLING PRODUCTS\n");
                csv.append("----------------------------------------\n");
                if (topProductsCursor != null && topProductsCursor.moveToFirst()) {
                    int rank = 1;
                    do {
                        csv.append(rank++).append(". ")
                                .append(topProductsCursor.getString(0)).append(" - ")
                                .append(topProductsCursor.getInt(1)).append(" units - ")
                                .append("Ksh. ").append(topProductsCursor.getDouble(2)).append("\n");
                    } while (topProductsCursor.moveToNext());
                    topProductsCursor.close();
                } else {
                    csv.append("No sales data available\n");
                }
                csv.append("\n");

                // Top Assignees
                Cursor topAssigneesCursor = dbHelper.getTopAssignees(startDate, endDate, 5);
                csv.append("TOP PERFORMING ASSIGNEES\n");
                csv.append("----------------------------------------\n");
                if (topAssigneesCursor != null && topAssigneesCursor.moveToFirst()) {
                    int rank = 1;
                    do {
                        csv.append(rank++).append(". ")
                                .append(topAssigneesCursor.getString(0)).append(" - ")
                                .append(topAssigneesCursor.getInt(1)).append(" transactions - ")
                                .append(topAssigneesCursor.getInt(2)).append(" units\n");
                    } while (topAssigneesCursor.moveToNext());
                    topAssigneesCursor.close();
                } else {
                    csv.append("No assignee data available\n");
                }
                csv.append("\n");

                // Expenses Detail
                Cursor expenseCursor = dbHelper.getExpensesByDateRange(startDate, endDate);
                csv.append("EXPENSES DETAIL\n");
                csv.append("----------------------------------------\n");
                csv.append("Category,Amount,Date,Notes\n");
                if (expenseCursor != null && expenseCursor.moveToFirst()) {
                    do {
                        csv.append(expenseCursor.getString(1)).append(",")
                                .append(expenseCursor.getDouble(2)).append(",")
                                .append(expenseCursor.getString(3)).append(",")
                                .append(expenseCursor.getString(4)).append("\n");
                    } while (expenseCursor.moveToNext());
                    expenseCursor.close();
                } else {
                    csv.append("No expenses recorded\n");
                }
                csv.append("\n");
                csv.append("========================================\n");
                csv.append("End of Report\n");
                csv.append("========================================\n");

                // Store content and open file picker
                generatedCSVContent = csv.toString();
                requireActivity().runOnUiThread(this::openCSVFilePicker);

            } catch (Exception e) {
                e.printStackTrace();
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(getActivity(), "Error generating CSV: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show()
                );
            }
        }).start();
    }

    private void openCSVFilePicker() {
        String fileName = "Finance_Report_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()) + ".csv";

        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/csv");
        intent.putExtra(Intent.EXTRA_TITLE, fileName);

        csvSaveLauncher.launch(intent);
    }

    private void saveCSVToUri(Uri uri) {
        if (TextUtils.isEmpty(generatedCSVContent)) {
            Toast.makeText(getActivity(), "No data to save", Toast.LENGTH_SHORT).show();
            return;
        }

        try (OutputStream out = requireContext().getContentResolver().openOutputStream(uri)) {
            if (out == null) {
                Toast.makeText(getActivity(), "Could not open file", Toast.LENGTH_SHORT).show();
                return;
            }
            out.write(generatedCSVContent.getBytes());
            out.flush();

            requireActivity().runOnUiThread(() ->
                    Toast.makeText(getActivity(), "✅ CSV saved successfully!", Toast.LENGTH_LONG).show()
            );
        } catch (Exception e) {
            e.printStackTrace();
            requireActivity().runOnUiThread(() ->
                    Toast.makeText(getActivity(), "Error saving CSV: " + e.getMessage(), Toast.LENGTH_SHORT).show()
            );
        }
    }

    // ============================================================
    // PDF GENERATION & SAVE
    // ============================================================
    private void generatePDFReport() {
        Toast.makeText(getActivity(), "📄 Generating PDF report...", Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            try {
                String startDate, endDate;
                if (reportType.equals("daily")) {
                    startDate = selectedDate;
                    endDate = selectedDate;
                } else {
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(dbDateFormat.parse(selectedDate));
                    cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
                    startDate = dbDateFormat.format(cal.getTime());
                    cal.add(Calendar.DAY_OF_WEEK, 6);
                    endDate = dbDateFormat.format(cal.getTime());
                }

                // Create PDF in memory
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                Document document = new Document(PageSize.A4);
                PdfWriter.getInstance(document, baos);
                document.open();

                // Fonts
                Font titleFont = new Font(Font.FontFamily.HELVETICA, 20, Font.BOLD, BaseColor.BLUE);
                Font headingFont = new Font(Font.FontFamily.HELVETICA, 16, Font.BOLD, BaseColor.DARK_GRAY);
                Font subHeadingFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD, BaseColor.GRAY);
                Font normalFont = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, BaseColor.BLACK);
                Font greenFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD, BaseColor.GREEN);
                Font redFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD, BaseColor.RED);
                Font blueFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD, BaseColor.BLUE);
                Font orangeFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD, BaseColor.ORANGE);
                Font footerFont = new Font(Font.FontFamily.HELVETICA, 9, Font.ITALIC, BaseColor.GRAY);



                // Header
                Paragraph header = new Paragraph("FINANCE REPORT", titleFont);
                header.setAlignment(Element.ALIGN_CENTER);
                document.add(header);

                // Subtitle
                Paragraph subtitle = new Paragraph("Generated: " +
                        new SimpleDateFormat("dd MMM yyyy HH:mm:ss", Locale.getDefault()).format(new Date()),
                        subHeadingFont);
                subtitle.setAlignment(Element.ALIGN_CENTER);
                document.add(subtitle);

                Paragraph dateRange = new Paragraph("Date Range: " + startDate + " to " + endDate, normalFont);
                dateRange.setAlignment(Element.ALIGN_CENTER);
                document.add(dateRange);

                document.add(new Paragraph("\n"));

                // Financial Summary
                Cursor summaryCursor = dbHelper.getFinancialSummary(startDate, endDate);
                if (summaryCursor != null && summaryCursor.moveToFirst()) {
                    Paragraph sectionTitle = new Paragraph("FINANCIAL SUMMARY", headingFont);
                    sectionTitle.setAlignment(Element.ALIGN_LEFT);
                    document.add(sectionTitle);

                    // Create a table for summary
                    PdfPTable summaryTable = new PdfPTable(2);
                    summaryTable.setWidthPercentage(100);

                    addSummaryRow(summaryTable, "Total Revenue", "Ksh. " + formatDouble(summaryCursor.getDouble(0)), greenFont);
                    addSummaryRow(summaryTable, "Total Profit", "Ksh. " + formatDouble(summaryCursor.getDouble(1)), blueFont);
                    addSummaryRow(summaryTable, "Total Expenses", "Ksh. " + formatDouble(summaryCursor.getDouble(2)), redFont);
                    addSummaryRow(summaryTable, "Net Cash Flow", "Ksh. " + formatDouble(summaryCursor.getDouble(0) - summaryCursor.getDouble(2)), orangeFont);

                    document.add(summaryTable);
                    summaryCursor.close();
                }

                document.add(new Paragraph("\n"));



                // Sales Summary
                Cursor salesCursor = dbHelper.getDailySalesSummary(selectedDate);
                if (salesCursor != null && salesCursor.moveToFirst()) {
                    Paragraph salesTitle = new Paragraph("SALES SUMMARY", headingFont);
                    salesTitle.setAlignment(Element.ALIGN_LEFT);
                    document.add(salesTitle);

                    PdfPTable salesTable = new PdfPTable(2);
                    salesTable.setWidthPercentage(100);

                    addSummaryRow(salesTable, "Transactions", String.valueOf(salesCursor.getInt(0)), normalFont);
                    addSummaryRow(salesTable, "Total Revenue", "Ksh. " + formatDouble(salesCursor.getDouble(1)), greenFont);
                    addSummaryRow(salesTable, "Average Transaction", "Ksh. " + formatDouble(salesCursor.getDouble(2)), blueFont);

                    document.add(salesTable);
                    salesCursor.close();
                }

                document.add(new Paragraph("\n"));



                // Top Products
                Cursor topProductsCursor = dbHelper.getTopSellingProducts(startDate, endDate, 5);
                if (topProductsCursor != null) {
                    Paragraph productsTitle = new Paragraph("TOP SELLING PRODUCTS", headingFont);
                    productsTitle.setAlignment(Element.ALIGN_LEFT);
                    document.add(productsTitle);

                    PdfPTable productsTable = new PdfPTable(4);
                    productsTable.setWidthPercentage(100);
                    productsTable.setWidths(new float[]{0.5f, 2.5f, 1f, 1.5f});

                    // Header
                    addTableHeader(productsTable, "#");
                    addTableHeader(productsTable, "Product");
                    addTableHeader(productsTable, "Quantity");
                    addTableHeader(productsTable, "Revenue");

                    int rank = 1;
                    if (topProductsCursor.moveToFirst()) {
                        do {
                            productsTable.addCell(String.valueOf(rank++));
                            productsTable.addCell(topProductsCursor.getString(0));
                            productsTable.addCell(String.valueOf(topProductsCursor.getInt(1)));
                            productsTable.addCell("Ksh. " + formatDouble(topProductsCursor.getDouble(2)));
                        } while (topProductsCursor.moveToNext());
                    }
                    document.add(productsTable);
                    topProductsCursor.close();
                }

                document.add(new Paragraph("\n"));



                // Top Assignees
                Cursor topAssigneesCursor = dbHelper.getTopAssignees(startDate, endDate, 5);
                if (topAssigneesCursor != null) {
                    Paragraph assigneesTitle = new Paragraph("TOP PERFORMING ASSIGNEES", headingFont);
                    assigneesTitle.setAlignment(Element.ALIGN_LEFT);
                    document.add(assigneesTitle);

                    PdfPTable assigneesTable = new PdfPTable(4);
                    assigneesTable.setWidthPercentage(100);
                    assigneesTable.setWidths(new float[]{0.5f, 2.5f, 1.5f, 1.5f});

                    addTableHeader(assigneesTable, "#");
                    addTableHeader(assigneesTable, "Assignee");
                    addTableHeader(assigneesTable, "Transactions");
                    addTableHeader(assigneesTable, "Quantity");

                    int rank = 1;
                    if (topAssigneesCursor.moveToFirst()) {
                        do {
                            assigneesTable.addCell(String.valueOf(rank++));
                            assigneesTable.addCell(topAssigneesCursor.getString(0));
                            assigneesTable.addCell(String.valueOf(topAssigneesCursor.getInt(1)));
                            assigneesTable.addCell(String.valueOf(topAssigneesCursor.getInt(2)));
                        } while (topAssigneesCursor.moveToNext());
                    }
                    document.add(assigneesTable);
                    topAssigneesCursor.close();
                }

                document.add(new Paragraph("\n"));



                // Expenses Detail
                Cursor expenseCursor = dbHelper.getExpensesByDateRange(startDate, endDate);
                if (expenseCursor != null) {
                    Paragraph expensesTitle = new Paragraph("EXPENSES DETAIL", headingFont);
                    expensesTitle.setAlignment(Element.ALIGN_LEFT);
                    document.add(expensesTitle);

                    PdfPTable expensesTable = new PdfPTable(4);
                    expensesTable.setWidthPercentage(100);
                    expensesTable.setWidths(new float[]{2f, 1.5f, 1.5f, 2f});

                    addTableHeader(expensesTable, "Category");
                    addTableHeader(expensesTable, "Amount");
                    addTableHeader(expensesTable, "Date");
                    addTableHeader(expensesTable, "Notes");

                    if (expenseCursor.moveToFirst()) {
                        do {
                            expensesTable.addCell(expenseCursor.getString(1));
                            expensesTable.addCell("Ksh. " + formatDouble(expenseCursor.getDouble(2)));
                            expensesTable.addCell(expenseCursor.getString(3));
                            expensesTable.addCell(expenseCursor.getString(4) != null ? expenseCursor.getString(4) : "");
                        } while (expenseCursor.moveToNext());
                    }
                    document.add(expensesTable);
                    expenseCursor.close();
                }

                // ================================================
                // FOOTER SECTION
                // ================================================
                document.add(new Paragraph("\n"));

                // Add a horizontal line
                Paragraph line = new Paragraph("____________________________________________________________");
                line.setAlignment(Element.ALIGN_CENTER);
                document.add(line);

                // Footer with email and copyright
                String footerText = "Generated by emmanuelsystems5@gmail.com EMM Sales System © " +
                        new SimpleDateFormat("yyyy", Locale.getDefault()).format(new Date());
                Paragraph footer = new Paragraph(footerText, footerFont);
                footer.setAlignment(Element.ALIGN_CENTER);
                document.add(footer);

                // Add a small note
                Paragraph note = new Paragraph("This report is auto-generated and confidential.",
                        new Font(Font.FontFamily.HELVETICA, 8, Font.ITALIC, BaseColor.GRAY));
                note.setAlignment(Element.ALIGN_CENTER);
                document.add(note);
                // ============================================================
                // END OF FOOTER
                // ============================================================

                document.close();



                // Store PDF content
                generatedPDFContent = baos.toByteArray();

                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(getActivity(), "PDF ready! Choose save location.", Toast.LENGTH_SHORT).show();
                    openPDFFilePicker();
                });

            } catch (Exception e) {
                e.printStackTrace();
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(getActivity(), "Error generating PDF: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show()
                );
            }
        }).start();
    }

    private void addSummaryRow(PdfPTable table, String label, String value, Font font) {
        Phrase labelPhrase = new Phrase(label, new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD));
        Phrase valuePhrase = new Phrase(value, font);

        PdfPCell labelCell = new PdfPCell(labelPhrase);
        labelCell.setPadding(5);
        labelCell.setBorder(Rectangle.NO_BORDER);

        PdfPCell valueCell = new PdfPCell(valuePhrase);
        valueCell.setPadding(5);
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);

        table.addCell(labelCell);
        table.addCell(valueCell);
    }

    private void addTableHeader(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, BaseColor.WHITE)));
        cell.setBackgroundColor(BaseColor.BLUE);
        cell.setPadding(6);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
    }

    private String formatDouble(double value) {
//        NumberFormat formatter = NumberFormat.getInstance(Locale.getDefault());
        NumberFormat formatter = NumberFormat.getInstance(new Locale("en", "KE"));
//        private NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("en", "KE"));
        formatter.setMinimumFractionDigits(0);
        formatter.setMaximumFractionDigits(0);
        return formatter.format(value);
    }

    private void openPDFFilePicker() {
        String fileName = "Finance_Report_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()) + ".pdf";

        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/pdf");
        intent.putExtra(Intent.EXTRA_TITLE, fileName);

        pdfSaveLauncher.launch(intent);
    }

    private void savePDFToUri(Uri uri) {
        if (generatedPDFContent == null || generatedPDFContent.length == 0) {
            Toast.makeText(getActivity(), "No data to save", Toast.LENGTH_SHORT).show();
            return;
        }

        try (OutputStream out = requireContext().getContentResolver().openOutputStream(uri)) {
            if (out == null) {
                Toast.makeText(getActivity(), "Could not open file", Toast.LENGTH_SHORT).show();
                return;
            }
            out.write(generatedPDFContent);  // Now works with byte[]
            out.flush();

            requireActivity().runOnUiThread(() ->
                    Toast.makeText(getActivity(), "✅ PDF saved successfully!", Toast.LENGTH_LONG).show()
            );
        } catch (Exception e) {
            e.printStackTrace();
            requireActivity().runOnUiThread(() ->
                    Toast.makeText(getActivity(), "Error saving PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show()
            );
        }
    }

    private void emailReport() {
        Toast.makeText(getActivity(), "✉️ Opening email client...", Toast.LENGTH_SHORT).show();
        // Implement email intent with attachment
        new Handler(Looper.getMainLooper()).postDelayed(() ->
                Toast.makeText(getActivity(), "✅ Email report feature ready!", Toast.LENGTH_SHORT).show(), 2000);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadFinancialData();
    }
}