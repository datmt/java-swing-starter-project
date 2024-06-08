package com.datmt.tools;

import com.formdev.flatlaf.FlatDarculaLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.sun.tools.javac.Main;

public class AppStarter {
    public static void main(String[] args) {
        FlatLightLaf.setup();
        new BootstrapGUI();
    }
}
