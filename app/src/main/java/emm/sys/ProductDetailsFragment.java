package emm.sys;

import android.database.Cursor;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ProductDetailsFragment extends Fragment {

    private EditText txtProductName, txtBuyingPrice, txtSellingPrice;
    private Spinner spinnerWeight, spinnerFlavour;
    private Button saveButton, updatePriceButton, viewProductsButton, searchButton;
    private DBHelper dbHelper;

    private String selectedWeight = "";
    private String selectedFlavour = "";
    private List<String> existingProducts = new ArrayList<>();
    private ArrayAdapter<String> productNameAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_product_details, container, false);

        // Initialize database helper
        dbHelper = new DBHelper(getActivity());

        // Initialize views
        txtProductName = view.findViewById(R.id.txtProductName);
        txtBuyingPrice = view.findViewById(R.id.txtBuyingPrice);
        txtSellingPrice = view.findViewById(R.id.txtSellingPrice);
        spinnerWeight = view.findViewById(R.id.spinnerWeight);
        spinnerFlavour = view.findViewById(R.id.spinnerFlavour);
        saveButton = view.findViewById(R.id.saveButton);

        // Setup spinners
        setupSpinners();

        // Load existing products for suggestions
        loadExistingProducts();

        // Setup text watcher for product name suggestions
        setupProductNameSuggestions();

        // Setup save button listener
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveProductDetails();
            }
        });

        // Add Update Price button
        addUpdatePriceButton(view);

        // Add View Products button
        addViewProductsButton(view);

        // Add Search button
        addSearchButton(view);

        return view;
    }

    private void setupSpinners() {
        // Setup weight spinner
        ArrayAdapter<CharSequence> weightAdapter = ArrayAdapter.createFromResource(
                getActivity(),
                R.array.weight_array,
                android.R.layout.simple_spinner_item
        );
        weightAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerWeight.setAdapter(weightAdapter);

        // Setup flavour spinner
        ArrayAdapter<CharSequence> flavourAdapter = ArrayAdapter.createFromResource(
                getActivity(),
                R.array.flavour_array,
                android.R.layout.simple_spinner_item
        );
        flavourAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFlavour.setAdapter(flavourAdapter);

        // Set listeners to capture selected values
        spinnerWeight.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedWeight = parent.getItemAtPosition(position).toString();
                // Auto-fill prices if product exists
                autoFillPricesIfExists();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedWeight = "";
            }
        });

        spinnerFlavour.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedFlavour = parent.getItemAtPosition(position).toString();
                // Auto-fill prices if product exists
                autoFillPricesIfExists();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedFlavour = "";
            }
        });
    }

    private void loadExistingProducts() {
        existingProducts.clear();
        List<String> allProducts = getAllProductNames();
        existingProducts.addAll(allProducts);

        // Create adapter for suggestions
        productNameAdapter = new ArrayAdapter<>(
                getActivity(),
                android.R.layout.simple_dropdown_item_1line,
                existingProducts
        );
    }

    private List<String> getAllProductNames() {
        List<String> productNames = new ArrayList<>();
        Cursor cursor = dbHelper.getAllProductDetails();

        if (cursor != null) {
            Set<String> uniqueNames = new HashSet<>(); // Use Set to avoid duplicates
            while (cursor.moveToNext()) {
                String productName = cursor.getString(cursor.getColumnIndexOrThrow("product_name"));
                uniqueNames.add(productName);
            }
            productNames.addAll(uniqueNames);
            cursor.close();
        }
        return productNames;
    }

    private void setupProductNameSuggestions() {
        txtProductName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String input = s.toString().trim();
                if (input.length() > 1) {
                    filterProductSuggestions(input);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void filterProductSuggestions(String input) {
        List<String> filteredProducts = new ArrayList<>();
        for (String product : existingProducts) {
            if (product.toLowerCase().contains(input.toLowerCase())) {
                filteredProducts.add(product);
            }
        }

        if (!filteredProducts.isEmpty() && getActivity() != null) {
            // Show suggestions in a dialog
            showSuggestionsDialog(filteredProducts);
        }
    }

    private void showSuggestionsDialog(List<String> suggestions) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Select Product");

        builder.setItems(suggestions.toArray(new String[0]), (dialog, which) -> {
            String selectedProduct = suggestions.get(which);
            txtProductName.setText(selectedProduct);
            // Auto-fill other fields if product exists
            autoFillProductDetails(selectedProduct);
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void autoFillProductDetails(String productName) {
        // Get product details from database
        Cursor cursor = dbHelper.getProductByName(productName);

        if (cursor != null && cursor.moveToFirst()) {
            String weight = cursor.getString(cursor.getColumnIndexOrThrow("weight"));
            String flavour = cursor.getString(cursor.getColumnIndexOrThrow("flavour"));
            int buyingPrice = cursor.getInt(cursor.getColumnIndexOrThrow("buying_price"));
            int sellingPrice = cursor.getInt(cursor.getColumnIndexOrThrow("selling_price"));

            // Set weight spinner
            ArrayAdapter adapter = (ArrayAdapter) spinnerWeight.getAdapter();
            int weightPosition = adapter.getPosition(weight);
            if (weightPosition >= 0) {
                spinnerWeight.setSelection(weightPosition);
            }

            // Set flavour spinner
            adapter = (ArrayAdapter) spinnerFlavour.getAdapter();
            int flavourPosition = adapter.getPosition(flavour);
            if (flavourPosition >= 0) {
                spinnerFlavour.setSelection(flavourPosition);
            }

            // Set prices
            txtBuyingPrice.setText(String.valueOf(buyingPrice));
            txtSellingPrice.setText(String.valueOf(sellingPrice));

            cursor.close();
        }
    }

    private void autoFillPricesIfExists() {
        String productName = txtProductName.getText().toString().trim();
        if (!productName.isEmpty() && !selectedWeight.isEmpty() && !selectedFlavour.isEmpty()) {
            Cursor cursor = dbHelper.getProductByName(productName);
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    String weight = cursor.getString(cursor.getColumnIndexOrThrow("weight"));
                    String flavour = cursor.getString(cursor.getColumnIndexOrThrow("flavour"));

                    if (weight.equals(selectedWeight) && flavour.equals(selectedFlavour)) {
                        int buyingPrice = cursor.getInt(cursor.getColumnIndexOrThrow("buying_price"));
                        int sellingPrice = cursor.getInt(cursor.getColumnIndexOrThrow("selling_price"));
                        txtBuyingPrice.setText(String.valueOf(buyingPrice));
                        txtSellingPrice.setText(String.valueOf(sellingPrice));
                        break;
                    }
                }
                cursor.close();
            }
        }
    }

    private void saveProductDetails() {
        // Get input values
        String productName = txtProductName.getText().toString().trim();
        String buyingPriceStr = txtBuyingPrice.getText().toString().trim();
        String sellingPriceStr = txtSellingPrice.getText().toString().trim();

        // Validate inputs
        if (productName.isEmpty()) {
            txtProductName.setError("Please enter product name");
            txtProductName.requestFocus();
            return;
        }

        if (selectedWeight.isEmpty()) {
            Toast.makeText(getActivity(), "Please select weight", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedFlavour.isEmpty()) {
            Toast.makeText(getActivity(), "Please select flavour", Toast.LENGTH_SHORT).show();
            return;
        }

        if (buyingPriceStr.isEmpty()) {
            txtBuyingPrice.setError("Please enter buying price");
            txtBuyingPrice.requestFocus();
            return;
        }

        if (sellingPriceStr.isEmpty()) {
            txtSellingPrice.setError("Please enter selling price");
            txtSellingPrice.requestFocus();
            return;
        }

        try {
            // Convert prices to integers
            int buyingPrice = Integer.parseInt(buyingPriceStr);
            int sellingPrice = Integer.parseInt(sellingPriceStr);

            // Check if prices are valid
            if (buyingPrice <= 0) {
                txtBuyingPrice.setError("Buying price must be greater than 0");
                txtBuyingPrice.requestFocus();
                return;
            }

            if (sellingPrice <= 0) {
                txtSellingPrice.setError("Selling price must be greater than 0");
                txtSellingPrice.requestFocus();
                return;
            }

            if (sellingPrice <= buyingPrice) {
                txtSellingPrice.setError("Selling price must be greater than buying price");
                txtSellingPrice.requestFocus();
                return;
            }

            // Check if product with same details already exists
            boolean productExists = dbHelper.checkProductDetailsExists(productName, selectedWeight, selectedFlavour);

            if (productExists) {
                // Ask user if they want to update prices
                showUpdatePriceConfirmation(productName, selectedWeight, selectedFlavour, buyingPrice, sellingPrice);
            } else {
                // Save to product_details table
                boolean success = dbHelper.insertProductDetails(productName, selectedWeight, selectedFlavour, buyingPrice, sellingPrice);

                if (success) {
                    Toast.makeText(getActivity(),
                            "Product details saved successfully!\n" +
                                    "Product: " + productName + "\n" +
                                    "Weight: " + selectedWeight + "\n" +
                                    "Flavour: " + selectedFlavour + "\n" +
                                    "Buying Price: KES " + buyingPrice + "\n" +
                                    "Selling Price: KES " + sellingPrice,
                            Toast.LENGTH_LONG).show();

                    // Add to existing products list if not already there
                    if (!existingProducts.contains(productName)) {
                        existingProducts.add(productName);
                        if (productNameAdapter != null) {
                            productNameAdapter.notifyDataSetChanged();
                        }
                    }

                    // Clear form for next entry
                    clearForm();

                } else {
                    Toast.makeText(getActivity(), "Failed to save product details", Toast.LENGTH_SHORT).show();
                }
            }

        } catch (NumberFormatException e) {
            Toast.makeText(getActivity(), "Please enter valid numbers for prices", Toast.LENGTH_SHORT).show();
        }
    }

    private void showUpdatePriceConfirmation(String productName, String weight, String flavour, int newBuyingPrice, int newSellingPrice) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Product Already Exists");
        builder.setMessage("Product '" + productName + "' with weight '" + weight +
                "' and flavour '" + flavour + "' already exists.\n\n" +
                "Do you want to update the prices?\n" +
                "Buying Price: KES " + newBuyingPrice + "\n" +
                "Selling Price: KES " + newSellingPrice);

        builder.setPositiveButton("Update Prices", (dialog, which) -> {
            updateExistingProductPrices(productName, weight, flavour, newBuyingPrice, newSellingPrice);
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> {
            dialog.dismiss();
        });

        builder.show();
    }

    private void updateExistingProductPrices(String productName, String weight, String flavour, int newBuyingPrice, int newSellingPrice) {
        boolean success = dbHelper.updateProductPrices(productName, weight, flavour, newBuyingPrice, newSellingPrice);

        if (success) {
            Toast.makeText(getActivity(),
                    "Prices updated successfully!\n" +
                            "Product: " + productName + "\n" +
                            "New Buying Price: KES " + newBuyingPrice + "\n" +
                            "New Selling Price: KES " + newSellingPrice,
                    Toast.LENGTH_LONG).show();
            clearForm();
        } else {
            Toast.makeText(getActivity(), "Failed to update prices", Toast.LENGTH_SHORT).show();
        }
    }

    private void addUpdatePriceButton(View view) {
        // Create Update Price button
        updatePriceButton = new Button(getActivity());
        updatePriceButton.setText("Update Prices");
        updatePriceButton.setBackgroundResource(R.drawable.button_bg);
        updatePriceButton.setTextColor(getResources().getColor(android.R.color.white));

        // Set layout params
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 10, 0, 10);
        updatePriceButton.setLayoutParams(params);

        // Add click listener
        updatePriceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updatePricesForExistingProduct();
            }
        });

        // Find the layout and add button
        ViewGroup mainLayout = (ViewGroup) view;
        if (mainLayout instanceof LinearLayout) {
            ((LinearLayout) mainLayout).addView(updatePriceButton);
        } else {
            // Try to find a LinearLayout in the view
            LinearLayout linearLayout = view.findViewById(R.id.fragmentContainer);
            if (linearLayout != null) {
                linearLayout.addView(updatePriceButton);
            }
        }
    }

    private void updatePricesForExistingProduct() {
        String productName = txtProductName.getText().toString().trim();
        String buyingPriceStr = txtBuyingPrice.getText().toString().trim();
        String sellingPriceStr = txtSellingPrice.getText().toString().trim();

        if (productName.isEmpty()) {
            txtProductName.setError("Please enter product name");
            txtProductName.requestFocus();
            return;
        }

        if (selectedWeight.isEmpty()) {
            Toast.makeText(getActivity(), "Please select weight", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedFlavour.isEmpty()) {
            Toast.makeText(getActivity(), "Please select flavour", Toast.LENGTH_SHORT).show();
            return;
        }

        if (buyingPriceStr.isEmpty()) {
            txtBuyingPrice.setError("Please enter new buying price");
            txtBuyingPrice.requestFocus();
            return;
        }

        if (sellingPriceStr.isEmpty()) {
            txtSellingPrice.setError("Please enter new selling price");
            txtSellingPrice.requestFocus();
            return;
        }

        try {
            int newBuyingPrice = Integer.parseInt(buyingPriceStr);
            int newSellingPrice = Integer.parseInt(sellingPriceStr);

            if (newBuyingPrice <= 0) {
                txtBuyingPrice.setError("Buying price must be greater than 0");
                txtBuyingPrice.requestFocus();
                return;
            }

            if (newSellingPrice <= 0) {
                txtSellingPrice.setError("Selling price must be greater than 0");
                txtSellingPrice.requestFocus();
                return;
            }

            if (newSellingPrice <= newBuyingPrice) {
                txtSellingPrice.setError("Selling price must be greater than buying price");
                txtSellingPrice.requestFocus();
                return;
            }

            updateExistingProductPrices(productName, selectedWeight, selectedFlavour, newBuyingPrice, newSellingPrice);

        } catch (NumberFormatException e) {
            Toast.makeText(getActivity(), "Please enter valid numbers", Toast.LENGTH_SHORT).show();
        }
    }

    private void addViewProductsButton(View view) {
        // Create View Products button
        viewProductsButton = new Button(getActivity());
        viewProductsButton.setText("View All Products");
        viewProductsButton.setBackgroundResource(R.drawable.button_bg);
        viewProductsButton.setTextColor(getResources().getColor(android.R.color.white));

        // Set layout params
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 10, 0, 10);
        viewProductsButton.setLayoutParams(params);

        // Add click listener
        viewProductsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAllProducts();
            }
        });

        // Find the layout and add button
        ViewGroup mainLayout = (ViewGroup) view;
        if (mainLayout instanceof LinearLayout) {
            ((LinearLayout) mainLayout).addView(viewProductsButton);
        } else {
            LinearLayout linearLayout = view.findViewById(R.id.fragmentContainer);
            if (linearLayout != null) {
                linearLayout.addView(viewProductsButton);
            }
        }
    }

    private void showAllProducts() {
        Cursor cursor = dbHelper.getAllProductDetails();

        if (cursor == null || cursor.getCount() == 0) {
            Toast.makeText(getActivity(), "No products found in database", Toast.LENGTH_SHORT).show();
            return;
        }

        StringBuilder productsList = new StringBuilder();
        productsList.append("ALL PRODUCTS:\n\n");

        while (cursor.moveToNext()) {
            String productName = cursor.getString(cursor.getColumnIndexOrThrow("product_name"));
            String weight = cursor.getString(cursor.getColumnIndexOrThrow("weight"));
            String flavour = cursor.getString(cursor.getColumnIndexOrThrow("flavour"));
            int buyingPrice = cursor.getInt(cursor.getColumnIndexOrThrow("buying_price"));
            int sellingPrice = cursor.getInt(cursor.getColumnIndexOrThrow("selling_price"));

            productsList.append("Product: ").append(productName).append("\n")
                    .append("Weight: ").append(weight).append("\n")
                    .append("Flavour: ").append(flavour).append("\n")
                    .append("Buying Price: KES ").append(buyingPrice).append("\n")
                    .append("Selling Price: KES ").append(sellingPrice).append("\n")
                    .append("----------------\n");
        }
        cursor.close();

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Product Catalog");
        builder.setMessage(productsList.toString());
        builder.setPositiveButton("OK", null);
        builder.show();
    }

    private void addSearchButton(View view) {
        // Create Search button
        searchButton = new Button(getActivity());
        searchButton.setText("Search Product");
        searchButton.setBackgroundResource(R.drawable.button_bg);
        searchButton.setTextColor(getResources().getColor(android.R.color.white));

        // Set layout params
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 10, 0, 10);
        searchButton.setLayoutParams(params);

        // Add click listener
        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSearchDialog();
            }
        });

        // Find the layout and add button
        ViewGroup mainLayout = (ViewGroup) view;
        if (mainLayout instanceof LinearLayout) {
            ((LinearLayout) mainLayout).addView(searchButton);
        } else {
            LinearLayout linearLayout = view.findViewById(R.id.fragmentContainer);
            if (linearLayout != null) {
                linearLayout.addView(searchButton);
            }
        }
    }

    private void showSearchDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Search Product");

        // Create input field
        final EditText input = new EditText(getActivity());
        input.setHint("Enter product name to search");
        builder.setView(input);

        builder.setPositiveButton("Search", (dialog, which) -> {
            String searchTerm = input.getText().toString().trim();
            if (!searchTerm.isEmpty()) {
                searchProduct(searchTerm);
            } else {
                Toast.makeText(getActivity(), "Please enter search term", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void searchProduct(String searchTerm) {
        Cursor cursor = dbHelper.getAllProductDetails();

        if (cursor == null || cursor.getCount() == 0) {
            Toast.makeText(getActivity(), "No products found", Toast.LENGTH_SHORT).show();
            return;
        }

        StringBuilder searchResults = new StringBuilder();
        searchResults.append("SEARCH RESULTS for '").append(searchTerm).append("':\n\n");
        int resultCount = 0;

        while (cursor.moveToNext()) {
            String productName = cursor.getString(cursor.getColumnIndexOrThrow("product_name"));

            if (productName.toLowerCase().contains(searchTerm.toLowerCase())) {
                String weight = cursor.getString(cursor.getColumnIndexOrThrow("weight"));
                String flavour = cursor.getString(cursor.getColumnIndexOrThrow("flavour"));
                int buyingPrice = cursor.getInt(cursor.getColumnIndexOrThrow("buying_price"));
                int sellingPrice = cursor.getInt(cursor.getColumnIndexOrThrow("selling_price"));

                searchResults.append("Product: ").append(productName).append("\n")
                        .append("Weight: ").append(weight).append("\n")
                        .append("Flavour: ").append(flavour).append("\n")
                        .append("Buying Price: KES ").append(buyingPrice).append("\n")
                        .append("Selling Price: KES ").append(sellingPrice).append("\n")
                        .append("----------------\n");
                resultCount++;
            }
        }
        cursor.close();

        if (resultCount == 0) {
            searchResults.append("No products found matching '").append(searchTerm).append("'");
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Search Results (" + resultCount + " found)");
        builder.setMessage(searchResults.toString());
        builder.setPositiveButton("OK", null);
        builder.show();
    }

    private void clearForm() {
        txtProductName.setText("");
        txtBuyingPrice.setText("");
        txtSellingPrice.setText("");
        spinnerWeight.setSelection(0);
        spinnerFlavour.setSelection(0);
        txtProductName.requestFocus();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) {
            dbHelper.close();
        }
    }
}