package com.jian.transmit.handler.local;

import com.jian.transmit.handler.local.http.HttpHeader;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.TooLongFrameException;
import io.netty.handler.codec.http.*;
import io.netty.util.AsciiString;
import io.netty.util.ByteProcessor;
import io.netty.util.internal.AppendableCharSequence;
import io.netty.util.internal.StringUtil;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;

import static io.netty.util.internal.ObjectUtil.checkPositive;

/***
 * HTTP协议解析处理器
 * @author Jian
 * @date 2022-05-13 重写
 */
@Slf4j
public abstract class LocalHttpBaseDecoder extends ByteToMessageDecoder {

    public static final int DEFAULT_MAX_INITIAL_LINE_LENGTH = 4096;
    public static final int DEFAULT_MAX_HEADER_SIZE = 8192;
    public static final boolean DEFAULT_CHUNKED_SUPPORTED = true;
    public static final boolean DEFAULT_VALIDATE_HEADERS = true;
    public static final int DEFAULT_INITIAL_BUFFER_SIZE = 128;
    public static final boolean DEFAULT_ALLOW_DUPLICATE_CONTENT_LENGTHS = false;

    private static final String EMPTY_VALUE = "";

    private final boolean chunkedSupported;
    protected final boolean validateHeaders;
    private final boolean allowDuplicateContentLengths;
    private final HeaderParser headerParser;
    private final LineParser lineParser;

    private HttpMessage message;
    private long chunkSize;
    private long contentLength = Long.MIN_VALUE;
    private volatile boolean resetRequested;

    // These will be updated by splitHeader(...)
//    private CharSequence name;
    //private CharSequence value;

    //private LastHttpContent trailer;

    /***
     * 被穿透机器host
     */
    private final String host;

    /***
     * 被穿透机器端口
     */
    private final Integer port;

    /**
     * The internal state of {@link HttpObjectDecoder}.
     * <em>Internal use only</em>.
     */
    private enum State {
        SKIP_CONTROL_CHARS,
        READ_INITIAL,
        READ_HEADER,
        READ_VARIABLE_LENGTH_CONTENT,
        READ_FIXED_LENGTH_CONTENT,
        READ_CHUNK_SIZE,
        READ_CHUNKED_CONTENT,
        READ_CHUNK_DELIMITER,
        READ_CHUNK_FOOTER,
        BAD_MESSAGE,
        UPGRADED
    }

    private State reqState = State.SKIP_CONTROL_CHARS;

    private final boolean TRUE = true;

    private final boolean FALSE = false;

    /**
     * Creates a new instance with the default
     * {@code maxInitialLineLength (4096}}, {@code maxHeaderSize (8192)}, and
     * {@code maxChunkSize (8192)}.
     */
    protected LocalHttpBaseDecoder(String host, Integer port) {
        this(host, port, DEFAULT_MAX_INITIAL_LINE_LENGTH, DEFAULT_MAX_HEADER_SIZE, DEFAULT_CHUNKED_SUPPORTED);
        //setCumulator(COMPOSITE_CUMULATOR);
    }

    /**
     * Creates a new instance with the specified parameters.
     */
    protected LocalHttpBaseDecoder(String host, Integer port, int maxInitialLineLength, int maxHeaderSize, boolean chunkedSupported) {
        this(host, port, maxInitialLineLength, maxHeaderSize, chunkedSupported, DEFAULT_VALIDATE_HEADERS);
    }

    /**
     * Creates a new instance with the specified parameters.
     */
    protected LocalHttpBaseDecoder(String host, Integer port, int maxInitialLineLength, int maxHeaderSize, boolean chunkedSupported, boolean validateHeaders) {
        this(host, port, maxInitialLineLength, maxHeaderSize, chunkedSupported, validateHeaders, DEFAULT_INITIAL_BUFFER_SIZE);
    }

    /**
     * Creates a new instance with the specified parameters.
     */
    protected LocalHttpBaseDecoder(String host, Integer port, int maxInitialLineLength, int maxHeaderSize, boolean chunkedSupported, boolean validateHeaders, int initialBufferSize) {
        this(host, port, maxInitialLineLength, maxHeaderSize, chunkedSupported, validateHeaders, initialBufferSize, DEFAULT_ALLOW_DUPLICATE_CONTENT_LENGTHS);
    }

    public abstract String getReqHost();

    public abstract void setReqHost(String reqHost);

