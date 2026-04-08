package emm.sys;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class NoteEditorFragment extends Fragment {

    private static final String ARG_NOTE_ID      = "note_id";
    private static final String ARG_NOTE_TITLE   = "note_title";
    private static final String ARG_NOTE_CONTENT = "note_content";

    private EditText etTitle, etContent;
    private Button   btnSave;
    private Button   btnDelete;
    private TextView tvCharCount;
    private DBHelper dbHelper;
    private int      noteId = -1;   // -1 means new note

    // ── Factory ──────────────────────────────────────────────────────────────
    public static NoteEditorFragment newInstance(@Nullable Note note) {
        NoteEditorFragment fragment = new NoteEditorFragment();
        Bundle args = new Bundle();
        if (note != null) {
            args.putInt(ARG_NOTE_ID,      note.getId());
            args.putString(ARG_NOTE_TITLE,   note.getTitle());
            args.putString(ARG_NOTE_CONTENT, note.getContent());
        }
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_note_editor, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        dbHelper   = new DBHelper(requireContext());
        etTitle    = view.findViewById(R.id.etNoteTitle);
        etContent  = view.findViewById(R.id.etNoteContent);
        btnSave    = view.findViewById(R.id.btnSaveNote);
        btnDelete  = view.findViewById(R.id.btnDeleteNote);
        tvCharCount = view.findViewById(R.id.tvCharCount);

        ImageButton btnBack = view.findViewById(R.id.btnBack);

        // ── Populate if editing existing note ──
        Bundle args = getArguments();
        if (args != null && args.containsKey(ARG_NOTE_ID)) {
            noteId = args.getInt(ARG_NOTE_ID, -1);
            etTitle.setText(args.getString(ARG_NOTE_TITLE, ""));
            etContent.setText(args.getString(ARG_NOTE_CONTENT, ""));
            btnDelete.setVisibility(View.VISIBLE);
        } else {
            btnDelete.setVisibility(View.GONE);
        }

        updateCharCount();

        // ── Live character counter ──
        etContent.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) { updateCharCount(); }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });

        // ── Save ──
        btnSave.setOnClickListener(v -> saveNote());

        // ── Delete ──
        btnDelete.setOnClickListener(v -> confirmDelete());

        // ── Back ──
        btnBack.setOnClickListener(v -> requireActivity()
                .getSupportFragmentManager().popBackStack());
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void saveNote() {
        String title   = etTitle.getText().toString().trim();
        String content = etContent.getText().toString().trim();

        if (TextUtils.isEmpty(title) && TextUtils.isEmpty(content)) {
            Toast.makeText(requireContext(),
                    "Note is empty — nothing saved.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Use first line of content as auto-title if title left blank
        if (TextUtils.isEmpty(title)) {
            title = content.length() > 30 ? content.substring(0, 30) + "…" : content;
        }

        boolean success;
        if (noteId == -1) {
            success = dbHelper.insertNote(title, content);
        } else {
            success = dbHelper.updateNote(noteId, title, content);
        }

        if (success) {
            Toast.makeText(requireContext(), "Note saved ✓", Toast.LENGTH_SHORT).show();
            requireActivity().getSupportFragmentManager().popBackStack();
        } else {
            Toast.makeText(requireContext(), "Failed to save note.", Toast.LENGTH_SHORT).show();
        }
    }

    private void confirmDelete() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Note")
                .setMessage("Are you sure you want to delete this note? This cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    boolean deleted = dbHelper.deleteNote(noteId);
                    if (deleted) {
                        Toast.makeText(requireContext(),
                                "Note deleted.", Toast.LENGTH_SHORT).show();
                        requireActivity().getSupportFragmentManager().popBackStack();
                    } else {
                        Toast.makeText(requireContext(),
                                "Failed to delete note.", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateCharCount() {
        int count = etContent.getText().length();
        tvCharCount.setText(count + " chars");
    }
}
