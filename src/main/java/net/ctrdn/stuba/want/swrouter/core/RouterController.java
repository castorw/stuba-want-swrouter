package net.ctrdn.stuba.want.swrouter.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.json.JsonWriter;
import javax.json.stream.JsonGenerator;
import net.ctrdn.stuba.want.swrouter.api.APIMethodRegistry;
import net.ctrdn.stuba.want.swrouter.api.APIServlet;
import net.ctrdn.stuba.want.swrouter.core.processing.PacketProcessor;
import net.ctrdn.stuba.want.swrouter.exception.ModuleInitializationException;
import net.ctrdn.stuba.want.swrouter.exception.NoSuchModuleException;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RouterController {

    private final Logger logger = LoggerFactory.getLogger(RouterController.class);
    private final Map<Class<? extends RouterModule>, RouterModule> moduleMap = new HashMap<>();
    private final File configurationFile = new File("stuba-want-swrouter.conf.json");
    private Date bootDate;
    private Date bootFinishDate;
    private JsonObject configurationObject;

    private String hostname = "SoftwareRouter";
    private PacketProcessor packetProcessor;

    public void start() {
        try {
            Thread.currentThread().setName("RouterControllerThread");
            this.bootDate = new Date();
            this.logger.info("Shitstorm Operating System Initializing...");
            this.logger.info("SHOS v19.12.1 by Shitstorm Telecommunicatons, compiled by shit_rel_team");
            this.logger.info("This product contains ufopornographic and alien abuse-related content and is not to be released to public. Any misuse or redistribution of content contained within this product is prohibited and will be prosecuted according to the law of the Vajnorska street.");
            this.loadConfiguration();
            this.startPacketProcessor();
            this.loadModules();
            this.startModules();
            this.startAPIServer();
            this.bootFinishDate = new Date();
        } catch (ModuleInitializationException ex) {
            throw new RuntimeException(ex);
        }
    }

    private void loadModules() {
        this.logger.info("Loading modules...");
        List<RouterModule> foundModuleList = new ArrayList<>();
        Reflections loaderReflections = new Reflections("net.ctrdn");
        for (Class moduleClass : loaderReflections.getSubTypesOf(RouterModule.class)) {
            if (!Modifier.isAbstract(moduleClass.getModifiers())) {
                try {
                    Constructor moduleConstructor = moduleClass.getDeclaredConstructor(RouterController.class);
                    RouterModule moduleInstance = (RouterModule) moduleConstructor.newInstance(this);
                    foundModuleList.add(moduleInstance);
                } catch (NoSuchMethodException | IllegalAccessException | IllegalArgumentException | InstantiationException | InvocationTargetException ex) {
                    this.logger.warn("Failed to load module", ex);
                }
            }
        }

        Integer successCount = 0;
        Integer failureCount = 0;

        Collections.sort(foundModuleList, new Comparator<RouterModule>() {

            @Override
            public int compare(RouterModule o1, RouterModule o2) {
                return o1.getLoadPriority() < o2.getLoadPriority() ? -1 : o1.getLoadPriority() == o2.getLoadPriority() ? 0 : 1;
            }
        });

        for (RouterModule module : foundModuleList) {
            try {
                this.moduleMap.put(module.getClass(), module);
                module.initialize();
                module.reloadConfiguration(this.getModuleConfiguration(module.getClass()));
                this.logger.debug("Loaded and initialized module {} revision {}", module.getName(), module.getRevision());
                successCount++;
            } catch (ModuleInitializationException ex) {
                failureCount++;
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
        this.packetProcessor = new PacketProcessor();
        if (this.configurationObject != null && this.configurationObject.containsKey("CoreConfiguration") && this.configurationObject.getJsonObject("CoreConfiguration").containsKey("PacketProcessor") && !this.configurationObject.getJsonObject("CoreConfiguration").isNull("PacketProcessor")) {
            this.packetProcessor.reloadConfiguration(this.configurationObject.getJsonObject("CoreConfiguration").getJsonObject("PacketProcessor"));
        }
    }

    private void startAPIServer() throws ModuleInitializationException {
        try {
            APIMethodRegistry.initalize(this);
            ServletContextHandler sch = new ServletContextHandler(ServletContextHandler.SESSIONS);
            sch.setContextPath("/");
            sch.addServlet(new ServletHolder(new APIServlet(this)), "/api/*");
            Server server = new Server(8844);
            server.setHandler(sch);
            server.start();
            this.logger.info("Started API server on port {}", 8844);
        } catch (Exception ex) {
            ModuleInitializationException finalEx = new ModuleInitializationException("Failed to start API server");
            finalEx.addSuppressed(ex);
            throw finalEx;
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
            FileInputStream configurationFileInputStream = new FileInputStream(this.configurationFile);
            if (configurationFileInputStream.available() == 0) {
                configurationFileInputStream.close();
                try (FileOutputStream cpfos = new FileOutputStream(this.configurationFile)) {
                    cpfos.write("{}".getBytes());
                }
                configurationFileInputStream = new FileInputStream(this.configurationFile);
            }
            JsonReader reader = Json.createReader(configurationFileInputStream);
            this.configurationObject = reader.readObject();
            if (this.configurationObject.containsKey("CoreConfiguration")) {
                JsonObject coreConfigObject = this.configurationObject.getJsonObject("CoreConfiguration");
                this.hostname = coreConfigObject.getString("Hostname", "SoftwareRouter");
            }
            this.logger.info("Loaded configuration from {}", this.configurationFile.getAbsolutePath());
        } catch (IOException ex) {
            this.logger.error("Failed to load configuration", ex);
        }
    }

    public JsonObject getRunningConfiguration() {
        JsonObjectBuilder configJob = Json.createObjectBuilder();

        JsonObjectBuilder coreConfigJob = Json.createObjectBuilder();
        coreConfigJob.add("Hostname", this.getHostname());
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
        return configJob.build();
    }

    public JsonObject getStartupConfiguration() {
        return this.configurationObject;
    }

    public void writeConfiguration() {
        this.configurationObject = this.getRunningConfiguration();
        try {
            Map<String, Object> jwConfig = new HashMap<>();
            jwConfig.put(JsonGenerator.PRETTY_PRINTING, true);
            JsonWriter jw = Json.createWriterFactory(jwConfig).createWriter(new FileOutputStream(this.configurationFile));
            jw.writeObject(this.configurationObject);
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

    public Class<? extends RouterModule>[] getModuleClasses() {
        return this.moduleMap.keySet().toArray(new Class[this.moduleMap.keySet().size()]);
    }

    public PacketProcessor getPacketProcessor() {
        return packetProcessor;
    }

    public String getHostname() {
        return hostname;
    }

    public Date getBootDate() {
        return bootDate;
    }

    public Date getBootFinishDate() {
        return bootFinishDate;
    }
}
