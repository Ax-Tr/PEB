package com.paywithease.product.api;

import com.paywithease.product.application.ProductService;
import com.paywithease.product.domain.HsnSac;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** HSN/SAC master lookup API — used to prefill GST rate at catalog-entry time. */
@RestController
@RequestMapping("/api/v1/hsn-sac")
@Tag(name = "hsn-sac", description = "HSN/SAC master code lookup")
public class HsnSacController {

  private final ProductService service;

  public HsnSacController(ProductService service) {
    this.service = service;
  }

  @GetMapping
  @Operation(summary = "Search HSN/SAC codes by code prefix or description (max 20)")
  public List<ProductDtos.HsnSacResponse> search(@RequestParam(name = "q") String q) {
    return service.searchHsnSac(q).stream().map(HsnSacController::toResponse).toList();
  }

  private static ProductDtos.HsnSacResponse toResponse(HsnSac h) {
    return new ProductDtos.HsnSacResponse(
        h.getCode(), h.getDescription(), h.getGstRate(), h.getKind());
  }
}
