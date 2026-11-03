import React from 'react';
import { Modal } from '@/shared/components/Modal';
import { Button } from '@/shared/components/Button';
import { BookingResponse } from '@/types/api';
import { formatDateTime } from '@/shared/utils/dateUtils';
import { AlertTriangle } from 'lucide-react';

export interface CancelBookingModalProps {
  isOpen: boolean;
  onClose: () => void;
  booking: BookingResponse | null;
  onConfirm: () => Promise<void>;
  isLoading: boolean;
}

export const CancelBookingModal: React.FC<CancelBookingModalProps> = ({
  isOpen,
  onClose,
  booking,
  onConfirm,
  isLoading,
}) => {
  if (!booking) return null;

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title="Отмена записи"
      description="Подтвердите отмену предстоящего визита"
      maxWidth="md"
    >
      <div className="space-y-4">
        <div className="flex items-start gap-3 p-3.5 rounded-2xl bg-rose-50 border border-rose-200 text-rose-900 text-sm">
          <AlertTriangle className="w-5 h-5 text-rose-600 flex-shrink-0 mt-0.5" />
          <div className="leading-snug">
            Вы уверены, что хотите отменить запись? Слот освободится для бронирования другими клиентами.
          </div>
        </div>

        <div className="bg-slate-50 border border-slate-200/80 rounded-xl p-4 space-y-2 text-xs">
          <div className="flex justify-between">
            <span className="text-slate-500">Услуга:</span>
            <span className="font-semibold text-slate-800">{booking.serviceName}</span>
          </div>
          <div className="flex justify-between">
            <span className="text-slate-500">Специалист:</span>
            <span className="font-semibold text-slate-800">{booking.masterName}</span>
          </div>
          <div className="flex justify-between">
            <span className="text-slate-500">Время визита:</span>
            <span className="font-semibold text-slate-800">
              {formatDateTime(booking.startTime)}
            </span>
          </div>
        </div>

        <div className="flex items-center justify-end gap-2.5 pt-2">
          <Button type="button" variant="outline" onClick={onClose} disabled={isLoading}>
            Оставить запись
          </Button>
          <Button
            type="button"
            variant="danger"
            onClick={onConfirm}
            isLoading={isLoading}
          >
            Да, отменить
          </Button>
        </div>
      </div>
    </Modal>
  );
};
