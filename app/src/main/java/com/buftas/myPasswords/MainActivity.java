//Nikolaos Katsiopis
//icsd13076

package com.buftas.myPasswords;
//androidx imports

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
//android imports
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.InputType;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_REGISTER_CODE = 1, REQUEST_PASS_RECOVERY_CODE = 2;
    private DatabaseConnectionHelper db_connection;
    private SQLiteDatabase database;
    private EditText usernameLoginField, passwordLoginField;
    private TextView passwordReset, credits;
    private Button registerButton, loginButton;
    private String currentUsernameInUse = "", usernameForPassRecovery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setupUI(findViewById(R.id.parent));
        initializeGraphicElements();
    }

    private void initializeGraphicElements() {
        //Link the buttons to Java code
        //Username Field
        usernameLoginField = findViewById(R.id.usernameLoginField);
        //Password Field
        passwordLoginField = findViewById(R.id.passwordLoginField);
        //Register Button
        registerButton = findViewById(R.id.registerButton);
        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                goToRegisterActivity();
            }
        });
        //Login Button
        loginButton = findViewById(R.id.loginButton);
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isTextFieldEmpty(usernameLoginField) || isTextFieldEmpty(passwordLoginField)) {
                    showMessage("Error!", "Username or Password field can't be empty!");
                } else {
                    //Confirm user's existence and authentication
                    if (confirmUser()) {
                        //Set current logged user
                        currentUsernameInUse = usernameLoginField.getText().toString().trim();
                        usernameLoginField.setText("");
                        passwordLoginField.setText("");
                        goToDataManagementActivity();
                    } else {
                        showMessage("Error!", "Authentication Failed!");
                        usernameLoginField.setText("");
                        passwordLoginField.setText("");
                    }
                }
            }
        });
        //Clickable textViews
        //Forgot Your Password
        passwordReset = findViewById(R.id.forgotYourPasswordText);
        passwordReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Show an alert dialogue with input option for the username he/she wants to recover
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Type your username");
                // Set up the input
                final EditText input = new EditText(MainActivity.this);
                // Specify the type of input expected; this, for example
                input.setInputType(InputType.TYPE_CLASS_TEXT);
                builder.setView(input);
                // Set up the buttons
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (isTextFieldEmpty(input)) {
                            showMessage("Error!", "Please provide a username");
                        } else {
                            usernameForPassRecovery = input.getText().toString().trim();
                            db_connection = new DatabaseConnectionHelper(MainActivity.this);
                            database = db_connection.getReadableDatabase();
                            if (userExists(usernameForPassRecovery, database)) {
                                goToPasswordRecoveryActivity();
                            } else {
                                showMessage("Error!", "Invalid user");
                            }
                        }
                    }
                });
                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
                builder.show();
            }
        });
        //Credits
        credits = findViewById(R.id.creditsText);
        credits.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showMessage("Credits", "Creator: Nick Katsiopis\nSID: icsd13076\nDepartment: ICSD\nInstitution: University of Aegean");
            }
        });
    }

    //This method sets up the UI in a manner that will hide the keyboard i the user hasn't touched on a EditText field
    public void setupUI(View view) {

        // Set up touch listener for non-text box views to hide keyboard. It works recursively
        if (!(view instanceof EditText)) {
            view.setOnTouchListener(new View.OnTouchListener() {
                public boolean onTouch(View v, MotionEvent event) {
                    hideSoftKeyboard(MainActivity.this);
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

    //This method opens RegisterView
    private void goToRegisterActivity() {
        Intent intent = new Intent(this, RegisterActivity.class);
        startActivityForResult(intent, REQUEST_REGISTER_CODE);
    }

    //Overridden method to receive results from the RegisterActivity
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_CANCELED) {
            if (requestCode == REQUEST_REGISTER_CODE) {
                if (resultCode == Activity.RESULT_OK) {
                    showMessage("Success!", "Registration completed!\nWelcome to MyPasswords");
                }
            } else if (requestCode == REQUEST_PASS_RECOVERY_CODE) {
                if (resultCode == Activity.RESULT_OK) {
                    showMessage("Success!", "Password reset successful!");
                }
            }
        }
    }

    //This method opens VaultManagementActivity
    private void goToDataManagementActivity() {
        Intent intent = new Intent(this, VaultManagementActivity.class);
        intent.putExtra("currentUsername", this.currentUsernameInUse);
        startActivityForResult(intent, REQUEST_PASS_RECOVERY_CODE);
    }

    //This method opens PasswordResetActivity
    private void goToPasswordRecoveryActivity() {
        Intent intent = new Intent(this, PasswordResetActivity.class);
        intent.putExtra("usernameForPassRecovery", this.usernameForPassRecovery);
        startActivity(intent);
    }

    //This method provides an information dialog to the user
    private void showMessage(String title, String message) {
        InfoDialog info = new InfoDialog();
        info.setTitle(title);
        info.setMessage(message);
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        ft.add(info, "infoDialog");
        //This is needed to execute the
        ft.commitAllowingStateLoss();
    }

    //This method authenticates the user to the app
    private boolean confirmUser() {
        //Connect to Database
        db_connection = new DatabaseConnectionHelper(MainActivity.this);
        database = db_connection.getReadableDatabase();
        //Save credentials on variables
        String username = usernameLoginField.getText().toString().trim();
        //Check if the user exists
        if (!userExists(username, database)) {
            showMessage("Error!", "Invalid User!");
            return false;
        }
        String password = passwordLoginField.getText().toString().trim();
        //Query the database and retrieve the hashed password and the salt
        Cursor c = database.query(DatabaseSchema.Users.TABLE_NAME,
                new String[]{DatabaseSchema.Users.MASTER_PASSWORD_HASH, DatabaseSchema.Users.MASTER_PASSWORD_SALT}, DatabaseSchema.Users.USERNAME + " = ?",
                new String[]{username}, null, null, null);
        if (c != null) {
            //Save the queried hash and salt
            c.moveToFirst();
            String storedHash = c.getString(c.getColumnIndex(DatabaseSchema.Users.MASTER_PASSWORD_HASH));
            String storedSalt = c.getString(c.getColumnIndex(DatabaseSchema.Users.MASTER_PASSWORD_SALT));
            c.close();
            //Calculate the hash for the provided password
            String hashedInputPassword = DataManipulator.getSaltedHash(password, storedSalt);
            //Compare the hashes
            return storedHash.equals(hashedInputPassword);
        } else {
            showMessage("Error!", "Unexpected Failure!");
            return false;
        }
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

