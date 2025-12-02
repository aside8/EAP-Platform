package com.github.aside8.eap.protocol.hsms;

import com.github.aside8.eap.protocol.Message;
import com.github.aside8.eap.protocol.Protocol;
import com.github.aside8.eap.protocol.secs2.SECSII;
import com.github.aside8.eap.protocol.secs2.SecsDataItem;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.CompositeByteBuf;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Optional;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HsmsMessage implements Message {

    private HsmsHeader header;

    private SECSII body;

    // Delegate getter methods to the header object
    public int getStream() {
        return Optional.ofNullable(header)
                .map(HsmsHeader::getStream)
                .orElseThrow(() -> new IllegalStateException("HSMS header cannot be null."));
    }

    public int getFunction() {
        return Optional.ofNullable(header)
                .map(HsmsHeader::getFunction)
                .orElseThrow(() -> new IllegalStateException("HSMS header cannot be null."));
    }

    public HsmsMessageType getMessageType() {
        return Optional.ofNullable(header)
                .map(HsmsHeader::getStype)
                .map(HsmsMessageType::fromSType)
                .orElseThrow(() -> new IllegalStateException("HSMS header cannot be null."));
    }

    public int getSystemBytes() {
        return Optional.ofNullable(header)
                .map(HsmsHeader::getSystemBytes)
                .orElse(0);
    }

    public void setSystemBytes(int systemBytes) {
        if (header == null) {
            header = new HsmsHeader();
        }
        header.setSystemBytes(systemBytes);
    }


    @Override
    public ByteBuf encode(ByteBufAllocator allocator) {
        if (header == null) {
            throw new IllegalStateException("HSMS header cannot be null.");
        }

        ByteBuf headerBuf = header.encode(allocator);
        ByteBuf bodyBuf = null;
        if (body != null) {
            bodyBuf = body.encode(allocator);
        }

        int bodyLength = (bodyBuf != null) ? bodyBuf.readableBytes() : 0;
        int totalLength = headerBuf.readableBytes() + bodyLength;

        ByteBuf lengthBuf = allocator.buffer(4).writeInt(totalLength);
        
        CompositeByteBuf compositeBuf = allocator.compositeBuffer();
        compositeBuf.addComponents(true, lengthBuf, headerBuf);
        if (bodyBuf != null) {
            compositeBuf.addComponents(true, bodyBuf);
        }
        
        return compositeBuf;
    }

    @Override
    public void decode(ByteBuf in) {
        this.header = new HsmsHeader();
        header.decode(in);

        if (in.readableBytes() > 0) {
            this.body = new SecsDataItem(); // Use the public no-arg constructor
            body.decode(in);
        }
    }

    @Override
    public Protocol getProtocol() {
        return Protocol.HSMS;
    }

    @Override
    public String getTraceId() {
        return String.valueOf(getSystemBytes());
    }

}
