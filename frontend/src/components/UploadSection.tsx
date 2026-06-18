import React, { useCallback, useState } from 'react';

interface Props {
  onUpload: (file: File) => void;
  loading: boolean;
}

const UploadSection: React.FC<Props> = ({ onUpload, loading }) => {
  const [dragging, setDragging] = useState(false);
  const [selectedFile, setSelectedFile] = useState<File | null>(null);

  const handleFile = (file: File) => {
    if (!file.name.endsWith('.zip')) {
      alert('Please upload a .zip file');
      return;
    }
    setSelectedFile(file);
  };

  const handleDrop = useCallback((e: React.DragEvent) => {
    e.preventDefault();
    setDragging(false);
    const file = e.dataTransfer.files[0];
    if (file) handleFile(file);
  }, []);

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) handleFile(file);
  };

  return (
    <div style={styles.container}>
      <div
        style={{ ...styles.dropzone, ...(dragging ? styles.dropzoneDragging : {}) }}
        onDragOver={(e) => { e.preventDefault(); setDragging(true); }}
        onDragLeave={() => setDragging(false)}
        onDrop={handleDrop}
        onClick={() => document.getElementById('fileInput')?.click()}
      >
        <input
          id="fileInput"
          type="file"
          accept=".zip"
          style={{ display: 'none' }}
          onChange={handleInputChange}
        />
        <div style={styles.icon}>📁</div>
        {selectedFile ? (
          <p style={styles.fileName}>{selectedFile.name}</p>
        ) : (
          <>
            <p style={styles.dropText}>Drag & drop your repository ZIP here</p>
            <p style={styles.subText}>or click to browse</p>
          </>
        )}
      </div>

      <button
        style={{ ...styles.button, ...(loading || !selectedFile ? styles.buttonDisabled : {}) }}
        onClick={() => selectedFile && onUpload(selectedFile)}
        disabled={loading || !selectedFile}
      >
        {loading ? 'Analyzing...' : 'Analyze Repository'}
      </button>
    </div>
  );
};

const styles: Record<string, React.CSSProperties> = {
  container: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    gap: '16px',
    width: '100%',
  },
  dropzone: {
    width: '100%',
    maxWidth: '600px',
    border: '2px dashed #4A90E2',
    borderRadius: '12px',
    padding: '48px 24px',
    textAlign: 'center',
    cursor: 'pointer',
    backgroundColor: '#f8faff',
    transition: 'all 0.2s',
  },
  dropzoneDragging: {
    backgroundColor: '#e0ecff',
    borderColor: '#1a6fd4',
  },
  icon: { fontSize: '48px', marginBottom: '12px' },
  dropText: { fontSize: '16px', color: '#333', margin: '0 0 4px 0' },
  subText: { fontSize: '13px', color: '#888', margin: 0 },
  fileName: { fontSize: '15px', color: '#4A90E2', fontWeight: 600, margin: 0 },
  button: {
    padding: '12px 36px',
    fontSize: '15px',
    fontWeight: 600,
    backgroundColor: '#4A90E2',
    color: '#fff',
    border: 'none',
    borderRadius: '8px',
    cursor: 'pointer',
  },
  buttonDisabled: {
    backgroundColor: '#aac7f0',
    cursor: 'not-allowed',
  },
};

export default UploadSection;
