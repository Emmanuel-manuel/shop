package emm.sys;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
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

import java.util.List;

public class IssueGoodsFragment extends Fragment {

    // GLOBAL VARIABLES
    private Spinner spinnerAssignee, spinnerProduct, spinnerWeight, spinnerFlavour;
    private EditText txtProductName, txtQty, txtStation;
    private TextView txtProductBal, txtUpdatedBalance;
    private Button saveButton, editButton;
    private DBHelper dbHelper;

    private int currentBalance = 0;
    private boolean isEditMode = false;
    private int issuedId = -1;
    private int originalQuantity = 0;
    private int initialBalance = 0; // This will store the current inventory balance

    // Store the data to be populated
    private String editAssignee, editProductName, editWeight, editFlavour, editStation;
    private int editQuantity;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_issue_goods, container, false);

        // Initialize views
        spinnerAssignee = view.findViewById(R.id.spinnerAssignee);
        spinnerProduct = view.findViewById(R.id.spinnerProduct);
        spinnerWeight = view.findViewById(R.id.spinnerWeight);
        spinnerFlavour = view.findViewById(R.id.spinnerFlavour);
        txtProductName = view.findViewById(R.id.txtProductName);
        txtQty = view.findViewById(R.id.txtQty);
        txtStation = view.findViewById(R.id.txtStation);
        txtProductBal = view.findViewById(R.id.txtProductBal);
        txtUpdatedBalance = view.findViewById(R.id.txtUpdatedBalance);
        saveButton = view.findViewById(R.id.saveButton);
        editButton = view.findViewById(R.id.editButton);

        // Initialize DBHelper
        dbHelper = new DBHelper(getActivity());

        // Setup spinners first
        setupSpinners();

        // Check if we're in edit mode
        checkEditMode();

        // Setup listeners
        setupSpinnerListeners();
        setupTextWatchers();
        setupButtonListeners();

        return view;
    }

    private void setupSpinners() {
        // Setup assignee spinner
        ArrayAdapter<CharSequence> assigneeAdapter = ArrayAdapter.createFromResource(
                getActivity(),
                R.array.product_assignee,
                android.R.layout.simple_spinner_item
        );
        assigneeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerAssignee.setAdapter(assigneeAdapter);

        // Setup product spinner
        ArrayAdapter<CharSequence> productAdapter = ArrayAdapter.createFromResource(
                getActivity(),
                R.array.product_array,
                android.R.layout.simple_spinner_item
        );
        productAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerProduct.setAdapter(productAdapter);

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

    private void checkEditMode() {
        Bundle args = getArguments();
        if (args != null && args.getInt("EDIT_MODE", 0) == 1) {
            isEditMode = true;
            issuedId = args.getInt("ISSUED_ID", -1);

            // Store the edit data
            editAssignee = args.getString("ASSIGNEE");
            editProductName = args.getString("PRODUCT_NAME");
            editWeight = args.getString("WEIGHT");
            editFlavour = args.getString("FLAVOUR");
            editQuantity = args.getInt("QUANTITY");
            editStation = args.getString("STATION");

            // Change UI for edit mode
            saveButton.setText("Update");

            // Populate form with existing data after a short delay to ensure spinners are ready
            spinnerAssignee.post(() -> populateFormData());

            // Hide the edit button since we're already in edit mode
            editButton.setVisibility(View.GONE);
        }
    }

    private void populateFormData() {
        // Set assignee spinner
        setSpinnerSelection(spinnerAssignee, editAssignee);

        // Set product name
        txtProductName.setText(editProductName);

        // Set weight spinner
        setSpinnerSelection(spinnerWeight, editWeight);

        // Set flavour spinner
        setSpinnerSelection(spinnerFlavour, editFlavour);

        // Set quantity and store original quantity
        txtQty.setText(String.valueOf(editQuantity));
        originalQuantity = editQuantity;

        // Set station
        txtStation.setText(editStation);

        // Fetch and display CURRENT inventory balance for the product
        fetchCurrentInventoryBalance(editProductName);

        Toast.makeText(getActivity(), "Editing: " + editProductName, Toast.LENGTH_SHORT).show();
    }

    private void fetchCurrentInventoryBalance(String productName) {
        // Get the CURRENT balance from inventory table for this product
        currentBalance = dbHelper.getProductBalance(productName);
        initialBalance = currentBalance; // Store the initial balance for calculations

        if (currentBalance >= 0) {
            // Display the current inventory balance
            txtProductBal.setText(String.valueOf(currentBalance));

            // Calculate and display the updated balance based on the original quantity
            calculateUpdatedBalanceForEdit();

            enableIssueForm();
        } else {
            txtProductBal.setText("0");
            txtUpdatedBalance.setText("0");
            Toast.makeText(getActivity(), "No balance found for " + productName, Toast.LENGTH_SHORT).show();
        }
    }

    private void calculateUpdatedBalanceForEdit() {
        String quantityStr = txtQty.getText().toString().trim();

        if (quantityStr.isEmpty()) {
            // If no quantity entered, show current balance
            txtUpdatedBalance.setText(String.valueOf(currentBalance));
            return;
        }

        try {
            int newQuantity = Integer.parseInt(quantityStr);

            // NEW LOGIC: initialBalance + (originalQuantity - newQuantity)
            int updatedBalance = initialBalance + (originalQuantity - newQuantity);

            // Update the new balance display
            txtUpdatedBalance.setText(String.valueOf(updatedBalance));

            // Change color if balance goes negative
            if (updatedBalance < 0) {
                txtUpdatedBalance.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                saveButton.setEnabled(false);
            } else {
                txtUpdatedBalance.setTextColor(getResources().getColor(android.R.color.black));
                saveButton.setEnabled(true);
            }

        } catch (NumberFormatException e) {
            txtUpdatedBalance.setText(String.valueOf(currentBalance));
        }
    }

    private void setSpinnerSelection(Spinner spinner, String value) {
        if (value != null && spinner.getAdapter() != null) {
            ArrayAdapter adapter = (ArrayAdapter) spinner.getAdapter();
            for (int i = 0; i < adapter.getCount(); i++) {
                if (adapter.getItem(i).toString().equalsIgnoreCase(value)) {
                    spinner.setSelection(i);
                    break;
                }
            }
        }
    }

    private void setupSpinnerListeners() {
        spinnerProduct.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedProduct = parent.getItemAtPosition(position).toString();
                // Check if a valid product is selected (not "Select Product")
                if (!selectedProduct.equals("Select Product")) {

                    // AUTO-POPULATE WEIGHT FEATURE: Check if product exists in TODAY'S inventory
                    if (dbHelper.productExists(selectedProduct)) {
                        // Get the weight(s) for this product
                        List<String> weights = dbHelper.getAllWeightsForProduct(selectedProduct);

                        if (!weights.isEmpty()) {
                            // Create a new adapter for weights
                            ArrayAdapter<String> weightAdapter = new ArrayAdapter<>(
                                    getActivity(),
                                    android.R.layout.simple_spinner_item,
                                    weights
                            );
                            weightAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                            spinnerWeight.setAdapter(weightAdapter);

                            // If there's only one weight, select it automatically
                            if (weights.size() == 1) {
                                spinnerWeight.setSelection(0);

                                // Also try to get the weight from today's inventory
                                String todayWeight = dbHelper.getProductWeight(selectedProduct);
                                if (todayWeight != null && !todayWeight.isEmpty()) {
                                    setSpinnerSelection(spinnerWeight, todayWeight);
                                }
                            }
                        }
                        else {
                            // No weights found in today's inventory, use default
                            setupWeightSpinnerDefault();
                        }
                    } else {
                        // Product doesn't exist in today's inventory, use default
                        setupWeightSpinnerDefault();

                        // Show warning to user
                        Toast.makeText(getActivity(),
                                selectedProduct + " was not received today. Please receive it first.",
                                Toast.LENGTH_SHORT).show();

                        // Disable the form since product wasn't received today
                        disableIssueForm();
                    }

                    if (isEditMode) {
                        // In edit mode, fetch current inventory balance
                        fetchCurrentInventoryBalance(selectedProduct);
                    } else {
                        // In new issue mode, use today's balance
                        fetchProductBalance(selectedProduct);
                    }
                    // Only auto-fill product name if not in edit mode or if product name is empty
                    if (!isEditMode || txtProductName.getText().toString().isEmpty()) {
                        txtProductName.setText(selectedProduct);
                    }
                } else {
                    // Reset to default weight spinner when "Select Product" is chosen
                    setupWeightSpinnerDefault();
                    resetBalanceFields();enableIssueForm(); // Re-enables form for new selection

                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                setupWeightSpinnerDefault();
                resetBalanceFields();
                enableIssueForm(); // Re-enable form for new selection
            }
        });
    }

    // Helper method to reset weight spinner to default
    private void setupWeightSpinnerDefault() {
        ArrayAdapter<CharSequence> weightAdapter = ArrayAdapter.createFromResource(
                getActivity(),
                R.array.weight_array,
                android.R.layout.simple_spinner_item
        );
        weightAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerWeight.setAdapter(weightAdapter);
    }

    private void setupTextWatchers() {
        txtQty.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (isEditMode) {
                    calculateUpdatedBalanceForEdit();
                } else {
                    calculateUpdatedBalance();
                }
            }
        });
    }

    private void setupButtonListeners() {
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isEditMode) {
                    updateIssuedGoods();
                } else {
                    issueGoods();
                }
            }
        });

        editButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearForm();
            }
        });
    }

    private void fetchProductBalance(String productName) {
        // In new issue mode, only use today's balance
        currentBalance = dbHelper.getTodayProductBalance(productName);

        // Check if product was received today
        if (currentBalance == 0) {
            boolean productReceivedToday = dbHelper.isProductReceivedToday(productName);
            if (!productReceivedToday) {
                // Product wasn't received today
                Toast.makeText(getActivity(), productName + " wasn't received today", Toast.LENGTH_LONG).show();
                resetBalanceFields();
                disableIssueForm();
                return;
            }
        }

        if (currentBalance > 0) {
            txtProductBal.setText(String.valueOf(currentBalance));
            txtUpdatedBalance.setText(String.valueOf(currentBalance));
            enableIssueForm();
        } else {
            txtProductBal.setText("0");
            txtUpdatedBalance.setText("0");
            Toast.makeText(getActivity(), "No available balance for " + productName, Toast.LENGTH_SHORT).show();
            disableIssueForm();
        }
    }

    private void calculateUpdatedBalance() {
        String quantityStr = txtQty.getText().toString().trim();

        if (quantityStr.isEmpty()) {
            txtUpdatedBalance.setText(String.valueOf(currentBalance));
            return;
        }

        try {
            int quantity = Integer.parseInt(quantityStr);
            // In new issue mode, simply subtract from current balance
            int updatedBalance = currentBalance - quantity;

            // Update the new balance display
            txtUpdatedBalance.setText(String.valueOf(updatedBalance));

            // Change color if balance goes negative
            if (updatedBalance < 0) {
                txtUpdatedBalance.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                saveButton.setEnabled(false);
            } else {
                txtUpdatedBalance.setTextColor(getResources().getColor(android.R.color.black));
                saveButton.setEnabled(true);
            }

        } catch (NumberFormatException e) {
            txtUpdatedBalance.setText(String.valueOf(currentBalance));
        }
    }

    private void disableIssueForm() {
        // Disable form elements when product wasn't received today
        txtQty.setEnabled(false);
        txtStation.setEnabled(false);
        saveButton.setEnabled(false);
        spinnerAssignee.setEnabled(false);
        spinnerWeight.setEnabled(false);
        spinnerFlavour.setEnabled(false);

        // Clear any existing data
        txtQty.setText("");
        txtStation.setText("");
        spinnerAssignee.setSelection(0);
        spinnerWeight.setSelection(0);
        spinnerFlavour.setSelection(0);
    }

    private void enableIssueForm() {
        // Enable form elements when product is available
        txtQty.setEnabled(true);
        txtStation.setEnabled(true);
        saveButton.setEnabled(true);
        spinnerAssignee.setEnabled(true);
        spinnerWeight.setEnabled(true);
        spinnerFlavour.setEnabled(true);
    }

    private void issueGoods() {
        // Get values from form
        String assignee = spinnerAssignee.getSelectedItem().toString();
        String productName = txtProductName.getText().toString().trim();
        String weight = spinnerWeight.getSelectedItem().toString();
        String flavour = spinnerFlavour.getSelectedItem().toString();
        String quantityStr = txtQty.getText().toString().trim();
        String station = txtStation.getText().toString().trim();
        String updatedBalanceStr = txtUpdatedBalance.getText().toString().trim();

        // Validate inputs
        if (!validateInputs(assignee, productName, weight, flavour, quantityStr, station)) {
            return;
        }

        int quantity = Integer.parseInt(quantityStr);
        int newBalance = Integer.parseInt(updatedBalanceStr);

        // Check for duplicate issue (skip if in edit mode)
        if (!isEditMode && dbHelper.checkDuplicateIssue(assignee, productName, weight, flavour, station)) {
            String message = String.format("You've already issued this product - (%s, %s, %s) to %s at %s station",
                    productName, weight, flavour, assignee, station);
            Toast.makeText(getActivity(), message, Toast.LENGTH_LONG).show();
            return;
        }

        // Insert into database and update balance with the new balance value
        boolean isInserted = dbHelper.insertIssuedGoods(assignee, productName, weight, flavour, quantity, station, newBalance);

        if (isInserted) {
            String message = "Goods issued successfully! New balance: " + newBalance;
            Toast.makeText(getActivity(), message, Toast.LENGTH_LONG).show();
            clearForm();
        } else {
            Toast.makeText(getActivity(), "Failed to issue goods", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateIssuedGoods() {
        // Get values from form
        String assignee = spinnerAssignee.getSelectedItem().toString();
        String productName = txtProductName.getText().toString().trim();
        String weight = spinnerWeight.getSelectedItem().toString();
        String flavour = spinnerFlavour.getSelectedItem().toString();
        String quantityStr = txtQty.getText().toString().trim();
        String station = txtStation.getText().toString().trim();
        String updatedBalanceStr = txtUpdatedBalance.getText().toString().trim();

        // Validate inputs
        if (!validateInputs(assignee, productName, weight, flavour, quantityStr, station)) {
            return;
        }

        int newQuantity = Integer.parseInt(quantityStr);
        int displayedBalance = Integer.parseInt(updatedBalanceStr);

        // NEW LOGIC: Use the calculated balance directly
        // The displayedBalance is already calculated as: initialBalance + (originalQuantity - newQuantity)
        int actualNewBalance = displayedBalance;

        // Update the issued goods record with the new logic
        boolean isUpdated = dbHelper.updateIssuedGoodsWithNewLogic(
                issuedId, assignee, productName, weight, flavour,
                originalQuantity, newQuantity, station, actualNewBalance
        );

        if (isUpdated) {
            String message = "Issued goods updated successfully! New balance: " + actualNewBalance;
            Toast.makeText(getActivity(), message, Toast.LENGTH_LONG).show();

            // Go back to previous fragment
            getParentFragmentManager().popBackStack();
        } else {
            Toast.makeText(getActivity(), "Failed to update issued goods", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean validateInputs(String assignee, String productName, String weight,
                                   String flavour, String quantityStr, String station) {
        if (assignee.equals("Select Assignee")) {
            Toast.makeText(getActivity(), "Please select an assignee", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (productName.isEmpty()) {
            productName = spinnerProduct.getSelectedItem().toString();
        }
        if (productName.isEmpty()||productName.equals("Select Product")) {
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

        int quantity;
        try {
            quantity = Integer.parseInt(quantityStr);
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

    private void resetBalanceFields() {
        txtProductBal.setText("");
        txtUpdatedBalance.setText("");
        currentBalance = 0;
        initialBalance = 0;
    }

    private void clearForm() {
        txtProductName.setText("");
        txtQty.setText("");
        txtStation.setText("");
        spinnerAssignee.setSelection(0);
        spinnerProduct.setSelection(0);
        // Reset weight spinner to default
        setupWeightSpinnerDefault();

        spinnerFlavour.setSelection(0);
        resetBalanceFields();
        enableIssueForm(); // Makes sure form is enabled when cleared
    }

    @Override
    public void onDestroy() {
        if (dbHelper != null) {
            dbHelper.close();
        }
        super.onDestroy();
    }
}