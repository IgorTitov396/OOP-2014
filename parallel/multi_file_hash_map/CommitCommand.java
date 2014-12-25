package ru.fizteh.fivt.students.titov.parallel.multi_file_hash_map;

import ru.fizteh.fivt.students.titov.parallel.file_map.FileMap;

import java.io.IOException;

public class CommitCommand extends MultiFileHashMapCommand {
    public CommitCommand() {
        initialize("commit", 1);
    }

    @Override
    public boolean run(MFileHashMap myMap, String[] args) {
        FileMap currentTable = myMap.getCurrentTable();
        if (currentTable == null) {
            isTable(currentTable);
        } else {
            try {
                System.out.println(currentTable.commit());
            } catch (IOException e) {
                System.err.println("io exception while writing in file");
                return false;
            }
        }
        return true;
    }
}
