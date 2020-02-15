//Nikolaos Katsiopis
//icsd13076

package com.buftas.myPasswords;

public class Note extends DataObject {
    private String note;

    public Note(String name, String username, String note) {
        super(name, username);
        this.note = note;
    }


    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
