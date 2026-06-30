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

public class ExpenseAdapter extends RecyclerView.Adapter<ExpenseAdapter.ViewHolder> {

    private List<Expense> expenseList;
    private OnExpenseActionListener listener;

    public interface OnExpenseActionListener {
        void onDeleteClick(Expense expense);
    }

    public ExpenseAdapter(List<Expense> expenseList) {
        this.expenseList = expenseList;
    }

    public void setOnExpenseActionListener(OnExpenseActionListener listener) {
        this.listener = listener;
    }

    public void updateList(List<Expense> newList) {
        this.expenseList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_expense, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Expense expense = expenseList.get(position);
        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("en", "KE")); //displays Kenyan Currency symbol

        holder.txtCategory.setText(expense.getCategory());
        holder.txtAmount.setText(formatter.format(expense.getAmount()));
        holder.txtDate.setText(expense.getDate());

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteClick(expense);
            }
        });
    }

    @Override
    public int getItemCount() {
        return expenseList != null ? expenseList.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtCategory, txtAmount, txtDate;
        TextView btnDelete;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtCategory = itemView.findViewById(R.id.txtExpenseCategory);
            txtAmount = itemView.findViewById(R.id.txtExpenseAmount);
            txtDate = itemView.findViewById(R.id.txtExpenseDate);
            btnDelete = itemView.findViewById(R.id.btnDeleteExpense);
        }
    }
}