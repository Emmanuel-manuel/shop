package emm.sys;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
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

import androidx.core.content.ContextCompat;
import androidx.cardview.widget.CardView;
import androidx.appcompat.widget.PopupMenu;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

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

        return view;
    }

    private void initializeViews(View view) {
        productsRecyclerView = view.findViewById(R.id.productsRecyclerView);
        totalProductsText = view.findViewById(R.id.totalProductsText);
        averagePriceText = view.findViewById(R.id.averagePriceText);
        uniqueProductsText = view.findViewById(R.id.uniqueProductsText);
        searchEditText = view.findViewById(R.id.searchEditText);
        clearSearchButton = view.findViewById(R.id.clearSearchButton);
        filterAllButton = view.findViewById(R.id.filterAllButton);
        filterByNameButton = view.findViewById(R.id.filterByNameButton);
        filterByPriceButton = view.findViewById(R.id.filterByPriceButton);
        emptyStateLayout = view.findViewById(R.id.emptyStateLayout);
        fabQuickActions = view.findViewById(R.id.fabQuickActions);
    }

    private void setupRecyclerView() {
        productList = new ArrayList<>();
        originalProductList = new ArrayList<>();
        productAdapter = new ProductAdapter(productList); // Fixed: Removed Context parameter

        // Set up the click listener
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

        clearSearchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchEditText.setText("");
                clearSearchButton.setVisibility(View.GONE);
                loadProductData();
            }
        });
    }

    private void setupFilterButtons() {
        filterAllButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setActiveFilter("all");
                loadProductData();
            }
        });

        filterByNameButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setActiveFilter("name");
                sortProductsByName();
            }
        });

        filterByPriceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setActiveFilter("price");
                sortProductsByPrice();
            }
        });
    }

    private void setActiveFilter(String filter) {
        currentFilter = filter;

        // Reset all buttons to unselected state
        filterAllButton.setBackgroundResource(R.drawable.button_filter_unselected);
        filterByNameButton.setBackgroundResource(R.drawable.button_filter_unselected);
        filterByPriceButton.setBackgroundResource(R.drawable.button_filter_unselected);

        // Set active button background
        switch (filter) {
            case "all":
                filterAllButton.setBackgroundResource(R.drawable.button_filter_selected);
                break;
            case "name":
                filterByNameButton.setBackgroundResource(R.drawable.button_filter_selected);
                break;
            case "price":
                filterByPriceButton.setBackgroundResource(R.drawable.button_filter_selected);
                break;
        }
    }

    private void setupFAB() {
        fabQuickActions.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showQuickActionsMenu();
            }
        });
    }

    private void showQuickActionsMenu() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Quick Actions");

        String[] actions = {"Refresh List", "Export Data", "Share", "Add New Product", "Delete All Products"};

        builder.setItems(actions, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0: // Refresh
                        loadProductData();
                        Toast.makeText(getActivity(), "Product list refreshed", Toast.LENGTH_SHORT).show();
                        break;
                    case 1: // Export
                        Toast.makeText(getActivity(), "Export feature coming soon", Toast.LENGTH_SHORT).show();
                        break;
                    case 2: // Share
                        Toast.makeText(getActivity(), "Share feature coming soon", Toast.LENGTH_SHORT).show();
                        break;
                    case 3: // Add New
                        navigateToAddProduct();
                        break;
                    case 4: // Delete All
                        showDeleteAllConfirmation();
                        break;
                }
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void navigateToAddProduct() {
        // Navigate to ProductDetailsFragment
        if (getActivity() instanceof LandingPageActivity) {
            ((LandingPageActivity) getActivity()).handleAddProductDetailsNavigation();
        }
    }

    private void showDeleteAllConfirmation() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Delete All Products");
        builder.setMessage("Are you sure you want to delete ALL products? This action cannot be undone.");

        builder.setPositiveButton("Delete All", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                deleteAllProducts();
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void deleteAllProducts() {
        boolean success = dbHelper.deleteAllProducts();
        if (success) {
            Toast.makeText(getActivity(), "All products deleted successfully", Toast.LENGTH_SHORT).show();
            loadProductData(); // Refresh the list
        } else {
            Toast.makeText(getActivity(), "Failed to delete products", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadProductData() {
        productList.clear();
        originalProductList.clear();
        Cursor cursor = dbHelper.getAllProductDetails();

        if (cursor != null && cursor.moveToFirst()) {
            do {
                Product product = extractProductFromCursor(cursor);
                productList.add(product);
                originalProductList.add(product); // Store original data
            } while (cursor.moveToNext());

            cursor.close();

            // Update statistics
            updateStatistics();

            // Sort based on current filter
            applyCurrentFilter();

            // Update UI
            productAdapter.notifyDataSetChanged();
            showEmptyState(false);
        } else {
            showEmptyState(true);
            updateStatistics(); // Update with zero values
        }
    }

    private Product extractProductFromCursor(Cursor cursor) {
        Product product = new Product();

        product.setId(cursor.getInt(cursor.getColumnIndexOrThrow("id")));
        product.setProductName(cursor.getString(cursor.getColumnIndexOrThrow("product_name")));
        product.setWeight(cursor.getString(cursor.getColumnIndexOrThrow("weight")));
        product.setFlavour(cursor.getString(cursor.getColumnIndexOrThrow("flavour")));
        // Updated to read new columns
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

        // Total products
        int totalProducts = productList.size();
        totalProductsText.setText(String.valueOf(totalProducts));

        // Average selling price
        int totalSellingPrice = 0;
        for (Product product : productList) {
            totalSellingPrice += product.getSellingPrice();
        }
        int averageSellingPrice = totalSellingPrice / totalProducts;
        averagePriceText.setText("KES " + NumberFormat.getInstance().format(averageSellingPrice));

        // Unique products (based on product name)
        Set<String> uniqueProductNames = new HashSet<>();
        for (Product product : productList) {
            uniqueProductNames.add(product.getProductName());
        }
        uniqueProductsText.setText(String.valueOf(uniqueProductNames.size()));
    }

    private void applyCurrentFilter() {
        switch (currentFilter) {
            case "all":
                // Already in default order from database (by product_name)
                break;
            case "name":
                sortProductsByName();
                break;
            case "price":
                sortProductsByPrice();
                break;
        }
    }

    private void sortProductsByName() {
        Collections.sort(productList, new Comparator<Product>() {
            @Override
            public int compare(Product p1, Product p2) {
                return p1.getProductName().compareToIgnoreCase(p2.getProductName());
            }
        });
        productAdapter.notifyDataSetChanged();
    }

    private void sortProductsByPrice() {
        Collections.sort(productList, new Comparator<Product>() {
            @Override
            public int compare(Product p1, Product p2) {
                return Integer.compare(p1.getSellingPrice(), p2.getSellingPrice());
            }
        });
        productAdapter.notifyDataSetChanged();
    }

    private void filterProducts(String query) {
        List<Product> filteredList = new ArrayList<>();

        for (Product product : originalProductList) { // Filter from original data
            if (product.getProductName().toLowerCase().contains(query.toLowerCase()) ||
                    product.getWeight().toLowerCase().contains(query.toLowerCase()) ||
                    product.getFlavour().toLowerCase().contains(query.toLowerCase())) {
                filteredList.add(product);
            }
        }

        productAdapter.updateList(filteredList);

        if (filteredList.isEmpty()) {
            showEmptyState(true);
        } else {
            showEmptyState(false);
        }
    }

    private void showEmptyState(boolean show) {
        if (show) {
            emptyStateLayout.setVisibility(View.VISIBLE);
            productsRecyclerView.setVisibility(View.GONE);
        } else {
            emptyStateLayout.setVisibility(View.GONE);
            productsRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void editProduct(Product product) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Edit Product: " + product.getProductName());

        View dialogView = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_edit_product, null);
        builder.setView(dialogView);

        // Initialize all EditTexts
        EditText editProductName = dialogView.findViewById(R.id.editProductName);
        EditText editWeight = dialogView.findViewById(R.id.editWeight);
        EditText editFlavour = dialogView.findViewById(R.id.editFlavour);
        EditText editBuyingPrice = dialogView.findViewById(R.id.editBuyingPrice);
        EditText editSellingPrice = dialogView.findViewById(R.id.editSellingPrice);
        TextView profitDisplay = dialogView.findViewById(R.id.profitDisplay);

        // Set current values
        editProductName.setText(product.getProductName());
        editWeight.setText(product.getWeight());
        editFlavour.setText(product.getFlavour());
        editBuyingPrice.setText(String.valueOf(product.getBuyingPrice()));
        editSellingPrice.setText(String.valueOf(product.getSellingPrice()));
        profitDisplay.setText("Profit: KES " + NumberFormat.getInstance().format(product.getProfit()));

        // Add TextWatchers for real-time profit calculation
        TextWatcher profitCalculator = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                calculateProfit(editBuyingPrice, editSellingPrice, profitDisplay);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };

        editBuyingPrice.addTextChangedListener(profitCalculator);
        editSellingPrice.addTextChangedListener(profitCalculator);

        builder.setPositiveButton("Update", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String newName = editProductName.getText().toString().trim();
                String newWeight = editWeight.getText().toString().trim();
                String newFlavour = editFlavour.getText().toString().trim();
                String newBuyingPriceStr = editBuyingPrice.getText().toString().trim();
                String newSellingPriceStr = editSellingPrice.getText().toString().trim();

                // Validate all fields
                if (newName.isEmpty() || newWeight.isEmpty() || newFlavour.isEmpty() ||
                        newBuyingPriceStr.isEmpty() || newSellingPriceStr.isEmpty()) {
                    Toast.makeText(getActivity(), "All fields are required", Toast.LENGTH_SHORT).show();
                    return;
                }

                try {
                    int newBuyingPrice = Integer.parseInt(newBuyingPriceStr);
                    int newSellingPrice = Integer.parseInt(newSellingPriceStr);

                    // Validate prices
                    if (newBuyingPrice <= 0) {
                        Toast.makeText(getActivity(), "Buying price must be greater than 0", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (newSellingPrice <= 0) {
                        Toast.makeText(getActivity(), "Selling price must be greater than 0", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (newSellingPrice <= newBuyingPrice) {
                        Toast.makeText(getActivity(), "Selling price must be greater than buying price", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Check if product with new details already exists (different ID)
                    if (!newName.equals(product.getProductName()) ||
                            !newWeight.equals(product.getWeight()) ||
                            !newFlavour.equals(product.getFlavour())) {

                        boolean exists = dbHelper.checkProductDetailsExists(newName, newWeight, newFlavour);
                        if (exists) {
                            Toast.makeText(getActivity(),
                                    "Product with these details already exists",
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }

                    // ENHANCED: Update with all fields including profit calculation
                    boolean success = dbHelper.updateProductDetails(product.getId(), newName, newWeight,
                            newFlavour, newBuyingPrice, newSellingPrice);

                    if (success) {
                        int profit = newSellingPrice - newBuyingPrice;
                        Toast.makeText(getActivity(),
                                "Product updated successfully! " +
                                "Profit: KES " + NumberFormat.getInstance().format(profit),
                                Toast.LENGTH_LONG).show();
                        loadProductData(); // Refresh list
                    } else {
                        Toast.makeText(getActivity(),
                                "Failed to update product",
                                Toast.LENGTH_SHORT).show();
                    }

                } catch (NumberFormatException e) {
                    Toast.makeText(getActivity(), "Please enter valid numbers for prices", Toast.LENGTH_SHORT).show();
                }
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    // Helper method for real-time profit calculation
    private void calculateProfit(EditText buyingPriceEdit, EditText sellingPriceEdit, TextView profitDisplay) {
        try {
            String buyingStr = buyingPriceEdit.getText().toString().trim();
            String sellingStr = sellingPriceEdit.getText().toString().trim();

            if (!buyingStr.isEmpty() && !sellingStr.isEmpty()) {
                int buyingPrice = Integer.parseInt(buyingStr);
                int sellingPrice = Integer.parseInt(sellingStr);
                int profit = sellingPrice - buyingPrice;

                profitDisplay.setText("Profit: KES " + NumberFormat.getInstance().format(profit));

                // Set color based on profit
                if (profit > 0) {
                    profitDisplay.setTextColor(Color.parseColor("#4CAF50")); // Green
                } else if (profit < 0) {
                    profitDisplay.setTextColor(Color.parseColor("#F44336")); // Red
                } else {
                    profitDisplay.setTextColor(Color.parseColor("#757575")); // Gray
                }
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
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Delete Product");
        // ENHANCED: Show more product details in confirmation
        String message = "Are you sure you want to delete this product? " +
        "Product: " + product.getProductName() + " " +
        "Weight: " + product.getWeight() + " " +
        "Flavour: " + product.getFlavour() + " " +
        "ID: #" + String.format("%03d", product.getId()) + " " +
        "This action cannot be undone.";

        builder.setMessage(message);

        builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // FIXED: Using product.getId() instead of product name
                boolean success = dbHelper.deleteProduct(product.getId());

                if (success) {
                    Toast.makeText(getActivity(),
                            "Product #" + product.getId() + " deleted successfully",
                            Toast.LENGTH_SHORT).show();
                    loadProductData(); // Refresh list
                } else {
                    Toast.makeText(getActivity(),
                            "Failed to delete product #" + product.getId(),
                            Toast.LENGTH_SHORT).show();
                }
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    // Add missing method for product details dialog
    private void showProductDetailsDialog(Product product) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Product Details");

        String details = "Product: " + product.getProductName() + " " +
        "Weight: " + product.getWeight() + " " +
        "Flavour: " + product.getFlavour() + " " +
        "Buying Price: KES " + NumberFormat.getInstance().format(product.getBuyingPrice()) + " " +
        "Selling Price: KES " + NumberFormat.getInstance().format(product.getSellingPrice()) + " " +
        "Profit: KES " + NumberFormat.getInstance().format(product.getProfit()) + " " +
         "Added: " + formatTimestamp(product.getTimestamp());

        builder.setMessage(details);
        builder.setPositiveButton("OK", null);
        builder.show();
    }

    private static String formatTimestamp(String timestamp) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());

            Date date = inputFormat.parse(timestamp);
            return outputFormat.format(date);
        } catch (Exception e) {
            return timestamp;
        }
    }

    private static String getTimeAgo(String timestamp) {
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            Date past = format.parse(timestamp);
            Date now = new Date();

            long diff = now.getTime() - past.getTime();
            long seconds = diff / 1000;
            long minutes = seconds / 60;
            long hours = minutes / 60;
            long days = hours / 24;

            if (days > 0) {
                return days + " day" + (days > 1 ? "s" : "") + " ago";
            } else if (hours > 0) {
                return hours + " hour" + (hours > 1 ? "s" : "") + " ago";
            } else if (minutes > 0) {
                return minutes + " minute" + (minutes > 1 ? "s" : "") + " ago";
            } else {
                return "Just now";
            }
        } catch (Exception e) {
            return "Unknown";
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) {
            dbHelper.close();
        }
    }

    // Product Model Class
    private static class Product {
        private int id;
        private String productName;
        private String weight;
        private String flavour;
        private int buyingPrice;
        private int sellingPrice;
        private int profit;
        private String timestamp;

        // Getters and Setters
        public int getId() { return id; }
        public void setId(int id) { this.id = id; }

        public String getProductName() { return productName; }
        public void setProductName(String productName) { this.productName = productName; }

        public String getWeight() { return weight; }
        public void setWeight(String weight) { this.weight = weight; }

        public String getFlavour() { return flavour; }
        public void setFlavour(String flavour) { this.flavour = flavour; }

        public int getBuyingPrice() { return buyingPrice; }
        public void setBuyingPrice(int buyingPrice) { this.buyingPrice = buyingPrice; }

        public int getSellingPrice() { return sellingPrice; }
        public void setSellingPrice(int sellingPrice) { this.sellingPrice = sellingPrice; }

        public int getProfit() { return profit; }
        public void setProfit(int profit) { this.profit = profit; }

        public String getTimestamp() { return timestamp; }
        public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

        // Backward compatibility - return selling price as main price
        @Deprecated
        public int getPrice() { return sellingPrice; }

        @Deprecated
        public void setPrice(int price) { this.sellingPrice = price; }
    }

    // RecyclerView Adapter (Fixed Inner Class)
    public static class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ViewHolder> {
        private List<Product> productList;
        private OnProductClickListener listener;

        // Interface for handling product clicks
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
            NumberFormat formatter = NumberFormat.getInstance();

            // Basic product information
            holder.productNameText.setText(product.getProductName());
            holder.productWeightText.setText(product.getWeight());
            holder.productFlavourText.setText(product.getFlavour());
            holder.productIdText.setText("#" + String.format("%03d", product.getId()));

            // Pricing information
            holder.buyingPriceText.setText("Buy: KES " + formatter.format(product.getBuyingPrice()));
            holder.sellingPriceText.setText("KES " + formatter.format(product.getSellingPrice()));
            holder.profitText.setText("Profit: KES " + formatter.format(product.getProfit()));

            // Profit margin calculation
            double profitMargin = 0;
            if (product.getBuyingPrice() > 0) {
                profitMargin = ((double) product.getProfit() / product.getBuyingPrice()) * 100;
            }
            holder.profitMarginText.setText(String.format("%.1f%% margin", profitMargin));

            // FIXED: Set profit color using Color.parseColor
            if (product.getProfit() > 0) {
                holder.profitText.setTextColor(Color.parseColor("#4CAF50")); // Green
            } else if (product.getProfit() < 0) {
                holder.profitText.setTextColor(Color.parseColor("#F44336")); // Red
            } else {
                holder.profitText.setTextColor(Color.parseColor("#757575")); // Gray
            }

            // Timestamp information
            holder.timestampText.setText(formatTimestamp(product.getTimestamp()));
            holder.timeAgoText.setText(getTimeAgo(product.getTimestamp()));

            // Click listeners
            holder.editButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onEditProduct(product);
                }
            });

            holder.deleteButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDeleteProduct(product);
                }
            });

            // Expand/Collapse functionality
            final boolean[] isExpanded = {false};
            holder.expandButton.setOnClickListener(v -> {
                isExpanded[0] = !isExpanded[0];

                if (isExpanded[0]) {
                    holder.extraDetailsLayout.setVisibility(View.VISIBLE);
                    holder.expandButton.setRotation(180);
                    holder.expandButton.setContentDescription("Collapse details");
                } else {
                    holder.extraDetailsLayout.setVisibility(View.GONE);
                    holder.expandButton.setRotation(0);
                    holder.expandButton.setContentDescription("Expand details");
                }
            });

            // Card click listener for details
            holder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onProductDetails(product);
                }
            });

            // REMOVED: Long click popup menu (since resource doesn't exist)
        }

        @Override
        public int getItemCount() {
            return productList.size();
        }

        // ViewHolder class
        class ViewHolder extends RecyclerView.ViewHolder {
            // Basic information
            TextView productNameText, productWeightText, productFlavourText, productIdText;

            // Pricing information
            TextView buyingPriceText, sellingPriceText, profitText, profitMarginText;

            // Timestamp information
            TextView timestampText, timeAgoText;

            // Action buttons
            ImageView editButton, deleteButton, expandButton;

            // Layouts
            LinearLayout extraDetailsLayout;
            CardView cardView;

            ViewHolder(View itemView) {
                super(itemView);

                // Initialize basic information views
                productNameText = itemView.findViewById(R.id.productNameText);
                productWeightText = itemView.findViewById(R.id.productWeightText);
                productFlavourText = itemView.findViewById(R.id.productFlavourText);
                productIdText = itemView.findViewById(R.id.productIdText);

                // Initialize pricing views
                buyingPriceText = itemView.findViewById(R.id.buyingPriceText);
                sellingPriceText = itemView.findViewById(R.id.sellingPriceText);
                profitText = itemView.findViewById(R.id.profitText);
                profitMarginText = itemView.findViewById(R.id.profitMarginText);

                // Initialize timestamp views
                timestampText = itemView.findViewById(R.id.timestampText);
                timeAgoText = itemView.findViewById(R.id.timeAgoText);

                // Initialize action buttons
                editButton = itemView.findViewById(R.id.editButton);
                deleteButton = itemView.findViewById(R.id.deleteButton);
                expandButton = itemView.findViewById(R.id.expandButton);

                // Initialize layouts
                extraDetailsLayout = itemView.findViewById(R.id.extraDetailsLayout);
                cardView = itemView.findViewById(R.id.cardView);
            }
        }

        // REMOVED: showQuickActionsPopup method (since menu resource doesn't exist)

        // Filter products based on search query
        public void filter(String query) {
            List<Product> filteredList = new ArrayList<>();

            for (Product product : originalProductList) {
                if (product.getProductName().toLowerCase().contains(query.toLowerCase()) ||
                        product.getWeight().toLowerCase().contains(query.toLowerCase()) ||
                        product.getFlavour().toLowerCase().contains(query.toLowerCase())) {
                    filteredList.add(product);
                }
            }

            updateList(filteredList);
        }
    }
}