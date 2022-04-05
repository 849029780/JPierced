package com.jian.transmit.handler.local.http;

import io.netty.util.internal.AppendableCharSequence;
import lombok.AllArgsConstructor;
import lombok.Data;

/***
 * 读取行相关信息
 * @author Jian
 * @date 2022/1/21
 */
@Data
@AllArgsConstructor
public class ParseAppendableCharSequence {

    /***
     * 该行在该次buffer中的开始位置，如果该行没内容，则该值为空
     */
    private Integer startIndex;

    /***
     * 该行在该次buffer中的开始位置，如果该行没内容，则该值为空
     */
    private Integer endIndex;

    /***
     * 该行内容
     */
    private AppendableCharSequence appendableCharSequence;

}
