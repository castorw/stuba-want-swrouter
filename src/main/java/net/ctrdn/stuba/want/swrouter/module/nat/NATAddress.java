package net.ctrdn.stuba.want.swrouter.module.nat;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.ctrdn.stuba.want.swrouter.common.net.IPv4Address;
import net.ctrdn.stuba.want.swrouter.exception.NATAllocationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NATAddress {

    private final Logger logger = LoggerFactory.getLogger(NATAddress.class);
    private final IPv4Address address;
    private boolean configuredForAddressTranslation = false;
    private boolean configuredForPortTranslation = false;
    private final List<Integer> tcpAllocationList = new ArrayList<>();
    private final List<Integer> udpAllocationList = new ArrayList<>();
    private final List<Integer> icmpAllocationList = new ArrayList<>();
    private final int minTcpPort;
    private final int maxTcpPort;
    private int nextTcpPort;
    private final int minUdpPort;
    private final int maxUdpPort;
    private int nextUdpPort;
    private final int minIcmpIdentifier;
    private final int maxIcmpIdentifier;
    private int nextIcmpIdentifier;

    public NATAddress(IPv4Address address, int minTcpPort, int maxTcpPort, int minUdpPort, int maxUdpPort, int minIcmpIdentifier, int maxIcmpIdentifier) {
        this.address = address;
        this.minTcpPort = minTcpPort;
        this.maxTcpPort = maxTcpPort;
        this.nextTcpPort = this.minTcpPort;
        this.minUdpPort = minUdpPort;
        this.maxUdpPort = maxUdpPort;
        this.nextUdpPort = this.minUdpPort;
        this.minIcmpIdentifier = minIcmpIdentifier;
        this.maxIcmpIdentifier = (maxIcmpIdentifier <= 65535) ? maxIcmpIdentifier : 65535;
        this.nextIcmpIdentifier = this.minIcmpIdentifier;
    }

    public IPv4Address getAddress() {
        return address;
    }

    public boolean isConfiguredForAddressTranslation() {
        return configuredForAddressTranslation;
    }

    public void setConfiguredForAddressTranslation(boolean configuredForAddressTranslation) {
        this.configuredForAddressTranslation = configuredForAddressTranslation;
    }

    public boolean isConfiguredForPortTranslation() {
        return configuredForPortTranslation;
    }

    public void setConfiguredForPortTranslation(boolean configuredForPortTranslation) {
        this.configuredForPortTranslation = configuredForPortTranslation;
    }

    public synchronized Integer allocateTCPPort() throws NATAllocationException {
        if (this.isConfiguredForAddressTranslation() || !this.configuredForPortTranslation) {
            throw new NATAllocationException("NAT address " + this.getAddress() + " is in invalid configuration and cannot provide TCP port allocation (AT: " + this.isConfiguredForAddressTranslation() + ", PT: " + this.isConfiguredForPortTranslation() + ")");
        }
        Integer freePort = null;
        boolean cycleRestarted = false;
        for (; !cycleRestarted && this.nextTcpPort <= this.maxTcpPort; this.nextTcpPort++) {
            if (this.nextTcpPort >= this.maxTcpPort) {
                this.nextTcpPort = this.minTcpPort;
                cycleRestarted = true;
            }
            if (!this.tcpAllocationList.contains(this.nextTcpPort)) {
                freePort = this.nextTcpPort;
                break;
            }
        }
        if (freePort == null) {
            throw new NATAllocationException("No free TCP ports available for NAT address " + this.getAddress());
        }
        this.tcpAllocationList.add(freePort);
        this.logger.debug("NAT address {} has allocated TCP port {}", this.getAddress(), freePort);
        return freePort;
    }

    public synchronized void releaseTCPPort(Integer port) {
        if (port != null && this.tcpAllocationList.contains(port)) {
            this.tcpAllocationList.remove(port);
            this.logger.debug("NAT address {} has freed TCP port {}", this.getAddress(), port);
        }
    }

    public synchronized int allocateUDPPort() throws NATAllocationException {
        if (this.isConfiguredForAddressTranslation() || !this.configuredForPortTranslation) {
            throw new NATAllocationException("NAT address " + this.getAddress() + " is in invalid configuration and cannot provide UDP port allocation (AT: " + this.isConfiguredForAddressTranslation() + ", PT: " + this.isConfiguredForPortTranslation() + ")");
        }
        Integer freePort = null;
        boolean cycleRestarted = false;
        for (; !cycleRestarted && this.nextUdpPort <= this.maxUdpPort; this.nextUdpPort++) {
            if (this.nextUdpPort >= this.maxUdpPort) {
                this.nextUdpPort = this.minUdpPort;
                cycleRestarted = true;
            }
            if (!this.udpAllocationList.contains(this.nextUdpPort)) {
                freePort = this.nextUdpPort;
                break;
            }
        }
        if (freePort == null) {
            throw new NATAllocationException("No free UDP ports available for NAT address " + this.getAddress());
        }
        this.udpAllocationList.add(freePort);
        this.logger.debug("NAT address {} has allocated UDP port {}", this.getAddress(), freePort);
        return freePort;
    }

    public synchronized void releaseUDPPort(Integer port) {
        if (port != null && this.udpAllocationList.contains(port)) {
            this.udpAllocationList.remove(port);
            this.logger.debug("NAT address {} has freed UDP port {}", this.getAddress(), port);
        }
    }

    public synchronized Integer allocateICMPIdentifier() throws NATAllocationException {
        if (this.isConfiguredForAddressTranslation() || !this.configuredForPortTranslation) {
            throw new NATAllocationException("NAT address " + this.getAddress() + " is in invalid configuration and cannot provide ICMP identifier allocation (AT: " + this.isConfiguredForAddressTranslation() + ", PT: " + this.isConfiguredForPortTranslation() + ")");
        }
        Integer freeIdentifier = null;
        boolean cycleRestarted = false;
        for (; !cycleRestarted && this.nextIcmpIdentifier <= this.maxIcmpIdentifier; this.nextIcmpIdentifier++) {
            if (this.nextIcmpIdentifier >= this.maxIcmpIdentifier) {
                this.nextIcmpIdentifier = this.minIcmpIdentifier;
                cycleRestarted = true;
            }
            if (!this.icmpAllocationList.contains(this.nextIcmpIdentifier)) {
                freeIdentifier = this.nextIcmpIdentifier;
                break;
            }
        }
        if (freeIdentifier == null) {
            throw new NATAllocationException("No free ICMP identifiers available for NAT address " + this.getAddress());
        }
        this.icmpAllocationList.add(freeIdentifier);
        this.logger.debug("NAT address {} has allocated ICMP identifier {}", this.getAddress(), freeIdentifier);
        return freeIdentifier;
    }

    public synchronized void releaseICMPIdentifier(Integer identifier) {
        if (identifier != null && this.icmpAllocationList.contains(identifier)) {
            this.icmpAllocationList.remove(identifier);
            this.logger.debug("NAT address {} has freed ICMP identifier {}", this.getAddress(), identifier);
        }
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 89 * hash + Objects.hashCode(this.address);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final NATAddress other = (NATAddress) obj;
        if (!Objects.equals(this.address, other.address)) {
            return false;
        }
        return true;
    }
}
