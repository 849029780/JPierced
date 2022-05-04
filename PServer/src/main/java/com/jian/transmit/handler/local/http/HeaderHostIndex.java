package com.jian.transmit.handler.local.http;

import lombok.Data;

/***
 * http header中host在buffer中的位置
 * @author Jian
 * @date 2022/1/20
 */
@Data
public class HeaderHostIndex {

    /***
     * 开始位置
     */
    private Integer startIndex;

    /***
     * 结束位置
     */
    private Integer endIndex;


}
