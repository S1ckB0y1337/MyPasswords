//Nikolaos Katsiopis
//icsd13076

package com.buftas.myPasswords;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;


public class RegisterActivity extends AppCompatActivity {

    private DatabaseConnectionHelper db_connection;
    private SQLiteDatabase database;
    private String username, password, selectedSecQuestion1, selectedSecQuestion2, selectedSecAnswer1, selectedSecAnswer2;
    private Spinner questionSpinner1, questionSpinner2;
    private EditText usernameField, passwordField, secAnswer1, secAnswer2;
    private Button submitButton;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        setupUI(findViewById(R.id.parent));
        //Initialize and make the graphic elements responsive
        initializeGraphicElements();
    }

    private void initializeGraphicElements() {
        //Bind EditText fields to objects
        usernameField = findViewById(R.id.usernameRegisterField);
        passwordField = findViewById(R.id.passwordRegisterField);
        secAnswer1 = findViewById(R.id.secAnswer1);
        secAnswer2 = findViewById(R.id.secAnswer2);
        //Bind and set listeners to Spinners
        //First Spinner
        questionSpinner1 = findViewById(R.id.secQuestionSpinner1);
        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.security_questions_1, R.layout.my_spinner);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        questionSpinner1.setAdapter(adapter);
        //Set listener to process events when an item gets selected
        questionSpinner1.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedSecQuestion1 = questionSpinner1.getSelectedItem().toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                showMessage("Error!", "Select a security question!");
            }
        });
        //Second Spinner
        questionSpinner2 = findViewById(R.id.secQuestionSpinner2);
        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapter2 = ArrayAdapter.createFromResource(this,
                R.array.security_questions_2, R.layout.my_spinner);
        // Specify the layout to use when the list of choices appears
        adapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        questionSpinner2.setAdapter(adapter2);
        //Set listener to process events when an item gets selected
        questionSpinner2.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedSecQuestion2 = questionSpinner2.getSelectedItem().toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                showMessage("Error!", "Select a security question!");
            }
        });
        //Submit Button
        submitButton = findViewById(R.id.submitRegisterButton);
        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                registerNewUser();
            }
        });
    }

    //This method sets up the UI in a manner that will hide the keyboard i the user hasn't touched on a EditText field
    public void setupUI(View view) {

        // Set up touch listener for non-text box views to hide keyboard. It works recursively
        if (!(view instanceof EditText)) {
            view.setOnTouchListener(new View.OnTouchListener() {
                public boolean onTouch(View v, MotionEvent event) {
                    hideSoftKeyboard(RegisterActivity.this);
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

    private void registerNewUser() {
        db_connection = new DatabaseConnectionHelper(RegisterActivity.this);
        database = db_connection.getWritableDatabase();
        //First check if any of the fields are empty
        if (isTextFieldEmpty(usernameField) || isTextFieldEmpty(passwordField) ||
                isTextFieldEmpty(secAnswer1) || isTextFieldEmpty(secAnswer2)) {
            showMessage("Error!", "Please fill all the registration fields!");
        } else if (userExists(usernameField.getText().toString().trim(), database)) {
            showMessage("Error!", "Username Already Exists!");
        } else {
            //Get the submitted values and add them to variables
            username = usernameField.getText().toString().trim();
            password = passwordField.getText().toString();
            selectedSecAnswer1 = secAnswer1.getText().toString();
            selectedSecAnswer2 = secAnswer2.getText().toString();
            //Hash the answers to the security questions
            String hashedSecAnswer1 = DataManipulator.getHash(selectedSecAnswer1);
            String hashedSecAnswer2 = DataManipulator.getHash(selectedSecAnswer2);
            //Hash the master password and save the hash and the salt on two variables
            String hashedPassword = DataManipulator.getSaltedHash(password);
            String salt = DataManipulator.getCurrentSalt();
            Log.d("Hashing Debug", "Hashed Password:" + hashedPassword);
            Log.d("Hashing Debug", "Salt:" + salt);
            //Execute insertion query
            long result = db_connection.addUser(username, hashedPassword, salt, selectedSecQuestion1, hashedSecAnswer1,
                    selectedSecQuestion2, hashedSecAnswer2, database);
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
        }
        //Close the database connection
        database.close();
    }

    //This method checks if a given username already exists on the Database
    private boolean userExists(String username, SQLiteDatabase database) {
        Cursor c = database.query(DatabaseSchema.Users.TABLE_NAME,
                new String[]{DatabaseSchema.Users.USERNAME_ID, DatabaseSchema.Users.USERNAME},
                DatabaseSchema.Users.USERNAME + " = ?",
                new String[]{username}, null, null, null);
        boolean result = c.getCount() > 0;
        c.close();
        return result;
    }
}
