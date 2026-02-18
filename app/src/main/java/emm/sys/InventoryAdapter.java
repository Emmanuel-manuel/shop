package emm.sys;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class InventoryAdapter extends RecyclerView.Adapter<InventoryAdapter.ViewHolder> {

    private List<InventoryItem> inventoryList;

    public InventoryAdapter(List<InventoryItem> inventoryList) {
        this.inventoryList = inventoryList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.recycler_inventory_items, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        InventoryItem item = inventoryList.get(position);

        // Basic product information
        holder.txtProductName.setText(item.getProductName());
        holder.txtWeight.setText(item.getWeight());
        holder.txtFlavour.setText(item.getFlavour());

        // CORRECTED: Display values according to your specification
        int quantityDelivered = item.getQuantity();  // From 'quantity' column - total delivered
        int currentBalance = item.getBalance();      // From 'balance' column - current remaining
        int issuedQuantity = quantityDelivered - currentBalance; // Calculate issued/sold

        // Display according to your requirements:
        holder.txtQty.setText(String.valueOf(quantityDelivered));      // Shows 'quantity' column value
        holder.txtBalance.setText(String.valueOf(currentBalance));     // Shows 'balance' column value
        holder.txtDelivered.setText(String.valueOf(issuedQuantity));   // Shows difference (quantity - balance)

        // Format and display timestamp
        holder.txtTimestamp.setText(formatTime(item.getTimestamp()));

        // Color coding for current balance
        if (currentBalance <= 0) {
            // Out of stock - Red
            holder.txtBalance.setTextColor(Color.parseColor("#F44336"));
            holder.txtQty.setTextColor(Color.parseColor("#2196F3")); // Keep quantity normal color
            holder.txtLowStockWarning.setVisibility(View.VISIBLE);
            holder.txtLowStockWarning.setText("⚠️ Out of Stock!");
        } else if (item.isLowStock()) {
            // Low stock - Orange
            holder.txtBalance.setTextColor(Color.parseColor("#FF9800"));
            holder.txtQty.setTextColor(Color.parseColor("#2196F3")); // Keep quantity normal color
            holder.txtLowStockWarning.setVisibility(View.VISIBLE);
            holder.txtLowStockWarning.setText("⚠️ Low Stock Alert!");
        } else {
            // Good stock - Green
            holder.txtBalance.setTextColor(Color.parseColor("#4CAF50"));
            holder.txtQty.setTextColor(Color.parseColor("#2196F3"));
            holder.txtLowStockWarning.setVisibility(View.GONE);
        }

        // Color coding for issued/delivered quantity
        if (issuedQuantity > 0) {
            holder.txtDelivered.setTextColor(Color.parseColor("#FF9800")); // Orange for issued items
        } else {
            holder.txtDelivered.setTextColor(Color.parseColor("#757575")); // Gray for no issues
        }
    }

    @Override
    public int getItemCount() {
        return inventoryList.size();
    }

    public void updateList(List<InventoryItem> newList) {
        inventoryList = newList;
        notifyDataSetChanged();
    }

    // Helper method to format timestamp
    private String formatTime(String timestamp) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
            Date date = inputFormat.parse(timestamp);
            return outputFormat.format(date);
        } catch (Exception e) {
            return "Today";
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtProductName, txtWeight, txtFlavour, txtQty, txtBalance, txtDelivered, txtTimestamp, txtLowStockWarning;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            // Initialize all TextViews
            txtProductName = itemView.findViewById(R.id.txtProductName);
            txtWeight = itemView.findViewById(R.id.txtWeight);
            txtFlavour = itemView.findViewById(R.id.txtFlavour);
            txtQty = itemView.findViewById(R.id.txtQty);
            txtBalance = itemView.findViewById(R.id.txtBalance);
            txtDelivered = itemView.findViewById(R.id.txtDelivered);
            txtTimestamp = itemView.findViewById(R.id.txtTimestamp);
            txtLowStockWarning = itemView.findViewById(R.id.txtLowStockWarning);
        }
    }
}