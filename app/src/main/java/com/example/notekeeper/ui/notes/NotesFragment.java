package com.example.notekeeper.ui.notes;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.notekeeper.NoteInfo;
import com.example.notekeeper.NoteRecyclerAdapter;
import com.example.notekeeper.R;

import java.util.List;

public class NotesFragment extends Fragment {

    private NotesViewModel mNotesViewModel;
    private NoteRecyclerAdapter mNoteRecyclerAdapter;
    private View mRoot;


    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        mNotesViewModel =
                ViewModelProviders.of(this).get(NotesViewModel.class);
        mRoot = inflater.inflate(R.layout.fragment_notes, container, false);

        initializeDisplayContent();

        return mRoot;
    }

    @Override
    public void onResume() {
        super.onResume();
        mNoteRecyclerAdapter.notifyDataSetChanged();
    }

    private void initializeDisplayContent() {
        final RecyclerView recyclerNotes = mRoot.findViewById(R.id.list_notes);
        final LinearLayoutManager notesLayoutManager = new LinearLayoutManager(getContext());

        mNoteRecyclerAdapter = new NoteRecyclerAdapter(getContext(), mNotesViewModel.getNotes().getValue());
        recyclerNotes.setLayoutManager(notesLayoutManager);
        recyclerNotes.setAdapter(mNoteRecyclerAdapter);

        mNotesViewModel.getNotes().observe(getViewLifecycleOwner(), new Observer<List<NoteInfo>>() {
            @Override
            public void onChanged(List<NoteInfo> noteInfos) {
                mNoteRecyclerAdapter.notifyDataSetChanged();
            }
        });

    }
}
