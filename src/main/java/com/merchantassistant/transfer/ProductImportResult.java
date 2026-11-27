package com.merchantassistant.transfer;

import java.util.List;

public record ProductImportResult(int totalRows, int successCount, int failureCount,
                                  List<ProductImportError> errors) {
}
