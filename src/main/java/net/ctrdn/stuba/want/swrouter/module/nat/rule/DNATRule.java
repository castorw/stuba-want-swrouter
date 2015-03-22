package net.ctrdn.stuba.want.swrouter.module.nat.rule;

import java.util.ArrayList;
import java.util.List;
import net.ctrdn.stuba.want.swrouter.common.IPv4Protocol;
import net.ctrdn.stuba.want.swrouter.common.net.IPv4Address;
import net.ctrdn.stuba.want.swrouter.core.processing.Packet;
import net.ctrdn.stuba.want.swrouter.core.processing.TCPForIPv4PacketEncapsulation;
import net.ctrdn.stuba.want.swrouter.core.processing.UDPForIPv4PacketEncapsulation;
import net.ctrdn.stuba.want.swrouter.exception.NATException;
import net.ctrdn.stuba.want.swrouter.exception.NoSuchModuleException;
import net.ctrdn.stuba.want.swrouter.exception.PacketException;
import net.ctrdn.stuba.want.swrouter.module.arpmanager.ARPManagerModule;
import net.ctrdn.stuba.want.swrouter.module.interfacemanager.InterfaceManagerModule;
import net.ctrdn.stuba.want.swrouter.module.interfacemanager.NetworkInterface;
import net.ctrdn.stuba.want.swrouter.module.nat.DefaultNATRule;
import net.ctrdn.stuba.want.swrouter.module.nat.NATAddress;
import net.ctrdn.stuba.want.swrouter.module.nat.NATModule;
import net.ctrdn.stuba.want.swrouter.module.nat.NATRuleResult;
import net.ctrdn.stuba.want.swrouter.module.nat.NATTranslation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DNATRule extends DefaultNATRule {

    private final Logger logger = LoggerFactory.getLogger(DNATRule.class);
    private final NATAddress outsideAddress;
    private final Integer outsideProtocolSpecificIdentifier;
    private final NetworkInterface outsideInterface;
    private final IPv4Address insideAddress;
    private final Integer insideProtocolSpecificIdentifier;
    private final IPv4Protocol protocol;
    private final List<NetworkInterface> ecmpOutsideInterfaceList = new ArrayList<>();

    public DNATRule(NATModule natModule, int priority, NATAddress outsideAddress, IPv4Address insideAddress, IPv4Protocol protocol, Integer outsideProtocolSpecificIdentifier, Integer insideProtocolSpecificIdentifier) throws NATException {
        super(natModule, priority);
        this.outsideAddress = outsideAddress;
        this.insideAddress = insideAddress;
        this.protocol = protocol;
        this.outsideProtocolSpecificIdentifier = outsideProtocolSpecificIdentifier;
        this.insideProtocolSpecificIdentifier = insideProtocolSpecificIdentifier;
        try {
            NetworkInterface iface = null;
            for (NetworkInterface ni : this.getNatModule().getRouterController().getModule(InterfaceManagerModule.class).getNetworkInterfaces()) {
                if (ni.getIPv4InterfaceAddress() != null && ni.getIPv4InterfaceAddress().getPrefix().containsAddress(this.outsideAddress.getAddress())) {
                    iface = ni;
                    break;
                }
            }
            if (iface == null) {
                throw new NATException("Could not find interface for outside NAT address " + this.outsideAddress.getAddress());
            } else {
                this.outsideInterface = iface;
            }
            if (!this.outsideInterface.getIPv4InterfaceAddress().getAddress().equals(this.outsideAddress.getAddress())) {
                this.getNatModule().getRouterController().getModule(ARPManagerModule.class).addVirtualAddress(this.outsideAddress.getAddress(), this.outsideInterface);
            }
        } catch (NoSuchModuleException ex) {
            throw new NATException("Unable to get required module", ex);
        }
    }

    @Override
    public String getTypeString() {
        return "DNAT";
    }

    @Override
    public NATRuleResult translate(Packet packet) {
        return NATRuleResult.CONTINUE;
    }

    @Override
    public NATRuleResult untranslate(Packet packet) {
        try {
            if (this.outsideAddress.getAddress().equals(packet.getDestinationIPv4Address()) && this.outsideInterface.equals(packet.getIngressNetworkInterface())) {
                if (this.protocol == null) {
                    NATTranslation xlation = NATTranslation.newAddressTranslation(this, this.outsideAddress, this.outsideInterface, this.insideAddress);
                    for (NetworkInterface ecmpIface : this.ecmpOutsideInterfaceList) {
                        xlation.getEcmpOutsideInterfaceList().add(ecmpIface);
                    }
                    this.getNatModule().installTranslation(xlation);
                    xlation.apply(packet);
                    return NATRuleResult.HANDLED;
                } else {
                    boolean match = false;
                    if (this.protocol == packet.getIPv4Protocol()) {
                        switch (this.protocol) {
                            case ICMP: {
                                match = true;
                                break;
                            }
                            case TCP: {
                                TCPForIPv4PacketEncapsulation tcpEncap = new TCPForIPv4PacketEncapsulation(packet);
                                if (tcpEncap.getDestinationPort() == this.getOutsideProtocolSpecificIdentifier()) {
                                    match = true;
                                }
                                break;
                            }
                            case UDP: {
                                UDPForIPv4PacketEncapsulation udpEncap = new UDPForIPv4PacketEncapsulation(packet);
                                if (udpEncap.getDestinationPort() == this.getOutsideProtocolSpecificIdentifier()) {
                                    match = true;
                                }
                                break;
                            }
                            default: {
                                throw new NATException("Unsupported protocol");
                            }
                        }
                        if (match) {
                            NATTranslation xlation = NATTranslation.newPortTranslation(this, this.protocol, this.outsideAddress, this.outsideInterface, this.insideAddress, this.insideProtocolSpecificIdentifier, this.outsideProtocolSpecificIdentifier);
                            for (NetworkInterface ecmpIface : this.ecmpOutsideInterfaceList) {
                                xlation.getEcmpOutsideInterfaceList().add(ecmpIface);
                            }
                            this.getNatModule().installTranslation(xlation);
                            xlation.apply(packet);
                            return NATRuleResult.HANDLED;
                        }
                    }
                }
            }
        } catch (PacketException | NATException ex) {
            this.logger.warn("Problem processing NAT UNXLATE on packet {}", packet.getPacketIdentifier().getUuid().toString(), ex);
            return NATRuleResult.DROP;
        }
        return NATRuleResult.CONTINUE;
    }

    @Override
    public void clear() throws NATException {
        try {
            if (!this.outsideInterface.getIPv4InterfaceAddress().getAddress().equals(this.outsideAddress.getAddress())) {
                this.getNatModule().getRouterController().getModule(ARPManagerModule.class).removeVirtualAddress(this.outsideAddress.getAddress(), this.outsideInterface);
            }
        } catch (NoSuchModuleException ex) {
            throw new NATException("Unable to get required module", ex);
        }
    }

    @Override
    public String toString() {
        if (this.protocol == null) {
            return "<RULE:DNAT/NAT outside " + this.getOutsideAddress().getAddress() + " on " + this.getOutsideInterface().getName() + " <---> inside " + this.getInsideAddress() + ">";
        } else if (this.protocol == IPv4Protocol.ICMP) {
            return "<RULE:DNAT/PAT outside " + this.getOutsideAddress().getAddress() + " on " + this.getOutsideInterface().getName() + " ICMP <---> inside " + this.getInsideAddress() + " ICMP>";
        } else {
            return "<RULE:DNAT/PAT outside " + this.getOutsideAddress().getAddress() + " on " + this.getOutsideInterface().getName() + " " + this.protocol.name() + "/" + this.outsideProtocolSpecificIdentifier + " <---> inside " + this.getInsideAddress() + " " + this.protocol.name() + "/" + this.insideProtocolSpecificIdentifier + ">";
        }
    }

    public NATAddress getOutsideAddress() {
        return outsideAddress;
    }

    public Integer getOutsideProtocolSpecificIdentifier() {
        return outsideProtocolSpecificIdentifier;
    }

    public NetworkInterface getOutsideInterface() {
        return outsideInterface;
    }

    public IPv4Address getInsideAddress() {
        return insideAddress;
    }

    public Integer getInsideProtocolSpecificIdentifier() {
        return insideProtocolSpecificIdentifier;
    }

    public IPv4Protocol getProtocol() {
        return protocol;
    }

    @Override
    public List<NetworkInterface> getEcmpOutsideInterfaceList() {
        return ecmpOutsideInterfaceList;
    }

    @Override
    public void onTranslationDeactivated(NATTranslation translation) {
    }

}
