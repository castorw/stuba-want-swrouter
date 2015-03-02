package net.ctrdn.stuba.want.swrouter.core.processing;

import java.util.Date;
import java.util.UUID;

public class PacketIdentifier {

    private final Date receiveDate;
    private Date transmitDate;
    private final UUID uuid;

    protected PacketIdentifier(boolean received) {
        this.receiveDate = (received) ? new Date() : null;
        this.uuid = UUID.randomUUID();
    }

    public Date getReceiveDate() {
        return receiveDate;
    }

    public Date getTransmitDate() {
        return transmitDate;
    }

    public void setTransmitted() {
        this.transmitDate = new Date();
    }

    public UUID getUuid() {
        return uuid;
    }
}
