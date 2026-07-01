package com.paywithease.product.api;

import com.paywithease.product.application.ProductService;
import com.paywithease.product.domain.Product;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Product/service catalog API. */
@RestController
@RequestMapping("/api/v1/products")
@Tag(name = "products", description = "Product & service catalog with HSN/SAC, GST, and pricing")
public class ProductController {

  private final ProductService service;

  public ProductController(ProductService service) {
    this.service = service;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Create a catalog product or service")
  public ProductDtos.ProductResponse create(@Valid @RequestBody ProductDtos.CreateProduct body) {
    Product p =
        service.create(
            body.name(),
            body.type(),
            body.hsnSac(),
            body.gstRate(),
            body.unit(),
            body.salePriceMinor(),
            body.purchasePriceMinor(),
            body.marginDefault());
    return toResponse(p);
  }

  @GetMapping
  @Operation(summary = "List the tenant's catalog, newest first")
  public List<ProductDtos.ProductResponse> list() {
    return service.list().stream().map(this::toResponse).toList();
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get a single catalog line")
  public ProductDtos.ProductResponse get(@PathVariable String id) {
    return toResponse(service.get(id));
  }

  private ProductDtos.ProductResponse toResponse(Product p) {
    return new ProductDtos.ProductResponse(
        p.getId(),
        p.getName(),
        p.getType(),
        p.getHsnSac(),
        p.getGstRate(),
        p.getUnit(),
        p.getSalePriceMinor(),
        p.getPurchasePriceMinor(),
        p.getMarginDefault());
  }
}
