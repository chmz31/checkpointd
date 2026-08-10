import { Link } from 'react-router-dom';
import { formatDate } from '../dateUtils';
import { listPath } from '../routePaths';
import type { GameList } from '../types';

export function ListCard({ list }: { list: GameList }) {
  return (
    <Link className="list-card" to={listPath(list.username, list)}>
      <div className="review-heading">
        <h3>{list.name}</h3>
        {list.visibility === 'PRIVATE' && <span className="status-badge">Private</span>}
      </div>
      {list.description && <p className="review-text">{list.description}</p>}
      <p className="muted">
        {list.itemCount} {list.itemCount === 1 ? 'game' : 'games'} · Updated {formatDate(list.updatedAt)}
      </p>
    </Link>
  );
}
