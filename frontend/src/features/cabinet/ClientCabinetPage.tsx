import React, { useState } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { bookingApi } from '@/shared/api/bookingApi';
import { BookingResponse } from '@/types/api';
import { useToast } from '@/shared/context/ToastContext';
import { getErrorMessage } from '@/shared/api/client';
import { BookingCard } from './BookingCard';
import { CancelBookingModal } from './CancelBookingModal';
import { Skeleton } from '@/shared/components/Skeleton';
import { EmptyState } from '@/shared/components/EmptyState';
import { Calendar, History, ArrowRight } from 'lucide-react';
import { Button } from '@/shared/components/Button';
import { Link } from 'react-router-dom';

export const ClientCabinetPage: React.FC = () => {
  const queryClient = useQueryClient();
  const { success: toastSuccess, error: toastError } = useToast();
  const [activeTab, setActiveTab] = useState<'active' | 'archive'>('active');

  const [bookingToCancel, setBookingToCancel] = useState<BookingResponse | null>(null);
  const [isCancelling, setIsCancelling] = useState(false);

  const {
    data: bookingsData,
    isLoading,
    isError,
  } = useQuery({
    queryKey: ['clientBookings'],
    queryFn: () => bookingApi.getClientBookings(),
  });

  const activeBookings = bookingsData?.active ?? [];
  const archiveBookings = bookingsData?.archive ?? [];

  const handleConfirmCancel = async () => {
    if (!bookingToCancel) return;

    setIsCancelling(true);
    try {
      await bookingApi.cancelClientBooking(bookingToCancel.id);
      toastSuccess('Запись успешно отменена', 'Бронирование аннулировано');

      // Invalidate queries to refresh lists and free up slots
      await queryClient.invalidateQueries({ queryKey: ['clientBookings'] });
      await queryClient.invalidateQueries({ queryKey: ['availability'] });

      setBookingToCancel(null);
    } catch (err) {
      const msg = getErrorMessage(err, 'Не удалось отменить запись');
      toastError(msg);
    } finally {
      setIsCancelling(false);
    }
  };

  return (
    <div className="max-w-4xl mx-auto py-8 px-4 sm:px-6 space-y-6">
      {/* Page Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-4 border-b border-slate-200">
        <div>
          <h1 className="text-2xl sm:text-3xl font-extrabold text-slate-900 tracking-tight">
            Личный кабинет
          </h1>
          <p className="text-sm text-slate-500 mt-1">
            Управляйте предстоящими визитами и просматривайте историю записей
          </p>
        </div>
        <Link to="/">
          <Button variant="primary" size="md" icon={<ArrowRight className="w-4 h-4" />}>
            Новая запись
          </Button>
        </Link>
      </div>

      {/* Tabs */}
      <div className="flex border-b border-slate-200 gap-4">
        <button
          type="button"
          onClick={() => setActiveTab('active')}
          className={`flex items-center gap-2 py-3 px-2 text-sm font-semibold border-b-2 transition min-h-[44px] ${
            activeTab === 'active'
              ? 'border-teal-600 text-teal-700'
              : 'border-transparent text-slate-500 hover:text-slate-800'
          }`}
        >
          <Calendar className="w-4 h-4" />
          <span>Предстоящие визиты</span>
          {activeBookings.length > 0 && (
            <span className="ml-1.5 px-2 py-0.5 rounded-full text-xs bg-teal-100 text-teal-800 font-bold">
              {activeBookings.length}
            </span>
          )}
        </button>

        <button
          type="button"
          onClick={() => setActiveTab('archive')}
          className={`flex items-center gap-2 py-3 px-2 text-sm font-semibold border-b-2 transition min-h-[44px] ${
            activeTab === 'archive'
              ? 'border-teal-600 text-teal-700'
              : 'border-transparent text-slate-500 hover:text-slate-800'
          }`}
        >
          <History className="w-4 h-4" />
          <span>История визитов</span>
          {archiveBookings.length > 0 && (
            <span className="ml-1.5 px-2 py-0.5 rounded-full text-xs bg-slate-200 text-slate-700 font-bold">
              {archiveBookings.length}
            </span>
          )}
        </button>
      </div>

      {/* Content */}
      {isLoading ? (
        <div className="space-y-4">
          <Skeleton className="h-32 w-full rounded-2xl" count={3} />
        </div>
      ) : isError ? (
        <div className="p-8 text-center bg-rose-50 border border-rose-200 rounded-2xl text-rose-800">
          <p className="font-semibold">Не удалось загрузить список записей.</p>
          <p className="text-xs mt-1 text-rose-600">Пожалуйста, попробуйте обновить страницу.</p>
        </div>
      ) : activeTab === 'active' ? (
        activeBookings.length === 0 ? (
          <EmptyState
            icon={Calendar}
            title="Нет предстоящих визитов"
            description="У вас пока нет активных записей. Выберите услугу и удобное время в каталоге."
            actionLabel="Выбрать услугу"
            onAction={() => (window.location.href = '/')}
          />
        ) : (
          <div className="space-y-4">
            {activeBookings.map((b) => (
              <BookingCard
                key={b.id}
                booking={b}
                onCancelRequest={(booking) => setBookingToCancel(booking)}
              />
            ))}
          </div>
        )
      ) : archiveBookings.length === 0 ? (
        <EmptyState
          icon={History}
          title="История визитов пуста"
          description="Здесь будут отображаться ваши завершенные и отмененные записи."
        />
      ) : (
        <div className="space-y-4">
          {archiveBookings.map((b) => (
            <BookingCard key={b.id} booking={b} />
          ))}
        </div>
      )}

      {/* Cancel Modal */}
      <CancelBookingModal
        isOpen={!!bookingToCancel}
        onClose={() => setBookingToCancel(null)}
        booking={bookingToCancel}
        onConfirm={handleConfirmCancel}
        isLoading={isCancelling}
      />
    </div>
  );
};
