package ru.fizteh.fivt.students.titov.parallel.file_map;

import ru.fizteh.fivt.storage.structured.Storeable;
import ru.fizteh.fivt.students.titov.parallel.storeable.Serializator;

import java.text.ParseException;
import java.util.NoSuchElementException;

public class PutFileMapCommand extends FileMapCommand {
    public PutFileMapCommand() {
        initialize("put", 2);
    }
    @Override
    public boolean run(FileMap fileMap, String[] args) {
        Storeable oldValue = fileMap.get(args[1]);
        try {
            fileMap.put(args[1], Serializator.deserialize(fileMap, args[2]));
        } catch (ParseException e) {
            System.out.println("wrong type (" + e.getMessage() + ")");
            return false;
        } catch (NoSuchElementException e) {
            System.err.println("error: not xml format value");
            return false;
        }
        if (oldValue != null) {
            System.out.println("overwrite\n" + Serializator.serialize(fileMap, oldValue));
        } else {
            System.out.println("new");
        }
        return true;
    }
}
