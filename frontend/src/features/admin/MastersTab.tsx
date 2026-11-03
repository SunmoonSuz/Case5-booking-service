import React, { useState } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { adminApi } from '@/shared/api/adminApi';
import { MasterDto, CreateMasterRequest, UpdateMasterRequest } from '@/types/api';
import { useToast } from '@/shared/context/ToastContext';
import { getErrorMessage } from '@/shared/api/client';
import { Button } from '@/shared/components/Button';
import { Modal } from '@/shared/components/Modal';
import { Badge } from '@/shared/components/Badge';
import { Skeleton } from '@/shared/components/Skeleton';
import { Plus, Edit2, Trash2, Scissors, Check } from 'lucide-react';

export const MastersTab: React.FC = () => {
  const queryClient = useQueryClient();
  const { success: toastSuccess, error: toastError } = useToast();

  const [isMasterModalOpen, setIsMasterModalOpen] = useState(false);
  const [editingMaster, setEditingMaster] = useState<MasterDto | null>(null);

  // Form states for master CRUD
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [isActive, setIsActive] = useState(true);
  const [errors, setErrors] = useState<{ name?: string }>({});
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Assign Services Modal state
  const [isAssignModalOpen, setIsAssignModalOpen] = useState(false);
  const [selectedMasterForServices, setSelectedMasterForServices] = useState<MasterDto | null>(null);
  const [selectedServiceIds, setSelectedServiceIds] = useState<string[]>([]);
  const [isAssigning, setIsAssigning] = useState(false);

  // Fetch all masters
  const {
    data: masters = [],
    isLoading: isMastersLoading,
    isError: isMastersError,
  } = useQuery({
    queryKey: ['adminMasters'],
    queryFn: () => adminApi.getAllMasters(),
  });

  // Fetch all services
  const { data: services = [] } = useQuery({
    queryKey: ['adminServices'],
    queryFn: () => adminApi.getAllServices(),
  });

  const openCreateModal = () => {
    setEditingMaster(null);
    setName('');
    setDescription('');
    setIsActive(true);
    setErrors({});
    setIsMasterModalOpen(true);
  };

  const openEditModal = (master: MasterDto) => {
    setEditingMaster(master);
    setName(master.name);
    setDescription(master.description || '');
    setIsActive(master.isActive);
    setErrors({});
    setIsMasterModalOpen(true);
  };

  const openAssignModal = (master: MasterDto) => {
    setSelectedMasterForServices(master);
    setSelectedServiceIds(master.services.map((s) => s.id));
    setIsAssignModalOpen(true);
  };

  const validate = () => {
    const errs: { name?: string } = {};
    if (!name.trim()) {
      errs.name = 'Имя мастера обязательно';
    } else if (name.trim().length > 100) {
      errs.name = 'Не более 100 символов';
    }
    setErrors(errs);
    return Object.keys(errs).length === 0;
  };

  const handleSubmitMaster = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!validate()) return;

    setIsSubmitting(true);
    try {
      if (editingMaster) {
        const updateData: UpdateMasterRequest = {
          name: name.trim(),
          description: description.trim() || null,
          isActive,
        };
        await adminApi.updateMaster(editingMaster.id, updateData);
        toastSuccess('Данные мастера обновлены');
      } else {
        const createData: CreateMasterRequest = {
          name: name.trim(),
          description: description.trim() || null,
        };
        await adminApi.createMaster(createData);
        toastSuccess('Мастер успешно добавлен');
      }

      await queryClient.invalidateQueries({ queryKey: ['adminMasters'] });
      await queryClient.invalidateQueries({ queryKey: ['masters'] });
      setIsMasterModalOpen(false);
    } catch (err) {
      toastError(getErrorMessage(err, 'Ошибка при сохранении мастера'));
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleDeactivate = async (id: string) => {
    if (!window.confirm('Деактивировать мастера? Проверьте, нет ли у него предстоящих активных записей.')) {
      return;
    }
    try {
      await adminApi.deleteMaster(id);
      toastSuccess('Мастер деактивирован');
      await queryClient.invalidateQueries({ queryKey: ['adminMasters'] });
      await queryClient.invalidateQueries({ queryKey: ['masters'] });
    } catch (err) {
      toastError(getErrorMessage(err, 'Не удалось деактивировать мастера'));
    }
  };

  const handleSaveServices = async () => {
    if (!selectedMasterForServices) return;

    setIsAssigning(true);
    try {
      await adminApi.assignServices(selectedMasterForServices.id, selectedServiceIds);
      toastSuccess('Услуги мастера успешно обновлены');
      await queryClient.invalidateQueries({ queryKey: ['adminMasters'] });
      await queryClient.invalidateQueries({ queryKey: ['masters'] });
      setIsAssignModalOpen(false);
    } catch (err) {
      toastError(getErrorMessage(err, 'Ошибка при привязке услуг'));
    } finally {
      setIsAssigning(false);
    }
  };

  const toggleServiceId = (serviceId: string) => {
    setSelectedServiceIds((prev) =>
      prev.includes(serviceId) ? prev.filter((id) => id !== serviceId) : [...prev, serviceId]
    );
  };

  return (
    <div className="space-y-6">
      {/* Header and Add Button */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-xl font-bold text-slate-900">Специалисты</h2>
          <p className="text-xs sm:text-sm text-slate-500">
            Управление мастерами и привязка оказываемых услуг
          </p>
        </div>
        <Button
          variant="primary"
          size="sm"
          onClick={openCreateModal}
          icon={<Plus className="w-4 h-4" />}
        >
          Добавить мастера
        </Button>
      </div>

      {/* Masters List */}
      {isMastersLoading ? (
        <div className="space-y-3">
          <Skeleton className="h-20 w-full rounded-2xl" count={3} />
        </div>
      ) : isMastersError ? (
        <div className="p-8 text-center bg-rose-50 border border-rose-200 rounded-2xl text-rose-800">
          Ошибка при загрузке специалистов.
        </div>
      ) : masters.length === 0 ? (
        <div className="p-8 text-center bg-slate-50 border border-slate-200 rounded-2xl text-slate-500">
          Мастеров пока нет. Нажмите «Добавить мастера», чтобы создать первого.
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {masters.map((master) => (
            <div
              key={master.id}
              className="bg-white border border-slate-200 rounded-2xl p-5 shadow-sm space-y-4 flex flex-col justify-between"
            >
              <div>
                <div className="flex items-start justify-between gap-2 mb-2">
                  <div className="flex items-center gap-3">
                    <div className="w-12 h-12 rounded-xl bg-teal-100 text-teal-800 flex items-center justify-center font-bold text-lg flex-shrink-0">
                      {master.name.charAt(0).toUpperCase()}
                    </div>
                    <div>
                      <h3 className="font-bold text-slate-900 text-base">{master.name}</h3>
                      <Badge variant={master.isActive ? 'success' : 'neutral'} size="sm">
                        {master.isActive ? 'Активен' : 'Деактивирован'}
                      </Badge>
                    </div>
                  </div>
                </div>

                {master.description && (
                  <p className="text-xs sm:text-sm text-slate-500 line-clamp-2 mb-3">
                    {master.description}
                  </p>
                )}

                {/* Assigned Services */}
                <div className="space-y-1 pt-1">
                  <span className="text-[11px] font-semibold text-slate-400 uppercase tracking-wider block">
                    Оказываемые услуги ({master.services.length})
                  </span>
                  {master.services.length === 0 ? (
                    <p className="text-xs text-amber-600">Услуги не привязаны</p>
                  ) : (
                    <div className="flex flex-wrap gap-1.5 pt-1">
                      {master.services.map((svc) => (
                        <Badge key={svc.id} size="sm" variant="neutral">
                          {svc.name}
                        </Badge>
                      ))}
                    </div>
                  )}
                </div>
              </div>

              {/* Actions */}
              <div className="pt-3 border-t border-slate-100 flex flex-wrap items-center justify-end gap-2">
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => openAssignModal(master)}
                  icon={<Scissors className="w-3.5 h-3.5 text-teal-600" />}
                >
                  Услуги
                </Button>
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => openEditModal(master)}
                  icon={<Edit2 className="w-3.5 h-3.5" />}
                >
                  Изменить
                </Button>
                {master.isActive && (
                  <Button
                    variant="ghost"
                    size="sm"
                    onClick={() => handleDeactivate(master.id)}
                    className="text-rose-600 hover:text-rose-700 hover:bg-rose-50"
                    icon={<Trash2 className="w-3.5 h-3.5" />}
                    aria-label="Деактивировать мастера"
                  >
                    Деактивировать
                  </Button>
                )}
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Create / Edit Master Modal */}
      <Modal
        isOpen={isMasterModalOpen}
        onClose={() => setIsMasterModalOpen(false)}
        title={editingMaster ? 'Редактирование специалиста' : 'Новый специалист'}
        description="Заполните информацию о мастере"
        maxWidth="md"
      >
        <form onSubmit={handleSubmitMaster} className="space-y-4">
          <div>
            <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1">
              ФИО / Имя мастера *
            </label>
            <input
              type="text"
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="Анна Смирнова"
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
              Описание / Специализация
            </label>
            <textarea
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="Топ-стилист, стаж 7 лет..."
              rows={3}
              className="w-full px-3.5 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-teal-500"
            />
          </div>

          {editingMaster && (
            <div className="pt-2">
              <label className="inline-flex items-center gap-2 cursor-pointer min-h-[44px]">
                <input
                  type="checkbox"
                  checked={isActive}
                  onChange={(e) => setIsActive(e.target.checked)}
                  className="w-4 h-4 text-teal-600 rounded border-slate-300 focus:ring-teal-500"
                />
                <span className="text-sm font-medium text-slate-700">Активен в каталоге</span>
              </label>
            </div>
          )}

          <div className="flex items-center justify-end gap-2.5 pt-3 border-t border-slate-100">
            <Button
              type="button"
              variant="outline"
              onClick={() => setIsMasterModalOpen(false)}
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

      {/* Assign Services Modal */}
      <Modal
        isOpen={isAssignModalOpen}
        onClose={() => setIsAssignModalOpen(false)}
        title={`Услуги мастера: ${selectedMasterForServices?.name}`}
        description="Отметьте галочками услуги, которые оказывает данный специалист"
        maxWidth="md"
      >
        <div className="space-y-4">
          <div className="max-h-60 overflow-y-auto space-y-2 pr-1">
            {services.map((service) => {
              const checked = selectedServiceIds.includes(service.id);
              return (
                <label
                  key={service.id}
                  className={`flex items-center justify-between p-3 rounded-xl border cursor-pointer transition min-h-[44px] ${
                    checked
                      ? 'bg-teal-50/70 border-teal-400 text-teal-900'
                      : 'bg-white border-slate-200 hover:bg-slate-50 text-slate-700'
                  }`}
                >
                  <div className="flex items-center gap-2.5">
                    <input
                      type="checkbox"
                      checked={checked}
                      onChange={() => toggleServiceId(service.id)}
                      className="w-4 h-4 text-teal-600 rounded border-slate-300 focus:ring-teal-500"
                    />
                    <span className="text-sm font-medium">{service.name}</span>
                  </div>
                  <span className="text-xs text-slate-500 font-semibold">
                    {service.durationMinutes} мин
                  </span>
                </label>
              );
            })}
          </div>

          <div className="flex items-center justify-end gap-2.5 pt-3 border-t border-slate-100">
            <Button
              type="button"
              variant="outline"
              onClick={() => setIsAssignModalOpen(false)}
              disabled={isAssigning}
            >
              Отмена
            </Button>
            <Button
              type="button"
              variant="primary"
              onClick={handleSaveServices}
              isLoading={isAssigning}
              icon={<Check className="w-4 h-4" />}
            >
              Сохранить привязку
            </Button>
          </div>
        </div>
      </Modal>
    </div>
  );
};
