package net.ctrdn.stuba.want.swrouter.module.nat;

import java.io.IOException;
import java.util.Date;
import net.ctrdn.stuba.want.swrouter.common.EthernetType;
import net.ctrdn.stuba.want.swrouter.common.IPv4Protocol;
import net.ctrdn.stuba.want.swrouter.common.MACAddress;
import net.ctrdn.stuba.want.swrouter.common.net.IPv4Address;
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

public class SNATTranslation implements NATTranslation {

    private final Logger logger = LoggerFactory.getLogger(SNATTranslation.class);
    private final IPv4Protocol protocol;
    private final NetworkInterface outsideInterface;
    private final NATAddress outsideAddress;
    private final Integer outsideProtocolPort;
    private final IPv4Address insideAddress;
    private final Integer insideProtocolPort;
    private boolean active = true;
    private Date lastActivityDate;

    public final static SNATTranslation newAddressTranslation(NATAddress outsideAddress, NetworkInterface outsideInterface, IPv4Address insideAddress) throws NATTranslationException {
        if (outsideAddress.isConfiguredForPortTranslation()) {
            throw new NATTranslationException("NAT address " + outsideAddress.getAddress() + " is already configured for port translation and cannot be used for NAT");
        }
        outsideAddress.setConfiguredForAddressTranslation(true);
        return new SNATTranslation(null, outsideAddress, outsideInterface, null, insideAddress, null);
    }

    public final static SNATTranslation newPortTranslation(IPv4Protocol protocol, NATAddress outsideAddress, NetworkInterface outsideInterface, IPv4Address insideAddress, Integer insideProtocolPort) throws NATException {
        if (outsideAddress.isConfiguredForAddressTranslation()) {
            throw new NATTranslationException("NAT address " + outsideAddress.getAddress() + " is already configured for address translation and cannot be used for PAT");
        }
        outsideAddress.setConfiguredForPortTranslation(true);
        return new SNATTranslation(protocol, outsideAddress, outsideInterface, outsideAddress.allocateTCPPort(), insideAddress, insideProtocolPort);
    }

