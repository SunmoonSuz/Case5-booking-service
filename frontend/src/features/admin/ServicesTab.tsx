import React, { useState } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { adminApi } from '@/shared/api/adminApi';
import { ServiceDto, CreateServiceRequest, UpdateServiceRequest } from '@/types/api';
import { useToast } from '@/shared/context/ToastContext';
import { getErrorMessage } from '@/shared/api/client';
import { formatPrice } from '@/shared/utils/formatters';
import { Button } from '@/shared/components/Button';
import { Modal } from '@/shared/components/Modal';
import { Badge } from '@/shared/components/Badge';
import { Skeleton } from '@/shared/components/Skeleton';
import { Plus, Edit2, Trash2, Clock } from 'lucide-react';

export const ServicesTab: React.FC = () => {
  const queryClient = useQueryClient();
  const { success: toastSuccess, error: toastError } = useToast();

  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingService, setEditingService] = useState<ServiceDto | null>(null);

  // Form states
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [durationMinutes, setDurationMinutes] = useState<number>(60);
  const [price, setPrice] = useState<string>('1500');
  const [isActive, setIsActive] = useState<boolean>(true);
  const [errors, setErrors] = useState<{ name?: string; price?: string }>({});
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Fetch all services
  const {
    data: services = [],
    isLoading,
    isError,
  } = useQuery({
    queryKey: ['adminServices'],
    queryFn: () => adminApi.getAllServices(),
  });

  const openCreateModal = () => {
    setEditingService(null);
    setName('');
    setDescription('');
    setDurationMinutes(60);
    setPrice('1500');
    setIsActive(true);
    setErrors({});
    setIsModalOpen(true);
  };

  const openEditModal = (service: ServiceDto) => {
    setEditingService(service);
    setName(service.name);
    setDescription(service.description || '');
    setDurationMinutes(service.durationMinutes);
    setPrice(service.price.toString());
    setIsActive(service.isActive);
    setErrors({});
    setIsModalOpen(true);
  };

  const validate = () => {
    const errs: { name?: string; price?: string } = {};
    if (!name.trim()) {
      errs.name = 'Название услуги обязательно';
    } else if (name.trim().length > 100) {
      errs.name = 'Не более 100 символов';
    }

    const priceNum = parseFloat(price);
    if (isNaN(priceNum) || priceNum < 0) {
      errs.price = 'Цена должна быть положительным числом';
    }

    setErrors(errs);
    return Object.keys(errs).length === 0;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!validate()) return;

    setIsSubmitting(true);
    try {
      const priceNum = parseFloat(price);

      if (editingService) {
        const updateData: UpdateServiceRequest = {
          name: name.trim(),
          description: description.trim() || null,
          durationMinutes,
          price: priceNum,
          isActive,
        };
        await adminApi.updateService(editingService.id, updateData);
        toastSuccess('Услуга успешно обновлена');
      } else {
        const createData: CreateServiceRequest = {
          name: name.trim(),
          description: description.trim() || null,
          durationMinutes,
          price: priceNum,
        };
        await adminApi.createService(createData);
        toastSuccess('Услуга успешно создана');
      }

      await queryClient.invalidateQueries({ queryKey: ['adminServices'] });
      await queryClient.invalidateQueries({ queryKey: ['services'] });
      setIsModalOpen(false);
    } catch (err) {
      toastError(getErrorMessage(err, 'Ошибка при сохранении услуги'));
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleDeactivate = async (id: string) => {
    if (!window.confirm('Деактивировать данную услугу? Она перестанет отображаться в публичном каталоге.')) {
      return;
    }
    try {
      await adminApi.deleteService(id);
      toastSuccess('Услуга деактивирована');
      await queryClient.invalidateQueries({ queryKey: ['adminServices'] });
      await queryClient.invalidateQueries({ queryKey: ['services'] });
    } catch (err) {
      toastError(getErrorMessage(err, 'Не удалось деактивировать услугу'));
    }
  };

  return (
    <div className="space-y-6">
      {/* Header and Add Button */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-xl font-bold text-slate-900">Каталог услуг</h2>
          <p className="text-xs sm:text-sm text-slate-500">
            Управление перечнем услуг, длительностью (30/60 мин) и стоимостью в рублях
          </p>
        </div>
        <Button
          variant="primary"
          size="sm"
          onClick={openCreateModal}
          icon={<Plus className="w-4 h-4" />}
        >
          Добавить услугу
        </Button>
      </div>

      {/* Services List / Table */}
      {isLoading ? (
        <div className="space-y-3">
          <Skeleton className="h-16 w-full rounded-2xl" count={4} />
        </div>
      ) : isError ? (
        <div className="p-8 text-center bg-rose-50 border border-rose-200 rounded-2xl text-rose-800">
          Ошибка при загрузке услуг.
        </div>
      ) : services.length === 0 ? (
        <div className="p-8 text-center bg-slate-50 border border-slate-200 rounded-2xl text-slate-500">
          Услуг пока нет. Нажмите «Добавить услугу», чтобы создать первую.
        </div>
      ) : (
        <div className="bg-white border border-slate-200 rounded-2xl overflow-hidden shadow-sm">
          <div className="divide-y divide-slate-100">
            {services.map((svc) => (
              <div
                key={svc.id}
                className="p-4 sm:p-5 flex flex-col sm:flex-row sm:items-center justify-between gap-3 hover:bg-slate-50 transition"
              >
                <div className="space-y-1">
                  <div className="flex items-center gap-2">
                    <h3 className="font-bold text-slate-900 text-base">{svc.name}</h3>
                    <Badge variant={svc.isActive ? 'success' : 'neutral'} size="sm">
                      {svc.isActive ? 'Активна' : 'Деактивирована'}
                    </Badge>
                  </div>
                  {svc.description && (
                    <p className="text-xs text-slate-500 line-clamp-1">{svc.description}</p>
                  )}
                  <div className="flex items-center gap-3 text-xs text-slate-600 font-medium pt-1">
                    <span className="flex items-center gap-1">
                      <Clock className="w-3.5 h-3.5 text-slate-400" />
                      {svc.durationMinutes} мин
                    </span>
                    <span>•</span>
                    <span className="font-bold text-teal-700">{formatPrice(svc.price)}</span>
                  </div>
                </div>

                <div className="flex items-center gap-2 self-end sm:self-center">
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => openEditModal(svc)}
                    icon={<Edit2 className="w-3.5 h-3.5" />}
                  >
                    Редактировать
                  </Button>
                  {svc.isActive && (
                    <Button
                      variant="ghost"
                      size="sm"
                      onClick={() => handleDeactivate(svc.id)}
                      className="text-rose-600 hover:text-rose-700 hover:bg-rose-50"
                      icon={<Trash2 className="w-3.5 h-3.5" />}
                      aria-label="Деактивировать услугу"
                    >
                      Деактивировать
                    </Button>
                  )}
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Create / Edit Modal */}
      <Modal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        title={editingService ? 'Редактирование услуги' : 'Новая услуга'}
        description="Заполните параметры услуги"
        maxWidth="md"
      >
        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1">
              Название услуги *
            </label>
            <input
              type="text"
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="Например, Модельная стрижка"
              className={`w-full px-3.5 py-2.5 bg-slate-50 border rounded-xl text-sm focus:outline-none focus:ring-2 min-h-[44px] ${
                errors.name
                  ? 'border-rose-300 focus:ring-rose-400'
                  : 'border-slate-200 focus:ring-teal-500'
              }`}
            />
            {errors.name && <p className="text-xs text-rose-600 mt-1">{errors.name}</p>}
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1">
              Описание (необязательно)
            </label>
            <textarea
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="Краткое описание услуги..."
              rows={2}
              className="w-full px-3.5 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-teal-500"
            />
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1">
                Длительность *
              </label>
              <div className="grid grid-cols-2 gap-2">
                <button
                  type="button"
                  onClick={() => setDurationMinutes(30)}
                  className={`py-2 rounded-xl text-xs font-bold border transition min-h-[44px] ${
                    durationMinutes === 30
                      ? 'bg-teal-600 text-white border-teal-600'
                      : 'bg-slate-50 text-slate-700 border-slate-200 hover:bg-slate-100'
                  }`}
                >
                  30 мин
                </button>
                <button
                  type="button"
                  onClick={() => setDurationMinutes(60)}
                  className={`py-2 rounded-xl text-xs font-bold border transition min-h-[44px] ${
                    durationMinutes === 60
                      ? 'bg-teal-600 text-white border-teal-600'
                      : 'bg-slate-50 text-slate-700 border-slate-200 hover:bg-slate-100'
                  }`}
                >
                  60 мин
                </button>
              </div>
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1">
                Цена (RUB) *
              </label>
              <input
                type="number"
                step="50"
                min="0"
                value={price}
                onChange={(e) => setPrice(e.target.value)}
                placeholder="1500"
                className={`w-full px-3.5 py-2 bg-slate-50 border rounded-xl text-sm focus:outline-none focus:ring-2 min-h-[44px] ${
                  errors.price
                    ? 'border-rose-300 focus:ring-rose-400'
                    : 'border-slate-200 focus:ring-teal-500'
                }`}
              />
              {errors.price && <p className="text-xs text-rose-600 mt-1">{errors.price}</p>}
            </div>
          </div>

          {editingService && (
            <div className="pt-2">
              <label className="inline-flex items-center gap-2 cursor-pointer min-h-[44px]">
                <input
                  type="checkbox"
                  checked={isActive}
                  onChange={(e) => setIsActive(e.target.checked)}
                  className="w-4 h-4 text-teal-600 rounded border-slate-300 focus:ring-teal-500"
                />
                <span className="text-sm font-medium text-slate-700">Активна в каталоге</span>
              </label>
            </div>
          )}

          <div className="flex items-center justify-end gap-2.5 pt-3 border-t border-slate-100">
            <Button
              type="button"
              variant="outline"
              onClick={() => setIsModalOpen(false)}
              disabled={isSubmitting}
            >
              Отмена
            </Button>
            <Button type="submit" variant="primary" isLoading={isSubmitting}>
              Сохранить
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  );
};
