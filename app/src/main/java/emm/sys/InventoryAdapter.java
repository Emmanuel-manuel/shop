package emm.sys;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

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

        holder.txtProductName.setText(item.getProductName());
        holder.txtWeight.setText(item.getWeight());
        holder.txtFlavour.setText(item.getFlavour());
        holder.txtQty.setText(String.valueOf(item.getQuantity()));
        holder.txtBal.setText(String.valueOf(item.getBalance()));

        // Color coding for low balance if needed
        if (item.getBalance() <= 0) {
            holder.txtBal.setTextColor(holder.itemView.getContext().getResources().getColor(android.R.color.holo_red_dark));
        } else if (item.getBalance() < 10) { // Low stock threshold
            holder.txtBal.setTextColor(holder.itemView.getContext().getResources().getColor(android.R.color.holo_orange_dark));
        } else {
            holder.txtBal.setTextColor(holder.itemView.getContext().getResources().getColor(android.R.color.holo_green_dark));
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

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtProductName, txtWeight, txtFlavour, txtQty, txtBal;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtProductName = itemView.findViewById(R.id.txtProductName);
            txtWeight = itemView.findViewById(R.id.txtWeight);
            txtFlavour = itemView.findViewById(R.id.txtFlavour);
            txtQty = itemView.findViewById(R.id.txtQty);
            txtBal = itemView.findViewById(R.id.txtBal);
        }
    }
}