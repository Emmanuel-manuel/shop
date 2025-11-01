package emm.sys;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class IssueGoodsFragment extends Fragment {

    // GLOBAL VARIABLES
    private Spinner spinnerAssignee, spinnerProduct, spinnerWeight, spinnerFlavour;
    private EditText txtProductName, txtQty, txtStation;
    private TextView txtProductBal, txtUpdatedBalance;
    private Button saveButton, editButton;
    private DBHelper dbHelper;

    private int currentBalance = 0;

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

        // Setup listeners
        setupSpinnerListeners();
        setupTextWatchers();
        setupButtonListeners();

        return view;
    }

    private void setupSpinnerListeners() {
        spinnerProduct.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedProduct = parent.getItemAtPosition(position).toString();
                if (!selectedProduct.equals("Select Product")) {
                    fetchProductBalance(selectedProduct);
                    txtProductName.setText(selectedProduct);
                } else {
                    resetBalanceFields();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                resetBalanceFields();
            }
        });
    }

    private void setupTextWatchers() {
        txtQty.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                calculateUpdatedBalance();
            }
        });
    }

    private void setupButtonListeners() {
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                issueGoods();
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
        currentBalance = dbHelper.getProductBalance(productName);
        if (currentBalance > 0) {
            txtProductBal.setText(String.valueOf(currentBalance));
            txtUpdatedBalance.setText(String.valueOf(currentBalance));
        } else {
            txtProductBal.setText("0");
            txtUpdatedBalance.setText("0");
            Toast.makeText(getActivity(), "No available balance for " + productName, Toast.LENGTH_SHORT).show();
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
            int updatedBalance = currentBalance - quantity;

            // Update the new balance display
            txtUpdatedBalance.setText(String.valueOf(updatedBalance));

            // Change color if balance goes negative
            if (updatedBalance < 0) {
                txtUpdatedBalance.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            } else {
                txtUpdatedBalance.setTextColor(getResources().getColor(android.R.color.black));
            }

        } catch (NumberFormatException e) {
            txtUpdatedBalance.setText(String.valueOf(currentBalance));
        }
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
        if (assignee.equals("Select Assignee")) {
            Toast.makeText(getActivity(), "Please select an assignee", Toast.LENGTH_SHORT).show();
            return;
        }

        if (productName.isEmpty()) {
            productName = spinnerProduct.getSelectedItem().toString();
        }
        if (productName.isEmpty()||productName.equals("Select Product")) {
            txtProductName.setError("Product name is required");
            Toast.makeText(getActivity(), "Please enter or select a Product", Toast.LENGTH_SHORT).show();
            txtProductName.requestFocus();
            return;
        }

        if (weight.equals("Select Weight")) {
            Toast.makeText(getActivity(), "Please select a weight", Toast.LENGTH_SHORT).show();
            return;
        }

        if (flavour.equals("Select Type/Flavour")) {
            Toast.makeText(getActivity(), "Please select a flavour", Toast.LENGTH_SHORT).show();
            return;
        }

        if (quantityStr.isEmpty()) {
            txtQty.setError("Quantity is required");
            txtQty.requestFocus();
            return;
        }

        if (station.isEmpty()) {
            txtStation.setError("Station is required");
            txtStation.requestFocus();
            return;
        }

        int quantity;
        int newBalance;
        try {
            quantity = Integer.parseInt(quantityStr);
            newBalance = Integer.parseInt(updatedBalanceStr);

            if (quantity <= 0) {
                txtQty.setError("Quantity must be positive");
                return;
            }

            if (newBalance < 0) {
                Toast.makeText(getActivity(), "Issue quantity exceeds available balance", Toast.LENGTH_LONG).show();
                return;
            }
        } catch (NumberFormatException e) {
            Toast.makeText(getActivity(), "Please enter valid numbers", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check current balance before proceeding
        if (currentBalance <= 0) {
            Toast.makeText(getActivity(), "No available balance for " + productName, Toast.LENGTH_LONG).show();
            return;
        }

        if (quantity > currentBalance) {
            Toast.makeText(getActivity(),
                    "Issue quantity (" + quantity + ") exceeds available balance (" + currentBalance + ")",
                    Toast.LENGTH_LONG).show();
            return;
        }

        // Check for duplicate issue
        if (dbHelper.checkDuplicateIssue(assignee, productName, weight, flavour, station)) {
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

    private void resetBalanceFields() {
        txtProductBal.setText("");
        txtUpdatedBalance.setText("");
        currentBalance = 0;
    }

    private void clearForm() {
        txtProductName.setText("");
        txtQty.setText("");
        txtStation.setText("");
        spinnerAssignee.setSelection(0);
        spinnerProduct.setSelection(0);
        spinnerWeight.setSelection(0);
        spinnerFlavour.setSelection(0);
        resetBalanceFields();
    }

    @Override
    public void onDestroy() {
        if (dbHelper != null) {
            dbHelper.close();
        }
        super.onDestroy();
    }
}