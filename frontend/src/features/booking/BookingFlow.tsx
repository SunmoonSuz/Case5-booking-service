import React, { useState, useEffect } from 'react';
import axios from 'axios';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { catalogApi } from '@/shared/api/catalogApi';
import { bookingApi } from '@/shared/api/bookingApi';
import { ServiceDto, MasterDto, AvailableSlotDto } from '@/types/api';
import { useAuth } from '@/shared/context/AuthContext';
import { useToast } from '@/shared/context/ToastContext';
import { getApiError, getErrorMessage } from '@/shared/api/client';
import { formatISODate } from '@/shared/utils/dateUtils';
import { ServiceCard } from '@/features/catalog/ServiceCard';
import { MasterCard } from '@/features/catalog/MasterCard';
import { DatePickerHorizon } from './DatePickerHorizon';
import { SlotsGrid } from './SlotsGrid';
import { BookingConfirmModal } from './BookingConfirmModal';
import { AuthModal } from '@/features/auth/AuthModal';
import { Spinner } from '@/shared/components/Spinner';
import { useNavigate } from 'react-router-dom';

export const BookingFlow: React.FC = () => {
  const queryClient = useQueryClient();
  const { user, isAuthenticated } = useAuth();
  const { error: toastError, success: toastSuccess } = useToast();
  const navigate = useNavigate();

  // Selection states
  const [selectedService, setSelectedService] = useState<ServiceDto | null>(null);
  const [selectedMaster, setSelectedMaster] = useState<MasterDto | null>(null);
  const [selectedDate, setSelectedDate] = useState<string>(() => formatISODate(new Date()));
  const [selectedSlot, setSelectedSlot] = useState<AvailableSlotDto | null>(null);

  // Modals
  const [isConfirmModalOpen, setIsConfirmModalOpen] = useState(false);
  const [isAuthModalOpen, setIsAuthModalOpen] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);

  // 1. Fetch active services
  const {
    data: services = [],
    isLoading: isServicesLoading,
  } = useQuery({
    queryKey: ['services'],
    queryFn: () => catalogApi.getActiveServices(),
  });

  // Automatically select first service if none selected and services loaded
  useEffect(() => {
    if (!selectedService && services.length > 0) {
      setSelectedService(services[0]);
    }
  }, [services, selectedService]);

  // 2. Fetch active masters for selected service
  const {
    data: masters = [],
    isLoading: isMastersLoading,
  } = useQuery({
    queryKey: ['masters', selectedService?.id],
    queryFn: () => catalogApi.getActiveMasters(selectedService?.id),
    enabled: !!selectedService,
  });

  // Automatically select first master when masters list changes
  useEffect(() => {
    if (masters.length > 0) {
      if (!selectedMaster || !masters.some((m) => m.id === selectedMaster.id)) {
        setSelectedMaster(masters[0]);
      }
    } else {
      setSelectedMaster(null);
    }
  }, [masters, selectedMaster]);

  // 3. Fetch available slots for selected service + master + date
  const {
    data: slots = [],
    isLoading: isSlotsLoading,
  } = useQuery({
    queryKey: ['availability', selectedService?.id, selectedDate, selectedMaster?.id],
    queryFn: () =>
      catalogApi.getAvailability({
        serviceId: selectedService!.id,
        date: selectedDate,
        masterId: selectedMaster?.id,
      }),
    enabled: !!selectedService && !!selectedMaster && !!selectedDate,
  });

  // Handle slot click
  const handleSelectSlot = (slot: AvailableSlotDto) => {
    setSelectedSlot(slot);
    if (!isAuthenticated) {
      // Guest: open AuthModal to login or register on the fly (SCN-001 Alt 2)
      setIsAuthModalOpen(true);
    } else {
      // Authenticated: open confirmation modal
      setIsConfirmModalOpen(true);
    }
  };

  // Called after successful inline login/registration
  const handleAuthSuccess = () => {
    setIsAuthModalOpen(false);
    if (selectedSlot) {
      setIsConfirmModalOpen(true);
    }
  };

  // Submit booking creation
  const handleConfirmBooking = async () => {
    if (!selectedService || !selectedMaster || !selectedSlot) return;

    setIsSubmitting(true);
    try {
      const response = await bookingApi.createBooking({
        serviceId: selectedService.id,
        masterId: selectedMaster.id,
        startTime: selectedSlot.startTime,
      });

      toastSuccess(
        `Заявка №${response.id.substring(0, 8)} успешно создана! Ожидает подтверждения.`,
        'Успешная запись'
      );

      // Invalidate slots cache and client bookings
      await queryClient.invalidateQueries({
        queryKey: ['availability', selectedService.id, selectedDate, selectedMaster.id],
      });
      await queryClient.invalidateQueries({ queryKey: ['clientBookings'] });

      setIsConfirmModalOpen(false);
      setSelectedSlot(null);

      // Navigate to client cabinet
      navigate('/cabinet');
    } catch (err: unknown) {
      const apiError = getApiError(err);
      const is409 =
        (axios.isAxiosError(err) && err.response?.status === 409) ||
        (typeof err === 'object' &&
          err !== null &&
          'response' in err &&
          (err as { response?: { status?: number } }).response?.status === 409);

      // CRITICAL REQUIREMENT AC-004:
      // При получении ошибки 409 Conflict (SLOT_ALREADY_BOOKED)
      // UI показывает Toast-уведомление и автоматически инвалидирует кэш слотов через queryClient.invalidateQueries
      if (apiError?.error === 'SLOT_ALREADY_BOOKED' || is409) {
        toastError(
          'Выбранный слот уже занят другим клиентом. Пожалуйста, выберите другое время.',
          'Слот занят'
        );

        // Immediate cache invalidation without page reload
        await queryClient.invalidateQueries({
          queryKey: ['availability', selectedService.id, selectedDate, selectedMaster.id],
        });

        setSelectedSlot(null);
        setIsConfirmModalOpen(false);
      } else {
        const errorMsg = getErrorMessage(err, 'Не удалось оформить запись. Попробуйте снова.');
        toastError(errorMsg);
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="space-y-8" id="booking-section">
      {/* Step 1: Select Service */}
      <section className="bg-white rounded-3xl p-6 sm:p-8 border border-slate-200/80 shadow-sm space-y-4">
        <div className="flex items-center gap-2.5 pb-2 border-b border-slate-100">
          <div className="w-8 h-8 rounded-lg bg-teal-600 text-white flex items-center justify-center font-bold text-sm">
            1
          </div>
          <div>
            <h2 className="text-lg sm:text-xl font-bold text-slate-900">
              Шаг 1. Выберите услугу
            </h2>
            <p className="text-xs sm:text-sm text-slate-500">
              Длительность 30 или 60 минут, фиксированная стоимость в рублях
            </p>
          </div>
        </div>

        {isServicesLoading ? (
          <div className="py-8 flex justify-center">
            <Spinner label="Загрузка каталога услуг..." />
          </div>
        ) : services.length === 0 ? (
          <p className="text-sm text-slate-500 py-4">Нет активных услуг в каталоге.</p>
        ) : (
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4 pt-2">
            {services.map((service) => (
              <ServiceCard
                key={service.id}
                service={service}
                isSelected={selectedService?.id === service.id}
                onSelect={(svc) => {
                  setSelectedService(svc);
                  setSelectedSlot(null);
                }}
              />
            ))}
          </div>
        )}
      </section>

      {/* Step 2: Select Master */}
      {selectedService && (
        <section className="bg-white rounded-3xl p-6 sm:p-8 border border-slate-200/80 shadow-sm space-y-4 animate-in fade-in duration-300">
          <div className="flex items-center gap-2.5 pb-2 border-b border-slate-100">
            <div className="w-8 h-8 rounded-lg bg-teal-600 text-white flex items-center justify-center font-bold text-sm">
              2
            </div>
            <div>
              <h2 className="text-lg sm:text-xl font-bold text-slate-900">
                Шаг 2. Выберите специалиста
              </h2>
              <p className="text-xs sm:text-sm text-slate-500">
                Специалисты, оказывающие услугу: «{selectedService.name}»
              </p>
            </div>
          </div>

          {isMastersLoading ? (
            <div className="py-8 flex justify-center">
              <Spinner label="Загрузка специалистов..." />
            </div>
          ) : masters.length === 0 ? (
            <div className="p-6 text-center text-sm text-slate-500 bg-slate-50 rounded-2xl border border-slate-200">
              К сожалению, для выбранной услуги пока нет назначенных специалистов.
            </div>
          ) : (
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4 pt-2">
              {masters.map((master) => (
                <MasterCard
                  key={master.id}
                  master={master}
                  isSelected={selectedMaster?.id === master.id}
                  selectedServiceId={selectedService.id}
                  onSelect={(m) => {
                    setSelectedMaster(m);
                    setSelectedSlot(null);
                  }}
                />
              ))}
            </div>
          )}
        </section>
      )}

      {/* Step 3: Date & Time Selection */}
      {selectedService && selectedMaster && (
        <section className="bg-white rounded-3xl p-6 sm:p-8 border border-slate-200/80 shadow-sm space-y-6 animate-in fade-in duration-300">
          <div className="flex items-center gap-2.5 pb-2 border-b border-slate-100">
            <div className="w-8 h-8 rounded-lg bg-teal-600 text-white flex items-center justify-center font-bold text-sm">
              3
            </div>
            <div>
              <h2 className="text-lg sm:text-xl font-bold text-slate-900">
                Шаг 3. Выберите дату и время визита
              </h2>
              <p className="text-xs sm:text-sm text-slate-500">
                Специалист: {selectedMaster.name} • Услуга: {selectedService.name} (
                {selectedService.durationMinutes} мин)
              </p>
            </div>
          </div>

          {/* Interactive Date Horizon */}
          <DatePickerHorizon
            selectedDate={selectedDate}
            onSelectDate={(date) => {
              setSelectedDate(date);
              setSelectedSlot(null);
            }}
          />

          {/* Slots Grid */}
          <SlotsGrid
            slots={slots}
            isLoading={isSlotsLoading}
            selectedSlot={selectedSlot}
            onSelectSlot={handleSelectSlot}
          />
        </section>
      )}

      {/* Booking Confirmation Modal */}
      <BookingConfirmModal
        isOpen={isConfirmModalOpen}
        onClose={() => setIsConfirmModalOpen(false)}
        service={selectedService}
        master={selectedMaster}
        slot={selectedSlot}
        user={user}
        onConfirm={handleConfirmBooking}
        isLoading={isSubmitting}
      />

      {/* Auth Modal for Guests */}
      <AuthModal
        isOpen={isAuthModalOpen}
        onClose={() => setIsAuthModalOpen(false)}
        onSuccess={handleAuthSuccess}
        title="Авторизация для оформления записи"
        subtitle="Войдите или зарегистрируйтесь, чтобы завершить бронирование"
      />
    </div>
  );
};