    /**
     * Creates a new instance with the specified parameters.
     */
    protected LocalHttpBaseDecoder(String host, Integer port, int maxInitialLineLength, int maxHeaderSize, boolean chunkedSupported, boolean validateHeaders, int initialBufferSize, boolean allowDuplicateContentLengths) {
        checkPositive(maxInitialLineLength, "maxInitialLineLength");
        checkPositive(maxHeaderSize, "maxHeaderSize");

        AppendableCharSequence seq = new AppendableCharSequence(initialBufferSize);
        this.lineParser = new LineParser(seq, maxInitialLineLength);
        this.headerParser = new HeaderParser(seq, maxHeaderSize);
        this.chunkedSupported = chunkedSupported;
        this.validateHeaders = validateHeaders;
        this.allowDuplicateContentLengths = allowDuplicateContentLengths;
        this.host = host;
        this.port = port;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf buffer, List<Object> outs) {
        //ByteBuf buf = ctx.alloc().buffer();
        if (resetRequested) {
            resetNow();
        }

        switch (reqState) {
            case SKIP_CONTROL_CHARS:
                // Fall-through
            case READ_INITIAL:
                Boolean isContinue_r = readInitial(buffer, outs);
                if (!isContinue_r) {
                    return;
                }
            case READ_HEADER:
                Boolean isContinue_h = readHeader(ctx, buffer, outs);
                if (!isContinue_h) {
                    return;
                }
            case READ_VARIABLE_LENGTH_CONTENT: {
                Boolean isContinue_rvlc = readVariableLengthContent(buffer, outs);
                if (!isContinue_rvlc) {
                    return;
                }
            }
            case READ_FIXED_LENGTH_CONTENT: {
                Boolean isContinue_rflc = readFixedLengthContent(buffer, outs);
                if (!isContinue_rflc) {
                    return;
                }
            }
            /**
             * everything else after this point takes care of reading chunked content. basically, read chunk size,
             * read chunk, read and ignore the CRLF and repeat until 0
             */
            case READ_CHUNK_SIZE:
                Boolean isContinue_rcs = readChunkSize(ctx, buffer, outs);
                if (!isContinue_rcs) {
                    return;
                }
            case READ_CHUNKED_CONTENT: {
                Boolean isContinue_rcc = readChunkContent(ctx, buffer, outs);
                if (!isContinue_rcc) {
                    return;
                }
            }
            case READ_CHUNK_DELIMITER: {
                Boolean isContinue_rcd = readChunkDelimiter(ctx, buffer, outs);
                if (Objects.isNull(isContinue_rcd)) {
                    break;
                } else if (!isContinue_rcd) {
                    return;
                }
            }
            case READ_CHUNK_FOOTER:
                Boolean isContinue_rcf = readChunkFooter(buffer, outs);
                if (!isContinue_rcf) {
                    return;
                }
            case BAD_MESSAGE: {
                Boolean isContinue_bm = badMessage(buffer);
                if (Objects.isNull(isContinue_bm)) {
                    break;
                }
            }
            case UPGRADED: {
                Boolean isContinue_up = upgraded(buffer, outs);
                if (Objects.isNull(isContinue_up)) {
                    break;
                }
            }
            default:
                break;
        }
    }


    private Boolean readInitial(ByteBuf buffer, List<Object> outs) {
        try {
            int ptlStartIdx = buffer.readerIndex();
            AppendableCharSequence line = lineParser.parse(buffer);
            if (line == null) {
                return FALSE;
            }
            String[] initialLine = splitInitialLine(line);
            if (initialLine.length < 3) {
                // Invalid initial line - ignore.
                reqState = State.SKIP_CONTROL_CHARS;
                return FALSE;
            }
            int ptlEndIdx = buffer.readerIndex();
            message = createMessage(initialLine);
            reqState = State.READ_HEADER;
            buffer.readerIndex(ptlStartIdx);
            int ptlReadLen = ptlEndIdx - ptlStartIdx;
            outs.add(buffer.readRetainedSlice(ptlReadLen));
            return TRUE;
            // fall-through
        } catch (Exception e) {
            invalidMessage(buffer);
            return FALSE;
        }
    }

    private Boolean readHeader(ChannelHandlerContext ctx, ByteBuf buffer, List<Object> outs) {
        try {
            State nextState = readHeaders(buffer, ctx, outs);
            if (nextState == null) {
                return FALSE;
            }
            reqState = nextState;
            switch (nextState) {
                case SKIP_CONTROL_CHARS -> {
                    // fast-path
                    // No content is expected.
                    //out.add(reqMessage);
                    //out.add(LastHttpContent.EMPTY_LAST_CONTENT);
                    resetNow();
                    return FALSE;
                }
                case READ_CHUNK_SIZE -> {
                    if (!chunkedSupported) {
                        throw new IllegalArgumentException("Chunked messages not supported");
                    }
                    readChunkSize(ctx, buffer, outs);
                    // Chunked encoding - generate HttpMessage first.  HttpChunks will follow.
                    //out.add(reqMessage);
                    return FALSE;
                }
                default -> {
                    /**
                     * <a href="https://tools.ietf.org/html/rfc7230#section-3.3.3">RFC 7230, 3.3.3</a> states that if a
                     * request does not have either a transfer-encoding or a content-length header then the message body
                     * length is 0. However for a response the body length is the number of octets received prior to the
                     * server closing the connection. So we treat this as variable length chunked encoding.
                     */
                    long contentLength = contentLength();
                    if (contentLength == 0 || contentLength == -1 && isDecodingRequest()) {
                        //out.add(reqMessage);
                        //out.add(LastHttpContent.EMPTY_LAST_CONTENT);
                        resetNow();
                        return FALSE;
                    }
                    assert nextState == State.READ_FIXED_LENGTH_CONTENT || nextState == State.READ_VARIABLE_LENGTH_CONTENT;

                    //out.add(reqMessage);

                    if (nextState == State.READ_FIXED_LENGTH_CONTENT) {
                        // chunkSize will be decreased as the READ_FIXED_LENGTH_CONTENT state reads data chunk by chunk.
                        chunkSize = contentLength;
                        readFixedLengthContent(buffer, outs);
                    } else {
                        readVariableLengthContent(buffer, outs);
                    }
                    // We return here, this forces decode to be called again where we will decode the content
                    return FALSE;
                }
            }
        } catch (Exception e) {
            invalidMessage(buffer);
            return FALSE;
        }
    }

