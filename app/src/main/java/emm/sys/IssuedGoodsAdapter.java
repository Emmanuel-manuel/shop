package emm.sys;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class IssuedGoodsAdapter extends RecyclerView.Adapter<IssuedGoodsAdapter.ViewHolder> {

    private List<IssuedGoodsItem> issuedGoodsList;
    private OnItemActionListener actionListener;

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

        holder.txtAssigneeName.setText(item.getAssignee());
        holder.txtStation.setText(item.getStation());
        holder.txtProductName.setText(item.getProductName());
        holder.txtWeight.setText(item.getWeight());
        holder.txtFlavour.setText(item.getFlavour());
        holder.txtQty.setText(String.valueOf(item.getQuantity()));

        // Set click listeners for action buttons
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
        TextView txtAssigneeName, txtStation, txtProductName, txtWeight, txtFlavour, txtQty;
        Button btnEdit, btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtAssigneeName = itemView.findViewById(R.id.txtAssigneeName);
            txtStation = itemView.findViewById(R.id.txtStation);
            txtProductName = itemView.findViewById(R.id.txtProductName);
            txtWeight = itemView.findViewById(R.id.txtWeight);
            txtFlavour = itemView.findViewById(R.id.txtFlavour);
            txtQty = itemView.findViewById(R.id.txtQty);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}