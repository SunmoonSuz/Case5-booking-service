import React, { useRef } from 'react';
import { getHorizonDates } from '@/shared/utils/dateUtils';
import { ChevronLeft, ChevronRight, Calendar as CalendarIcon } from 'lucide-react';
import { format } from 'date-fns';
import { ru } from 'date-fns/locale';

export interface DatePickerHorizonProps {
  selectedDate: string; // YYYY-MM-DD
  onSelectDate: (dateStr: string) => void;
}

export const DatePickerHorizon: React.FC<DatePickerHorizonProps> = ({
  selectedDate,
  onSelectDate,
}) => {
  const dates = getHorizonDates(30);
  const scrollContainerRef = useRef<HTMLDivElement>(null);

  const scroll = (direction: 'left' | 'right') => {
    if (scrollContainerRef.current) {
      const offset = direction === 'left' ? -240 : 240;
      scrollContainerRef.current.scrollBy({ left: offset, behavior: 'smooth' });
    }
  };

  return (
    <div className="space-y-3">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2 text-sm font-semibold text-slate-800">
          <CalendarIcon className="w-4 h-4 text-teal-600" />
          <span>Выберите дату (горизонт 30 дней)</span>
        </div>
        <div className="flex items-center gap-1">
          <button
            type="button"
            onClick={() => scroll('left')}
            className="p-1.5 rounded-lg border border-slate-200 text-slate-500 hover:text-slate-900 hover:bg-slate-100 min-h-[44px] min-w-[44px] flex items-center justify-center transition"
            aria-label="Прокрутить даты назад"
          >
            <ChevronLeft className="w-5 h-5" />
          </button>
          <button
            type="button"
            onClick={() => scroll('right')}
            className="p-1.5 rounded-lg border border-slate-200 text-slate-500 hover:text-slate-900 hover:bg-slate-100 min-h-[44px] min-w-[44px] flex items-center justify-center transition"
            aria-label="Прокрутить даты вперед"
          >
            <ChevronRight className="w-5 h-5" />
          </button>
        </div>
      </div>

      <div
        ref={scrollContainerRef}
        className="flex gap-2.5 overflow-x-auto pb-2 pt-1 scroll-smooth no-scrollbar"
        tabIndex={0}
        aria-label="Список доступных дат для записи"
      >
        {dates.map(({ date, dateStr }, index) => {
          const isSelected = selectedDate === dateStr;
          const isToday = index === 0;
          const dayOfWeek = format(date, 'EEE', { locale: ru });
          const dayOfMonth = format(date, 'd');
          const monthName = format(date, 'MMM', { locale: ru });

          return (
            <button
              key={dateStr}
              type="button"
              onClick={() => onSelectDate(dateStr)}
              className={`flex-shrink-0 flex flex-col items-center justify-center px-4 py-3 rounded-2xl border transition-all duration-150 min-h-[64px] min-w-[64px] text-center focus:outline-none focus:ring-2 focus:ring-teal-500 ${
                isSelected
                  ? 'bg-teal-600 text-white border-teal-600 shadow-md transform -translate-y-0.5'
                  : 'bg-white border-slate-200 text-slate-700 hover:border-teal-300 hover:bg-teal-50/30'
              }`}
            >
              <span
                className={`text-[11px] font-semibold uppercase tracking-wider ${
                  isSelected ? 'text-teal-100' : 'text-slate-400'
                }`}
              >
                {isToday ? 'Сегодня' : dayOfWeek}
              </span>
              <span className="text-lg font-bold leading-tight mt-0.5">{dayOfMonth}</span>
              <span
                className={`text-[10px] ${
                  isSelected ? 'text-teal-100' : 'text-slate-400'
                }`}
              >
                {monthName}
              </span>
            </button>
          );
        })}
      </div>
    </div>
  );
};
