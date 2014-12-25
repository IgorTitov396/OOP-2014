package ru.fizteh.fivt.students.titov.parallel.file_map;

import ru.fizteh.fivt.storage.structured.Storeable;

public class RemoveFileMapCommand extends FileMapCommand {
    public RemoveFileMapCommand() {
        initialize("remove", 2);
    }
    @Override
    public boolean run(FileMap fileMap, String[] args) {
        Storeable value = fileMap.remove(args[1]);
        if (value != null) {
            System.out.println("removed");
        } else {
            System.out.println("not found");
        }
        return true;
    }
}
