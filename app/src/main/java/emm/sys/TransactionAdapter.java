package emm.sys;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.ViewHolder> {

    private List<TransactionItem> transactionList;
    private NumberFormat formatter = NumberFormat.getInstance();

    public TransactionAdapter(List<TransactionItem> transactionList) {
        this.transactionList = transactionList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_transaction, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TransactionItem item = transactionList.get(position);

        // Set item number
        holder.txtNumber.setText(String.valueOf(position + 1));

        // Set customer name (truncate if too long)
        String custName = item.getCustName();
        if (custName.length() > 12) {
            custName = custName.substring(0, 10) + "...";
        }
        holder.txtCustomer.setText(custName);

        // Set market (truncate if too long)
        String market = item.getMarket();
        if (market.length() > 12) {
            market = market.substring(0, 10) + "...";
        }
        holder.txtMarket.setText(market);

        // Set product name (truncate if too long)
        String product = item.getProductName();
        if (product.length() > 12) {
            product = product.substring(0, 10) + "...";
        }
        holder.txtProduct.setText(product);

        // Set quantity
        holder.txtQuantity.setText(String.valueOf(item.getQuantity()));

        // Set amount
        holder.txtAmount.setText(formatter.format(item.getBill()));

        // Set status based on transaction type
        if ("Paid".equals(item.getTransactionType())) {
            holder.txtStatus.setText("Paid");
            holder.txtStatus.setBackgroundColor(Color.parseColor("#4CAF50")); // Green
            holder.txtStatus.setTextColor(Color.WHITE);
        } else {
            holder.txtStatus.setText("To Pay");
            holder.txtStatus.setBackgroundColor(Color.parseColor("#F44336")); // Red
            holder.txtStatus.setTextColor(Color.WHITE);
        }

        // Format and set timestamp
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
            case "To-Pay":
                holder.txtPaymentMode.setText("📝");
                break;
            default:
                holder.txtPaymentMode.setText("💳");
        }
    }

    @Override
    public int getItemCount() {
        return transactionList.size();
    }

    public void updateList(List<TransactionItem> newList) {
        transactionList = newList;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtNumber, txtCustomer, txtMarket, txtProduct, txtQuantity, txtAmount, txtStatus, txtTimestamp, txtPaymentMode;
        CardView cardView;

        ViewHolder(View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardView);
            txtNumber = itemView.findViewById(R.id.txtNumber);
            txtCustomer = itemView.findViewById(R.id.txtCustomer);
            txtMarket = itemView.findViewById(R.id.txtMarket);
            txtProduct = itemView.findViewById(R.id.txtProduct);
            txtQuantity = itemView.findViewById(R.id.txtQuantity);
            txtAmount = itemView.findViewById(R.id.txtAmount);
            txtStatus = itemView.findViewById(R.id.txtStatus);
            txtTimestamp = itemView.findViewById(R.id.txtTimestamp);
            txtPaymentMode = itemView.findViewById(R.id.txtPaymentMode);
        }
    }
}