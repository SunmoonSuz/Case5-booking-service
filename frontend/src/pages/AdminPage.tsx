import React, { useState } from 'react';
import { BookingsModerationTab } from '@/features/admin/BookingsModerationTab';
import { ServicesTab } from '@/features/admin/ServicesTab';
import { MastersTab } from '@/features/admin/MastersTab';
import { ScheduleTab } from '@/features/admin/ScheduleTab';
import { Shield, Inbox, Scissors, Users, Calendar } from 'lucide-react';

type AdminTab = 'bookings' | 'services' | 'masters' | 'schedule';

export const AdminPage: React.FC = () => {
  const [activeTab, setActiveTab] = useState<AdminTab>('bookings');

  const tabs: { id: AdminTab; label: string; icon: React.ElementType }[] = [
    { id: 'bookings', label: 'Заявки и визиты', icon: Inbox },
    { id: 'services', label: 'Каталог услуг', icon: Scissors },
    { id: 'masters', label: 'Специалисты', icon: Users },
    { id: 'schedule', label: 'Расписание смен', icon: Calendar },
  ];

  return (
    <div className="max-w-7xl mx-auto py-8 px-4 sm:px-6 lg:px-8 space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-4 border-b border-slate-200">
        <div className="flex items-center gap-3">
          <div className="w-12 h-12 rounded-2xl bg-teal-600 text-white flex items-center justify-center shadow-sm">
            <Shield className="w-6 h-6" />
          </div>
          <div>
            <h1 className="text-2xl sm:text-3xl font-extrabold text-slate-900 tracking-tight">
              Панель управления администратора
            </h1>
            <p className="text-xs sm:text-sm text-slate-500">
              Модерация заявок, управление каталогом услуг, мастерами и расписанием смен
            </p>
          </div>
        </div>
      </div>

      {/* Tabs Bar */}
      <div className="flex flex-wrap border-b border-slate-200 gap-2 sm:gap-4">
        {tabs.map((tab) => {
          const Icon = tab.icon;
          const isActive = activeTab === tab.id;
          return (
            <button
              key={tab.id}
              type="button"
              onClick={() => setActiveTab(tab.id)}
              className={`flex items-center gap-2 py-3 px-3 text-sm font-semibold border-b-2 transition min-h-[44px] ${
                isActive
                  ? 'border-teal-600 text-teal-700'
                  : 'border-transparent text-slate-500 hover:text-slate-800'
              }`}
            >
              <Icon className="w-4 h-4" />
              <span>{tab.label}</span>
            </button>
          );
        })}
      </div>

      {/* Tab content */}
      <div className="pt-2">
        {activeTab === 'bookings' && <BookingsModerationTab />}
        {activeTab === 'services' && <ServicesTab />}
        {activeTab === 'masters' && <MastersTab />}
        {activeTab === 'schedule' && <ScheduleTab />}
      </div>
    </div>
  );
};
