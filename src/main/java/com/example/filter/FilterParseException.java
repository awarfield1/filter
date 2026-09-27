package com.example.filter;

public class FilterParseException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String input;
    private final int position;

    public FilterParseException(String message, String input, int position) {
        super(message + " (at index " + position + " of \"" + input + "\")");
        this.input = input;
        this.position = position;
    }

    public String getInput() {
        return input;
    }

    public int getPosition() {
        return position;
    }
}
