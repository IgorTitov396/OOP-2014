package ru.fizteh.fivt.students.titov.parallel.multi_file_hash_map;

import java.io.IOException;

public class DropCommand extends MultiFileHashMapCommand {
    public DropCommand() {
        initialize("drop", 2);
    }

    @Override
    public boolean run(MFileHashMap myDataBase, String[] args) {
        try {
            myDataBase.removeTable(args[1]);
            System.out.println("dropped");
        } catch (IOException e) {
            System.err.println("io exception while removing directory");
            return false;
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
        } catch (IllegalStateException e) {
            System.err.println(e.getMessage());
        }
        return true;
    }
}
