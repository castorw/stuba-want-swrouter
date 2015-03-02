package net.ctrdn.stuba.want.swrouter.core.processing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PacketProcessor {

    private final Logger logger = LoggerFactory.getLogger(PacketProcessor.class);
    private final Executor threadPool;
    private int threadCount;
    private final List<PipelineBranch> pipeline = new ArrayList<>();
    private JsonObject configuration;

    public PacketProcessor(int threadCount) {
        this.threadCount = threadCount;
        this.threadPool = Executors.newFixedThreadPool(this.threadCount, new ThreadFactory() {
            private int nextProcessingThreadId = 0;

            @Override
            public Thread newThread(Runnable r) {
                Thread newThread = new Thread(r);
                newThread.setName("PacketProcessingThread-" + this.nextProcessingThreadId);
                nextProcessingThreadId++;
                return newThread;
            }
        });
    }

    public void reloadConfiguration(JsonObject configuration) {
        this.configuration = configuration;
        JsonObject pipelineConfigurationObject = this.configuration.getJsonObject("PipelineConfiguration");
        if (pipelineConfigurationObject != null) {
            for (PipelineBranch branch : this.pipeline) {
                if (pipelineConfigurationObject.containsKey(branch.getClass().getName())) {
                    branch.setEnabled(pipelineConfigurationObject.getJsonObject(branch.getClass().getName()).getBoolean("Enabled"));
                }
            }
        }
    }

    public JsonObjectBuilder dumpConfiguration() {
        JsonObjectBuilder configJob = Json.createObjectBuilder();
        JsonObjectBuilder pipelineConfigJob = Json.createObjectBuilder();
        configJob.add("PipelineConfiguration", pipelineConfigJob);
        for (PipelineBranch branch : this.pipeline) {
            JsonObjectBuilder branchJob = Json.createObjectBuilder();
            branchJob.add("Enabled", branch.isEnabled());
            pipelineConfigJob.add(branch.getClass().getName(), branchJob);
        }
        return configJob;
    }

    public void addPipelineBranch(PipelineBranch branch) {
        this.pipeline.add(branch);
        Collections.sort(this.pipeline, new Comparator<PipelineBranch>() {

            @Override
            public int compare(PipelineBranch o1, PipelineBranch o2) {
                return o1.getPriority() < o2.getPriority() ? -1 : o1.getPriority() == o2.getPriority() ? 0 : 1;
            }
        });
        if (this.configuration != null) {
            JsonObject pipelineConfigurationObject = this.configuration.getJsonObject("PipelineConfiguration");
            if (pipelineConfigurationObject != null) {
                if (pipelineConfigurationObject.containsKey(branch.getClass().getName())) {
                    branch.setEnabled(pipelineConfigurationObject.getJsonObject(branch.getClass().getName()).getBoolean("Enabled"));
                } else {
                    branch.setEnabled(true);
                }
            } else {
                branch.setEnabled(true);
            }
        }
        this.logger.debug("Added branch {} to the packet processing pipeline with priority {}", branch.getName(), branch.getPriority());
    }

    public PipelineBranch[] getPipelineBranches() {
        return this.pipeline.toArray(new PipelineBranch[this.pipeline.size()]);
    }

    public void processPacket(final Packet packet) {
        Runnable process = new Runnable() {

            @Override
            public void run() {
                boolean breakPipeline = false;
                for (PipelineBranch branch : PacketProcessor.this.pipeline) {
                    PipelineResult result = branch.process(packet);
                    if (!branch.isEnabled()) {
                        continue;
                    }
                    switch (result) {
                        case HANDLED: {
                            breakPipeline = true;
                            PacketProcessor.this.logger.debug("Branch {} has handled packet {}@{}", branch.getName(), packet.getPacketIdentifier().getUuid().toString(), packet.getProcessingChain().name());
                            break;
                        }
                        case DROP: {
                            breakPipeline = true;
                            PacketProcessor.this.logger.debug("Branch {} has dropped packet {}@{}", branch.getName(), packet.getPacketIdentifier().getUuid().toString(), packet.getProcessingChain().name());
                            break;
                        }
                        case CONTINUE:
                    }
                    if (breakPipeline) {
                        break;
                    }
                }
                if (!breakPipeline) {
                    PacketProcessor.this.logger.info("Packet {}@{} reached the end of processing pipeline without being handled", packet.getPacketIdentifier().getUuid().toString(), packet.getProcessingChain().name());
                }
            }
        };

        this.threadPool.execute(process);
    }

    public int getThreadCount() {
        return threadCount;
    }
}
