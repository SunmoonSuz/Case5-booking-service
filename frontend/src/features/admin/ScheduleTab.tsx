import React, { useState, useEffect } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { adminApi } from '@/shared/api/adminApi';
import { ScheduleIntervalDto } from '@/types/api';
import { useToast } from '@/shared/context/ToastContext';
import { getErrorMessage } from '@/shared/api/client';
import { DAYS_OF_WEEK } from '@/shared/utils/formatters';
import { isValidInterval } from '@/shared/utils/dateUtils';
import { Button } from '@/shared/components/Button';
import { Skeleton } from '@/shared/components/Skeleton';
import { Calendar, Plus, Trash2, Clock, Check, AlertCircle } from 'lucide-react';

interface DaySchedule {
  dayOfWeek: number;
  intervals: { startTime: string; endTime: string }[];
}

export const ScheduleTab: React.FC = () => {
  const queryClient = useQueryClient();
  const { success: toastSuccess, error: toastError } = useToast();

  const [userSelectedMasterId, setUserSelectedMasterId] = useState<string>('');
  const [scheduleState, setScheduleState] = useState<DaySchedule[]>(() =>
    DAYS_OF_WEEK.map((d) => ({
      dayOfWeek: d.id,
      intervals: [],
    }))
  );
  const [validationErrors, setValidationErrors] = useState<Record<string, string>>({});
  const [isSaving, setIsSaving] = useState(false);

  // 1. Fetch masters list
  const { data: masters = [], isLoading: isMastersLoading } = useQuery({
    queryKey: ['adminMasters'],
    queryFn: () => adminApi.getAllMasters(),
  });

  const selectedMasterId = userSelectedMasterId || masters[0]?.id || '';

  // 2. Fetch schedule for selected master
  const {
    data: fetchedIntervals,
    isLoading: isScheduleLoading,
  } = useQuery({
    queryKey: ['adminSchedule', selectedMasterId],
    queryFn: () => adminApi.getMasterSchedule(selectedMasterId),
    enabled: !!selectedMasterId,
  });

  // Transform backend list of intervals into day-by-day structure
  useEffect(() => {
    if (!fetchedIntervals) return;

    const newSchedule: DaySchedule[] = DAYS_OF_WEEK.map((d) => {
      const dayIntervals = fetchedIntervals
        .filter((i) => i.dayOfWeek === d.id)
        .map((i) => ({
          startTime: i.startTime.substring(0, 5),
          endTime: i.endTime.substring(0, 5),
        }));
      return {
        dayOfWeek: d.id,
        intervals: dayIntervals,
      };
    });
    setScheduleState(newSchedule);
    setValidationErrors({});
  }, [fetchedIntervals]);

  // Add interval for a day
  const handleAddInterval = (dayOfWeek: number) => {
    setScheduleState((prev) =>
      prev.map((day) => {
        if (day.dayOfWeek !== dayOfWeek) return day;
        // Default new interval
        const defaultStart = day.intervals.length === 0 ? '09:00' : '14:00';
        const defaultEnd = day.intervals.length === 0 ? '13:00' : '18:00';
        return {
          ...day,
          intervals: [...day.intervals, { startTime: defaultStart, endTime: defaultEnd }],
        };
      })
    );
  };

  // Remove interval
  const handleRemoveInterval = (dayOfWeek: number, index: number) => {
    setScheduleState((prev) =>
      prev.map((day) => {
        if (day.dayOfWeek !== dayOfWeek) return day;
        const updated = [...day.intervals];
        updated.splice(index, 1);
        return {
          ...day,
          intervals: updated,
        };
      })
    );
  };

  // Update interval times
  const handleIntervalChange = (
    dayOfWeek: number,
    index: number,
    field: 'startTime' | 'endTime',
    value: string
  ) => {
    setScheduleState((prev) => {
      const updatedDays = prev.map((day) => {
        if (day.dayOfWeek !== dayOfWeek) return day;
        const updated = [...day.intervals];
        updated[index] = {
          ...updated[index],
          [field]: value,
        };
        return {
          ...day,
          intervals: updated,
        };
      });
      validateSchedule(updatedDays);
      return updatedDays;
    });
  };

  // Validate all intervals
  const validateSchedule = (stateToValidate: DaySchedule[] = scheduleState): boolean => {
    const errors: Record<string, string> = {};

    for (const day of stateToValidate) {
      for (let i = 0; i < day.intervals.length; i++) {
        const current = day.intervals[i];
        const key = `${day.dayOfWeek}-${i}`;

        // 1. Check start < end
        if (!isValidInterval(current.startTime, current.endTime)) {
          errors[key] = `Время начала (${current.startTime}) должно быть раньше окончания (${current.endTime})`;
          continue;
        }

        // 2. Check overlap with other intervals on same day
        for (let j = i + 1; j < day.intervals.length; j++) {
          const other = day.intervals[j];
          const [curStartH, curStartM] = current.startTime.split(':').map(Number);
          const [curEndH, curEndM] = current.endTime.split(':').map(Number);
          const [othStartH, othStartM] = other.startTime.split(':').map(Number);
          const [othEndH, othEndM] = other.endTime.split(':').map(Number);

          const curStart = curStartH * 60 + curStartM;
          const curEnd = curEndH * 60 + curEndM;
          const othStart = othStartH * 60 + othStartM;
          const othEnd = othEndH * 60 + othEndM;

          if (Math.max(curStart, othStart) < Math.min(curEnd, othEnd)) {
            errors[key] = `Интервал перекрывается с другим интервалом (${other.startTime}–${other.endTime})`;
            break;
          }
        }
      }
    }

    setValidationErrors(errors);
    return Object.keys(errors).length === 0;
  };

  // Save to backend
  const handleSaveSchedule = async () => {
    if (!selectedMasterId) return;
    if (!validateSchedule()) {
      toastError('Исправьте ошибки в интервалах расписания перед сохранением');
      return;
    }

    setIsSaving(true);
    try {
      const flattenedIntervals: ScheduleIntervalDto[] = [];
      scheduleState.forEach((day) => {
        day.intervals.forEach((interval) => {
          flattenedIntervals.push({
            dayOfWeek: day.dayOfWeek,
            startTime: interval.startTime,
            endTime: interval.endTime,
          });
        });
      });

      await adminApi.setMasterSchedule(selectedMasterId, {
        intervals: flattenedIntervals,
      });

      toastSuccess('График смен мастера успешно сохранен');
      await queryClient.invalidateQueries({ queryKey: ['adminSchedule', selectedMasterId] });
      await queryClient.invalidateQueries({ queryKey: ['availability'] });
    } catch (err) {
      toastError(getErrorMessage(err, 'Не удалось сохранить график смен'));
    } finally {
      setIsSaving(false);
    }
  };

  return (
    <div className="space-y-6">
      {/* Header and Master Selector */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 bg-white p-5 rounded-2xl border border-slate-200 shadow-sm">
        <div className="space-y-1">
          <h2 className="text-xl font-bold text-slate-900 flex items-center gap-2">
            <Calendar className="w-5 h-5 text-teal-600" />
            Расписание смен мастера
          </h2>
          <p className="text-xs sm:text-sm text-slate-500">
            Настройте рабочие интервалы по дням недели (1..7, Пн-Вс). Поддерживаются обеденные перерывы.
          </p>
        </div>

        <div className="flex items-center gap-3">
          <div className="w-64">
            <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1">
              Выберите мастера
            </label>
            <select
              value={selectedMasterId}
              onChange={(e) => setUserSelectedMasterId(e.target.value)}
              disabled={isMastersLoading}
              className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-sm font-semibold focus:outline-none focus:ring-2 focus:ring-teal-500 min-h-[44px]"
            >
              {masters.map((m) => (
                <option key={m.id} value={m.id}>
                  {m.name} {!m.isActive ? '(неактивен)' : ''}
                </option>
              ))}
            </select>
          </div>

          <div className="self-end">
            <Button
              variant="primary"
              size="md"
              onClick={handleSaveSchedule}
              isLoading={isSaving}
              disabled={!selectedMasterId || isScheduleLoading}
              icon={<Check className="w-4 h-4" />}
            >
              Сохранить график
            </Button>
          </div>
        </div>
      </div>

      {/* Days of Week Editor */}
      {isScheduleLoading ? (
        <div className="space-y-3">
          <Skeleton className="h-24 w-full rounded-2xl" count={4} />
        </div>
      ) : !selectedMasterId ? (
        <div className="p-8 text-center bg-slate-50 border border-slate-200 rounded-2xl text-slate-500">
          Выберите мастера для настройки расписания.
        </div>
      ) : (
        <div className="space-y-3">
          {DAYS_OF_WEEK.map((dayDef) => {
            const daySchedule = scheduleState.find((d) => d.dayOfWeek === dayDef.id);
            const intervals = daySchedule?.intervals ?? [];
            const isDayOff = intervals.length === 0;

            return (
              <div
                key={dayDef.id}
                className={`p-4 sm:p-5 rounded-2xl border transition-all ${
                  isDayOff
                    ? 'bg-slate-50/70 border-slate-200/60'
                    : 'bg-white border-slate-200 shadow-sm'
                }`}
              >
                <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 mb-3">
                  <div className="flex items-center gap-3">
                    <div
                      className={`w-9 h-9 rounded-xl flex items-center justify-center font-bold text-sm ${
                        isDayOff
                          ? 'bg-slate-200 text-slate-600'
                          : 'bg-teal-600 text-white shadow-sm'
                      }`}
                    >
                      {dayDef.short}
                    </div>
                    <div>
                      <h4 className="font-bold text-slate-900 text-base">{dayDef.name}</h4>
                      <span className="text-xs text-slate-500">
                        {isDayOff
                          ? 'Выходной день'
                          : `Рабочий день (${intervals.length} смен/интервалов)`}
                      </span>
                    </div>
                  </div>

                  <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    onClick={() => handleAddInterval(dayDef.id)}
                    icon={<Plus className="w-3.5 h-3.5" />}
                  >
                    Добавить интервал
                  </Button>
                </div>

                {/* Intervals List */}
                {intervals.length > 0 && (
                  <div className="space-y-2 pt-2 border-t border-slate-100">
                    {intervals.map((interval, idx) => {
                      const errorKey = `${dayDef.id}-${idx}`;
                      const errorMsg = validationErrors[errorKey];

                      return (
                        <div key={idx} className="space-y-1">
                          <div className="flex flex-wrap items-center gap-2.5">
                            <div className="flex items-center gap-2 bg-slate-50 p-2 rounded-xl border border-slate-200">
                              <Clock className="w-4 h-4 text-slate-400" />
                              <span className="text-xs text-slate-500 font-medium">С:</span>
                              <input
                                type="time"
                                value={interval.startTime}
                                onChange={(e) =>
                                  handleIntervalChange(
                                    dayDef.id,
                                    idx,
                                    'startTime',
                                    e.target.value
                                  )
                                }
                                className="px-3 py-2 bg-white border border-slate-300 rounded-xl text-sm font-semibold focus:outline-none focus:ring-1 focus:ring-teal-500 min-h-[44px]"
                              />

                              <span className="text-xs text-slate-500 font-medium ml-1">По:</span>
                              <input
                                type="time"
                                value={interval.endTime}
                                onChange={(e) =>
                                  handleIntervalChange(dayDef.id, idx, 'endTime', e.target.value)
                                }
                                className="px-3 py-2 bg-white border border-slate-300 rounded-xl text-sm font-semibold focus:outline-none focus:ring-1 focus:ring-teal-500 min-h-[44px]"
                              />
                            </div>

                            <button
                              type="button"
                              onClick={() => handleRemoveInterval(dayDef.id, idx)}
                              className="text-slate-400 hover:text-rose-600 hover:bg-rose-50 p-2 rounded-xl transition min-h-[44px] min-w-[44px] flex items-center justify-center"
                              title="Удалить этот интервал"
                              aria-label="Удалить интервал смены"
                            >
                              <Trash2 className="w-4 h-4" />
                            </button>
                          </div>

                          {errorMsg && (
                            <div className="flex items-center gap-1.5 text-xs text-rose-600 font-medium pl-2">
                              <AlertCircle className="w-3.5 h-3.5 flex-shrink-0" />
                              <span>{errorMsg}</span>
                            </div>
                          )}
                        </div>
                      );
                    })}
                  </div>
                )}
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
};
