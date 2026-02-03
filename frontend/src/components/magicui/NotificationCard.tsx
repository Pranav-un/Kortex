import './Notification.css';

type Item = {
  name: string;
  description: string;
  icon?: string;
  color?: string;
  time: string;
};

export default function NotificationCard({ name, description, icon, color, time }: Item) {
  return (
    <figure className="notif-card">
      <div className="notif-row">
        {icon && (
          <div className="notif-icon" style={{ backgroundColor: color ?? '#7A5CF5' }}>
            <span className="notif-emoji">{icon}</span>
          </div>
        )}
        <div className="notif-content">
          <figcaption className="notif-title">
            <span className="notif-name">{name}</span>
            <span className="notif-dot">·</span>
            <span className="notif-time">{time}</span>
          </figcaption>
          <p className="notif-desc">{description}</p>
        </div>
      </div>
    </figure>
  );
}
