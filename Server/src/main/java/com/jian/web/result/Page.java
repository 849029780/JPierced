package com.jian.web.result;

import lombok.Data;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/**
 * 描述
 *
 * @author Jian
 * @date 2022/04/05
 */
@Data
public class Page {

    private Object data;

    private int size;

    private Page() {
    }

    public static <T> Page page(Collection<T> data) {
        Page tPage = new Page();
        Optional.ofNullable(data).ifPresent(dt->{
            tPage.setData(data);
            tPage.setSize(data.size());
        });
        return tPage;
    }

    public static <K, V> Page page(Map<K, V> data) {
        Page tPage = new Page();
        Optional.ofNullable(data).ifPresent(dt->{
            tPage.setData(data);
            tPage.setSize(data.size());
        });
        return tPage;
    }


}
