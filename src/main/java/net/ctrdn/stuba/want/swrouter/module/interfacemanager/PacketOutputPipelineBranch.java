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

public class PacketOutputPipelineBranch extends DefaultPipelineBranch {

    private final Logger logger = LoggerFactory.getLogger(PacketOutputPipelineBranch.class);

    @Override
    public String getName() {
        return "PACKET_OUTPUT";
    }

    @Override
    public String getDescription() {
        return "Transmission of output traffic";
    }

    @Override
    public int getPriority() {
        return 90001;
    }

    @Override
    public PipelineResult process(Packet packet) {
        try {
            if (packet.getProcessingChain() == ProcessingChain.OUTPUT && (packet.getEthernetType() != EthernetType.IPV4 || packet.getForwarderIPv4Address() == null)) {
                if (packet.getDestinationHardwareAddress() != MACAddress.ZERO && packet.getEgressNetworkInterface() != null) {
                    this.logger.trace("Transmitting OUTPUT packet over interface {}\n{}", packet.getEgressNetworkInterface().getName(), DataTypeHelpers.byteArrayToHexString(packet.getPacketBuffer().getByteArray(0, packet.getPacketBuffer().size()), true));
                    packet.getEgressNetworkInterface().sendPacket(packet);
                    this.logger.debug("Transmitted packet {} over network interface {}", packet.getPacketIdentifier().getUuid().toString(), packet.getEgressNetworkInterface().getName());
                    return PipelineResult.HANDLED;
                } else {
                    this.logger.warn("Packet {} has no destination hardware address or egress interface", packet.getDestinationHardwareAddress());
                    return PipelineResult.DROP;
                }
            }
        } catch (PacketException ex) {
            this.logger.warn("Problem processing output packet {}", packet.getPacketIdentifier().getUuid().toString(), ex);
        }
        return PipelineResult.CONTINUE;
    }

}
