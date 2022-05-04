package com.jian.transmit.handler.local;

import com.jian.transmit.handler.local.http.HeaderHostIndex;
import com.jian.transmit.handler.local.http.HttpParser;
import com.jian.transmit.handler.local.http.ParseAppendableCharSequence;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpConstants;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.util.internal.AppendableCharSequence;
import io.netty.util.internal.StringUtil;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

/***
 * http协议处理器
 * @author Jian
 * @date 2022/04/04
 */
public class LocalHttpByteToMessageDecoder extends MessageToMessageDecoder<ByteBuf> {
    public static final int DEFAULT_INITIAL_BUFFER_SIZE = 128;

    public static final int DEFAULT_MAX_INITIAL_LINE_LENGTH = 4096;

    private static final String EMPTY_VALUE = "";

    private STATE state = STATE.URL_START;

    private final HttpParser httpParser;

    private HttpHeaders headers;

    private final String host;

    private final Integer port;

    public LocalHttpByteToMessageDecoder(String host, Integer port) {
        this.host = host;
        this.port = port;
        this.httpParser = new HttpParser(new AppendableCharSequence(DEFAULT_INITIAL_BUFFER_SIZE), DEFAULT_MAX_INITIAL_LINE_LENGTH);
    }

    /***
     * 读取到的位置
     */
    private int headerEndIdx = -1;

    /***
     * 内容长度
     */
    private int contentLen = 0;

    /***
     * 内容已读取的长度
     */
    private int contentReadedLen = 0;

    enum STATE {
        URL_START,
        URL_END,

        HEADER_START,
        HEADER_END,

        CONTENT_READ
    }

