package com.adaptiveticket.exception;

public class InsufficientInventoryException extends RuntimeException {
    public InsufficientInventoryException(String tierName, int remaining, int requested) {
        super("Not enough tickets in '" + tierName + "': " + remaining + " remaining, " + requested + " requested");
    }
}
