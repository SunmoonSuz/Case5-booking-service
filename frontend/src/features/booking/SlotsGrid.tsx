import React from 'react';
import { AvailableSlotDto } from '@/types/api';
import { Clock, CalendarX2 } from 'lucide-react';
import { Skeleton } from '@/shared/components/Skeleton';
import { formatTimeRange } from '@/shared/utils/dateUtils';

export interface SlotsGridProps {
  slots: AvailableSlotDto[];
  isLoading: boolean;
  selectedSlot: AvailableSlotDto | null;
  onSelectSlot: (slot: AvailableSlotDto) => void;
}

export const SlotsGrid: React.FC<SlotsGridProps> = ({
  slots,
  isLoading,
  selectedSlot,
  onSelectSlot,
}) => {
  if (isLoading) {
    return (
      <div className="space-y-3">
        <div className="flex items-center gap-2 text-sm font-semibold text-slate-800">
          <Clock className="w-4 h-4 text-teal-600 animate-spin" />
          <span>Поиск доступного времени...</span>
        </div>
        <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-6 gap-2.5">
          {Array.from({ length: 8 }).map((_, i) => (
            <Skeleton key={i} className="h-12 w-full rounded-xl" />
          ))}
        </div>
      </div>
    );
  }

  if (slots.length === 0) {
    return (
      <div className="p-8 text-center bg-slate-50 border border-slate-200 rounded-2xl">
        <div className="w-12 h-12 rounded-xl bg-amber-50 text-amber-600 flex items-center justify-center mx-auto mb-3">
          <CalendarX2 className="w-6 h-6" />
        </div>
        <h4 className="text-base font-semibold text-slate-800 mb-1">
          Нет свободных слотов
        </h4>
        <p className="text-xs sm:text-sm text-slate-500 max-w-sm mx-auto">
          На эту дату у специалиста нет свободных часов или смена закончилась.
          Пожалуйста, выберите другой день в календаре.
        </p>
      </div>
    );
  }

  return (
    <div className="space-y-3">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2 text-sm font-semibold text-slate-800">
          <Clock className="w-4 h-4 text-teal-600" />
          <span>Свободное время ({slots.length} слотов)</span>
        </div>
      </div>

      <div
        className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-6 gap-2.5"
        role="group"
        aria-label="Сетка доступных слотов времени"
      >
        {slots.map((slot) => {
          const isSelected = selectedSlot?.startTime === slot.startTime;
          const displayLabel = slot.formattedTime || formatTimeRange(slot.startTime, slot.endTime);

          return (
            <button
              key={slot.startTime}
              type="button"
              onClick={() => onSelectSlot(slot)}
              className={`px-3 py-3 rounded-xl border text-sm font-semibold transition-all duration-150 flex items-center justify-center min-h-[44px] min-w-[44px] focus:outline-none focus:ring-2 focus:ring-teal-500 ${
                isSelected
                  ? 'bg-teal-600 text-white border-teal-600 shadow-md ring-2 ring-teal-600/30'
                  : 'bg-white border-slate-200 text-slate-800 hover:border-teal-400 hover:bg-teal-50/40 hover:text-teal-800'
              }`}
            >
              {displayLabel}
            </button>
          );
        })}
      </div>
    </div>
  );
};
