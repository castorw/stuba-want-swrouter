package net.ctrdn.stuba.want.swrouter.module.interfacemanager;

import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import net.ctrdn.stuba.want.swrouter.common.IPv4Address;
import net.ctrdn.stuba.want.swrouter.common.MACAddress;
import net.ctrdn.stuba.want.swrouter.core.DefaultRouterModule;
import net.ctrdn.stuba.want.swrouter.core.RouterController;
import net.ctrdn.stuba.want.swrouter.exception.ModuleInitializationException;
import org.jnetpcap.Pcap;
import org.jnetpcap.PcapIf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InterfaceManagerModule extends DefaultRouterModule {

    private boolean initialized = false;
    private JsonObject configuration;
    private final Logger logger = LoggerFactory.getLogger(InterfaceManagerModule.class);
    private final List<NetworkInterface> interfaceList = new ArrayList<>();

    public InterfaceManagerModule(RouterController controller) {
        super(controller);
    }

    @Override
    public void reloadConfiguration(JsonObject moduleConfiguration) {
        this.configuration = moduleConfiguration;
        if (this.configuration != null) {
            JsonObject interfacesConfiguration = this.configuration.getJsonObject("Interfaces");
            if (interfacesConfiguration != null) {
                for (NetworkInterface iface : this.interfaceList) {
                    if (interfacesConfiguration.containsKey(iface.getName()) && !interfacesConfiguration.isNull(iface.getName())) {
                        JsonObject ifaceConfig = interfacesConfiguration.getJsonObject(iface.getName());
                        if (!ifaceConfig.isNull("IPv4Address") && !ifaceConfig.isNull("IPv4NetworkMask")) {
                            iface.setIPv4Address(new IPv4Address(ifaceConfig.getString("IPv4Address")), new IPv4Address(ifaceConfig.getString("IPv4NetworkMask")));
                        }
                        iface.setEnabled(ifaceConfig.getBoolean("Enabled"));
                    }
                }
            }
        }
    }

    @Override
    public JsonObjectBuilder dumpConfiguration() {
        JsonObjectBuilder configJob = Json.createObjectBuilder();
        JsonObjectBuilder interfacesJob = Json.createObjectBuilder();
        for (NetworkInterface iface : this.interfaceList) {
            JsonObjectBuilder ifaceJob = Json.createObjectBuilder();
            ifaceJob.add("Enabled", iface.isEnabled());
            if (iface.getIPv4Address() != null) {
                ifaceJob.add("IPv4Address", iface.getIPv4Address().toString());
            } else {
                ifaceJob.addNull("IPv4Address");
            }
            if (iface.getIPv4NetworkMask() != null) {
                ifaceJob.add("IPv4NetworkMask", iface.getIPv4NetworkMask().toString());
            } else {
                ifaceJob.addNull("IPv4NetworkMask");
            }
            interfacesJob.add(iface.getName(), ifaceJob);
        }
        configJob.add("Interfaces", interfacesJob);
        return configJob;
    }

    @Override
    public void initialize() throws ModuleInitializationException {
        List<PcapIf> list = new ArrayList<>();
        StringBuilder errorBuffer = new StringBuilder();
        Pcap.findAllDevs(list, errorBuffer);
        try {
            Enumeration<java.net.NetworkInterface> nics = java.net.NetworkInterface.getNetworkInterfaces();
            ArrayList<java.net.NetworkInterface> nicsList = Collections.list(nics);
            for (PcapIf iface : list) {
                String interfaceName = iface.getName();
                byte[] interfaceMacAddressBytes = null;
                Integer interfaceMtu = null;
                for (java.net.NetworkInterface jnic : nicsList) {
                    if (jnic.getName().equals(interfaceName)) {
                        if (jnic.getHardwareAddress() != null) {
                            interfaceMacAddressBytes = jnic.getHardwareAddress();
                        }
                        interfaceMtu = jnic.getMTU();
                        break;
                    }
                }
                if (interfaceMacAddressBytes == null) {
                    this.logger.debug("Ignoring interface {} because no hardware address is available", interfaceName);
                } else {
                    NetworkInterfaceImpl nicImpl = new NetworkInterfaceImpl(interfaceName, interfaceMtu, new MACAddress(interfaceMacAddressBytes), iface, this.routerController);
                    this.interfaceList.add((NetworkInterface) nicImpl);
                    this.logger.debug("Adding network interface {} with hardware address {}", nicImpl.getName(), nicImpl.getHardwareAddress());
                }
            }
            Collections.sort(this.interfaceList, new Comparator<NetworkInterface>() {

                @Override
                public int compare(NetworkInterface o1, NetworkInterface o2) {
                    return o1.getName().compareTo(o2.getName());
                }
            });
            this.initialized = true;
            this.logger.info("Enumerated {} interfaces", this.interfaceList.size());
            this.routerController.getPacketProcessor().addPipelineBranch(new PacketLoggerPipelineBranch());
            this.routerController.getPacketProcessor().addPipelineBranch(new PacketOutputPipelineBranch());
            this.routerController.getPacketProcessor().addPipelineBranch(new PacketForwardPipelineBranch());
        } catch (SocketException ex) {
            ModuleInitializationException finalEx = new ModuleInitializationException("Failed to enumerate interfaces");
            finalEx.addSuppressed(ex);
            throw finalEx;
        }
    }

    @Override
    public void start() {
        for (NetworkInterface iface : this.interfaceList) {
            NetworkInterfaceImpl ifaceC = (NetworkInterfaceImpl) iface;
            ifaceC.start();
        }
    }

    @Override
    public String getName() {
        return "Interface Manager";
    }

    @Override
    public Integer getRevision() {
        return 1;
    }
}