    private SNATTranslation(IPv4Protocol protocol, NATAddress outsideAddress, NetworkInterface outsideInterface, Integer outsideProtocolPort, IPv4Address insideAddress, Integer insideProtocolPort) {
        this.protocol = protocol;
        this.outsideAddress = outsideAddress;
        this.outsideInterface = outsideInterface;
        this.insideAddress = insideAddress;
        this.insideProtocolPort = insideProtocolPort;
        this.outsideProtocolPort = outsideProtocolPort;
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

    public Integer getOutsideProtocolPort() {
        return outsideProtocolPort;
    }

    public IPv4Address getInsideAddress() {
        return insideAddress;
    }

    public Integer getInsideProtocolPort() {
        return insideProtocolPort;
    }

    @Override
    public boolean isActive() {
        return active;
    }

    @Override
    public Date getLastActivityDate() {
        return lastActivityDate;
    }

    @Override
    public boolean matchAndApply(Packet packet) throws NATTranslationException {
        if (this.isActive()) {
            if ((packet.getProcessingChain() == ProcessingChain.INPUT || packet.getProcessingChain() == ProcessingChain.FORWARD) && packet.getEthernetType() == EthernetType.IPV4) {
                try {
                    if (packet.getProcessingChain() == ProcessingChain.FORWARD && this.getProtocol() == null && this.getOutsideInterface().equals(packet.getEgressNetworkInterface()) && this.getInsideAddress().equals(packet.getSourceIPv4Address())) {
                        // IMPLEMENT
                        throw new NATTranslationException("SNAT Address Translation is not yet supported");
                    } else if (this.getProtocol() == null && this.getOutsideAddress().equals(packet.getIngressNetworkInterface()) && this.getOutsideAddress().getAddress().equals(packet.getDestinationIPv4Address())) {
                        // IMPLEMENT
                        throw new NATTranslationException("SNAT Address Translation is not yet supported");
                    } else if (packet.getProcessingChain() == ProcessingChain.FORWARD && this.getProtocol() == packet.getIPv4Protocol() && this.getOutsideInterface().equals(packet.getEgressNetworkInterface()) && this.getInsideAddress().equals(packet.getSourceIPv4Address())) {
                        // Possible Port Translation
                        switch (this.getProtocol()) {
                            case TCP: {
                                TCPForIPv4PacketEncapsulation tcpEncapsulation = new TCPForIPv4PacketEncapsulation(packet);
                                if (tcpEncapsulation.getSourcePort() == this.getInsideProtocolPort()) {
                                    tcpEncapsulation.setSourcePort(this.getOutsideProtocolPort());
                                    tcpEncapsulation.getPacket().setSourceIPv4Address(this.outsideAddress.getAddress());
                                    tcpEncapsulation.calculateTCPChecksum();
                                    tcpEncapsulation.getPacket().calculateIPv4Checksum();
                                    return true;
                                }
                                break;
                            }
                            case UDP: {
                                UDPForIPv4PacketEncapsulation udpEncapsulation = new UDPForIPv4PacketEncapsulation(packet);
                                if (udpEncapsulation.getSourcePort() == this.getInsideProtocolPort()) {
                                    udpEncapsulation.setSourcePort(this.getOutsideProtocolPort());
                                    udpEncapsulation.getPacket().setSourceIPv4Address(this.outsideAddress.getAddress());
                                    udpEncapsulation.calculateUDPChecksum();
                                    udpEncapsulation.getPacket().calculateIPv4Checksum();
                                    return true;
                                }
                                break;
                            }
                            case ICMP: {
                                throw new NATTranslationException("ICMP SNAT Translation is not yet supported");
                            }
                        }
                    } else if (this.getProtocol() == packet.getIPv4Protocol() && this.getOutsideInterface().equals(packet.getIngressNetworkInterface()) && this.getOutsideAddress().getAddress().equals(packet.getDestinationIPv4Address())) {
                        // Possible Port Untranslation
                        switch (this.getProtocol()) {
                            case TCP: {
                                TCPForIPv4PacketEncapsulation tcpEncapsulation = new TCPForIPv4PacketEncapsulation(packet);
                                if (tcpEncapsulation.getDestinationPort() == this.getOutsideProtocolPort()) {
                                    tcpEncapsulation.setDestinationPort(this.getInsideProtocolPort());
                                    tcpEncapsulation.getPacket().setDestinationIPv4Address(this.getInsideAddress());
                                    tcpEncapsulation.getPacket().setDestinationHardwareAddress(MACAddress.ZERO);
                                    tcpEncapsulation.calculateTCPChecksum();
                                    tcpEncapsulation.getPacket().calculateIPv4Checksum();
                                    packet.setProcessingChain(ProcessingChain.FORWARD);
                                    return true;
                                }
                                break;
                            }
                            case UDP: {
                                UDPForIPv4PacketEncapsulation udpEncapsulation = new UDPForIPv4PacketEncapsulation(packet);
                                if (udpEncapsulation.getDestinationPort() == this.getOutsideProtocolPort()) {
                                    udpEncapsulation.setDestinationPort(this.getInsideProtocolPort());
                                    udpEncapsulation.getPacket().setDestinationIPv4Address(this.getInsideAddress());
                                    udpEncapsulation.getPacket().setDestinationHardwareAddress(MACAddress.ZERO);
                                    udpEncapsulation.calculateUDPChecksum();
                                    udpEncapsulation.getPacket().calculateIPv4Checksum();
                                    packet.setProcessingChain(ProcessingChain.FORWARD);
                                    return true;
                                }
                                break;
                            }
                            case ICMP: {
                                throw new NATTranslationException("ICMP SNAT Translation is not yet supported");
                            }
                        }
                    }
                } catch (PacketException | IOException ex) {
                    throw new NATTranslationException("Packet SNAT Translation has failet", ex);
                }
            } else {
                throw new NATTranslationException("SNAT Translation cannot process packet in processing chain " + packet.getProcessingChain().name() + " of ethernet type " + packet.getEthernetType().name());
            }
        }
        return false;
    }
}
