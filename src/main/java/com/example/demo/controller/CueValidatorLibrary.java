package com.example.demo.controller;
import com.sun.jna.Library;
import com.sun.jna.Native;

public interface CueValidatorLibrary extends Library {
    CueValidatorLibrary INSTANCE = Native.load("cue_validator", CueValidatorLibrary.class);
    String ValidateJSONWithCue(String jsonData, String cueSchema);
    
}
