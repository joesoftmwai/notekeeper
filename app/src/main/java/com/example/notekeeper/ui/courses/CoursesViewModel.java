package com.example.notekeeper.ui.courses;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.notekeeper.CourseInfo;
import com.example.notekeeper.DataManager;

import java.util.List;

public class CoursesViewModel extends ViewModel {

    private MutableLiveData<List<CourseInfo>> mCourses;
    private List<CourseInfo> previewCourses;

    public CoursesViewModel() {
        mCourses = new MutableLiveData<>();
        previewCourses = DataManager.getInstance().getCourses();
        mCourses.setValue(previewCourses);
    }

    public LiveData<List<CourseInfo>> getCourses() {
        return mCourses;
    }
}