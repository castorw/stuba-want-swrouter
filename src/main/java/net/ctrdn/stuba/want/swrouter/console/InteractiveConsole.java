package net.ctrdn.stuba.want.swrouter.console;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import net.ctrdn.stuba.want.swrouter.core.RouterController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InteractiveConsole implements Runnable, ConsoleCommandRepository {

    private final Logger logger = LoggerFactory.getLogger(InteractiveConsole.class);
    private final RouterController routerController;
    private final InputStream inputStream;
    private final OutputStream outputStream;
    private boolean consoleRunning = false;
    private Thread consoleThread = null;
    private String ttyConfig;
    private String currentCommand = "";
    private final List<String> commandHistoryList = new ArrayList<>();
    private int commandHistoryIndex = 0;
    private final List<ConsoleCommand> commandList;
    private final List<ConsoleCommandProvider> commandProviderList;

    public InteractiveConsole(RouterController routerController, InputStream inputStream, OutputStream outputStream) {
        this.routerController = routerController;
        this.inputStream = inputStream;
        this.outputStream = outputStream;
        this.commandList = new ArrayList<>();
        this.commandProviderList = new ArrayList<>();
    }

    public void start() {
        this.clear();
        if (this.consoleThread != null) {
            this.consoleRunning = false;
            this.consoleThread.interrupt();
        }
        this.consoleRunning = true;
        this.consoleThread = new Thread(this);
        this.consoleThread.setName("InteractiveConsoleThread");
        this.consoleThread.start();
    }

    @Override
    public void run() {
        try {
            this.setTerminalToCBreak();
            this.printPrompt();
            while (this.consoleRunning) {
                if (this.inputStream.available() > 0) {
                    int inputByte = this.inputStream.read();
                    this.outputStream.write(inputByte);
                    this.outputStream.flush();
                    switch (inputByte) {
                        case 127: {
                            if (this.currentCommand.length() > 0) {
                                this.currentCommand = this.currentCommand.substring(0, this.currentCommand.length() - 1);
                                this.outputStream.write(8);
                                this.outputStream.write(8);
                                this.outputStream.write(32);
                                this.outputStream.write(8);
                            } else {
                                this.outputStream.write(8);
                            }
                            this.outputStream.flush();
                            break;
                        }
                        case '\n': {
                            this.currentCommand = this.currentCommand.trim();
                            if (!this.currentCommand.isEmpty()) {
                                if (this.commandHistoryList.isEmpty() || !this.commandHistoryList.get(this.commandHistoryList.size() - 1).equals(this.currentCommand)) {
                                    this.commandHistoryList.add(this.currentCommand);
                                }
                                this.commandHistoryIndex = this.commandHistoryList.size();
                                this.currentCommand = "";

                                // command execution here
                            }
                            this.printPrompt();
                            break;
                        }
                        case '?': {
                            System.out.println("\n" + this.currentCommand + " <------------------");
                            break;
                        }
                        case 27: {
                            if (this.inputStream.available() > 0) {
                                int nextChar = this.inputStream.read();
                                if (nextChar == 91) {
                                    if (this.inputStream.available() > 0) {
                                        int nextChar2 = this.inputStream.read();
                                        if (this.commandHistoryList.size() > 0) {
                                            switch (nextChar2) {
                                                case 'A': { // arrow up
                                                    this.commandHistoryIndex--;
                                                    if (this.commandHistoryIndex < 0) {
                                                        this.commandHistoryIndex = this.commandHistoryList.size() - 1;
                                                    }
                                                    this.currentCommand = this.commandHistoryList.get(this.commandHistoryIndex);
                                                    this.print("\r\r\n", false);
                                                    this.printPrompt();
                                                    break;
                                                }
                                                case 'B': { // arrow down
                                                    this.commandHistoryIndex++;
                                                    if (this.commandHistoryIndex >= this.commandHistoryList.size()) {
                                                        this.commandHistoryIndex = 0;
                                                    }
                                                    this.currentCommand = this.commandHistoryList.get(this.commandHistoryIndex);
                                                    this.print("\r\r\n", false);
                                                    this.printPrompt();
                                                    break;
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            break;
                        }
                        default: {
                            this.currentCommand += (char) inputByte;
                        }
                    }
                } else {
                    Thread.sleep(2);
                }
            }
        } catch (IOException | InterruptedException ex) {
            this.logger.error("Interactive console has failed", ex);
        } finally {
            try {
                stty(this.ttyConfig.trim());
            } catch (Exception e) {
                System.err.println("Exception restoring tty config");
            }
        }
    }

    private void printPrompt() throws IOException {
        this.print("Smerovatko#" + this.currentCommand, false);
    }

    private void print(String message, boolean breakLine) throws IOException {
        byte[] outputMessage = (message + ((breakLine) ? "\n" : "")).getBytes("UTF-8");
        this.outputStream.write(outputMessage);
        this.outputStream.flush();
    }

    private void setTerminalToCBreak() throws IOException, InterruptedException {
        this.ttyConfig = stty("-g");
        stty("-icanon min 1");
        stty("-echo");
    }

    private String stty(final String args) throws IOException, InterruptedException {
        String cmd = "stty " + args + " < /dev/tty";
        return exec(new String[]{"sh", "-c", cmd});
    }

    private String exec(final String[] cmd) throws IOException, InterruptedException {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();

        Process p = Runtime.getRuntime().exec(cmd);
        int c;
        InputStream in = p.getInputStream();

        while ((c = in.read()) != -1) {
            bout.write(c);
        }

        in = p.getErrorStream();

        while ((c = in.read()) != -1) {
            bout.write(c);
        }

        p.waitFor();

        String result = new String(bout.toByteArray());
        return result;
    }

    public void addConsoleCommandProvider(ConsoleCommandProvider provider) {
        this.commandProviderList.add(provider);
    }

    @Override
    public ConsoleCommand command(final String name, final String description) {
        ConsoleCommand cmd = null;
        for (ConsoleCommand command : this.commandList) {
            if (command.getCommand().equals(name)) {
                cmd = command;
                break;
            }
        }
        if (cmd == null) {
            cmd = new ConsoleCommand() {
                private final List<String> parameters = new ArrayList<>();
                private final List<ConsoleCommand> subcommands = new ArrayList<>();

                @Override
                public String getCommand() {
                    return name;
                }

                @Override
                public String getDescription() {
                    return description;
                }

                @Override
                public List<String> parameters() {
                    return parameters;
                }

                @Override
                public boolean isExecutable() {
                    return false;
                }

                @Override
                public void execute() {
                }

                @Override
                public List<ConsoleCommand> subcommands() {
                    return this.subcommands;
                }
            };
            this.commandList.add(cmd);
        }
        return cmd;
    }

    @Override
    public void clear() {
        this.commandList.clear();
        for (ConsoleCommandProvider provider : this.commandProviderList) {
            provider.registerConsoleCommands(this);
        }
    }
}
