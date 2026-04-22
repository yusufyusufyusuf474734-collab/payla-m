package com.netshare.vpn

import android.net.VpnService
import java.io.FileOutputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel
import java.nio.channels.SocketChannel

object PacketForwarder {

    fun forward(buffer: ByteBuffer, output: FileOutputStream, vpn: VpnService) {
        try {
            val packet = buffer.array()
            val length = buffer.limit()
            val protocol = packet[9].toInt() and 0xFF

            val destIp = InetAddress.getByAddress(packet.copyOfRange(16, 20))
            val destPort = ((packet[22].toInt() and 0xFF) shl 8) or (packet[23].toInt() and 0xFF)

            when (protocol) {
                6 -> forwardTcp(packet, length, destIp, destPort, output, vpn)
                17 -> forwardUdp(packet, length, destIp, destPort, output, vpn)
            }
        } catch (_: Exception) {}
    }

    private fun forwardTcp(
        packet: ByteArray, length: Int,
        destIp: InetAddress, destPort: Int,
        output: FileOutputStream, vpn: VpnService
    ) {
        Thread {
            try {
                val channel = SocketChannel.open()
                vpn.protect(channel.socket())
                channel.connect(InetSocketAddress(destIp, destPort))

                // IP header length
                val ipHeaderLen = (packet[0].toInt() and 0x0F) * 4
                // TCP header length
                val tcpHeaderLen = ((packet[ipHeaderLen + 12].toInt() and 0xF0) shr 4) * 4
                val dataOffset = ipHeaderLen + tcpHeaderLen
                val dataLen = length - dataOffset

                if (dataLen > 0) {
                    channel.write(ByteBuffer.wrap(packet, dataOffset, dataLen))
                }

                val responseBuffer = ByteBuffer.allocate(32767)
                val read = channel.read(responseBuffer)
                if (read > 0) {
                    output.write(responseBuffer.array(), 0, read)
                }
                channel.close()
            } catch (_: Exception) {}
        }.start()
    }

    private fun forwardUdp(
        packet: ByteArray, length: Int,
        destIp: InetAddress, destPort: Int,
        output: FileOutputStream, vpn: VpnService
    ) {
        try {
            val channel = DatagramChannel.open()
            vpn.protect(channel.socket())
            channel.connect(InetSocketAddress(destIp, destPort))

            val payload = packet.copyOfRange(28, length)
            channel.write(ByteBuffer.wrap(payload))

            val responseBuffer = ByteBuffer.allocate(32767)
            channel.read(responseBuffer)
            channel.close()
        } catch (_: Exception) {}
    }
}
