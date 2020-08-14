package com.example.notekeeper;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.LoaderManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.lifecycle.ViewModelProvider;

import android.os.SystemClock;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;

import com.example.notekeeper.NoteKeeperDatabaseContract.CourseInfoEntry;
import com.example.notekeeper.NoteKeeperDatabaseContract.NoteInfoEntry;
import com.example.notekeeper.NoteKeeperProviderContract.Courses;
import com.example.notekeeper.NoteKeeperProviderContract.Notes;

import static com.example.notekeeper.NoteActivityViewModel.ORIGINAL_NOTE_COURSE_ID;
import static com.example.notekeeper.NoteActivityViewModel.ORIGINAL_NOTE_TEXT;
import static com.example.notekeeper.NoteActivityViewModel.ORIGINAL_NOTE_TITLE;
import static com.example.notekeeper.NoteActivityViewModel.ORIGINAL_NOTE_URI;

public class NoteActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {
    public static final int LOADER_NOTES = 0;
    public static final int LOADER_COURSES = 1;
    public static final int NOTES_NOTIFICATION_ID = 0;
    private final  String TAG = getClass().getSimpleName();
    public static final String CHANNEL_ID = "noteReminderNotification";
    public static final String NOTE_ID = "com.example.notekeeper.NOTE_ID";
    public static final int ID_NOT_SET = -1;
    private NoteInfo mNote;
    private boolean mIsNewNote;
    private Spinner mSpinnerCourses;
    private EditText mTextNoteTitle;
    private EditText mTextNoteText;
    private int mNoteId;
    private boolean mIsCancelling;
    private  NoteActivityViewModel mViewModel;
    private NoteKeeperOpenHelper mDbOpenHelper;
    private Cursor mNoteCursor;
    private int mCourseIdPos;
    private int mNoteTitlePos;
    private int mNoteTextPos;
    private SimpleCursorAdapter mAdapterCourses;
    private boolean mCoursesQueryFinished;
    private boolean mNotesQueryFinished;
    private Uri mNoteUri;
    private ModuleStatusView mViewModuleStatus;

    @Override
    protected void onDestroy() {
        mDbOpenHelper.close();
        super.onDestroy();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // register notification channel
        createNotificationChannel();

        mDbOpenHelper = new NoteKeeperOpenHelper(this);

        ViewModelProvider viewModelProvider = new ViewModelProvider(getViewModelStore(),
                ViewModelProvider.AndroidViewModelFactory.getInstance(getApplication()));
        mViewModel = viewModelProvider.get(NoteActivityViewModel.class);

        if(mViewModel.mIsNewlyCreated && savedInstanceState != null)
            mViewModel.restoreState(savedInstanceState);

        mViewModel.mIsNewlyCreated = false;

        mSpinnerCourses = findViewById(R.id.spinner_courses);
        mAdapterCourses = new SimpleCursorAdapter(this,
                android.R.layout.simple_spinner_item, null,
                new String[] {CourseInfoEntry.COLUMN_COURSE_TITLE},
                new int[] {android.R.id.text1}, 0);
        mAdapterCourses.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinnerCourses.setAdapter(mAdapterCourses);

//        loadCourseData();
        getLoaderManager().restartLoader(LOADER_COURSES, null, this);

        mTextNoteTitle = findViewById(R.id.text_note_title);
        mTextNoteText = findViewById(R.id.text_note_text);

        readDisplayStateValues();
//        saveOriginalNoteValues();

        if(!mIsNewNote)
//            displayNote();
//            loadNoteData();
            getLoaderManager().initLoader(LOADER_NOTES, null, this);

        mViewModuleStatus = findViewById(R.id.module_status);
        loadModuleStatusValues();
        Log.d(TAG, "*****************onCreate*****************");
    }

