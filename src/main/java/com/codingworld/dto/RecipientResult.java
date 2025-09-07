package com.codingworld.dto;

public class RecipientResult {
    public String recipient;
    public boolean success;
    public String error; // null if success

    public RecipientResult() {}

    public RecipientResult(String recipient, boolean success, String error) {
        this.recipient = recipient;
        this.success = success;
        this.error = error;
    }

    public static RecipientResult ok(String recipient)    { return new RecipientResult(recipient, true,  null); }
    public static RecipientResult fail(String r, String e){ return new RecipientResult(r,         false, e);   }
    public static RecipientResult skip(String email, String reason) {
        return new RecipientResult(email, false, reason);
    }
}
