package emm.sys;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

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
        private final TextView tvTime;

        NoteViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle   = itemView.findViewById(R.id.tvNoteTitle);
            tvSnippet = itemView.findViewById(R.id.tvNoteSnippet);
            tvDate    = itemView.findViewById(R.id.tvNoteDate);
            tvTime    = itemView.findViewById(R.id.tvNoteTime);  // Initialize time TextView
        }

        void bind(Note note, OnNoteClickListener listener) {
            tvTitle.setText(note.getTitle());
            tvSnippet.setText(note.getSnippet());

            // Format timestamp: "2025-04-08 14:30:00" → "Apr 8, 2025"
//            tvDate.setText(formatDate(note.getTimestamp()));

            // Format timestamp to show both date and time
            String[] dateAndTime = formatDateTime(note.getTimestamp());
            tvDate.setText(dateAndTime[0]);  // Date part
            tvTime.setText(dateAndTime[1]);  // Time part

            itemView.setOnClickListener(v -> listener.onNoteClick(note));
        }

        private String[] formatDateTime(String raw) {
            String[] result = new String[]{"", ""};

            if (raw == null || raw.isEmpty()) {
                return result;
            }

            try {
                // Parse the timestamp from DB: "YYYY-MM-DD HH:MM:SS"
                SimpleDateFormat dbFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                Date date = dbFormat.parse(raw);

                if (date != null) {
                    // Format date: "Apr 8, 2025"
                    SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
                    result[0] = dateFormat.format(date);

                    // Format time: "2:30 PM" (12-hour format) or "14:30" (24-hour format)
                    // Using 12-hour format with AM/PM
                    SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());
                    result[1] = timeFormat.format(date);
                }
            } catch (Exception e) {
                // Fallback: try to parse manually if SimpleDateFormat fails
                try {
                    if (raw.length() >= 16) {
                        // Extract time part: HH:MM:SS
                        String timePart = raw.substring(11, 16);
                        result[1] = timePart;

                        // Format date part
                        String[] dateParts = raw.substring(0, 10).split("-");
                        if (dateParts.length == 3) {
                            int year = Integer.parseInt(dateParts[0]);
                            int month = Integer.parseInt(dateParts[1]);
                            int day = Integer.parseInt(dateParts[2]);
                            String[] months = {"Jan","Feb","Mar","Apr","May","Jun",
                                    "Jul","Aug","Sep","Oct","Nov","Dec"};
                            result[0] = months[month - 1] + " " + day + ", " + year;
                        }
                    }
                } catch (Exception ex) {
                    result[0] = raw;
                    result[1] = "";
                }
            }

            return result;
        }
    }
}