    private Boolean readVariableLengthContent(ByteBuf buffer, List<Object> outs) {
        // Keep reading data as a chunk until the end of connection is reached.
        /*int toRead = Math.min(buffer.readableBytes(), maxChunkSize);
        if (toRead > 0) {
            ByteBuf content = buffer.readRetainedSlice(toRead);
            out.add(new DefaultHttpContent(content));
        }*/
        int varReadableLen = buffer.readableBytes();
        if (varReadableLen == 0) {
            return FALSE;
        }
        outs.add(buffer.readRetainedSlice(varReadableLen));
        return FALSE;
    }

    private Boolean readFixedLengthContent(ByteBuf buffer, List<Object> outs) {

        Boolean isContinueRcc = readContent(buffer, outs);
        if (!isContinueRcc) {
            return FALSE;
        }

        if (chunkSize == 0) {
            // Read all content.
            //out.add(new DefaultLastHttpContent(content, validateHeaders));
            resetNow();
        } else {
            //out.add(new DefaultHttpContent(content));
        }
        return FALSE;
    }

    private Boolean readChunkSize(ChannelHandlerContext ctx, ByteBuf buffer, List<Object> outs) {
        try {
            int chunkSizeStartIdx = buffer.readerIndex();
            AppendableCharSequence line = lineParser.parse(buffer);
            if (line == null) {
                return FALSE;
            }
            int chunkSize = getChunkSize(line.toString());
            int chunkSizeEndIdx = buffer.readerIndex();
            int chunkSizeLen = chunkSizeEndIdx - chunkSizeStartIdx;
            buffer.readerIndex(chunkSizeStartIdx);
            outs.add(buffer.readRetainedSlice(chunkSizeLen));
            buffer.readerIndex(chunkSizeEndIdx);

            this.chunkSize = chunkSize;
            if (chunkSize == 0) {
                reqState = State.READ_CHUNK_FOOTER;
                readChunkFooter(buffer, outs);
                return FALSE;
            }
            reqState = State.READ_CHUNKED_CONTENT;
            readChunkContent(ctx, buffer, outs);
            return FALSE;
            // fall-through
        } catch (Exception e) {
            //out.add(invalidChunk(buffer, e));
            invalidChunk(buffer);
            return FALSE;
        }
    }

    private Boolean readChunkContent(ChannelHandlerContext ctx, ByteBuf buffer, List<Object> outs) {
        assert chunkSize <= Integer.MAX_VALUE;

        Boolean isContinueRcc = readContent(buffer, outs);
        if (!isContinueRcc) {
            return FALSE;
        }

        if (chunkSize != 0) {
            return FALSE;
        }
        reqState = State.READ_CHUNK_DELIMITER;
        readChunkDelimiter(ctx, buffer, outs);
        return FALSE;
        // fall-through
    }


    private Boolean readContent(ByteBuf buffer, List<Object> outs) {
        int toRead = buffer.readableBytes();
        if (toRead == 0) {
            return FALSE;
        }

        if (toRead > chunkSize) {
            toRead = (int) chunkSize;
        }

        ByteBuf content = buffer.readRetainedSlice(toRead);
        chunkSize -= toRead;
        outs.add(content);
        return TRUE;
    }


    private Boolean readChunkDelimiter(ChannelHandlerContext ctx, ByteBuf buffer, List<Object> outs) {
        final int wIdx = buffer.writerIndex();
        ByteBuf buf = ctx.alloc().buffer();
        int rIdx = buffer.readerIndex();
        while (wIdx > rIdx) {
            byte next = buffer.getByte(rIdx++);
            buf.writeByte(next);
            if (next == HttpConstants.LF) {
                outs.add(buf);
                reqState = State.READ_CHUNK_SIZE;
                break;
            }
        }
        buffer.readerIndex(rIdx);
        readChunkSize(ctx, buffer, outs);
        return FALSE;
    }

    private Boolean readChunkFooter(ByteBuf buffer, List<Object> outs) {
        try {
            /*LastHttpContent trailer = readTrailingHeaders(buffer);
            if (trailer == null) {
                return;
            }
            out.add(trailer);*/
            int i = buffer.readableBytes();
            if (i < 2) {
                return FALSE;
            }
            outs.add(buffer.readRetainedSlice(2));
            resetNow();
            return FALSE;
        } catch (Exception e) {
            //out.add(invalidChunk(buffer, e));
            invalidChunk(buffer);
            return FALSE;
        }
    }

