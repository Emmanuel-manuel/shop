package emm.sys;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

public class ReceiveInventoryFragment extends Fragment {

    // Global Variables
    private Spinner spinnerProduct, spinnerWeight, spinnerFlavour;
    private EditText txtProductName, txtQty;
    private Button saveButton;
    private DBHelper dbHelper;

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

        saveButton.setOnClickListener(v -> saveInventoryData());

        return view;
    }

    private void saveInventoryData() {
        // Get product name (manual entry takes priority)
        String weight = spinnerWeight.getSelectedItem().toString();
        String flavour = spinnerFlavour.getSelectedItem().toString();
        String productName = txtProductName.getText().toString().trim();

        if (productName.isEmpty()) {
            productName = spinnerProduct.getSelectedItem().toString();
        }

        // =======VALIDATION BLOCK ======
        if (productName.isEmpty()||productName.equals("Select Product")) {
            txtProductName.setError("Product name is required");
            Toast.makeText(getActivity(), "Please enter or select a product", Toast.LENGTH_SHORT).show();
            txtProductName.requestFocus();
            return;
        }

        // Validate quantity
        String quantityStr = txtQty.getText().toString().trim();
        if (quantityStr.isEmpty()) {
            txtQty.setError("Please enter quantity");
            return;
        }
        int quantity;
        try {
            quantity = Integer.parseInt(quantityStr);
            if (quantity <= 0) {
                txtQty.setError("Quantity must be positive");
                return;
            }
        } catch (NumberFormatException e) {
            txtQty.setError("Invalid quantity");
            return;
        }

        // ====== DUPLICATE CHECK ======
        if (dbHelper.checkInventoryExists(productName, weight, flavour)) {
            Toast.makeText(getActivity(),
                    "You have already received this product today",
                    Toast.LENGTH_LONG).show();
            clearForm();
            return;
        }

        // Save to database - balance will be automatically set equal to quantity
        boolean isSuccess = dbHelper.insertInventory(productName, weight, flavour, quantity);

        if (isSuccess) {
            // Display balance information
            int currentBalance = dbHelper.getProductBalance(productName);
            String message = "Inventory saved successfully! Balance: " + currentBalance;
            Toast.makeText(getActivity(), message, Toast.LENGTH_LONG).show();
            clearForm();
        } else {
            Toast.makeText(getActivity(), "Failed to save inventory", Toast.LENGTH_SHORT).show();
        }
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
    public void onDestroy() {
        if (dbHelper != null) {
            dbHelper.close();
        }
        super.onDestroy();
    }
}