package emm.sys;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class TopProductAdapter extends RecyclerView.Adapter<TopProductAdapter.ViewHolder> {
    private List<TopProduct> productList;

    public TopProductAdapter(List<TopProduct> productList) {
        this.productList = productList;
    }

    public void updateList(List<TopProduct> newList) {
        this.productList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_top_product, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TopProduct product = productList.get(position);
        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("en", "KE")); //displays Kenyan Currency symbol

        String rankColor;
        if (position == 0) rankColor = "#FFD700";
        else if (position == 1) rankColor = "#C0C0C0";
        else if (position == 2) rankColor = "#CD7F32";
        else rankColor = "#757575";

        holder.txtRank.setText(String.valueOf(product.getRank()));
        holder.txtRank.setBackgroundColor(android.graphics.Color.parseColor(rankColor));
        holder.txtProductName.setText(product.getName());
        holder.txtProductRevenue.setText(formatter.format(product.getRevenue()));
    }

    @Override
    public int getItemCount() {
        return productList != null ? productList.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtRank, txtProductName, txtProductRevenue;
        ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtRank = itemView.findViewById(R.id.txtRank);
            txtProductName = itemView.findViewById(R.id.txtProductName);
            txtProductRevenue = itemView.findViewById(R.id.txtProductRevenue);
        }
    }
}