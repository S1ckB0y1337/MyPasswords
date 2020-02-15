//Nikolaos Katsiopis
//icsd13076
package com.buftas.myPasswords;

public class Password extends DataObject {//This class creates a password instance with tis description
    private String pass;

    public Password(String name, String username, String pass) {
        super(name, username);
        this.pass = pass;
    }

    public String getPass() {
        return pass;
    }

    public void setPass(String pass) {
        this.pass = pass;
    }
}
