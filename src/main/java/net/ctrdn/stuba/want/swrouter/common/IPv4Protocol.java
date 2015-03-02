package net.ctrdn.stuba.want.swrouter.common;

public enum IPv4Protocol {

    UNKNOWN("00"),
    TCP("06"),
    UDP("11"),
    ICMP("01");

    private final byte code;
    private byte originalCode;

    private IPv4Protocol(String code) {
        this.code = DataTypeHelpers.hexStringToByteArray(code)[0];
        this.originalCode = this.code;
    }

    public byte getCode() {
        return code;
    }

    public byte getOriginalCode() {
        return this.originalCode;
    }

    public static IPv4Protocol valueOf(byte codeByte) {
        for (IPv4Protocol t : IPv4Protocol.values()) {
            if (t.getCode() == codeByte) {
                return t;
            }
        }
        IPv4Protocol proto = IPv4Protocol.UNKNOWN;
        proto.originalCode = codeByte;
        return proto;
    }
}
