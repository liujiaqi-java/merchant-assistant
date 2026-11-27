package com.merchantassistant.report;

import com.merchantassistant.sale.SaleOrder;
import com.merchantassistant.sale.SaleOrderItem;
import com.merchantassistant.sale.SaleOrderRepository;
import com.merchantassistant.store.StoreContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class SalesReportService {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");
    private final SaleOrderRepository repository;
    public SalesReportService(SaleOrderRepository repository) { this.repository = repository; }

    @Transactional(readOnly = true)
    public SalesReport create(LocalDate from, LocalDate to) {
        validate(from, to);
        List<SaleOrder> orders = repository.findByStoreIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDesc(StoreContext.get(),
                from.atStartOfDay(BUSINESS_ZONE).toInstant(), to.plusDays(1).atStartOfDay(BUSINESS_ZONE).toInstant());
        BigDecimal sales = orders.stream().map(SaleOrder::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal cost = orders.stream().map(SaleOrder::getTotalCost).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal profit = sales.subtract(cost);
        BigDecimal margin = sales.signum() == 0 ? BigDecimal.ZERO : profit.multiply(BigDecimal.valueOf(100)).divide(sales, 2, RoundingMode.HALF_UP);
        Map<Long, ProductTotal> totals = new LinkedHashMap<>();
        orders.stream().flatMap(order -> order.getItems().stream()).forEach(item -> totals
                .computeIfAbsent(item.getProduct().getId(), ignored -> new ProductTotal(item)).add(item));
        List<SalesReportProduct> products = totals.values().stream()
                .sorted((left, right) -> right.sales.compareTo(left.sales)).map(ProductTotal::view).toList();
        int quantity = products.stream().mapToInt(SalesReportProduct::quantity).sum();
        return new SalesReport(from, to, orders.size(), quantity, sales, cost, profit, margin, products);
    }

    @Transactional(readOnly = true)
    public byte[] csv(LocalDate from, LocalDate to) {
        SalesReport report = create(from, to);
        StringBuilder csv = new StringBuilder("\ufeff统计开始,统计结束,订单数,销量,销售额,成本,毛利,毛利率\r\n")
                .append(report.from()).append(',').append(report.to()).append(',').append(report.orderCount()).append(',')
                .append(report.productQuantity()).append(',').append(report.salesAmount()).append(',').append(report.totalCost()).append(',')
                .append(report.grossProfit()).append(',').append(report.grossMargin()).append("%\r\n\r\n")
                .append("商品编码,商品名称,销量,销售额\r\n");
        report.products().forEach(item -> csv.append(escape(item.sku())).append(',').append(escape(item.productName())).append(',')
                .append(item.quantity()).append(',').append(item.salesAmount()).append("\r\n"));
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private void validate(LocalDate from, LocalDate to) {
        if (from == null || to == null) throw new IllegalArgumentException("请选择完整的报表日期范围");
        if (from.isAfter(to)) throw new IllegalArgumentException("开始日期不能晚于结束日期");
        if (from.plusYears(1).isBefore(to)) throw new IllegalArgumentException("单次报表范围不能超过一年");
    }
    private String escape(String value) { return "\"" + value.replace("\"", "\"\"") + "\""; }
    private static final class ProductTotal {
        private final Long id; private final String name; private final String sku;
        private int quantity; private BigDecimal sales = BigDecimal.ZERO;
        private ProductTotal(SaleOrderItem item) { id = item.getProduct().getId(); name = item.getProduct().getName(); sku = item.getProduct().getSku(); }
        private void add(SaleOrderItem item) { quantity += item.getQuantity(); sales = sales.add(item.getSubtotal()); }
        private SalesReportProduct view() { return new SalesReportProduct(id, name, sku, quantity, sales); }
    }
}
