import { FormEvent, useEffect, useState } from 'react';
import type { GameList, GameListRequest, ListVisibility } from '../types';

export function ListForm({
  list,
  submitting,
  onSubmit,
}: {
  list?: GameList | null;
  submitting: boolean;
  onSubmit: (request: GameListRequest) => Promise<void>;
}) {
  const [name, setName] = useState(list?.name || '');
  const [description, setDescription] = useState(list?.description || '');
  const [visibility, setVisibility] = useState<ListVisibility>(list?.visibility || 'PUBLIC');
  const [clientError, setClientError] = useState<string | null>(null);

  useEffect(() => {
    setName(list?.name || '');
    setDescription(list?.description || '');
    setVisibility(list?.visibility || 'PUBLIC');
  }, [list]);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const cleanName = name.trim();

    if (!cleanName) {
      setClientError('List name is required.');
      return;
    }

    setClientError(null);
    await onSubmit({
      name: cleanName,
      description: description.trim() || null,
      visibility,
    });
  }

  return (
    <form className="edit-form" onSubmit={submit}>
      <label>
        Name
        <input
          value={name}
          maxLength={200}
          onChange={(event) => {
            setName(event.target.value);
            setClientError(null);
          }}
          placeholder="e.g. Games to play this Halloween"
        />
      </label>
      <label>
        Visibility
        <select value={visibility} onChange={(event) => setVisibility(event.target.value as ListVisibility)}>
          <option value="PUBLIC">PUBLIC</option>
          <option value="PRIVATE">PRIVATE</option>
        </select>
      </label>
      <label className="full-width">
        Description
        <textarea
          value={description}
          maxLength={2000}
          rows={3}
          onChange={(event) => setDescription(event.target.value)}
          placeholder="What's this list about?"
        />
      </label>
      {clientError && <p className="error compact-message full-width">{clientError}</p>}
      <button type="submit" disabled={submitting}>
        {submitting ? 'Saving...' : list ? 'Save list' : 'Create list'}
      </button>
    </form>
  );
}
