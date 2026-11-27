package com.merchantassistant.analytics;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.merchantassistant.product.Product;
import com.merchantassistant.product.ProductRepository;
import com.merchantassistant.sale.SaleOrder;
import com.merchantassistant.sale.SaleOrderItem;
import com.merchantassistant.sale.SaleOrderRepository;
import com.merchantassistant.store.StoreContext;
import com.merchantassistant.store.StoreRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AnalyticsService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");
    private static final int REPLENISHMENT_COVERAGE_DAYS = 14;
    private static final String RULES = "RULES";
    private static final String AI = "AI";

    private final SaleOrderRepository saleOrderRepository;
    private final ProductRepository productRepository;
    private final StoreRepository storeRepository;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final String apiKey;
    private final String apiUrl;
    private final String model;

    public AnalyticsService(SaleOrderRepository saleOrderRepository, ProductRepository productRepository,
                            StoreRepository storeRepository,
                            ObjectMapper objectMapper,
                            @Value("${openai.api-key:}") String apiKey,
                            @Value("${openai.api-url:https://api.openai.com/v1/responses}") String apiUrl,
                            @Value("${openai.model:gpt-4.1-mini}") String model) {
        this.saleOrderRepository = saleOrderRepository;
        this.productRepository = productRepository;
        this.storeRepository = storeRepository;
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.apiUrl = apiUrl;
        this.model = model;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(6)).build();
    }

    @Transactional(readOnly = true)
    public AnalyticsOverview overview() {
        LocalDate today = LocalDate.now(BUSINESS_ZONE);
        Instant from = today.minusDays(29).atStartOfDay(BUSINESS_ZONE).toInstant();
        List<SaleOrder> orders = saleOrderRepository.findByStoreIdAndCreatedAtGreaterThanEqual(StoreContext.get(), from);
        List<Product> products = productRepository.findByStoreId(StoreContext.get());

        BigDecimal sales = orders.stream().map(SaleOrder::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal cost = orders.stream().map(SaleOrder::getTotalCost).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal profit = sales.subtract(cost);
        BigDecimal margin = percentage(profit, sales);

        Map<LocalDate, List<SaleOrder>> byDate = new LinkedHashMap<>();
        for (int i = 6; i >= 0; i--) byDate.put(today.minusDays(i), new ArrayList<>());
        orders.forEach(order -> {
            LocalDate date = order.getCreatedAt().atZone(BUSINESS_ZONE).toLocalDate();
            if (byDate.containsKey(date)) byDate.get(date).add(order);
        });
        List<DailySalesView> dailySales = byDate.entrySet().stream().map(entry -> new DailySalesView(
                entry.getKey(), entry.getValue().stream().map(SaleOrder::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add),
                entry.getValue().size())).toList();

        Map<Long, ProductSales> productSales = aggregateProductSales(orders);
        List<TopProductView> topProducts = productSales.values().stream()
                .sorted(Comparator.comparing(ProductSales::salesAmount).reversed()).limit(5)
                .map(ProductSales::toView).toList();

        List<LowStockView> lowStock = products.stream()
                .filter(product -> Boolean.TRUE.equals(product.getEnabled()) && product.getStock() <= product.getStockWarningLine())
                .sorted(Comparator.comparing(Product::getStock))
                .map(product -> new LowStockView(product.getId(), product.getName(), product.getSku(),
                        product.getStock(), product.getStockWarningLine())).toList();
        int totalStock = products.stream().filter(product -> Boolean.TRUE.equals(product.getEnabled()))
                .mapToInt(Product::getStock).sum();

        return new AnalyticsOverview(30, sales, orders.size(), profit, margin, totalStock,
                lowStock.size(), dailySales, topProducts, lowStock);
    }

    @Transactional(readOnly = true)
    public List<StoreComparisonView> storeComparison() {
        LocalDate today = LocalDate.now(BUSINESS_ZONE);
        Instant from = today.minusDays(29).atStartOfDay(BUSINESS_ZONE).toInstant();
        return storeRepository.findByEnabledTrueOrderById().stream().map(store -> {
            List<SaleOrder> orders = saleOrderRepository.findByStoreIdAndCreatedAtGreaterThanEqual(store.getId(), from);
            List<Product> products = productRepository.findByStoreId(store.getId());
            BigDecimal sales = orders.stream().map(SaleOrder::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal cost = orders.stream().map(SaleOrder::getTotalCost).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal profit = sales.subtract(cost);
            int totalStock = products.stream().filter(product -> Boolean.TRUE.equals(product.getEnabled()))
                    .mapToInt(Product::getStock).sum();
            int lowStockCount = (int) products.stream()
                    .filter(product -> Boolean.TRUE.equals(product.getEnabled()))
                    .filter(product -> product.getStock() <= product.getStockWarningLine()).count();
            return new StoreComparisonView(store.getId(), store.getName(), store.getAddress(), sales, orders.size(),
                    profit, percentage(profit, sales), totalStock, lowStockCount);
        }).toList();
    }

    @Transactional(readOnly = true)
    public AdviceResponse advice() {
        AnalyticsOverview overview = overview();
        AdviceResponse fallback = rulesAdvice(overview, "已使用规则分析。");
        String prompt = "你是中小便利店经营分析助手。根据以下30天聚合数据给出简洁、保守、可执行的中文建议。"
                + "不得虚构数据。数据：" + writeJson(overview);
        AiJsonResult ai = requestAiJson(prompt, adviceSchema(), "merchant_advice", 500);
        if (ai.content() == null) return rulesAdvice(overview, ai.statusMessage());

        List<String> actions = textList(ai.content().path("actions"));
        if (actions.isEmpty()) return fallback;
        return new AdviceResponse(AI, ai.statusMessage(),
                ai.content().path("summary").asText(fallback.summary()), actions, Instant.now());
    }

    @Transactional(readOnly = true)
    public WeeklyReportResponse weeklyReport() {
        LocalDate today = LocalDate.now(BUSINESS_ZONE);
        LocalDate fromDate = today.minusDays(6);
        Instant from = fromDate.atStartOfDay(BUSINESS_ZONE).toInstant();
        Instant to = today.plusDays(1).atStartOfDay(BUSINESS_ZONE).toInstant();
        Instant previousFrom = fromDate.minusDays(7).atStartOfDay(BUSINESS_ZONE).toInstant();
        PeriodStats current = periodStats(from, to);
        PeriodStats previous = periodStats(previousFrom, from);
        BigDecimal salesChange = previous.salesAmount().signum() == 0 ? null
                : current.salesAmount().subtract(previous.salesAmount()).multiply(BigDecimal.valueOf(100))
                .divide(previous.salesAmount(), 2, RoundingMode.HALF_UP);

        AnalyticsOverview overview = overview();
        List<String> fallbackHighlights = weeklyHighlights(current, previous, salesChange, overview);
        List<String> fallbackActions = weeklyActions(current, overview);
        String fallbackSummary = "本周完成 " + current.orderCount() + " 笔销售，销售额 "
                + current.salesAmount() + " 元，毛利 " + current.grossProfit() + " 元，毛利率 "
                + current.grossMargin() + "% 。";

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("from", fromDate);
        data.put("to", today);
        data.put("currentWeek", current);
        data.put("previousWeek", previous);
        data.put("salesChangePercent", salesChange);
        data.put("lowStockProducts", overview.lowStockProducts());
        String prompt = "你是中小便利店经营周报助手。根据给定数据生成中文周报，不得虚构数据。"
                + "总结一到两句，亮点和行动建议各不超过3条。数据：" + writeJson(data);
        AiJsonResult ai = requestAiJson(prompt, weeklyReportSchema(), "merchant_weekly_report", 700);

        String source = ai.content() == null ? RULES : AI;
        String summary = ai.content() == null ? fallbackSummary : ai.content().path("summary").asText(fallbackSummary);
        List<String> highlights = ai.content() == null ? fallbackHighlights : textList(ai.content().path("highlights"));
        List<String> actions = ai.content() == null ? fallbackActions : textList(ai.content().path("actions"));
        if (highlights.isEmpty()) highlights = fallbackHighlights;
        if (actions.isEmpty()) actions = fallbackActions;
        return new WeeklyReportResponse(source, ai.statusMessage(), fromDate, today, current.salesAmount(),
                current.orderCount(), current.productQuantity(), current.grossProfit(), current.grossMargin(), salesChange,
                summary, highlights, actions, Instant.now());
    }

    @Transactional(readOnly = true)
    public ReplenishmentAdviceResponse replenishmentAdvice() {
        LocalDate today = LocalDate.now(BUSINESS_ZONE);
        Instant from = today.minusDays(29).atStartOfDay(BUSINESS_ZONE).toInstant();
        List<SaleOrder> orders = saleOrderRepository.findByStoreIdAndCreatedAtGreaterThanEqual(StoreContext.get(), from);
        Map<Long, Integer> soldQuantity = new LinkedHashMap<>();
        orders.stream().flatMap(order -> order.getItems().stream()).forEach(item -> soldQuantity.merge(
                item.getProduct().getId(), item.getQuantity(), Integer::sum));

        List<ReplenishmentItemView> items = productRepository.findByStoreId(StoreContext.get()).stream()
                .filter(product -> Boolean.TRUE.equals(product.getEnabled()))
                .map(product -> replenishmentItem(product, soldQuantity.getOrDefault(product.getId(), 0)))
                .filter(item -> item.recommendedQuantity() > 0)
                .sorted(Comparator.comparing(ReplenishmentItemView::recommendedQuantity).reversed())
                .toList();
        BigDecimal estimatedCost = items.stream().map(ReplenishmentItemView::estimatedCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        String fallbackSummary = items.isEmpty()
                ? "当前没有需要立即补货的商品，建议继续观察销量和库存预警。"
                : "建议补充 " + items.size() + " 种商品，预计采购成本 " + estimatedCost + " 元。";

        Map<String, Object> data = Map.of(
                "coverageDays", REPLENISHMENT_COVERAGE_DAYS,
                "estimatedCost", estimatedCost,
                "items", items);
        String prompt = "你是中小便利店补货助手。补货数量已经由系统根据近30天销量、库存预警线和14天覆盖周期算好。"
                + "不得修改数量或虚构商品，只用一到两句中文总结优先级和风险。数据：" + writeJson(data);
        AiJsonResult ai = requestAiJson(prompt, summarySchema(), "merchant_replenishment_summary", 300);
        String summary = ai.content() == null ? fallbackSummary : ai.content().path("summary").asText(fallbackSummary);
        return new ReplenishmentAdviceResponse(ai.content() == null ? RULES : AI, ai.statusMessage(),
                REPLENISHMENT_COVERAGE_DAYS, estimatedCost, summary, items, Instant.now());
    }

    private PeriodStats periodStats(Instant from, Instant to) {
        List<SaleOrder> orders = saleOrderRepository
                .findByStoreIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDesc(StoreContext.get(), from, to);
        BigDecimal sales = orders.stream().map(SaleOrder::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal cost = orders.stream().map(SaleOrder::getTotalCost).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal profit = sales.subtract(cost);
        int quantity = orders.stream().flatMap(order -> order.getItems().stream())
                .mapToInt(SaleOrderItem::getQuantity).sum();
        String topProduct = aggregateProductSales(orders).values().stream()
                .max(Comparator.comparing(ProductSales::salesAmount)).map(ProductSales::name).orElse(null);
        return new PeriodStats(sales, orders.size(), quantity, profit, percentage(profit, sales), topProduct);
    }

    private List<String> weeklyHighlights(PeriodStats current, PeriodStats previous, BigDecimal change,
                                          AnalyticsOverview overview) {
        List<String> highlights = new ArrayList<>();
        if (change == null) {
            highlights.add(previous.salesAmount().signum() == 0 ? "上周暂无销售额，本周暂不计算环比。"
                    : "暂无足够数据计算销售额环比。");
        } else {
            highlights.add("本周销售额较上周" + (change.signum() >= 0 ? "增长 " : "下降 ")
                    + change.abs() + "% 。");
        }
        highlights.add(current.topProductName() == null ? "本周暂无商品销售排行。"
                : "本周销售额最高的商品是“" + current.topProductName() + "”。");
        highlights.add(overview.lowStockCount() == 0 ? "当前没有商品低于库存预警线。"
                : "当前有 " + overview.lowStockCount() + " 种商品低于库存预警线。");
        return highlights;
    }

    private List<String> weeklyActions(PeriodStats current, AnalyticsOverview overview) {
        List<String> actions = new ArrayList<>();
        if (overview.lowStockCount() > 0) actions.add("优先处理库存预警商品，确认供应商和到货周期。");
        if (current.orderCount() == 0) actions.add("持续录入真实销售，积累可用于趋势判断的数据。");
        else actions.add("复盘本周畅销商品，并保证下周库存可覆盖正常销量。");
        if (current.salesAmount().signum() > 0 && current.grossMargin().compareTo(BigDecimal.valueOf(20)) < 0) {
            actions.add("本周毛利率低于20%，核对采购价和售价设置。");
        } else {
            actions.add("继续关注采购成本变化，保持当前毛利空间。");
        }
        return actions;
    }

    private ReplenishmentItemView replenishmentItem(Product product, int salesQuantity) {
        int salesTarget = (int) Math.ceil(salesQuantity / 30.0 * REPLENISHMENT_COVERAGE_DAYS);
        int safetyTarget = Math.max(product.getStockWarningLine() + 1, product.getStockWarningLine() * 2);
        int targetStock = Math.max(salesTarget, safetyTarget);
        boolean belowWarning = product.getStock() <= product.getStockWarningLine();
        boolean belowSalesCoverage = salesQuantity > 0 && product.getStock() < salesTarget;
        int recommended = belowWarning || belowSalesCoverage ? Math.max(1, targetStock - product.getStock()) : 0;
        String reason = belowWarning ? "当前库存已达到或低于预警线，建议补至 " + targetStock + " 件。"
                : belowSalesCoverage ? "当前库存不足以覆盖预计14天销量，建议补至 " + targetStock + " 件。"
                : "当前库存充足。";
        BigDecimal estimatedCost = product.getPurchasePrice().multiply(BigDecimal.valueOf(recommended))
                .setScale(2, RoundingMode.HALF_UP);
        return new ReplenishmentItemView(product.getId(), product.getName(), product.getSku(), product.getStock(),
                product.getStockWarningLine(), salesQuantity, recommended, estimatedCost, reason);
    }

    private AdviceResponse rulesAdvice(AnalyticsOverview overview, String statusMessage) {
        List<String> actions = new ArrayList<>();
        if (overview.lowStockCount() > 0) {
            String names = overview.lowStockProducts().stream().limit(3).map(LowStockView::productName)
                    .reduce((a, b) -> a + "、" + b).orElse("");
            actions.add("优先补充低库存商品：" + names + "，避免影响正常销售。");
        } else {
            actions.add("当前没有商品低于预警线，继续按销售节奏观察库存变化。");
        }
        if (!overview.topProducts().isEmpty()) {
            TopProductView top = overview.topProducts().get(0);
            actions.add("近30天销售额最高的是“" + top.productName() + "”，建议保证货源并观察其连带销售。");
        } else {
            actions.add("当前销售数据较少，建议连续登记销售，以便形成可靠的商品排行。");
        }
        if (overview.salesAmount().signum() > 0 && overview.grossMargin().compareTo(BigDecimal.valueOf(20)) < 0) {
            actions.add("当前毛利率低于20%，建议核对采购价并减少低毛利促销。");
        } else {
            actions.add("定期复核采购价和售价，保持毛利空间稳定。");
        }
        String summary = overview.orderCount() == 0 ? "近30天暂无销售单，建议先持续录入真实经营数据。"
                : "近30天完成 " + overview.orderCount() + " 笔销售，销售额 " + overview.salesAmount()
                + " 元，毛利率 " + overview.grossMargin() + "% 。";
        return new AdviceResponse(RULES, statusMessage, summary, actions, Instant.now());
    }

    private AiJsonResult requestAiJson(String prompt, Map<String, Object> schema, String schemaName, int maxTokens) {
        if (apiKey == null || apiKey.isBlank()) {
            return new AiJsonResult(null, "未配置 OpenAI API 密钥，已使用规则分析。");
        }
        try {
            Map<String, Object> format = new LinkedHashMap<>();
            format.put("type", "json_schema");
            format.put("name", schemaName);
            format.put("strict", true);
            format.put("schema", schema);
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("model", model);
            payload.put("input", prompt);
            payload.put("text", Map.of("format", format));
            payload.put("max_output_tokens", maxTokens);

            HttpRequest request = HttpRequest.newBuilder(URI.create(apiUrl))
                    .timeout(Duration.ofSeconds(25))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(writeJson(payload), StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return new AiJsonResult(null, apiFailureMessage(response.statusCode(), response.body()));
            }
            String outputText = extractOutputText(objectMapper.readTree(response.body()));
            if (outputText == null || outputText.isBlank()) {
                return new AiJsonResult(null, "OpenAI API 未返回可用内容，已使用规则分析。");
            }
            return new AiJsonResult(objectMapper.readTree(stripCodeFence(outputText)), "OpenAI API 已生成分析结果。");
        } catch (HttpConnectTimeoutException | ConnectException exception) {
            return new AiJsonResult(null, "无法连接 OpenAI API，已使用规则分析。");
        } catch (HttpTimeoutException exception) {
            return new AiJsonResult(null, "OpenAI API 响应超时，已使用规则分析。");
        } catch (Exception exception) {
            return new AiJsonResult(null, "OpenAI API 返回内容无法解析，已使用规则分析。");
        }
    }

    private String apiFailureMessage(int statusCode, String responseBody) {
        String body = responseBody == null ? "" : responseBody.toLowerCase();
        if (statusCode == 401) return "OpenAI API 密钥无效，已使用规则分析。";
        if (statusCode == 403) return "当前密钥没有模型或项目权限，已使用规则分析。";
        if (statusCode == 429 && body.contains("insufficient_quota")) return "OpenAI API 额度不足，已使用规则分析。";
        if (statusCode == 429) return "OpenAI API 请求频率受限，已使用规则分析。";
        return "OpenAI API 暂时不可用（HTTP " + statusCode + "），已使用规则分析。";
    }

    private Map<Long, ProductSales> aggregateProductSales(List<SaleOrder> orders) {
        Map<Long, ProductSales> productSales = new LinkedHashMap<>();
        orders.stream().flatMap(order -> order.getItems().stream()).forEach(item -> productSales
                .computeIfAbsent(item.getProduct().getId(), ignored -> new ProductSales(item))
                .add(item));
        return productSales;
    }

    private BigDecimal percentage(BigDecimal numerator, BigDecimal denominator) {
        return denominator.signum() == 0 ? BigDecimal.ZERO
                : numerator.multiply(BigDecimal.valueOf(100)).divide(denominator, 2, RoundingMode.HALF_UP);
    }

    private List<String> textList(JsonNode node) {
        List<String> values = new ArrayList<>();
        node.forEach(item -> {
            if (!item.asText().isBlank()) values.add(item.asText());
        });
        return values;
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalStateException("无法生成分析数据", exception);
        }
    }

    private Map<String, Object> adviceSchema() {
        return objectSchema(Map.of(
                "summary", Map.of("type", "string"),
                "actions", stringArraySchema()), List.of("summary", "actions"));
    }

    private Map<String, Object> weeklyReportSchema() {
        return objectSchema(Map.of(
                "summary", Map.of("type", "string"),
                "highlights", stringArraySchema(),
                "actions", stringArraySchema()), List.of("summary", "highlights", "actions"));
    }

    private Map<String, Object> summarySchema() {
        return objectSchema(Map.of("summary", Map.of("type", "string")), List.of("summary"));
    }

    private Map<String, Object> stringArraySchema() {
        return Map.of("type", "array", "items", Map.of("type", "string"));
    }

    private Map<String, Object> objectSchema(Map<String, Object> properties, List<String> required) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("required", required);
        schema.put("additionalProperties", false);
        return schema;
    }

    private String extractOutputText(JsonNode root) {
        for (JsonNode output : root.path("output")) {
            for (JsonNode content : output.path("content")) {
                if ("output_text".equals(content.path("type").asText())) return content.path("text").asText();
            }
        }
        return root.path("output_text").asText();
    }

    private String stripCodeFence(String value) {
        String text = value == null ? "" : value.trim();
        if (text.startsWith("```")) text = text.replaceFirst("^```(?:json)?\\s*", "").replaceFirst("\\s*```$", "");
        return text;
    }

    private record AiJsonResult(JsonNode content, String statusMessage) { }

    private record PeriodStats(BigDecimal salesAmount, int orderCount, int productQuantity,
                               BigDecimal grossProfit, BigDecimal grossMargin, String topProductName) { }

    private static final class ProductSales {
        private final Long productId;
        private final String name;
        private final String sku;
        private int quantity;
        private BigDecimal salesAmount = BigDecimal.ZERO;

        private ProductSales(SaleOrderItem item) {
            this.productId = item.getProduct().getId();
            this.name = item.getProduct().getName();
            this.sku = item.getProduct().getSku();
        }

        private void add(SaleOrderItem item) {
            quantity += item.getQuantity();
            salesAmount = salesAmount.add(item.getSubtotal());
        }

        private String name() { return name; }
        private BigDecimal salesAmount() { return salesAmount; }
        private TopProductView toView() { return new TopProductView(productId, name, sku, quantity, salesAmount); }
    }
}
