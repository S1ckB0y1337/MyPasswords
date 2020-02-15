//Nikolaos Katsiopis
//icsd13076

package com.buftas.myPasswords;
//androidx imports

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
//android imports
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ImageSpan;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
//Static imports
import static java.lang.Math.toIntExact;
//Java imports
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

public class VaultManagementActivity extends AppCompatActivity {


    private ListView dataView;
    private ArrayAdapter<String> adapter;
    private ArrayList<String> finalEntries;
    private ArrayList<Long> clicked;
    private LinkedHashMap<DataObject, String> data;
    private DatabaseConnectionHelper db_connection;
    private SQLiteDatabase database;
    private static final int REQUEST_CODE_NEW_ENTRY = 1, REQUEST_CODE_EDIT_ENTRY = 2;
    private String currentUsername, newEntryType = "";
    private Button addButton;
    private Spinner choicesSpinner;
    private long currentSelectedViewId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vault_management);
        setupUI(findViewById(R.id.parent));
        //Get the data from the caller activity
        Intent intent = getIntent();
        currentUsername = intent.getStringExtra("currentUsername");
        //Initialize UI
        initializeGraphicElements();
        //Load data and update UI
        updateUIEntries();
        //Register the ListView for a custom context menu
        registerForContextMenu(dataView);
    }

    private void initializeGraphicElements() {
        clicked = new ArrayList<>();
        finalEntries = new ArrayList<>();
        dataView = findViewById(R.id.dataList);
        //Bellow i implemented the functionality on the list where, at first the user will see the names of the entries
        //And if it clicks on one it will retrieve the value, if again clicks on it the value will hide again, etc
        dataView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //Check if the specific entry has been clicked once
                if (clicked.contains(id)) {
                    //If yes and it is a password, we hide the value and return the name of the entry instead
                    if (new ArrayList<>(data.keySet()).get(position) instanceof Password) {
                        TextView item = (TextView) adapter.getView(toIntExact(id), view, parent);
                        String text = (new ArrayList<>(data.keySet())).get(toIntExact(id)).getName();
                        Drawable myIcon;
                        //Get the icon on a Drawable object
                        myIcon = ContextCompat.getDrawable(VaultManagementActivity.this, R.drawable.passwordicon);
                        //Set its size
                        myIcon.setBounds(0, 0, item.getLineHeight(), item.getLineHeight());
                        //Create the appropriate objects and set the image + text to the text view
                        SpannableStringBuilder ssb = new SpannableStringBuilder("  " + text);
                        ssb.setSpan(new ImageSpan(myIcon, ImageSpan.ALIGN_BASELINE), 0, 1, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
                        item.setText(ssb, TextView.BufferType.SPANNABLE);
                    }
                    clicked.remove(id);
                } else {//Else if is a password swap the name with the value, if it is a credit card or note show the value using a dialogue
                    switch ((new ArrayList<>(data.values()).get(toIntExact(id)))) {
                        case "Password":
                            ((TextView) view).setText(((Password) (new ArrayList<>(data.keySet())).get(toIntExact(id))).getPass());
                            clicked.add(id);
                            break;
                        case "CreditCard":
                            CreditCard card = (CreditCard) (new ArrayList<>(data.keySet())).get(toIntExact(id));
                            showMessage("Credit Card Information!", "Entry Name: " + card.getName() +
                                    "\nCard Number: " + card.getCardNumber() + "\nExpiration Date: " + card.getExpirationDate() +
                                    "\nCVV/CVV2: " + card.getCvv());
                            break;
                        case "Note":
                            Note note = (Note) (new ArrayList<>(data.keySet())).get(toIntExact(id));
                            showMessage(note.getName(), note.getNote());
                            break;
                        default:
                            break;
                    }
                }
            }
        });
        //Set a listener for the long click so i can get the current view id and save it for later process e.g context menu
        dataView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                //Get and save the current selected TextView's id
                currentSelectedViewId = id;
                //Return false, so the event wont be dropped and the context menu listener can pick it up for processing
                return false;
            }
        });

        //Create the ListView adapter and set the font and color of the listed TextViews, plus a custom icon image
        adapter = new ArrayAdapter<String>(VaultManagementActivity.this, android.R.layout.simple_list_item_1, finalEntries) {
            @Override
            //Override and set a custom edit on every View that gets inputted on the list using the Adapter
            public View getView(int position, View convertView, ViewGroup parent) {
                // Cast each item of the ListView as TextView
                TextView item = (TextView) super.getView(position, convertView, parent);
                String text = item.getText().toString();
                Drawable myIcon;
                //Check for every entry's object type instance and add a different icon for them
                if (new ArrayList<>(data.keySet()).get(position) instanceof Password) {
                    //Get the icon on a Drawable object
                    myIcon = ContextCompat.getDrawable(VaultManagementActivity.this, R.drawable.passwordicon);
                } else if (new ArrayList<>(data.keySet()).get(position) instanceof CreditCard) {
                    myIcon = ContextCompat.getDrawable(VaultManagementActivity.this, R.drawable.creditcardicon);
                } else {
                    myIcon = ContextCompat.getDrawable(VaultManagementActivity.this, R.drawable.noteicon);
                }

                //Set its size
                myIcon.setBounds(0, 0, item.getLineHeight(), item.getLineHeight());
                //Create the appropriate objects and set the image + text to the text view
                SpannableStringBuilder ssb = new SpannableStringBuilder("  " + text);
                ssb.setSpan(new ImageSpan(myIcon, ImageSpan.ALIGN_BASELINE), 0, 1, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
                item.setText(ssb, TextView.BufferType.SPANNABLE);
                // Set the typeface/font for the current item
                item.setTypeface(Typeface.SANS_SERIF);
                // Set the list view item's text color
                item.setTextColor(Color.rgb(0, 0, 0));
                // Set the item text style to bold
                item.setTypeface(item.getTypeface(), Typeface.NORMAL);
                // Change the item text size
                item.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 24);
                // return the view
                return item;
            }
        };
        //Set the adapter to the ListView object
        dataView.setAdapter(adapter);
        //Bind the button and set a listener in it!
        addButton = findViewById(R.id.addNewDataButton);
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addNewEncryptedEntry();
            }
        });
    }

    //This method sets up the UI in a manner that will hide the keyboard if the user hasn't touched on a EditText field
    public void setupUI(View view) {
        // Set up touch listener for non-text box views to hide keyboard. It works recursively
        if (!(view instanceof EditText)) {
            view.setOnTouchListener(new View.OnTouchListener() {
                public boolean onTouch(View v, MotionEvent event) {
                    hideSoftKeyboard(VaultManagementActivity.this);
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

    //Override the Context menu creation method and add my item resource file
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.vault_items_menu, menu);
    }

    //Override the item selected method and add listeners to my items
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            //Copy to clipboard functionality
            case R.id.copyToclipItem:
                //Get the object from the list
                String objectType = (new ArrayList<>(data.entrySet())).get(toIntExact(currentSelectedViewId)).getValue();
                //Check the data type of the entry and process the copy to clipboard in a different manner
                switch (objectType) {
                    case "Password":
                        setClipboard(VaultManagementActivity.this,
                                (((Password) (new ArrayList<>(data.keySet())).get(toIntExact(currentSelectedViewId))).getPass()));
                        break;
                    case "CreditCard":
                        CreditCard card = (CreditCard) (new ArrayList<>(data.keySet())).get(toIntExact(currentSelectedViewId));
                        setClipboard(VaultManagementActivity.this, card.getName() + "\n" + card.getCardNumber() + "\n" +
                                card.getExpirationDate() + "\n" + card.getCvv());
                        break;
                    case "Note":
                        Note note = (Note) (new ArrayList<>(data.keySet())).get(toIntExact(currentSelectedViewId));
                        setClipboard(VaultManagementActivity.this, note.getNote());
                        break;
                    default:
                        break;
                }
                Toast.makeText(getApplicationContext(), "Copied to clipboard.", Toast.LENGTH_SHORT).show();
                break;
            case R.id.editItem:
                //Get the object from the list
                String objectType2 = (new ArrayList<>(data.entrySet())).get(toIntExact(currentSelectedViewId)).getValue();
                Intent intent = null;
                switch (objectType2) {
                    case "Password":
                        intent = new Intent(VaultManagementActivity.this, PasswordActivity.class);
                        Password pass = (Password) (new ArrayList<>(data.entrySet())).get(toIntExact(currentSelectedViewId)).getKey();
                        intent.putExtra("Object", pass);
                        break;
                    case "CreditCard":
                        intent = new Intent(VaultManagementActivity.this, CreditCardActivity.class);
                        CreditCard card = (CreditCard) (new ArrayList<>(data.entrySet())).get(toIntExact(currentSelectedViewId)).getKey();
                        intent.putExtra("Object", card);
                        break;
                    case "Note":
                        intent = new Intent(VaultManagementActivity.this, NoteActivity.class);
                        Note note = (Note) (new ArrayList<>(data.entrySet())).get(toIntExact(currentSelectedViewId)).getKey();
                        intent.putExtra("Object", note);
                        break;
                    default:
                        break;
                }
                if (intent != null) {
                    intent.putExtra("Mode", "EditEntry");
                    intent.putExtra("currentUsername", currentUsername);
                    startActivityForResult(intent, REQUEST_CODE_EDIT_ENTRY);
                }
                break;
            case R.id.deleteItem:
                //Calling the delete function for the current selected item
                if (deleteSelectedVaultItem()) {
                    updateUIEntries();
                    Toast.makeText(getApplicationContext(), "Item Deleted!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getApplicationContext(), "Unexpected Error!", Toast.LENGTH_SHORT).show();
                }
                break;
            default:
                break;
        }
        return true;
    }

    //This method provides an information dialog to the user
    private void showMessage(String title, String message) {
        InfoDialog info = new InfoDialog();
        info.setTitle(title);
        info.setMessage(message);
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        ft.add(info, "infoDialog");
        ft.commitAllowingStateLoss();
    }

    //This method gives the user the option to create and add a new secure entry on his personal Vault
    private void addNewEncryptedEntry() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(VaultManagementActivity.this);
        builder.setTitle("Choose an entry type!");
        Context context = VaultManagementActivity.this;
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        choicesSpinner = new Spinner(VaultManagementActivity.this);
        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.secure_entries, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        choicesSpinner.setAdapter(adapter);
        //Set listener to process events when an item gets selected
        choicesSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                newEntryType = choicesSpinner.getSelectedItem().toString().trim();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                showMessage("Error!", "Select an entry type!");
            }
        });
        //And spinner to view
        layout.addView(choicesSpinner);
        //Add the elements on the Dialogue
        builder.setPositiveButton("Submit", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //Call the new entry activity for the specified entry type
                goToNewEntryActivity(newEntryType);
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.setView(layout);
        builder.show();
    }

    private void goToNewEntryActivity(String entryType) {
        Intent intent = null;
        switch (entryType) {
            case "Password":
                intent = new Intent(VaultManagementActivity.this, PasswordActivity.class);
                break;
            case "Credit Card":
                intent = new Intent(VaultManagementActivity.this, CreditCardActivity.class);
                break;
            case "Note":
                intent = new Intent(VaultManagementActivity.this, NoteActivity.class);
                break;
            default:
                break;
        }
        if (intent != null) {
            //Because this is a new entry adding call we will add the "Mode" as a parameter so the Activity will know what to do
            intent.putExtra("Mode", "NewEntry");
            intent.putExtra("currentUsername", currentUsername);
            startActivityForResult(intent, REQUEST_CODE_NEW_ENTRY);
        } else {
            showMessage("Error!", "Unexpected Error!");
        }
    }

    //Overridden method to receive results from the New/Edit Entry Activities
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_NEW_ENTRY) {
            if (resultCode == Activity.RESULT_OK) {
                Toast.makeText(getApplicationContext(), "Entry added successfully", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQUEST_CODE_EDIT_ENTRY) {
            Toast.makeText(getApplicationContext(), "Entry edited successfully", Toast.LENGTH_SHORT).show();
        }
        //Update the listed entries
        updateUIEntries();
    }

    //This method updates the listed Entries when a new entry gets added or deleted or the Activity opens for the first time
    private void updateUIEntries() {
        db_connection = new DatabaseConnectionHelper(VaultManagementActivity.this);
        database = db_connection.getWritableDatabase();
        data = DataManipulator.unlockVault(database, currentUsername);
        if (data != null) {
            adapter.clear();
            for (HashMap.Entry<DataObject, String> entry : data.entrySet()) {
                switch (entry.getValue()) {
                    case "Password":
                        //Cast the Object to its correct class
                        final Password pass = (Password) entry.getKey();
                        adapter.add(pass.getName());
                        break;
                    case "CreditCard":
                        //Cast the Object to its correct class
                        final CreditCard card = (CreditCard) entry.getKey();
                        adapter.add(card.getName());
                        break;
                    case "Note":
                        //Cast the Object to its correct class
                        final Note note = (Note) entry.getKey();
                        adapter.add(note.getName());
                        break;
                    default:
                        break;
                }
                adapter.notifyDataSetChanged();
            }
        } else {
            adapter.clear();
            adapter.notifyDataSetChanged();
        }
    }

    //This method copies the entry's text value to the clipboard
    private void setClipboard(Context context, String text) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Copied Text", text);
        clipboard.setPrimaryClip(clip);
    }

    //Method that removes the current selected object from the user's vault, after he selects the deletion
    private boolean deleteSelectedVaultItem() {
        //Take the item's object
        DataObject currentObject = (new ArrayList<>(data.keySet())).get(toIntExact(currentSelectedViewId));
        //Connect to database
        db_connection = new DatabaseConnectionHelper(VaultManagementActivity.this);
        database = db_connection.getWritableDatabase();
        //Make a query and delete the row that this object's name exists on
        int result = database.delete(DatabaseSchema.EncryptedData.TABLE_NAME,
                DatabaseSchema.EncryptedData.USERNAME +
                        " = ? AND " + DatabaseSchema.EncryptedData.ENTRY_NAME + " = ?",
                new String[]{currentUsername, currentObject.getName()});
        return result > 0;
    }
}