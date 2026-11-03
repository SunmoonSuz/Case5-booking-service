import React from 'react';
import { Calendar, ShieldCheck, Clock, MapPin } from 'lucide-react';

export const Footer: React.FC = () => {
  return (
    <footer className="bg-white border-t border-slate-200 mt-auto py-10">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
          <div>
            <div className="flex items-center gap-2 mb-3">
              <div className="w-8 h-8 rounded-lg bg-teal-600 flex items-center justify-center text-white">
                <Calendar className="w-4 h-4" />
              </div>
              <span className="font-bold text-slate-900">Сервис Онлайн-Записи</span>
            </div>
            <p className="text-sm text-slate-500 leading-relaxed mb-4">
              Удобный и быстрый сервис бронирования услуг. Гарантия отсутствия овербукинга,
              прозрачное расписание и мгновенное подтверждение.
            </p>
            <div className="flex items-center gap-2 text-xs text-slate-400">
              <ShieldCheck className="w-4 h-4 text-teal-600" />
              <span>Защищённое соединение и надёжное хранение данных</span>
            </div>
          </div>

          <div>
            <h4 className="text-sm font-semibold text-slate-900 uppercase tracking-wider mb-3">
              Режим работы и часовой пояс
            </h4>
            <ul className="space-y-2 text-sm text-slate-600">
              <li className="flex items-center gap-2">
                <Clock className="w-4 h-4 text-teal-600 flex-shrink-0" />
                <span>Ежедневно: 09:00 – 21:00</span>
              </li>
              <li className="flex items-center gap-2">
                <MapPin className="w-4 h-4 text-teal-600 flex-shrink-0" />
                <span>Часовой пояс: Europe/Moscow (UTC+3)</span>
              </li>
            </ul>
          </div>

          <div>
            <h4 className="text-sm font-semibold text-slate-900 uppercase tracking-wider mb-3">
              Информация
            </h4>
            <p className="text-xs text-slate-500 leading-relaxed">
              Отмена записи возможна в любое время до наступления времени визита через Личный кабинет.
              Все цены указаны в рублях (RUB) без скрытых комиссий.
            </p>
            <div className="mt-4 pt-4 border-t border-slate-100 text-xs text-slate-400">
              © {new Date().getFullYear()} Сервис бронирования. Все права защищены.
            </div>
          </div>
        </div>
      </div>
    </footer>
  );
};
