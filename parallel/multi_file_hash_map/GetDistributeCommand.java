package ru.fizteh.fivt.students.titov.parallel.multi_file_hash_map;

import ru.fizteh.fivt.students.titov.parallel.file_map.FileMap;
import ru.fizteh.fivt.students.titov.parallel.file_map.GetFileMapCommand;

public class GetDistributeCommand extends MultiFileHashMapCommand {
    public GetDistributeCommand() {
        initialize("get", 2);
    }

    @Override
    public boolean run(MFileHashMap myMap, String[] args) {
        FileMap currentTable = myMap.getCurrentTable();
        if (myMap.getCurrentTable() == null) {
            isTable(currentTable);
            return true;
        }
        GetFileMapCommand commandGet = new GetFileMapCommand();
        return commandGet.run(currentTable, args);
    }
}
