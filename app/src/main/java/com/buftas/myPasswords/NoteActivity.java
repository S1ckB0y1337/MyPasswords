//Nikolaos Katsiopis
//icsd13076

package com.buftas.myPasswords;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;

import org.apache.commons.lang3.SerializationUtils;

public class NoteActivity extends AppCompatActivity {

    private DatabaseConnectionHelper db_connection;
    private SQLiteDatabase database;
    private static final String dataType = "Note";
    private String currentUsername, entryName, noteText, mode;
    private EditText noteNameField, noteField;
    private Button submitButton;
    private Note editableObj = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note);
        setupUI(findViewById(R.id.parent));
        //Get the data from the caller Activity
        Intent intent = getIntent();
        mode = intent.getStringExtra("Mode");
        currentUsername = intent.getStringExtra("currentUsername");
        initializeGraphicElements();
        if (mode.equals("EditEntry")) {
            //Retrieve the passed object and cast it to the proper class
            editableObj = (Note) intent.getSerializableExtra("Object");
            //Set the fields to their existing values
            noteNameField.setText(editableObj.getName());
            noteField.setText(editableObj.getNote());
        }
    }

    private void initializeGraphicElements() {
        noteNameField = findViewById(R.id.newNoteNameField);
        noteField = findViewById(R.id.newNoteField);
        submitButton = findViewById(R.id.submitNewNote);
        if (mode.equals("NewEntry")) {
            submitButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (isTextFieldEmpty(noteField) || isTextFieldEmpty(noteNameField)) {
                        showMessage("Error!", "Please fill all fields!");
                    } else {
                        entryName = noteNameField.getText().toString().trim();
                        noteText = noteField.getText().toString();
                        addNewNoteObject();
                    }
                }
            });
        } else {
            submitButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (isTextFieldEmpty(noteField) || isTextFieldEmpty(noteNameField)) {
                        showMessage("Error!", "Please fill all fields!");
                    } else {
                        entryName = noteNameField.getText().toString().trim();
                        noteText = noteField.getText().toString();
                        editNoteObject();
                    }
                }
            });
        }
    }

    //This method sets up the UI in a manner that will hide the keyboard i the user hasn't touched on a EditText field
    public void setupUI(View view) {

        // Set up touch listener for non-text box views to hide keyboard. It works recursively
        if (!(view instanceof EditText)) {
            view.setOnTouchListener(new View.OnTouchListener() {
                public boolean onTouch(View v, MotionEvent event) {
                    hideSoftKeyboard(NoteActivity.this);
                    return false;
                }
            });
        }
    }

    //This method hides the virtual keyboard
    public static void hideSoftKeyboard(Activity activity) {
        InputMethodManager inputMethodManager =
                (InputMethodManager) activity.getSystemService(
                        Activity.INPUT_METHOD_SERVICE);
        if (inputMethodManager.isAcceptingText()) {
            inputMethodManager.hideSoftInputFromWindow(
                    activity.getCurrentFocus().getWindowToken(), 0);
        }
    }

    //This function checks if a provided EditText field is empty and returns it
    private boolean isTextFieldEmpty(EditText etText) {
        return etText.getText().toString().trim().length() == 0;
    }

    //This method provides an information dialog to the user
    private void showMessage(String title, String message) {
        InfoDialog info = new InfoDialog();
        info.setTitle(title);
        info.setMessage(message);
        info.show(getSupportFragmentManager(), title);
    }

    private void addNewNoteObject() {
        db_connection = new DatabaseConnectionHelper(NoteActivity.this);
        database = db_connection.getWritableDatabase();
        //First Check if the entry name the user provided already exists in his Vault
        Cursor c = database.query(DatabaseSchema.EncryptedData.TABLE_NAME,
                new String[]{DatabaseSchema.EncryptedData.ENTRY_NAME}, DatabaseSchema.EncryptedData.ENTRY_NAME +
                        " = ? AND " + DatabaseSchema.EncryptedData.USERNAME + " = ?", new String[]{entryName, currentUsername},
                null, null, null);
        if (c != null) {
            if (c.getCount() > 0) {
                showMessage("Error!", "This name exists!\nChoose another!");
            } else {
                Note noteObject = new Note(entryName, currentUsername, noteText);
                //Serialize the object and take the bytes
                //For our convenience i will use the external Apache library -> org.apache.commons
                byte[] objBytes = SerializationUtils.serialize(noteObject);
                //Then we will call the encryptBlob() method of our class DataManipulator to encrypt the data
                //First lets retrieve the encryption key from database for the current user
                String key = DataManipulator.getSymmetricKey(database, currentUsername);
                if (key != null) {
                    byte[] encryptedData = DataManipulator.encryptBlob(key, objBytes);
                    if (encryptedData != null) {
                        long result = db_connection.addEncryptedEntry(currentUsername, entryName, encryptedData, dataType, database);
                        //Close the connection to the database
                        database.close();
                        //Send the results back to the Main Activity
                        Intent intent = new Intent();
                        intent.putExtra("result", result);
                        if (result != -1) {
                            setResult(Activity.RESULT_OK, intent);
                        } else {
                            setResult(Activity.RESULT_CANCELED, intent);
                        }
                        //Return to previous Activity
                        finish();
                    } else {
                        showMessage("Encryption Error!", "Unexpected Error Occurred!");
                    }
                } else {
                    showMessage("Database Error!", "Unexpected Error Occurred!");
                }
            }
            c.close();
        } else {
            Log.d("Database Debug", "Cursor is null!");
        }
    }

    //This method will edit a current existing object in the database with the new data that the user provided
    private void editNoteObject() {
        db_connection = new DatabaseConnectionHelper(NoteActivity.this);
        database = db_connection.getWritableDatabase();
        //First Check if the entry name the user provided already exists in his Vault
        Note noteObject = new Note(entryName, currentUsername, noteText);
        //Serialize the object and take the bytes
        //For our convenience i will use the external Apache library -> org.apache.commons
        byte[] objBytes = SerializationUtils.serialize(noteObject);
        //Then we will call the encryptBlob() method of our class DataManipulator to encrypt the data
        //First lets retrieve the encryption key from database for the current user
        String key = DataManipulator.getSymmetricKey(database, currentUsername);
        if (key != null) {
            byte[] encryptedData = DataManipulator.encryptBlob(key, objBytes);
            if (encryptedData != null) {
                ContentValues cv = new ContentValues();
                cv.put(DatabaseSchema.EncryptedData.ENTRY_NAME, noteObject.getName());
                cv.put(DatabaseSchema.EncryptedData.ENCRYPTED_DATA, encryptedData);
                long result = database.update(DatabaseSchema.EncryptedData.TABLE_NAME, cv,
                        DatabaseSchema.EncryptedData.USERNAME + " = ? AND " +
                                DatabaseSchema.EncryptedData.ENTRY_NAME + " = ?",
                        new String[]{currentUsername, editableObj.getName()});
                //Close the connection to the database
                database.close();
                //Send the results back to the Main Activity
                Intent intent = new Intent();
                intent.putExtra("result", result);
                if (result != -1) {
                    setResult(Activity.RESULT_OK, intent);
                } else {
                    setResult(Activity.RESULT_CANCELED, intent);
                }
                //Return to previous Activity
                finish();
            } else {
                showMessage("Encryption Error!", "Unexpected Error Occurred!");
            }
        } else {
            showMessage("Database Error!", "Unexpected Error Occurred!");
        }
    }
}