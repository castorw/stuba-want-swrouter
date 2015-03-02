package net.ctrdn.stuba.want.swrouter.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.json.JsonWriter;
import javax.json.stream.JsonGenerator;
import net.ctrdn.stuba.want.swrouter.core.processing.PacketProcessor;
import net.ctrdn.stuba.want.swrouter.exception.ModuleInitializationException;
import net.ctrdn.stuba.want.swrouter.exception.NoSuchModuleException;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RouterController {

    private final Logger logger = LoggerFactory.getLogger(RouterController.class);
    private final Map<Class<? extends RouterModule>, RouterModule> moduleMap = new HashMap<>();
    private final File configurationFile = new File("stuba-want-swrouter.conf.json");
    private JsonObject configurationObject;

    private String hostname = "SoftwareRouter";
    private int packetProcessorThreadCount = 8;
    private PacketProcessor packetProcessor;

    public void start() {
        Thread.currentThread().setName("RouterControllerThread");
        this.logger.info("Shitstorm Operating System Initializing...");
        this.logger.info("SHOS v19.12.1 by Shitstorm Telecommunicatons, compiled by shit_rel_team");
        this.logger.info("This product contains ufopornographic and alien abuse-related content and is not to be released to public. Any misuse or redistribution of content contained within this product is prohibited and will be prosecuted according to the law of the Vajnorska street.");
        this.loadConfiguration();
        this.startPacketProcessor();
        this.loadModules();
        this.writeConfiguration();
        this.startModules();
    }

    private void loadModules() {
        this.logger.info("Loading modules...");
        Reflections loaderReflections = new Reflections("net.ctrdn");
        Integer successCount = 0;
        Integer failureCount = 0;
        for (Class moduleClass : loaderReflections.getSubTypesOf(RouterModule.class)) {
            if (!Modifier.isAbstract(moduleClass.getModifiers())) {
                try {
                    Constructor moduleConstructor = moduleClass.getDeclaredConstructor(RouterController.class);
                    RouterModule moduleInstance = (RouterModule) moduleConstructor.newInstance(this);
                    this.moduleMap.put(moduleClass, moduleInstance);
                    moduleInstance.initialize();
                    moduleInstance.reloadConfiguration(this.getModuleConfiguration(moduleClass));
                    this.logger.debug("Loaded and initialized module {} revision {}", moduleInstance.getName(), moduleInstance.getRevision());
                    successCount++;
                } catch (NoSuchMethodException | IllegalAccessException | IllegalArgumentException | InstantiationException | InvocationTargetException | ModuleInitializationException ex) {
                    this.logger.warn("Failed to load module", ex);
                    failureCount++;
                }
            }
        }
        this.logger.info("Router module load completed ({} loaded, {} failed)", successCount, failureCount);
    }

    private void startModules() {
        for (Map.Entry<Class<? extends RouterModule>, RouterModule> entry : this.moduleMap.entrySet()) {
            entry.getValue().start();
        }
    }

    private void startPacketProcessor() {
        this.packetProcessor = new PacketProcessor(this.packetProcessorThreadCount);
        if (this.configurationObject.getJsonObject("CoreConfiguration").containsKey("PacketProcessor") && !this.configurationObject.getJsonObject("CoreConfiguration").isNull("PacketProcessor")) {
            this.packetProcessor.reloadConfiguration(this.configurationObject.getJsonObject("CoreConfiguration").getJsonObject("PacketProcessor"));
        }
    }

    private JsonObject getModuleConfiguration(Class<? extends RouterModule> moduleClass) {
        JsonObject moduleConfigJob = this.configurationObject.getJsonObject("ModuleConfiguration");
        if (moduleConfigJob != null) {
            if (moduleConfigJob.containsKey(moduleClass.getName())) {
                return moduleConfigJob.isNull(moduleClass.getName()) ? null : moduleConfigJob.getJsonObject(moduleClass.getName());
            }
        }
        return null;
    }

    private void loadConfiguration() {
        try {
            if (!this.configurationFile.exists()) {
                this.configurationFile.createNewFile();
            }
            JsonReader reader = Json.createReader(new FileInputStream(this.configurationFile));
            this.configurationObject = reader.readObject();
            if (this.configurationObject.containsKey("CoreConfiguration")) {
                JsonObject coreConfigObject = this.configurationObject.getJsonObject("CoreConfiguration");
                this.hostname = coreConfigObject.getString("Hostname", "SoftwareRouter");
                this.packetProcessorThreadCount = coreConfigObject.getInt("PacketProcessorThreadCount", 8);
            }
            this.logger.info("Loaded configuration from {}", this.configurationFile.getAbsolutePath());
        } catch (IOException ex) {
            this.logger.error("Failed to load configuration", ex);
        }
    }

    private void writeConfiguration() {
        JsonObjectBuilder configJob = Json.createObjectBuilder();

        JsonObjectBuilder coreConfigJob = Json.createObjectBuilder();
        coreConfigJob.add("Hostname", this.getHostname());
        coreConfigJob.add("PacketProcessorThreadCount", this.getPacketProcessor().getThreadCount());
        coreConfigJob.add("PacketProcessor", this.packetProcessor.dumpConfiguration());
        configJob.add("CoreConfiguration", coreConfigJob);

        JsonObjectBuilder moduleConfigJob = Json.createObjectBuilder();
        for (Map.Entry<Class<? extends RouterModule>, RouterModule> entry : this.moduleMap.entrySet()) {
            JsonObjectBuilder moduleJob = entry.getValue().dumpConfiguration();
            if (moduleJob == null) {
                moduleConfigJob.addNull(entry.getKey().getName());
            } else {
                moduleConfigJob.add(entry.getKey().getName(), moduleJob);
            }
        }
        configJob.add("ModuleConfiguration", moduleConfigJob);

        try {
            Map<String, Object> jwConfig = new HashMap<>();
            jwConfig.put(JsonGenerator.PRETTY_PRINTING, true);
            JsonWriter jw = Json.createWriterFactory(jwConfig).createWriter(new FileOutputStream(this.configurationFile));
            jw.writeObject(configJob.build());
            this.logger.info("Configuration has been written to {}", this.configurationFile.getAbsolutePath());
        } catch (FileNotFoundException ex) {
            this.logger.warn("Failed to write configuration", ex);
        }
    }

    public <M extends RouterModule> M getModule(Class<M> moduleClass) throws NoSuchModuleException {
        if (this.moduleMap.containsKey(moduleClass)) {
            return (M) this.moduleMap.get(moduleClass);
        }
        throw new NoSuchModuleException("Module of class " + moduleClass.getName() + " not loaded");
    }

    public PacketProcessor getPacketProcessor() {
        return packetProcessor;
    }

    public String getHostname() {
        return hostname;
    }
}
