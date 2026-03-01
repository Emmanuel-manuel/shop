package emm.sys;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ToPayAdapter extends RecyclerView.Adapter<ToPayAdapter.ViewHolder> {

    private List<ToPayItem> toPayList;
    private NumberFormat formatter = NumberFormat.getInstance();
    private OnItemSelectedListener onItemSelectedListener;
    private boolean selectionMode = false;

    public interface OnItemSelectedListener {
        void onItemSelected(ToPayItem item, boolean isSelected);
        void onSelectionModeChanged(boolean isActive);
    }

    public ToPayAdapter(List<ToPayItem> toPayList) {
        this.toPayList = toPayList;
    }

    public void setOnItemSelectedListener(OnItemSelectedListener listener) {
        this.onItemSelectedListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_to_pay, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ToPayItem item = toPayList.get(position);

        // Set checkbox state
        holder.checkBox.setChecked(item.isSelected());

        // Set data
        holder.txtCustomer.setText(truncateString(item.getCustName(), 15));
        holder.txtMarket.setText(truncateString(item.getMarket(), 12));
        holder.txtProduct.setText(truncateString(item.getProductName(), 12));
        holder.txtQuantity.setText(String.valueOf(item.getQuantity()));
        holder.txtBill.setText(formatter.format(item.getBill()));
        holder.txtBalance.setText(formatter.format(item.getBalance()));

        // Format timestamp
        try {
            SimpleDateFormat dbFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            SimpleDateFormat displayFormat = new SimpleDateFormat("dd/MM HH:mm", Locale.getDefault());
            Date date = dbFormat.parse(item.getTimestamp());
            holder.txtTimestamp.setText(displayFormat.format(date));
        } catch (Exception e) {
            holder.txtTimestamp.setText("");
        }

        // Set payment mode indicator
        String paymentMode = item.getPaymentMode();
        switch (paymentMode) {
            case "Cash":
                holder.txtPaymentMode.setText("💵");
                break;
            case "M-Pesa":
                holder.txtPaymentMode.setText("📱");
                break;
            default:
                holder.txtPaymentMode.setText("📝");
        }

        // Highlight if balance is high
        if (item.getBalance() > 5000) {
            holder.txtBalance.setTextColor(Color.parseColor("#F44336")); // Red
            holder.txtBalance.setBackgroundColor(Color.parseColor("#FFEBEE")); // Light red
        } else {
            holder.txtBalance.setTextColor(Color.parseColor("#FF9800")); // Orange
            holder.txtBalance.setBackgroundColor(Color.parseColor("#FFF3E0")); // Light orange
        }

        // Handle checkbox visibility based on selection mode
        if (selectionMode) {
            holder.checkBox.setVisibility(View.VISIBLE);
        } else {
            holder.checkBox.setVisibility(View.GONE);
        }

        // Handle item click
        holder.itemView.setOnClickListener(v -> {
            if (selectionMode) {
                // In selection mode, toggle checkbox
                boolean newState = !item.isSelected();
                item.setSelected(newState);
                holder.checkBox.setChecked(newState);
                if (onItemSelectedListener != null) {
                    onItemSelectedListener.onItemSelected(item, newState);
                }
            } else {
                // Not in selection mode, just show details (can be expanded later)
                // Could show a dialog with full details
            }
        });

        // Handle long press to enter selection mode
        holder.itemView.setOnLongClickListener(v -> {
            if (!selectionMode) {
                selectionMode = true;
                item.setSelected(true);
                notifyDataSetChanged();
                if (onItemSelectedListener != null) {
                    onItemSelectedListener.onSelectionModeChanged(true);
                    onItemSelectedListener.onItemSelected(item, true);
                }
            }
            return true;
        });

        // Handle checkbox click
        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            item.setSelected(isChecked);
            if (onItemSelectedListener != null) {
                onItemSelectedListener.onItemSelected(item, isChecked);
            }
        });
    }

    private String truncateString(String text, int maxLength) {
        if (text == null) return "";
        return text.length() > maxLength ? text.substring(0, maxLength - 3) + "..." : text;
    }

    @Override
    public int getItemCount() {
        return toPayList.size();
    }

    public void updateList(List<ToPayItem> newList) {
        toPayList = newList;
        notifyDataSetChanged();
    }

    public void setSelectionMode(boolean selectionMode) {
        this.selectionMode = selectionMode;
        if (!selectionMode) {
            // Clear all selections
            for (ToPayItem item : toPayList) {
                item.setSelected(false);
            }
        }
        notifyDataSetChanged();
    }

    public List<ToPayItem> getSelectedItems() {
        List<ToPayItem> selected = new java.util.ArrayList<>();
        for (ToPayItem item : toPayList) {
            if (item.isSelected()) {
                selected.add(item);
            }
        }
        return selected;
    }

    public boolean hasSelectedItems() {
        for (ToPayItem item : toPayList) {
            if (item.isSelected()) {
                return true;
            }
        }
        return false;
    }

    public void clearSelections() {
        for (ToPayItem item : toPayList) {
            item.setSelected(false);
        }
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        CheckBox checkBox;
        TextView txtCustomer, txtMarket, txtProduct, txtQuantity, txtBill, txtBalance, txtTimestamp, txtPaymentMode;
        CardView cardView;

        ViewHolder(View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardView);
            checkBox = itemView.findViewById(R.id.checkBox);
            txtCustomer = itemView.findViewById(R.id.txtCustomer);
            txtMarket = itemView.findViewById(R.id.txtMarket);
            txtProduct = itemView.findViewById(R.id.txtProduct);
            txtQuantity = itemView.findViewById(R.id.txtQuantity);
            txtBill = itemView.findViewById(R.id.txtBill);
            txtBalance = itemView.findViewById(R.id.txtBalance);
            txtTimestamp = itemView.findViewById(R.id.txtTimestamp);
            txtPaymentMode = itemView.findViewById(R.id.txtPaymentMode);
        }
    }
}