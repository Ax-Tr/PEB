import { theme } from "../../theme/theme";

/** Colour for a compliance report's display state (unreconciled/draft/reviewed/approved/filed). */
export function statusColor(displayState: string): string {
  switch (displayState) {
    case "FILED":
      return theme.color.success;
    case "APPROVED":
      return theme.color.primary;
    case "REVIEWED":
      return theme.color.warning;
    case "UNRECONCILED":
      return theme.color.danger;
    default:
      return theme.color.textMuted; // DRAFT
  }
}
