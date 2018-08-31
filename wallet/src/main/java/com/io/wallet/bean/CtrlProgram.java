package com.io.wallet.bean;

public class CtrlProgram {
    private String accountId;
    private String address;
    private int keyIndex;
    private byte[] controlProgram;
    private boolean change;

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public int getKeyIndex() {
        return keyIndex;
    }

    public void setKeyIndex(int keyIndex) {
        this.keyIndex = keyIndex;
    }

    public byte[] getControlProgram() {
        return controlProgram;
    }

    public void setControlProgram(byte[] controlProgram) {
        this.controlProgram = controlProgram;
    }

    public boolean isChange() {
        return change;
    }

    public void setChange(boolean change) {
        this.change = change;
    }
}
