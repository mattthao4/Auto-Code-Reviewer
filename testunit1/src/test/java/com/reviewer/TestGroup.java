package com.reviewer;

import java.util.ArrayList;

public class TestGroup {
    String name;
    ArrayList<String> files;

    public TestGroup(String name) {
        this.name = name;
        this.files = new ArrayList<>();
    }

    public String getName(){
        return this.name;
    }

    
    public String getFileName(String file){
        if(this.files.contains(file))
            return file+this.name;
        else
            return null;
    }

    public void addFile(String file) {
        this.files.add(file);
    }
}
