package ru.fizteh.fivt.students.titov.parallel.file_map;

import java.util.HashMap;
import java.util.Scanner;

public class Shell<T> {
    private static final String INVITATION = "$ ";
    private HashMap<String, Command<T>> shellCommands;

    private T objectForShell;
    public Shell(T obj) {
        shellCommands = new HashMap<>();
        objectForShell = obj;
    }

    public void addCommand(Command<T> newCommand) {
        shellCommands.put(newCommand.toString(), newCommand);
    }

    public boolean interactiveMode() {
        System.out.print(INVITATION);
        boolean ended = false;
        boolean errorOccuried = false;

        try (Scanner inStream = new Scanner(System.in)) {
            String[] parsedCommands;
            String[] parsedArguments;
            while (!ended) {
                if (inStream.hasNextLine()) {
                    parsedCommands = inStream.nextLine().split(";|\n");
                } else {
                    break;
                }
                for (String oneCommand : parsedCommands) {
                    parsedArguments = oneCommand.trim().split("\\s+");
                    if (parsedArguments[0].equals("put")) {
                        if (oneCommand.contains("<")) {
                            String valueForPut = oneCommand.trim().substring(oneCommand.indexOf('<'));
                            parsedArguments[2] = valueForPut;
                        }
                    }
                    if (parsedArguments.length == 0 || parsedArguments[0].equals("")) {
                        continue;
                    }
                    if (parsedArguments[0].equals("exit")) {
                        ended = true;
                        break;
                    }
                    Command<T> commandToExecute = shellCommands.get(parsedArguments[0]);
                    if (commandToExecute != null) {
                        if (commandToExecute.numberOfArguments != parsedArguments.length
                                & commandToExecute.numberOfArguments != -1) {
                            System.err.println(commandToExecute.name + ": wrong number of arguments");
                            errorOccuried = true;
                        } else if (!commandToExecute.run(objectForShell, parsedArguments)) {
                            errorOccuried = true;
                        }
                    } else {
                        System.err.println(parsedArguments[0] + ": command not found");
                        errorOccuried = true;
                    }
                }
                if (!ended) {
                    System.out.print(INVITATION);
                }
            }
        }
        return !errorOccuried;
    }

    public boolean batchMode(final String[] arguments) {

        String[] parsedCommands;
        String[] parsedArguments;
        String commandLine = arguments[0];
        boolean errorOccuried = false;

        for (int i = 1; i < arguments.length; ++i) {
            commandLine = commandLine + " " + arguments[i];
        }

        parsedCommands = commandLine.split(";|\n");
        if (parsedCommands.length == 0) {
            return true;
        }
        for (String oneCommand : parsedCommands) {
            parsedArguments = oneCommand.trim().split("\\s+");
            if (parsedArguments.length == 0 || parsedArguments[0].equals("")) {
                continue;
            }
            if (parsedArguments[0].equals("exit")) {
                return !errorOccuried;
            }
            Command<T> commandToExecute = shellCommands.get(parsedArguments[0]);
            if (commandToExecute != null) {
                if (commandToExecute.numberOfArguments != parsedArguments.length
                        & commandToExecute.numberOfArguments != -1) {
                    System.err.println(commandToExecute.name + " wrong number of arguments");
                    errorOccuried = true;
                } else if (!commandToExecute.run(objectForShell, parsedArguments)) {
                    errorOccuried = true;
                }
            } else {
                System.err.println(parsedArguments[0] + ": command not found");
                errorOccuried = true;
            }
        }
        return !errorOccuried;
    }
}

