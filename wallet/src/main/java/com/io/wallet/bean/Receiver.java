package com.io.wallet.bean;

/**
 * Created by hwj on 2018/9/15.
 */

public class Receiver {
    public String control_program;
    public String address;

    public Receiver(String control_program, String address) {
        this.control_program = control_program;
        this.address = address;
    }
}
