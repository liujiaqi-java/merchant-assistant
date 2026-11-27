package com.merchantassistant.common;

public class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException(Long id) {
        super("商品不存在，id=" + id);
    }
}
