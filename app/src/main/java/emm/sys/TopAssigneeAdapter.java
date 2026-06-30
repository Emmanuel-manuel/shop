package emm.sys;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class TopAssigneeAdapter extends RecyclerView.Adapter<TopAssigneeAdapter.ViewHolder> {
    private List<TopAssignee> assigneeList;

    public TopAssigneeAdapter(List<TopAssignee> assigneeList) {
        this.assigneeList = assigneeList;
    }

    public void updateList(List<TopAssignee> newList) {
        this.assigneeList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_top_assignee, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TopAssignee assignee = assigneeList.get(position);

        String rankColor;
        if (position == 0) rankColor = "#FFD700";
        else if (position == 1) rankColor = "#C0C0C0";
        else if (position == 2) rankColor = "#CD7F32";
        else rankColor = "#757575";

        holder.txtRank.setText(String.valueOf(assignee.getRank()));
        holder.txtRank.setBackgroundColor(android.graphics.Color.parseColor(rankColor));
        holder.txtAssigneeName.setText(assignee.getName());
        holder.txtAssigneeQuantity.setText(assignee.getQuantity() + " units");
    }

    @Override
    public int getItemCount() {
        return assigneeList != null ? assigneeList.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtRank, txtAssigneeName, txtAssigneeQuantity;
        ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtRank = itemView.findViewById(R.id.txtRank);
            txtAssigneeName = itemView.findViewById(R.id.txtAssigneeName);
            txtAssigneeQuantity = itemView.findViewById(R.id.txtAssigneeQuantity);
        }
    }
}