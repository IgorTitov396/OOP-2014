package ru.fizteh.fivt.students.titov.parallel.shell;

import java.nio.file.Paths;
import java.nio.file.Path;
import java.io.File;

public class PathsFunction {

    public static Path toAbsolutePathString(final String myPath) {
        String absolutePathString;
        String fileSeparator = File.separator;
        if (Paths.get(myPath).isAbsolute()) {
            absolutePathString = myPath;
        } else {
            absolutePathString = System.getProperty("user.dir")
                                 + fileSeparator + myPath;
        }
        absolutePathString = Paths.get(absolutePathString).normalize().toString();
        return Paths.get(absolutePathString);
    }
}
