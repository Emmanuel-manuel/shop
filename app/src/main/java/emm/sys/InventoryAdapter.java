package emm.sys;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class InventoryAdapter extends RecyclerView.Adapter<InventoryAdapter.InventoryViewHolder> {

    private List<InventoryItem> inventoryItems;

    public InventoryAdapter(List<InventoryItem> inventoryItems) {
        this.inventoryItems = inventoryItems;
    }

    @NonNull
    @Override
    public InventoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.recycler_inventory_items, parent, false);
        return new InventoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull InventoryViewHolder holder, int position) {
        InventoryItem item = inventoryItems.get(position);
        holder.txtProductName.setText(item.getProductName());
        holder.txtWeight.setText(item.getWeight());
        holder.txtFlavour.setText(item.getFlavour());
        holder.txtQty.setText(String.valueOf(item.getQuantity()));
    }

    @Override
    public int getItemCount() {
        return inventoryItems.size();
    }

    public static class InventoryViewHolder extends RecyclerView.ViewHolder {
        TextView txtProductName, txtWeight, txtFlavour, txtQty;

        public InventoryViewHolder(@NonNull View itemView) {
            super(itemView);
            txtProductName = itemView.findViewById(R.id.txtProductName);
            txtWeight = itemView.findViewById(R.id.txtWeight);
            txtFlavour = itemView.findViewById(R.id.txtFlavour);
            txtQty = itemView.findViewById(R.id.txtQty);
        }
    }

    public void updateList(List<InventoryItem> newList) {
        inventoryItems = newList;
        notifyDataSetChanged();
    }
}
