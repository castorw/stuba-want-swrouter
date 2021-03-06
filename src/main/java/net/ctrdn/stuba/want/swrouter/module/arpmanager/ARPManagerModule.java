package net.ctrdn.stuba.want.swrouter.module.arpmanager;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import net.ctrdn.stuba.want.swrouter.common.net.IPv4Address;
import net.ctrdn.stuba.want.swrouter.common.MACAddress;
import net.ctrdn.stuba.want.swrouter.core.DefaultRouterModule;
import net.ctrdn.stuba.want.swrouter.core.RouterController;
import net.ctrdn.stuba.want.swrouter.module.interfacemanager.NetworkInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ARPManagerModule extends DefaultRouterModule {

    private class ARPTableWatchdog implements Runnable {

        private boolean running = false;
        private int entryTimeout = 60000;

        @Override
        public void run() {
            this.running = true;
            try {
                while (this.running) {
                    Date currentDate = new Date();
                    for (ARPTableEntry entry : ARPManagerModule.this.arpTable) {
                        if (currentDate.getTime() - entry.getLastUpdateDate().getTime() > this.entryTimeout) {
                            ARPManagerModule.this.arpTable.remove(entry);
                            if (entry.isComplete()) {
                                ARPManagerModule.this.logger.debug("ARP Table entry {}@{} on {} has timed out", entry.getProtocolAddress(), entry.getHardwareAddress(), entry.getNetworkInterface().getName());
                            } else {
                                ARPManagerModule.this.logger.debug("ARP Table entry {} (incomplete) has timed out", entry.getProtocolAddress());
                            }
                        }
                    }
                    Thread.sleep(1000);
                }
            } catch (InterruptedException ex) {
                ARPManagerModule.this.logger.error("ARP Table Watchdog has been interrupted forcefully");
            }
        }
    }

    private final Logger logger = LoggerFactory.getLogger(ARPManagerModule.class);
    private final ARPPipelineBranch pipelineBranch;
    private final List<ARPTableEntry> arpTable = new CopyOnWriteArrayList<>();
    private final Map<IPv4Address, NetworkInterface> virtualAddressMap = new HashMap<>();
    private final ARPTableWatchdog arpTableWatchdog;
    private Thread arpTableWatchdogThread;
    private int pipelineResolutionTimeout = 5000;

    public ARPManagerModule(RouterController controller) {
        super(controller);
        this.pipelineBranch = new ARPPipelineBranch(this);
        this.arpTableWatchdog = new ARPTableWatchdog();
    }

    @Override
    public void reloadConfiguration(JsonObject moduleConfiguration) {
        if (moduleConfiguration != null) {
            this.arpTableWatchdog.entryTimeout = moduleConfiguration.getInt("TableEntryTimeout", 60000);
            this.setPipelineResolutionTimeout(moduleConfiguration.getInt("PipelineResolutionTimeout", 5000));
        }
    }

    @Override
    public JsonObjectBuilder dumpConfiguration() {
        JsonObjectBuilder configJob = Json.createObjectBuilder();
        configJob.add("TableEntryTimeout", this.arpTableWatchdog.entryTimeout);
        configJob.add("PipelineResolutionTimeout", this.getPipelineResolutionTimeout());
        return configJob;
    }

    @Override
    public void initialize() {
        this.routerController.getPacketProcessor().addPipelineBranch(this.pipelineBranch);
    }

    @Override
    public void start() {
        if (this.arpTableWatchdog != null && this.arpTableWatchdog.running) {
            this.arpTableWatchdog.running = false;
        }
        this.arpTableWatchdogThread = new Thread(this.arpTableWatchdog);
        this.arpTableWatchdogThread.start();
    }

    @Override
    public String getName() {
        return "ARP Manager";
    }

    @Override
    public Integer getRevision() {
        return 1;
    }

    @Override
    public int getLoadPriority() {
        return 128;
    }

    protected RouterController getRouterController() {
        return this.routerController;
    }

    public List<ARPTableEntry> getEntries() {
        return this.arpTable;
    }

    protected synchronized ARPTableEntry getARPTableEntry(IPv4Address protocolAddress, NetworkInterface networkInterface) {
        for (ARPTableEntry entry : this.arpTable) {
            if (entry.getProtocolAddress().equals(protocolAddress) && networkInterface == entry.getNetworkInterface()) {
                return entry;
            }
        }
        ARPTableEntry entry = new ARPTableEntry(protocolAddress, networkInterface);
        this.arpTable.add(entry);
        return entry;
    }

    protected synchronized void updateARPTable(IPv4Address protocolAddress, MACAddress hardwareAddress, NetworkInterface networkInterface) {
        ARPTableEntry entry = this.getARPTableEntry(protocolAddress, networkInterface);
        if (entry == null) {
            entry = new ARPTableEntry(protocolAddress, networkInterface);
            this.arpTable.add(entry);
        }
        entry.update(hardwareAddress);
        this.logger.debug("Updated ARP Table entry {}@{} on {}", entry.getProtocolAddress(), entry.getHardwareAddress(), entry.getNetworkInterface().getName());
    }

    public Map<IPv4Address, NetworkInterface> getVirtualAddressMap() {
        return virtualAddressMap;
    }

    public synchronized void addVirtualAddress(IPv4Address address, NetworkInterface iface) {
        if (!this.virtualAddressMap.containsKey(address)) {
            this.virtualAddressMap.put(address, iface);
        }
    }

    public synchronized void removeVirtualAddress(IPv4Address address, NetworkInterface iface) {
        this.virtualAddressMap.remove(address);
    }

    public void setPipelineResolutionTimeout(int pipelineResolutionTimeout) {
        this.pipelineResolutionTimeout = pipelineResolutionTimeout;
    }

    public int getPipelineResolutionTimeout() {
        return pipelineResolutionTimeout;
    }

    public int getEntryTimeout() {
        return this.arpTableWatchdog.entryTimeout;
    }

    public void setEntryTimeout(int timeout) {
        this.arpTableWatchdog.entryTimeout = timeout;
    }

}
