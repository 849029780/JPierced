package com.jian.transmit.tcp.handler.remote.transfer;

import com.jian.commons.Constants;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.ssl.SslHandler;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;

/**
 * 描述
 *
 * @author Jian
 * @date 2022/04/03
 */
public class RemoteChannelInitializer extends ChannelInitializer<Channel> {

    private final RemoteChannelInBoundHandler remoteChannelInBoundHandler;
    private final RemoteMessageToMessageCodec remoteMessageToMessageCodec;

    public RemoteChannelInitializer() {
        remoteChannelInBoundHandler = new RemoteChannelInBoundHandler();
        remoteMessageToMessageCodec = new RemoteMessageToMessageCodec();
    }

    @Override
    protected void initChannel(Channel channel) {
        ChannelPipeline pipeline = channel.pipeline();
        if (Constants.CONFIG.getServer().getUseSsl()) {

            SslHandler sslHandler = Constants.REMOTE_SSL_CONTEXT.newHandler(channel.alloc());
            SSLEngine engine = sslHandler.engine();

            // 关闭主机名校验
            SSLParameters params = engine.getSSLParameters();
            params.setEndpointIdentificationAlgorithm(null);
            engine.setSSLParameters(params);

            pipeline.addFirst(sslHandler);


            //pipeline.addLast(Constants.REMOTE_SSL_CONTEXT.newHandler(channel.alloc()));
        }
        pipeline.addLast(new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4, -4, 0));
        pipeline.addLast(this.remoteMessageToMessageCodec);
        pipeline.addLast(this.remoteChannelInBoundHandler);
    }
}
