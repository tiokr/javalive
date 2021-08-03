package com.github.tiokr.javalive;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class TextCollector extends OutputStream {
    private final List<String> lines = new ArrayList<>();
    private StringBuilder buffer = new StringBuilder();

    @Override
    public void write(int b) {
        if (b == '\n') {
            lines.add(buffer.toString());
            buffer = new StringBuilder();
        } else {
            buffer.append((char) b);
        }
    }

    public List<String> getLines() {
        return lines;
    }
}
