package com.example.notekeeper.ui.notes;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.notekeeper.DataManager;
import com.example.notekeeper.NoteInfo;

import java.util.List;

public class NotesViewModel extends ViewModel {

    private MutableLiveData<List<NoteInfo>> mNotes;
    private List<NoteInfo> previewNotes;

    public NotesViewModel() {
        mNotes = new MutableLiveData<>();
 //       previewNotes = DataManager.getInstance().getNotes();
        mNotes.setValue(null);
    }

    public LiveData<List<NoteInfo>> getNotes() {
        return mNotes;
    }
}