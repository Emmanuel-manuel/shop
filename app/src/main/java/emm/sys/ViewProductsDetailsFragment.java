package emm.sys;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.database.Cursor;
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
        productAdapter = new ProductAdapter(productList);
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

        String[] actions = {"Refresh List", "Export Data", "Add New Product", "Delete All Products"};

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
                    case 2: // Add New
                        navigateToAddProduct();
                        break;
                    case 3: // Delete All
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
        // This would require a new method in DBHelper to delete all products
        // For now, show a message
        Toast.makeText(getActivity(),
                "Delete all functionality requires database method implementation",
                Toast.LENGTH_LONG).show();
    }

    private void loadProductData() {
        productList.clear();
        Cursor cursor = dbHelper.getAllProductDetails();

        if (cursor != null && cursor.moveToFirst()) {
            do {
                Product product = extractProductFromCursor(cursor);
                productList.add(product);
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
        product.setPrice(cursor.getInt(cursor.getColumnIndexOrThrow("price")));
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

        // Average price
        int totalPrice = 0;
        for (Product product : productList) {
            totalPrice += product.getPrice();
        }
        int averagePrice = totalPrice / totalProducts;
        averagePriceText.setText("KES " + NumberFormat.getInstance().format(averagePrice));

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
        // Using Collections.sort() instead of List.sort() for API level 23 compatibility
        Collections.sort(productList, new Comparator<Product>() {
            @Override
            public int compare(Product p1, Product p2) {
                return p1.getProductName().compareToIgnoreCase(p2.getProductName());
            }
        });
        productAdapter.notifyDataSetChanged();
    }

    private void sortProductsByPrice() {
        // Using Collections.sort() instead of List.sort() for API level 23 compatibility
        Collections.sort(productList, new Comparator<Product>() {
            @Override
            public int compare(Product p1, Product p2) {
                return Integer.compare(p1.getPrice(), p2.getPrice());
            }
        });
        productAdapter.notifyDataSetChanged();
    }

    private void filterProducts(String query) {
        List<Product> filteredList = new ArrayList<>();

        for (Product product : productList) {
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

        EditText editProductName = dialogView.findViewById(R.id.editProductName);
        EditText editWeight = dialogView.findViewById(R.id.editWeight);
        EditText editFlavour = dialogView.findViewById(R.id.editFlavour);
        EditText editPrice = dialogView.findViewById(R.id.editPrice);

        // Set current values
        editProductName.setText(product.getProductName());
        editWeight.setText(product.getWeight());
        editFlavour.setText(product.getFlavour());
        editPrice.setText(String.valueOf(product.getPrice()));

        builder.setPositiveButton("Update", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String newName = editProductName.getText().toString().trim();
                String newWeight = editWeight.getText().toString().trim();
                String newFlavour = editFlavour.getText().toString().trim();
                String newPriceStr = editPrice.getText().toString().trim();

                if (newName.isEmpty() || newWeight.isEmpty() || newFlavour.isEmpty() || newPriceStr.isEmpty()) {
                    Toast.makeText(getActivity(), "All fields are required", Toast.LENGTH_SHORT).show();
                    return;
                }

                try {
                    int newPrice = Integer.parseInt(newPriceStr);

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

                    // For now, we can only update price with existing DBHelper method
                    // You might need to add a method to update all fields
                    boolean success = dbHelper.updateProductPrice(product.getProductName(),
                            product.getWeight(), product.getFlavour(), newPrice);

                    if (success) {
                        Toast.makeText(getActivity(),
                                "Product price updated successfully",
                                Toast.LENGTH_SHORT).show();
                        loadProductData(); // Refresh list
                    } else {
                        Toast.makeText(getActivity(),
                                "Failed to update product",
                                Toast.LENGTH_SHORT).show();
                    }

                } catch (NumberFormatException e) {
                    Toast.makeText(getActivity(), "Invalid price format", Toast.LENGTH_SHORT).show();
                }
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void deleteProduct(Product product) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Delete Product");
        builder.setMessage("Are you sure you want to delete '" + product.getProductName() + "'?");

        builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // You need to add a delete method in DBHelper
                Toast.makeText(getActivity(),
                        "Delete functionality requires database method implementation",
                        Toast.LENGTH_LONG).show();
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private String formatTimestamp(String timestamp) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());

            Date date = inputFormat.parse(timestamp);
            return outputFormat.format(date);
        } catch (Exception e) {
            return timestamp;
        }
    }

    private String getTimeAgo(String timestamp) {
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
        private int price;
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

        public int getPrice() { return price; }
        public void setPrice(int price) { this.price = price; }

        public String getTimestamp() { return timestamp; }
        public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    }

    // RecyclerView Adapter
    private class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ViewHolder> {
        private List<Product> productList;

        public ProductAdapter(List<Product> productList) {
            this.productList = productList;
        }

        public void updateList(List<Product> newList) {
            this.productList = newList;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.recycler_product_details, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Product product = productList.get(position);

            holder.productNameText.setText(product.getProductName());
            holder.productWeightText.setText(product.getWeight());
            holder.productFlavourText.setText(product.getFlavour());
            holder.productPriceText.setText("KES " + NumberFormat.getInstance().format(product.getPrice()));
            holder.productTimestampText.setText(formatTimestamp(product.getTimestamp()));
            holder.productIdText.setText("#" + String.format("%03d", product.getId()));
            holder.lastUpdatedText.setText(getTimeAgo(product.getTimestamp()));

            // Set click listeners
            holder.editButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    editProduct(product);
                }
            });

            holder.deleteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    deleteProduct(product);
                }
            });

            // Expand/Collapse functionality
            final boolean[] isExpanded = {false};
            holder.expandButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    isExpanded[0] = !isExpanded[0];

                    if (isExpanded[0]) {
                        holder.extraDetailsLayout.setVisibility(View.VISIBLE);
                        holder.expandButton.setRotation(180); // Rotate arrow
                    } else {
                        holder.extraDetailsLayout.setVisibility(View.GONE);
                        holder.expandButton.setRotation(0);
                    }
                }
            });

            // Card click listener
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Toggle expand/collapse on card click
                    holder.expandButton.performClick();
                }
            });
        }

        @Override
        public int getItemCount() {
            return productList.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView productNameText, productWeightText, productFlavourText,
                    productPriceText, productTimestampText, productIdText, lastUpdatedText;
            ImageView editButton, deleteButton, expandButton; // Changed from ImageButton to ImageView
            LinearLayout extraDetailsLayout;

            ViewHolder(View itemView) {
                super(itemView);

                productNameText = itemView.findViewById(R.id.productNameText);
                productWeightText = itemView.findViewById(R.id.productWeightText);
                productFlavourText = itemView.findViewById(R.id.productFlavourText);
                productPriceText = itemView.findViewById(R.id.productPriceText);
                productTimestampText = itemView.findViewById(R.id.productTimestampText);
                productIdText = itemView.findViewById(R.id.productIdText);
                lastUpdatedText = itemView.findViewById(R.id.lastUpdatedText);

                // Change these from ImageButton to ImageView
                editButton = itemView.findViewById(R.id.editButton);
                deleteButton = itemView.findViewById(R.id.deleteButton);
                expandButton = itemView.findViewById(R.id.expandButton);

                extraDetailsLayout = itemView.findViewById(R.id.extraDetailsLayout);
            }
        }
    }
}