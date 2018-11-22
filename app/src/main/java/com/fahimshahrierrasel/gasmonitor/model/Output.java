package com.fahimshahrierrasel.gasmonitor.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Output {

    @SerializedName("out")
    @Expose
    private int out;

    public int getOut() {
        return out;
    }

    public void setOut(int out) {
        this.out = out;
    }

}