package de.neuefische.finalproject.ohboy.utils;

import java.time.Instant;

public class timestampUtils {
    public Instant generateTimeStampEpochSeconds() {
        return Instant.ofEpochSecond(Instant.now().getEpochSecond());
    }
}
