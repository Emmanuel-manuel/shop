package emm.sys;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import java.io.OutputStream;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class ViewProductsDetailsFragment extends Fragment {

    private RecyclerView productsRecyclerView;
    private ProductAdapter productAdapter;
    private List<Product> productList;
    private static List<Product> originalProductList; // Store original data for filtering
    private DBHelper dbHelper;

    private TextView totalProductsText, averagePriceText, uniqueProductsText;
    private EditText searchEditText;
    private ImageButton clearSearchButton;
    private Button filterAllButton, filterByNameButton, filterByPriceButton;
    private LinearLayout emptyStateLayout;
    private FloatingActionButton fabQuickActions;

    private String currentFilter = "all"; // all, name, price

    // ---------------------------------------------------------------
    // Export — Storage Access Framework launcher
    // Registered here (not in onCreate) so it captures the correct
    // fragment lifecycle. The launcher opens the system file picker
    // and returns the URI the user chose to save the spreadsheet into.
    // ---------------------------------------------------------------
    private ActivityResultLauncher<Intent> exportFileLauncher;

    // ---------------------------------------------------------------
    // Share server — started when this fragment becomes visible so this
    // device can also act as a receiver when its hotspot is ON.
    // ---------------------------------------------------------------
    private ProductShareServer shareServer;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Register the SAF file-save launcher.
        // When the system file picker returns a URI, write the workbook there.
        exportFileLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == android.app.Activity.RESULT_OK
                            && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            writeWorkbookToUri(uri);
                        }
                    }
                }
        );
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_view_products_details, container, false);

        // Initialize database helper
        dbHelper = new DBHelper(getActivity());

        // Initialize views
        initializeViews(view);

        // Setup RecyclerView
        setupRecyclerView();

        // Setup search functionality
        setupSearch();

        // Setup filter buttons
        setupFilterButtons();

        // Setup FAB
        setupFAB();

        // Load initial data
        loadProductData();

        // Start the local share server so this device can receive data too
        startShareServer();

        return view;
    }

    // ---------------------------------------------------------------
    // Share server lifecycle
    // ---------------------------------------------------------------
    private void startShareServer() {
        try {
            shareServer = new ProductShareServer(requireContext());
            shareServer.start();
        } catch (java.io.IOException e) {
            // Non-fatal — device simply won't be reachable as a receiver
            android.util.Log.w("ViewProductsDetails", "Could not start share server: " + e.getMessage());
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) {
            dbHelper.close();
        }
        if (shareServer != null) {
            shareServer.stop();
        }
    }

    private void initializeViews(View view) {
        productsRecyclerView = view.findViewById(R.id.productsRecyclerView);
        totalProductsText    = view.findViewById(R.id.totalProductsText);
        averagePriceText     = view.findViewById(R.id.averagePriceText);
        uniqueProductsText   = view.findViewById(R.id.uniqueProductsText);
        searchEditText       = view.findViewById(R.id.searchEditText);
        clearSearchButton    = view.findViewById(R.id.clearSearchButton);
        filterAllButton      = view.findViewById(R.id.filterAllButton);
        filterByNameButton   = view.findViewById(R.id.filterByNameButton);
        filterByPriceButton  = view.findViewById(R.id.filterByPriceButton);
        emptyStateLayout     = view.findViewById(R.id.emptyStateLayout);
        fabQuickActions      = view.findViewById(R.id.fabQuickActions);
    }

    private void setupRecyclerView() {
        productList         = new ArrayList<>();
        originalProductList = new ArrayList<>();
        productAdapter      = new ProductAdapter(productList);

        productAdapter.setOnProductClickListener(new ProductAdapter.OnProductClickListener() {
            @Override
            public void onEditProduct(Product product) {
                editProduct(product);
            }

            @Override
            public void onDeleteProduct(Product product) {
                deleteProduct(product);
            }

            @Override
            public void onProductDetails(Product product) {
                showProductDetailsDialog(product);
            }
        });

        productsRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        productsRecyclerView.setAdapter(productAdapter);
    }

    private void setupSearch() {
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String searchQuery = s.toString().trim();
                if (searchQuery.isEmpty()) {
                    clearSearchButton.setVisibility(View.GONE);
                    loadProductData();
                } else {
                    clearSearchButton.setVisibility(View.VISIBLE);
                    filterProducts(searchQuery);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        clearSearchButton.setOnClickListener(v -> {
            searchEditText.setText("");
            clearSearchButton.setVisibility(View.GONE);
            loadProductData();
        });
    }

    private void setupFilterButtons() {
        filterAllButton.setOnClickListener(v -> {
            setActiveFilter("all");
            loadProductData();
        });

        filterByNameButton.setOnClickListener(v -> {
            setActiveFilter("name");
            sortProductsByName();
        });

        filterByPriceButton.setOnClickListener(v -> {
            setActiveFilter("price");
            sortProductsByPrice();
        });
    }

    private void setActiveFilter(String filter) {
        currentFilter = filter;

        filterAllButton.setBackgroundResource(R.drawable.button_filter_unselected);
        filterByNameButton.setBackgroundResource(R.drawable.button_filter_unselected);
        filterByPriceButton.setBackgroundResource(R.drawable.button_filter_unselected);

        switch (filter) {
            case "all":   filterAllButton.setBackgroundResource(R.drawable.button_filter_selected);   break;
            case "name":  filterByNameButton.setBackgroundResource(R.drawable.button_filter_selected); break;
            case "price": filterByPriceButton.setBackgroundResource(R.drawable.button_filter_selected); break;
        }
    }

    private void setupFAB() {
        fabQuickActions.setOnClickListener(v -> showQuickActionsMenu());
    }

    // ---------------------------------------------------------------
    // Quick Actions menu
    // ---------------------------------------------------------------
    private void showQuickActionsMenu() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Quick Actions");

        String[] actions = {"Refresh List", "Export Data", "Share", "Add New Product", "Delete All Products"};

        builder.setItems(actions, (dialog, which) -> {
            switch (which) {
                case 0: // Refresh
                    loadProductData();
                    Toast.makeText(getActivity(), "Product list refreshed", Toast.LENGTH_SHORT).show();
                    break;

                case 1: // Export — generate spreadsheet and let user pick save location
                    initiateExport();
                    break;

                case 2: // Share — wire up the WiFi transfer
                    initiateShare();
                    break;

                case 3: // Add New
                    navigateToAddProduct();
                    break;

                case 4: // Delete All
                    showDeleteAllConfirmation();
                    break;
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    // ---------------------------------------------------------------
    // Share — WiFi product transfer
    // ---------------------------------------------------------------

    /**
     * Called when the user taps "Share" in the Quick Actions menu.
     *
     * Shows a progress dialog, then performs the transfer on a background
     * thread, and updates the UI when done.
     */
    private void initiateShare() {
        if (productList == null || productList.isEmpty()) {
            Toast.makeText(getActivity(), "No products to share", Toast.LENGTH_SHORT).show();
            return;
        }

        // Make a snapshot of the list for the background thread
        final List<Product> snapshot = new ArrayList<>(productList);

        // Show progress dialog
        AlertDialog progressDialog = new AlertDialog.Builder(getActivity())
                .setTitle("Sharing Products")
                .setMessage("Connecting to nearby device…")
                .setCancelable(false)
                .create();
        progressDialog.show();

        // Background thread — network calls must not run on the main thread
        new Thread(() -> {
            ProductShareClient.ShareResponse response =
                    ProductShareClient.shareProducts(requireContext(), snapshot);

            // Back to UI thread
            new Handler(Looper.getMainLooper()).post(() -> {
                progressDialog.dismiss();
                handleShareResponse(response);
            });
        }).start();
    }

    /**
     * Translates the ShareResponse into a user-facing message.
     */
    private void handleShareResponse(ProductShareClient.ShareResponse response) {
        if (getActivity() == null) return;

        switch (response.result) {
            case SUCCESS:
                String msg = "Transfer complete!\n"
                        + response.inserted + " product(s) sent";
                if (response.skipped > 0) {
                    msg += ", " + response.skipped + " skipped (already exist on receiver)";
                }
                showShareResultDialog("Share Successful", msg, false);
                break;

            case DATA_TRANSFER_ERROR:
                // Covers: receiver app not found, wrong app, schema mismatch
                showShareResultDialog("Transfer Failed",
                        "Data transfer error, please contact your developer or system admin",
                        true);
                break;

            case NETWORK_ERROR:
                showShareResultDialog("Transfer Failed",
                        "Could not reach the other device.\n" +
                                "Make sure both devices are on the same WiFi/hotspot and try again.",
                        true);
                break;
        }
    }

    private void showShareResultDialog(String title, String message, boolean isError) {
        if (getActivity() == null) return;
        new AlertDialog.Builder(getActivity())
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }

    // ---------------------------------------------------------------
    // Export — spreadsheet via Apache POI + Storage Access Framework
    // ---------------------------------------------------------------

    /**
     * Step 1 — Guard check, then open the system file-save picker.
     * The picker lets the user choose any location: Downloads, Google Drive,
     * SD card, etc. The result is handled in exportFileLauncher (registered
     * in onCreate) which calls writeWorkbookToUri().
     */
    private void initiateExport() {
        if (productList == null || productList.isEmpty()) {
            Toast.makeText(getActivity(), "No products to export", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if fragment is attached
        if (!isAdded() || getActivity() == null) {
            return;
        }

        // Build filename: ProductDetails_2026-04-28.xls
        String dateStamp = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String fileName  = "ProductDetails_" + dateStamp + ".xls";

        // ACTION_CREATE_DOCUMENT opens the system picker in "save" mode.
        // The user sees the filename pre-filled but can rename or change location.
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/vnd.ms-excel");
        intent.putExtra(Intent.EXTRA_TITLE, fileName);

        exportFileLauncher.launch(intent);
    }

    /**
     * Step 2 — Called by exportFileLauncher after the user confirms a save location.
     * Builds the workbook on a background thread and streams it to the chosen URI.
     */
    private void writeWorkbookToUri(Uri uri) {
        // Snapshot the list so the background thread has a stable copy
        final List<Product> snapshot = new ArrayList<>(productList);

        // Store dialog reference
        final AlertDialog[] progress = new AlertDialog[1];

        // Check if fragment is still attached before showing dialog
        if (!isAdded() || getActivity() == null) {
            return;
        }

        // Show dialog on UI thread
        requireActivity().runOnUiThread(() -> {
            if (isAdded() && getActivity() != null) {
                progress[0] = new AlertDialog.Builder(requireActivity())
                        .setTitle("Exporting…")
                        .setMessage("Building spreadsheet, please wait.")
                        .setCancelable(false)
                        .create();
                progress[0].show();
            }
        });

        new Thread(() -> {
            boolean success = false;
            String errorMsg = "";

            try (Workbook workbook = buildWorkbook(snapshot);
                 OutputStream out = requireContext().getContentResolver().openOutputStream(uri)) {

                if (out == null) throw new Exception("Could not open output stream for chosen location.");
                workbook.write(out);
                out.flush();
                success = true;

            } catch (Exception e) {
                android.util.Log.e("Export", "Failed to write spreadsheet", e);
                errorMsg = e.getMessage();
            }

            final boolean finalSuccess = success;
            final String finalError = errorMsg;

            // Update UI on main thread, checking if fragment is still alive
            new Handler(Looper.getMainLooper()).post(() -> {
                // Dismiss dialog if it exists and fragment is still valid
                if (progress[0] != null && progress[0].isShowing()) {
                    try {
                        progress[0].dismiss();
                    } catch (Exception e) {
                        android.util.Log.e("Export", "Error dismissing dialog", e);
                    }
                }

                // Check if fragment is still attached before showing result
                if (!isAdded() || getActivity() == null) {
                    return;
                }

                if (finalSuccess) {
                    new AlertDialog.Builder(requireActivity())
                            .setTitle("Export Successful")
                            .setMessage("Spreadsheet saved successfully to your chosen location.")
                            .setPositiveButton("OK", null)
                            .show();
                } else {
                    new AlertDialog.Builder(requireActivity())
                            .setTitle("Export Failed")
                            .setMessage("Could not save the file.\n\nDetails: " + finalError)
                            .setPositiveButton("OK", null)
                            .show();
                }
            });
        }).start();
    }

    /**
     * Step 3 — Builds the HSSFWorkbook (Excel .xls) with full formatting.
     *
     * Sheet layout:
     *   Row 0  — Report title (merged, bold)
     *   Row 1  — Generated date
     *   Row 2  — Summary stats (Total, Avg Selling Price, Unique Products)
     *   Row 3  — (blank spacer)
     *   Row 4  — Column headers (bold, coloured background)
     *   Row 5+ — One row per product
     *   Last   — Totals row (sum of buying, selling, profit)
     */
    private Workbook buildWorkbook(List<Product> products) {
        Workbook wb = new HSSFWorkbook();
        Sheet sheet = wb.createSheet("Product Details");

        // ── Styles ──────────────────────────────────────────────────

        // Title style — large, bold, centred
        CellStyle titleStyle = wb.createCellStyle();
        Font titleFont = wb.createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 14);
        titleStyle.setFont(titleFont);
        titleStyle.setAlignment(HorizontalAlignment.CENTER);

        // Sub-header style — italic (no color issue here)
        CellStyle subStyle = wb.createCellStyle();
        Font subFont = wb.createFont();
        subFont.setItalic(true);
        // Use HSSFColor.GREY_50_PERCENT.index instead of IndexedColors
        subFont.setColor(HSSFColor.HSSFColorPredefined.GREY_50_PERCENT.getIndex());
        subStyle.setFont(subFont);

        // Column header style — bold white text on dark blue background
        CellStyle headerStyle = wb.createCellStyle();
        Font headerFont = wb.createFont();
        headerFont.setBold(true);
        // Use HSSFColor.WHITE instead of IndexedColors.WHITE
        headerFont.setColor(HSSFColor.HSSFColorPredefined.WHITE.getIndex());
        headerStyle.setFont(headerFont);
        // Use HSSFColor.DARK_BLUE for background
        headerStyle.setFillForegroundColor(HSSFColor.HSSFColorPredefined.DARK_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);

        // Data style — normal
        CellStyle dataStyle = wb.createCellStyle();
        dataStyle.setAlignment(HorizontalAlignment.LEFT);

        // Currency style — right-aligned
        CellStyle currencyStyle = wb.createCellStyle();
        currencyStyle.setAlignment(HorizontalAlignment.RIGHT);

        // Profit positive — using HSSFColor.GREEN
        CellStyle profitGoodStyle = wb.createCellStyle();
        Font greenFont = wb.createFont();
        greenFont.setColor(HSSFColor.HSSFColorPredefined.GREEN.getIndex());
        greenFont.setBold(true);
        profitGoodStyle.setFont(greenFont);
        profitGoodStyle.setAlignment(HorizontalAlignment.RIGHT);

        // Profit negative — using HSSFColor.RED
        CellStyle profitBadStyle = wb.createCellStyle();
        Font redFont = wb.createFont();
        redFont.setColor(HSSFColor.HSSFColorPredefined.RED.getIndex());
        redFont.setBold(true);
        profitBadStyle.setFont(redFont);
        profitBadStyle.setAlignment(HorizontalAlignment.RIGHT);

        // Totals row style — using HSSFColor.LIGHT_YELLOW
        CellStyle totalsStyle = wb.createCellStyle();
        Font totalsFont = wb.createFont();
        totalsFont.setBold(true);
        totalsStyle.setFont(totalsFont);
        totalsStyle.setFillForegroundColor(HSSFColor.HSSFColorPredefined.LIGHT_YELLOW.getIndex());
        totalsStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        totalsStyle.setAlignment(HorizontalAlignment.RIGHT);

        // ── Row 0: Title ─────────────────────────────────────────────
        Row titleRow = sheet.createRow(0);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("Product Details Report");
        titleCell.setCellStyle(titleStyle);

        // Merge cells for title (optional but nice)
        sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 8));

        // ── Row 1: Generated date ─────────────────────────────────────
        Row dateRow = sheet.createRow(1);
        Cell dateCell = dateRow.createCell(0);
        String nowStr = new SimpleDateFormat("EEEE, MMMM dd yyyy  HH:mm", Locale.getDefault()).format(new Date());
        dateCell.setCellValue("Generated: " + nowStr);
        dateCell.setCellStyle(subStyle);
        sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(1, 1, 0, 8));

        // ── Row 2: Summary stats ──────────────────────────────────────
        Row statsRow = sheet.createRow(2);
        int totalProducts = products.size();
        long totalSelling = 0;
        Set<String> uniqueNames = new HashSet<>();
        for (Product p : products) {
            totalSelling += p.getSellingPrice();
            uniqueNames.add(p.getProductName());
        }
        long avgSelling = totalProducts > 0 ? totalSelling / totalProducts : 0;

        Cell totalCell = statsRow.createCell(0);
        totalCell.setCellValue("Total Products: " + totalProducts);

        Cell avgCell = statsRow.createCell(2);
        avgCell.setCellValue("Avg Selling Price: KES " + NumberFormat.getInstance().format(avgSelling));

        Cell uniqueCell = statsRow.createCell(5);
        uniqueCell.setCellValue("Unique Products: " + uniqueNames.size());

        // ── Row 3: Blank spacer ───────────────────────────────────────
        sheet.createRow(3);

        // ── Row 4: Column headers ─────────────────────────────────────
        String[] headers = {
                "#", "Product Name", "Weight", "Flavour",
                "Buying Price (KES)", "Selling Price (KES)", "Profit (KES)",
                "Profit Margin (%)", "Date Added"
        };
        Row headerRow = sheet.createRow(4);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // ── Rows 5+: Product data ─────────────────────────────────────
        int rowNum = 5;
        long grandBuying = 0;
        long grandSelling = 0;
        long grandProfit = 0;

        for (Product p : products) {
            Row row = sheet.createRow(rowNum++);

            row.createCell(0).setCellValue(p.getId());

            Cell nameCell = row.createCell(1);
            nameCell.setCellValue(p.getProductName());
            nameCell.setCellStyle(dataStyle);

            Cell weightCell = row.createCell(2);
            weightCell.setCellValue(p.getWeight());
            weightCell.setCellStyle(dataStyle);

            Cell flavourCell = row.createCell(3);
            flavourCell.setCellValue(p.getFlavour());
            flavourCell.setCellStyle(dataStyle);

            Cell buyCell = row.createCell(4);
            buyCell.setCellValue(p.getBuyingPrice());
            buyCell.setCellStyle(currencyStyle);

            Cell sellCell = row.createCell(5);
            sellCell.setCellValue(p.getSellingPrice());
            sellCell.setCellStyle(currencyStyle);

            // Profit — colour-coded
            Cell profitCell = row.createCell(6);
            profitCell.setCellValue(p.getProfit());
            profitCell.setCellStyle(p.getProfit() >= 0 ? profitGoodStyle : profitBadStyle);

            // Profit margin %
            double margin = p.getBuyingPrice() > 0
                    ? ((double) p.getProfit() / p.getBuyingPrice()) * 100 : 0;
            Cell marginCell = row.createCell(7);
            marginCell.setCellValue(Math.round(margin * 10.0) / 10.0); // 1 decimal
            marginCell.setCellStyle(currencyStyle);

            Cell tsCell = row.createCell(8);
            tsCell.setCellValue(formatTimestamp(p.getTimestamp()));
            tsCell.setCellStyle(dataStyle);

            grandBuying += p.getBuyingPrice();
            grandSelling += p.getSellingPrice();
            grandProfit += p.getProfit();
        }

        // ── Totals row ────────────────────────────────────────────────
        Row totalsRow = sheet.createRow(rowNum);
        Cell totalsLabel = totalsRow.createCell(0);
        totalsLabel.setCellValue("TOTALS");
        totalsLabel.setCellStyle(totalsStyle);

        Cell totBuyCell = totalsRow.createCell(4);
        totBuyCell.setCellValue(grandBuying);
        totBuyCell.setCellStyle(totalsStyle);

        Cell totSellCell = totalsRow.createCell(5);
        totSellCell.setCellValue(grandSelling);
        totSellCell.setCellStyle(totalsStyle);

        Cell totProfitCell = totalsRow.createCell(6);
        totProfitCell.setCellValue(grandProfit);
        totProfitCell.setCellStyle(totalsStyle);

        // ── Auto-size all columns ─────────────────────────────────────
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }

        return wb;
    }
    // ---------------------------------------------------------------
    // Navigation helpers
    // ---------------------------------------------------------------
    private void navigateToAddProduct() {
        if (getActivity() instanceof LandingPageActivity) {
            ((LandingPageActivity) getActivity()).handleAddProductDetailsNavigation();
        }
    }

    private void showDeleteAllConfirmation() {
        new AlertDialog.Builder(getActivity())
                .setTitle("Delete All Products")
                .setMessage("Are you sure you want to delete ALL products? This action cannot be undone.")
                .setPositiveButton("Delete All", (dialog, which) -> deleteAllProducts())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteAllProducts() {
        boolean success = dbHelper.deleteAllProducts();
        if (success) {
            Toast.makeText(getActivity(), "All products deleted successfully", Toast.LENGTH_SHORT).show();
            loadProductData();
        } else {
            Toast.makeText(getActivity(), "Failed to delete products", Toast.LENGTH_SHORT).show();
        }
    }

    // ---------------------------------------------------------------
    // Data loading
    // ---------------------------------------------------------------
    private void loadProductData() {
        productList.clear();
        originalProductList.clear();
        Cursor cursor = dbHelper.getAllProductDetails();

        if (cursor != null && cursor.moveToFirst()) {
            do {
                Product product = extractProductFromCursor(cursor);
                productList.add(product);
                originalProductList.add(product);
            } while (cursor.moveToNext());

            cursor.close();
            updateStatistics();
            applyCurrentFilter();
            productAdapter.notifyDataSetChanged();
            showEmptyState(false);
        } else {
            showEmptyState(true);
            updateStatistics();
        }
    }

    private Product extractProductFromCursor(Cursor cursor) {
        Product product = new Product();
        product.setId(cursor.getInt(cursor.getColumnIndexOrThrow("id")));
        product.setProductName(cursor.getString(cursor.getColumnIndexOrThrow("product_name")));
        product.setWeight(cursor.getString(cursor.getColumnIndexOrThrow("weight")));
        product.setFlavour(cursor.getString(cursor.getColumnIndexOrThrow("flavour")));
        product.setBuyingPrice(cursor.getInt(cursor.getColumnIndexOrThrow("buying_price")));
        product.setSellingPrice(cursor.getInt(cursor.getColumnIndexOrThrow("selling_price")));
        product.setProfit(cursor.getInt(cursor.getColumnIndexOrThrow("profit")));
        product.setTimestamp(cursor.getString(cursor.getColumnIndexOrThrow("timestamp")));
        return product;
    }

    private void updateStatistics() {
        if (productList.isEmpty()) {
            totalProductsText.setText("0");
            averagePriceText.setText("KES 0");
            uniqueProductsText.setText("0");
            return;
        }

        int total = productList.size();
        totalProductsText.setText(String.valueOf(total));

        int totalSelling = 0;
        Set<String> uniqueNames = new HashSet<>();
        for (Product p : productList) {
            totalSelling += p.getSellingPrice();
            uniqueNames.add(p.getProductName());
        }

        averagePriceText.setText("KES " + NumberFormat.getInstance().format(totalSelling / total));
        uniqueProductsText.setText(String.valueOf(uniqueNames.size()));
    }

    private void applyCurrentFilter() {
        switch (currentFilter) {
            case "name":  sortProductsByName();  break;
            case "price": sortProductsByPrice(); break;
        }
    }

    private void sortProductsByName() {
        Collections.sort(productList, (p1, p2) ->
                p1.getProductName().compareToIgnoreCase(p2.getProductName()));
        productAdapter.notifyDataSetChanged();
    }

    private void sortProductsByPrice() {
        Collections.sort(productList, (p1, p2) ->
                Integer.compare(p1.getSellingPrice(), p2.getSellingPrice()));
        productAdapter.notifyDataSetChanged();
    }

    private void filterProducts(String query) {
        List<Product> filtered = new ArrayList<>();
        for (Product p : originalProductList) {
            if (p.getProductName().toLowerCase().contains(query.toLowerCase()) ||
                    p.getWeight().toLowerCase().contains(query.toLowerCase()) ||
                    p.getFlavour().toLowerCase().contains(query.toLowerCase())) {
                filtered.add(p);
            }
        }
        productAdapter.updateList(filtered);
        showEmptyState(filtered.isEmpty());
    }

    private void showEmptyState(boolean show) {
        emptyStateLayout.setVisibility(show ? View.VISIBLE : View.GONE);
        productsRecyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    // ---------------------------------------------------------------
    // Edit / Delete dialogs
    // ---------------------------------------------------------------
    private void editProduct(Product product) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Edit Product: " + product.getProductName());

        View dialogView = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_edit_product, null);
        builder.setView(dialogView);

        EditText editProductName  = dialogView.findViewById(R.id.editProductName);
        EditText editWeight       = dialogView.findViewById(R.id.editWeight);
        EditText editFlavour      = dialogView.findViewById(R.id.editFlavour);
        EditText editBuyingPrice  = dialogView.findViewById(R.id.editBuyingPrice);
        EditText editSellingPrice = dialogView.findViewById(R.id.editSellingPrice);
        TextView profitDisplay    = dialogView.findViewById(R.id.profitDisplay);

        editProductName.setText(product.getProductName());
        editWeight.setText(product.getWeight());
        editFlavour.setText(product.getFlavour());
        editBuyingPrice.setText(String.valueOf(product.getBuyingPrice()));
        editSellingPrice.setText(String.valueOf(product.getSellingPrice()));
        profitDisplay.setText("Profit: KES " + NumberFormat.getInstance().format(product.getProfit()));

        TextWatcher profitCalculator = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                calculateProfit(editBuyingPrice, editSellingPrice, profitDisplay);
            }
            @Override public void afterTextChanged(Editable s) {}
        };
        editBuyingPrice.addTextChangedListener(profitCalculator);
        editSellingPrice.addTextChangedListener(profitCalculator);

        builder.setPositiveButton("Update", (dialog, which) -> {
            String newName         = editProductName.getText().toString().trim();
            String newWeight       = editWeight.getText().toString().trim();
            String newFlavour      = editFlavour.getText().toString().trim();
            String newBuyingStr    = editBuyingPrice.getText().toString().trim();
            String newSellingStr   = editSellingPrice.getText().toString().trim();

            if (newName.isEmpty() || newWeight.isEmpty() || newFlavour.isEmpty() ||
                    newBuyingStr.isEmpty() || newSellingStr.isEmpty()) {
                Toast.makeText(getActivity(), "All fields are required", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                int newBuying  = Integer.parseInt(newBuyingStr);
                int newSelling = Integer.parseInt(newSellingStr);

                if (newBuying <= 0) {
                    Toast.makeText(getActivity(), "Buying price must be greater than 0", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (newSelling <= 0) {
                    Toast.makeText(getActivity(), "Selling price must be greater than 0", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (newSelling <= newBuying) {
                    Toast.makeText(getActivity(), "Selling price must be greater than buying price", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (!newName.equals(product.getProductName()) ||
                        !newWeight.equals(product.getWeight()) ||
                        !newFlavour.equals(product.getFlavour())) {
                    if (dbHelper.checkProductDetailsExists(newName, newWeight, newFlavour)) {
                        Toast.makeText(getActivity(), "Product with these details already exists", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }

                boolean success = dbHelper.updateProductDetails(product.getId(), newName, newWeight,
                        newFlavour, newBuying, newSelling);

                if (success) {
                    int profit = newSelling - newBuying;
                    Toast.makeText(getActivity(),
                            "Product updated! Profit: KES " + NumberFormat.getInstance().format(profit),
                            Toast.LENGTH_LONG).show();
                    loadProductData();
                } else {
                    Toast.makeText(getActivity(), "Failed to update product", Toast.LENGTH_SHORT).show();
                }
            } catch (NumberFormatException e) {
                Toast.makeText(getActivity(), "Please enter valid numbers for prices", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void calculateProfit(EditText buyingEdit, EditText sellingEdit, TextView profitDisplay) {
        try {
            String buyStr  = buyingEdit.getText().toString().trim();
            String sellStr = sellingEdit.getText().toString().trim();

            if (!buyStr.isEmpty() && !sellStr.isEmpty()) {
                int buying  = Integer.parseInt(buyStr);
                int selling = Integer.parseInt(sellStr);
                int profit  = selling - buying;

                profitDisplay.setText("Profit: KES " + NumberFormat.getInstance().format(profit));
                if      (profit > 0) profitDisplay.setTextColor(Color.parseColor("#4CAF50"));
                else if (profit < 0) profitDisplay.setTextColor(Color.parseColor("#F44336"));
                else                 profitDisplay.setTextColor(Color.parseColor("#757575"));
            } else {
                profitDisplay.setText("Profit: KES 0");
                profitDisplay.setTextColor(Color.parseColor("#757575"));
            }
        } catch (NumberFormatException e) {
            profitDisplay.setText("Profit: Invalid");
            profitDisplay.setTextColor(Color.parseColor("#F44336"));
        }
    }

    private void deleteProduct(Product product) {
        String message = "Are you sure you want to delete this product?\n"
                + "Product: " + product.getProductName() + "\n"
                + "Weight: " + product.getWeight() + "\n"
                + "Flavour: " + product.getFlavour() + "\n"
                + "ID: #" + String.format("%03d", product.getId()) + "\n"
                + "This action cannot be undone.";

        new AlertDialog.Builder(getActivity())
                .setTitle("Delete Product")
                .setMessage(message)
                .setPositiveButton("Delete", (dialog, which) -> {
                    boolean success = dbHelper.deleteProduct(product.getId());
                    if (success) {
                        Toast.makeText(getActivity(),
                                "Product #" + product.getId() + " deleted successfully",
                                Toast.LENGTH_SHORT).show();
                        loadProductData();
                    } else {
                        Toast.makeText(getActivity(),
                                "Failed to delete product #" + product.getId(),
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showProductDetailsDialog(Product product) {
        String details = "Product: "       + product.getProductName() + "\n"
                + "Weight: "       + product.getWeight()       + "\n"
                + "Flavour: "      + product.getFlavour()      + "\n"
                + "Buying Price: KES "  + NumberFormat.getInstance().format(product.getBuyingPrice())  + "\n"
                + "Selling Price: KES " + NumberFormat.getInstance().format(product.getSellingPrice()) + "\n"
                + "Profit: KES "        + NumberFormat.getInstance().format(product.getProfit())       + "\n"
                + "Added: "        + formatTimestamp(product.getTimestamp());

        new AlertDialog.Builder(getActivity())
                .setTitle("Product Details")
                .setMessage(details)
                .setPositiveButton("OK", null)
                .show();
    }

    // ---------------------------------------------------------------
    // Timestamp helpers
    // ---------------------------------------------------------------
    private static String formatTimestamp(String timestamp) {
        try {
            SimpleDateFormat in  = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            SimpleDateFormat out = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
            return out.format(in.parse(timestamp));
        } catch (Exception e) {
            return timestamp;
        }
    }

    private static String getTimeAgo(String timestamp) {
        try {
            SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            long diff    = new Date().getTime() - fmt.parse(timestamp).getTime();
            long seconds = diff / 1000;
            long minutes = seconds / 60;
            long hours   = minutes / 60;
            long days    = hours / 24;

            if (days > 0)    return days    + " day"    + (days > 1    ? "s" : "") + " ago";
            if (hours > 0)   return hours   + " hour"   + (hours > 1   ? "s" : "") + " ago";
            if (minutes > 0) return minutes + " minute" + (minutes > 1 ? "s" : "") + " ago";
            return "Just now";
        } catch (Exception e) {
            return "Unknown";
        }
    }

    // ===============================================================
    // Product Model
    // ===============================================================
    static class Product {
        private int id;
        private String productName, weight, flavour, timestamp;
        private int buyingPrice, sellingPrice, profit;

        public int getId()                    { return id; }
        public void setId(int id)             { this.id = id; }

        public String getProductName()                  { return productName; }
        public void setProductName(String productName)  { this.productName = productName; }

        public String getWeight()             { return weight; }
        public void setWeight(String weight)  { this.weight = weight; }

        public String getFlavour()              { return flavour; }
        public void setFlavour(String flavour)  { this.flavour = flavour; }

        public int getBuyingPrice()                   { return buyingPrice; }
        public void setBuyingPrice(int buyingPrice)   { this.buyingPrice = buyingPrice; }

        public int getSellingPrice()                    { return sellingPrice; }
        public void setSellingPrice(int sellingPrice)   { this.sellingPrice = sellingPrice; }

        public int getProfit()                { return profit; }
        public void setProfit(int profit)     { this.profit = profit; }

        public String getTimestamp()                  { return timestamp; }
        public void setTimestamp(String timestamp)    { this.timestamp = timestamp; }

        @Deprecated public int    getPrice()           { return sellingPrice; }
        @Deprecated public void   setPrice(int price)  { this.sellingPrice = price; }
    }

    // ===============================================================
    // RecyclerView Adapter
    // ===============================================================
    public static class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ViewHolder> {
        private List<Product> productList;
        private OnProductClickListener listener;

        public interface OnProductClickListener {
            void onEditProduct(Product product);
            void onDeleteProduct(Product product);
            void onProductDetails(Product product);
        }

        public ProductAdapter(List<Product> productList) {
            this.productList = productList;
        }

        public void setOnProductClickListener(OnProductClickListener listener) {
            this.listener = listener;
        }

        public void updateList(List<Product> newList) {
            this.productList = newList;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_product, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Product product = productList.get(position);
            NumberFormat fmt = NumberFormat.getInstance();

            holder.productNameText.setText(product.getProductName());
            holder.productWeightText.setText(product.getWeight());
            holder.productFlavourText.setText(product.getFlavour());
            holder.productIdText.setText("#" + String.format("%03d", product.getId()));

            holder.buyingPriceText.setText("Buy: KES " + fmt.format(product.getBuyingPrice()));
            holder.sellingPriceText.setText("KES " + fmt.format(product.getSellingPrice()));
            holder.profitText.setText("Profit: KES " + fmt.format(product.getProfit()));

            double margin = product.getBuyingPrice() > 0
                    ? ((double) product.getProfit() / product.getBuyingPrice()) * 100 : 0;
            holder.profitMarginText.setText(String.format("%.1f%% margin", margin));

            if      (product.getProfit() > 0) holder.profitText.setTextColor(Color.parseColor("#4CAF50"));
            else if (product.getProfit() < 0) holder.profitText.setTextColor(Color.parseColor("#F44336"));
            else                              holder.profitText.setTextColor(Color.parseColor("#757575"));

            holder.timestampText.setText(formatTimestamp(product.getTimestamp()));
            holder.timeAgoText.setText(getTimeAgo(product.getTimestamp()));

            holder.editButton.setOnClickListener(v -> {
                if (listener != null) listener.onEditProduct(product);
            });
            holder.deleteButton.setOnClickListener(v -> {
                if (listener != null) listener.onDeleteProduct(product);
            });
            holder.itemView.setOnClickListener(v -> {
                if (listener != null) listener.onProductDetails(product);
            });

            final boolean[] isExpanded = {false};
            holder.expandButton.setOnClickListener(v -> {
                isExpanded[0] = !isExpanded[0];
                holder.extraDetailsLayout.setVisibility(isExpanded[0] ? View.VISIBLE : View.GONE);
                holder.expandButton.setRotation(isExpanded[0] ? 180 : 0);
                holder.expandButton.setContentDescription(isExpanded[0] ? "Collapse details" : "Expand details");
            });
        }

        @Override
        public int getItemCount() {
            return productList.size();
        }

        public void filter(String query) {
            List<Product> filtered = new ArrayList<>();
            for (Product p : originalProductList) {
                if (p.getProductName().toLowerCase().contains(query.toLowerCase()) ||
                        p.getWeight().toLowerCase().contains(query.toLowerCase()) ||
                        p.getFlavour().toLowerCase().contains(query.toLowerCase())) {
                    filtered.add(p);
                }
            }
            updateList(filtered);
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView productNameText, productWeightText, productFlavourText, productIdText;
            TextView buyingPriceText, sellingPriceText, profitText, profitMarginText;
            TextView timestampText, timeAgoText;
            ImageView editButton, deleteButton, expandButton;
            LinearLayout extraDetailsLayout;
            CardView cardView;

            ViewHolder(View itemView) {
                super(itemView);
                productNameText   = itemView.findViewById(R.id.productNameText);
                productWeightText = itemView.findViewById(R.id.productWeightText);
                productFlavourText= itemView.findViewById(R.id.productFlavourText);
                productIdText     = itemView.findViewById(R.id.productIdText);
                buyingPriceText   = itemView.findViewById(R.id.buyingPriceText);
                sellingPriceText  = itemView.findViewById(R.id.sellingPriceText);
                profitText        = itemView.findViewById(R.id.profitText);
                profitMarginText  = itemView.findViewById(R.id.profitMarginText);
                timestampText     = itemView.findViewById(R.id.timestampText);
                timeAgoText       = itemView.findViewById(R.id.timeAgoText);
                editButton        = itemView.findViewById(R.id.editButton);
                deleteButton      = itemView.findViewById(R.id.deleteButton);
                expandButton      = itemView.findViewById(R.id.expandButton);
                extraDetailsLayout= itemView.findViewById(R.id.extraDetailsLayout);
                cardView          = itemView.findViewById(R.id.cardView);
            }
        }
    }
}
