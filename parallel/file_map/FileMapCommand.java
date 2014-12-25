package ru.fizteh.fivt.students.titov.parallel.file_map;

public abstract class FileMapCommand extends Command<FileMap> {
    public void initialize(String commandName, int n) {
        name = commandName;
        numberOfArguments = n;
    }
}
