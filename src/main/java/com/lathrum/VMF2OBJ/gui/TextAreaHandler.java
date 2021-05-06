
package com.lathrum.VMF2OBJ.gui;

import java.util.logging.ErrorManager;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import javax.swing.JTextArea;

public class TextAreaHandler extends Handler {

    private JTextArea out;

    public TextAreaHandler(JTextArea out) {
        this.out = out;
    }

    @Override
    public void publish(LogRecord record) {
        String msg;
        try {
            msg = getFormatter().format(record);
        } catch (Exception ex) {
            reportError(null, ex, ErrorManager.FORMAT_FAILURE);
            return;
        }

        try {
            out.append(msg);
            // make sure the last line is always visible
            out.setCaretPosition(out.getDocument().getLength());
        } catch (Exception ex) {
            reportError(null, ex, ErrorManager.WRITE_FAILURE);
        }
    }

    @Override
    public void flush() {
    }

    @Override
    public void close() throws SecurityException {
    }
}