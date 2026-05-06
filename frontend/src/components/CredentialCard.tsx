import { useState } from 'react';
import api from '../api/client';

interface Props {
  credential: { id: string; name: string; url: string; username: string };
  onDeleted: () => void;
}

export default function CredentialCard({ credential, onDeleted }: Props) {
  const [confirming, setConfirming] = useState(false);
  const [deleting, setDeleting] = useState(false);

  async function handleDelete() {
    setDeleting(true);
    try {
      await api.delete(`/api/credentials/${credential.id}`);
      onDeleted();
    } finally {
      setDeleting(false);
      setConfirming(false);
    }
  }

  const favicon = `https://www.google.com/s2/favicons?domain=${encodeURIComponent(credential.url)}&sz=32`;

  return (
    <div className="card" style={card.wrapper}>
      {/* Left: favicon + info */}
      <div style={card.left}>
        <img
          src={favicon}
          alt=""
          style={card.favicon}
          onError={e => { (e.target as HTMLImageElement).style.display = 'none'; }}
        />
        <div>
          <p style={card.name}>{credential.name}</p>
          <p style={card.meta}>
            <span style={card.url}>{credential.url}</span>
            <span style={card.dot}>·</span>
            <span style={card.username}>{credential.username}</span>
          </p>
        </div>
      </div>

      {/* Right: delete */}
      <div style={card.right}>
        {confirming ? (
          <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
            <span style={{ fontSize: 13, color: '#94a3b8' }}>Delete?</span>
            <button className="btn-danger" onClick={handleDelete} disabled={deleting}>
              {deleting ? '…' : 'Yes'}
            </button>
            <button className="btn-ghost" style={{ padding: '7px 12px', fontSize: 12 }} onClick={() => setConfirming(false)}>
              No
            </button>
          </div>
        ) : (
          <button className="btn-danger" onClick={() => setConfirming(true)}>
            Delete
          </button>
        )}
      </div>
    </div>
  );
}

const card: Record<string, React.CSSProperties> = {
  wrapper: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: '16px 20px',
    transition: 'border-color 0.15s',
  },
  left: { display: 'flex', alignItems: 'center', gap: 14 },
  favicon: { width: 28, height: 28, borderRadius: 6, flexShrink: 0 },
  name: { fontSize: 15, fontWeight: 600, color: '#f1f5f9', marginBottom: 3 },
  meta: { display: 'flex', alignItems: 'center', gap: 6, fontSize: 13 },
  url: { color: '#64748b' },
  dot: { color: '#334155' },
  username: { color: '#94a3b8' },
  right: { flexShrink: 0 },
};
