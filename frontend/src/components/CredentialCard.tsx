import api from '../api/client';

interface Props {
  credential: { id: string; name: string; url: string; username: string };
  onDeleted: () => void;
}

export default function CredentialCard({ credential, onDeleted }: Props) {
  async function handleDelete() {
    if (!confirm(`Delete "${credential.name}"?`)) return;
    await api.delete(`/api/credentials/${credential.id}`);
    onDeleted();
  }

  return (
    <div style={{ border: '1px solid #ddd', borderRadius: 6, padding: 16, marginBottom: 12 }}>
      <strong>{credential.name}</strong>
      <span style={{ marginLeft: 12, color: '#666' }}>{credential.url}</span>
      <span style={{ marginLeft: 12 }}>user: {credential.username}</span>
      <button onClick={handleDelete} style={{ float: 'right', color: 'red' }}>Delete</button>
    </div>
  );
}
