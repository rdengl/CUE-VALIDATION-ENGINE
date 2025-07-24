package com.example.demo.controller;

import com.sun.jna.Native;
import com.sun.jna.Platform;

import java.io.IOException;
import java.io.InputStream;

import java.io.*;

public class CueValidatorLoader {

    private static final String LIB_NAME = "cue_validator";

    public interface CueValidatorLibrary extends com.sun.jna.Library {
        // Define your native methods here, e.g.:
         String ValidateJSONWithCue(String json, String cueSchema);
    }

    private static CueValidatorLibrary INSTANCE;

    public static CueValidatorLibrary getInstance() {
        if (INSTANCE == null) {
            INSTANCE = loadNativeLibrary();
        }
        return INSTANCE;
    }

    private static CueValidatorLibrary loadNativeLibrary() {
        String fileName;
        if (Platform.isWindows()) {
            fileName = LIB_NAME + ".dll";
        } else if (Platform.isLinux()) {
            fileName = "lib" + LIB_NAME + ".so";
        } else if (Platform.isMac()) {
            fileName = "lib" + LIB_NAME + ".dylib";
        } else {
            throw new UnsupportedOperationException("Unsupported OS: " + System.getProperty("os.name"));
        }

        try (InputStream is = CueValidatorLoader.class.getResourceAsStream("/libs/" + fileName)) {
            if (is == null) {
                throw new IllegalStateException("Native library not found in resources: /native/" + fileName);
            }

            // Copy to temporary file
            File tempFile = File.createTempFile(LIB_NAME, fileName);
            tempFile.deleteOnExit();

            try (OutputStream os = new FileOutputStream(tempFile)) {
                is.transferTo(os);
            }

            return Native.load(tempFile.getAbsolutePath(), CueValidatorLibrary.class);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load native library: " + fileName, e);
        }
    }
}