    private void loadModuleStatusValues() {
        // in real life module status value would be fetched from a content provider
        int totalNumberOfModules = 10;
        int completedNumberOfModules =6;
        boolean[] moduleStatus = new boolean[totalNumberOfModules];
        for (int moduleIndex = 0; moduleIndex < completedNumberOfModules; moduleIndex++)
            moduleStatus[moduleIndex] = true;
        mViewModuleStatus.setmModuleStatus(moduleStatus);

    }

    private void loadCourseData() {
        SQLiteDatabase db = mDbOpenHelper.getReadableDatabase();
        String[] courseColumns = {
                CourseInfoEntry.COLUMN_COURSE_TITLE,
                CourseInfoEntry.COLUMN_COURSE_ID,
                CourseInfoEntry._ID
        };
        Cursor cursor = db.query(CourseInfoEntry.TABLE_NAME, courseColumns,
                null, null, null, null,
                CourseInfoEntry.COLUMN_COURSE_TITLE + " ASC");
        mAdapterCourses.changeCursor(cursor);

    }

    private void loadNoteData() {
        SQLiteDatabase db = mDbOpenHelper.getReadableDatabase();

        String selection = NoteInfoEntry._ID + " = ? ";
        String[] selectionArgs = {Integer.toString(mNoteId)};

        String[] noteColumns = {
                NoteInfoEntry.COLUMN_COURSE_ID,
                NoteInfoEntry.COLUMN_NOTE_TITLE,
                NoteInfoEntry.COLUMN_NOTE_TEXT,
                NoteInfoEntry._ID
        };
        mNoteCursor = db.query(NoteInfoEntry.TABLE_NAME, noteColumns,
                selection, selectionArgs, null, null, null);
        mCourseIdPos = mNoteCursor.getColumnIndex(NoteInfoEntry.COLUMN_COURSE_ID);
        mNoteTitlePos = mNoteCursor.getColumnIndex(NoteInfoEntry.COLUMN_NOTE_TITLE);
        mNoteTextPos = mNoteCursor.getColumnIndex(NoteInfoEntry.COLUMN_NOTE_TEXT);

        mNoteCursor.moveToNext();
        displayNote();
    }

    private void saveOriginalNoteValues() {
        if(mIsNewNote)
            return;


//        mViewModel.mOriginalNoteCourseId = mNote.getCourse().getCourseId();
//        mViewModel.mOriginalNoteTitle = mNote.getTitle();
//        mViewModel.mOriginalNoteText = mNote.getText();
    }

    private void displayNote() {
        String courseId = mNoteCursor.getString(mCourseIdPos);
        String noteTitle = mNoteCursor.getString(mNoteTitlePos);
        String noteText = mNoteCursor.getString(mNoteTextPos);

//        List<CourseInfo> courses = DataManager.getInstance().getCourses();
//        CourseInfo course = DataManager.getInstance().getCourse(courseId);
//        int courseIndex = courses.indexOf(course);

        int courseIndex = getIndexOfCourseId(courseId);

        mSpinnerCourses.setSelection(courseIndex);
        mTextNoteTitle.setText(noteTitle);
        mTextNoteText.setText(noteText);

        CourseEventBroadCastHelper.sendEventBroadcast(this, courseId, "Editing note");
    }

    private int getIndexOfCourseId(String courseId) {
        Cursor cursor = mAdapterCourses.getCursor();
        int courseIdPos = -1;
        int courseRowIndex = 0;
        boolean more = false;
        if (cursor != null) {
            courseIdPos = cursor.getColumnIndex(Courses.COLUMN_COURSE_ID);
            more = cursor.moveToFirst();
        }


        while (more) {
            String cursorCourseId = cursor.getString(courseIdPos);
            if(courseId.equals(cursorCourseId))
                break;

            courseRowIndex++;
            cursor.moveToNext();
        }
        return courseRowIndex;
    }

