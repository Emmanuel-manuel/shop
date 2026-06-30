package emm.sys;

import android.database.Cursor;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;

public class EmployeeReceiveGoodsFragment extends Fragment {

    // GLOBAL VARIABLES
    private Spinner spinnerProduct, spinnerWeight, spinnerFlavour;
    private EditText txtProductName, txtQty, txtStation;
    private Button saveButton;
    private DBHelper dbHelper;

    private String assigneeEmail; // Will hold the logged-in user's email

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_employee_receive_goods, container, false);

        // Initialize views
        spinnerProduct = view.findViewById(R.id.spinnerProduct);
        spinnerWeight = view.findViewById(R.id.spinnerWeight);
        spinnerFlavour = view.findViewById(R.id.spinnerFlavour);
        txtProductName = view.findViewById(R.id.txtProductName);
        txtQty = view.findViewById(R.id.txtQty);
        txtStation = view.findViewById(R.id.txtStation);
        saveButton = view.findViewById(R.id.saveButton);

        // Initialize DBHelper
        dbHelper = new DBHelper(getActivity());

        // Get the logged-in user's email
        assigneeEmail = LandingPageEmployeeActivity.getCurrentUserEmail();
        if (TextUtils.isEmpty(assigneeEmail)) {
            // Fallback: try to get from arguments if available
            Bundle args = getArguments();
            if (args != null) {
                assigneeEmail = args.getString("USER_EMAIL");
            }
        }

        // Setup spinners
        setupSpinners();

        // Setup listeners
        setupSpinnerListeners();
        setupButtonListener();

        return view;
    }

    private void setupSpinners() {
        // Setup product spinner - get products from inventory with available balance
        setupProductSpinner();

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
    }

    private void setupProductSpinner() {
        // Get products from product_details table
        List<String> productNames = dbHelper.getAllProductNamesFromProductDetails();

        if (productNames.isEmpty()) {
            productNames.add("No products available");
        } else {
            // Add "Select Product" as first item
            productNames.add(0, "Select Product");
        }

        // Create adapter
        ArrayAdapter<String> productAdapter = new ArrayAdapter<>(
                getActivity(),
                android.R.layout.simple_spinner_item,
                productNames
        );
        productAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerProduct.setAdapter(productAdapter);
    }

    private void setupSpinnerListeners() {
        spinnerProduct.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedProduct = parent.getItemAtPosition(position).toString();

                if (!selectedProduct.equals("Select Product") &&
                        !selectedProduct.equals("No products available")) {

                    // Get product details from product_details table
                    Cursor cursor = dbHelper.getProductDetailsFromProductDetails(selectedProduct);

                    if (cursor != null && cursor.moveToFirst()) {
                        try {
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
                            e.printStackTrace();
                        } finally {
                            cursor.close();
                        }
                    } else {
                        // If product not found in product_details, reset spinners
                        spinnerWeight.setSelection(0);
                        spinnerFlavour.setSelection(0);
                    }

                    txtProductName.setText(selectedProduct);
                } else {
                    // Reset spinners when "Select Product" is chosen
                    spinnerWeight.setSelection(0);
                    spinnerFlavour.setSelection(0);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                spinnerWeight.setSelection(0);
                spinnerFlavour.setSelection(0);
            }
        });
    }

    private void setupButtonListener() {
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                receiveGoods();
            }
        });
    }

    private void receiveGoods() {
        // Get values from form
        String productName = txtProductName.getText().toString().trim();
        String weight = spinnerWeight.getSelectedItem().toString();
        String flavour = spinnerFlavour.getSelectedItem().toString();
        String quantityStr = txtQty.getText().toString().trim();
        String station = txtStation.getText().toString().trim();

        // Validate inputs
        if (!validateInputs(productName, weight, flavour, quantityStr, station)) {
            return;
        }

        int quantity = Integer.parseInt(quantityStr);

        // Check if assignee email is available
        if (TextUtils.isEmpty(assigneeEmail)) {
            Toast.makeText(getActivity(), "User email not found. Please login again.", Toast.LENGTH_LONG).show();
            return;
        }

        // Check if product already exists in today's inventory
        if (dbHelper.checkInventoryExists(productName, weight, flavour)) {
            Toast.makeText(getActivity(),
                    productName + " (" + weight + ", " + flavour + ") already received today!",
                    Toast.LENGTH_LONG).show();
            return;
        }

        // Insert into inventory table
        boolean isInserted = dbHelper.insertInventory(productName, weight, flavour, quantity);

        if (isInserted) {
            // Get the current balance (which equals quantity for new receipt)
            int newBalance = dbHelper.getSpecificProductBalance(productName, weight, flavour);

            // Insert into issue_goods table with employee's email as assignee
            boolean issuedInserted = dbHelper.insertIssuedGoods(
                    assigneeEmail,    // Assignee is the logged-in employee's email
                    productName,
                    weight,
                    flavour,
                    quantity,
                    station,
                    newBalance
            );

            // *** NEW: Insert into emp_received_goods table ***
            boolean empReceivedInserted = dbHelper.insertEmployeeReceivedGoods(
                    assigneeEmail,    // Assignee is the logged-in employee's email
                    productName,
                    weight,
                    flavour,
                    quantity,
                    station
            );

            if (issuedInserted && empReceivedInserted) {
                String message = "✓ Stock received and recorded successfully!\n" +
                        "Product: " + productName + "\n" +
                        "Quantity: " + quantity + "\n" +
                        "Station: " + station + "\n" +
                        "Assignee: " + assigneeEmail;
                Toast.makeText(getActivity(), message, Toast.LENGTH_LONG).show();
                clearForm();
                // Refresh product spinner
                setupProductSpinner();
            } else if (issuedInserted) {
                Toast.makeText(getActivity(), "Stock saved but failed to record issuance in one table", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getActivity(), "Failed to record issuance", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(getActivity(), "Failed to receive stock", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean validateInputs(String productName, String weight,
                                   String flavour, String quantityStr, String station) {
        if (productName.isEmpty() || productName.equals("Select Product") ||
                productName.equals("No products available")) {
            txtProductName.setError("Product name is required");
            Toast.makeText(getActivity(), "Please enter or select a Product", Toast.LENGTH_SHORT).show();
            txtProductName.requestFocus();
            return false;
        }

        if (weight.equals("Select Weight")) {
            Toast.makeText(getActivity(), "Please select a weight", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (flavour.equals("Select Type/Flavour")) {
            Toast.makeText(getActivity(), "Please select a flavour", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (quantityStr.isEmpty()) {
            txtQty.setError("Quantity is required");
            txtQty.requestFocus();
            return false;
        }

        if (station.isEmpty()) {
            txtStation.setError("Station is required");
            txtStation.requestFocus();
            return false;
        }

        try {
            int quantity = Integer.parseInt(quantityStr);
            if (quantity <= 0) {
                txtQty.setError("Quantity must be positive");
                return false;
            }
        } catch (NumberFormatException e) {
            txtQty.setError("Enter a valid number");
            txtQty.requestFocus();
            return false;
        }

        return true;
    }

    private void clearForm() {
        txtProductName.setText("");
        txtQty.setText("");
        txtStation.setText("");
        spinnerProduct.setSelection(0);
        spinnerWeight.setSelection(0);
        spinnerFlavour.setSelection(0);
        // Clear any error states
        txtProductName.setError(null);
        txtQty.setError(null);
        txtStation.setError(null);

        // Request focus to the first input field
        txtProductName.requestFocus();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh product list when fragment is resumed
        setupProductSpinner();
    }

    @Override
    public void onDestroy() {
        if (dbHelper != null) {
            dbHelper.close();
        }
        super.onDestroy();
    }
}