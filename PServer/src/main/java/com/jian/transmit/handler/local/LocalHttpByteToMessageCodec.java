package com.jian.transmit.handler.local;


import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.ByteToMessageCodec;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.EncoderException;
import io.netty.handler.codec.http.*;
import io.netty.util.ReferenceCountUtil;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;


/***
 * 处理http协议
 * @author Jian
 * @date 2022-05-13 重写
 */
public class LocalHttpByteToMessageCodec extends ByteToMessageCodec<ByteBuf> {

    private final LocalHttpBaseDecoder reqDecoder;
    private final LocalHttpBaseDecoder respDecoder;

    @Getter
    @Setter
    private String reqHost;

    ByteBuf cumulation;

    private boolean first;

    private final int discardAfterReads;

    private int numReads;

    public LocalHttpByteToMessageCodec(String host, Integer port) {
        //super(host, port);
        LocalHttpByteToMessageCodec ts = this;
        this.discardAfterReads = 16;
        reqDecoder = new LocalHttpBaseDecoder(host, port) {
            @Override
            public String getReqHost() {
                return ts.getReqHost();
            }

            @Override
            public void setReqHost(String reqHost) {
                ts.setReqHost(reqHost);
            }

            @Override
            protected boolean isDecodingRequest() {
                return true;
            }

            @Override
            protected HttpMessage createMessage(String[] initialLine) {
                return new DefaultHttpRequest(
                        HttpVersion.valueOf(initialLine[2]),
                        HttpMethod.valueOf(initialLine[0]), initialLine[1], DefaultHttpHeadersFactory.headersFactory().withValidation(validateHeaders));
            }
        };

        respDecoder = new LocalHttpBaseDecoder(host, port) {
            @Override
            public String getReqHost() {
                return ts.getReqHost();
            }

            @Override
            public void setReqHost(String reqHost) {
                //响应中的host不需要设置
            }

            @Override
            protected boolean isDecodingRequest() {
                return false;
            }

            @Override
            protected HttpMessage createMessage(String[] initialLine) {
                return new DefaultHttpResponse(
                        HttpVersion.valueOf(initialLine[0]),
                        HttpResponseStatus.valueOf(Integer.parseInt(initialLine[1]), initialLine[2]), DefaultHttpHeadersFactory.headersFactory().withValidation(validateHeaders));
            }
        };
    }


    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf buffer, List<Object> outs) {
        reqDecoder.decode(ctx, buffer, outs);
    }

    @Override
    protected void decodeLast(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        reqDecoder.decodeLast(ctx, in, out);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        reqDecoder.userEventTriggered(ctx, evt);
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf msg, ByteBuf out) {
        if (out instanceof CompositeByteBuf compositeByteBuf) {
            List<Object> list = new ArrayList<>();
            respDecoder.decode(ctx, msg, list);
            //msg.retain();
            for (Object o : list) {
                if (o instanceof ByteBuf buf) {
                    //System.out.println(buf.toString(StandardCharsets.UTF_8));
                    compositeByteBuf.addComponent(true, buf);
                }
            }
            //out.writeBytes(msg.readSlice(msg.readableBytes()));
        } else {
            out.writeBytes(msg.readSlice(msg.readableBytes()));
        }
    }


    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
        ByteBuf buf = null;
        try {
            if (acceptOutboundMessage(msg)) {
                first = cumulation == null;
                cumulation = ByteToMessageDecoder.MERGE_CUMULATOR.cumulate(ctx.alloc(), first ? Unpooled.EMPTY_BUFFER : cumulation, (ByteBuf) msg);
                buf = ctx.alloc().compositeBuffer();
                //buf = ctx.alloc().buffer();
                try {
                    encode(ctx, cumulation, buf);
                } finally {
                    if (cumulation != null && !cumulation.isReadable()) {
                        ReferenceCountUtil.release(cumulation);
                        cumulation = null;
                    } else if (++numReads >= discardAfterReads) {
                        numReads = 0;
                        discardSomeReadBytes();
                    }
                }

                if (buf.isReadable()) {
                    ctx.write(buf, promise);
                } else {
                    buf.release();
                    ctx.write(Unpooled.EMPTY_BUFFER, promise);
                }
                buf = null;
            } else {
                ctx.write(msg, promise);
            }
        } catch (EncoderException e) {
            throw e;
        } catch (Throwable e) {
            throw new EncoderException(e);
        } finally {
            if (buf != null) {
                buf.release();
            }
        }
    }


    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        super.handlerRemoved(ctx);
        ByteBuf buf = this.cumulation;
        if (buf != null) {
            this.cumulation = null;
            this.numReads = 0;
            int readable = buf.readableBytes();
            if (readable > 0 && buf.refCnt() > 0) {
                buf.release();
            }
        }
    }


    protected final void discardSomeReadBytes() {
        if (cumulation != null && !first && cumulation.refCnt() == 1) {
            // discard some bytes if possible to make more room in the
            // buffer but only if the refCnt == 1  as otherwise the user may have
            // used slice().retain() or duplicate().retain().
            //
            // See:
            // - https://github.com/netty/netty/issues/2327
            // - https://github.com/netty/netty/issues/1764
            cumulation.discardSomeReadBytes();
        }
    }


}
