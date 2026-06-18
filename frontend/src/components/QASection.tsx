import React, { useState, useRef, useEffect } from 'react';
import ReactMarkdown from 'react-markdown';
import { askQuestion } from '../api';
import type { Message } from '../types';

interface Props {
  repoId: string;
}

const QASection: React.FC<Props> = ({ repoId }) => {
  const [messages, setMessages] = useState<Message[]>([
    {
      role: 'assistant',
      content: 'Repository loaded! Ask me anything about this codebase — e.g. "Where is login handled?" or "What does the payment service do?"',
    },
  ]);
  const [input, setInput] = useState('');
  const [loading, setLoading] = useState(false);
  const bottomRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  const handleSend = async () => {
    const question = input.trim();
    if (!question || loading) return;

    setInput('');
    setMessages(prev => [...prev, { role: 'user', content: question }]);
    setLoading(true);

    try {
      const response = await askQuestion(repoId, question);
      setMessages(prev => [...prev, {
        role: 'assistant',
        content: response.answer,
        sources: response.sources,
      }]);
    } catch (e: any) {
      setMessages(prev => [...prev, {
        role: 'assistant',
        content: 'Sorry, something went wrong. Please try again.',
      }]);
    } finally {
      setLoading(false);
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  return (
    <div style={styles.container}>
      <h2 style={styles.title}>💬 Ask About the Codebase</h2>

      {/* Chat messages */}
      <div style={styles.chatBox}>
        {messages.map((msg, i) => (
          <div key={i} style={{ ...styles.message, ...(msg.role === 'user' ? styles.userMessage : styles.assistantMessage) }}>
            <div style={styles.bubble}>
              <ReactMarkdown>{msg.content}</ReactMarkdown>
            </div>

            {/* Sources */}
            {msg.sources && msg.sources.length > 0 && (
              <div style={styles.sources}>
                <span style={styles.sourcesLabel}>Sources:</span>
                {msg.sources.map((src, j) => (
                  <span key={j} style={styles.sourceChip}>{src}</span>
                ))}
              </div>
            )}
          </div>
        ))}

        {loading && (
          <div style={{ ...styles.message, ...styles.assistantMessage }}>
            <div style={styles.bubble}>
              <span style={styles.typing}>Thinking...</span>
            </div>
          </div>
        )}
        <div ref={bottomRef} />
      </div>

      {/* Input */}
      <div style={styles.inputRow}>
        <textarea
          style={styles.input}
          value={input}
          onChange={e => setInput(e.target.value)}
          onKeyDown={handleKeyDown}
          placeholder="Ask a question about the codebase... (Enter to send)"
          rows={2}
          disabled={loading}
        />
        <button
          style={{ ...styles.sendBtn, ...(loading || !input.trim() ? styles.sendBtnDisabled : {}) }}
          onClick={handleSend}
          disabled={loading || !input.trim()}
        >
          Send
        </button>
      </div>
    </div>
  );
};

const styles: Record<string, React.CSSProperties> = {
  container: {
    backgroundColor: '#fff',
    borderRadius: '12px',
    padding: '24px',
    boxShadow: '0 2px 8px rgba(0,0,0,0.08)',
    width: '100%',
    display: 'flex',
    flexDirection: 'column',
    gap: '16px',
  },
  title: {
    fontSize: '18px',
    fontWeight: 700,
    margin: 0,
    color: '#1a1a2e',
  },
  chatBox: {
    display: 'flex',
    flexDirection: 'column',
    gap: '12px',
    maxHeight: '480px',
    overflowY: 'auto',
    padding: '8px 0',
  },
  message: {
    display: 'flex',
    flexDirection: 'column',
    gap: '6px',
  },
  userMessage: {
    alignItems: 'flex-end',
  },
  assistantMessage: {
    alignItems: 'flex-start',
  },
  bubble: {
    maxWidth: '80%',
    padding: '10px 14px',
    borderRadius: '12px',
    fontSize: '14px',
    lineHeight: '1.6',
    backgroundColor: '#f0f4ff',
    color: '#333',
  },
  sources: {
    display: 'flex',
    flexWrap: 'wrap',
    gap: '6px',
    alignItems: 'center',
    maxWidth: '80%',
  },
  sourcesLabel: {
    fontSize: '11px',
    color: '#888',
    fontWeight: 600,
  },
  sourceChip: {
    fontSize: '11px',
    backgroundColor: '#e8f0fe',
    color: '#4A90E2',
    padding: '2px 8px',
    borderRadius: '10px',
    fontFamily: 'monospace',
  },
  typing: {
    color: '#888',
    fontStyle: 'italic',
  },
  inputRow: {
    display: 'flex',
    gap: '10px',
    alignItems: 'flex-end',
  },
  input: {
    flex: 1,
    padding: '10px 12px',
    borderRadius: '8px',
    border: '1px solid #dde',
    fontSize: '14px',
    resize: 'none',
    fontFamily: 'inherit',
    outline: 'none',
  },
  sendBtn: {
    padding: '10px 20px',
    backgroundColor: '#4A90E2',
    color: '#fff',
    border: 'none',
    borderRadius: '8px',
    fontWeight: 600,
    fontSize: '14px',
    cursor: 'pointer',
    height: 'fit-content',
  },
  sendBtnDisabled: {
    backgroundColor: '#aac7f0',
    cursor: 'not-allowed',
  },
};

export default QASection;
