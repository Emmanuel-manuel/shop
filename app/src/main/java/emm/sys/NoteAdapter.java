package emm.sys;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class NoteAdapter extends RecyclerView.Adapter<NoteAdapter.NoteViewHolder> {

    public interface OnNoteClickListener {
        void onNoteClick(Note note);
    }

    private List<Note>          noteList;
    private OnNoteClickListener listener;

    public NoteAdapter(List<Note> noteList, OnNoteClickListener listener) {
        this.noteList = noteList;
        this.listener = listener;
    }

    // Called externally (search / refresh)
    public void updateList(List<Note> newList) {
        this.noteList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_note, parent, false);
        return new NoteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        Note note = noteList.get(position);
        holder.bind(note, listener);
    }

    @Override
    public int getItemCount() {
        return noteList == null ? 0 : noteList.size();
    }

    // ── ViewHolder ───────────────────────────────────────────────────────────
    static class NoteViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvTitle;
        private final TextView tvSnippet;
        private final TextView tvDate;

        NoteViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle   = itemView.findViewById(R.id.tvNoteTitle);
            tvSnippet = itemView.findViewById(R.id.tvNoteSnippet);
            tvDate    = itemView.findViewById(R.id.tvNoteDate);
        }

        void bind(Note note, OnNoteClickListener listener) {
            tvTitle.setText(note.getTitle());
            tvSnippet.setText(note.getSnippet());

            // Format timestamp: "2025-04-08 14:30:00" → "Apr 8, 2025"
            tvDate.setText(formatDate(note.getTimestamp()));

            itemView.setOnClickListener(v -> listener.onNoteClick(note));
        }

        private String formatDate(String raw) {
            if (raw == null || raw.length() < 10) return raw != null ? raw : "";
            try {
                // raw format: "YYYY-MM-DD HH:MM:SS"
                String[] parts = raw.substring(0, 10).split("-");
                int year  = Integer.parseInt(parts[0]);
                int month = Integer.parseInt(parts[1]);
                int day   = Integer.parseInt(parts[2]);
                String[] months = {"Jan","Feb","Mar","Apr","May","Jun",
                        "Jul","Aug","Sep","Oct","Nov","Dec"};
                return months[month - 1] + " " + day + ", " + year;
            } catch (Exception e) {
                return raw;
            }
        }
    }
}
