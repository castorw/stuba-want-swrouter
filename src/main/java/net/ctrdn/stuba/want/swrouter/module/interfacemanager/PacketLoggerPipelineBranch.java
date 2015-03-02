package net.ctrdn.stuba.want.swrouter.module.interfacemanager;

import net.ctrdn.stuba.want.swrouter.core.processing.DefaultPipelineBranch;
import net.ctrdn.stuba.want.swrouter.core.processing.Packet;
import net.ctrdn.stuba.want.swrouter.core.processing.PipelineBranch;
import net.ctrdn.stuba.want.swrouter.core.processing.PipelineResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PacketLoggerPipelineBranch extends DefaultPipelineBranch {

    private final Logger logger = LoggerFactory.getLogger(PacketLoggerPipelineBranch.class);

    @Override
    public String getName() {
        return "PACKET_LOG";
    }

    @Override
    public String getDescription() {
        return "Logs packets entering the processing pipeline";
    }

    @Override
    public int getPriority() {
        return 1;
    }

    @Override
    public PipelineResult process(Packet packet) {
        this.logger.debug(packet.getPacketIdentifier().getUuid().toString() + ", chain " + packet.getProcessingChain().name());
        return PipelineResult.CONTINUE;
    }

}
