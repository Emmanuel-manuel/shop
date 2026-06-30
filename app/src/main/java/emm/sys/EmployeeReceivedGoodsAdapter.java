package emm.sys;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class EmployeeReceivedGoodsAdapter extends RecyclerView.Adapter<EmployeeReceivedGoodsAdapter.ViewHolder> {

    private List<EmployeeReceivedGoodsItem> itemList;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(EmployeeReceivedGoodsItem item);
        void onDeleteClick(EmployeeReceivedGoodsItem item);
    }

    public EmployeeReceivedGoodsAdapter(List<EmployeeReceivedGoodsItem> itemList) {
        this.itemList = itemList;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void updateList(List<EmployeeReceivedGoodsItem> newList) {
        this.itemList = newList;
        notifyDataSetChanged();
    }

    public List<EmployeeReceivedGoodsItem> getCurrentList() {
        return itemList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_employee_received_goods, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        EmployeeReceivedGoodsItem item = itemList.get(position);

        holder.txtProductName.setText(item.getProductName());
        holder.txtWeight.setText(item.getWeight());
        holder.txtFlavour.setText(item.getFlavour());
        holder.txtQuantity.setText(String.valueOf(item.getQuantity()));
        holder.txtStation.setText(item.getStation());

        // Format and display timestamp
        String formattedTime = formatTimestamp(item.getTimestamp());
        holder.txtTimestamp.setText(formattedTime);

        // Set click listeners
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(item);
            }
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return itemList != null ? itemList.size() : 0;
    }

    private String formatTimestamp(String timestamp) {
        if (TextUtils.isEmpty(timestamp)) return "";
        try {
            SimpleDateFormat in = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            SimpleDateFormat out = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
            return out.format(in.parse(timestamp));
        } catch (Exception e) {
            return timestamp;
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtProductName, txtWeight, txtFlavour, txtQuantity, txtStation, txtTimestamp;
        TextView btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtProductName = itemView.findViewById(R.id.txtProductName);
            txtWeight = itemView.findViewById(R.id.txtWeight);
            txtFlavour = itemView.findViewById(R.id.txtFlavour);
            txtQuantity = itemView.findViewById(R.id.txtQuantity);
            txtStation = itemView.findViewById(R.id.txtStation);
            txtTimestamp = itemView.findViewById(R.id.txtTimestamp);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}