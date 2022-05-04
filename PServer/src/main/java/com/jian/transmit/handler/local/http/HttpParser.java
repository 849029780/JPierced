package com.jian.transmit.handler.local.http;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.TooLongFrameException;
import io.netty.handler.codec.http.HttpConstants;
import io.netty.util.ByteProcessor;
import io.netty.util.internal.AppendableCharSequence;

/***
 * 读行转换器
 * @author Jian
 * @date 2022/1/19
 */
public class HttpParser implements ByteProcessor {

    private final AppendableCharSequence seq;
    private final int maxLength;
    int size;

    public HttpParser(AppendableCharSequence seq, int maxLength) {
        this.seq = seq;
        this.maxLength = maxLength;
    }

    public ParseAppendableCharSequence parse(ByteBuf buffer) {
        final int oldSize = size;
        seq.reset();
        int startIndex = buffer.readerIndex();
        int i = buffer.forEachByte(this);
        if (i == -1) {
            size = oldSize;
            return null;
        }
        int endIndex = i + 1;
        buffer.readerIndex(endIndex);
        return new ParseAppendableCharSequence(startIndex, endIndex, seq);
        //return seq;
    }

    public ParseAppendableCharSequence parseReset(ByteBuf buffer) {
        reset();
        return parse(buffer);
    }

    public void reset() {
        size = 0;
    }

    @Override
    public boolean process(byte value) throws Exception {
        char nextByte = (char) (value & 0xFF);
        if (nextByte == HttpConstants.LF) {
            int len = seq.length();
            // Drop CR if we had a CRLF pair
            if (len >= 1 && seq.charAtUnsafe(len - 1) == HttpConstants.CR) {
                --size;
                seq.setLength(len - 1);
            }
            return false;
        }

        increaseCount();

        seq.append(nextByte);
        return true;
    }

    protected final void increaseCount() {
        if (++size > maxLength) {
            // TODO: Respond with Bad Request and discard the traffic
            //    or close the connection.
            //       No need to notify the upstream handlers - just log.
            //       If decoding a response, just throw an exception.
            throw newException(maxLength);
        }
    }

    protected TooLongFrameException newException(int maxLength) {
        return new TooLongFrameException("HTTP header is larger than " + maxLength + " bytes.");
    }


}
