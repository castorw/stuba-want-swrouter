package net.ctrdn.stuba.want.swrouter.module.nat;

import java.io.IOException;
import java.util.Date;
import net.ctrdn.stuba.want.swrouter.common.EthernetType;
import net.ctrdn.stuba.want.swrouter.common.IPv4Protocol;
import net.ctrdn.stuba.want.swrouter.common.MACAddress;
import net.ctrdn.stuba.want.swrouter.common.net.IPv4Address;
import net.ctrdn.stuba.want.swrouter.core.processing.ICMPForIPv4QueryPacketEncapsulation;
import net.ctrdn.stuba.want.swrouter.core.processing.Packet;
import net.ctrdn.stuba.want.swrouter.core.processing.ProcessingChain;
import net.ctrdn.stuba.want.swrouter.core.processing.TCPForIPv4PacketEncapsulation;
import net.ctrdn.stuba.want.swrouter.core.processing.UDPForIPv4PacketEncapsulation;
import net.ctrdn.stuba.want.swrouter.exception.NATException;
import net.ctrdn.stuba.want.swrouter.exception.NATTranslationException;
import net.ctrdn.stuba.want.swrouter.exception.PacketException;
import net.ctrdn.stuba.want.swrouter.module.interfacemanager.NetworkInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NATTranslation {

    private final Logger logger = LoggerFactory.getLogger(NATTranslation.class);
    private final IPv4Protocol protocol;
    private final NetworkInterface outsideInterface;
    private final NATAddress outsideAddress;
    private final Integer outsideProtocolSpecificIdentifier;
    private final IPv4Address insideAddress;
    private final Integer insideProtocolSpecificIdentifier;
    private boolean active = true;
    private Date lastActivityDate;

    public final static NATTranslation newAddressTranslation(NATAddress outsideAddress, NetworkInterface outsideInterface, IPv4Address insideAddress) throws NATTranslationException {
        if (outsideAddress.isConfiguredForPortTranslation()) {
            throw new NATTranslationException("NAT address " + outsideAddress.getAddress() + " is already configured for port translation and cannot be used for NAT");
        }
        outsideAddress.setConfiguredForAddressTranslation(true);
        return new NATTranslation(null, outsideAddress, outsideInterface, null, insideAddress, null);
    }

    public final static NATTranslation newPortTranslation(IPv4Protocol protocol, NATAddress outsideAddress, NetworkInterface outsideInterface, IPv4Address insideAddress, Integer insideProtocolPort) throws NATException {
        if (outsideAddress.isConfiguredForAddressTranslation()) {
            throw new NATTranslationException("NAT address " + outsideAddress.getAddress() + " is already configured for address translation and cannot be used for PAT");
        }
        outsideAddress.setConfiguredForPortTranslation(true);
        Integer psi = (protocol == IPv4Protocol.TCP) ? outsideAddress.allocateTCPPort() : (protocol == IPv4Protocol.UDP) ? outsideAddress.allocateUDPPort() : (protocol == IPv4Protocol.ICMP) ? outsideAddress.allocateICMPIdentifier() : null;
        return new NATTranslation(protocol, outsideAddress, outsideInterface, psi, insideAddress, insideProtocolPort);
    }

    private NATTranslation(IPv4Protocol protocol, NATAddress outsideAddress, NetworkInterface outsideInterface, Integer outsideProtocolPort, IPv4Address insideAddress, Integer insideProtocolPort) {
        this.protocol = protocol;
        this.outsideAddress = outsideAddress;
        this.outsideInterface = outsideInterface;
        this.insideAddress = insideAddress;
        this.insideProtocolSpecificIdentifier = insideProtocolPort;
        this.outsideProtocolSpecificIdentifier = outsideProtocolPort;
        this.lastActivityDate = new Date();
    }

    public IPv4Protocol getProtocol() {
        return protocol;
    }

    public NetworkInterface getOutsideInterface() {
        return outsideInterface;
    }

    public NATAddress getOutsideAddress() {
        return outsideAddress;
    }

    public Integer getOutsideProtocolSpecificIdentifier() {
        return outsideProtocolSpecificIdentifier;
    }

    public IPv4Address getInsideAddress() {
        return insideAddress;
    }

    public Integer getInsideProtocolSpecificIdentifier() {
        return insideProtocolSpecificIdentifier;
    }

    public boolean isActive() {
        return active;
    }

    public Date getLastActivityDate() {
        return lastActivityDate;
    }

    @Override
    public String toString() {
        if (this.protocol == null) {
            return "NAT inside " + this.getInsideAddress() + " <---> outside " + this.getOutsideAddress().getAddress() + " on " + this.getOutsideInterface().getName();
        } else {
            return "PAT inside " + this.getInsideAddress() + " " + this.getProtocol().name() + "/" + this.getInsideProtocolSpecificIdentifier() + " <---> outside " + this.getOutsideAddress().getAddress() + " " + this.getProtocol().name() + "/" + this.getOutsideProtocolSpecificIdentifier() + " on " + this.getOutsideInterface().getName();
        }
    }

    public boolean apply(Packet packet) throws NATTranslationException {
        if (this.isActive()) {
            if ((packet.getProcessingChain() == ProcessingChain.INPUT || packet.getProcessingChain() == ProcessingChain.FORWARD) && packet.getEthernetType() == EthernetType.IPV4) {
                try {
                    if (packet.getProcessingChain() == ProcessingChain.FORWARD && this.getProtocol() == null && this.getOutsideInterface().equals(packet.getEgressNetworkInterface()) && this.getInsideAddress().equals(packet.getSourceIPv4Address())) {
                        // IMPLEMENT
                        throw new NATTranslationException("NAT Address Translation is not yet supported");
                    } else if (this.getProtocol() == null && this.getOutsideAddress().equals(packet.getIngressNetworkInterface()) && this.getOutsideAddress().getAddress().equals(packet.getDestinationIPv4Address())) {
                        // IMPLEMENT
                        throw new NATTranslationException("NAT Address Translation is not yet supported");
                    } else if (packet.getProcessingChain() == ProcessingChain.FORWARD && this.getProtocol() == packet.getIPv4Protocol() && this.getOutsideInterface().equals(packet.getEgressNetworkInterface()) && this.getInsideAddress().equals(packet.getSourceIPv4Address())) {
                        // Possible Port Translation
                        switch (this.getProtocol()) {
                            case TCP: {
                                TCPForIPv4PacketEncapsulation tcpEncapsulation = new TCPForIPv4PacketEncapsulation(packet);
                                if (tcpEncapsulation.getSourcePort() == this.getInsideProtocolSpecificIdentifier()) {
                                    tcpEncapsulation.setSourcePort(this.getOutsideProtocolSpecificIdentifier());
                                    tcpEncapsulation.getPacket().setSourceIPv4Address(this.outsideAddress.getAddress());
                                    tcpEncapsulation.calculateTCPChecksum();
                                    tcpEncapsulation.getPacket().calculateIPv4Checksum();
                                    this.updateLastActivity();
                                    return true;
                                }
                                break;
                            }
                            case UDP: {
                                UDPForIPv4PacketEncapsulation udpEncapsulation = new UDPForIPv4PacketEncapsulation(packet);
                                if (udpEncapsulation.getSourcePort() == this.getInsideProtocolSpecificIdentifier()) {
                                    udpEncapsulation.setSourcePort(this.getOutsideProtocolSpecificIdentifier());
                                    udpEncapsulation.getPacket().setSourceIPv4Address(this.outsideAddress.getAddress());
                                    udpEncapsulation.calculateUDPChecksum();
                                    udpEncapsulation.getPacket().calculateIPv4Checksum();
                                    this.updateLastActivity();
                                    return true;
                                }
                                break;
                            }
                            case ICMP: {
                                ICMPForIPv4QueryPacketEncapsulation icmpEncapsulation = new ICMPForIPv4QueryPacketEncapsulation(packet);
                                if (icmpEncapsulation.getIdentifier() == this.getInsideProtocolSpecificIdentifier()) {
                                    icmpEncapsulation.setIdentifier(this.getOutsideProtocolSpecificIdentifier());
                                    icmpEncapsulation.getPacket().setSourceIPv4Address(this.outsideAddress.getAddress());
                                    icmpEncapsulation.calculateICMPChecksum();
                                    icmpEncapsulation.getPacket().calculateIPv4Checksum();
                                    this.updateLastActivity();
                                    return true;
                                }
                            }
                        }
                    } else if (this.getProtocol() == packet.getIPv4Protocol() && this.getOutsideInterface().equals(packet.getIngressNetworkInterface()) && this.getOutsideAddress().getAddress().equals(packet.getDestinationIPv4Address())) {
                        // Possible Port Untranslation
                        switch (this.getProtocol()) {
                            case TCP: {
                                TCPForIPv4PacketEncapsulation tcpEncapsulation = new TCPForIPv4PacketEncapsulation(packet);
                                if (tcpEncapsulation.getDestinationPort() == this.getOutsideProtocolSpecificIdentifier()) {
                                    tcpEncapsulation.setDestinationPort(this.getInsideProtocolSpecificIdentifier());
                                    tcpEncapsulation.getPacket().setDestinationIPv4Address(this.getInsideAddress());
                                    tcpEncapsulation.getPacket().setDestinationHardwareAddress(MACAddress.ZERO);
                                    tcpEncapsulation.calculateTCPChecksum();
                                    tcpEncapsulation.getPacket().calculateIPv4Checksum();
                                    packet.setProcessingChain(ProcessingChain.FORWARD);
                                    this.updateLastActivity();
                                    return true;
                                }
                                break;
                            }
                            case UDP: {
                                UDPForIPv4PacketEncapsulation udpEncapsulation = new UDPForIPv4PacketEncapsulation(packet);
                                if (udpEncapsulation.getDestinationPort() == this.getOutsideProtocolSpecificIdentifier()) {
                                    udpEncapsulation.setDestinationPort(this.getInsideProtocolSpecificIdentifier());
                                    udpEncapsulation.getPacket().setDestinationIPv4Address(this.getInsideAddress());
                                    udpEncapsulation.getPacket().setDestinationHardwareAddress(MACAddress.ZERO);
                                    udpEncapsulation.calculateUDPChecksum();
                                    udpEncapsulation.getPacket().calculateIPv4Checksum();
                                    packet.setProcessingChain(ProcessingChain.FORWARD);
                                    this.updateLastActivity();
                                    return true;
                                }
                                break;
                            }
                            case ICMP: {
                                ICMPForIPv4QueryPacketEncapsulation icmpEncapsulation = new ICMPForIPv4QueryPacketEncapsulation(packet);
                                if (icmpEncapsulation.getIdentifier() == this.getOutsideProtocolSpecificIdentifier() && icmpEncapsulation.isQueryBasedMessage()) {
                                    icmpEncapsulation.setIdentifier(this.getInsideProtocolSpecificIdentifier());
                                    icmpEncapsulation.getPacket().setDestinationIPv4Address(this.getInsideAddress());
                                    icmpEncapsulation.getPacket().setDestinationHardwareAddress(MACAddress.ZERO);
                                    icmpEncapsulation.calculateICMPChecksum();
                                    icmpEncapsulation.getPacket().calculateIPv4Checksum();
                                    packet.setProcessingChain(ProcessingChain.FORWARD);
                                    this.updateLastActivity();
                                    return true;
                                }
                            }
                        }
                    }
                } catch (PacketException | IOException ex) {
                    throw new NATTranslationException("Packet NAT Translation has failed", ex);
                }
            } else {
                throw new NATTranslationException("NAT Translation cannot process packet in processing chain " + packet.getProcessingChain().name() + " of ethernet type " + packet.getEthernetType().name());
            }
        }
        return false;
    }

    private void updateLastActivity() {
        this.lastActivityDate = new Date();
    }
}
