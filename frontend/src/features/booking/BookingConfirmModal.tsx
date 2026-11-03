import React from 'react';
import { Modal } from '@/shared/components/Modal';
import { Button } from '@/shared/components/Button';
import { ServiceDto, MasterDto, AvailableSlotDto, User } from '@/types/api';
import { formatPrice } from '@/shared/utils/formatters';
import { formatDateTime } from '@/shared/utils/dateUtils';
import { User as UserIcon, Scissors, CheckCircle, Info } from 'lucide-react';

export interface BookingConfirmModalProps {
  isOpen: boolean;
  onClose: () => void;
  service: ServiceDto | null;
  master: MasterDto | null;
  slot: AvailableSlotDto | null;
  user: User | null;
  onConfirm: () => Promise<void>;
  isLoading: boolean;
}

export const BookingConfirmModal: React.FC<BookingConfirmModalProps> = ({
  isOpen,
  onClose,
  service,
  master,
  slot,
  user,
  onConfirm,
  isLoading,
}) => {
  if (!service || !master || !slot) return null;

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title="Подтверждение записи"
      description="Проверьте детали перед отправкой заявки"
      maxWidth="md"
    >
      <div className="space-y-4">
        {/* Booking Details Card */}
        <div className="bg-slate-50 border border-slate-200/80 rounded-2xl p-4 space-y-3">
          <div className="flex items-start gap-3">
            <div className="w-10 h-10 rounded-xl bg-teal-100 text-teal-800 flex items-center justify-center flex-shrink-0">
              <Scissors className="w-5 h-5" />
            </div>
            <div className="flex-1">
              <h4 className="text-sm font-bold text-slate-900">{service.name}</h4>
              <p className="text-xs text-slate-500">
                Длительность: {service.durationMinutes} мин
              </p>
            </div>
            <div className="text-right">
              <span className="text-sm font-extrabold text-teal-700">
                {formatPrice(service.price)}
              </span>
            </div>
          </div>

          <div className="pt-2 border-t border-slate-200/60 grid grid-cols-2 gap-3 text-xs">
            <div>
              <span className="text-slate-400 block mb-0.5">Специалист</span>
              <span className="font-semibold text-slate-800">{master.name}</span>
            </div>
            <div>
              <span className="text-slate-400 block mb-0.5">Дата и время</span>
              <span className="font-semibold text-slate-800">
                {formatDateTime(slot.startTime)}
              </span>
            </div>
          </div>
        </div>

        {/* Client Contacts */}
        {user && (
          <div className="p-3.5 bg-white border border-slate-200 rounded-xl space-y-1.5 text-xs">
            <div className="font-semibold text-slate-700 flex items-center gap-1.5 mb-1">
              <UserIcon className="w-3.5 h-3.5 text-teal-600" />
              <span>Данные клиента</span>
            </div>
            <div className="text-slate-600">
              <span className="text-slate-400">Имя:</span> {user.name}
            </div>
            <div className="text-slate-600">
              <span className="text-slate-400">Телефон:</span> {user.phone}
            </div>
            <div className="text-slate-600">
              <span className="text-slate-400">Email:</span> {user.email}
            </div>
          </div>
        )}

        {/* Info Note */}
        <div className="flex items-start gap-2.5 p-3 rounded-xl bg-amber-50 border border-amber-200 text-amber-900 text-xs leading-relaxed">
          <Info className="w-4 h-4 text-amber-600 flex-shrink-0 mt-0.5" />
          <div>
            Заявка оформляется в статусе <strong>Ожидает подтверждения</strong>.
            Вы можете отменить её в любой момент до наступления визита через Личный кабинет.
          </div>
        </div>

        {/* Action Buttons */}
        <div className="flex items-center justify-end gap-2.5 pt-2">
          <Button
            type="button"
            variant="outline"
            onClick={onClose}
            disabled={isLoading}
          >
            Назад
          </Button>
          <Button
            type="button"
            variant="primary"
            onClick={onConfirm}
            isLoading={isLoading}
            icon={<CheckCircle className="w-4 h-4" />}
          >
            Записаться
          </Button>
        </div>
      </div>
    </Modal>
  );
};