    private Boolean upgraded(ByteBuf buffer, List<Object> outs) {
        int readableBytes = buffer.readableBytes();
        if (readableBytes > 0) {
            // Keep on consuming as otherwise we may trigger an DecoderException,
            // other handler will replace this codec with the upgraded protocol codec to
            // take the traffic over at some point then.
            // See https://github.com/netty/netty/issues/2173
            //out.add(buffer.readBytes(readableBytes));
            //buf.writeBytes(buffer);
            outs.add(buffer.readRetainedSlice(readableBytes));
        }
        return null;
    }

    private Boolean badMessage(ByteBuf buffer) {
        // Keep discarding until disconnection.
        buffer.skipBytes(buffer.readableBytes());
        return null;
    }


    @Override

    protected void decodeLast(ChannelHandlerContext ctx, ByteBuf in, List<Object> outs) throws Exception {
        super.decodeLast(ctx, in, outs);

        if (resetRequested) {
            // If a reset was requested by decodeLast() we need to do it now otherwise we may produce a
            // LastHttpContent while there was already one.
            resetNow();
        }
        // Handle the last unfinished message.
        if (message != null) {
            boolean chunked = HttpUtil.isTransferEncodingChunked(message);
            if (reqState == State.READ_VARIABLE_LENGTH_CONTENT && !in.isReadable() && !chunked) {
                // End of connection.
                //out.add(LastHttpContent.EMPTY_LAST_CONTENT);
                resetNow();
                return;
            }

            if (reqState == State.READ_HEADER) {
                // If we are still in the state of reading headers we need to create a new invalid message that
                // signals that the connection was closed before we received the headers.
                //out.add(invalidMessage(Unpooled.EMPTY_BUFFER, new PrematureChannelClosureException("Connection closed before received headers")));
                resetNow();
                return;
            }

            /*
            // Check if the closure of the connection signifies the end of the content.
            boolean prematureClosure;
            if (isDecodingRequest() || chunked) {
                // The last request did not wait for a response.
                prematureClosure = true;
            } else {
                // Compare the length of the received content and the 'Content-Length' header.
                // If the 'Content-Length' header is absent, the length of the content is determined by the end of the
                // connection, so it is perfectly fine.
                prematureClosure = contentLength() > 0;
            }

            if (!prematureClosure) {
                //out.add(LastHttpContent.EMPTY_LAST_CONTENT);
            }*/
            resetNow();
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof HttpExpectationFailedEvent) {
            switch (reqState) {
                case READ_FIXED_LENGTH_CONTENT, READ_VARIABLE_LENGTH_CONTENT, READ_CHUNK_SIZE -> reset();
                default -> {
                }
            }
        }
        super.userEventTriggered(ctx, evt);
    }

    protected boolean isContentAlwaysEmpty(HttpMessage msg) {
        if (msg instanceof HttpResponse res) {
            int code = res.status().code();

            // Correctly handle return codes of 1xx.
            //
            // See:
            //     - https://www.w3.org/Protocols/rfc2616/rfc2616-sec4.html Section 4.4
            //     - https://github.com/netty/netty/issues/222
            if (code >= 100 && code < 200) {
                // One exception: Hixie 76 websocket handshake response
                return !(code == 101 && !res.headers().contains(HttpHeaderNames.SEC_WEBSOCKET_ACCEPT) && res.headers().contains(HttpHeaderNames.UPGRADE, HttpHeaderValues.WEBSOCKET, true));
            }

            return switch (code) {
                case 204, 304 -> true;
                default -> false;
            };
        }
        return false;
    }

    /**
     * Returns true if the server switched to a different protocol than HTTP/1.0 or HTTP/1.1, e.g. HTTP/2 or Websocket.
     * Returns false if the upgrade happened in a different layer, e.g. upgrade from HTTP/1.1 to HTTP/1.1 over TLS.
     */
    protected boolean isSwitchingToNonHttp1Protocol(HttpResponse msg) {
        if (msg.status().code() != HttpResponseStatus.SWITCHING_PROTOCOLS.code()) {
            return false;
        }
        String newProtocol = msg.headers().get(HttpHeaderNames.UPGRADE);
        return newProtocol == null || !newProtocol.contains(HttpVersion.HTTP_1_0.text()) && !newProtocol.contains(HttpVersion.HTTP_1_1.text());
    }

    /**
     * Resets the state of the decoder so that it is ready to decode a new message.
     * This method is useful for handling a rejected request with {@code Expect: 100-continue} header.
     */
    public void reset() {
        resetRequested = true;
    }

    private void resetNow() {
        HttpMessage message = this.message;
        this.message = null;
        contentLength = Long.MIN_VALUE;
        lineParser.reset();
        headerParser.reset();
        if (!isDecodingRequest()) {
            HttpResponse res = (HttpResponse) message;
            if (res != null && isSwitchingToNonHttp1Protocol(res)) {
                reqState = State.UPGRADED;
                return;
            }
        }

        resetRequested = false;
        reqState = State.SKIP_CONTROL_CHARS;
    }

