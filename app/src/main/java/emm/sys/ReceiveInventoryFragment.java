package emm.sys;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;

public class ReceiveInventoryFragment extends Fragment {

    // Global Variables
    private Spinner spinnerProduct, spinnerWeight, spinnerFlavour;
    private EditText txtProductName, txtQty;
    private Button saveButton;
    private DBHelper dbHelper;
    private List<String> productNames = new ArrayList<>();
    private ArrayAdapter<String> productAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_receive_inventory, container, false);

        // Initialize views
        spinnerProduct = view.findViewById(R.id.spinnerProduct);
        spinnerWeight = view.findViewById(R.id.spinnerWeight);
        spinnerFlavour = view.findViewById(R.id.spinnerFlavour);
        txtProductName = view.findViewById(R.id.txtProductName);
        txtQty = view.findViewById(R.id.txtQty);
        saveButton = view.findViewById(R.id.saveButton);
        dbHelper = new DBHelper(getActivity());

        // Setup spinners
        setupProductSpinner();
        setupWeightAndFlavourSpinners();

        // Set up spinner selection listener
        setupSpinnerListeners();

        saveButton.setOnClickListener(v -> saveInventoryData());

        return view;
    }

    private void setupProductSpinner() {
        // Get product names from database
        productNames = dbHelper.getAllProductNames();

        // Add "Select Product" as first item
        productNames.add(0, "Select Product");

        // Create adapter
        productAdapter = new ArrayAdapter<>(
                getActivity(),
                android.R.layout.simple_spinner_item,
                productNames
        );
        productAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // Set adapter to spinner
        spinnerProduct.setAdapter(productAdapter);
    }

    private void setupWeightAndFlavourSpinners() {
        // Setup weight spinner from resources
        ArrayAdapter<CharSequence> weightAdapter = ArrayAdapter.createFromResource(
                getActivity(),
                R.array.weight_array,
                android.R.layout.simple_spinner_item
        );
        weightAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerWeight.setAdapter(weightAdapter);

        // Setup flavour spinner from resources
        ArrayAdapter<CharSequence> flavourAdapter = ArrayAdapter.createFromResource(
                getActivity(),
                R.array.flavour_array,
                android.R.layout.simple_spinner_item
        );
        flavourAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFlavour.setAdapter(flavourAdapter);
    }

    private void setupSpinnerListeners() {
        // When product is selected from spinner, auto-fill weight and flavour if available
        spinnerProduct.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0) { // Not "Select Product"
                    String selectedProduct = productNames.get(position);
                    // Clear manual entry when selecting from spinner
                    txtProductName.setText("");

                    // Try to get weight and flavour for this product from product_details
                    autoFillProductDetails(selectedProduct);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });

        // When user types in manual product name, clear spinner selection
        txtProductName.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0) {
                    // Clear spinner selection when user starts typing
                    spinnerProduct.setSelection(0);
                }
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });
    }

    private void autoFillProductDetails(String productName) {
        // Get product details from database
        android.database.Cursor cursor = dbHelper.getProductByName(productName);

        if (cursor != null && cursor.moveToFirst()) {
            try {
                // Get weight and flavour from first matching product
                String weight = cursor.getString(cursor.getColumnIndexOrThrow("weight"));
                String flavour = cursor.getString(cursor.getColumnIndexOrThrow("flavour"));

                // Auto-select weight in spinner
                ArrayAdapter adapter = (ArrayAdapter) spinnerWeight.getAdapter();
                int weightPosition = adapter.getPosition(weight);
                if (weightPosition >= 0) {
                    spinnerWeight.setSelection(weightPosition);
                }

                // Auto-select flavour in spinner
                adapter = (ArrayAdapter) spinnerFlavour.getAdapter();
                int flavourPosition = adapter.getPosition(flavour);
                if (flavourPosition >= 0) {
                    spinnerFlavour.setSelection(flavourPosition);
                }
            } catch (Exception e) {
                // Product might not have weight/flavour in database
                // Keep default selections
            } finally {
                cursor.close();
            }
        }
    }

    private void saveInventoryData() {
        // Get product name (manual entry takes priority)
        String weight = spinnerWeight.getSelectedItem().toString();
        String flavour = spinnerFlavour.getSelectedItem().toString();
        String productName = txtProductName.getText().toString().trim();

        // If manual entry is empty, use spinner selection
        if (productName.isEmpty()) {
            int selectedPosition = spinnerProduct.getSelectedItemPosition();
            if (selectedPosition > 0) {
                productName = spinnerProduct.getSelectedItem().toString();
            }
        }

        // =======VALIDATION BLOCK ======
        if (productName.isEmpty()) {
            txtProductName.setError("Product name is required");
            Toast.makeText(getActivity(), "Please enter or select a product", Toast.LENGTH_SHORT).show();
            txtProductName.requestFocus();
            return;
        }

        if (productName.equals("Select Product")) {
            txtProductName.setError("Product name is required");
            Toast.makeText(getActivity(), "Please enter or select a product", Toast.LENGTH_SHORT).show();
            txtProductName.requestFocus();
            return;
        }

        // Validate quantity
        String quantityStr = txtQty.getText().toString().trim();
        if (quantityStr.isEmpty()) {
            txtQty.setError("Please enter quantity");
            txtQty.requestFocus();
            return;
        }
        int quantity;
        try {
            quantity = Integer.parseInt(quantityStr);
            if (quantity <= 0) {
                txtQty.setError("Quantity must be positive");
                txtQty.requestFocus();
                return;
            }
        } catch (NumberFormatException e) {
            txtQty.setError("Invalid quantity");
            txtQty.requestFocus();
            return;
        }

        // ====== DUPLICATE CHECK ======
        if (dbHelper.checkInventoryExists(productName, weight, flavour)) {
            Toast.makeText(getActivity(),
                    "You have already received '" + productName + "' (" + weight + ", " + flavour + ") today",
                    Toast.LENGTH_LONG).show();
            clearForm();
            return;
        }

        // Save to database - balance will be automatically set equal to quantity
        boolean isSuccess = dbHelper.insertInventory(productName, weight, flavour, quantity);

        if (isSuccess) {
            // Display balance information
            int currentBalance = dbHelper.getProductBalance(productName);
            String message = "✅ Inventory saved successfully!\n" +
                    "Product: " + productName + "\n" +
                    "Weight: " + weight + "\n" +
                    "Flavour: " + flavour + "\n" +
                    "Quantity: " + quantity + "\n" +
                    "Balance: " + currentBalance;
            Toast.makeText(getActivity(), message, Toast.LENGTH_LONG).show();
            clearForm();

            // Refresh product list in spinner if new product was added manually
            if (spinnerProduct.getSelectedItemPosition() == 0) {
                refreshProductSpinner();
            }
        } else {
            Toast.makeText(getActivity(), "Failed to save inventory", Toast.LENGTH_SHORT).show();
        }
    }

    private void refreshProductSpinner() {
        // Refresh the product list from database
        List<String> updatedProducts = dbHelper.getAllProductNames();
        updatedProducts.add(0, "Select Product");

        productAdapter.clear();
        productAdapter.addAll(updatedProducts);
        productAdapter.notifyDataSetChanged();
    }

    private void clearForm() {
        txtProductName.setText("");
        txtQty.setText("");
        spinnerProduct.setSelection(0);
        spinnerWeight.setSelection(0);
        spinnerFlavour.setSelection(0);
        txtProductName.requestFocus();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh product list when fragment is resumed
        refreshProductSpinner();
    }

    @Override
    public void onDestroy() {
        if (dbHelper != null) {
            dbHelper.close();
        }
        super.onDestroy();
    }
}