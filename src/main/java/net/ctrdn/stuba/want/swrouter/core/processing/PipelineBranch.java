package net.ctrdn.stuba.want.swrouter.core.processing;

public interface PipelineBranch {

    public String getName();

    public String getDescription();

    public int getPriority();

    public boolean isEnabled();

    public void setEnabled(boolean enabled);

    public PipelineResult process(Packet packet);
}