    private void readDisplayStateValues() {
        Intent intent = getIntent();
//        mNote = intent.getParcelableExtra(NOTE_POSITION);
//        mIsNewNote = mNote == null;
        mNoteId = intent.getIntExtra(NOTE_ID, ID_NOT_SET);
        mIsNewNote = mNoteId == ID_NOT_SET;
        if(mIsNewNote)
            createNewNote();

        Log.i(TAG, "readDisplayStateValues: mNoteId: " + mNoteId);
//            mNote = DataManager.getInstance().getNotes().get(mNoteId);

    }

    private void createNewNote() {
//        DataManager dm = DataManager.getInstance();
//        mNoteId = dm.createNewNote();
//        mNote = dm.getNotes().get(mNoteId);

//        ContentValues values = new ContentValues();
//        values.put(NoteInfoEntry.COLUMN_COURSE_ID, "");
//        values.put(NoteInfoEntry.COLUMN_NOTE_TITLE, "");
//        values.put(NoteInfoEntry.COLUMN_NOTE_TEXT, "");

//        SQLiteDatabase db = mDbOpenHelper.getWritableDatabase();
//        mNoteId = (int) db.insert(NoteInfoEntry.TABLE_NAME, null, values);

        @SuppressLint("StaticFieldLeak")
        AsyncTask<ContentValues, Integer, Uri> task = new AsyncTask<ContentValues, Integer, Uri>() {
            private ProgressBar mProgressBar;

            @Override
            protected void onPreExecute() {
                mProgressBar = findViewById(R.id.progressBar);
                mProgressBar.setVisibility(View.VISIBLE);
                mProgressBar.setProgress(1);
            }

            @Override
            protected Uri doInBackground(ContentValues... contentValues) {
                Log.d(TAG, "doInBackground - thread: " + Thread.currentThread().getId());
                ContentValues insertValues = contentValues[0];
                Uri rowUri = getContentResolver().insert(Notes.CONTENT_URI, insertValues);
                simulateLongRunningWork();
                publishProgress(2);
                simulateLongRunningWork();
                publishProgress(3);
                return rowUri;
            }

            @Override
            protected void onProgressUpdate(Integer... values) {
                int progressValue = values[0];
                mProgressBar.setProgress(progressValue);
            }

            @Override
            protected void onPostExecute(Uri uri) {
                Log.d(TAG, "onPostExecute - thread: " + Thread.currentThread().getId());
                mNoteUri = uri;
                mProgressBar.setVisibility(View.GONE);
            }
        };

        // using content provider
        ContentValues values = new ContentValues();
        values.put(Notes.COLUMN_COURSE_ID, "");
        values.put(Notes.COLUMN_NOTE_TITLE, "");
        values.put(Notes.COLUMN_NOTE_TEXT, "");

//        mNoteUri = getContentResolver().insert(Notes.CONTENT_URI, values);
            Log.d(TAG, "Call to execute - thread: " + Thread.currentThread().getId());
        task.execute(values);


    }

