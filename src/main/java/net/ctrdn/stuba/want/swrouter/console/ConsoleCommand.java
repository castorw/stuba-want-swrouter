package net.ctrdn.stuba.want.swrouter.console;

import java.util.List;

public interface ConsoleCommand {

    public String getCommand();

    public String getDescription();

    public List<String> parameters();

    public boolean isExecutable();

    public void execute();

    public List<ConsoleCommand> subcommands();
}