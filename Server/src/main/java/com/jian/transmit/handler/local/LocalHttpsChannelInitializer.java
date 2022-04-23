package com.jian.transmit.handler.local;

import com.jian.commons.Constants;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;

/***
 *
 * @author Jian
 * @date 2022/4/2
 */
public class LocalHttpsChannelInitializer extends ChannelInitializer {

    @Override
    protected void initChannel(Channel channel) {
        ChannelPipeline pipeline = channel.pipeline();
        if(Constants.IS_ENABLE_HTTPS){
            pipeline.addFirst(Constants.SSL_LOCAL_PORT_CONTEXT.newHandler(channel.alloc()));
        }
        pipeline.addLast(Constants.LOCAL_CHANNEL_IN_BOUND_HANDLER);
    }

}
