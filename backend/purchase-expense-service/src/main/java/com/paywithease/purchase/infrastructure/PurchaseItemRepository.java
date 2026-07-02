package com.paywithease.purchase.infrastructure;

import com.paywithease.purchase.domain.PurchaseItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseItemRepository extends JpaRepository<PurchaseItem, String> {

  List<PurchaseItem> findByPurchaseBillId(String purchaseBillId);
}
