package com.buftas.myPasswords;

import java.io.Serializable;

public class DataObject implements Serializable {//Template and parent class of all entry objects
    private static final long serialVersionUID = 1L;
    private String name, username;

    public DataObject(String name, String username) {
        this.name = name;
        this.username = username;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUsername() {
        return username;
    }

}
