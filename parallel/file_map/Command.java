package ru.fizteh.fivt.students.titov.parallel.file_map;

public abstract class Command<T> {
    protected String name;
    protected int numberOfArguments;

    public abstract boolean run(T object, String[] args);

    @Override
    public final String toString() {
        return name;
    }
}
