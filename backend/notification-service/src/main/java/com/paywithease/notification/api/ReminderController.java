package com.paywithease.notification.api;

import com.paywithease.common.error.ApiException;
import com.paywithease.common.error.ErrorCode;
import com.paywithease.common.tenant.TenantContext;
import com.paywithease.notification.application.NotificationService;
import com.paywithease.notification.domain.Channel;
import com.paywithease.notification.infrastructure.ReminderScheduleRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Schedules D-3/D-1/D-day payment reminders and lists a source's scheduled reminders. */
@RestController
@RequestMapping("/api/v1/reminders")
@Tag(name = "reminders", description = "D-3/D-1/D-day payment reminders")
public class ReminderController {

  private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

  private final NotificationService service;
  private final ReminderScheduleRepository reminders;

  public ReminderController(NotificationService service, ReminderScheduleRepository reminders) {
    this.service = service;
    this.reminders = reminders;
  }

  @PostMapping
  @Operation(summary = "Schedule reminders for a due date")
  public NotificationDtos.CountResponse schedule(
      @Valid @RequestBody NotificationDtos.ScheduleReminderRequest body) {
    if (!Channel.isValid(body.channel())) {
      throw new ApiException(ErrorCode.VALIDATION_FAILED, "Unknown channel: " + body.channel());
    }
    int created =
        service.scheduleReminders(
            body.sourceType(),
            body.sourceRef(),
            body.emiNumber(),
            Channel.valueOf(body.channel()),
            body.templateCode(),
            body.recipient(),
            body.variables() == null ? Map.of() : body.variables(),
            body.dueDate(),
            body.offsets(),
            LocalDate.now(IST));
    return new NotificationDtos.CountResponse(created);
  }

  @GetMapping
  @Operation(summary = "List scheduled reminders for a source reference")
  public List<NotificationDtos.ReminderResponse> list(@RequestParam String sourceRef) {
    return reminders
        .findByTenantIdAndSourceRefOrderBySendOn(TenantContext.requireTenantId(), sourceRef)
        .stream()
        .map(
            r ->
                new NotificationDtos.ReminderResponse(
                    r.getId(), r.getSendOn(), r.getOffsetDays(), r.getStatus()))
        .toList();
  }
}
