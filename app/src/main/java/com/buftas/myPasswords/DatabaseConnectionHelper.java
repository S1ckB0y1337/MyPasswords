//Nikolaos Katsiopis
//icsd13076
package com.buftas.myPasswords;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;

/*
  Class that defines the methods for creation of the Database and the tables.
  Also defines the onCreate and onUpgrade methods for the database management and the add and delete methods of data
*/
public class DatabaseConnectionHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "MyPasswordsDB";
    public static final int DATABASE_VERSION = 5;

    //User Table creation
    public static final String CREATE_TABLE_USERS = "create table if not exists " +
            DatabaseSchema.Users.TABLE_NAME +
            " (" + DatabaseSchema.Users.USERNAME_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            DatabaseSchema.Users.USERNAME + " VARCHAR(40) NOT NULL, " +
            DatabaseSchema.Users.MASTER_PASSWORD_HASH + " VARCHAR(64) NOT NULL, " +
            DatabaseSchema.Users.MASTER_PASSWORD_SALT + " VARCHAR(64) NOT NULL, " +
            DatabaseSchema.Users.SECURITY_QUESTION_1 + " VARCHAR(40) NOT NULL, " +
            DatabaseSchema.Users.SECURITY_ANSWER_1 + " VARCHAR(64) NOT NULL, " +
            DatabaseSchema.Users.SECURITY_QUESTION_2 + " VARCHAR(40) NOT NULL, " +
            DatabaseSchema.Users.SECURITY_ANSWER_2 + " VARCHAR(64) NOT NULL " + " );";

    //Encrypted Data Table Creation
    public static final String CREATE_TABLE_ENCRYPTED_DATA = "create table if not exists " +
            DatabaseSchema.EncryptedData.TABLE_NAME +
            " (" + DatabaseSchema.EncryptedData.ENTRY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            DatabaseSchema.EncryptedData.USERNAME + " VARCHAR(40) NOT NULL, " +
            DatabaseSchema.EncryptedData.ENTRY_NAME + " VARCHAR(40) NOT NULL, " +
            DatabaseSchema.EncryptedData.ENCRYPTED_DATA + " BLOB NOT NULL, " +
            DatabaseSchema.EncryptedData.DATA_TYPE + " VARCHAR(12) NOT NULL, " +
            "CONSTRAINT fk_Users FOREIGN KEY(" + DatabaseSchema.EncryptedData.USERNAME + ") " +
            "REFERENCES " + DatabaseSchema.Users.TABLE_NAME + "(" + DatabaseSchema.Users.USERNAME + ") " +
            "ON DELETE CASCADE" + " );";

    //Table Deletion
    public static final String DROP_TABLE_USERS = "drop table if exists " + DatabaseSchema.Users.TABLE_NAME;
    public static final String DROP_TABLE_ENCRYPTED_DATA = "drop table if exists " + DatabaseSchema.EncryptedData.TABLE_NAME;

    public DatabaseConnectionHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        Log.d("Database Operations", "Database Created...");
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_USERS);
        Log.d("Database Operations", "TABLE Users Created...");
        db.execSQL(CREATE_TABLE_ENCRYPTED_DATA);
        Log.d("Database Operations", "TABLE EncryptedData Created...");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(DROP_TABLE_USERS);
        Log.d("Database Operations", "TABLE Users Dropped...");
        db.execSQL(DROP_TABLE_ENCRYPTED_DATA);
        Log.d("Database Operations", "TABLE EncryptedData Dropped...");
        onCreate(db);
        Log.d("Database Operations", "Database Re-initialized...");
    }

    //This method adds a new user on the database
    public long addUser(String username, String password, String salt, String secQuestion1, String secAnswer1,
                        String secQuestion2, String secAnswer2, SQLiteDatabase db) {
        //Create the insert query
        String sql = "INSERT INTO " + DatabaseSchema.Users.TABLE_NAME + " (" + DatabaseSchema.Users.USERNAME + ","
                + DatabaseSchema.Users.MASTER_PASSWORD_HASH + "," + DatabaseSchema.Users.MASTER_PASSWORD_SALT + ","
                + DatabaseSchema.Users.SECURITY_QUESTION_1 + "," + DatabaseSchema.Users.SECURITY_ANSWER_1 + ","
                + DatabaseSchema.Users.SECURITY_QUESTION_2 + "," + DatabaseSchema.Users.SECURITY_ANSWER_2 + ") " +
                "VALUES (?,?,?,?,?,?,?)";
        //Build the statement object and bind each value
        //I use prepared statements for security protection against SQL Injection attacks
        SQLiteStatement statement = db.compileStatement(sql);
        statement.bindString(1, username);
        statement.bindString(2, password);
        statement.bindString(3, salt);
        statement.bindString(4, secQuestion1);
        statement.bindString(5, secAnswer1);
        statement.bindString(6, secQuestion2);
        statement.bindString(7, secAnswer2);
        return statement.executeInsert();
    }

    //This method adds a new encrypted entry on the database
    //Same process as on the addUser() method
    public long addEncryptedEntry(String username, String entryName, byte[] encryptedData, String dataType, SQLiteDatabase db) {
        String sql = "INSERT INTO " + DatabaseSchema.EncryptedData.TABLE_NAME +
                " (" + DatabaseSchema.EncryptedData.USERNAME + "," +
                DatabaseSchema.EncryptedData.ENTRY_NAME + "," +
                DatabaseSchema.EncryptedData.ENCRYPTED_DATA + "," +
                DatabaseSchema.EncryptedData.DATA_TYPE + ") " + "VALUES (?,?,?,?)";
        SQLiteStatement statement = db.compileStatement(sql);
        statement.bindString(1, username);
        statement.bindString(2, entryName);
        statement.bindBlob(3, encryptedData);
        statement.bindString(4, dataType);
        return statement.executeInsert();
    }
}
