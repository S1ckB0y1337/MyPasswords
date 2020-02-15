//Nikolaos Katsiopis
//icsd13076

package com.buftas.myPasswords;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;


import org.apache.commons.lang3.SerializationUtils;

import java.util.LinkedHashMap;

public class PasswordResetActivity extends AppCompatActivity {

    private DatabaseConnectionHelper db_connection;
    private SQLiteDatabase database;
    private TextView secQuestion1Message, secQuestion2Message;
    private EditText secAnswer1Field, secAnswer2Field;
    private Button resetPassword;
    private String usernameForPassRecovery;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_password_recovery);
        Intent intent = getIntent();
        usernameForPassRecovery = intent.getStringExtra("usernameForPassRecovery");
        setupUI(findViewById(R.id.parent));
        initializeGraphicElements();
    }

    private void initializeGraphicElements() {
        secQuestion1Message = findViewById(R.id.secQuestion1ChosenText);
        secQuestion2Message = findViewById(R.id.secQuestion2ChosenText);
        secAnswer1Field = findViewById(R.id.secAnswer1ConfirmField);
        secAnswer2Field = findViewById(R.id.secAnswer2ConfirmField);
        resetPassword = findViewById(R.id.resetPasswordButton);
        //Connect to DB
        db_connection = new DatabaseConnectionHelper(PasswordResetActivity.this);
        database = db_connection.getReadableDatabase();
        //Query the two security questions
        Cursor c = database.query(DatabaseSchema.Users.TABLE_NAME,
                new String[]{DatabaseSchema.Users.SECURITY_QUESTION_1, DatabaseSchema.Users.SECURITY_QUESTION_2},
                DatabaseSchema.Users.USERNAME + " = ?",
                new String[]{usernameForPassRecovery}, null, null, null);
        c.moveToFirst();
        String secQuest1 = c.getString(c.getColumnIndex(DatabaseSchema.Users.SECURITY_QUESTION_1));
        String secQuest2 = c.getString(c.getColumnIndex(DatabaseSchema.Users.SECURITY_QUESTION_2));
        c.close();
        //Set the two questions
        secQuestion1Message.setText(secQuest1);
        secQuestion2Message.setText(secQuest2);
        //Set listener on the button
        resetPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setNewPassword();
            }
        });
    }

    //This method sets up the UI in a manner that will hide the keyboard if the user hasn't touched on a EditText field
    public void setupUI(View view) {

        // Set up touch listener for non-text box views to hide keyboard. It works recursively
        if (!(view instanceof EditText)) {
            view.setOnTouchListener(new View.OnTouchListener() {
                public boolean onTouch(View v, MotionEvent event) {
                    hideSoftKeyboard(PasswordResetActivity.this);
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

    //This method provides an information dialog to the user
    private void showMessage(String title, String message) {
        InfoDialog info = new InfoDialog();
        info.setTitle(title);
        info.setMessage(message);
        info.show(getSupportFragmentManager(), title);
    }

    //This function checks if a provided EditText field is empty and returns it
    private boolean isTextFieldEmpty(EditText etText) {
        return etText.getText().toString().trim().length() == 0;
    }


    //Authenticates User using the two Security Questions and gives him the opportunity to reset his password and set a new one
    private void setNewPassword() {
        //Check if answer fields are empty
        if (isTextFieldEmpty(secAnswer1Field) || isTextFieldEmpty(secAnswer2Field)) {
            showMessage("Error!", "Please answer both security questions!");
        } else {
            //Get an instance of writable db
            db_connection = new DatabaseConnectionHelper(PasswordResetActivity.this);
            database = db_connection.getWritableDatabase();
            //Query up the hashed answers
            Cursor c = database.query(DatabaseSchema.Users.TABLE_NAME,
                    new String[]{DatabaseSchema.Users.SECURITY_ANSWER_1, DatabaseSchema.Users.SECURITY_ANSWER_2},
                    DatabaseSchema.Users.USERNAME + " = ?",
                    new String[]{usernameForPassRecovery}, null, null, null);
            //Get the values from the Cursor
            c.moveToFirst();
            String storedSecAnsw1 = c.getString(c.getColumnIndex(DatabaseSchema.Users.SECURITY_ANSWER_1));
            String storedSecAnsw2 = c.getString(c.getColumnIndex(DatabaseSchema.Users.SECURITY_ANSWER_2));
            c.close();
            //Get the input answers and hash them, then compare them with the stored hashes
            String inputSecAnsw1 = DataManipulator.getHash(secAnswer1Field.getText().toString().trim());
            String inputSecAnsw2 = DataManipulator.getHash(secAnswer2Field.getText().toString().trim());
            //Compare the answers
            if (storedSecAnsw1.equals(inputSecAnsw1) && storedSecAnsw2.equals(inputSecAnsw2)) {
                //Show an alert dialogue with input option for the new password
                final AlertDialog.Builder builder = new AlertDialog.Builder(PasswordResetActivity.this);
                builder.setTitle("Type your new password:");
                // Set up the input
                final EditText input = new EditText(PasswordResetActivity.this);
                // Specify the type of input expected; this, for example
                input.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
                input.setTransformationMethod(PasswordTransformationMethod.getInstance());
                builder.setView(input);
                // Set up the buttons
                builder.setPositiveButton("SUBMIT", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (isTextFieldEmpty(input)) {
                            showMessage("Error!", "Please provide a valid password");
                        } else {
                            //Lets hash the new password and get a new salt
                            String hashedPassword = DataManipulator.getSaltedHash(input.getText().toString().trim());
                            String salt = DataManipulator.getCurrentSalt();
                            //Update the User's account data
                            updateUserAccount(hashedPassword, salt);
                        }
                    }
                });
                builder.show();
            } else {
                showMessage("Error!", "Wrong answers!");
                secAnswer1Field.setText("");
                secAnswer2Field.setText("");
            }
        }

    }

    //This method decrypts the user's stored data with his current password and encrypts them with the new one
    //Also resets his current password with the new one
    private void updateUserAccount(String newPassword, String newSalt) {
        //Get an instance of writable db
        db_connection = new DatabaseConnectionHelper(PasswordResetActivity.this);
        database = db_connection.getWritableDatabase();
        //Get the decrypted items of the vault
        LinkedHashMap<DataObject, String> storedItems = DataManipulator.unlockVault(database, usernameForPassRecovery);
        //Lets update the User's hashed password and salt
        //Create and bind the content values
        ContentValues cv = new ContentValues();
        cv.put(DatabaseSchema.Users.MASTER_PASSWORD_HASH, newPassword);
        cv.put(DatabaseSchema.Users.MASTER_PASSWORD_SALT, newSalt);
        //Update the new hash and salt of the user account
        int result = database.update(DatabaseSchema.Users.TABLE_NAME, cv,
                DatabaseSchema.Users.USERNAME + " = ?", new String[]{usernameForPassRecovery});
        //Iterate through the HashMap get each object, serialize, encrypt it with the new hash and update its value on the database
        for (LinkedHashMap.Entry<DataObject, String> entry : storedItems.entrySet()) {
            byte[] objBytes;
            byte[] encryptedBytes;
            ContentValues cv2;
            switch (entry.getValue()) {
                case "Password":
                    //Cast the Object to its correct class
                    final Password pass = (Password) entry.getKey();
                    //Serialize the object and take the bytes
                    //For our convenience i will use the external Apache library -> org.apache.commons
                    objBytes = SerializationUtils.serialize(pass);
                    encryptedBytes = DataManipulator.encryptBlob(newPassword, objBytes);
                    cv2 = new ContentValues();
                    cv2.put(DatabaseSchema.EncryptedData.ENCRYPTED_DATA, encryptedBytes);
                    database.update(DatabaseSchema.EncryptedData.TABLE_NAME, cv2,
                            DatabaseSchema.EncryptedData.USERNAME +
                                    " = ? AND " + DatabaseSchema.EncryptedData.ENTRY_NAME + " = ?",
                            new String[]{usernameForPassRecovery, pass.getName()});
                    break;
                case "CreditCard":
                    //Cast the Object to its correct class
                    final CreditCard card = (CreditCard) entry.getKey();
                    //Serialize the object and take the bytes
                    //For our convenience i will use the external Apache library -> org.apache.commons
                    objBytes = SerializationUtils.serialize(card);
                    encryptedBytes = DataManipulator.encryptBlob(newPassword, objBytes);
                    cv2 = new ContentValues();
                    cv2.put(DatabaseSchema.EncryptedData.ENCRYPTED_DATA, encryptedBytes);
                    database.update(DatabaseSchema.EncryptedData.TABLE_NAME, cv2,
                            DatabaseSchema.EncryptedData.USERNAME +
                                    " = ? AND " + DatabaseSchema.EncryptedData.ENTRY_NAME + " = ?",
                            new String[]{usernameForPassRecovery, card.getName()});
                    break;
                case "Note":
                    //Cast the Object to its correct class
                    final Note note = (Note) entry.getKey();
                    //Serialize the object and take the bytes
                    //For our convenience i will use the external Apache library -> org.apache.commons
                    objBytes = SerializationUtils.serialize(note);
                    encryptedBytes = DataManipulator.encryptBlob(newPassword, objBytes);
                    cv2 = new ContentValues();
                    cv2.put(DatabaseSchema.EncryptedData.ENCRYPTED_DATA, encryptedBytes);
                    database.update(DatabaseSchema.EncryptedData.TABLE_NAME, cv2,
                            DatabaseSchema.EncryptedData.USERNAME +
                                    " = ? AND " + DatabaseSchema.EncryptedData.ENTRY_NAME + " = ?",
                            new String[]{usernameForPassRecovery, note.getName()});
                    break;
                default:
                    break;
            }
        }
        //Send the results back to the Main Activity
        Intent intent = new Intent();
        intent.putExtra("result", result);
        if (result > 0) {
            setResult(Activity.RESULT_OK, intent);
        } else {
            setResult(Activity.RESULT_CANCELED, intent);
        }
        //Return to previous Activity
        finish();
    }
}