    private void invalidMessage(ByteBuf in) {
        reqState = State.BAD_MESSAGE;

        // Advance the readerIndex so that ByteToMessageDecoder does not complain
        // when we produced an invalid message without consuming anything.
        in.skipBytes(in.readableBytes());
        message = null;
    }

    private void invalidChunk(ByteBuf in) {
        reqState = State.BAD_MESSAGE;

        // Advance the readerIndex so that ByteToMessageDecoder does not complain
        // when we produced an invalid message without consuming anything.
        in.skipBytes(in.readableBytes());

        /*HttpContent chunk = new DefaultLastHttpContent(Unpooled.EMPTY_BUFFER);
        chunk.setDecoderResult(DecoderResult.failure(cause));*/
        message = null;
    }

    //保存比host先解析出需替换url的heander
    List<HttpHeader> notAppendHeaders;

    private State readHeaders(ByteBuf buffer, ChannelHandlerContext ctx, List<Object> outs) {
        ByteBuf buf = ctx.alloc().buffer();
        final HttpMessage message = this.message;
        final HttpHeaders headers = message.headers();
//--------------
        /*AppendableCharSequence line = headerParser.parse(buffer);
        if (line == null) {
            return null;
        }
        if (line.length() > 0) {
            do {
                char firstChar = line.charAtUnsafe(0);
                if (name != null && (firstChar == ' ' || firstChar == '\t')) {
                    //please do not make one line from below code
                    //as it breaks +XX:OptimizeStringConcat optimization
                    String trimmedLine = line.toString().trim();
                    String valueStr = String.valueOf(value);
                    value = valueStr + ' ' + trimmedLine;
                } else {
                    if (name != null) {
                        headers.add(name, value);
                    }
                    splitHeader(line);
                }

                line = headerParser.parse(buffer);
                if (line == null) {
                    return null;
                }
            } while (line.length() > 0);
        }

        // Add the last header.
        if (name != null) {
            headers.add(name, value);
        }*/
//---------------------
        int startIdx;
        int endIdx;
        while (true) {
            //记录header读取开始位置
            startIdx = buffer.readerIndex();
            AppendableCharSequence line = headerParser.parse(buffer);
            if (Objects.isNull(line)) {
                //退出解析header时将已解析的header发出
                if (buf.isReadable()) {
                    outs.add(buf);
                } else {
                    buf.release();
                }
                return null;
            }
            //记录header读取结束位置
            endIdx = buffer.readerIndex();
            int len = line.length();
            if (len == 0) {
                //退出解析header时将已解析的header发出
                if (buf.isReadable()) {
                    outs.add(buf);
                } else {
                    buf.release();
                }
                //读取写入header结束符
                int readLen = endIdx - startIdx;
                buffer.readerIndex(startIdx);
                outs.add(buffer.readRetainedSlice(readLen));
                buffer.readerIndex(endIdx);
                notAppendHeaders = null;
                break;
            }
            if (len > 0) {
                HttpHeader header = splitHeader(line);
                String name = header.getName();
                String value = header.getValue();
                char firstChar = line.charAtUnsafe(0);
                if (name != null && (firstChar == ' ' || firstChar == '\t')) {
                    //please do not make one line from below code
                    //as it breaks +XX:OptimizeStringConcat optimization
                    /*String trimmedLine = line.toString().trim();
                    String valueStr = String.valueOf(value);
                    value = valueStr + ' ' + trimmedLine;*/
                } else {
                    if (Objects.nonNull(name)) {
                        headers.add(name, value);
                        if (HttpHeaderNames.HOST.contentEqualsIgnoreCase(name)) {
                            setReqHost(value);
                            replaceHost(buf);

                            //判断是否有在host先解析且需要被替换url的header，如果存在，则解析完host时需要再解析那些待解析的hander
                            if (Objects.nonNull(notAppendHeaders) && !notAppendHeaders.isEmpty()) {
                                ListIterator<HttpHeader> httpHeaderListIterator = notAppendHeaders.listIterator();
                                while (httpHeaderListIterator.hasNext()) {
                                    HttpHeader httpHeader = httpHeaderListIterator.next();
                                    AsciiString headerName = httpHeader.getHeaderName();
                                    if (headerName.equals(HttpHeaderNames.REFERER)) {
                                        replaceReferer(buf, value);
                                    } else if (headerName.equals(HttpHeaderNames.LOCATION)) {
                                        replaceLocation(buf, value);
                                    } else if (headerName.equals(HttpHeaderNames.ORIGIN)) {
                                        replaceOrigin(buf, value);
                                    }
                                    httpHeaderListIterator.remove();
                                }
                            }
                        } else if (HttpHeaderNames.REFERER.contentEqualsIgnoreCase(name)) {
                            //判断是否已解析过host,如果未解析，则添加到集合中等待host解析完后再解析该header
                            if (StringUtil.isNullOrEmpty(getReqHost())) {
                                if (Objects.isNull(notAppendHeaders)) {
                                    notAppendHeaders = new LinkedList<>();
                                }
                                notAppendHeaders.add(new HttpHeader(HttpHeaderNames.REFERER, value));
                            } else {
                                replaceReferer(buf, value);
                            }
                        } else if (HttpHeaderNames.LOCATION.contentEqualsIgnoreCase(name)) {
                            //判断是否已解析过host,如果未解析，则添加到集合中等待host解析完后再解析该header
                            if (StringUtil.isNullOrEmpty(getReqHost())) {
                                if (Objects.isNull(notAppendHeaders)) {
                                    notAppendHeaders = new LinkedList<>();
                                }
                                notAppendHeaders.add(new HttpHeader(HttpHeaderNames.LOCATION, value));
                            } else {
                                replaceLocation(buf, value);
                            }
                        } else if (HttpHeaderNames.ORIGIN.contentEqualsIgnoreCase(name)) {
                            //判断是否已解析过host,如果未解析，则添加到集合中等待host解析完后再解析该header
                            if (StringUtil.isNullOrEmpty(getReqHost())) {
                                if (Objects.isNull(notAppendHeaders)) {
                                    notAppendHeaders = new LinkedList<>();
                                }
                                notAppendHeaders.add(new HttpHeader(HttpHeaderNames.ORIGIN, value));
                            } else {
                                replaceOrigin(buf, value);
                            }
                        } else {
                            int readLen = endIdx - startIdx;
                            buffer.readerIndex(startIdx);
                            outs.add(buffer.readRetainedSlice(readLen));
                            buffer.readerIndex(endIdx);
                        }
                    }
                }
            }
        }


        List<String> contentLengthFields = headers.getAll(HttpHeaderNames.CONTENT_LENGTH);
        if (!contentLengthFields.isEmpty()) {
            HttpVersion version = message.protocolVersion();
            boolean isHttp10OrEarlier = version.majorVersion() < 1 || (version.majorVersion() == 1 && version.minorVersion() == 0);
            // Guard against multiple Content-Length headers as stated in
            // https://tools.ietf.org/html/rfc7230#section-3.3.2:
            contentLength = HttpUtil.normalizeAndGetContentLength(contentLengthFields, isHttp10OrEarlier, allowDuplicateContentLengths);
            if (contentLength != -1) {
                headers.set(HttpHeaderNames.CONTENT_LENGTH, contentLength);
            }
        }

        if (isContentAlwaysEmpty(message)) {
            HttpUtil.setTransferEncodingChunked(message, false);
            return State.SKIP_CONTROL_CHARS;
        } else if (HttpUtil.isTransferEncodingChunked(message)) {
            if (!contentLengthFields.isEmpty() && message.protocolVersion() == HttpVersion.HTTP_1_1) {
                handleTransferEncodingChunkedWithContentLength(message);
            }
            return State.READ_CHUNK_SIZE;
        } else if (contentLength() >= 0) {
            return State.READ_FIXED_LENGTH_CONTENT;
        } else {
            return State.READ_VARIABLE_LENGTH_CONTENT;
        }
    }


