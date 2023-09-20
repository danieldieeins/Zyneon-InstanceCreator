package com.zyneonstudios.instance.creator;

import javax.swing.*;
import java.io.File;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public class FileChooser extends JFileChooser {

    public String getZipPath() {
        setFileSelectionMode(FILES_ONLY);
        setFileFilter(new javax.swing.filechooser.FileFilter() {
            public boolean accept(File file) {
                return file.getName().toLowerCase().endsWith(".zip") || file.isDirectory();
            }

            public String getDescription() {
                return "ZIP Dateien (*.zip)";
            }
        });
        int result = showOpenDialog(this);
        requestFocusInWindow();
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = getSelectedFile();
            return URLDecoder.decode(selectedFile.getPath(), StandardCharsets.UTF_8);
        }
        return null;
    }

    public String getJsonPath() {
        setFileSelectionMode(FILES_ONLY);
        setFileFilter(new javax.swing.filechooser.FileFilter() {
            public boolean accept(File file) {
                return file.getName().toLowerCase().endsWith(".json") || file.isDirectory();
            }

            public String getDescription() {
                return "JSON Dateien (*.json)";
            }
        });
        int result = showOpenDialog(this);
        requestFocusInWindow();
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = getSelectedFile();
            return URLDecoder.decode(selectedFile.getPath(), StandardCharsets.UTF_8);
        }
        return null;
    }
}