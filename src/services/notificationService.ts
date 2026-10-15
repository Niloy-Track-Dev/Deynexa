import { Task } from '../types';
import { TimeUtils } from '../utils/timeUtils';

export class NotificationService {
  private hasPermission: boolean = false;

  constructor() {
    if (typeof window !== 'undefined' && 'Notification' in window) {
      this.hasPermission = Notification.permission === 'granted';
    }
  }

  async requestPermission(): Promise<boolean> {
    if (typeof window === 'undefined' || !('Notification' in window)) {
      return false;
    }
    try {
      const permission = await Notification.requestPermission();
      this.hasPermission = permission === 'granted';
      return this.hasPermission;
    } catch {
      return false;
    }
  }

  sendNotification(title: string, body: string, icon: string = '/daynexa-icon.svg'): void {
    if (this.hasPermission && 'Notification' in window) {
      try {
        new Notification(title, {
          body,
          icon,
          badge: icon,
        });
        return;
      } catch {
        // Fallback below
      }
    }

    // Audible / visual fallback in UI
    console.log(`[Daynexa Reminder Alert] ${title} - ${body}`);
  }

  sendTestAlert(): void {
    this.sendNotification(
      'Daynexa Test Routine Alert',
      'This is a sample routine notification with on-device sound and banner!'
    );
  }

  scheduleTaskReminder(task: Task, categoryName: string): void {
    if (!task.reminderEnabled || task.startTime === null || task.startTime === undefined) return;
    const offset = task.reminderOffsetMinutes || 0;
    const timeFormatted = TimeUtils.formatMinutes(task.startTime, false);
    const offsetText = offset === 0 ? 'starting now' : `in ${offset} minutes`;

    this.sendNotification(
      `Reminder: ${task.name}`,
      `${categoryName ? `[${categoryName}] ` : ''}Scheduled for ${timeFormatted} (${offsetText})`
    );
  }
}

export const notificationService = new NotificationService();
