import { BookingStatus } from '@/types/api';

export function formatPrice(price: number | string): string {
  const num = typeof price === 'string' ? parseFloat(price) : price;
  if (isNaN(num)) return '0 ₽';
  return new Intl.NumberFormat('ru-RU', {
    style: 'currency',
    currency: 'RUB',
    maximumFractionDigits: 0,
  }).format(num);
}

export function getStatusLabel(status: BookingStatus): string {
  switch (status) {
    case 'PENDING':
      return 'Ожидает подтверждения';
    case 'CONFIRMED':
      return 'Подтверждено';
    case 'REJECTED':
      return 'Отклонено';
    case 'CANCELLED':
      return 'Отменено';
    case 'COMPLETED':
      return 'Завершено';
    default:
      return status;
  }
}

export function getStatusBadgeVariant(status: BookingStatus): {
  bg: string;
  text: string;
  border: string;
} {
  switch (status) {
    case 'PENDING':
      return {
        bg: 'bg-amber-50',
        text: 'text-amber-700',
        border: 'border-amber-200',
      };
    case 'CONFIRMED':
      return {
        bg: 'bg-emerald-50',
        text: 'text-emerald-700',
        border: 'border-emerald-200',
      };
    case 'REJECTED':
      return {
        bg: 'bg-rose-50',
        text: 'text-rose-700',
        border: 'border-rose-200',
      };
    case 'CANCELLED':
      return {
        bg: 'bg-slate-100',
        text: 'text-slate-600',
        border: 'border-slate-200',
      };
    case 'COMPLETED':
      return {
        bg: 'bg-sky-50',
        text: 'text-sky-700',
        border: 'border-sky-200',
      };
    default:
      return {
        bg: 'bg-slate-50',
        text: 'text-slate-700',
        border: 'border-slate-200',
      };
  }
}

export const DAYS_OF_WEEK = [
  { id: 1, name: 'Понедельник', short: 'Пн' },
  { id: 2, name: 'Вторник', short: 'Вт' },
  { id: 3, name: 'Среда', short: 'Ср' },
  { id: 4, name: 'Четверг', short: 'Чт' },
  { id: 5, name: 'Пятница', short: 'Пт' },
  { id: 6, name: 'Суббота', short: 'Сб' },
  { id: 7, name: 'Воскресенье', short: 'Вс' },
];

export function getDayName(dayOfWeek: number): string {
  return DAYS_OF_WEEK.find((d) => d.id === dayOfWeek)?.name ?? `День ${dayOfWeek}`;
}
