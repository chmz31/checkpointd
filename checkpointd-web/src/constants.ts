import type { LibraryStatus } from './types';

export const libraryStatuses: LibraryStatus[] = [
  'WISHLIST',
  'BACKLOG',
  'PLAYING',
  'COMPLETED',
  'DROPPED',
  'PAUSED',
];

export type AuthMode = 'login' | 'register';
