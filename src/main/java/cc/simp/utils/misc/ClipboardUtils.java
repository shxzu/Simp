package cc.simp.utils.misc;

import cc.simp.utils.Util;

import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

public class ClipboardUtils extends Util {
    public static String getClipboardContents() {
        try {
            return (String) Toolkit.getDefaultToolkit()
                    .getSystemClipboard().getData(DataFlavor.stringFlavor);
        } catch (UnsupportedFlavorException | IOException ignored) {
            return null;
        }
    }

    public static void setClipboardContents(String contents) {
        StringSelection selection = new StringSelection(contents);
        Toolkit.getDefaultToolkit()
                .getSystemClipboard()
                .setContents(selection, selection);
    }
}
