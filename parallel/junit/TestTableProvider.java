package ru.fizteh.fivt.students.titov.parallel.junit;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import ru.fizteh.fivt.storage.structured.*;
import ru.fizteh.fivt.students.titov.parallel.storeable.TypesUtils;
import ru.fizteh.fivt.students.titov.parallel.multi_file_hash_map.MFileHashMapFactory;
import ru.fizteh.fivt.students.titov.parallel.shell.FileUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class TestTableProvider {
    String providerDirectory;
    String tableName;
    TableProvider provider;
    TableProviderFactory factory;

    static List<Class<?>> typeList;
    static List<Class<?>> wrongTypeList;

    @BeforeClass
    public static void setUpBeforeClass() {
        typeList = new ArrayList<>();
        typeList.add(Integer.class);
        typeList.add(String.class);

        wrongTypeList = new ArrayList<>();
        wrongTypeList.add(Integer.class);
        wrongTypeList.add(String.class);
        wrongTypeList.add(Double.class);
    }

    @Before
    public void setUp() {
        providerDirectory = Paths.get("").resolve("provider").toString();
        tableName = "testTable";
        factory = new MFileHashMapFactory();
        try {
            provider = factory.create(providerDirectory);
        } catch (IOException e) {
            //suppress
        }
    }

    @After
    public void tearDown() {
        try {
            FileUtils.rmdir(Paths.get(providerDirectory));
        } catch (IOException e) {
            //suppress
        }
        FileUtils.mkdir(Paths.get(providerDirectory));
    }

    @Test
    public void testGetTable() throws Exception {
        assertNull(provider.getTable(tableName));
        assertNotNull(provider.createTable(tableName, typeList));
        assertNotNull(provider.getTable(tableName));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetTableNull() throws Exception {
        provider.getTable(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateTableDirExists() throws Exception {
        FileUtils.mkdir(Paths.get(providerDirectory, tableName));
        provider.createTable(tableName, typeList);
    }

    @Test
    public void testCreateDouble() throws Exception {
        provider.createTable(tableName, typeList);
        assertNull(provider.createTable(tableName, typeList));
    }

    @Test
    public void testCreateTableDirNotExists() throws Exception {
        try {
            FileUtils.rmdir(Paths.get(providerDirectory, tableName));
        } catch (IllegalArgumentException e) {
            //suppress - means directory doesn't exist
        }
        assertNotNull(provider.createTable(tableName, typeList));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateTableNullDirectory() throws Exception {
        provider.createTable(null, typeList);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateTableNullTypeList() throws Exception {
        provider.createTable(tableName, null);
    }

    @Test
    public void testRemoveTableExists() throws Exception {
        provider.createTable(tableName, typeList);
        provider.removeTable(tableName);
        assertNull(provider.getTable(tableName));
        assertTrue(!Files.exists(Paths.get(providerDirectory, tableName)));
    }

    @Test(expected = IllegalStateException.class)
    public void testRemoveTableNotExists() throws Exception {
        provider.removeTable(tableName);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRemoveTableNull() throws Exception {
        provider.removeTable(null);
    }

    @Test
    public void testSerialize() {
        try {
            Table newTable;
            newTable = provider.createTable(tableName, typeList);
            Storeable value = provider.createFor(newTable);
            value.setColumnAt(0, 100);
            value.setColumnAt(1, "example");

            String serValue = "<row><col>100</col><col>example</col></row>";
            assertEquals(provider.serialize(newTable, value), serValue);

            Table wrongTable;
            wrongTable = provider.createTable(tableName + 2, wrongTypeList);
            Storeable wrongValue = provider.createFor(wrongTable);
            wrongValue.setColumnAt(0, 100);
            wrongValue.setColumnAt(1, "example");
            wrongValue.setColumnAt(2, 5.5);

            try {
                provider.serialize(newTable, wrongValue);
                fail();
            } catch (ColumnFormatException e) {
                //all right
            }
        } catch (IOException e) {
            //suppress
        }
    }

    @Test
    public void testDeserialize() {
        try {
            Table newTable;
            newTable = provider.createTable(tableName, typeList);
            String stringForParse = "<row><col>100</col><col>example</col></row>";
            String stringForParse2 = "<row><col>qwerty</col><col>example</col></row>";
            try {
                Storeable deserValue = provider.deserialize(newTable, stringForParse);
                assertTrue(deserValue.getIntAt(0).equals(100));
                assertEquals(deserValue.getStringAt(1), "example");
            } catch (ParseException e) {
                assertNotNull(null);
            }

            try {
                provider.deserialize(newTable, stringForParse + 2);
                fail();
            } catch (ParseException e) {
                //all right
            }

            try {
                provider.deserialize(newTable, stringForParse2);
                fail();
            } catch (ParseException e) {
                //all right
            }
        } catch (IOException e) {
            //suppress
        }
    }

    @Test
    public void testCreateFor() {
        try {
            Table newTable;
            newTable = provider.createTable(tableName, typeList);
            Storeable temp = provider.createFor(newTable);
            assertEquals(TypesUtils.getSizeOfStoreable(temp), newTable.getColumnsCount());
        } catch (IOException e) {
            //suppress
        }
    }

    @Test
    public void testCreateForWithValue() {
        try {
            Table newTable;
            newTable = provider.createTable(tableName, typeList);
            List<Object> value = new ArrayList<>();
            value.add(100);
            value.add("new");
            Storeable temp = provider.createFor(newTable, value);
            assertEquals(TypesUtils.getSizeOfStoreable(temp), newTable.getColumnsCount());

            try {
                value = new ArrayList<>();
                value.add("new");
                value.add(100);
                provider.createFor(newTable, value);
                fail();
            } catch (ColumnFormatException e) {
                //all right
            }

            try {
                value = new ArrayList<>();
                value.add("new2");
                provider.createFor(newTable, value);
                fail();
            } catch (IndexOutOfBoundsException e) {
                //all right
            }

        } catch (IOException e) {
            //suppress
        }
    }
}