    HeaderHostIndex headerHostIndex;

    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> list) {
        byteBuf.markReaderIndex();
        ByteBuf buffer = channelHandlerContext.alloc().buffer();
        switch (state) {
            case URL_START: {
                ParseAppendableCharSequence parseAppendableCharSequence = httpParser.parseReset(byteBuf);
                if (Objects.isNull(parseAppendableCharSequence)) {
                    return;
                }
                AppendableCharSequence line = parseAppendableCharSequence.getAppendableCharSequence();
                if (Objects.isNull(line)) {
                    return;
                }
                String[] initialLine = splitInitialLine(line);
                //如果读出来用空格分割的没有3个元素，则认为条件不满足，还需要再获得流如下协议 GET /home HTTP/1.1
                if (initialLine.length < 3) {
                    // Invalid initial line - ignore.
                    state = STATE.URL_START;
                    return;
                }
                //如果满足第一行，则开始读取头
                state = STATE.HEADER_START;
            }
            case HEADER_START: {
                headerHostIndex = new HeaderHostIndex();
                headers = readHeaders(byteBuf, headerHostIndex);
                if (Objects.isNull(headers)) {
                    return;
                }
                //标记header结束位置
                headerEndIdx = byteBuf.readerIndex();
                state = STATE.HEADER_END;
            }
            case HEADER_END: {
                String hostHeader = headers.get(HttpHeaderNames.HOST);
                //获取到host的header后,替换原Host
                if (!StringUtil.isNullOrEmpty(hostHeader)) {
                    //重置读入位置
                    byteBuf.resetReaderIndex();
                    //从0开始到host:结束位置
                    int hostIpEndIdx = headerHostIndex.getStartIndex() + 6;
                    ByteBuf byteBuf1 = byteBuf.readBytes(hostIpEndIdx);
                    buffer.writeBytes(byteBuf1);

                    //-------------------获取通道上绑定的C端连接的ip和端口,用于替换原Host中的地址
                    String remoteLocalHost = this.host + ":" + this.port;
                    byte[] bytes = remoteLocalHost.getBytes(StandardCharsets.UTF_8);
                    buffer.writeBytes(bytes);
                    buffer.writeByte(HttpConstants.CR);
                    buffer.writeByte(HttpConstants.LF);
                    //------------------获取通道上绑定的C端连接的ip和端口,用于替换原Host中的地址

                    //0开头到host结束位置-从0开始到host:结束位置 就为ip:端口长度 如 192.168.0.1:80
                    int hostIpLen = headerHostIndex.getEndIndex() - hostIpEndIdx;
                    //从原来读取中需要跳过该段位置
                    byteBuf.skipBytes(hostIpLen);

                    //当前读取到的位置
                    int thisReadIdx = byteBuf.readerIndex();
                    //header总结束位置-当前读取到的位置=剩余header的字节数
                    int lastHeaderLen = headerEndIdx - thisReadIdx;
                    //读取出剩余header
                    ByteBuf lastHeaderByteBuf = byteBuf.readBytes(lastHeaderLen);
                    //写入剩余header
                    buffer.writeBytes(lastHeaderByteBuf);
                }

                //获取是否包含Content-length,如果包含,则需要标记读取位置
                String contentLengthStr = headers.get(HttpHeaderNames.CONTENT_LENGTH);
                if (!StringUtil.isNullOrEmpty(contentLengthStr)) {
                    contentLen = Integer.parseInt(contentLengthStr);
                    if (contentLen > 0) {
                        //如果有内容长度,则需要标记内容读取长度
                        state = STATE.CONTENT_READ;
                    } else {
                        //如果为空或为0,则该次请求读取完成,则重新将该状态设置为初始
                        state = STATE.URL_START;
                        resetNow();
                        break;
                    }
                } else {
                    //如果为空或为0,则该次请求读取完成,则重新将该状态设置为初始
                    state = STATE.URL_START;
                    resetNow();
                    break;
                }
            }
            case CONTENT_READ: {
                //读取内容位置标记
                //获取当次buf剩余读取字节数
                int readableBytes1 = byteBuf.readableBytes();
                if (readableBytes1 > 0) {
                    //写入剩余内容
                    buffer.writeBytes(byteBuf);
                    contentReadedLen += readableBytes1;
                }
                //如果内容已读取数和内容长度一致,则认为该此请求已完成
                if (contentReadedLen == contentLen) {
                    state = STATE.URL_START;
                    resetNow();
                }
                break;
            }
        }
        list.add(buffer);
    }


    /***
     * 重置该次参数
     */
    public void resetNow() {
        headers = null;
        headerHostIndex = null;
        headerEndIdx = -1;
        contentLen = 0;
        contentReadedLen = 0;
    }


    private static String[] splitInitialLine(AppendableCharSequence sb) {
        int aStart;
        int aEnd;
        int bStart;
        int bEnd;
        int cStart;
        int cEnd;

        aStart = findNonSPLenient(sb, 0);
        aEnd = findSPLenient(sb, aStart);

        bStart = findNonSPLenient(sb, aEnd);
        bEnd = findSPLenient(sb, bStart);

        cStart = findNonSPLenient(sb, bEnd);
        cEnd = findEndOfString(sb);

        return new String[]{
                sb.subStringUnsafe(aStart, aEnd),
                sb.subStringUnsafe(bStart, bEnd),
                cStart < cEnd ? sb.subStringUnsafe(cStart, cEnd) : ""};
    }

    private static int findEndOfString(AppendableCharSequence sb) {
        for (int result = sb.length() - 1; result > 0; --result) {
            if (!Character.isWhitespace(sb.charAtUnsafe(result))) {
                return result + 1;
            }
        }
        return 0;
    }

    private static boolean isSPLenient(char c) {
        // See https://tools.ietf.org/html/rfc7230#section-3.5
        return c == ' ' || c == (char) 0x09 || c == (char) 0x0B || c == (char) 0x0C || c == (char) 0x0D;
    }

    private static int findSPLenient(AppendableCharSequence sb, int offset) {
        for (int result = offset; result < sb.length(); ++result) {
            if (isSPLenient(sb.charAtUnsafe(result))) {
                return result;
            }
        }
        return sb.length();
    }


    private static int findNonSPLenient(AppendableCharSequence sb, int offset) {
        for (int result = offset; result < sb.length(); ++result) {
            char c = sb.charAtUnsafe(result);
            // See https://tools.ietf.org/html/rfc7230#section-3.5
            if (isSPLenient(c)) {
                continue;
            }
            if (Character.isWhitespace(c)) {
                // Any other whitespace delimiter is invalid
                throw new IllegalArgumentException("Invalid separator");
            }
            return result;
        }
        return sb.length();
    }

    private String name;
    private String value;

    private HttpHeaders readHeaders(ByteBuf buffer, HeaderHostIndex headerHostIndex) {
        final HttpHeaders headers = new DefaultHttpHeaders();

        while (true) {
            ParseAppendableCharSequence parseAppendableCharSequence = httpParser.parseReset(buffer);
            if (Objects.isNull(parseAppendableCharSequence)) {
                return null;
            }
            AppendableCharSequence line = parseAppendableCharSequence.getAppendableCharSequence();
            if (Objects.isNull(line)) {
                return null;
            }
            if (line.length() == 0) {
                break;
            }
            splitHeader(line);
            if (Objects.nonNull(name)) {
                if (name.toLowerCase().equals(HttpHeaderNames.HOST.toString())) {
                    headerHostIndex.setStartIndex(parseAppendableCharSequence.getStartIndex());
                    headerHostIndex.setEndIndex(parseAppendableCharSequence.getEndIndex());
                }
                headers.add(name, value);
            }
        }
        return headers;
    }


    private void splitHeader(AppendableCharSequence sb) {
        final int length = sb.length();
        int nameStart;
        int nameEnd;
        int colonEnd;
        int valueStart;
        int valueEnd;

        nameStart = findNonWhitespace(sb, 0);
        for (nameEnd = nameStart; nameEnd < length; nameEnd++) {
            char ch = sb.charAtUnsafe(nameEnd);
            // https://tools.ietf.org/html/rfc7230#section-3.2.4
            //
            // No whitespace is allowed between the header field-name and colon. In
            // the past, differences in the handling of such whitespace have led to
            // security vulnerabilities in request routing and response handling. A
            // server MUST reject any received request message that contains
            // whitespace between a header field-name and colon with a response code
            // of 400 (Bad Request). A proxy MUST remove any such whitespace from a
            // response message before forwarding the message downstream.
            if (ch == ':' ||
                    // In case of decoding a request we will just continue processing and header validation
                    // is done in the DefaultHttpHeaders implementation.
                    //
                    // In the case of decoding a response we will "skip" the whitespace.
                    (!isDecodingRequest() && isOWS(ch))) {
                break;
            }
        }

        if (nameEnd == length) {
            // There was no colon present at all.
            throw new IllegalArgumentException("No colon found");
        }

        for (colonEnd = nameEnd; colonEnd < length; colonEnd++) {
            if (sb.charAtUnsafe(colonEnd) == ':') {
                colonEnd++;
                break;
            }
        }

        name = sb.subStringUnsafe(nameStart, nameEnd);

        valueStart = findNonWhitespace(sb, colonEnd);
        if (valueStart == length) {
            value = EMPTY_VALUE;
        } else {
            valueEnd = findEndOfString(sb);
            value = sb.subStringUnsafe(valueStart, valueEnd);
        }
    }


    private static int findNonWhitespace(AppendableCharSequence sb, int offset) {
        for (int result = offset; result < sb.length(); ++result) {
            char c = sb.charAtUnsafe(result);
            if (!Character.isWhitespace(c)) {
                return result;
            } else if (!isOWS(c)) {
                // Only OWS is supported for whitespace
                throw new IllegalArgumentException("Invalid separator, only a single space or horizontal tab allowed," +
                        " but received a '" + c + "' (0x" + Integer.toHexString(c) + ")");
            }
        }
        return sb.length();
    }

    private static boolean isOWS(char ch) {
        return ch == ' ' || ch == (char) 0x09;
    }

    protected boolean isDecodingRequest() {
        return true;
    }


}
