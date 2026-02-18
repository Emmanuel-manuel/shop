package emm.sys;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.text.NumberFormat;
import java.util.List;

public class SalesAdapter extends RecyclerView.Adapter<SalesAdapter.ViewHolder> {
    private List<SaleItem> salesList;
    private OnItemClickListener onItemClickListener;
    private int selectedPosition = -1;

    // Interface for item click events
    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public SalesAdapter(List<SaleItem> salesList) {
        this.salesList = salesList;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_sale, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SaleItem saleItem = salesList.get(position);
        NumberFormat formatter = NumberFormat.getInstance();

        // Bind data to views
        holder.marketText.setText(saleItem.getMarket());
        holder.custNameText.setText(saleItem.getCustName());
        holder.productText.setText(saleItem.getProduct());
        holder.sellingPriceText.setText("KES " + formatter.format(saleItem.getSellingPrice()));
        holder.quantityText.setText(String.valueOf(saleItem.getQuantity()));
        holder.billText.setText("KES " + formatter.format(saleItem.getBill()));
        holder.itemNumberText.setText("#" + String.format("%02d", position + 1));

        // FIXED: Set background and border for selected item using alternative approach
        if (position == selectedPosition) {
            // Selected state - light blue background with border
            holder.cardView.setCardBackgroundColor(Color.parseColor("#E3F2FD")); // Light blue
            holder.cardView.setCardElevation(8f); // Increase elevation for selected state

            // Create border effect using background drawable
            GradientDrawable borderDrawable = new GradientDrawable();
            borderDrawable.setColor(Color.parseColor("#E3F2FD"));
            borderDrawable.setStroke(6, Color.parseColor("#2196F3")); // Primary blue border
            borderDrawable.setCornerRadius(24f); // Match CardView corner radius
            holder.cardView.setBackground(borderDrawable);
        } else {
            // Normal state - white background
            holder.cardView.setCardBackgroundColor(Color.parseColor("#FFFFFF")); // White
            holder.cardView.setCardElevation(2f); // Normal elevation

            // Reset background to default CardView appearance
            holder.cardView.setBackground(null);
        }

        // FIXED: Set click listener using holder.getAdapterPosition()
        holder.itemView.setOnClickListener(v -> {
            int clickedPosition = holder.getAdapterPosition();
            if (clickedPosition == RecyclerView.NO_POSITION) {
                return; // Item has been removed, ignore click
            }

            int previousPosition = selectedPosition;
            selectedPosition = clickedPosition;

            // Notify changes for visual feedback
            if (previousPosition != -1) {
                notifyItemChanged(previousPosition);
            }
            notifyItemChanged(selectedPosition);

            // Trigger callback
            if (onItemClickListener != null) {
                onItemClickListener.onItemClick(clickedPosition);
            }
        });

        // Long click listener for additional actions
        holder.itemView.setOnLongClickListener(v -> {
            int clickedPosition = holder.getAdapterPosition();
            if (clickedPosition == RecyclerView.NO_POSITION) {
                return false;
            }
            // Could add context menu or additional actions here
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return salesList.size();
    }

    // Method to clear selection
    public void clearSelection() {
        int previousPosition = selectedPosition;
        selectedPosition = -1;
        if (previousPosition != -1) {
            notifyItemChanged(previousPosition);
        }
    }

    // Method to get selected item
    public SaleItem getSelectedItem() {
        if (selectedPosition >= 0 && selectedPosition < salesList.size()) {
            return salesList.get(selectedPosition);
        }
        return null;
    }

    // Method to get selected position
    public int getSelectedPosition() {
        return selectedPosition;
    }

    // ViewHolder class
    static class ViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        TextView itemNumberText;
        TextView marketText;
        TextView custNameText;
        TextView productText;
        TextView sellingPriceText;
        TextView quantityText;
        TextView billText;

        ViewHolder(View itemView) {
            super(itemView);

            cardView = itemView.findViewById(R.id.cardView);
            itemNumberText = itemView.findViewById(R.id.itemNumberText);
            marketText = itemView.findViewById(R.id.marketText);
            custNameText = itemView.findViewById(R.id.custNameText);
            productText = itemView.findViewById(R.id.productText);
            sellingPriceText = itemView.findViewById(R.id.sellingPriceText);
            quantityText = itemView.findViewById(R.id.quantityText);
            billText = itemView.findViewById(R.id.billText);
        }
    }
}