import { useState } from 'react';
import { api } from '../api';
import type { ExternalGameSearchResult, LibraryEntry } from '../types';
import { AddToLibraryForm, type AddToLibraryFormValues } from './AddToLibraryForm';

export function AddSearchResultToLibrary({
  result,
  onAdded,
}: {
  result: ExternalGameSearchResult;
  onAdded: (entry: LibraryEntry) => void;
}) {
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  function clearFeedback() {
    setMessage(null);
    setError(null);
  }

  async function submit(values: AddToLibraryFormValues) {
    setLoading(true);
    setMessage(null);
    setError(null);

    try {
      const game = await api.importExternalGame(result.provider, result.externalId);
      const entry = await api.addLibraryEntry({
        gameId: game.id,
        ...values,
      });
      setMessage('Added to library.');
      onAdded(entry);
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : 'Could not add to library');
    } finally {
      setLoading(false);
    }
  }

  return (
    <AddToLibraryForm
      onSubmit={submit}
      submitting={loading}
      submitLabel="Add"
      message={message}
      error={error}
      onChange={clearFeedback}
    />
  );
}
