package net.ctrdn.stuba.want.swrouter.module.interfacemanager;

import net.ctrdn.stuba.want.swrouter.common.EthernetType;
import net.ctrdn.stuba.want.swrouter.common.IPv4Address;
import net.ctrdn.stuba.want.swrouter.common.MACAddress;
import net.ctrdn.stuba.want.swrouter.core.RouterController;
import net.ctrdn.stuba.want.swrouter.core.processing.Packet;
import net.ctrdn.stuba.want.swrouter.core.processing.ProcessingChain;
import net.ctrdn.stuba.want.swrouter.exception.PacketException;
import org.jnetpcap.Pcap;
import org.jnetpcap.PcapIf;
import org.jnetpcap.packet.PcapPacket;
import org.jnetpcap.packet.PcapPacketHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetworkInterfaceImpl implements NetworkInterface {

    private class Receiver implements Runnable {

        private final NetworkInterfaceImpl networkInterface = NetworkInterfaceImpl.this;
        private final Logger logger = NetworkInterfaceImpl.this.logger;
        private boolean running = true;

        @Override
        public void run() {
            PcapPacketHandler<String> jpacketHandler = new PcapPacketHandler<String>() {

                @Override
                public void nextPacket(PcapPacket pcapPacket, String user) {
                    try {
                        ProcessingChain chain = ProcessingChain.FORWARD;
                        Packet packet = new Packet(Receiver.this.networkInterface, pcapPacket);
                        if (packet.getEthernetType() == EthernetType.IPV4) {
                            if (packet.getDestinationIPv4Address().equals(Receiver.this.networkInterface.getIPv4Address())) {
                                chain = ProcessingChain.INPUT;
                            }
                        } else if (packet.getDestinationHardwareAddress().equals(Receiver.this.networkInterface.getHardwareAddress()) || packet.getDestinationHardwareAddress().isBroadcast()) {
                            chain = ProcessingChain.INPUT;
                        }
                        packet.setProcessingChain(chain);
                        Receiver.this.networkInterface.routerController.getPacketProcessor().processPacket(packet);
                    } catch (PacketException ex) {
                        Receiver.this.networkInterface.logger.warn("Failed to process incoming packet on interface {}", Receiver.this.networkInterface.getName(), ex);
                    }
                }
            };

            while (this.running) {
                this.networkInterface.pcap.loop(Pcap.LOOP_INFINITE, jpacketHandler, null);
            }
        }
    }

    private final Logger logger = LoggerFactory.getLogger(NetworkInterface.class);
    private final RouterController routerController;
    private final String name;
    private final int mtu;
    private final MACAddress hardwareAddress;
    private IPv4Address ipv4Address = null;
    private IPv4Address ipv4NetworkMask = null;
    private boolean enabled = false;

    private Pcap pcap;
    private final PcapIf pcapInterface;
    private Receiver receiver;
    private Thread receiverThread;

    protected NetworkInterfaceImpl(String name, int mtu, MACAddress hardwareAddress, PcapIf pcapInterface, RouterController routerController) {
        this.routerController = routerController;
        this.name = name;
        this.mtu = mtu;
        this.hardwareAddress = hardwareAddress;
        this.pcapInterface = pcapInterface;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public int getMTU() {
        return this.mtu;
    }

    @Override
    public MACAddress getHardwareAddress() {
        return this.hardwareAddress;
    }

    @Override
    public IPv4Address getIPv4Address() {
        return this.ipv4Address;
    }

    @Override
    public IPv4Address getIPv4NetworkMask() {
        return this.ipv4NetworkMask;
    }

    @Override
    public void setIPv4Address(IPv4Address address, IPv4Address netmask) {
        this.ipv4Address = address;
        this.ipv4NetworkMask = netmask;
    }

    @Override
    public boolean isEnabled() {
        return this.enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    protected void start() {
        if (this.enabled) {
            if (this.pcap == null) {
                StringBuilder errbuf = new StringBuilder();
                this.logger.debug("Starting packet receiver for interface " + this.pcapInterface.getName());
                int snaplen = 64 * 1024;
                int flags = Pcap.MODE_PROMISCUOUS;
                int timeout = 10 * 1000;
                this.pcap = Pcap.openLive(this.pcapInterface.getName(), snaplen, flags, timeout, errbuf);

                if (pcap == null) {
                    this.logger.error("Failed to open device for capture: " + errbuf.toString());
                    return;
                }
            }

            this.receiver = new Receiver();
            this.receiverThread = new Thread(this.receiver);
            this.receiverThread.start();
        }
    }

    protected void stop() {
        if (this.receiverThread != null) {
            this.receiver.running = false;
            this.receiverThread = null;
            this.receiver = null;
        }
    }

    @Override
    public void sendPacket(Packet packet) {
        if (this.receiver != null) {
            this.pcap.sendPacket(packet.getPcapPacket());
        } else {
            this.logger.warn("Cannot transmit data over inactive interface");
        }
    }
}
