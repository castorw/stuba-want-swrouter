package net.ctrdn.stuba.want.swrouter.module.interfacemanager;

import net.ctrdn.stuba.want.swrouter.common.DataTypeHelpers;
import net.ctrdn.stuba.want.swrouter.common.EthernetType;
import net.ctrdn.stuba.want.swrouter.common.MACAddress;
import net.ctrdn.stuba.want.swrouter.core.processing.DefaultPipelineBranch;
import net.ctrdn.stuba.want.swrouter.core.processing.Packet;
import net.ctrdn.stuba.want.swrouter.core.processing.PipelineResult;
import net.ctrdn.stuba.want.swrouter.core.processing.ProcessingChain;
import net.ctrdn.stuba.want.swrouter.exception.PacketException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PacketForwardPipelineBranch extends DefaultPipelineBranch {

    private final Logger logger = LoggerFactory.getLogger(PacketForwardPipelineBranch.class);

    @Override
    public String getName() {
        return "PACKET_FORWARD";
    }

    @Override
    public String getDescription() {
        return "Forwarding of IPv4 traffic";
    }

    @Override
    public int getPriority() {
        return 90000;
    }

    @Override
    public PipelineResult process(Packet packet) {
        try {
            if ((packet.getProcessingChain() == ProcessingChain.FORWARD || packet.getProcessingChain() == ProcessingChain.OUTPUT) && packet.getEthernetType() == EthernetType.IPV4 && packet.getForwarderIPv4Address() != null && packet.getForwarderHardwareAddress() != null) {
                try {
                    if (packet.getForwarderIPv4Address() == null && packet.getForwarderHardwareAddress() == null && packet.getEgressNetworkInterface() != null && !packet.getDestinationHardwareAddress().equals(MACAddress.ZERO) && packet.getDestinationHardwareAddress() != null) {
                        packet.setSourceHardwareAddress(packet.getEgressNetworkInterface().getHardwareAddress());
                        packet.setIPv4TimeToLive((short) (packet.getIPv4TimeToLive() - 1));
                        packet.calculateIPv4Checksum();
                        this.logger.trace("Transmitting FORWARD packet over interface {}\n{}", packet.getEgressNetworkInterface().getName(), DataTypeHelpers.byteArrayToHexString(packet.getPacketBuffer().getByteArray(0, packet.getPacketBuffer().size()), true));
                        packet.getEgressNetworkInterface().sendPacket(packet);
                        return PipelineResult.HANDLED;
                    } else if (packet.getForwarderIPv4Address() != null && packet.getForwarderHardwareAddress() != null && !packet.getForwarderHardwareAddress().equals(MACAddress.ZERO) && packet.getEgressNetworkInterface() != null) {
                        packet.setSourceHardwareAddress(packet.getEgressNetworkInterface().getHardwareAddress());
                        packet.setDestinationHardwareAddress(packet.getForwarderHardwareAddress());
                        packet.setIPv4TimeToLive((short) (packet.getIPv4TimeToLive() - 1));
                        packet.calculateIPv4Checksum();
                        this.logger.trace("Transmitting FORWARD packet over interface {}\n{}", packet.getEgressNetworkInterface().getName(), DataTypeHelpers.byteArrayToHexString(packet.getPacketBuffer().getByteArray(0, packet.getPacketBuffer().size()), true));
                        packet.getEgressNetworkInterface().sendPacket(packet);
                        return PipelineResult.HANDLED;
                    } else {
                        this.logger.info("Cannot forward packet {} - no forwarding information set", packet.getPacketIdentifier().getUuid().toString());
                        return PipelineResult.DROP;
                    }
                } catch (PacketException ex) {
                    this.logger.warn("Failed processing forwarded packet {}", packet.getPacketIdentifier().getUuid().toString(), ex);
                    return PipelineResult.DROP;
                }
            }
        } catch (PacketException ex) {
            this.logger.warn("Problem processing forward packet {}", packet.getPacketIdentifier().getUuid().toString(), ex);
        }
        return PipelineResult.CONTINUE;
    }

}
