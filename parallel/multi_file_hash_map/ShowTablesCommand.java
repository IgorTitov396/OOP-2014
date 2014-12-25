package ru.fizteh.fivt.students.titov.parallel.multi_file_hash_map;

import ru.fizteh.fivt.students.titov.parallel.file_map.FileMap;

import java.util.Map.Entry;
import java.util.Set;

public class ShowTablesCommand extends MultiFileHashMapCommand {
    public ShowTablesCommand() {
        initialize("show", 2);
    }

    @Override
    public boolean run(MFileHashMap myMap, String[] args) {
        if (!args[1].equals("tables")) {
            System.err.println(name + ": wrong arguments");
            return false;
        }
        Set<Entry<String, FileMap>> pairSet = myMap.getTables().entrySet();
        for (Entry<String, FileMap> oneTable: pairSet) {
            System.out.println(oneTable.getKey() + " " + oneTable.getValue().size());
        }
        return true;
    }
}
