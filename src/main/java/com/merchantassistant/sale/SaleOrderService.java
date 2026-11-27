package com.merchantassistant.sale;

import com.merchantassistant.product.Product;
import com.merchantassistant.product.ProductRepository;
import com.merchantassistant.common.PageResponse;
import com.merchantassistant.notification.NotificationService;
import com.merchantassistant.purchase.InventoryTransactionType;
import com.merchantassistant.purchase.InventoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.HashMap;
import java.util.Map;
import com.merchantassistant.store.StoreContext;

@Service
public class SaleOrderService {

    private final SaleOrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final InventoryService inventoryService;
    private final NotificationService notificationService;

    public SaleOrderService(SaleOrderRepository orderRepository, ProductRepository productRepository,
                            InventoryService inventoryService, NotificationService notificationService) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.inventoryService = inventoryService;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public List<SaleOrderView> list() { return orderRepository.findByStoreIdOrderByCreatedAtDesc(StoreContext.get()).stream().map(this::toView).toList(); }

    @Transactional(readOnly = true)
    public PageResponse<SaleOrderView> page(int page, int size, SaleOrderStatus status, LocalDate from, LocalDate to, String keyword) {
        Specification<SaleOrder> specification = (root, query, builder) -> builder.equal(root.get("storeId"), StoreContext.get());
        if (status != null) specification = specification.and((root, query, builder) -> builder.equal(root.get("status"), status));
        if (from != null) {
            Instant start = from.atStartOfDay(ZoneId.of("Asia/Shanghai")).toInstant();
            specification = specification.and((root, query, builder) -> builder.greaterThanOrEqualTo(root.get("createdAt"), start));
        }
        if (to != null) {
            Instant end = to.plusDays(1).atStartOfDay(ZoneId.of("Asia/Shanghai")).toInstant();
            specification = specification.and((root, query, builder) -> builder.lessThan(root.get("createdAt"), end));
        }
        if (keyword != null && !keyword.isBlank()) {
            String value = "%" + keyword.trim().toLowerCase() + "%";
            specification = specification.and((root, query, builder) -> builder.like(builder.lower(root.get("orderNo")), value));
        }
        var result = orderRepository.findAll(specification,
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by(Sort.Direction.DESC, "createdAt")));
        return new PageResponse<>(result.getContent().stream().map(this::toView).toList(), result.getTotalElements(),
                result.getTotalPages(), result.getNumber(), result.getSize());
    }

    @Transactional
    public SaleOrderView create(SaleOrderRequest request) {
        Map<Long, Integer> requestedQuantities = new HashMap<>();
        for (SaleItemRequest item : request.items()) requestedQuantities.merge(item.productId(), item.quantity(), Integer::sum);
        Map<Long, Product> requestedProducts = new HashMap<>();
        requestedQuantities.forEach((productId, quantity) -> {
            Product product = productRepository.findById(productId).filter(item -> StoreContext.get() == item.getStoreId())
                    .orElseThrow(() -> new IllegalArgumentException("商品不存在，id=" + productId));
            if (!Boolean.TRUE.equals(product.getEnabled())) throw new IllegalArgumentException("商品已停用: " + product.getName());
            if (quantity > product.getStock()) {
                notificationService.createSaleAlert(product.getName(), quantity, product.getStock());
                throw new IllegalArgumentException("库存不足: " + product.getName() + "，当前库存 " + product.getStock());
            }
            requestedProducts.put(productId, product);
        });
        SaleOrder order = new SaleOrder();
        order.setStoreId(StoreContext.get());
        order.setOrderNo("SO-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase());
        order.setRemark(request.remark());
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal cost = BigDecimal.ZERO;
        for (SaleItemRequest itemRequest : request.items()) {
            Product product = requestedProducts.get(itemRequest.productId());
            SaleOrderItem item = new SaleOrderItem();
            item.setProduct(product);
            item.setQuantity(itemRequest.quantity());
            item.setUnitPrice(product.getSellingPrice());
            item.setSubtotal(product.getSellingPrice().multiply(BigDecimal.valueOf(itemRequest.quantity())));
            order.addItem(item);
            total = total.add(item.getSubtotal());
            cost = cost.add(product.getPurchasePrice().multiply(BigDecimal.valueOf(itemRequest.quantity())));
        }
        order.setTotalAmount(total);
        order.setTotalCost(cost);
        SaleOrder saved = orderRepository.save(order);
        for (SaleOrderItem item : saved.getItems()) {
            inventoryService.apply(item.getProduct(), -item.getQuantity(), InventoryTransactionType.SALE, "SALE_ORDER", saved.getId());
        }
        return toView(saved);
    }

    private SaleOrderView toView(SaleOrder order) {
        List<SaleOrderItemView> items = order.getItems().stream().map(item -> new SaleOrderItemView(
                item.getProduct().getId(), item.getProduct().getName(), item.getProduct().getSku(),
                item.getQuantity(), item.getUnitPrice(), item.getSubtotal())).toList();
        return new SaleOrderView(order.getId(), order.getOrderNo(), order.getStatus(), order.getTotalAmount(),
                order.getTotalCost(), order.getTotalAmount().subtract(order.getTotalCost()), order.getRemark(), order.getCreatedAt(), items);
    }
}
