import React from 'react';
import { BookingFlow } from '@/features/booking/BookingFlow';
import { Calendar, Clock, ShieldCheck, Sparkles, CheckCircle2 } from 'lucide-react';
import { Button } from '@/shared/components/Button';

export const HomePage: React.FC = () => {
  const scrollToBooking = () => {
    const el = document.getElementById('booking-section');
    if (el) {
      el.scrollIntoView({ behavior: 'smooth' });
    }
  };

  return (
    <div className="space-y-12 pb-16">
      {/* Hero Section */}
      <section className="relative overflow-hidden rounded-3xl bg-gradient-to-br from-teal-900 via-slate-900 to-teal-950 text-white p-8 sm:p-12 lg:p-16 shadow-xl">
        <div className="relative z-10 max-w-3xl space-y-6">
          <div className="inline-flex items-center gap-2 px-3 py-1.5 rounded-full bg-teal-500/20 border border-teal-400/30 text-teal-300 text-xs font-semibold uppercase tracking-wider backdrop-blur-sm">
            <Sparkles className="w-3.5 h-3.5" />
            Онлайн-запись 24/7 без звонков
          </div>

          <h1 className="text-3xl sm:text-5xl font-extrabold tracking-tight leading-tight">
            Запишитесь к лучшим специалистам в пару кликов
          </h1>

          <p className="text-slate-300 text-base sm:text-lg leading-relaxed max-w-2xl">
            Выберите необходимую услугу, любимого мастера и удобное время в интерактивном
            календаре. Гарантированная защита от пересечений и мгновенное подтверждение.
          </p>

          <div className="flex flex-wrap items-center gap-4 pt-2">
            <Button
              variant="primary"
              size="lg"
              onClick={scrollToBooking}
              icon={<Calendar className="w-5 h-5" />}
              className="bg-teal-500 hover:bg-teal-600 text-slate-950 font-bold"
            >
              Выбрать время визита
            </Button>
          </div>

          {/* Value Props */}
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 pt-6 border-t border-white/10 text-xs sm:text-sm text-slate-300">
            <div className="flex items-center gap-2.5">
              <CheckCircle2 className="w-4 h-4 text-teal-400 flex-shrink-0" />
              <span>Дискретная сетка 30 и 60 мин</span>
            </div>
            <div className="flex items-center gap-2.5">
              <ShieldCheck className="w-4 h-4 text-teal-400 flex-shrink-0" />
              <span>100% защита от овербукинга</span>
            </div>
            <div className="flex items-center gap-2.5">
              <Clock className="w-4 h-4 text-teal-400 flex-shrink-0" />
              <span>Бесплатная отмена до начала визита</span>
            </div>
          </div>
        </div>

        {/* Decorative background glow */}
        <div className="absolute -right-20 -bottom-20 w-96 h-96 bg-teal-500/10 rounded-full blur-3xl pointer-events-none" />
      </section>

      {/* Main Interactive Booking Flow */}
      <BookingFlow />
    </div>
  );
};
