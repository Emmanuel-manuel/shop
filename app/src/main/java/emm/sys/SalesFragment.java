package emm.sys;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

public class SalesFragment extends Fragment {

    // UI Components
    private AutoCompleteTextView editMarket, editCustName;
    private Spinner spinnerProduct;
    private TextView sellingPriceTextView, billTextView, totalBillTextView, balanceTextView;
    private RadioButton discountRadioButton, balanceRadioButton;
    private EditText discountAmountEditText, editQuantity, editSubmitted;
    private Button addButton, deleteButton, submitButton;
    private CheckBox cashCheckBox, mpesaCheckBox, toPayCheckBox;
    private RecyclerView salesRecyclerView;

    // Data and Adapters
    private DBHelper dbHelper;
    private SalesAdapter salesAdapter;
    private List<SaleItem> salesList;
    private ArrayAdapter<String> productAdapter;
    private ArrayAdapter<String> marketAdapter;
    private ArrayAdapter<String> customerAdapter;

    // Current values
    private int originalSellingPrice = 0;
    private int currentSellingPrice = 0;
    private int selectedItemPosition = -1;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_sales, container, false);

        // Initialize database
        dbHelper = new DBHelper(getActivity());

        // Initialize views
        initializeViews(view);

        // Setup adapters
        setupAdapters();

        // Setup listeners
        setupListeners();

        // Load initial data
        loadInitialData();

        return view;
    }

    private void initializeViews(View view) {
        // Customer info
        editMarket = view.findViewById(R.id.editMarket);
        editCustName = view.findViewById(R.id.editCustName);

        // Product selection
        spinnerProduct = view.findViewById(R.id.spinnerProduct);
        sellingPriceTextView = view.findViewById(R.id.sellingPriceTextView);
        discountRadioButton = view.findViewById(R.id.discountRadioButton);
        discountAmountEditText = view.findViewById(R.id.discountAmountEditText);

        // Quantity and actions
        editQuantity = view.findViewById(R.id.editQuantity);
        billTextView = view.findViewById(R.id.billTextView);
        addButton = view.findViewById(R.id.addButton);
        deleteButton = view.findViewById(R.id.deleteButton);

        // Sales list
        salesRecyclerView = view.findViewById(R.id.salesRecyclerView);
        totalBillTextView = view.findViewById(R.id.totalBillTextView);

        // Payment mode
        cashCheckBox = view.findViewById(R.id.cashCheckBox);
        mpesaCheckBox = view.findViewById(R.id.mpesaCheckBox);
        toPayCheckBox = view.findViewById(R.id.toPayCheckBox);

        // Balance section
        balanceRadioButton = view.findViewById(R.id.balanceRadioButton);
        editSubmitted = view.findViewById(R.id.editSubmitted);
        balanceTextView = view.findViewById(R.id.balanceTextView);

        // Submit button
        submitButton = view.findViewById(R.id.submitButton);
    }

    private void setupAdapters() {
        // Sales RecyclerView
        salesList = new ArrayList<>();
        salesAdapter = new SalesAdapter(salesList);
        salesRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        salesRecyclerView.setAdapter(salesAdapter);

        // AutoComplete adapters
        marketAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_dropdown_item_1line);
        customerAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_dropdown_item_1line);

        editMarket.setAdapter(marketAdapter);
        editCustName.setAdapter(customerAdapter);

        // Product spinner adapter
        productAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item);
        productAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerProduct.setAdapter(productAdapter);
    }

    private void setupListeners() {
        // Product spinner listener
        spinnerProduct.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0) { // Skip "Select Product" hint
                    String selectedProduct = (String) parent.getItemAtPosition(position);
                    loadProductPrice(selectedProduct);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Discount radio button listener
        discountRadioButton.setOnCheckedChangeListener((buttonView, isChecked) -> {
            discountAmountEditText.setEnabled(isChecked);
            if (!isChecked) {
                discountAmountEditText.setText("");
                currentSellingPrice = originalSellingPrice;
                updateSellingPriceDisplay();
                calculateBill();
            }
        });

        // Discount amount text watcher
        discountAmountEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                calculateDiscountedPrice();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Quantity text watcher
        editQuantity.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                calculateBill();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Balance radio button listener
        balanceRadioButton.setOnCheckedChangeListener((buttonView, isChecked) -> {
            editSubmitted.setEnabled(isChecked);
            if (!isChecked) {
                editSubmitted.setText("");
                updateBalanceDisplay();
            }
        });

        // Submitted amount text watcher
        editSubmitted.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                calculateBalance();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Payment mode checkboxes (single selection)
        cashCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                mpesaCheckBox.setChecked(false);
                toPayCheckBox.setChecked(false);
            }
        });

        mpesaCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                cashCheckBox.setChecked(false);
                toPayCheckBox.setChecked(false);
            }
        });

        toPayCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                cashCheckBox.setChecked(false);
                mpesaCheckBox.setChecked(false);
            }
        });

        // Button listeners
        addButton.setOnClickListener(v -> addItemToSales());
        deleteButton.setOnClickListener(v -> deleteSelectedItem());
        submitButton.setOnClickListener(v -> submitSale());

        // Sales adapter item click listener
        salesAdapter.setOnItemClickListener(position -> {
            selectedItemPosition = position;
            loadItemToForm(salesList.get(position));
        });
    }

    private void loadInitialData() {
        // Load markets for auto-complete
        List<String> markets = dbHelper.getDistinctMarkets();
        marketAdapter.clear();
        marketAdapter.addAll(markets);

        // Load customers for auto-complete
        List<String> customers = dbHelper.getDistinctCustomers();
        customerAdapter.clear();
        customerAdapter.addAll(customers);

        // Load products with available balance
        loadAvailableProducts();

        // Initialize displays
        updateBalanceDisplay();
        calculateTotalBill();
    }

    private void loadAvailableProducts() {
        List<String> availableProducts = dbHelper.getProductsWithAvailableBalance();
        productAdapter.clear();
        productAdapter.add("Select Product"); // Hint item
        productAdapter.addAll(availableProducts);
        productAdapter.notifyDataSetChanged();
    }

    private void loadProductPrice(String productName) {
        originalSellingPrice = dbHelper.getProductSellingPrice(productName);
        currentSellingPrice = originalSellingPrice;
        updateSellingPriceDisplay();
        calculateBill();
    }

    private void calculateDiscountedPrice() {
        if (discountRadioButton.isChecked()) {
            String discountStr = discountAmountEditText.getText().toString().trim();
            if (!discountStr.isEmpty()) {
                try {
                    int discountAmount = Integer.parseInt(discountStr);
                    if (discountAmount >= 0 && discountAmount < originalSellingPrice) {
                        currentSellingPrice = originalSellingPrice - discountAmount;
                    } else {
                        currentSellingPrice = originalSellingPrice;
                        Toast.makeText(getActivity(), "Invalid discount amount", Toast.LENGTH_SHORT).show();
                    }
                } catch (NumberFormatException e) {
                    currentSellingPrice = originalSellingPrice;
                }
            } else {
                currentSellingPrice = originalSellingPrice;
            }
        } else {
            currentSellingPrice = originalSellingPrice;
        }
        updateSellingPriceDisplay();
        calculateBill();
    }

    private void updateSellingPriceDisplay() {
        sellingPriceTextView.setText("KES " + NumberFormat.getInstance().format(currentSellingPrice));
    }

    private void calculateBill() {
        String quantityStr = editQuantity.getText().toString().trim();
        if (!quantityStr.isEmpty()) {
            try {
                int quantity = Integer.parseInt(quantityStr);
                int bill = quantity * currentSellingPrice;
                billTextView.setText("KES " + NumberFormat.getInstance().format(bill));
            } catch (NumberFormatException e) {
                billTextView.setText("KES 0");
            }
        } else {
            billTextView.setText("KES 0");
        }
    }

    private void addItemToSales() {
        // Validate inputs
        if (!validateInputs()) {
            return;
        }

        String market = editMarket.getText().toString().trim();
        String custName = editCustName.getText().toString().trim();
        String product = (String) spinnerProduct.getSelectedItem();
        String quantityStr = editQuantity.getText().toString().trim();

        int quantity = Integer.parseInt(quantityStr);
        int bill = quantity * currentSellingPrice;

        // Check inventory balance
        int availableBalance = dbHelper.getTodayProductBalance(product);
        if (quantity > availableBalance) {
            Toast.makeText(getActivity(), "Insufficient stock. Available: " + availableBalance, Toast.LENGTH_SHORT).show();
            return;
        }

        // Create sale item
        SaleItem saleItem = new SaleItem();
        saleItem.setMarket(market);
        saleItem.setCustName(custName);
        saleItem.setProduct(product);
        saleItem.setSellingPrice(currentSellingPrice);
        saleItem.setQuantity(quantity);
        saleItem.setBill(bill);

        // Add or update item
        if (selectedItemPosition >= 0) {
            salesList.set(selectedItemPosition, saleItem);
            selectedItemPosition = -1;
        } else {
            salesList.add(saleItem);
        }

        salesAdapter.notifyDataSetChanged();
        calculateTotalBill();
        clearForm();

        Toast.makeText(getActivity(), "Item added to sales", Toast.LENGTH_SHORT).show();
    }

    private boolean validateInputs() {
        if (editMarket.getText().toString().trim().isEmpty()) {
            editMarket.setError("Market is required");
            editMarket.requestFocus();
            return false;
        }

        if (editCustName.getText().toString().trim().isEmpty()) {
            editCustName.setError("Customer name is required");
            editCustName.requestFocus();
            return false;
        }

        if (spinnerProduct.getSelectedItemPosition() == 0) {
            Toast.makeText(getActivity(), "Please select a product", Toast.LENGTH_SHORT).show();
            return false;
        }

        String quantityStr = editQuantity.getText().toString().trim();
        if (quantityStr.isEmpty()) {
            editQuantity.setError("Quantity is required");
            editQuantity.requestFocus();
            return false;
        }

        try {
            int quantity = Integer.parseInt(quantityStr);
            if (quantity <= 0) {
                editQuantity.setError("Quantity must be greater than 0");
                editQuantity.requestFocus();
                return false;
            }
        } catch (NumberFormatException e) {
            editQuantity.setError("Invalid quantity");
            editQuantity.requestFocus();
            return false;
        }

        return true;
    }

    private void deleteSelectedItem() {
        if (selectedItemPosition >= 0) {
            salesList.remove(selectedItemPosition);
            salesAdapter.notifyDataSetChanged();
            calculateTotalBill();
            clearForm();
            selectedItemPosition = -1;
            Toast.makeText(getActivity(), "Item removed", Toast.LENGTH_SHORT).show();
        } else {
            clearForm();
        }
    }

    private void loadItemToForm(SaleItem item) {
        editMarket.setText(item.getMarket());
        editCustName.setText(item.getCustName());

        // Set spinner selection
        for (int i = 0; i < productAdapter.getCount(); i++) {
            if (productAdapter.getItem(i).equals(item.getProduct())) {
                spinnerProduct.setSelection(i);
                break;
            }
        }

        editQuantity.setText(String.valueOf(item.getQuantity()));

        // Calculate if there was a discount
        int originalPrice = dbHelper.getProductSellingPrice(item.getProduct());
        if (item.getSellingPrice() < originalPrice) {
            discountRadioButton.setChecked(true);
            int discount = originalPrice - item.getSellingPrice();
            discountAmountEditText.setText(String.valueOf(discount));
        }
    }

    private void clearForm() {
        spinnerProduct.setSelection(0);
        sellingPriceTextView.setText("KES 0");
        discountRadioButton.setChecked(false);
        discountAmountEditText.setText("");
        discountAmountEditText.setEnabled(false);
        editQuantity.setText("");
        billTextView.setText("KES 0");

        originalSellingPrice = 0;
        currentSellingPrice = 0;
    }

    private void calculateTotalBill() {
        int total = 0;
        for (SaleItem item : salesList) {
            total += item.getBill();
        }
        totalBillTextView.setText("Total: KES " + NumberFormat.getInstance().format(total));
        updateBalanceDisplay();
    }

    private void updateBalanceDisplay() {
        int totalBill = getTotalBillAmount();
        if (balanceRadioButton.isChecked()) {
            calculateBalance();
        } else {
            balanceTextView.setText("Balance: KES " + NumberFormat.getInstance().format(totalBill));
        }
    }

    private void calculateBalance() {
        int totalBill = getTotalBillAmount();
        String submittedStr = editSubmitted.getText().toString().trim();

        if (!submittedStr.isEmpty()) {
            try {
                int submitted = Integer.parseInt(submittedStr);
                if (submitted <= totalBill) {
                    int balance = totalBill - submitted;
                    balanceTextView.setText("Balance: KES " + NumberFormat.getInstance().format(balance));
                } else {
                    balanceTextView.setText("Change: KES " + NumberFormat.getInstance().format(submitted - totalBill));
                }
            } catch (NumberFormatException e) {
                balanceTextView.setText("Balance: KES " + NumberFormat.getInstance().format(totalBill));
            }
        } else {
            balanceTextView.setText("Balance: KES " + NumberFormat.getInstance().format(totalBill));
        }
    }

    private int getTotalBillAmount() {
        int total = 0;
        for (SaleItem item : salesList) {
            total += item.getBill();
        }
        return total;
    }

    private void submitSale() {
        if (salesList.isEmpty()) {
            Toast.makeText(getActivity(), "No items to submit", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!validatePaymentMode()) {
            return;
        }

        String paymentMode = getSelectedPaymentMode();
        int totalBill = getTotalBillAmount();

        boolean isPartialPayment = balanceRadioButton.isChecked();
        boolean isToPay = toPayCheckBox.isChecked();

        // Process each sale item
        boolean allSuccess = true;
        for (SaleItem item : salesList) {
            boolean success;

            if (isPartialPayment || isToPay) {
                // Insert to to_pay table
                int balance = 0;
                if (isPartialPayment) {
                    String submittedStr = editSubmitted.getText().toString().trim();
                    if (!submittedStr.isEmpty()) {
                        int submitted = Integer.parseInt(submittedStr);
                        balance = Math.max(0, totalBill - submitted);
                    } else {
                        balance = totalBill;
                    }
                }

                success = dbHelper.insertToPay(
                        item.getMarket(),
                        item.getCustName(),
                        item.getProduct(),
                        item.getSellingPrice(),
                        item.getQuantity(),
                        item.getBill(),
                        paymentMode,
                        totalBill,
                        balance
                );
            } else {
                // Insert to sales table
                success = dbHelper.insertSale(
                        item.getMarket(),
                        item.getCustName(),
                        item.getProduct(),
                        item.getSellingPrice(),
                        item.getQuantity(),
                        item.getBill(),
                        paymentMode,
                        totalBill
                );
            }

            if (!success) {
                allSuccess = false;
                break;
            }

            // Update inventory balance
            int currentBalance = dbHelper.getTodayProductBalance(item.getProduct());
            int newBalance = currentBalance - item.getQuantity();
            dbHelper.updateInventoryBalance(item.getProduct(), newBalance);
        }

        if (allSuccess) {
            String message = isPartialPayment || isToPay ? "Sale saved to pending payments" : "Sale completed successfully";
            Toast.makeText(getActivity(), message, Toast.LENGTH_LONG).show();
            resetForm();
        } else {
            Toast.makeText(getActivity(), "Error saving sale. Please try again.", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean validatePaymentMode() {
        if (!cashCheckBox.isChecked() && !mpesaCheckBox.isChecked() && !toPayCheckBox.isChecked()) {
            Toast.makeText(getActivity(), "Please select a payment mode", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private String getSelectedPaymentMode() {
        if (cashCheckBox.isChecked()) return "Cash";
        if (mpesaCheckBox.isChecked()) return "M-Pesa";
        if (toPayCheckBox.isChecked()) return "To-Pay";
        return "";
    }

    private void resetForm() {
        // Clear customer info
        editMarket.setText("");
        editCustName.setText("");

        // Clear form
        clearForm();

        // Clear sales list
        salesList.clear();
        salesAdapter.notifyDataSetChanged();
        selectedItemPosition = -1;

        // Reset payment
        cashCheckBox.setChecked(false);
        mpesaCheckBox.setChecked(false);
        toPayCheckBox.setChecked(false);

        // Reset balance
        balanceRadioButton.setChecked(false);
        editSubmitted.setText("");
        editSubmitted.setEnabled(false);

        // Update displays
        calculateTotalBill();

        // Reload available products
        loadAvailableProducts();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) {
            dbHelper.close();
        }
    }
}