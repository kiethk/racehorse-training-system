import { useState, useEffect } from 'react';

/**
 * Hook cung cấp đồng hồ thời gian thực tế đồng bộ theo múi giờ máy chủ (Asia/Ho_Chi_Minh).
 */
export function useServerClock() {
  const [currentTime, setCurrentTime] = useState<Date>(() => new Date());

  useEffect(() => {
    const timer = setInterval(() => {
      setCurrentTime(new Date());
    }, 1000);
    return () => clearInterval(timer);
  }, []);

  const formattedTime = new Intl.DateTimeFormat('vi-VN', {
    timeZone: 'Asia/Ho_Chi_Minh',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false,
  }).format(currentTime);

  const formattedDate = new Intl.DateTimeFormat('vi-VN', {
    timeZone: 'Asia/Ho_Chi_Minh',
    dateStyle: 'medium',
  }).format(currentTime);

  return {
    currentTime,
    formattedTime,
    formattedDate,
  };
}
