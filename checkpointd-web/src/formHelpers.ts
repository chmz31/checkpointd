export function dateInputValue(value?: string | null) {
  return value ? value.slice(0, 10) : '';
}

export function optionalDate(value: string) {
  return value || null;
}

export function optionalNotes(value: string) {
  const trimmed = value.trim();
  return trimmed || null;
}

export function dateOrderValidationMessage(startedAt: string, completedAt: string) {
  if (!startedAt || !completedAt) {
    return null;
  }

  return completedAt < startedAt ? 'Completed date cannot be before started date.' : null;
}
