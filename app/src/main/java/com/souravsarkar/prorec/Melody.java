package com.souravsarkar.prorec;

/**
 * Created by souravsarkar on 27/09/15.
 */
public class Melody {
    public String downloadLink;
    public String title;

    public Melody(){
        this.downloadLink = "";
        this.title = "";
        return;
    }

    public Melody(String downloadLink,String title){
        this.title = title;
        this.downloadLink = downloadLink;
        return;
    }
    @Override
    public String toString() {
        return "Title : " + this.title + "\r\n" + "Link : " + this.downloadLink;
    }
}



