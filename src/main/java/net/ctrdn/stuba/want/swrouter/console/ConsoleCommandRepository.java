package net.ctrdn.stuba.want.swrouter.console;

public interface ConsoleCommandRepository {

    public ConsoleCommand command(String name, String description);

    public void clear();
}
