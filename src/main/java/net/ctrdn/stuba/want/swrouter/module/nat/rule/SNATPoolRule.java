package net.ctrdn.stuba.want.swrouter.module.nat.rule;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import net.ctrdn.stuba.want.swrouter.common.IPv4Protocol;
import net.ctrdn.stuba.want.swrouter.common.net.IPv4Prefix;
import net.ctrdn.stuba.want.swrouter.core.processing.ICMPForIPv4QueryPacketEncapsulation;
import net.ctrdn.stuba.want.swrouter.core.processing.Packet;
import net.ctrdn.stuba.want.swrouter.core.processing.TCPForIPv4PacketEncapsulation;
import net.ctrdn.stuba.want.swrouter.core.processing.UDPForIPv4PacketEncapsulation;
import net.ctrdn.stuba.want.swrouter.exception.NATException;
import net.ctrdn.stuba.want.swrouter.exception.NoSuchModuleException;
import net.ctrdn.stuba.want.swrouter.exception.PacketException;
import net.ctrdn.stuba.want.swrouter.module.interfacemanager.InterfaceManagerModule;
import net.ctrdn.stuba.want.swrouter.module.interfacemanager.NetworkInterface;
import net.ctrdn.stuba.want.swrouter.module.nat.DefaultNATRule;
import net.ctrdn.stuba.want.swrouter.module.nat.NATAddress;
import net.ctrdn.stuba.want.swrouter.module.nat.NATModule;
import net.ctrdn.stuba.want.swrouter.module.nat.NATPool;
import net.ctrdn.stuba.want.swrouter.module.nat.NATRuleResult;
import net.ctrdn.stuba.want.swrouter.module.nat.NATTranslation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SNATPoolRule extends DefaultNATRule {

    private final Logger logger = LoggerFactory.getLogger(SNATPoolRule.class);
    private final IPv4Prefix insidePrefix;
    private final NATPool outsidePool;
    private final boolean overloadEnabled;

    private final List<NATAddress> availableAddressList = new ArrayList<>();
    private final List<NATAddress> usedAddressList = new ArrayList<>();
    private final Map<NATAddress, NetworkInterface> addressInterfaceMap = new HashMap<>();

    public SNATPoolRule(NATModule natModule, int priority, IPv4Prefix insidePrefix, NATPool outsidePool, boolean overloadEnabled) throws NATException {
        super(natModule, priority);
        this.insidePrefix = insidePrefix;
        this.outsidePool = outsidePool;
        this.overloadEnabled = overloadEnabled;
        try {
            for (NATAddress na : this.outsidePool.getAddressList()) {
                NetworkInterface iface = null;
                for (NetworkInterface ni : this.getNatModule().getRouterController().getModule(InterfaceManagerModule.class).getNetworkInterfaces()) {
                    if (ni.getIPv4InterfaceAddress() != null && ni.getIPv4InterfaceAddress().getPrefix().containsAddress(na.getAddress())) {
                        iface = ni;
                        break;
                    }
                }
                if (iface == null) {
                    throw new NATException("Could not find interface for pool address " + na);
                } else {
                    this.availableAddressList.add(na);
                    this.addressInterfaceMap.put(na, iface);
                }
            }
        } catch (NoSuchModuleException ex) {
            throw new NATException("Unable to get required module", ex);
        }
    }

    @Override
    public String getTypeString() {
        return "SNAT_POOL";
    }

    @Override
    public synchronized NATRuleResult translate(Packet packet) {
        try {
            if (this.getInsidePrefix().containsAddress(packet.getSourceIPv4Address())) {
                List<NATAddress> feasibleAddresses = new ArrayList<>();
                for (Map.Entry<NATAddress, NetworkInterface> entry : this.addressInterfaceMap.entrySet()) {
                    if (entry.getValue().equals(packet.getEgressNetworkInterface())) {
                        feasibleAddresses.add(entry.getKey());
                    }
                }
                if (feasibleAddresses.isEmpty()) {
                    return NATRuleResult.CONTINUE;
                }
                if (this.overloadEnabled) {
                    NATAddress natAddress = this.availableAddressList.get(new Random().nextInt(this.availableAddressList.size()));
                    NATTranslation xlation;
                    switch (packet.getIPv4Protocol()) {
                        case TCP: {
                            TCPForIPv4PacketEncapsulation tcpEncapsulation = new TCPForIPv4PacketEncapsulation(packet);
                            xlation = NATTranslation.newPortTranslation(this.getNatModule(), IPv4Protocol.TCP, natAddress, this.addressInterfaceMap.get(natAddress), packet.getSourceIPv4Address(), tcpEncapsulation.getSourcePort());
                            break;
                        }
                        case UDP: {
                            UDPForIPv4PacketEncapsulation udpEncapsulation = new UDPForIPv4PacketEncapsulation(packet);
                            xlation = NATTranslation.newPortTranslation(this.getNatModule(), IPv4Protocol.UDP, natAddress, this.addressInterfaceMap.get(natAddress), packet.getSourceIPv4Address(), udpEncapsulation.getSourcePort());
                            break;
                        }
                        case ICMP: {
                            ICMPForIPv4QueryPacketEncapsulation icmpEncapsulation = new ICMPForIPv4QueryPacketEncapsulation(packet);
                            if (icmpEncapsulation.isQueryBasedMessage()) {
                                xlation = NATTranslation.newPortTranslation(this.getNatModule(), IPv4Protocol.ICMP, natAddress, this.addressInterfaceMap.get(natAddress), packet.getSourceIPv4Address(), icmpEncapsulation.getIdentifier());
                            } else {
                                this.logger.info("Cannot perform NAT on non-query ICMP message on packet {}", packet.getPacketIdentifier().getUuid().toString());
                                return NATRuleResult.DROP;
                            }
                            break;
                        }
                        default: {
                            this.logger.info("Cannot perform NAT on unsupported IPv4 protocol {} on packet {}", packet.getIPv4Protocol().name(), packet.getPacketIdentifier().getUuid().toString());
                            return NATRuleResult.DROP;
                        }
                    }
                    this.getNatModule().installTranslation(xlation);
                    xlation.apply(packet);
                    return NATRuleResult.HANDLED;
                } else {
                    NATAddress natAddress = null;
                    for (NATAddress fa : feasibleAddresses) {
                        if (!this.usedAddressList.contains(fa)) {
                            natAddress = fa;
                            break;
                        }
                    }
                    if (natAddress == null) {
                        this.logger.info("No NAT address available from pool {} for packet {}", this.outsidePool.getName(), packet.getPacketIdentifier().getUuid().toString());
                        return NATRuleResult.DROP;
                    }
                    this.logger.debug("Created address translation for {} via {} on interface {}", packet.getSourceIPv4Address(), natAddress.getAddress(), packet.getEgressNetworkInterface().getName());
                    this.usedAddressList.add(natAddress);
                    NATTranslation xlation = NATTranslation.newAddressTranslation(this.getNatModule(), natAddress, packet.getEgressNetworkInterface(), packet.getSourceIPv4Address());
                    this.getNatModule().installTranslation(xlation);
                    xlation.apply(packet);
                    return NATRuleResult.HANDLED;
                }
            }
        } catch (PacketException | NATException ex) {
            this.logger.warn("Problem processing NAT XLATE on packet {}", packet.getPacketIdentifier().getUuid().toString(), ex);
            return NATRuleResult.DROP;
        }
        return NATRuleResult.CONTINUE;
    }

    @Override
    public NATRuleResult untranslate(Packet packet) {
        return NATRuleResult.CONTINUE;
    }

    @Override
    public void clear() {

    }

    @Override
    public String toString() {
        if (this.overloadEnabled) {
            return "SNAT_POOL/PAT inside " + this.getInsidePrefix() + " <---> outside pool " + this.getOutsidePool().getName();
        } else {
            return "SNAT_POOL/NAT inside " + this.getInsidePrefix() + " <---> outside pool " + this.getOutsidePool().getName();
        }
    }

    public IPv4Prefix getInsidePrefix() {
        return insidePrefix;
    }

    public NATPool getOutsidePool() {
        return outsidePool;
    }

    public boolean isOverloadEnabled() {
        return overloadEnabled;
    }
}
