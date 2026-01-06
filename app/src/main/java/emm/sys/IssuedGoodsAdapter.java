package emm.sys;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class IssuedGoodsAdapter extends RecyclerView.Adapter<IssuedGoodsAdapter.ViewHolder> {

    private List<IssuedGoodsItem> issuedGoodsList;
    private OnItemActionListener actionListener;
    private SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
    private SimpleDateFormat displayFormat = new SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault());

    public interface OnItemActionListener {
        void onEditClicked(IssuedGoodsItem item);
        void onDeleteClicked(IssuedGoodsItem item);
    }

    public IssuedGoodsAdapter(List<IssuedGoodsItem> issuedGoodsList, OnItemActionListener actionListener) {
        this.issuedGoodsList = issuedGoodsList;
        this.actionListener = actionListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.recycler_issued_goods, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        IssuedGoodsItem item = issuedGoodsList.get(position);

        // Set main data
        holder.txtAssigneeName.setText(item.getAssignee());
        holder.txtStation.setText(item.getStation());
        holder.txtProductName.setText(item.getProductName());
        holder.txtQty.setText(String.valueOf(item.getQuantity()));

        // Format and set product details
        String productDetails = item.getWeight() + " | " + item.getFlavour();
        holder.txtProductDetails.setText(productDetails);

        // Format and set timestamp
        try {
            Date date = inputFormat.parse(item.getTimestamp());
            String formattedTime = displayFormat.format(date);
            holder.txtTimestamp.setText(formattedTime);
        } catch (ParseException e) {
            holder.txtTimestamp.setText(item.getTimestamp());
        }

        // Set click listeners
        holder.btnEdit.setOnClickListener(v -> {
            if (actionListener != null) {
                actionListener.onEditClicked(item);
            }
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (actionListener != null) {
                actionListener.onDeleteClicked(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return issuedGoodsList.size();
    }

    public void updateList(List<IssuedGoodsItem> newList) {
        issuedGoodsList = newList;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtAssigneeName, txtStation, txtProductName, txtProductDetails, txtQty, txtTimestamp;
        MaterialButton btnEdit, btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtAssigneeName = itemView.findViewById(R.id.txtAssigneeName);
            txtStation = itemView.findViewById(R.id.txtStation);
            txtProductName = itemView.findViewById(R.id.txtProductName);
            txtProductDetails = itemView.findViewById(R.id.txtProductDetails);
            txtQty = itemView.findViewById(R.id.txtQty);
            txtTimestamp = itemView.findViewById(R.id.txtTimestamp);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}