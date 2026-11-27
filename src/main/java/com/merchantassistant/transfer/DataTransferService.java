package com.merchantassistant.transfer;

import com.merchantassistant.product.Product;
import com.merchantassistant.product.ProductRepository;
import com.merchantassistant.product.ProductRequest;
import com.merchantassistant.product.ProductService;
import com.merchantassistant.purchase.InventoryService;
import com.merchantassistant.purchase.InventoryTransaction;
import com.merchantassistant.purchase.InventoryTransactionRepository;
import com.merchantassistant.purchase.InventoryTransactionType;
import com.merchantassistant.purchase.PurchaseOrder;
import com.merchantassistant.purchase.PurchaseOrderItem;
import com.merchantassistant.purchase.PurchaseOrderRepository;
import com.merchantassistant.sale.SaleOrder;
import com.merchantassistant.sale.SaleOrderItem;
import com.merchantassistant.sale.SaleOrderRepository;
import com.merchantassistant.store.StoreContext;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class DataTransferService {
    private static final List<String> PRODUCT_HEADERS = List.of(
            "商品编码", "商品名称", "分类", "采购价", "销售价", "初始库存", "预警库存", "启用状态");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    private final ProductService productService;
    private final ProductRepository productRepository;
    private final InventoryService inventoryService;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final SaleOrderRepository saleOrderRepository;
    private final InventoryTransactionRepository transactionRepository;

    public DataTransferService(ProductService productService, ProductRepository productRepository,
                               InventoryService inventoryService, PurchaseOrderRepository purchaseOrderRepository,
                               SaleOrderRepository saleOrderRepository,
                               InventoryTransactionRepository transactionRepository) {
        this.productService = productService;
        this.productRepository = productRepository;
        this.inventoryService = inventoryService;
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.saleOrderRepository = saleOrderRepository;
        this.transactionRepository = transactionRepository;
    }

    public byte[] productTemplate() {
        return workbook("商品导入模板", PRODUCT_HEADERS, List.of(
                List.of("SP-001", "示例矿泉水", "饮料", "1.20", "2.00", "20", "5", "是"),
                List.of("说明：请删除示例行后导入；分类必须已在当前门店启用。", "", "", "", "", "", "", "")));
    }

    public ProductImportResult importProducts(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("请选择要导入的 Excel 文件");
        List<ProductImportError> errors = new ArrayList<>();
        int total = 0;
        int success = 0;
        Set<String> seenSkus = new HashSet<>();
        DataFormatter formatter = new DataFormatter(Locale.CHINA);
        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getNumberOfSheets() == 0 ? null : workbook.getSheetAt(0);
            if (sheet == null || sheet.getPhysicalNumberOfRows() == 0) throw new IllegalArgumentException("Excel 中没有可导入的数据");
            validateHeaders(sheet.getRow(0), formatter);
            if (sheet.getLastRowNum() > 1000) throw new IllegalArgumentException("单次最多导入 1000 条商品");
            for (int index = 1; index <= sheet.getLastRowNum(); index++) {
                Row row = sheet.getRow(index);
                if (row == null || rowIsBlank(row, formatter)) continue;
                String sku = text(row, 0, formatter);
                try {
                    if (sku.startsWith("说明：")) continue;
                    total++;
                    if (sku.isBlank()) throw new IllegalArgumentException("商品编码不能为空");
                    String normalizedSku = sku.trim().toLowerCase(Locale.ROOT);
                    if (!seenSkus.add(normalizedSku)) throw new IllegalArgumentException("Excel 内商品编码重复");
                    ProductRequest request = new ProductRequest(
                            sku, required(text(row, 1, formatter), "商品名称不能为空"),
                            text(row, 2, formatter), decimal(row, 3, formatter, "采购价"),
                            decimal(row, 4, formatter, "销售价"), integer(row, 6, formatter, "预警库存"),
                            enabled(text(row, 7, formatter)));
                    int initialStock = integer(row, 5, formatter, "初始库存");
                    Product product = productService.create(request);
                    if (initialStock > 0) {
                        inventoryService.apply(product, initialStock, InventoryTransactionType.ADJUSTMENT,
                                "PRODUCT_IMPORT", product.getId());
                    }
                    success++;
                } catch (Exception exception) {
                    errors.add(new ProductImportError(index + 1, sku, rootMessage(exception)));
                }
            }
        } catch (IOException exception) {
            throw new IllegalArgumentException("Excel 文件无法读取，请使用系统提供的 .xlsx 模板");
        }
        return new ProductImportResult(total, success, errors.size(), errors);
    }

    @Transactional(readOnly = true)
    public byte[] exportProducts() {
        List<List<?>> rows = productRepository.findByStoreIdOrderByUpdatedAtDesc(StoreContext.get()).stream()
                .<List<?>>map(p -> List.of(p.getSku(), p.getName(), value(p.getCategory()), p.getPurchasePrice(),
                        p.getSellingPrice(), p.getStock(), p.getStockWarningLine(), Boolean.TRUE.equals(p.getEnabled()) ? "启用" : "停用",
                        DATE_TIME.format(p.getUpdatedAt())))
                .toList();
        return workbook("商品库存", List.of("商品编码", "商品名称", "分类", "采购价", "销售价", "当前库存", "预警库存", "状态", "更新时间"), rows);
    }

    @Transactional(readOnly = true)
    public byte[] exportPurchases() {
        List<List<?>> rows = new ArrayList<>();
        for (PurchaseOrder order : purchaseOrderRepository.findByStoreIdOrderByCreatedAtDesc(StoreContext.get())) {
            for (PurchaseOrderItem item : order.getItems()) {
                rows.add(List.of(order.getOrderNo(), order.getSupplier().getName(), order.getStatus().name(),
                        item.getProduct().getSku(), item.getProduct().getName(), item.getQuantity(), item.getUnitPrice(),
                        item.getSubtotal(), order.getTotalAmount(), DATE_TIME.format(order.getCreatedAt()), value(order.getRemark())));
            }
        }
        return workbook("采购单", List.of("采购单号", "供应商", "状态", "商品编码", "商品名称", "数量", "采购单价", "明细金额", "订单总额", "创建时间", "备注"), rows);
    }

    @Transactional(readOnly = true)
    public byte[] exportSales() {
        List<List<?>> rows = new ArrayList<>();
        for (SaleOrder order : saleOrderRepository.findByStoreIdOrderByCreatedAtDesc(StoreContext.get())) {
            for (SaleOrderItem item : order.getItems()) {
                rows.add(List.of(order.getOrderNo(), order.getStatus().name(), item.getProduct().getSku(),
                        item.getProduct().getName(), item.getQuantity(), item.getUnitPrice(), item.getSubtotal(),
                        order.getTotalAmount(), order.getTotalCost(), DATE_TIME.format(order.getCreatedAt()), value(order.getRemark())));
            }
        }
        return workbook("销售单", List.of("销售单号", "状态", "商品编码", "商品名称", "数量", "销售单价", "明细金额", "订单总额", "订单成本", "销售时间", "备注"), rows);
    }

    @Transactional(readOnly = true)
    public byte[] exportInventory() {
        List<List<?>> rows = transactionRepository.findByStoreIdOrderByCreatedAtDesc(StoreContext.get()).stream()
                .<List<?>>map(t -> List.of(DATE_TIME.format(t.getCreatedAt()), t.getProduct().getSku(), t.getProduct().getName(),
                        t.getType().name(), t.getAfterStock() - t.getBeforeStock(), t.getBeforeStock(), t.getAfterStock(),
                        t.getReferenceType(), t.getReferenceId()))
                .toList();
        return workbook("库存流水", List.of("发生时间", "商品编码", "商品名称", "类型", "库存变化", "变化前", "变化后", "关联类型", "关联ID"), rows);
    }

    private byte[] workbook(String sheetName, List<String> headers, List<? extends List<?>> rows) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(sheetName);
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.DARK_GREEN.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            Row header = sheet.createRow(0);
            for (int i = 0; i < headers.size(); i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(headers.get(i));
                cell.setCellStyle(headerStyle);
            }
            int rowIndex = 1;
            for (List<?> values : rows) {
                Row row = sheet.createRow(rowIndex++);
                for (int i = 0; i < values.size(); i++) setCell(row.createCell(i), values.get(i));
            }
            for (int i = 0; i < headers.size(); i++) {
                int characters = Math.min(Math.max(headers.get(i).length() * 2 + 4, 12), 28);
                sheet.setColumnWidth(i, characters * 256);
            }
            sheet.createFreezePane(0, 1);
            workbook.write(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Excel 文件生成失败", exception);
        }
    }

    private void validateHeaders(Row row, DataFormatter formatter) {
        if (row == null) throw new IllegalArgumentException("Excel 缺少表头");
        for (int i = 0; i < PRODUCT_HEADERS.size(); i++) {
            if (!PRODUCT_HEADERS.get(i).equals(text(row, i, formatter))) {
                throw new IllegalArgumentException("Excel 表头不正确，请下载最新导入模板");
            }
        }
    }

    private void setCell(Cell cell, Object value) {
        if (value instanceof Number number) cell.setCellValue(number.doubleValue());
        else cell.setCellValue(value == null ? "" : value.toString());
    }

    private String text(Row row, int index, DataFormatter formatter) {
        Cell cell = row.getCell(index);
        return cell == null ? "" : formatter.formatCellValue(cell).trim();
    }

    private boolean rowIsBlank(Row row, DataFormatter formatter) {
        for (int i = 0; i < PRODUCT_HEADERS.size(); i++) if (!text(row, i, formatter).isBlank()) return false;
        return true;
    }

    private String required(String value, String message) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(message);
        return value.trim();
    }

    private BigDecimal decimal(Row row, int index, DataFormatter formatter, String field) {
        String value = text(row, index, formatter).replace(",", "");
        try {
            BigDecimal number = new BigDecimal(value);
            if (number.signum() < 0) throw new NumberFormatException();
            return number;
        } catch (Exception exception) {
            throw new IllegalArgumentException(field + "必须是大于等于 0 的数字");
        }
    }

    private int integer(Row row, int index, DataFormatter formatter, String field) {
        String value = text(row, index, formatter).replace(",", "");
        try {
            BigDecimal number = new BigDecimal(value);
            if (number.signum() < 0 || number.stripTrailingZeros().scale() > 0) throw new NumberFormatException();
            return number.intValueExact();
        } catch (Exception exception) {
            throw new IllegalArgumentException(field + "必须是大于等于 0 的整数");
        }
    }

    private Boolean enabled(String value) {
        if (value == null || value.isBlank() || List.of("是", "启用", "true", "1").contains(value.toLowerCase(Locale.ROOT))) return true;
        if (List.of("否", "停用", "false", "0").contains(value.toLowerCase(Locale.ROOT))) return false;
        throw new IllegalArgumentException("启用状态只能填写是/否");
    }

    private String rootMessage(Exception exception) {
        Throwable current = exception;
        while (current.getCause() != null && current.getCause() != current) current = current.getCause();
        return current.getMessage() == null ? "导入失败" : current.getMessage();
    }

    private String value(String value) { return value == null ? "" : value; }
}