    /***
     * 替换host
     * @param buf 替换后新的buffer
     */
    public void replaceHost(ByteBuf buf) {
        //获取到host的header后,替换原Host
        byte[] nameBytes = HttpHeaderNames.HOST.array();
        //从0开始到host: 结束位置
        buf.writeBytes(nameBytes);
        buf.writeByte(HttpConstants.COLON);
        buf.writeByte(HttpConstants.SP);
        //-------------------获取通道上绑定的C端连接的ip和端口,用于替换原Host中的地址
        String remoteLocalHost = this.host + ":" + this.port;
        byte[] bytes = remoteLocalHost.getBytes(StandardCharsets.UTF_8);
        buf.writeBytes(bytes);
        buf.writeByte(HttpConstants.CR);
        buf.writeByte(HttpConstants.LF);
        //------------------获取通道上绑定的C端连接的ip和端口,用于替换原Host中的地址
    }

    /***
     * 替换Origin
     * @param buf 替换后的buffer
     * @param value header值
     */
    private void replaceOrigin(ByteBuf buf, String value) {
        int bufWriteIdx = buf.writerIndex();
        byte[] nameBytes = HttpHeaderNames.ORIGIN.array();
        //获取到host的header后,替换原Location
        try {
            //添加header名
            if (appendHeaderNames(buf, nameBytes, value)) return;
            String localHost = this.host + ":" + this.port;
            commonReplaceUrl(buf, value, getReqHost(), localHost);
        } catch (Exception e) {
            log.error("Origin URL处理错误！{}", e.getMessage());
            buf.writerIndex(bufWriteIdx);
            appendHeader(buf, nameBytes, value);
        }
    }


    /***
     * 替换referer
     * @param buf 替换后新的buffer
     * @param value header值
     */
    public void replaceReferer(ByteBuf buf, String value) {
        int bufWriteIdx = buf.writerIndex();
        byte[] nameBytes = HttpHeaderNames.REFERER.array();
        try {
            //从0开始到referer: 结束位置
            if (appendHeaderNames(buf, nameBytes, value)) return;
            URI uri = new URI(value);
            String rawAuthority = uri.getRawAuthority();
            //如果host和原host一致，则替换掉host为被穿透地址的host
            if (!StringUtil.isNullOrEmpty(rawAuthority) && rawAuthority.equalsIgnoreCase(getReqHost())) {
                String localHost = this.host + ":" + this.port;
                joinUrl(buf, uri, localHost);
            } else {
                throw new IllegalStateException("匹配条件不满足抛出异常，可忽略！");
            }
        } catch (Exception e) {
            log.error("Referer URL处理错误！{}", e.getMessage());
            buf.writerIndex(bufWriteIdx);
            appendHeader(buf, nameBytes, value);
        }
    }

