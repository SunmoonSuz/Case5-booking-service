import React, { useState } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { adminApi } from '@/shared/api/adminApi';
import { BookingStatus } from '@/types/api';
import { useToast } from '@/shared/context/ToastContext';
import { getErrorMessage } from '@/shared/api/client';
import { formatPrice, getStatusBadgeVariant, getStatusLabel } from '@/shared/utils/formatters';
import { formatDateTime } from '@/shared/utils/dateUtils';
import { Button } from '@/shared/components/Button';
import { Skeleton } from '@/shared/components/Skeleton';
import { EmptyState } from '@/shared/components/EmptyState';
import { Check, X, Ban, CheckCircle2, Filter, Phone, Mail, User } from 'lucide-react';

export const BookingsModerationTab: React.FC = () => {
  const queryClient = useQueryClient();
  const { success: toastSuccess, error: toastError } = useToast();

  const [statusFilter, setStatusFilter] = useState<BookingStatus | 'ALL'>('ALL');
  const [masterFilter, setMasterFilter] = useState<string>('ALL');
  const [dateFilter, setDateFilter] = useState<string>('');
  const [actionInProgressId, setActionInProgressId] = useState<string | null>(null);

  // Fetch masters for filter
  const { data: masters = [] } = useQuery({
    queryKey: ['adminMasters'],
    queryFn: () => adminApi.getAllMasters(),
  });

  // Fetch bookings with filters
  const {
    data: bookings = [],
    isLoading,
    isError,
  } = useQuery({
    queryKey: ['adminBookings', statusFilter, masterFilter, dateFilter],
    queryFn: () =>
      adminApi.getBookings({
        status: statusFilter === 'ALL' ? undefined : statusFilter,
        masterId: masterFilter === 'ALL' ? undefined : masterFilter,
        date: dateFilter || undefined,
      }),
  });

  const handleAction = async (
    bookingId: string,
    action: 'confirm' | 'reject' | 'cancel' | 'complete'
  ) => {
    setActionInProgressId(bookingId);
    try {
      if (action === 'confirm') {
        await adminApi.confirmBooking(bookingId);
        toastSuccess('Заявка успешно подтверждена', 'Статус: CONFIRMED');
      } else if (action === 'reject') {
        await adminApi.rejectBooking(bookingId);
        toastSuccess('Заявка отклонена, слот освобождён', 'Статус: REJECTED');
      } else if (action === 'cancel') {
        await adminApi.cancelBooking(bookingId);
        toastSuccess('Запись отменена администратором', 'Статус: CANCELLED');
      } else if (action === 'complete') {
        await adminApi.completeBooking(bookingId);
        toastSuccess('Визит успешно завершен', 'Статус: COMPLETED');
      }

      // Invalidate queries
      await queryClient.invalidateQueries({ queryKey: ['adminBookings'] });
      await queryClient.invalidateQueries({ queryKey: ['availability'] });
    } catch (err) {
      toastError(getErrorMessage(err, 'Не удалось изменить статус записи'));
    } finally {
      setActionInProgressId(null);
    }
  };

  return (
    <div className="space-y-6">
      {/* Filters Bar */}
      <div className="bg-white p-4 sm:p-5 rounded-2xl border border-slate-200/90 shadow-sm space-y-3">
        <div className="flex items-center gap-2 text-sm font-semibold text-slate-800 pb-2 border-b border-slate-100">
          <Filter className="w-4 h-4 text-teal-600" />
          <span>Фильтрация записей</span>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
          {/* Status filter */}
          <div>
            <label htmlFor="filter-status" className="block text-xs font-medium text-slate-600 mb-1">Статус</label>
            <select
              id="filter-status"
              aria-label="Фильтр по статусу"
              value={statusFilter}
              onChange={(e) => {
                const val = e.target.value;
                if (['ALL', 'PENDING', 'CONFIRMED', 'COMPLETED', 'CANCELLED', 'REJECTED'].includes(val)) {
                  setStatusFilter(val as BookingStatus | 'ALL');
                }
              }}
              className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-teal-500 min-h-[44px]"
            >
              <option value="ALL">Все статусы</option>
              <option value="PENDING">Ожидает подтверждения (PENDING)</option>
              <option value="CONFIRMED">Подтверждено (CONFIRMED)</option>
              <option value="COMPLETED">Завершено (COMPLETED)</option>
              <option value="CANCELLED">Отменено (CANCELLED)</option>
              <option value="REJECTED">Отклонено (REJECTED)</option>
            </select>
          </div>

          {/* Master filter */}
          <div>
            <label htmlFor="filter-master" className="block text-xs font-medium text-slate-600 mb-1">Специалист</label>
            <select
              id="filter-master"
              aria-label="Фильтр по специалисту"
              value={masterFilter}
              onChange={(e) => setMasterFilter(e.target.value)}
              className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-teal-500 min-h-[44px]"
            >
              <option value="ALL">Все специалисты</option>
              {masters.map((m) => (
                <option key={m.id} value={m.id}>
                  {m.name} {!m.isActive ? '(неактивен)' : ''}
                </option>
              ))}
            </select>
          </div>

          {/* Date filter */}
          <div>
            <label htmlFor="filter-date" className="block text-xs font-medium text-slate-600 mb-1">Дата визита</label>
            <div className="flex gap-2">
              <input
                id="filter-date"
                aria-label="Фильтр по дате"
                type="date"
                value={dateFilter}
                onChange={(e) => setDateFilter(e.target.value)}
                className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-teal-500 min-h-[44px]"
              />
              {dateFilter && (
                <Button
                  type="button"
                  variant="ghost"
                  size="sm"
                  onClick={() => setDateFilter('')}
                  aria-label="Сбросить фильтр даты"
                >
                  Сброс
                </Button>
              )}
            </div>
          </div>
        </div>
      </div>

      {/* Bookings Table / List */}
      {isLoading ? (
        <div className="space-y-3">
          <Skeleton className="h-28 w-full rounded-2xl" count={3} />
        </div>
      ) : isError ? (
        <div className="p-8 text-center bg-rose-50 border border-rose-200 rounded-2xl text-rose-800">
          Ошибка при загрузке заявок.
        </div>
      ) : bookings.length === 0 ? (
        <EmptyState
          title="Записей не найдено"
          description="По заданным фильтрам нет записей."
        />
      ) : (
        <div className="space-y-4">
          {bookings.map((b) => {
            const badge = getStatusBadgeVariant(b.status);
            const isPending = b.status === 'PENDING';
            const isConfirmed = b.status === 'CONFIRMED';
            const inProgress = actionInProgressId === b.id;

            return (
              <div
                key={b.id}
                className="bg-white border border-slate-200 rounded-2xl p-5 shadow-sm space-y-4"
              >
                <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 pb-3 border-b border-slate-100">
                  <div className="flex items-center gap-3">
                    <span
                      className={`inline-flex items-center px-2.5 py-1 rounded-full text-xs font-bold border ${badge.bg} ${badge.text} ${badge.border}`}
                    >
                      {getStatusLabel(b.status)}
                    </span>
                    <span className="text-xs text-slate-400">ID: {b.id.substring(0, 8)}</span>
                  </div>
                  <div className="text-xs text-slate-500 font-medium">
                    Создана: {formatDateTime(b.createdAt)}
                  </div>
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 text-xs sm:text-sm">
                  {/* Service & Master */}
                  <div className="space-y-1">
                    <div className="text-slate-400 text-[11px] font-semibold uppercase tracking-wider">
                      Услуга и мастер
                    </div>
                    <div className="font-bold text-slate-900">{b.serviceName}</div>
                    <div className="text-slate-600 font-medium">Мастер: {b.masterName}</div>
                    <div className="font-bold text-teal-700">{formatPrice(b.servicePrice)}</div>
                  </div>

                  {/* Visit Time */}
                  <div className="space-y-1">
                    <div className="text-slate-400 text-[11px] font-semibold uppercase tracking-wider">
                      Время визита
                    </div>
                    <div className="font-semibold text-slate-800">
                      {formatDateTime(b.startTime)}
                    </div>
                  </div>

                  {/* Client Contacts */}
                  <div className="space-y-1 bg-slate-50 p-3 rounded-xl border border-slate-100">
                    <div className="text-slate-400 text-[11px] font-semibold uppercase tracking-wider flex items-center gap-1">
                      <User className="w-3 h-3 text-teal-600" />
                      Клиент
                    </div>
                    <div className="font-semibold text-slate-900">{b.clientName}</div>
                    <div className="flex items-center gap-1.5 text-slate-600 text-xs">
                      <Phone className="w-3 h-3 text-slate-400" />
                      <a href={`tel:${b.clientPhone}`} className="hover:underline">
                        {b.clientPhone}
                      </a>
                    </div>
                    <div className="flex items-center gap-1.5 text-slate-600 text-xs">
                      <Mail className="w-3 h-3 text-slate-400" />
                      <a href={`mailto:${b.clientEmail}`} className="hover:underline">
                        {b.clientEmail}
                      </a>
                    </div>
                  </div>
                </div>

                {/* Moderation Actions Bar */}
                {(isPending || isConfirmed) && (
                  <div className="pt-3 border-t border-slate-100 flex flex-wrap items-center justify-end gap-2">
                    {isPending && (
                      <>
                        <Button
                          variant="danger"
                          size="sm"
                          onClick={() => handleAction(b.id, 'reject')}
                          isLoading={inProgress}
                          icon={<X className="w-4 h-4" />}
                        >
                          Отклонить
                        </Button>
                        <Button
                          variant="primary"
                          size="sm"
                          onClick={() => handleAction(b.id, 'confirm')}
                          isLoading={inProgress}
                          icon={<Check className="w-4 h-4" />}
                        >
                          Подтвердить
                        </Button>
                      </>
                    )}

                    {isConfirmed && (
                      <>
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() => handleAction(b.id, 'cancel')}
                          isLoading={inProgress}
                          icon={<Ban className="w-4 h-4 text-rose-600" />}
                        >
                          Отменить
                        </Button>
                        <Button
                          variant="primary"
                          size="sm"
                          onClick={() => handleAction(b.id, 'complete')}
                          isLoading={inProgress}
                          icon={<CheckCircle2 className="w-4 h-4" />}
                        >
                          Завершить визит
                        </Button>
                      </>
                    )}
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
