package net.ctrdn.stuba.want.swrouter.module.icmp;

import net.ctrdn.stuba.want.swrouter.common.EthernetType;
import net.ctrdn.stuba.want.swrouter.common.IPv4Protocol;
import net.ctrdn.stuba.want.swrouter.common.MACAddress;
import net.ctrdn.stuba.want.swrouter.core.processing.DefaultPipelineBranch;
import net.ctrdn.stuba.want.swrouter.core.processing.Packet;
import net.ctrdn.stuba.want.swrouter.core.processing.PipelineResult;
import net.ctrdn.stuba.want.swrouter.core.processing.ProcessingChain;
import net.ctrdn.stuba.want.swrouter.exception.PacketException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ICMPEchoResponderPipelineBranch extends DefaultPipelineBranch {

    private final Logger logger = LoggerFactory.getLogger(ICMPEchoResponderPipelineBranch.class);
    private final ICMPModule icmpModule;

    public ICMPEchoResponderPipelineBranch(ICMPModule module) {
        this.icmpModule = module;
    }

    @Override
    public String getName() {
        return "ICMP_ECHO_RESPONDER";
    }

    @Override
    public String getDescription() {
        return "Provides responses for ICMP echo requests";
    }

    @Override
    public int getPriority() {
        return 65500;
    }

    @Override
    public PipelineResult process(Packet packet) {
        try {
            if (packet.getProcessingChain() == ProcessingChain.INPUT && packet.getEthernetType() == EthernetType.IPV4 && packet.getIPv4Protocol() == IPv4Protocol.ICMP) {
                ICMPv4EchoPacketEncapsulation icmpEncap = new ICMPv4EchoPacketEncapsulation(packet);
                if (icmpEncap.getEchoType() == ICMPv4EchoPacketEncapsulation.EchoType.REQUEST) {
                    Packet replyPacket = new Packet(packet.getPacketBuffer().size(), packet.getIngressNetworkInterface());
                    replyPacket.setEthernetType(EthernetType.IPV4);
                    replyPacket.setDestinationHardwareAddress(MACAddress.ZERO);
                    replyPacket.defaultIPv4Setup();
                    replyPacket.setDestinationIPv4Address(packet.getSourceIPv4Address());
                    replyPacket.setSourceIPv4Address(packet.getDestinationIPv4Address());
                    replyPacket.setIPv4Protocol(IPv4Protocol.ICMP);
                    replyPacket.setIPv4TimeToLive((short) (packet.getIPv4TimeToLive() - 1));
                    replyPacket.setIPv4TotalLength(replyPacket.getIPv4HeaderLength() + 8 + icmpEncap.getData().length);

                    ICMPv4EchoPacketEncapsulation replyEncap = new ICMPv4EchoPacketEncapsulation(replyPacket);
                    replyEncap.setEchoType(ICMPv4EchoPacketEncapsulation.EchoType.REPLY);
                    replyEncap.setIdentifier(icmpEncap.getIdentifier());
                    replyEncap.setSequenceNumber(icmpEncap.getSequenceNumber());
                    replyEncap.setData(icmpEncap.getData());
                    replyEncap.calculateICMPChecksum();
                    replyPacket.calculateIPv4Checksum();
                    this.icmpModule.getRouterController().getPacketProcessor().processPacket(replyPacket);
                    this.logger.debug("Sending ICMP echo reply to {} on interface {}", packet.getSourceIPv4Address(), packet.getIngressNetworkInterface().getName());
                    return PipelineResult.HANDLED;
                } else if (icmpEncap.getEchoType() == ICMPv4EchoPacketEncapsulation.EchoType.UNKNOWN) {
                    this.logger.debug("Not handling unknown ICMP type within packet {}", packet.getPacketIdentifier().getUuid().toString());
                }
            }
            return PipelineResult.CONTINUE;
        } catch (PacketException ex) {
            this.logger.warn("Problem processing packet", ex);
            return PipelineResult.DROP;
        }
    }

}
