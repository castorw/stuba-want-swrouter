package net.ctrdn.stuba.want.swrouter.module.nat.rule;

import net.ctrdn.stuba.want.swrouter.common.IPv4Protocol;
import net.ctrdn.stuba.want.swrouter.common.net.IPv4Prefix;
import net.ctrdn.stuba.want.swrouter.core.processing.Packet;
import net.ctrdn.stuba.want.swrouter.core.processing.TCPForIPv4PacketEncapsulation;
import net.ctrdn.stuba.want.swrouter.core.processing.UDPForIPv4PacketEncapsulation;
import net.ctrdn.stuba.want.swrouter.exception.NATException;
import net.ctrdn.stuba.want.swrouter.exception.PacketException;
import net.ctrdn.stuba.want.swrouter.module.interfacemanager.NetworkInterface;
import net.ctrdn.stuba.want.swrouter.module.nat.DefaultNATRule;
import net.ctrdn.stuba.want.swrouter.module.nat.NATAddress;
import net.ctrdn.stuba.want.swrouter.module.nat.NATModule;
import net.ctrdn.stuba.want.swrouter.module.nat.NATRuleResult;
import net.ctrdn.stuba.want.swrouter.module.nat.SNATTranslation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SNATInterfaceRule extends DefaultNATRule {

    private final Logger logger = LoggerFactory.getLogger(SNATInterfaceRule.class);
    private final IPv4Prefix insidePrefix;
    private final NetworkInterface outsideInterface;
    private final NATAddress outsideAddress;

    public SNATInterfaceRule(NATModule natModule, int priority, IPv4Prefix insidePrefix, NetworkInterface ousideInterface) {
        super(natModule, priority);
        this.insidePrefix = insidePrefix;
        this.outsideInterface = ousideInterface;
        this.outsideAddress = this.getNatModule().getNATAddress(this.outsideInterface);
    }

    @Override
    public NATRuleResult translate(Packet packet) {
        try {
            if (this.getInsidePrefix().containsAddress(packet.getSourceIPv4Address()) && this.outsideInterface.equals(packet.getEgressNetworkInterface())) {
                switch (packet.getIPv4Protocol()) {
                    case TCP: {
                        TCPForIPv4PacketEncapsulation tcpEncapsulation = new TCPForIPv4PacketEncapsulation(packet);
                        SNATTranslation xlation = SNATTranslation.newPortTranslation(IPv4Protocol.TCP, this.outsideAddress, this.outsideInterface, packet.getSourceIPv4Address(), tcpEncapsulation.getSourcePort());
                        this.getNatModule().installTranslation(xlation);
                        xlation.matchAndApply(packet);
                        return NATRuleResult.HANDLED;
                    }
                    case UDP: {
                        UDPForIPv4PacketEncapsulation udpEncapsulation = new UDPForIPv4PacketEncapsulation(packet);
                        SNATTranslation xlation = SNATTranslation.newPortTranslation(IPv4Protocol.TCP, this.outsideAddress, this.outsideInterface, packet.getSourceIPv4Address(), udpEncapsulation.getSourcePort());
                        this.getNatModule().installTranslation(xlation);
                        xlation.matchAndApply(packet);
                        return NATRuleResult.HANDLED;
                    }
                    default: {
                        this.logger.warn("Cannot preform NAT XLATE on packet of protocol {}", packet.getIPv4Protocol().name());
                    }
                }
                return NATRuleResult.CONTINUE;
            }
        } catch (PacketException | NATException ex) {
            this.logger.warn("Problem processing NAT XLATE on packet {}", packet.getPacketIdentifier().getUuid().toString(), ex);
            return NATRuleResult.DROP;
        }
        return NATRuleResult.CONTINUE;
    }

    public IPv4Prefix getInsidePrefix() {
        return insidePrefix;
    }

    public NetworkInterface getOutsideInterface() {
        return outsideInterface;
    }

    @Override
    public void clear() {
    }

    @Override
    public String getTypeString() {
        return "SNAT_INTERFACE";
    }
}
