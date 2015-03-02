package net.ctrdn.stuba.want.swrouter.core;

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import net.ctrdn.stuba.want.swrouter.exception.ModuleInitializationException;

public interface RouterModule {
    
    public void reloadConfiguration(JsonObject moduleConfiguration);
    
    public JsonObjectBuilder dumpConfiguration();
    
    public void initialize() throws ModuleInitializationException;
    
    public void start();
    
    public String getName();
    
    public Integer getRevision();
}
