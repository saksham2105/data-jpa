package com.orm.test;

import com.orm.annotations.Column;
import com.orm.annotations.PrimaryKey;
import com.orm.annotations.Table;

@Table(value = "student")
public class Student {

    @PrimaryKey
    @Column(name = "roll_no")
    private int rollNumber;

    @Column(name = "name")
    private String name;

    @Column(name = "address")
    private String address;

    public void setRollNumber(int rollNumber) {this.rollNumber = rollNumber;}
    public int getRollNumber() {return this.rollNumber;}
    public void setName(String name) {this.name = name;}
    public String getName() {return this.name;}
    public void setAddress(String address) {this.address = address;}
    public String getAddress() {return this.address;}
}
