// @vitest-environment jsdom
// happy-dom parses application/xml as HTML, so this file uses jsdom.

import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';

vi.mock('mirador', () => ({
  ConnectedCompanionWindow: ({ title, children, paperClassName }) => (
    <section aria-label={title} className={paperClassName}>
      <h3>{title}</h3>
      {children}
    </section>
  ),
}));

import { BookDescriptionPanel } from './BookDescriptionPanel';
import { clearBookDescriptionCache, loadBookDescription } from '../hooks/useBookDescription';
import { TeiParseError } from '../utils/tei';

const URL = 'https://x/iiif/rose/Douce332/description.xml';

const XML = `<?xml version="1.0" encoding="UTF-8"?>
<TEI xmlns="http://www.tei-c.org/ns/1.0"><teiHeader><fileDesc><notesStmt>
  <note rend="IDENTIFICATION"><p>MS Douce 332, Bodleian Library, Oxford.</p></note>
  <note rend="QUIRES"><p>Remnants of signatures, e.g. fol. <locus>9r</locus> and <locus>21 bis</locus>.</p></note>
</notesStmt></fileDesc></teiHeader></TEI>`;

const manifest = {
  id: 'https://x/iiif/rose/Douce332/manifest.json',
  type: 'Manifest',
  seeAlso: [
    {
      id: URL,
      type: 'Dataset',
      format: 'application/tei+xml',
      profile: 'http://www.tei-c.org/ns/1.0',
    },
  ],
  items: [
    { id: 'https://x/canvas/0', label: { en: ['binding front cover'] } },
    { id: 'https://x/canvas/1', label: { en: ['9r'] } },
  ],
};

const respond = (body, ok = true, status = 200) =>
  Promise.resolve({ ok, status, text: () => Promise.resolve(body) });

beforeEach(() => {
  clearBookDescriptionCache();
  global.fetch = vi.fn();
});

describe('BookDescriptionPanel', () => {
  it('loads the TEI seeAlso and renders the description', async () => {
    global.fetch.mockReturnValue(respond(XML));
    const setCanvas = vi.fn();
    render(
      <BookDescriptionPanel id="cw" windowId="w1" manifest={manifest} setCanvas={setCanvas} />
    );

    expect(screen.getByRole('status')).toHaveTextContent('Loading the book description');
    expect(await screen.findByRole('button', { name: 'Identification' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Book description' })).toBeInTheDocument();
    expect(global.fetch).toHaveBeenCalledWith(
      URL,
      expect.objectContaining({ signal: expect.anything() })
    );

    fireEvent.click(screen.getByRole('button', { name: '9r, go to folio 9r' }));
    expect(setCanvas).toHaveBeenCalledWith('w1', 'https://x/canvas/1');
    expect(screen.getByText('21 bis').tagName).toBe('SPAN');
  });

  it('uses the cache for a description already loaded', async () => {
    global.fetch.mockReturnValue(respond(XML));
    const { unmount } = render(<BookDescriptionPanel id="cw" windowId="w1" manifest={manifest} />);
    await screen.findByRole('button', { name: 'Identification' });
    unmount();

    render(<BookDescriptionPanel id="cw" windowId="w2" manifest={manifest} />);
    expect(screen.getByRole('button', { name: 'Identification' })).toBeInTheDocument();
    expect(global.fetch).toHaveBeenCalledTimes(1);
  });

  it('says so when the manifest has no description', () => {
    render(<BookDescriptionPanel id="cw" windowId="w1" manifest={{ items: [] }} />);
    expect(screen.getByRole('status')).toHaveTextContent('No description is available');
    expect(global.fetch).not.toHaveBeenCalled();
  });

  it('says so when the description has no notes', async () => {
    global.fetch.mockReturnValue(respond('<TEI xmlns="http://www.tei-c.org/ns/1.0"/>'));
    render(<BookDescriptionPanel id="cw" windowId="w1" manifest={manifest} />);
    await waitFor(() =>
      expect(screen.getByRole('status')).toHaveTextContent('No description is available')
    );
  });

  it('reports a failed request', async () => {
    const error = vi.spyOn(console, 'error').mockImplementation(() => {});
    global.fetch.mockReturnValue(respond('', false, 404));
    render(<BookDescriptionPanel id="cw" windowId="w1" manifest={manifest} />);
    await waitFor(() =>
      expect(screen.getByRole('status')).toHaveTextContent('could not be loaded')
    );
    error.mockRestore();
  });
});

describe('loadBookDescription', () => {
  it('rejects malformed XML and does not cache it', async () => {
    const fetchImpl = vi.fn(() => respond('<TEI><notesStmt></TEI>'));
    await expect(loadBookDescription(URL, { fetchImpl })).rejects.toBeInstanceOf(TeiParseError);
    await expect(loadBookDescription(URL, { fetchImpl })).rejects.toBeInstanceOf(TeiParseError);
    expect(fetchImpl).toHaveBeenCalledTimes(2);
  });

  it('rejects HTTP errors', async () => {
    const fetchImpl = vi.fn(() => respond('', false, 500));
    await expect(loadBookDescription(URL, { fetchImpl })).rejects.toThrow('HTTP 500');
  });
});
