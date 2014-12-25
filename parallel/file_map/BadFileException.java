package ru.fizteh.fivt.students.titov.parallel.file_map;

import java.io.IOException;

public class BadFileException extends IOException {
    public BadFileException(Throwable cause) {
        super(cause);
    }

    public BadFileException() {
        super("Can't read from disk");
    }

    @Override
    public String getMessage() {
        Throwable cause = getCause();
        return cause.getClass().getName() + ": " + cause.getMessage();
    }
}
