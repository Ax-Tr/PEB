package com.paywithease.invoice.application;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;

/**
 * Result of building a compliance payload (e-invoice / e-way bill) in READINESS-ONLY mode: the
 * representative JSON plus any fields that must be supplied before it could actually be filed.
 */
public record Readiness(boolean ready, List<String> missingFields, ObjectNode payload) {}
