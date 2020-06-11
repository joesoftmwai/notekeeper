package com.example.notekeeper.ui.notes;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
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

import com.example.notekeeper.DataManager;
import com.example.notekeeper.NoteInfo;
import com.example.notekeeper.NoteKeeperDatabaseContract;
import com.example.notekeeper.NoteKeeperDatabaseContract.NoteInfoEntry;
import com.example.notekeeper.NoteKeeperOpenHelper;
import com.example.notekeeper.NoteRecyclerAdapter;
import com.example.notekeeper.R;

import java.util.List;

public class NotesFragment extends Fragment {

    private NotesViewModel mNotesViewModel;
    private NoteRecyclerAdapter mNoteRecyclerAdapter;
    private NoteKeeperOpenHelper mDbOpenHelper;
    private View mRoot;


    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        mDbOpenHelper = new NoteKeeperOpenHelper(getContext());

        mNotesViewModel =
                ViewModelProviders.of(this).get(NotesViewModel.class);
        mRoot = inflater.inflate(R.layout.fragment_notes, container, false);

        initializeDisplayContent();

        DataManager.loadFromDatabase(mDbOpenHelper);

        return mRoot;
    }

    @Override
    public void onDestroy() {
        mDbOpenHelper.close();
        super.onDestroy();
    }

    @Override
    public void onResume() {
        super.onResume();
//        mNoteRecyclerAdapter.notifyDataSetChanged();
        loadNotes();
    }

    private void loadNotes() {
        SQLiteDatabase db = mDbOpenHelper.getReadableDatabase();
        final String[] noteColumns = {
                NoteInfoEntry.COLUMN_NOTE_TITLE,
                NoteInfoEntry.COLUMN_COURSE_ID,
                NoteInfoEntry._ID
        };

        String noteOrderBy = NoteInfoEntry.COLUMN_COURSE_ID + ", " + NoteInfoEntry.COLUMN_NOTE_TITLE;
        final Cursor noteCursor = db.query(NoteInfoEntry.TABLE_NAME, noteColumns,
                null, null, null, null, noteOrderBy + " ASC");
        mNoteRecyclerAdapter.changeCursor(noteCursor);
    }

    private void initializeDisplayContent() {
        final RecyclerView recyclerNotes = mRoot.findViewById(R.id.list_notes);
        final LinearLayoutManager notesLayoutManager = new LinearLayoutManager(getContext());

        mNoteRecyclerAdapter = new NoteRecyclerAdapter(getContext(), null);
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
