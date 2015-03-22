package net.ctrdn.stuba.want.swrouter.module.nat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonString;
import net.ctrdn.stuba.want.swrouter.common.IPv4Protocol;
import net.ctrdn.stuba.want.swrouter.common.net.IPv4Address;
import net.ctrdn.stuba.want.swrouter.common.net.IPv4Prefix;
import net.ctrdn.stuba.want.swrouter.core.DefaultRouterModule;
import net.ctrdn.stuba.want.swrouter.core.RouterController;
import net.ctrdn.stuba.want.swrouter.exception.IPv4MathException;
import net.ctrdn.stuba.want.swrouter.exception.ModuleInitializationException;
import net.ctrdn.stuba.want.swrouter.exception.NATException;
import net.ctrdn.stuba.want.swrouter.exception.NATTranslationException;
import net.ctrdn.stuba.want.swrouter.exception.NoSuchModuleException;
import net.ctrdn.stuba.want.swrouter.module.interfacemanager.InterfaceManagerModule;
import net.ctrdn.stuba.want.swrouter.module.interfacemanager.NetworkInterface;
import net.ctrdn.stuba.want.swrouter.module.nat.rule.DNATRule;
import net.ctrdn.stuba.want.swrouter.module.nat.rule.SNATInterfaceRule;
import net.ctrdn.stuba.want.swrouter.module.nat.rule.SNATPoolRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NATModule extends DefaultRouterModule {

    private class TimeoutWatchdog implements Runnable {

        private Thread thread;
        private boolean running = true;

        public void start() {
            this.thread = new Thread(this);
            this.thread.setName("NATTranslationTimeoutWatchdogThread");
            this.thread.start();
        }

        @Override
        public void run() {
            try {
                NATModule.this.logger.debug("NAT Translation timeout watchdog started");
                while (this.running) {
                    Date currentDate = new Date();
                    List<NATTranslation> removeList = new ArrayList<>();
                    for (NATTranslation xlation : NATModule.this.translationList) {
                        if (currentDate.getTime() - xlation.getLastActivityDate().getTime() > xlation.getTimeout()) {
                            removeList.add(xlation);
                        }
                    }
                    for (NATTranslation xlation : removeList) {
                        try {
                            if (currentDate.getTime() - xlation.getLastActivityDate().getTime() > xlation.getTimeout() + NATModule.this.getTranslationHoldDownTimeout()) {
                                NATModule.this.translationList.remove(xlation);
                            } else {
                                xlation.deactivate();
                                NATModule.this.logger.debug("NAT Translation {} installed by {} has timed out and is entering hold-down state", xlation, xlation.getInstallerRule());
                            }
                        } catch (NATTranslationException ex) {
                            NATModule.this.logger.warn("Problem deactivating NAT Translation", ex);
                        }
                    }
                    Thread.sleep(2000);
                }
            } catch (InterruptedException ex) {
                throw new RuntimeException(ex);
            }
        }

    }

    private final Logger logger = LoggerFactory.getLogger(NATModule.class);
    private int addressTranslationTimeout = 300000;
    private int portTranslationTimeout = 300000;
    private int translationHoldDownTimeout = 10000;
    private final List<NATPool> poolList = new ArrayList<>();
    private final List<NATRule> ruleList = Collections.synchronizedList(new ArrayList<NATRule>());
    private final List<NATTranslation> translationList = Collections.synchronizedList(new CopyOnWriteArrayList<NATTranslation>());
    private final List<NATAddress> addressList = new CopyOnWriteArrayList<>();
    private final TimeoutWatchdog timeoutWatchdog = new TimeoutWatchdog();

    public NATModule(RouterController controller) {
        super(controller);
    }

    @Override
    public void reloadConfiguration(JsonObject moduleConfiguration) {
        this.poolList.clear();
        this.ruleList.clear();
        if (moduleConfiguration != null) {
            JsonObject timerConfigObject = moduleConfiguration.getJsonObject("Timers");
            JsonArray addressPoolsArray = moduleConfiguration.getJsonArray("AddressPools");
            JsonArray rulesArray = moduleConfiguration.getJsonArray("Rules");

            if (timerConfigObject != null) {
                this.setAddressTranslationTimeout(timerConfigObject.getInt("AddressTranslationTimeout", 60000));
                this.setPortTranslationTimeout(timerConfigObject.getInt("PortTranslationTimeout", 120000));
                this.setTranslationHoldDownTimeout(timerConfigObject.getInt("HoldDownTimeout", 10000));
                this.configureTimeouts();
            }

            if (addressPoolsArray != null) {
                for (JsonObject addressPoolJob : addressPoolsArray.getValuesAs(JsonObject.class)) {
                    try {
                        NATPool addressPool = new NATPool(this, addressPoolJob.getString("Name"), IPv4Prefix.fromString(addressPoolJob.getString("Prefix")));
                        for (JsonString addressString : addressPoolJob.getJsonArray("Addresses").getValuesAs(JsonString.class)) {
                            IPv4Address address = IPv4Address.fromString(addressString.getString());
                            addressPool.addAddress(address);
                        }
                        this.getPoolList().add(addressPool);
                    } catch (IPv4MathException | NATException ex) {
                        this.logger.warn("Failed to load NAT pool", ex);
                    }
                }
            }

            if (rulesArray != null) {
                for (JsonObject ruleConfigObject : rulesArray.getValuesAs(JsonObject.class)) {
                    String type = ruleConfigObject.getString("Type");
                    try {
                        int priority = ruleConfigObject.getInt("Priority");
                        switch (type) {
                            case "SNAT_INTERFACE": {
                                IPv4Prefix insidePrefix = IPv4Prefix.fromString(ruleConfigObject.getString("InsidePrefix"));
                                NetworkInterface iface = this.routerController.getModule(InterfaceManagerModule.class).getNetworkInterfaceByName(ruleConfigObject.getString("OutsideInterface"));
                                SNATInterfaceRule rule = new SNATInterfaceRule(this, priority, insidePrefix, iface);
                                if (ruleConfigObject.containsKey("ECMPOutsideInterfaces") && !ruleConfigObject.isNull("ECMPOutsideInterfaces")) {
                                    JsonArray ecmpIfaceArray = ruleConfigObject.getJsonArray("ECMPOutsideInterfaces");
                                    for (JsonString ecmpIfaceString : ecmpIfaceArray.getValuesAs(JsonString.class)) {
                                        rule.getEcmpOutsideInterfaceList().add(this.getRouterController().getModule(InterfaceManagerModule.class).getNetworkInterfaceByName(ecmpIfaceString.getString()));
                                    }
                                }
                                this.installNATRule(rule);
                                break;
                            }
                            case "SNAT_POOL": {
                                IPv4Prefix insidePrefix = IPv4Prefix.fromString(ruleConfigObject.getString("InsidePrefix"));
                                String outsidePoolName = ruleConfigObject.getString("OutsidePool");
                                NATPool pool = this.getNATPool(outsidePoolName);
                                if (pool != null) {
                                    SNATPoolRule rule = new SNATPoolRule(this, priority, insidePrefix, pool, ruleConfigObject.getBoolean("Overload"));
                                    if (ruleConfigObject.containsKey("ECMPOutsideInterfaces") && !ruleConfigObject.isNull("ECMPOutsideInterfaces")) {
                                        JsonArray ecmpIfaceArray = ruleConfigObject.getJsonArray("ECMPOutsideInterfaces");
                                        for (JsonString ecmpIfaceString : ecmpIfaceArray.getValuesAs(JsonString.class)) {
                                            rule.getEcmpOutsideInterfaceList().add(this.getRouterController().getModule(InterfaceManagerModule.class).getNetworkInterfaceByName(ecmpIfaceString.getString()));
                                        }
                                    }
                                    this.installNATRule(rule);
                                } else {
                                    this.logger.warn("NAT Address Pool named {} does not exist - not loading rule", outsidePoolName);
                                }
                                break;
                            }
                            case "DNAT": {
                                NATAddress outsideAddress = this.getNATAddress(IPv4Address.fromString(ruleConfigObject.getString("OutsideAddress")));
                                IPv4Address insideAddress = IPv4Address.fromString(ruleConfigObject.getString("InsideAddress"));
                                IPv4Protocol protocol = null;
                                Integer outsideProtocolSpecificIdentifier = null;
                                Integer insideProtocolSpecificIdentifier = null;
                                if (ruleConfigObject.containsKey("ServiceConstraints") && !ruleConfigObject.isNull("ServiceConstraints")) {
                                    JsonObject serviceConstraintsObject = ruleConfigObject.getJsonObject("ServiceConstraints");
                                    protocol = IPv4Protocol.valueOf(serviceConstraintsObject.getString("Protocol"));
                                    if (protocol == IPv4Protocol.TCP || protocol == IPv4Protocol.UDP) {
                                        outsideProtocolSpecificIdentifier = serviceConstraintsObject.getInt("OutsidePort");
                                        insideProtocolSpecificIdentifier = serviceConstraintsObject.getInt("InsidePort");
                                    }
                                }
                                DNATRule rule = new DNATRule(this, priority, outsideAddress, insideAddress, protocol, outsideProtocolSpecificIdentifier, insideProtocolSpecificIdentifier);
                                if (ruleConfigObject.containsKey("ECMPOutsideInterfaces") && !ruleConfigObject.isNull("ECMPOutsideInterfaces")) {
                                    JsonArray ecmpIfaceArray = ruleConfigObject.getJsonArray("ECMPOutsideInterfaces");
                                    for (JsonString ecmpIfaceString : ecmpIfaceArray.getValuesAs(JsonString.class)) {
                                        rule.getEcmpOutsideInterfaceList().add(this.getRouterController().getModule(InterfaceManagerModule.class).getNetworkInterfaceByName(ecmpIfaceString.getString()));
                                    }
                                }
                                this.installNATRule(rule);
                                break;
                            }
                            default: {
                                this.logger.warn("Not loading unsupported NAT rule type {}", type);
                            }
                        }
                    } catch (IPv4MathException | NATException ex) {
                        this.logger.warn("Failed to load NAT rule of type {}", type, ex);
                    } catch (NoSuchModuleException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            }
        }
    }

    @Override
    public JsonObjectBuilder dumpConfiguration() {
        JsonObjectBuilder configJob = Json.createObjectBuilder();

        JsonObjectBuilder timersJob = Json.createObjectBuilder();
        timersJob.add("AddressTranslationTimeout", this.getAddressTranslationTimeout());
        timersJob.add("PortTranslationTimeout", this.getPortTranslationTimeout());
        timersJob.add("HoldDownTimeout", this.getTranslationHoldDownTimeout());
        configJob.add("Timers", timersJob);

        JsonArrayBuilder addressPoolJab = Json.createArrayBuilder();
        for (NATPool pool : this.getPoolList()) {
            JsonObjectBuilder poolJob = Json.createObjectBuilder();
            poolJob.add("Name", pool.getName());
            poolJob.add("Prefix", pool.getPrefix().toString());
            JsonArrayBuilder addressJab = Json.createArrayBuilder();
            for (NATAddress address : pool.getAddressList()) {
                addressJab.add(address.getAddress().toString());
            }
            poolJob.add("Addresses", addressJab);
            addressPoolJab.add(poolJob);
        }
        configJob.add("AddressPools", addressPoolJab);

        JsonArrayBuilder rulesJab = Json.createArrayBuilder();
        for (NATRule rule : this.getRuleList()) {
            JsonObjectBuilder ruleJob = Json.createObjectBuilder();
            ruleJob.add("Type", rule.getTypeString());
            ruleJob.add("Priority", rule.getPriority());
            if (SNATInterfaceRule.class.isAssignableFrom(rule.getClass())) {
                SNATInterfaceRule ruleCast = (SNATInterfaceRule) rule;
                ruleJob.add("InsidePrefix", ruleCast.getInsidePrefix().toString());
                ruleJob.add("OutsideInterface", ruleCast.getOutsideInterface().getName());
                if (!ruleCast.getEcmpOutsideInterfaceList().isEmpty()) {
                    JsonArrayBuilder ecmpOutsideIfaceJab = Json.createArrayBuilder();
                    for (NetworkInterface iface : ruleCast.getEcmpOutsideInterfaceList()) {
                        ecmpOutsideIfaceJab.add(iface.getName());
                    }
                    ruleJob.add("ECMPOutsideInterfaces", ecmpOutsideIfaceJab);
                } else {
                    ruleJob.addNull("ECMPOutsideInterfaces");
                }
                rulesJab.add(ruleJob);
            } else if (SNATPoolRule.class.isAssignableFrom(rule.getClass())) {
                SNATPoolRule ruleCast = (SNATPoolRule) rule;
                ruleJob.add("InsidePrefix", ruleCast.getInsidePrefix().toString());
                ruleJob.add("OutsidePool", ruleCast.getOutsidePool().getName());
                ruleJob.add("Overload", ruleCast.isOverloadEnabled());
                if (!ruleCast.getEcmpOutsideInterfaceList().isEmpty()) {
                    JsonArrayBuilder ecmpOutsideIfaceJab = Json.createArrayBuilder();
                    for (NetworkInterface iface : ruleCast.getEcmpOutsideInterfaceList()) {
                        ecmpOutsideIfaceJab.add(iface.getName());
                    }
                    ruleJob.add("ECMPOutsideInterfaces", ecmpOutsideIfaceJab);
                } else {
                    ruleJob.addNull("ECMPOutsideInterfaces");
                }
                rulesJab.add(ruleJob);
            } else if (DNATRule.class.isAssignableFrom(rule.getClass())) {
                DNATRule ruleCast = (DNATRule) rule;
                ruleJob.add("OutsideAddress", ruleCast.getOutsideAddress().getAddress().toString());
                ruleJob.add("InsideAddress", ruleCast.getInsideAddress().toString());
                if (ruleCast.getProtocol() != null) {
                    JsonObjectBuilder serviceConstraintsJob = Json.createObjectBuilder();
                    serviceConstraintsJob.add("Protocol", ruleCast.getProtocol().name());
                    if (ruleCast.getProtocol() == IPv4Protocol.TCP || ruleCast.getProtocol() == IPv4Protocol.UDP) {
                        serviceConstraintsJob.add("OutsidePort", ruleCast.getOutsideProtocolSpecificIdentifier());
                        serviceConstraintsJob.add("InsidePort", ruleCast.getInsideProtocolSpecificIdentifier());
                    }
                    ruleJob.add("ServiceConstraints", serviceConstraintsJob);
                } else {
                    ruleJob.addNull("ServiceConstraints");
                }
                if (!ruleCast.getEcmpOutsideInterfaceList().isEmpty()) {
                    JsonArrayBuilder ecmpOutsideIfaceJab = Json.createArrayBuilder();
                    for (NetworkInterface iface : ruleCast.getEcmpOutsideInterfaceList()) {
                        ecmpOutsideIfaceJab.add(iface.getName());
                    }
                    ruleJob.add("ECMPOutsideInterfaces", ecmpOutsideIfaceJab);
                } else {
                    ruleJob.addNull("ECMPOutsideInterfaces");
                }
                rulesJab.add(ruleJob);
            }
        }
        configJob.add("Rules", rulesJab);

        return configJob;
    }

    public void installNATRule(NATRule rule) {
        this.getRuleList().add(rule);
        this.sortNATRules();
        this.logger.debug("Installed NAT rule Priority#{} {}", rule.getPriority(), rule);
    }

    public void uninstallNATRule(NATRule rule) throws NATException {
        rule.clear();
        this.ruleList.remove(rule);
    }

    public void sortNATRules() {
        Collections.sort(this.getRuleList(), new Comparator<NATRule>() {

            @Override
            public int compare(NATRule o1, NATRule o2) {
                return o1.getPriority() < o2.getPriority() ? -1 : o1.getPriority() == o2.getPriority() ? 0 : 1;
            }
        });
    }

    @Override
    public void initialize() throws ModuleInitializationException {
        this.routerController.getPacketProcessor().addPipelineBranch(new NATUntranslatePipelineBranch(this));
        this.routerController.getPacketProcessor().addPipelineBranch(new NATTranslatePipelineBranch(this));
    }

    @Override
    public void start() {
        this.timeoutWatchdog.start();
    }

    @Override
    public String getName() {
        return "Network Address Translation";
    }

    @Override
    public Integer getRevision() {
        return 1;
    }

    @Override
    public int getLoadPriority() {
        return 1536;
    }

    private void configureTimeouts() {
        NATTranslation.setGlobalTimeouts(this.getPortTranslationTimeout(), this.getAddressTranslationTimeout());
    }

    public int getAddressTranslationTimeout() {
        return addressTranslationTimeout;
    }

    public int getPortTranslationTimeout() {
        return portTranslationTimeout;
    }

    public int getTranslationHoldDownTimeout() {
        return translationHoldDownTimeout;
    }

    public void setAddressTranslationTimeout(int addressTranslationTimeout) {
        this.addressTranslationTimeout = addressTranslationTimeout;
    }

    public void setPortTranslationTimeout(int portTranslationTimeout) {
        this.portTranslationTimeout = portTranslationTimeout;
    }

    public void setTranslationHoldDownTimeout(int translationHoldDownTimeout) {
        this.translationHoldDownTimeout = translationHoldDownTimeout;
    }

    public List<NATPool> getPoolList() {
        return poolList;
    }

    public List<NATRule> getRuleList() {
        return ruleList;
    }

    public List<NATTranslation> getTranslationList() {
        return translationList;
    }

    public void installTranslation(NATTranslation xlation) {
        this.translationList.add(xlation);
        this.logger.debug("Installed NAT translation {}", xlation);
    }

    public NATAddress getNATAddress(IPv4Address address) {
        for (NATAddress na : this.addressList) {
            if (na.getAddress().equals(address)) {
                return na;
            }
        }
        NATAddress na = new NATAddress(address, 10000, 19999, 20000, 29999, 1, 65535);
        this.addressList.add(na);
        return na;
    }

    public NATAddress getNATAddress(NetworkInterface networkInterface) {
        return this.getNATAddress(networkInterface.getIPv4InterfaceAddress().getAddress());
    }

    public NATPool getNATPool(String name) {
        for (NATPool pool : this.poolList) {
            if (pool.getName().equals(name)) {
                return pool;
            }
        }
        return null;
    }

    public RouterController getRouterController() {
        return this.routerController;
    }

    public void addNATPool(NATPool pool) {
        this.poolList.add(pool);
    }
}
