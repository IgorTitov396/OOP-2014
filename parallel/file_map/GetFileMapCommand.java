package ru.fizteh.fivt.students.titov.parallel.file_map;

import ru.fizteh.fivt.storage.structured.Storeable;
import ru.fizteh.fivt.students.titov.parallel.storeable.Serializator;

public class GetFileMapCommand extends FileMapCommand {
    public GetFileMapCommand() {
        initialize("get", 2);
    }
    @Override
    public boolean run(FileMap fileMap, String[] args) {
        Storeable value = fileMap.get(args[1]);
        if (value != null) {
            System.err.println("found\n" + Serializator.serialize(fileMap, fileMap.get(args[1])));
        } else {
            System.err.println("not found");
        }
        return true;
    }
}
