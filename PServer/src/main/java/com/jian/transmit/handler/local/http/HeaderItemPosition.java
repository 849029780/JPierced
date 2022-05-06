package com.jian.transmit.handler.local.http;

import io.netty.util.AsciiString;
import lombok.Data;

/***
 * http header中host在buffer中的位置
 * @author Jian
 * @date 2022/1/20
 */
@Data
public class HeaderItemPosition {

    /***
     * header名
     */
    private AsciiString headerName;

    /***
     * 开始位置
     */
    private Integer startIndex;

    /***
     * 结束位置
     */
    private Integer endIndex;


}
