package net.ctrdn.stuba.want.swrouter.module.interfacemanager;

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
        return 91000;
    }

    @Override
    public PipelineResult process(Packet packet) {
        if (packet.getProcessingChain() == ProcessingChain.FORWARD && packet.getEthernetType() == EthernetType.IPV4) {
            try {
                if (packet.getForwarderIPv4Address() != null && packet.getForwarderHardwareAddress() != null && packet.getForwarderHardwareAddress() != MACAddress.ZERO && packet.getEgressNetworkInterface() != null) {
                    packet.setDestinationHardwareAddress(packet.getForwarderHardwareAddress());
                    packet.getEgressNetworkInterface().sendPacket(packet);
                    return PipelineResult.HANDLED;
                } else {
                    this.logger.warn("Cannot forward packet {} - no forwarding information set", packet.getPacketIdentifier().getUuid().toString());
                    return PipelineResult.DROP;
                }
            } catch (PacketException ex) {
                this.logger.warn("Failed processing forwarded packet {}", packet.getPacketIdentifier().getUuid().toString(), ex);
                return PipelineResult.DROP;
            }
        }
        return PipelineResult.CONTINUE;
    }

}
