package com.example.notekeeper.ui.courses;

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
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.notekeeper.CourseInfo;
import com.example.notekeeper.CourseRecyclerAdapter;
import com.example.notekeeper.R;

import java.util.List;

public class CoursesFragment extends Fragment {

    private CoursesViewModel mCoursesViewModel;
    private CourseRecyclerAdapter mCourseRecyclerAdapter;
    private View mRoot;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        mCoursesViewModel =
                ViewModelProviders.of(this).get(CoursesViewModel.class);
        mRoot = inflater.inflate(R.layout.fragment_courses, container, false);
        final RecyclerView recyclerCourses = mRoot.findViewById(R.id.list_courses);
        final GridLayoutManager courseLayoutManager = new GridLayoutManager(getContext(), 2);

        mCourseRecyclerAdapter = new CourseRecyclerAdapter(getContext(), mCoursesViewModel.getCourses().getValue());
        recyclerCourses.setLayoutManager(courseLayoutManager);
        recyclerCourses.setAdapter(mCourseRecyclerAdapter);

        mCoursesViewModel.getCourses().observe(getViewLifecycleOwner(), new Observer<List<CourseInfo>>() {
            @Override
            public void onChanged(List<CourseInfo> courseInfos) {
                mCourseRecyclerAdapter.notifyDataSetChanged();
            }
        });

        return mRoot;
    }
}