    /***
     * 添加header
     * @param buf buf
     * @param nameBytes header名bytes
     * @param val header值
     */
    private void appendHeader(ByteBuf buf, byte[] nameBytes, String val) {
        buf.writeBytes(nameBytes);
        buf.writeByte(HttpConstants.COLON);
        buf.writeByte(HttpConstants.SP);
        byte[] valBytes = val.getBytes(StandardCharsets.UTF_8);
        buf.writeBytes(valBytes);
        buf.writeByte(HttpConstants.CR);
        buf.writeByte(HttpConstants.LF);
    }

    /***
     * 添加header名，如果默认值为空，则直接结束添加header
     * @param buf buf
     * @param nameBytes header名
     * @param value header值
     * @return
     */
    private boolean appendHeaderNames(ByteBuf buf, byte[] nameBytes, String value) {
        buf.writeBytes(nameBytes);
        buf.writeByte(HttpConstants.COLON);
        buf.writeByte(HttpConstants.SP);
        if (StringUtil.isNullOrEmpty(value)) {
            buf.writeByte(HttpConstants.CR);
            buf.writeByte(HttpConstants.LF);
            return true;
        }
        return false;
    }

    private void joinUrl(ByteBuf buf, URI uri, String rHost) {
        String path = uri.getRawPath();
        String query = uri.getRawQuery();
        StringBuilder urlBuilfer = new StringBuilder();
        urlBuilfer.append(uri.getScheme());
        urlBuilfer.append("://");
        urlBuilfer.append(rHost);
        if (!StringUtil.isNullOrEmpty(path)) {
            urlBuilfer.append(path);
        }
        if (!StringUtil.isNullOrEmpty(query)) {
            urlBuilfer.append("?");
            urlBuilfer.append(query);
        }
        byte[] urlBytes = urlBuilfer.toString().getBytes(StandardCharsets.UTF_8);
        buf.writeBytes(urlBytes);
        buf.writeByte(HttpConstants.CR);
        buf.writeByte(HttpConstants.LF);
    }


    /***
     * 替换location
     * @param buf 替换后新的buffer
     * @param value header值
     */
    public void replaceLocation(ByteBuf buf, String value) {
        int bufWriteIdx = buf.writerIndex();
        byte[] nameBytes = HttpHeaderNames.LOCATION.array();
        //获取到host的header后,替换原Location
        try {
            //从0开始到location: 结束位置
            if (appendHeaderNames(buf, nameBytes, value)) return;
            String localHost = this.host + ":" + this.port;
            commonReplaceUrl(buf, value, localHost, getReqHost());
        } catch (Exception e) {
            log.error("Location URL处理错误！{}", e.getMessage());
            buf.writerIndex(bufWriteIdx);
            appendHeader(buf, nameBytes, value);
        }
    }

    /***
     * 替换host
     * @param buf buffer
     * @param value 原buffer
     * @param host 与该host匹配
     * @param rHost 替换为新的host
     * @throws Exception 处理异常
     */
    public void commonReplaceUrl(ByteBuf buf, String value, String host, String rHost) throws Exception {
        if (!StringUtil.isNullOrEmpty(value)) {
            URI uri = new URI(value);
            String rawAuthority = uri.getRawAuthority();
            //如果host和原host一致，则替换掉host为被穿透地址的host
            if (!StringUtil.isNullOrEmpty(rawAuthority) && rawAuthority.equalsIgnoreCase(host)) {
                joinUrl(buf, uri, rHost);
            } else {
                throw new IllegalStateException("匹配条件不满足抛出异常，可忽略！");
            }
        }
    }


    /**
     * Invoked when a message with both a "Transfer-Encoding: chunked" and a "Content-Length" header field is detected.
     * The default behavior is to <i>remove</i> the Content-Length field, but this method could be overridden
     * to change the behavior (to, e.g., throw an exception and produce an invalid message).
     * <p>
     * See: <a href="https://tools.ietf.org/html/rfc7230#section-3.3.3">https://tools.ietf.org/html/rfc7230#section-3.3.3</a>
     * <pre>
     *     If a message is received with both a Transfer-Encoding and a
     *     Content-Length header field, the Transfer-Encoding overrides the
     *     Content-Length.  Such a message might indicate an attempt to
     *     perform request smuggling (Section 9.5) or response splitting
     *     (Section 9.4) and ought to be handled as an error.  A sender MUST
     *     remove the received Content-Length field prior to forwarding such
     *     a message downstream.
     * </pre>
     * Also see:
     * <a href="https://github.com/apache/tomcat/blob/b693d7c1981fa7f51e58bc8c8e72e3fe80b7b773/">https://github.com/apache/tomcat/blob/b693d7c1981fa7f51e58bc8c8e72e3fe80b7b773/</a>
     * java/org/apache/coyote/http11/Http11Processor.java#L747-L755
     * <a href="https://github.com/nginx/nginx/blob/0ad4393e30c119d250415cb769e3d8bc8dce5186/">https://github.com/nginx/nginx/blob/0ad4393e30c119d250415cb769e3d8bc8dce5186/</a>
     * src/http/ngx_http_request.c#L1946-L1953
     */
    protected void handleTransferEncodingChunkedWithContentLength(HttpMessage message) {
        message.headers().remove(HttpHeaderNames.CONTENT_LENGTH);
        contentLength = Long.MIN_VALUE;
    }

