package ru.fizteh.fivt.students.titov.parallel.multi_file_hash_map;

import ru.fizteh.fivt.students.titov.parallel.file_map.FileMap;

public class RollbackCommand extends MultiFileHashMapCommand {
    public RollbackCommand() {
        initialize("rollback", 1);
    }

    @Override
    public boolean run(MFileHashMap myMap, String[] args) {
        FileMap currentTable = myMap.getCurrentTable();
        if (currentTable == null) {
            isTable(currentTable);
        } else {
            System.out.println(currentTable.rollback());
        }
        return true;
    }
}
