package emm.sys;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

public class NotepadFragment extends Fragment {

    private RecyclerView recyclerView;
    private NoteAdapter noteAdapter;
    private DBHelper dbHelper;
    private List<Note> noteList;
    private TextView emptyView;
    private EditText searchEditText;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_notepad, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        dbHelper      = new DBHelper(requireContext());
        recyclerView  = view.findViewById(R.id.notesRecyclerView);
        emptyView     = view.findViewById(R.id.emptyView);
        searchEditText = view.findViewById(R.id.searchEditText);

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        loadNotes();

        // FAB → open blank editor
        FloatingActionButton fabNewNote = view.findViewById(R.id.fabNewNote);
        fabNewNote.setOnClickListener(v -> openNoteEditor(null));

        // Live search
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterNotes(s.toString().trim());
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh list when returning from editor
        loadNotes();
    }

    private void loadNotes() {
        noteList = dbHelper.getAllNotes();
        noteAdapter = new NoteAdapter(noteList, note -> openNoteEditor(note));
        recyclerView.setAdapter(noteAdapter);
        toggleEmptyView();
    }

    private void filterNotes(String query) {
        List<Note> filtered = dbHelper.searchNotes(query);
        noteAdapter.updateList(filtered);
        toggleEmptyView();
    }

    private void openNoteEditor(Note note) {
        NoteEditorFragment editor = NoteEditorFragment.newInstance(note);
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(
                        R.anim.fragment_enter, R.anim.fragment_exit,
                        R.anim.fragment_enter, R.anim.fragment_exit)
                .replace(R.id.fragmentContainer, editor)
                .addToBackStack(null)
                .commit();
    }

    private void toggleEmptyView() {
        if (noteAdapter.getItemCount() == 0) {
            emptyView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }
}
