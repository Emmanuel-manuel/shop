package emm.sys;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;

public class ProductDetailsFragment extends Fragment {

    private EditText txtProductName, txtPrice;
    private Spinner spinnerWeight, spinnerFlavour;
    private Button saveButton;
    private DBHelper dbHelper;

    private String selectedWeight = "";
    private String selectedFlavour = "";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_product_details, container, false);

        // Initialize database helper
        dbHelper = new DBHelper(getActivity());

        // Initialize views
        txtProductName = view.findViewById(R.id.txtProductName);
        txtPrice = view.findViewById(R.id.txtPrice);
        spinnerWeight = view.findViewById(R.id.spinnerWeight);
        spinnerFlavour = view.findViewById(R.id.spinnerFlavour);
        saveButton = view.findViewById(R.id.saveButton);

        // Setup spinners
        setupSpinners();

        // Setup save button listener
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveProductDetails();
            }
        });

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
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedFlavour = "";
            }
        });
    }

    private void saveProductDetails() {
        // Get input values
        String productName = txtProductName.getText().toString().trim();
        String priceStr = txtPrice.getText().toString().trim();

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

        if (priceStr.isEmpty()) {
            txtPrice.setError("Please enter price");
            txtPrice.requestFocus();
            return;
        }

        try {
            // Convert price to integer
            int price = Integer.parseInt(priceStr);

            // Check if price is valid
            if (price <= 0) {
                txtPrice.setError("Price must be greater than 0");
                txtPrice.requestFocus();
                return;
            }

            // Check if product with same details already exists
            boolean productExists = dbHelper.checkProductDetailsExists(productName, selectedWeight, selectedFlavour);

            if (productExists) {
                // Option 1: Show error message
                Toast.makeText(getActivity(),
                        "Product '" + productName + "' with weight '" + selectedWeight +
                                "' and flavour '" + selectedFlavour + "' already exists!",
                        Toast.LENGTH_LONG).show();

                // Option 2: Or update the price if you want to allow price updates
                // updateExistingProductPrice(productName, selectedWeight, selectedFlavour, price);

            } else {
                // Save to product_details table using the new method
                boolean success = dbHelper.insertProductDetails(productName, selectedWeight, selectedFlavour, price);

                if (success) {
                    Toast.makeText(getActivity(),
                            "Product details saved successfully!\n" +
                                    "Product: " + productName + "\n" +
                                    "Weight: " + selectedWeight + "\n" +
                                    "Flavour: " + selectedFlavour + "\n" +
                                    "Price: KES " + price,
                            Toast.LENGTH_LONG).show();

                    // Clear form for next entry
                    clearForm();

                } else {
                    Toast.makeText(getActivity(), "Failed to save product details", Toast.LENGTH_SHORT).show();
                }
            }

        } catch (NumberFormatException e) {
            txtPrice.setError("Please enter a valid number for price");
            txtPrice.requestFocus();
        }
    }

    // Optional method to update price of existing product
    private void updateExistingProductPrice(String productName, String weight, String flavour, int newPrice) {
        boolean success = dbHelper.updateProductPrice(productName, weight, flavour, newPrice);

        if (success) {
            Toast.makeText(getActivity(),
                    "Price updated for '" + productName + "'\n" +
                            "New Price: KES " + newPrice,
                    Toast.LENGTH_LONG).show();
            clearForm();
        } else {
            Toast.makeText(getActivity(), "Failed to update price", Toast.LENGTH_SHORT).show();
        }
    }

    private void clearForm() {
        txtProductName.setText("");
        txtPrice.setText("");
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