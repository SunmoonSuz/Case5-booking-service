import { format, parseISO, isAfter } from 'date-fns';

export const VENUE_TIMEZONE = 'Europe/Moscow';

export function formatISODate(date: Date): string {
  return format(date, 'yyyy-MM-dd');
}

export function formatTimeInVenue(dateInput: string | Date): string {
  const date = typeof dateInput === 'string' ? parseISO(dateInput) : dateInput;
  return new Intl.DateTimeFormat('ru-RU', {
    timeZone: VENUE_TIMEZONE,
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  }).format(date);
}

export const formatTime = formatTimeInVenue;

export function formatTimeRange(startTimeIso: string, endTimeIso: string): string {
  try {
    const startStr = formatTimeInVenue(startTimeIso);
    const endStr = formatTimeInVenue(endTimeIso);
    return `${startStr} – ${endStr}`;
  } catch {
    return `${startTimeIso} – ${endTimeIso}`;
  }
}

export function formatDateTime(dateTimeIso: string): string {
  try {
    const date = parseISO(dateTimeIso);
    return new Intl.DateTimeFormat('ru-RU', {
      timeZone: VENUE_TIMEZONE,
      day: 'numeric',
      month: 'long',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
      hour12: false,
    }).format(date);
  } catch {
    return dateTimeIso;
  }
}

export function formatDisplayDate(dateInput: string | Date): string {
  try {
    const date = typeof dateInput === 'string' ? parseISO(dateInput) : dateInput;
    return new Intl.DateTimeFormat('ru-RU', {
      timeZone: VENUE_TIMEZONE,
      day: 'numeric',
      month: 'long',
      year: 'numeric',
      weekday: 'long',
    }).format(date);
  } catch {
    return String(dateInput);
  }
}

export function formatShortDate(dateInput: string | Date): string {
  try {
    const date = typeof dateInput === 'string' ? parseISO(dateInput) : dateInput;
    return new Intl.DateTimeFormat('ru-RU', {
      timeZone: VENUE_TIMEZONE,
      day: 'numeric',
      month: 'short',
      weekday: 'short',
    }).format(date);
  } catch {
    return String(dateInput);
  }
}

export function isUpcoming(startTimeIso: string): boolean {
  try {
    const start = parseISO(startTimeIso);
    return isAfter(start, new Date());
  } catch {
    return false;
  }
}

export function getHorizonDates(days = 30): { date: Date; dateStr: string }[] {
  const list = [];
  const today = new Date();
  today.setHours(0, 0, 0, 0);

  for (let i = 0; i < days; i++) {
    const d = new Date(today);
    d.setDate(today.getDate() + i);
    list.push({
      date: d,
      dateStr: formatISODate(d),
    });
  }
  return list;
}

export function isValidInterval(startTime: string, endTime: string): boolean {
  if (!startTime || !endTime) return false;
  const [startH, startM] = startTime.split(':').map(Number);
  const [endH, endM] = endTime.split(':').map(Number);
  if (isNaN(startH) || isNaN(startM) || isNaN(endH) || isNaN(endM)) return false;
  const startMinutes = startH * 60 + startM;
  const endMinutes = endH * 60 + endM;
  return startMinutes < endMinutes;
}
