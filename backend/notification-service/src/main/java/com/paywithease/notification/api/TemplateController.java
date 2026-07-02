package com.paywithease.notification.api;

import com.paywithease.common.error.ApiException;
import com.paywithease.common.error.ErrorCode;
import com.paywithease.common.ids.Ulid;
import com.paywithease.common.tenant.TenantContext;
import com.paywithease.notification.domain.Channel;
import com.paywithease.notification.domain.NotificationTemplate;
import com.paywithease.notification.infrastructure.NotificationTemplateRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Clock;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Per-tenant notification template management. */
@RestController
@RequestMapping("/api/v1/notification-templates")
@Tag(name = "notification-templates", description = "Per-tenant, per-channel message templates")
public class TemplateController {

  private final NotificationTemplateRepository templates;
  private final Clock clock;

  public TemplateController(NotificationTemplateRepository templates, Clock clock) {
    this.templates = templates;
    this.clock = clock;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Create a notification template")
  public NotificationDtos.TemplateResponse create(
      @Valid @RequestBody NotificationDtos.CreateTemplateRequest body) {
    if (!Channel.isValid(body.channel())) {
      throw new ApiException(ErrorCode.VALIDATION_FAILED, "Unknown channel: " + body.channel());
    }
    NotificationTemplate template =
        new NotificationTemplate(
            Ulid.newId(),
            TenantContext.requireTenantId(),
            body.code(),
            Channel.valueOf(body.channel()),
            body.subject(),
            body.body(),
            clock.instant());
    return toResponse(templates.save(template));
  }

  @GetMapping
  @Operation(summary = "List templates for the current tenant")
  public List<NotificationDtos.TemplateResponse> list() {
    return templates.findByTenantId(TenantContext.requireTenantId()).stream()
        .map(TemplateController::toResponse)
        .toList();
  }

  private static NotificationDtos.TemplateResponse toResponse(NotificationTemplate t) {
    return new NotificationDtos.TemplateResponse(
        t.getId(), t.getCode(), t.getChannel(), t.getSubject(), t.getBody(), t.isActive());
  }
}
