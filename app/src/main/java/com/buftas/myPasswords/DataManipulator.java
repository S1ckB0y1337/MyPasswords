//Nikolaos Katsiopis
//icsd13076

package com.buftas.myPasswords;

//android imports

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Base64;
import android.util.Log;
//Apache imports
import org.apache.commons.lang3.SerializationUtils;
//javax imports
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
//java imports
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.LinkedHashMap;

class DataManipulator {//This class in general messes with encrypted and decrypted data, generates salts and hashes

    private static byte[] currentSalt;

    public DataManipulator() {//Default Constructor

    }

    //This function generates a new salt and hashes the password returning a secure authentication hash
    public static String getSaltedHash(String passwordToHash) {
        String generatedPassword = null;
        try {
            //First generate a new secure salt for the password
            generateNewSalt();
            //Initialize an instance of SHA256 hashing Algorithm
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            //Set the salt
            md.update(currentSalt);
            //get the hash of the password in bytes
            byte[] bytes = md.digest(passwordToHash.getBytes());
            //Create a StringBuilder Instance
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < bytes.length; i++) {
                //Byte to hex 2byte conversion
                sb.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
            }
            generatedPassword = sb.toString();
        } catch (NoSuchAlgorithmException e) {
            Log.d("Hashing Debug!", e.toString());
        }
        return generatedPassword;
    }

    //Overloaded version that has provided salt as parameter for hash recreation purposes
    public static String getSaltedHash(String passwordToHash, String salt) {
        String generatedPassword = null;
        try {
            byte[] providedSalt = Base64.decode(salt, Base64.DEFAULT);
            //Initialize an instance of SHA256 hashing Algorithm
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            //Set the salt
            md.update(providedSalt);
            //get the hash of the password in bytes
            byte[] bytes = md.digest(passwordToHash.getBytes());
            //Create a StringBuilder Instance
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < bytes.length; i++) {
                sb.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
            }
            generatedPassword = sb.toString();
        } catch (NoSuchAlgorithmException e) {
            Log.d("Hashing Debug!", e.toString());
        }
        return generatedPassword;
    }


    //This function generates a new hash for the security question answers
    public static String getHash(String passwordToHash) {
        String generatedHash = null;
        try {
            //Initialize an instance of SHA256 hashing Algorithm
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            //get the hash of the password in bytes
            byte[] bytes = md.digest(passwordToHash.getBytes());
            //Create a StringBuilder Instance
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < bytes.length; i++) {
                sb.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
            }
            generatedHash = sb.toString();
        } catch (NoSuchAlgorithmException e) {
            Log.d("Hashing Debug!", e.toString());
        }
        return generatedHash;
    }

    //Method that generates a "Secure" and "Random" Salt
    public static void generateNewSalt() throws NoSuchAlgorithmException {
        SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");
        byte[] salt = new byte[16];
        sr.nextBytes(salt);
        currentSalt = salt;
    }

    //Returns current stored salt on encoded Base64 string format
    public static String getCurrentSalt() {
        return Base64.encodeToString(currentSalt, Base64.DEFAULT);
    }

    //Method to encrypt a blob of data by providing a symmetric key
    public static byte[] encryptBlob(String key, byte[] blob) {
        // Generating IV.
        int ivSize = 16;
        byte[] iv = new byte[ivSize];
        SecureRandom random = new SecureRandom();
        random.nextBytes(iv);
        IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);

        try {
            // Hashing key.
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(key.getBytes(StandardCharsets.UTF_8));
            byte[] keyBytes = new byte[16];
            System.arraycopy(digest.digest(), 0, keyBytes, 0, keyBytes.length);
            SecretKeySpec secretKeySpec = new SecretKeySpec(keyBytes, "AES");

            // Encrypt.
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivParameterSpec);
            byte[] encrypted = cipher.doFinal(blob);

            // Combine IV and encrypted part.
            byte[] encryptedIVAndText = new byte[ivSize + encrypted.length];
            System.arraycopy(iv, 0, encryptedIVAndText, 0, ivSize);
            System.arraycopy(encrypted, 0, encryptedIVAndText, ivSize, encrypted.length);
            return encryptedIVAndText;

        } catch (NoSuchAlgorithmException | InvalidKeyException | InvalidAlgorithmParameterException | IllegalBlockSizeException
                | NoSuchPaddingException | BadPaddingException e) {
            Log.d("Encryption Debug!", e.toString());
            return null;
        }
    }

    //Method to decrypt a blob by providing a symmetric key
    public static byte[] decryptBlob(String key, byte[] encryptedBlob) {
        int ivSize = 16;
        int keySize = 16;

        // Extract IV.
        byte[] iv = new byte[ivSize];
        System.arraycopy(encryptedBlob, 0, iv, 0, iv.length);
        IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);

        // Extract encrypted part.
        int encryptedSize = encryptedBlob.length - ivSize;
        byte[] encryptedBytes = new byte[encryptedSize];
        System.arraycopy(encryptedBlob, ivSize, encryptedBytes, 0, encryptedSize);

        // Hash key.
        byte[] keyBytes = new byte[keySize];
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(key.getBytes());
            System.arraycopy(md.digest(), 0, keyBytes, 0, keyBytes.length);
            SecretKeySpec secretKeySpec = new SecretKeySpec(keyBytes, "AES");

            // Decrypt.
            Cipher cipherDecrypt = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipherDecrypt.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec);

            return cipherDecrypt.doFinal(encryptedBytes);
        } catch (NoSuchAlgorithmException | InvalidKeyException | InvalidAlgorithmParameterException | IllegalBlockSizeException
                | NoSuchPaddingException | BadPaddingException e) {
            Log.d("Decryption Debug!", e.toString());
            return null;
        }
    }

    //This Method Retrieves every encrypted entry the user has, decrypts them and and puts them on a list, then return the list
    public static LinkedHashMap<DataObject, String> unlockVault(SQLiteDatabase database, String username) {
        //Create a LinkedHashMap for the entries
        LinkedHashMap<DataObject, String> entries = new LinkedHashMap<>();
        //Query the Database for encrypted entries
        Cursor c = database.query(DatabaseSchema.EncryptedData.TABLE_NAME,
                new String[]{DatabaseSchema.EncryptedData.ENCRYPTED_DATA, DatabaseSchema.EncryptedData.DATA_TYPE},
                DatabaseSchema.EncryptedData.USERNAME + " = ?", new String[]{username},
                null, null, null);
        if (c != null) {
            if (c.getCount() > 0) {
                //Get the symmetric key of the current user
                String key = getSymmetricKey(database, username);
                if (key != null) {
                    for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
                        //Get the encrypted blob and decrypt it
                        byte[] decryptedData = decryptBlob(key, c.getBlob(c.getColumnIndex(DatabaseSchema.EncryptedData.ENCRYPTED_DATA)));
                        //Check if the decryption was successful, if not inform the user
                        if (decryptedData != null) {
                            //Get The data type of the current column
                            String dataType = c.getString(c.getColumnIndex(DatabaseSchema.EncryptedData.DATA_TYPE));
                            //Deserialize the decrypted Object
                            switch (dataType) {
                                case "Password":
                                    Password pass = SerializationUtils.deserialize(decryptedData);
                                    entries.put(pass, dataType);
                                    break;
                                case "CreditCard":
                                    CreditCard cc = SerializationUtils.deserialize(decryptedData);
                                    entries.put(cc, dataType);
                                    break;
                                case "Note":
                                    Note note = SerializationUtils.deserialize(decryptedData);
                                    entries.put(note, dataType);
                                    break;
                                default:
                                    break;
                            }
                        } else {
                            return null;
                        }
                    }
                    c.close();
                    return entries;
                } else {
                    return null;
                }
            }
            c.close();
        } else {
            return null;
        }
        return null;
    }

    //This method queries and retrieves the symmetric key for the current user from the database
    public static String getSymmetricKey(SQLiteDatabase database, String username) {
        Cursor c = database.query(DatabaseSchema.Users.TABLE_NAME,
                new String[]{DatabaseSchema.Users.MASTER_PASSWORD_HASH}, DatabaseSchema.Users.USERNAME + " = ?",
                new String[]{username}, null, null, null);
        if (c != null) {
            if (c.getCount() > 0) {
                c.moveToFirst();
                String key = c.getString(c.getColumnIndex(DatabaseSchema.Users.MASTER_PASSWORD_HASH));
                c.close();
                return key;
            }
            c.close();
            return null;
        } else {
            return null;
        }
    }
}
