//Nikolaos Katsiopis
//icsd13076

package com.buftas.myPasswords;


public class CreditCard extends DataObject {//This class creates an instance of a credit card and its values
    private String cardNumber, expirationDate, cvv;


    public CreditCard(String name, String username, String cardNumber, String expirationDate, String cvv) {
        super(name, username);
        this.cardNumber = cardNumber;
        this.expirationDate = expirationDate;
        this.cvv = cvv;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    public String getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(String expirationDate) {
        this.expirationDate = expirationDate;
    }

    public String getCvv() {
        return cvv;
    }

    public void setCvv(String cvv) {
        this.cvv = cvv;
    }

}