    private void simulateLongRunningWork() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_note, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_send_mail) {
            sendEmail();
            return true;
        } else if(id == R.id.action_cancel) {
            mIsCancelling = true;
            finish();
        } else if(id == R.id.action_next) {
            moveNext();
        } else if (id == R.id.action_set_reminder) {
            showReminderNotification();
        }

        return super.onOptionsItemSelected(item);
    }

    private void showReminderNotification() {
        String noteTitle = mTextNoteTitle.getText().toString();
        String noteText = mTextNoteText.getText().toString();

        int noteId   = (int) ContentUris.parseId(mNoteUri);

//        handleNotificationDisplay(this, noteTitle, noteText, noteId);

        // use alarm to schedule a call to noteReminderBroadcastReceiver
        Intent intent = new Intent(getApplicationContext(), NoteReminderReceiver.class);
        intent.putExtra(NoteReminderReceiver.EXTRA_NOTE_TITLE, noteTitle);
        intent.putExtra(NoteReminderReceiver.EXTRA_NOTE_TEXT, noteText);
        intent.putExtra(NoteReminderReceiver.EXTRA_NOTE_ID, noteId);

        // creating the pending intent
        PendingIntent pendingIntent =
                PendingIntent.getBroadcast(getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        // reference to the alarm manager
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);

        // setting the alarm manager
        // 1. calculate the time we want to set the alarm to
        long currentTimeInMilliseconds  = SystemClock.elapsedRealtime();
        long ONE_HOUR = 60 * 60 * 1000;
        long TEN_SECONDS = 1000;
        // 2. set the time when the alarm will fire
        long alarmTime = currentTimeInMilliseconds + TEN_SECONDS;
        // 3. setting the alarm
        alarmManager.set(AlarmManager.ELAPSED_REALTIME, alarmTime, pendingIntent);



    }

    public static void handleNotificationDisplay(Context context, String noteTitle, String noteText, int noteId) {

        // Create an explicit intent for an Activity in your app
        Intent noteActivityIntent = new Intent(context, NoteActivity.class);
        noteActivityIntent.putExtra(NoteActivity.NOTE_ID, noteId);
        noteActivityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        // view note pending intent
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0, noteActivityIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        // view all notes pending intent
        PendingIntent allNotesPendingIntent = PendingIntent.getActivity(
                context,
                0, new Intent(context, MainActivity1.class),
                PendingIntent.FLAG_UPDATE_CURRENT
        );

        // Create an explicit intent for Backup Service in your app
        Intent backUpServiceIntent = new Intent(context, NoteBackupService.class);
        backUpServiceIntent.putExtra(NoteBackupService.EXTRA_COURSE_ID, NoteBackup.ALL_COURSES);

        // backup notes pending intent
        PendingIntent backupNotesPendingIntent = PendingIntent.getService(
                context,
                0, backUpServiceIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
        );


        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_assignment_black_24dp)
                .setContentTitle(noteTitle)
                .setContentText(noteText)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)

                // Set ticker text (preview) information for this notification.
                .setTicker("Review note")

                // Set display style for our notification
                .setStyle(new NotificationCompat.BigTextStyle()
                                .bigText(noteText)
                                .setBigContentTitle(noteTitle)
                        // .setSummaryText("Review note")
                )

                // Set the intent that will fire when the user taps the notification
                .setContentIntent(pendingIntent)

                // Add action
                .addAction(
                        0,
                       "View all notes",
                        allNotesPendingIntent
                )
                .addAction(
                        0,
                        "Backup notes",
                        backupNotesPendingIntent
                )

                // Automatically dismiss the notification when it is touched.
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

        // notificationId is a unique int for each notification that you must define
        notificationManager.notify(NOTES_NOTIFICATION_ID, builder.build());
    }


    // create notificationChannel and set importance
    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }


    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem item = menu.findItem(R.id.action_next);
        int noteLastIndex = DataManager.getInstance().getNotes().size() - 1;
        item.setEnabled(mNoteId < noteLastIndex);
        return super.onPrepareOptionsMenu(menu);
    }

    private void moveNext() {
        saveNote();

        ++mNoteId;
        mNote = DataManager.getInstance().getNotes().get(mNoteId);

 //       saveOriginalNoteValues();
        displayNote();
        invalidateOptionsMenu();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(mIsCancelling) {
            Log.i(TAG, "onPause: Canceling note at position " + mNoteId);
              if(mIsNewNote){
//                  DataManager.getInstance().removeNote(mNoteId);
                  deleteNoteFromDatabase();
              } else {
                  finish();
//                  storePreviousNoteValues();
              }

        } else {
            saveNote();
        }

        Log.d(TAG, "onPause");
    }

    private void deleteNoteFromDatabase() {
//        final String selection = NoteInfoEntry._ID + " = ? ";
//        final String[] selectionArgs = {Integer.toString(mNoteId)};

        @SuppressLint("StaticFieldLeak") AsyncTask task = new AsyncTask() {
            @Override
            protected Object doInBackground(Object[] objects) {
//                SQLiteDatabase db = mDbOpenHelper.getWritableDatabase();
//                db.delete(NoteInfoEntry.TABLE_NAME, selection, selectionArgs);
                getContentResolver().delete(mNoteUri, null, null);
                return null;
            }
        };
        task.execute();
    }

    private void storePreviousNoteValues() {
        CourseInfo course = DataManager.getInstance().getCourse(mViewModel.mOriginalNoteCourseId);
        mNote.setCourse(course);
        mNote.setTitle(mViewModel.mOriginalNoteTitle);
        mNote.setText(mViewModel.mOriginalNoteText);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if(outState != null)
            mViewModel.saveState(outState);
        outState.putString(ORIGINAL_NOTE_COURSE_ID, mViewModel.mOriginalNoteCourseId);
        outState.putString(ORIGINAL_NOTE_TITLE, mViewModel.mOriginalNoteTitle);
        outState.putString(ORIGINAL_NOTE_TEXT, mViewModel.mOriginalNoteText);

        outState.putString(ORIGINAL_NOTE_URI, mNoteUri.toString());
    }

    private void saveNote() {
//        mNote.setCourse((CourseInfo) mSpinnerCourses.getSelectedItem());
//        mNote.setTitle(mTextNoteTitle.getText().toString());
//        mNote.setText(mTextNoteText.getText().toString());

        String courseId = selectedCourseId();
        String noteTitle = mTextNoteTitle.getText().toString();
        String noteText = mTextNoteText.getText().toString();

        saveNoteToDatabase(courseId, noteTitle, noteText);
    }

    private String selectedCourseId() {
        int selectedPosition = mSpinnerCourses.getSelectedItemPosition();
        Cursor cursor = mAdapterCourses.getCursor();
        cursor.moveToPosition(selectedPosition);

        int courseIdPos = cursor.getColumnIndex(Notes.COLUMN_COURSE_ID);
        String courseId = cursor.getString(courseIdPos);
        return courseId;

    }

    private void saveNoteToDatabase(String courseId, String noteTitle, String noteText) {
//        String selection = NoteInfoEntry._ID + " = ?";
//        String[] selectionArgs = {Integer.toString(mNoteId)};
//
//        ContentValues values = new ContentValues();
//        values.put(NoteInfoEntry.COLUMN_COURSE_ID, courseId);
//        values.put(NoteInfoEntry.COLUMN_NOTE_TITLE, noteTitle);
//        values.put(NoteInfoEntry.COLUMN_NOTE_TEXT, noteText);
//
//        SQLiteDatabase db = mDbOpenHelper.getWritableDatabase();
//        db.update(NoteInfoEntry.TABLE_NAME, values, selection, selectionArgs);

        // using content provider
        ContentValues values = new ContentValues();
        values.put(Notes.COLUMN_COURSE_ID, courseId);
        values.put(Notes.COLUMN_NOTE_TITLE, noteTitle);
        values.put(Notes.COLUMN_NOTE_TEXT, noteText);

        getContentResolver().update(mNoteUri, values, null, null);
    }

    private void sendEmail() {
        CourseInfo course = (CourseInfo) mSpinnerCourses.getSelectedItem();
        String subject = mTextNoteTitle.getText().toString();
        String text = "Checkout what i learnt in the pluralsight course\""
                + course.getTitle() + "\"\n" + mTextNoteText.getText();

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("message/rfc2822");
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        intent.putExtra(Intent.EXTRA_TEXT, text);

        startActivity(intent);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        CursorLoader loader = null;
        if(id == LOADER_NOTES)
            loader = createLoaderNotes();
        else if(id == LOADER_COURSES)
            loader = createLoaderCourses();
        return loader;
    }

    @SuppressLint("StaticFieldLeak")
    private CursorLoader createLoaderCourses() {
        mCoursesQueryFinished = false;
//        return new CursorLoader(this) {
//            @Override
//            public Cursor loadInBackground() {
//                SQLiteDatabase db = mDbOpenHelper.getReadableDatabase();
//                String[] courseColumns = {
//                        CourseInfoEntry.COLUMN_COURSE_TITLE,
//                        CourseInfoEntry.COLUMN_COURSE_ID,
//                        CourseInfoEntry._ID
//                };
//                Cursor cursor = db.query(CourseInfoEntry.TABLE_NAME, courseColumns,
//                        null, null, null, null,
//                        CourseInfoEntry.COLUMN_COURSE_TITLE + " ASC");
//                return cursor;
//            }
//        };

        //****** using content provider to populate spinner courses *****//
//        Uri uri =  Uri.parse("content://com.example.notekeeper.provider");
//        String[] courseColumns = {
//                CourseInfoEntry.COLUMN_COURSE_TITLE,
//                CourseInfoEntry.COLUMN_COURSE_ID,
//                CourseInfoEntry._ID
//        };
//
//        return new CursorLoader(this, uri, courseColumns, null, null,
//                CourseInfoEntry.COLUMN_COURSE_TITLE + " ASC");

        Uri uri = Courses.CONTENT_URI;
        String[] courseColumns = {
                Courses.COLUMN_COURSE_TITLE,
                Courses.COLUMN_COURSE_ID,
                Courses._ID
        };

        return new CursorLoader(this, uri, courseColumns, null, null,
                Courses.COLUMN_COURSE_TITLE + " ASC");
    }

    @SuppressLint("StaticFieldLeak")
    private  CursorLoader createLoaderNotes() {
        mNotesQueryFinished = false;
//        return new CursorLoader(this) {
//            @Override
//            public Cursor loadInBackground() {
//                SQLiteDatabase db = mDbOpenHelper.getReadableDatabase();
//
//                String selection = NoteInfoEntry._ID + " = ? ";
//                String[] selectionArgs = {Integer.toString(mNoteId)};
//
//                String[] noteColumns = {
//                        NoteInfoEntry.COLUMN_COURSE_ID,
//                        NoteInfoEntry.COLUMN_NOTE_TITLE,
//                        NoteInfoEntry.COLUMN_NOTE_TEXT,
//                        NoteInfoEntry._ID
//                };
//                return db.query(NoteInfoEntry.TABLE_NAME, noteColumns,
//                        selection, selectionArgs, null, null, null);
//            }
//        };

        // displaying a note using content provider
        String[] noteColumns = {
                Notes.COLUMN_COURSE_ID,
                Notes.COLUMN_NOTE_TITLE,
                Notes.COLUMN_NOTE_TEXT,
                Notes._ID
        };

        mNoteUri = ContentUris.withAppendedId(Notes.CONTENT_URI, mNoteId);
        return new CursorLoader(this, mNoteUri, noteColumns, null, null, null);

    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (loader.getId() == LOADER_NOTES)
            loadFinishedNotes(data);
        else if(loader.getId() == LOADER_COURSES)
            mAdapterCourses.changeCursor(data);
            mCoursesQueryFinished = true;
            displayNoteWhenQueryIsFinished();

    }

    private void loadFinishedNotes(Cursor data) {
        mNoteCursor = data;
        mCourseIdPos = mNoteCursor.getColumnIndex(Notes.COLUMN_COURSE_ID);
        mNoteTitlePos = mNoteCursor.getColumnIndex(Notes.COLUMN_NOTE_TITLE);
        mNoteTextPos = mNoteCursor.getColumnIndex(Notes.COLUMN_NOTE_TEXT);

        mNoteCursor.moveToNext();
        mNotesQueryFinished = true;
        displayNoteWhenQueryIsFinished();

    }

    private void displayNoteWhenQueryIsFinished() {
        if (mNotesQueryFinished && mCoursesQueryFinished)
            displayNote();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        if (loader.getId() == LOADER_NOTES) {
            if (mNoteCursor != null)
                mNoteCursor.close();
        } else if (loader.getId() == LOADER_COURSES) {
            mAdapterCourses.changeCursor(null);
        }
    }
}
