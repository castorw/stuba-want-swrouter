package net.ctrdn.stuba.want.swrouter.core.processing;

abstract public class DefaultPipelineBranch implements PipelineBranch {

    private boolean enabled = false;

    @Override
    public boolean isEnabled() {
        return this.enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
