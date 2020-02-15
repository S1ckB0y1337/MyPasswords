//Nikolaos Katsiopis
//icsd13076

package com.buftas.myPasswords;

//This class constructs the Database's schema
public final class DatabaseSchema {

    private DatabaseSchema() {
    }

    //User Table Schema
    public static class Users {
        public static final String TABLE_NAME = "Users";
        public static final String USERNAME_ID = "username_id";
        public static final String USERNAME = "username";
        public static final String MASTER_PASSWORD_HASH = "main_hashed_password";
        public static final String MASTER_PASSWORD_SALT = "main_password_salt";
        public static final String SECURITY_QUESTION_1 = "sec_question_1";
        public static final String SECURITY_ANSWER_1 = "sec_answer_1";
        public static final String SECURITY_QUESTION_2 = "sec_question_2";
        public static final String SECURITY_ANSWER_2 = "sec_answer_2";
    }

    //Encrypted Data Table Schema
    public static class EncryptedData {
        public static final String TABLE_NAME = "EncryptedData";
        public static final String ENTRY_ID = "entry_id";
        public static final String USERNAME = "username";
        public static final String ENTRY_NAME = "entry_name";
        public static final String ENCRYPTED_DATA = "encrypted_data";
        public static final String DATA_TYPE = "data_type";
    }
}
