package com.merchantassistant.purchase;

import com.merchantassistant.product.Product;
import com.merchantassistant.product.ProductRepository;
import com.merchantassistant.common.PageResponse;
import com.merchantassistant.notification.NotificationService;
import com.merchantassistant.supplier.Supplier;
import com.merchantassistant.supplier.SupplierRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import com.merchantassistant.store.StoreContext;

@Service
public class PurchaseOrderService {

    private final PurchaseOrderRepository orderRepository;
    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;
    private final InventoryService inventoryService;
    private final NotificationService notificationService;

    public PurchaseOrderService(PurchaseOrderRepository orderRepository, SupplierRepository supplierRepository,
                                ProductRepository productRepository, InventoryService inventoryService,
                                NotificationService notificationService) {
        this.orderRepository = orderRepository;
        this.supplierRepository = supplierRepository;
        this.productRepository = productRepository;
        this.inventoryService = inventoryService;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public List<PurchaseOrderView> list() { return orderRepository.findByStoreIdOrderByCreatedAtDesc(StoreContext.get()).stream().map(this::toView).toList(); }

    @Transactional(readOnly = true)
    public PageResponse<PurchaseOrderView> page(int page, int size, PurchaseOrderStatus status, LocalDate from, LocalDate to, String keyword) {
        Specification<PurchaseOrder> specification = (root, query, builder) -> builder.equal(root.get("storeId"), StoreContext.get());
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
            specification = specification.and((root, query, builder) -> builder.or(
                    builder.like(builder.lower(root.get("orderNo")), value),
                    builder.like(builder.lower(root.join("supplier").get("name")), value)));
        }
        var result = orderRepository.findAll(specification,
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by(Sort.Direction.DESC, "createdAt")));
        return new PageResponse<>(result.getContent().stream().map(this::toView).toList(), result.getTotalElements(),
                result.getTotalPages(), result.getNumber(), result.getSize());
    }

    @Transactional(readOnly = true)
    public PurchaseOrderView get(Long id) { return toView(orderRepository.findByIdAndStoreId(id, StoreContext.get()).orElseThrow(() -> new IllegalArgumentException("采购单不存在，id=" + id))); }

    @Transactional
    public PurchaseOrderView create(PurchaseOrderRequest request) {
        Supplier supplier = supplierRepository.findById(request.supplierId()).filter(item -> StoreContext.get() == item.getStoreId()).orElseThrow(() -> new IllegalArgumentException("供应商不存在，id=" + request.supplierId()));
        if (!Boolean.TRUE.equals(supplier.getEnabled())) throw new IllegalArgumentException("供应商已停用");
        PurchaseOrder order = new PurchaseOrder();
        order.setStoreId(StoreContext.get());
        order.setOrderNo("PO-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase());
        order.setSupplier(supplier);
        order.setRemark(request.remark());
        BigDecimal total = BigDecimal.ZERO;
        for (PurchaseItemRequest itemRequest : request.items()) {
            Product product = productRepository.findById(itemRequest.productId()).filter(item -> StoreContext.get() == item.getStoreId()).orElseThrow(() -> new IllegalArgumentException("商品不存在，id=" + itemRequest.productId()));
            if (!Boolean.TRUE.equals(product.getEnabled())) throw new IllegalArgumentException("商品已停用: " + product.getName());
            PurchaseOrderItem item = new PurchaseOrderItem();
            item.setProduct(product);
            item.setQuantity(itemRequest.quantity());
            item.setUnitPrice(itemRequest.unitPrice());
            item.setSubtotal(itemRequest.unitPrice().multiply(BigDecimal.valueOf(itemRequest.quantity())));
            order.addItem(item);
            total = total.add(item.getSubtotal());
        }
        order.setTotalAmount(total);
        PurchaseOrder saved = orderRepository.save(order);
        notificationService.createPurchasePending(saved.getId(), saved.getOrderNo());
        return toView(saved);
    }

    @Transactional
    public PurchaseOrderView receive(Long id) {
        PurchaseOrder order = orderRepository.findByIdAndStoreId(id, StoreContext.get()).orElseThrow(() -> new IllegalArgumentException("采购单不存在，id=" + id));
        if (order.getStatus() != PurchaseOrderStatus.DRAFT) throw new IllegalArgumentException("只有草稿采购单可以入库");
        for (PurchaseOrderItem item : order.getItems()) {
            Product product = item.getProduct();
            inventoryService.apply(product, item.getQuantity(), InventoryTransactionType.PURCHASE, "PURCHASE_ORDER", order.getId());
        }
        order.setStatus(PurchaseOrderStatus.RECEIVED);
        order.setReceivedAt(Instant.now());
        PurchaseOrder saved = orderRepository.save(order);
        notificationService.completePurchase(saved.getId(), saved.getOrderNo());
        return toView(saved);
    }

    @Transactional
    public PurchaseOrderView cancel(Long id) {
        PurchaseOrder order = orderRepository.findByIdAndStoreId(id, StoreContext.get()).orElseThrow(() -> new IllegalArgumentException("采购单不存在，id=" + id));
        if (order.getStatus() != PurchaseOrderStatus.DRAFT) throw new IllegalArgumentException("只有草稿采购单可以取消");
        order.setStatus(PurchaseOrderStatus.CANCELLED);
        PurchaseOrder saved = orderRepository.save(order);
        notificationService.cancelPurchase(saved.getId());
        return toView(saved);
    }

    private PurchaseOrderView toView(PurchaseOrder order) {
        List<PurchaseItemView> items = order.getItems().stream().map(item -> new PurchaseItemView(item.getProduct().getId(), item.getProduct().getName(), item.getProduct().getSku(), item.getQuantity(), item.getUnitPrice(), item.getSubtotal())).toList();
        return new PurchaseOrderView(order.getId(), order.getOrderNo(), order.getSupplier().getId(), order.getSupplier().getName(), order.getStatus(), order.getTotalAmount(), order.getRemark(), order.getCreatedAt(), order.getReceivedAt(), items);
    }
}