    private long contentLength() {
        if (contentLength == Long.MIN_VALUE) {
            contentLength = HttpUtil.getContentLength(message, -1L);
        }
        return contentLength;
    }


    protected abstract boolean isDecodingRequest();

    protected abstract HttpMessage createMessage(String[] initialLine) throws Exception;

    private static int getChunkSize(String hex) {
        hex = hex.trim();
        for (int i = 0; i < hex.length(); i++) {
            char c = hex.charAt(i);
            if (c == ';' || Character.isWhitespace(c) || Character.isISOControl(c)) {
                hex = hex.substring(0, i);
                break;
            }
        }

        return Integer.parseInt(hex, 16);
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

        return new String[]{sb.subStringUnsafe(aStart, aEnd), sb.subStringUnsafe(bStart, bEnd), cStart < cEnd ? sb.subStringUnsafe(cStart, cEnd) : ""};
    }

    private HttpHeader splitHeader(AppendableCharSequence sb) {
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

        String name = sb.subStringUnsafe(nameStart, nameEnd);
        String value;
        valueStart = findNonWhitespace(sb, colonEnd);
        if (valueStart == length) {
            value = EMPTY_VALUE;
        } else {
            valueEnd = findEndOfString(sb);
            value = sb.subStringUnsafe(valueStart, valueEnd);
        }
        return new HttpHeader(name, value);
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

    private static int findSPLenient(AppendableCharSequence sb, int offset) {
        for (int result = offset; result < sb.length(); ++result) {
            if (isSPLenient(sb.charAtUnsafe(result))) {
                return result;
            }
        }
        return sb.length();
    }

    private static boolean isSPLenient(char c) {
        // See https://tools.ietf.org/html/rfc7230#section-3.5
        return c == ' ' || c == (char) 0x09 || c == (char) 0x0B || c == (char) 0x0C || c == (char) 0x0D;
    }

    private static int findNonWhitespace(AppendableCharSequence sb, int offset) {
        for (int result = offset; result < sb.length(); ++result) {
            char c = sb.charAtUnsafe(result);
            if (!Character.isWhitespace(c)) {
                return result;
            } else if (!isOWS(c)) {
                // Only OWS is supported for whitespace
                throw new IllegalArgumentException("Invalid separator, only a single space or horizontal tab allowed," + " but received a '" + c + "' (0x" + Integer.toHexString(c) + ")");
            }
        }
        return sb.length();
    }

    private static int findEndOfString(AppendableCharSequence sb) {
        for (int result = sb.length() - 1; result > 0; --result) {
            if (!Character.isWhitespace(sb.charAtUnsafe(result))) {
                return result + 1;
            }
        }
        return 0;
    }

    private static boolean isOWS(char ch) {
        return ch == ' ' || ch == (char) 0x09;
    }

    private static class HeaderParser implements ByteProcessor {
        private final AppendableCharSequence seq;
        private final int maxLength;
        int size;

        HeaderParser(AppendableCharSequence seq, int maxLength) {
            this.seq = seq;
            this.maxLength = maxLength;
        }

        public AppendableCharSequence parse(ByteBuf buffer) {
            final int oldSize = size;
            seq.reset();
            int i = buffer.forEachByte(this);
            if (i == -1) {
                size = oldSize;
                return null;
            }
            buffer.readerIndex(i + 1);
            return seq;
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
            return new TooLongHttpHeaderException("HTTP header is larger than " + maxLength + " bytes.");
        }
    }

    private final class LineParser extends HeaderParser {

        LineParser(AppendableCharSequence seq, int maxLength) {
            super(seq, maxLength);
        }

        @Override
        public AppendableCharSequence parse(ByteBuf buffer) {
            // Suppress a warning because HeaderParser.reset() is supposed to be called
            reset();    // lgtm[java/subtle-inherited-call]
            return super.parse(buffer);
        }

        @Override
        public boolean process(byte value) throws Exception {
            if (reqState == State.SKIP_CONTROL_CHARS) {
                char c = (char) (value & 0xFF);
                if (Character.isISOControl(c) || Character.isWhitespace(c)) {
                    increaseCount();
                    return true;
                }
                reqState = State.READ_INITIAL;
            }
            return super.process(value);
        }

        @Override
        protected TooLongFrameException newException(int maxLength) {
            return new TooLongHttpLineException("An HTTP line is larger than " + maxLength + " bytes.");
        }
    }

}
