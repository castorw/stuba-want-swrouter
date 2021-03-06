package net.ctrdn.stuba.want.swrouter.module.routingripv2;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonString;
import net.ctrdn.stuba.want.swrouter.common.EthernetType;
import net.ctrdn.stuba.want.swrouter.common.IPv4Protocol;
import net.ctrdn.stuba.want.swrouter.common.MACAddress;
import net.ctrdn.stuba.want.swrouter.common.net.IPv4Address;
import net.ctrdn.stuba.want.swrouter.common.net.IPv4NetworkMask;
import net.ctrdn.stuba.want.swrouter.common.net.IPv4Prefix;
import net.ctrdn.stuba.want.swrouter.core.DefaultRouterModule;
import net.ctrdn.stuba.want.swrouter.core.RouterController;
import net.ctrdn.stuba.want.swrouter.core.processing.Packet;
import net.ctrdn.stuba.want.swrouter.core.processing.UDPForIPv4PacketEncapsulation;
import net.ctrdn.stuba.want.swrouter.exception.IPv4MathException;
import net.ctrdn.stuba.want.swrouter.exception.ModuleInitializationException;
import net.ctrdn.stuba.want.swrouter.exception.NoSuchModuleException;
import net.ctrdn.stuba.want.swrouter.exception.PacketException;
import net.ctrdn.stuba.want.swrouter.exception.RIPv2Exception;
import net.ctrdn.stuba.want.swrouter.module.interfacemanager.InterfaceManagerModule;
import net.ctrdn.stuba.want.swrouter.module.interfacemanager.NetworkInterface;
import net.ctrdn.stuba.want.swrouter.module.routingcore.RoutingCoreModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RIPv2RoutingModule extends DefaultRouterModule {

    private class RouteWatchdog implements Runnable {

        private final RIPv2RoutingModule rm = RIPv2RoutingModule.this;
        private Date lastUpdateTransmitDate = null;

        @Override
        public void run() {
            try {
                while (true) {
                    while (!this.rm.receivedRouteEntryQueue.isEmpty()) {
                        RIPv2RouteEntry entry = this.rm.receivedRouteEntryQueue.poll();
                        RIPv2RoutingModule.this.logger.trace("Processing RIPv2 route entry to {} via {} metric {} route tag {} received from {}", entry.getTargetPrefix(), entry.getNextHopAddress(), entry.getMetric(), entry.getRouteTag(), entry.getSenderAddress());
                        int currentEntryIndex = this.rm.installedRouteEntryList.indexOf(entry);
                        RIPv2Route route = null;
                        for (RIPv2Route r : this.rm.installedRouteList) {
                            if (r.getTargetPrefix().equals(entry.getTargetPrefix())) {
                                route = r;
                                break;
                            }
                        }
                        if (currentEntryIndex == -1) {
                            if (route == null) {
                                if (entry.getMetric() >= 16) {
                                    RIPv2RoutingModule.this.logger.info("Not creating route from poisoned route entry {} received from {}", entry.getTargetPrefix(), entry.getSenderAddress());
                                } else {
                                    route = new RIPv2Route(entry);
                                    this.rm.installRoute(route);
                                }
                            } else {
                                route.newRouteEntryReceived(entry);
                            }
                            this.rm.installedRouteEntryList.add(entry);
                            if (entry.getMetric() >= 16) {
                                entry.setLastState(RIPv2RouteEntryState.HOLD_DOWN);
                            }
                        } else {
                            RIPv2RouteEntry installedEntry = this.rm.installedRouteEntryList.get(currentEntryIndex);
                            boolean updateResult = installedEntry.onUpdateReceived(entry);
                            if (route != null && updateResult) {
                                route.calculateActiveGateways();
                            }
                        }
                    }
                    for (RIPv2Route route : this.rm.installedRouteList) {
                        for (RIPv2RouteEntry entry : route.getRouteEntryList()) {
                            RIPv2RouteEntryState state = this.rm.evaluateRouteEntryState(entry);
                            switch (state) {
                                case FLUSH: {
                                    this.rm.installedRouteEntryList.remove(entry);
                                    route.getRouteEntryList().remove(entry);
                                    this.rm.logger.info("Flushing route to {} via {} received from {}", entry.getTargetPrefix(), entry.getNextHopAddress(), entry.getSenderAddress());
                                    route.calculateActiveGateways();
                                    if (route.getGateways().length <= 0) {
                                        this.rm.uninstallRoute(route);
                                    }
                                    break;
                                }
                                case HOLD_DOWN: {
                                    if (entry.getLastState() == RIPv2RouteEntryState.ACTIVE) {
                                        this.rm.logger.info("Route to {} via {} received from {} is entering hold-down state", entry.getTargetPrefix(), entry.getNextHopAddress(), entry.getSenderAddress());
                                        entry.setLastState(state);
                                        route.calculateActiveGateways();
                                    }
                                    break;
                                }
                                case ACTIVE: {
                                    if (entry.getLastState() == RIPv2RouteEntryState.ACTIVE) {
                                        entry.setLastState(state);
                                    }
                                    break;
                                }
                            }
                        }
                    }
                    if (this.lastUpdateTransmitDate == null) {
                        try {
                            this.rm.sendMulticastRequest();
                            this.lastUpdateTransmitDate = new Date();
                        } catch (RIPv2Exception ex) {
                            RIPv2RoutingModule.this.logger.warn("Failed sending all-interface multicast request", ex);
                        }
                    } else if (new Date().getTime() - this.lastUpdateTransmitDate.getTime() > this.rm.getUpdateInterval()) {
                        try {
                            this.rm.sendMulticastResponse();
                            this.lastUpdateTransmitDate = new Date();
                        } catch (RIPv2Exception ex) {
                            RIPv2RoutingModule.this.logger.warn("Failed sending all-interface multicast response", ex);
                        }
                    }
                    Thread.sleep(500);
                }
            } catch (InterruptedException ex) {
                RIPv2RoutingModule.this.logger.error("RIPv2 Route Watchdog has been interrupted forcefully");
            }
        }

    }

    private final Logger logger = LoggerFactory.getLogger(RIPv2RoutingModule.class);
    private final IPv4Address RIPv2MulticastIPv4Address;
    private final RIPv2PipelineBranch pipelineBranch;
    private final Map<NetworkInterface, RIPv2NetworkInterfaceConfiguration> interfaceConfigurationMap = new ConcurrentHashMap<>();
    private final Queue<RIPv2RouteEntry> receivedRouteEntryQueue = new ConcurrentLinkedQueue<>();
    private final List<RIPv2RouteEntry> installedRouteEntryList = new CopyOnWriteArrayList<>();
    private final List<RIPv2Route> installedRouteList = new CopyOnWriteArrayList<>();
    private final List<IPv4Prefix> networkPrefixList = new CopyOnWriteArrayList<>();
    private final RouteWatchdog watchdog = new RouteWatchdog();
    private Thread watchdogThread;
    private int updateInterval = 30000;
    private int holdDownTimeout = 180000;
    private int flushTimeout = 240000;

    public RIPv2RoutingModule(RouterController controller) {
        super(controller);
        try {
            this.RIPv2MulticastIPv4Address = IPv4Address.fromString("224.0.0.9");
            this.pipelineBranch = new RIPv2PipelineBranch(this);
        } catch (IPv4MathException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void reloadConfiguration(JsonObject moduleConfiguration) {
        if (moduleConfiguration != null) {
            if (moduleConfiguration.containsKey("Interfaces")) {
                JsonObject interfacesConfigObject = moduleConfiguration.getJsonObject("Interfaces");
                for (NetworkInterface iface : this.interfaceConfigurationMap.keySet()) {
                    if (interfacesConfigObject.containsKey(iface.getName())) {
                        JsonObject interfaceObject = interfacesConfigObject.getJsonObject(iface.getName());
                        RIPv2NetworkInterfaceConfiguration interfaceConfig = this.interfaceConfigurationMap.get(iface);
                        interfaceConfig.setEnabled(interfaceObject.getBoolean("Enabled"));
                    }
                }
            }
            if (moduleConfiguration.containsKey("Timers")) {
                this.setUpdateInterval(moduleConfiguration.getJsonObject("Timers").getInt("UpdateInterval"));
                this.setHoldDownTimeout(moduleConfiguration.getJsonObject("Timers").getInt("HoldDownTimeout"));
                this.setFlushTimeout(moduleConfiguration.getJsonObject("Timers").getInt("FlushTimeout"));
            }
            if (moduleConfiguration.containsKey("Networks")) {
                JsonArray networksArray = moduleConfiguration.getJsonArray("Networks");
                for (JsonString networkString : networksArray.getValuesAs(JsonString.class)) {
                    try {
                        String[] explode = networkString.getString().split("/");
                        if (explode.length != 2) {
                            this.logger.warn("{} is not a valid RIPv2 network", networkString.getString());
                        } else {
                            IPv4Prefix networkPrefix = new IPv4Prefix(IPv4Address.fromString(explode[0]), new IPv4NetworkMask(Integer.parseInt(explode[1])));
                            this.getNetworkPrefixList().add(networkPrefix);
                        }
                    } catch (IPv4MathException | NumberFormatException ex) {
                        this.logger.warn("Failed to add RIPv2 network {}", networkString.getString(), ex);
                    }
                }
            }
        }
    }

    @Override
    public JsonObjectBuilder dumpConfiguration() {
        JsonObjectBuilder configJob = Json.createObjectBuilder();
        JsonObjectBuilder interfacesJob = Json.createObjectBuilder();
        for (NetworkInterface iface : this.interfaceConfigurationMap.keySet()) {
            RIPv2NetworkInterfaceConfiguration ifaceConfig = this.interfaceConfigurationMap.get(iface);
            JsonObjectBuilder ifaceJob = Json.createObjectBuilder();
            ifaceJob.add("Enabled", ifaceConfig.isEnabled());
            interfacesJob.add(iface.getName(), ifaceJob);
        }
        configJob.add("Interfaces", interfacesJob);

        JsonObjectBuilder timersJob = Json.createObjectBuilder();
        timersJob.add("UpdateInterval", this.getUpdateInterval());
        timersJob.add("HoldDownTimeout", this.getHoldDownTimeout());
        timersJob.add("FlushTimeout", this.getFlushTimeout());
        configJob.add("Timers", timersJob);

        JsonArrayBuilder networksJab = Json.createArrayBuilder();
        for (IPv4Prefix prefix : this.getNetworkPrefixList()) {
            networksJab.add(prefix.toString());
        }
        configJob.add("Networks", networksJab);
        return configJob;
    }

    @Override
    public void initialize() throws ModuleInitializationException {
        try {
            for (NetworkInterface iface : this.getRouterController().getModule(InterfaceManagerModule.class).getNetworkInterfaces()) {
                this.interfaceConfigurationMap.put(iface, new RIPv2NetworkInterfaceConfiguration(iface));
            }
        } catch (NoSuchModuleException ex) {
            throw new RuntimeException(ex);
        }
        this.routerController.getPacketProcessor().addPipelineBranch(this.pipelineBranch);
    }

    public void processRouteEntry(RIPv2RouteEntry entry) {
        this.receivedRouteEntryQueue.offer(entry);
    }

    private void installRoute(RIPv2Route route) {
        try {
            RoutingCoreModule rcm = this.routerController.getModule(RoutingCoreModule.class);
            rcm.installRoute(route);
            this.installedRouteList.add(route);
        } catch (NoSuchModuleException ex) {
            this.logger.error("Failed to install RIPv2 route to {}", route.getTargetPrefix(), ex);
        }
    }

    private void uninstallRoute(RIPv2Route route) {
        try {
            if (this.installedRouteList.contains(route)) {
                RoutingCoreModule rcm = this.routerController.getModule(RoutingCoreModule.class);
                rcm.uninstallRoute(route);
                this.installedRouteList.remove(route);
            }
        } catch (NoSuchModuleException ex) {
            this.logger.error("Failed to uninstall RIPv2 route to {}", route.getTargetPrefix(), ex);
        }
    }

    private RIPv2RouteEntryState evaluateRouteEntryState(RIPv2RouteEntry entry) {
        Date currentDate = new Date();
        long diff = currentDate.getTime() - entry.getLastUpdateDate().getTime();
        if (diff < this.getHoldDownTimeout()) {
            return RIPv2RouteEntryState.ACTIVE;
        } else if (diff >= this.getHoldDownTimeout() && diff < this.getFlushTimeout()) {
            return RIPv2RouteEntryState.HOLD_DOWN;
        } else {
            return RIPv2RouteEntryState.FLUSH;
        }
    }

    @Override
    public void start() {
        this.watchdogThread = new Thread(this.watchdog);
        this.watchdogThread.setName("RIPv2RouteWatchdogThread");
        this.watchdogThread.start();
    }

    @Override
    public String getName() {
        return "RIPv2 Routing";
    }

    @Override
    public Integer getRevision() {
        return 1;
    }

    @Override
    public int getLoadPriority() {
        return 1024;
    }

    protected RouterController getRouterController() {
        return this.routerController;
    }

    public IPv4Address getRIPv2MulticastIPv4Address() {
        return RIPv2MulticastIPv4Address;
    }

    public RIPv2NetworkInterfaceConfiguration getNetworkInterfaceConfiguration(NetworkInterface networkInterface) {
        if (this.interfaceConfigurationMap.containsKey(networkInterface)) {
            return this.interfaceConfigurationMap.get(networkInterface);
        }
        return null;
    }

    public int getUpdateInterval() {
        return updateInterval;
    }

    public int getHoldDownTimeout() {
        return holdDownTimeout;
    }

    public int getFlushTimeout() {
        return flushTimeout;
    }

    public List<IPv4Prefix> getNetworkPrefixList() {
        return networkPrefixList;
    }

    public void setUpdateInterval(int updateInterval) {
        this.updateInterval = updateInterval;
    }

    public void setHoldDownTimeout(int holdDownTimeout) {
        this.holdDownTimeout = holdDownTimeout;
    }

    public void setFlushTimeout(int flushTimeout) {
        this.flushTimeout = flushTimeout;
    }

    private void sendRequest(NetworkInterface egressInterface, IPv4Address targetAddress) throws RIPv2Exception {
        try {
            if (this.interfaceConfigurationMap.containsKey(egressInterface) && egressInterface.getIPv4InterfaceAddress() != null) {
                if (this.interfaceConfigurationMap.get(egressInterface).isEnabled()) {
                    Packet packet = new Packet(14 + 20 + 8 + 4 + 20, egressInterface);
                    packet.setDestinationHardwareAddress(MACAddress.ZERO);
                    packet.setEthernetType(EthernetType.IPV4);
                    packet.defaultIPv4Setup();
                    packet.setDestinationIPv4Address(targetAddress == null ? this.getRIPv2MulticastIPv4Address() : targetAddress);
                    packet.setSourceIPv4Address(egressInterface.getIPv4InterfaceAddress().getAddress());
                    packet.setIPv4Protocol(IPv4Protocol.UDP);
                    packet.setIPv4TimeToLive((short) 1);
                    packet.setIPv4TotalLength(packet.getIPv4HeaderLength() + 8 + 4 + 20);

                    UDPForIPv4PacketEncapsulation udpEncap = new UDPForIPv4PacketEncapsulation(packet);
                    udpEncap.setSourcePort(520);
                    udpEncap.setDestinationPort(520);
                    udpEncap.setLength(udpEncap.getHeaderLength() + 4 + 20);

                    RIPv2PacketEncapsulation ripEncap = new RIPv2PacketEncapsulation(udpEncap, true);
                    ripEncap.setCommand((short) 1);
                    ripEncap.setRequestWholeTable();
                    udpEncap.calculateUDPChecksum();
                    packet.calculateIPv4Checksum();

                    this.logger.debug("Transmitting RIPv2 full table request over interface {}", egressInterface.getName());
                    this.getRouterController().getPacketProcessor().processPacket(packet);
                } else {
                    throw new RIPv2Exception("Cannot transmit RIPv2 message over RIPv2-disabled interface");
                }
            } else {
                throw new RIPv2Exception("RIPv2 configuration unavailable for interface " + egressInterface.getName());
            }
        } catch (IOException | PacketException ex) {
            throw new RIPv2Exception("Failed building RIPv2 request", ex);
        }
    }

    protected void sendMulticastRequest(NetworkInterface egressInterface) throws RIPv2Exception {
        this.sendRequest(egressInterface, null);
    }

    protected void sendMulticastRequest() throws RIPv2Exception {
        this.logger.debug("Requesting RIPv2 data on all interfaces");
        try {
            for (NetworkInterface iface : this.getRouterController().getModule(InterfaceManagerModule.class).getNetworkInterfaces()) {
                if (this.interfaceConfigurationMap.containsKey(iface) && iface.getIPv4InterfaceAddress() != null) {
                    if (this.interfaceConfigurationMap.get(iface).isEnabled()) {
                        this.sendMulticastRequest(iface);
                    }
                }
            }
        } catch (NoSuchModuleException ex) {
            throw new RuntimeException(ex);
        }
    }

    public List<RIPv2RouteEntry> buildEntryList(NetworkInterface ignoredInterface, Set<IPv4Prefix> includePrefixes) throws IPv4MathException {
        try {
            List<RIPv2RouteEntry> outputEntryList = new ArrayList<>();
            List<IPv4Prefix> addedPrefixes = new ArrayList<>();

            if (this.configuredPrefixesContain(IPv4Prefix.fromString("0.0.0.0/0"))) {
                this.logger.debug("Created RIPv2 response will contain default route to 0.0.0.0/0");
                outputEntryList.add(new RIPv2RouteEntry(2, IPv4Address.fromString("0.0.0.0"), 0, IPv4Prefix.fromString("0.0.0.0/0"), IPv4Address.fromString("0.0.0.0"), 1));
                addedPrefixes.add(IPv4Prefix.fromString("0.0.0.0/0"));
            }

            for (NetworkInterface iface2 : this.getRouterController().getModule(InterfaceManagerModule.class).getNetworkInterfaces()) {
                if (ignoredInterface != iface2 && iface2.getIPv4InterfaceAddress() != null && !addedPrefixes.contains(iface2.getIPv4InterfaceAddress().getPrefix()) && this.configuredPrefixesContain(iface2.getIPv4InterfaceAddress().getPrefix())) {
                    if (includePrefixes == null || includePrefixes.contains(iface2.getIPv4InterfaceAddress().getPrefix())) {
                        outputEntryList.add(new RIPv2RouteEntry(2, iface2.getIPv4InterfaceAddress().getAddress(), 0, iface2.getIPv4InterfaceAddress().getPrefix(), IPv4Address.fromString("0.0.0.0"), 1));
                        addedPrefixes.add(iface2.getIPv4InterfaceAddress().getPrefix());
                    }
                }
            }
            for (RIPv2Route ripRoute : this.installedRouteList) {
                if (!ripRoute.getTargetPrefix().equals(ignoredInterface.getIPv4InterfaceAddress().getPrefix()) && !addedPrefixes.contains(ripRoute.getTargetPrefix())) {
                    if ((includePrefixes == null || includePrefixes.contains(ripRoute.getTargetPrefix())) && ripRoute.getBestMetric() < 15) {
                        outputEntryList.add(new RIPv2RouteEntry(2, ignoredInterface.getIPv4InterfaceAddress().getAddress(), 0, ripRoute.getTargetPrefix(), IPv4Address.fromString("0.0.0.0"), ripRoute.getBestMetric() + 1));
                        addedPrefixes.add(ripRoute.getTargetPrefix());
                    }
                }
            }
            return outputEntryList;
        } catch (NoSuchModuleException ex) {
            throw new RuntimeException(ex);
        }
    }

    public List<RIPv2RouteEntry> buildEntryList(NetworkInterface ignoredInterface) throws IPv4MathException {
        return this.buildEntryList(ignoredInterface, null);
    }

    private List<RIPv2PacketEncapsulation> buildResponse(NetworkInterface egressInterface, IPv4Address targetAddress, List<RIPv2RouteEntry> entries) throws RIPv2Exception {
        try {
            List<RIPv2PacketEncapsulation> encapList = new ArrayList<>();
            int packetCount = (int) Math.ceil((double) (entries.size() / 25.0f));
            for (int packetIndex = 0; packetIndex < packetCount; packetIndex++) {
                int packetOutputEntryCount = entries.size() - (packetIndex * 25);
                Packet packet = new Packet(14 + 20 + 8 + 4 + packetOutputEntryCount * 20, egressInterface);
                packet.setDestinationHardwareAddress(MACAddress.ZERO);
                packet.setEthernetType(EthernetType.IPV4);
                packet.defaultIPv4Setup();
                packet.setDestinationIPv4Address(targetAddress == null ? this.getRIPv2MulticastIPv4Address() : targetAddress);
                packet.setSourceIPv4Address(egressInterface.getIPv4InterfaceAddress().getAddress());
                packet.setIPv4Protocol(IPv4Protocol.UDP);
                packet.setIPv4TimeToLive((short) 1);
                packet.setIPv4TotalLength(packet.getIPv4HeaderLength() + 8 + 4 + packetOutputEntryCount * 20);

                UDPForIPv4PacketEncapsulation udpEncap = new UDPForIPv4PacketEncapsulation(packet);
                udpEncap.setSourcePort(520);
                udpEncap.setDestinationPort(520);
                udpEncap.setLength(udpEncap.getHeaderLength() + 4 + packetOutputEntryCount * 20);

                RIPv2PacketEncapsulation ripEncap = new RIPv2PacketEncapsulation(udpEncap, true);
                ripEncap.setCommand((short) 2);
                ripEncap.setRouteEntries(entries.toArray(new RIPv2RouteEntry[entries.size()]));
                udpEncap.calculateUDPChecksum();
                packet.calculateIPv4Checksum();
                encapList.add(ripEncap);
            }
            return encapList;
        } catch (IOException | PacketException | RIPv2Exception ex) {
            throw new RIPv2Exception("Failed to construct RIPv2 response", ex);
        }
    }

    protected void sendResponse(NetworkInterface egressInterface, IPv4Address targetAddress, List<RIPv2RouteEntry> entries) throws RIPv2Exception {
        if (this.interfaceConfigurationMap.containsKey(egressInterface) && egressInterface.getIPv4InterfaceAddress() != null) {
            if (this.interfaceConfigurationMap.get(egressInterface).isEnabled()) {
                List<RIPv2PacketEncapsulation> encaps = this.buildResponse(egressInterface, targetAddress, entries);
                this.logger.debug("Transmitting RIPv2 update with {} routes over interface {}", entries.size(), egressInterface.getName());
                for (RIPv2PacketEncapsulation encap : encaps) {
                    this.getRouterController().getPacketProcessor().processPacket(encap.getUdpEncapsulation().getPacket());
                }
            } else {
                throw new RIPv2Exception("Cannot transmit RIPv2 message over RIPv2-disabled interface");
            }
        } else {
            throw new RIPv2Exception("RIPv2 configuration unavailable for interface " + egressInterface.getName());
        }
    }

    protected void sendResponse(NetworkInterface egressInterface, IPv4Address targetAddress) throws RIPv2Exception {
        try {
            this.sendResponse(egressInterface, targetAddress, this.buildEntryList(egressInterface));
        } catch (IPv4MathException ex) {
            throw new RIPv2Exception("Failed to transmit RIPv2 response on interface {}", ex);
        }
    }

    protected void sendMulticastResponse(NetworkInterface egressInterface) throws RIPv2Exception {
        if (this.interfaceConfigurationMap.containsKey(egressInterface) && egressInterface.getIPv4InterfaceAddress() != null) {
            if (this.interfaceConfigurationMap.get(egressInterface).isEnabled()) {
                this.sendResponse(egressInterface, null);
            }
        }
    }

    protected void sendMulticastResponse() throws RIPv2Exception {
        this.logger.debug("Transmitting RIPv2 update over all enabled interfaces");
        try {
            for (NetworkInterface iface : this.getRouterController().getModule(InterfaceManagerModule.class).getNetworkInterfaces()) {
                if (this.interfaceConfigurationMap.containsKey(iface) && iface.getIPv4InterfaceAddress() != null) {
                    if (this.interfaceConfigurationMap.get(iface).isEnabled()) {
                        this.sendMulticastResponse(iface);
                    }
                }
            }
        } catch (NoSuchModuleException ex) {
            throw new RuntimeException(ex);
        }
    }

    public boolean configuredPrefixesContain(IPv4Address address) {
        for (IPv4Prefix p : this.getNetworkPrefixList()) {
            if (p.containsAddress(address)) {
                return true;
            }
        }
        return false;
    }

    public boolean configuredPrefixesContain(IPv4Prefix prefix) {
        for (IPv4Prefix p : this.getNetworkPrefixList()) {
            if (p.containsPrefix(prefix)) {
                return true;
            }
        }
        return false;
    }
}
