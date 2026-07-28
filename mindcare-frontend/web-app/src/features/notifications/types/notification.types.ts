export interface NotificationItem {
  id: string;
  title: string;
  content: string;
  notificationType: string;
  actionUrl?: string | null;
  read: boolean;
  createdAt: string;
}
