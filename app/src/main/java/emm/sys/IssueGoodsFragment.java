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


public class IssueGoodsFragment extends Fragment {

    // GLOBAL VARIABLES
    private Spinner spinnerAssignee, spinnerProduct, spinnerWeight, spinnerFlavour;
    private EditText txtProductName, txtQty;
    private Button saveButton, editButton;
    private DBHelper dbHelper;

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
        saveButton = view.findViewById(R.id.saveButton);
        editButton = view.findViewById(R.id.editButton);

        // Initialize DBHelper
        dbHelper = new DBHelper(getActivity());

        // Set click listener for save button
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                issueGoods();
            }
        });

        return view;
    }

    private void issueGoods() {
        // Get values from form
        String assignee = spinnerAssignee.getSelectedItem().toString();
        String productName = txtProductName.getText().toString().trim();
        String weight = spinnerWeight.getSelectedItem().toString();
        String flavour = spinnerFlavour.getSelectedItem().toString();
        String quantityStr = txtQty.getText().toString().trim();

        // Validate inputs
        if (assignee.equals("Select Assignee")) {
            Toast.makeText(getActivity(), "Please select an assignee", Toast.LENGTH_SHORT).show();
            return;
        }

        if (productName.isEmpty()||productName.equals("Select Product")) {
            txtProductName.setError("Product name is required");
            Toast.makeText(getActivity(), "Please select a Product", Toast.LENGTH_SHORT).show();
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

        int quantity;
        try {
            quantity = Integer.parseInt(quantityStr);
        } catch (NumberFormatException e) {
            txtQty.setError("Enter a valid number");
            txtQty.requestFocus();
            return;
        }

        // Check for duplicate issue
        if (dbHelper.checkDuplicateIssue(assignee, productName, weight, flavour)) {
            String message = String.format("You've already issued this product - (%s, %s, %s) to %s",
                    productName, weight, flavour, assignee);
            Toast.makeText(getActivity(), message, Toast.LENGTH_LONG).show();
            return;
        }

        // Insert into database
        boolean isInserted = dbHelper.insertIssuedGoods(assignee, productName, weight, flavour, quantity);

        if (isInserted) {
            Toast.makeText(getActivity(), "Goods issued successfully", Toast.LENGTH_SHORT).show();
            clearForm();
        } else {
            Toast.makeText(getActivity(), "Failed to issue goods", Toast.LENGTH_SHORT).show();
        }
    }

    private void clearForm() {
        txtProductName.setText("");
        txtQty.setText("");
        spinnerAssignee.setSelection(0);
        spinnerProduct.setSelection(0);
        spinnerWeight.setSelection(0);
        spinnerFlavour.setSelection(0);
    }

    @Override
    public void onDestroy() {
        dbHelper.close();
        super.onDestroy();
    }
}