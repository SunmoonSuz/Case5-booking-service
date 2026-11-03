import React from 'react';
import { BookingResponse } from '@/types/api';
import { formatPrice, getStatusLabel, getStatusBadgeVariant } from '@/shared/utils/formatters';
import { formatDateTime, isUpcoming } from '@/shared/utils/dateUtils';
import { Button } from '@/shared/components/Button';
import { Calendar, User, Clock, Scissors, XCircle } from 'lucide-react';

export interface BookingCardProps {
  booking: BookingResponse;
  onCancelRequest?: (booking: BookingResponse) => void;
}

export const BookingCard: React.FC<BookingCardProps> = ({
  booking,
  onCancelRequest,
}) => {
  const badgeStyle = getStatusBadgeVariant(booking.status);
  const statusLabel = getStatusLabel(booking.status);
  const canCancel =
    (booking.status === 'PENDING' || booking.status === 'CONFIRMED') &&
    isUpcoming(booking.startTime);

  return (
    <div className="bg-white border border-slate-200/90 rounded-2xl p-5 shadow-sm hover:shadow transition space-y-4">
      {/* Header: Service Name & Status */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 pb-3 border-b border-slate-100">
        <div>
          <span className="text-[11px] font-bold text-slate-400 uppercase tracking-wider block mb-0.5">
            Заявка #{booking.id.substring(0, 8)}
          </span>
          <h3 className="text-base sm:text-lg font-bold text-slate-900 flex items-center gap-2">
            <Scissors className="w-4 h-4 text-teal-600 flex-shrink-0" />
            {booking.serviceName}
          </h3>
        </div>
        <div className="self-start sm:self-center">
          <span
            className={`inline-flex items-center px-3 py-1 rounded-full text-xs font-semibold border ${badgeStyle.bg} ${badgeStyle.text} ${badgeStyle.border}`}
          >
            {statusLabel}
          </span>
        </div>
      </div>

      {/* Details Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 text-xs sm:text-sm">
        <div className="flex items-center gap-2 text-slate-700">
          <User className="w-4 h-4 text-slate-400 flex-shrink-0" />
          <div>
            <span className="text-slate-400 block text-[11px]">Мастер</span>
            <span className="font-semibold">{booking.masterName}</span>
          </div>
        </div>

        <div className="flex items-center gap-2 text-slate-700">
          <Calendar className="w-4 h-4 text-slate-400 flex-shrink-0" />
          <div>
            <span className="text-slate-400 block text-[11px]">Дата и время</span>
            <span className="font-semibold">{formatDateTime(booking.startTime)}</span>
          </div>
        </div>

        <div className="flex items-center gap-2 text-slate-700">
          <Clock className="w-4 h-4 text-slate-400 flex-shrink-0" />
          <div>
            <span className="text-slate-400 block text-[11px]">Стоимость</span>
            <span className="font-bold text-teal-700">
              {formatPrice(booking.servicePrice)}
            </span>
          </div>
        </div>
      </div>

      {/* Footer: Action Button */}
      {canCancel && onCancelRequest && (
        <div className="pt-3 border-t border-slate-100 flex items-center justify-end">
          <Button
            type="button"
            variant="outline"
            size="sm"
            onClick={() => onCancelRequest(booking)}
            className="text-rose-600 border-rose-200 hover:bg-rose-50 hover:border-rose-300 min-h-[44px]"
            icon={<XCircle className="w-4 h-4" />}
          >
            Отменить запись
          </Button>
        </div>
      )}
    </div>
  );
};
