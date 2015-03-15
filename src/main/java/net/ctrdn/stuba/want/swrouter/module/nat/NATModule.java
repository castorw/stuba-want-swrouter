package net.ctrdn.stuba.want.swrouter.module.nat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonString;
import net.ctrdn.stuba.want.swrouter.common.net.IPv4Address;
import net.ctrdn.stuba.want.swrouter.common.net.IPv4Prefix;
import net.ctrdn.stuba.want.swrouter.core.DefaultRouterModule;
import net.ctrdn.stuba.want.swrouter.core.RouterController;
import net.ctrdn.stuba.want.swrouter.exception.IPv4MathException;
import net.ctrdn.stuba.want.swrouter.exception.ModuleInitializationException;
import net.ctrdn.stuba.want.swrouter.exception.NATException;
import net.ctrdn.stuba.want.swrouter.exception.NoSuchModuleException;
import net.ctrdn.stuba.want.swrouter.module.interfacemanager.InterfaceManagerModule;
import net.ctrdn.stuba.want.swrouter.module.interfacemanager.NetworkInterface;
import net.ctrdn.stuba.want.swrouter.module.nat.rule.SNATInterfaceRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NATModule extends DefaultRouterModule {

    private final Logger logger = LoggerFactory.getLogger(NATModule.class);
    private int addressTranslationTimeout = 300000;
    private int portTranslationTimeout = 300000;
    private final List<NATPool> poolList = new ArrayList<>();
    private final List<NATRule> ruleList = new ArrayList<>();
    private final List<NATTranslation> translationList = new CopyOnWriteArrayList<>();
    private final List<NATAddress> addressList = new CopyOnWriteArrayList<>();

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
                this.addressTranslationTimeout = timerConfigObject.getInt("AddressTranslationTimeout", 300000);
                this.portTranslationTimeout = timerConfigObject.getInt("PortTranslationTimeout", 300000);
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
                        switch (type) {
                            case "SNAT_INTERFACE": {
                                int priority = ruleConfigObject.getInt("Priority");
                                IPv4Prefix insidePrefix = IPv4Prefix.fromString(ruleConfigObject.getString("InsidePrefix"));
                                NetworkInterface iface = this.routerController.getModule(InterfaceManagerModule.class).getNetworkInterfaceByName(ruleConfigObject.getString("OutsideInterface"));
                                SNATInterfaceRule rule = new SNATInterfaceRule(this, priority, insidePrefix, iface);
                                this.installNATRule(rule);
                                break;
                            }
                            default: {
                                this.logger.warn("Not loading unsupported NAT rule type {}", type);
                            }
                        }
                    } catch (IPv4MathException ex) {
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
        timersJob.add("AddressTranslationTimeout", this.addressTranslationTimeout);
        timersJob.add("PortTranslationTimeout", this.portTranslationTimeout);
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
            ruleJob.add("Priority", rule.getPriority());
            if (SNATInterfaceRule.class.isAssignableFrom(rule.getClass())) {
                SNATInterfaceRule ruleCast = (SNATInterfaceRule) rule;
                ruleJob.add("Type", "SNAT_INTERFACE");
                ruleJob.add("InsidePrefix", ruleCast.getInsidePrefix().toString());
                ruleJob.add("OutsideInterface", ruleCast.getOutsideInterface().getName());
                rulesJab.add(ruleJob);
            }
        }
        configJob.add("Rules", rulesJab);

        return configJob;
    }

    private void installNATRule(NATRule rule) {
        this.getRuleList().add(rule);
        Collections.sort(this.getRuleList(), new Comparator<NATRule>() {

            @Override
            public int compare(NATRule o1, NATRule o2) {
                return o1.getPriority() < o2.getPriority() ? -1 : o1.getPriority() == o2.getPriority() ? 0 : 1;
            }
        });
        this.logger.debug("Installed NAT rule Priority#{} {}", rule.getPriority(), rule.getClass().getName());
    }

    @Override
    public void initialize() throws ModuleInitializationException {
        this.routerController.getPacketProcessor().addPipelineBranch(new NATUntranslatePipelineBranch(this));
        this.routerController.getPacketProcessor().addPipelineBranch(new NATTranslatePipelineBranch(this));
    }

    @Override
    public void start() {

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

    public int getAddressTranslationTimeout() {
        return addressTranslationTimeout;
    }

    public int getPortTranslationTimeout() {
        return portTranslationTimeout;
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
        NATAddress na = new NATAddress(address, 10000, 19999, 20000, 29999);
        this.addressList.add(na);
        return na;
    }

    public NATAddress getNATAddress(NetworkInterface networkInterface) {
        return this.getNATAddress(networkInterface.getIPv4InterfaceAddress().getAddress());
    }

}
