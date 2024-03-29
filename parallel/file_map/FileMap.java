package ru.fizteh.fivt.students.titov.parallel.file_map;

import ru.fizteh.fivt.storage.structured.Storeable;
import ru.fizteh.fivt.storage.structured.Table;
import ru.fizteh.fivt.storage.structured.TableProvider;
import ru.fizteh.fivt.students.titov.parallel.storeable.TypesUtils;
import ru.fizteh.fivt.students.titov.parallel.storeable.Serializator;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class FileMap implements Table {
    private static final int MAX_NUMBER_OF_DIRS = 16;
    private static final int MAX_NUMBER_OF_FILES = 16;
    private static final String SUFFIX_OF_DIRECTORY = ".dir";
    private static final String SUFFIX_OF_FILE = ".dat";
    private static final String FILE_ENCODING = "UTF-8";
    private Map<String, Storeable> stableData;
    private List<Class<?>> typeList;
    private int numberOfColumns;
    private String directoryOfTable;
    private TableProvider parent;
    private Lock lockForCommit;
    private ThreadLocal<Difference> diff;

    private class Difference {
        private HashMap<String, Storeable> addedData;
        private HashMap<String, Storeable> changedData;
        private HashSet<String> removedData;

        Difference() {
            addedData = new HashMap<>();
            changedData = new HashMap<>();
            removedData = new HashSet<>();
        }
    }

    private int getNumberOfDirectory(int hash) {
        int result = hash % MAX_NUMBER_OF_DIRS;
        if (result < 0) {
            result += MAX_NUMBER_OF_DIRS;
        }
        return result;
    }

    private int getNumberOfFile(int hash) {
        int result = hash / MAX_NUMBER_OF_DIRS % MAX_NUMBER_OF_FILES;
        if (result < 0) {
            result += MAX_NUMBER_OF_FILES;
        }
        return result;
    }

    private void clearAll() {
        Difference innerDiff = diff.get();
        innerDiff.removedData.clear();
        innerDiff.addedData.clear();
        innerDiff.changedData.clear();
    }

    /**
     * Create empty Filemap
     *
     * @param newDirectory - directory of this file_map
     * @param newTypeList - list of types (signature of table)
     */
    public FileMap(String newDirectory, List<Class<?>> newTypeList, TableProvider newParent) throws IOException {
        directoryOfTable = newDirectory;
        stableData = new HashMap<>();
        typeList = newTypeList;
        numberOfColumns = typeList.size();
        parent = newParent;
        lockForCommit = new ReentrantLock();
        diff = ThreadLocal.withInitial(()->new Difference());
        if (!init()) {
            throw new IOException("error while initialization");
        }
    }


    public TableProvider getTableProvider() {
        return parent;
    }

    @Override
    public String getName() {
        return Paths.get(directoryOfTable).getFileName().toString();
    }

    @Override
    public Storeable get(String key) throws IllegalArgumentException {
        if (key == null) {
            throw new IllegalArgumentException("null argument");
        }
        Difference innerDiff = diff.get();
        if (innerDiff.removedData.contains(key)) {
            return null;
        }
        if (innerDiff.changedData.containsKey(key)) {
            return innerDiff.changedData.get(key);
        }
        if (innerDiff.addedData.containsKey(key)) {
            return innerDiff.addedData.get(key);
        }
        return stableData.get(key);
    }

    @Override
    public Storeable remove(String key) throws IllegalArgumentException {
        if (key == null) {
            throw new IllegalArgumentException("null argument");
        }
        Difference innerDiff = diff.get();
        if (innerDiff.removedData.contains(key)) {
            return null;
        }
        if (innerDiff.addedData.containsKey(key)) {
            return innerDiff.addedData.remove(key);
        }
        if (innerDiff.changedData.containsKey(key)) {
            innerDiff.removedData.add(key);
            return innerDiff.changedData.remove(key);
        }
        if (stableData.containsKey(key)) {
            innerDiff.removedData.add(key);
        }
        return stableData.get(key);
    }

    @Override
    public Storeable put(String key, Storeable value) throws IllegalArgumentException {
        if (key == null || value == null) {
            throw new IllegalArgumentException("null argument");
        }
        TypesUtils.checkNewStorableValue(typeList, value);
        boolean wasDeleted = false;
        Difference innerDiff = diff.get();
        if (innerDiff.removedData.contains(key)) {
            innerDiff.removedData.remove(key);
            wasDeleted = true;
        }
        if (innerDiff.changedData.containsKey(key)) {
            return innerDiff.changedData.put(key, value);
        }
        if (innerDiff.addedData.containsKey(key)) {
            return innerDiff.addedData.put(key, value);
        }

        if (stableData.containsKey(key)) {
            innerDiff.changedData.put(key, value);
        } else {
            innerDiff.addedData.put(key, value);
        }

        if (wasDeleted) {
            return null;
        } else {
            return stableData.get(key);
        }
    }

    @Override
    public int size() {
        Difference innerDiff = diff.get();
        return stableData.size() + innerDiff.addedData.size() - innerDiff.removedData.size();
    }

    @Override
    public int getColumnsCount() {
        return numberOfColumns;
    }

    @Override
    public Class<?> getColumnType(int columnIndex) throws IndexOutOfBoundsException {
        return typeList.get(columnIndex);
    }

    @Override
    public int rollback() {
        Difference innerDiff = diff.get();
        int result = innerDiff.changedData.size() + innerDiff.removedData.size() + innerDiff.addedData.size();
        clearAll();
        return result;
    }

    @Override
    public int commit() throws IOException {
        try {
            lockForCommit.lock();

            Difference innerDiff = diff.get();
            HashMap<String, Storeable> tmpAddedData = new HashMap<>(innerDiff.addedData);
            HashMap<String, Storeable> tmpBufferAdded = new HashMap<>(innerDiff.addedData);
            HashMap<String, Storeable> tmpChangedData = new HashMap<>(innerDiff.changedData);
            HashMap<String, Storeable> tmpBufferChanged = new HashMap<>(innerDiff.changedData);

            tmpAddedData.keySet().removeAll(stableData.keySet());
            tmpBufferChanged.keySet().removeAll(stableData.keySet());
            tmpAddedData.putAll(tmpBufferChanged);

            tmpChangedData.keySet().retainAll(stableData.keySet());
            tmpBufferAdded.keySet().retainAll(stableData.keySet());
            tmpChangedData.putAll(tmpBufferAdded);

            innerDiff.removedData.retainAll(stableData.keySet());
            int result = tmpChangedData.size()
                    + innerDiff.removedData.size() + tmpAddedData.size();
            stableData.keySet().removeAll(innerDiff.removedData);
            stableData.putAll(tmpChangedData);
            stableData.putAll(tmpAddedData);

            boolean allRight = true;
            if (tmpChangedData.size() + innerDiff.removedData.size() > 0) {
                Set<String> reloadKeys = innerDiff.removedData;
                reloadKeys.addAll(tmpChangedData.keySet());
                for (String oneKey : reloadKeys) {
                    if (!save(oneKey, false)) {
                        allRight = false;
                    }
                }
            }
            for (String oneKey : tmpAddedData.keySet()) {
                if (!save(oneKey, true)) {
                    allRight = false;
                }
            }
            if (allRight) {
                return result;
            } else {
                throw new IOException();
            }
        } finally {
            lockForCommit.unlock();
            clearAll();
        }
    }

    public List<String> list() {
        Difference innerDiff = diff.get();
        ArrayList<String> keyList = new ArrayList<>(stableData.keySet());
        keyList.removeAll(innerDiff.removedData);
        keyList.addAll(innerDiff.addedData.keySet());
        return keyList;
    }

    @Override
    public int getNumberOfUncommittedChanges() {
        Difference innerDiff = diff.get();
        return innerDiff.addedData.size() + innerDiff.changedData.size() + innerDiff.removedData.size();
    }

    public boolean init() throws BadFileException {
        String[] listOfDirectories = new File(directoryOfTable).list();
        if (listOfDirectories == null) {
            return true;
        }
        for (String directory: listOfDirectories) {
            String currentDirectory = directoryOfTable + File.separator
                    + directory;
            if (!Files.isDirectory(Paths.get(currentDirectory))) {
                continue;
            }
            String[] listOfFiles = new File(currentDirectory).list();
            for (String oneFile : listOfFiles) {
                String currentFile = currentDirectory + File.separator
                        + oneFile;
                int numberOfDirectory = directory.charAt(0) - '0';
                if (directory.charAt(1) != '.') {
                    numberOfDirectory = 10 * numberOfDirectory + directory.charAt(1) - '0';
                }
                int numberOfFile = oneFile.charAt(0) - '0';
                if (oneFile.charAt(1) != '.') {
                    numberOfFile = 10 * numberOfFile + oneFile.charAt(1) - '0';
                }
                try (FileInputStream inStream = new FileInputStream(currentFile)) {
                    FileChannel inputChannel;
                    inputChannel = inStream.getChannel();
                    ByteBuffer bufferFromDisk;
                    try {
                        bufferFromDisk =
                                inputChannel.map(MapMode.READ_ONLY, 0, inputChannel.size());
                    } catch (IOException e) {
                        throw new BadFileException(e);
                    }
                    try {
                        while (bufferFromDisk.hasRemaining()) {
                            byte[] key;
                            byte[] value;
                            int keySize;
                            int valueSize;
                            if (bufferFromDisk.remaining() >= 4) {
                                keySize = bufferFromDisk.getInt();
                                key = new byte[keySize];
                            } else {
                                throw new BadFileException();
                            }

                            if (keySize < 0) {
                                throw new BadFileException();
                            }

                            if (bufferFromDisk.remaining() >= keySize) {
                                bufferFromDisk.get(key, 0, key.length);
                            } else {
                                throw new BadFileException();
                            }

                            if (bufferFromDisk.remaining() >= 4) {
                                valueSize = bufferFromDisk.getInt();
                                if (valueSize < 0) {
                                    throw new BadFileException();
                                }
                                value = new byte[valueSize];
                            } else {
                                throw new BadFileException();
                            }
                            if (bufferFromDisk.remaining() >= valueSize) {
                                bufferFromDisk.get(value, 0, value.length);
                            } else {
                                throw new BadFileException();
                            }

                            String keyString = new String(key, FILE_ENCODING);
                            if (getNumberOfDirectory(keyString.hashCode()) != numberOfDirectory
                                    || getNumberOfFile(keyString.hashCode()) != numberOfFile) {
                                throw new BadFileException();
                            }

                            try {
                                stableData.put(new String(key, FILE_ENCODING),
                                        Serializator.deserialize(this, new String(value, "UTF-8")));
                            } catch (UnsupportedEncodingException e) {
                                throw new BadFileException(e);
                            } catch (ParseException e) {
                                throw new BadFileException(e);
                            }
                        }
                    } catch (NullPointerException e) {
                        throw new BadFileException(e);
                    }
                } catch (FileNotFoundException e) {
                    throw new BadFileException(e);
                } catch (BadFileException e) {
                    throw new BadFileException(e);
                } catch (IOException e) {
                    throw new BadFileException(e);
                }
            }
        }
        return true;
    }

    /**
     * Returns True if the record on the disc was successful and otherwise False.
     */
    public boolean save(String key, boolean appendFile) throws BadFileException {
        HashSet<String> keySet = new HashSet<>();
        ByteBuffer bufferForSize = ByteBuffer.allocate(4);

        int numberOfDirectory = getNumberOfDirectory(key.hashCode());
        int numberOfFile = getNumberOfFile(key.hashCode());
        if (appendFile) {
            keySet.clear();
            keySet.add(key);
        } else {
            Set<String> keySetFromDB = stableData.keySet();
            for (String oneKey : keySetFromDB) {
                if (numberOfDirectory == getNumberOfDirectory(oneKey.hashCode())
                        & numberOfFile == getNumberOfFile(oneKey.hashCode())) {
                    keySet.add(oneKey);
                }
            }
        }

        Path directoryForsave;
        Path fileForsave;
        directoryForsave = Paths.get(directoryOfTable, numberOfDirectory + SUFFIX_OF_DIRECTORY);
        if (!Files.exists(directoryForsave)) {
            try {
                Files.createDirectory(directoryForsave);
            } catch (IOException e) {
                System.err.println("error while creating directory for save");
                return false;
            }
        }

        fileForsave = Paths.get(directoryForsave.toString(), numberOfFile + SUFFIX_OF_FILE);
        if (!Files.exists(fileForsave)) {
            try {
                Files.createFile(fileForsave);
            } catch (IOException e) {
                System.err.println("error while creating file for save");
                return false;
            }
        }

        try (FileOutputStream outputStream
                     = new FileOutputStream(fileForsave.toString(), appendFile)) {
            for (String oneKey : keySet) {
                try {
                    byte[] keyByte = oneKey.getBytes("UTF-8");
                    byte[] valueByte = Serializator.serialize(this, stableData.get(oneKey)).getBytes("UTF-8");
                    outputStream.write(bufferForSize.putInt(0, keyByte.length).array());
                    outputStream.write(keyByte);
                    outputStream.write(bufferForSize.putInt(0, valueByte.length).array());
                    outputStream.write(valueByte);
                } catch (UnsupportedEncodingException e) {
                    System.err.println("unsupported encoding");
                    return false;
                } catch (IOException e) {
                    System.err.println("io exception");
                    return false;
                }
            }
        } catch (FileNotFoundException e) {
            System.err.println("file not found");
            return false;
        } catch (IOException e) {
            System.err.println("io exception");
            return false;
        }

        if (!appendFile) {
            deleteEmptyFiles(directoryForsave, fileForsave);
        }
        return true;
    }

    public boolean deleteEmptyFiles(Path directory, Path file) throws BadFileException {
        try {
            if (Files.size(file) == 0) {
                Files.delete(file);
            }
        } catch (IOException e) {
            throw new BadFileException(e);
        }
        String[] listOfFiles = new File(directory.toString()).list();
        if (listOfFiles.length == 0) {
            try {
                Files.delete(directory);
            } catch (IOException e) {
                throw new BadFileException(e);
            }
        }
        return true;
    }
}
