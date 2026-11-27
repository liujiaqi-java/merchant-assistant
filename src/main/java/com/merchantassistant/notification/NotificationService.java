package com.merchantassistant.notification;

import com.merchantassistant.product.Product;
import com.merchantassistant.store.StoreContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class NotificationService {
    private final SystemNotificationRepository repository;

    public NotificationService(SystemNotificationRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public List<NotificationView> list() {
        return repository.findByStoreIdOrderByCreatedAtDesc(StoreContext.get()).stream().map(this::toView).toList();
    }

    @Transactional
    public void createPurchasePending(Long orderId, String orderNo) {
        createOnce(NotificationType.PURCHASE_PENDING, "采购单待处理", "采购单 " + orderNo + " 等待确认入库。", "purchases", "PURCHASE_ORDER", orderId);
    }

    @Transactional
    public void completePurchase(Long orderId, String orderNo) {
        resolve("PURCHASE_ORDER", orderId);
        create(NotificationType.PURCHASE_COMPLETED, "采购已入库", "采购单 " + orderNo + " 已完成入库，库存已更新。", "purchases", "PURCHASE_ORDER", orderId);
    }

    @Transactional
    public void cancelPurchase(Long orderId) { resolve("PURCHASE_ORDER", orderId); }

    @Transactional
    public void inventoryChanged(Product product) {
        if (product.getStock() <= product.getStockWarningLine()) createInventoryAlert(product);
        else resolve("PRODUCT", product.getId());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createSaleAlert(String productName, int requested, int stock) {
        create(NotificationType.SALE_ALERT, "销售库存异常", productName + " 销售数量 " + requested + "，但当前库存仅 " + stock + "。", "sales", "PRODUCT", null);
    }

    @Transactional
    public NotificationView markRead(Long id) {
        SystemNotification notification = repository.findByIdAndStoreId(id, StoreContext.get())
                .orElseThrow(() -> new IllegalArgumentException("通知不存在，id=" + id));
        markRead(notification);
        return toView(repository.save(notification));
    }

    @Transactional
    public void markAllRead() {
        List<SystemNotification> items = repository.findByStoreIdAndReadFlagFalse(StoreContext.get());
        items.forEach(this::markRead);
        repository.saveAll(items);
    }

    private void createInventoryAlert(Product product) {
        NotificationType type = product.getStock() == 0 ? NotificationType.OUT_OF_STOCK : NotificationType.LOW_STOCK;
        String title = product.getStock() == 0 ? "商品已缺货" : "库存不足";
        String message = product.getName() + " 当前库存 " + product.getStock() + "，预警线 " + product.getStockWarningLine() + "。";
        createOnce(type, title, message, "products", "PRODUCT", product.getId());
    }

    private void createOnce(NotificationType type, String title, String message, String targetView, String referenceType, Long referenceId) {
        if (referenceId != null && repository.findFirstByStoreIdAndTypeAndReferenceTypeAndReferenceIdAndReadFlagFalse(
                StoreContext.get(), type, referenceType, referenceId).isPresent()) return;
        create(type, title, message, targetView, referenceType, referenceId);
    }

    private void create(NotificationType type, String title, String message, String targetView, String referenceType, Long referenceId) {
        SystemNotification notification = new SystemNotification();
        notification.setStoreId(StoreContext.get());
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setTargetView(targetView);
        notification.setReferenceType(referenceType);
        notification.setReferenceId(referenceId);
        repository.save(notification);
    }

    private void resolve(String referenceType, Long referenceId) {
        List<SystemNotification> items = repository.findByStoreIdAndReferenceTypeAndReferenceIdAndReadFlagFalse(StoreContext.get(), referenceType, referenceId);
        items.forEach(this::markRead);
        repository.saveAll(items);
    }

    private void markRead(SystemNotification notification) {
        if (!notification.isReadFlag()) {
            notification.setReadFlag(true);
            notification.setReadAt(Instant.now());
        }
    }

    private NotificationView toView(SystemNotification item) {
        return new NotificationView(item.getId(), item.getType(), item.getTitle(), item.getMessage(), item.getTargetView(),
                item.getReferenceType(), item.getReferenceId(), item.isReadFlag(), item.getCreatedAt(), item.getReadAt());
    }
}
