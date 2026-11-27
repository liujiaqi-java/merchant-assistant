package com.merchantassistant.transfer;

public record ProductImportError(int row, String sku, String message) {
}
