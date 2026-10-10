import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api';
import type { GameList } from '../types';
import { ListCard } from './ListCard';

const PREVIEW_SIZE = 6;

export function GuestHomePage() {
  const [lists, setLists] = useState<GameList[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;

    api
      .getPopularLists(0, PREVIEW_SIZE)
      .then((page) => {
        if (!cancelled) setLists(page.content);
      })
      .catch(() => {
        // The landing page still reads fine without the preview.
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, []);

  return (
    <>
      <section className="panel guest-hero">
        <p className="eyebrow">your save file for every game you play.</p>
        <h2>Track the games you play. Keep the ones that mattered.</h2>
        <p className="guest-hero-pitch">
          checkpointd is a backlog and library for your games — log what you're playing, rate and review
          it, build lists, and follow what other players are into.
        </p>
        <div className="guest-hero-actions">
          <Link className="nav-link" to="/register">
            Create an account
          </Link>
          <Link className="nav-link button-ghost" to="/login">
            Log in
          </Link>
        </div>
        <p className="muted">
          Just looking around? Game pages, reviews, lists, and profiles are all open — no account needed.
        </p>
      </section>
      <section className="panel">
        <div className="section-heading">
          <h2>Popular lists</h2>
          <Link className="inline-link" to="/lists/popular">
            Browse all
          </Link>
        </div>
        {loading && <p className="muted">Loading lists...</p>}
        {!loading && lists.length === 0 && (
          <div className="empty-state catalog-empty">
            <h3>No popular lists yet</h3>
            <p>Once public lists start getting likes, the most liked ones will show up here.</p>
          </div>
        )}
        <div className="review-list">
          {lists.map((list) => (
            <ListCard key={list.id} list={list} />
          ))}
        </div>
      </section>
      <section className="panel guest-highlights">
        <div className="guest-highlight">
          <h3>A library, not a spreadsheet</h3>
          <p>Backlog, playing, completed, dropped — with hours, ratings, and your own notes.</p>
        </div>
        <div className="guest-highlight">
          <h3>Reviews worth reading</h3>
          <p>Write about a game once you've put it down, with spoiler blocks when you need them.</p>
        </div>
        <div className="guest-highlight">
          <h3>Lists and people</h3>
          <p>Curate lists, follow other members, and see what the people you follow are playing.</p>
        </div>
      </section>
    </>
  );
}
