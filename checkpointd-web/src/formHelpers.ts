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

export function optionalRating(value: string) {
  return value.trim() ? Number(value) : null;
}

export function ratingValidationMessage(value: string) {
  const trimmed = value.trim();

  if (!trimmed) {
    return null;
  }

  const rating = Number(trimmed);
  if (!Number.isInteger(rating) || rating < 1 || rating > 10) {
    return 'Rating must be a whole number from 1 to 10.';
  }

  return null;
}

export function displayDate(value?: string | null) {
  if (!value) {
    return 'Not set';
  }

  const dateOnly = /^(\d{4})-(\d{2})-(\d{2})$/.exec(value);
  const date = dateOnly
    ? new Date(Number(dateOnly[1]), Number(dateOnly[2]) - 1, Number(dateOnly[3]))
    : new Date(value);

  return new Intl.DateTimeFormat(undefined, {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  }).format(date);
}
