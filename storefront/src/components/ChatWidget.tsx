import { useEffect, useRef, useState, type FormEvent, type ReactNode } from 'react';
import { Link } from 'react-router-dom';
import { ApiError, post } from '../lib/api';
import { money } from '../lib/format';
import type { ChatMessage, ChatResponse, ProductCard } from '../lib/types';

interface UiMessage extends ChatMessage {
  products?: ProductCard[];
}

const GREETING: UiMessage = {
  role: 'assistant',
  content: "Hi! I'm Tara. I can help you find something, pick the right size, or check on an order.",
};

const SUGGESTIONS = ['Linen shirts under ₹2000', 'What size dress for 165cm, bust 88cm?', 'Where is my order?'];

/** Renders only plain text + internal markdown links ([label](/path)) - never raw HTML from the model. */
function renderReply(text: string): ReactNode[] {
  const out: ReactNode[] = [];
  const re = /\[([^\]]+)\]\((\/[^)\s]*)\)/g;
  let last = 0;
  let m: RegExpExecArray | null;
  while ((m = re.exec(text))) {
    if (m.index > last) out.push(text.slice(last, m.index));
    out.push(<Link key={m.index} to={m[2]}>{m[1]}</Link>);
    last = m.index + m[0].length;
  }
  out.push(text.slice(last).replace(/\*\*/g, ''));
  return out;
}

export function ChatWidget() {
  const [open, setOpen] = useState(false);
  const [messages, setMessages] = useState<UiMessage[]>([GREETING]);
  const [input, setInput] = useState('');
  const [busy, setBusy] = useState(false);
  const endRef = useRef<HTMLDivElement>(null);

  useEffect(() => endRef.current?.scrollIntoView({ behavior: 'smooth' }), [messages, open]);

  const send = async (text: string) => {
    const content = text.trim();
    if (!content || busy) return;
    const next = [...messages, { role: 'user' as const, content }];
    setMessages(next);
    setInput('');
    setBusy(true);
    try {
      // send transcript without the canned greeting and UI-only fields
      const transcript = next.filter((m) => m !== GREETING).map(({ role, content }) => ({ role, content }));
      const res = await post<ChatResponse>('/api/v1/chat', { messages: transcript.slice(-20) });
      setMessages((prev) => [...prev, { role: 'assistant', content: res.reply, products: res.products }]);
    } catch (e) {
      const msg = e instanceof ApiError && e.status === 429 ? e.message : 'Sorry, I could not reach the assistant. Please try again.';
      setMessages((prev) => [...prev, { role: 'assistant', content: msg }]);
    } finally {
      setBusy(false);
    }
  };

  const submit = (e: FormEvent) => {
    e.preventDefault();
    send(input);
  };

  if (!open) {
    return (
      <button className="chat-fab" onClick={() => setOpen(true)} aria-label="Open shopping assistant">
        Need help?
      </button>
    );
  }

  return (
    <aside className="chat-panel" aria-label="Shopping assistant">
      <header className="chat-head">
        <strong>Tara · Shopping assistant</strong>
        <button className="link-btn" onClick={() => setOpen(false)} aria-label="Close">✕</button>
      </header>
      <div className="chat-log" aria-live="polite">
        {messages.map((m, i) => (
          <div key={i} className={`chat-msg ${m.role}`}>
            <div className="bubble">{m.role === 'assistant' ? renderReply(m.content) : m.content}</div>
            {m.products && m.products.length > 0 && (
              <div className="chat-products">
                {m.products.map((p) => (
                  <Link key={p.id} to={`/p/${p.slug}`} className="chat-product" onClick={() => setOpen(false)}>
                    {p.imageUrl && <img src={p.imageUrl} alt="" />}
                    <span>{p.name}</span>
                    <span className="price">{money(p.price)}</span>
                  </Link>
                ))}
              </div>
            )}
          </div>
        ))}
        {busy && <div className="chat-msg assistant"><div className="bubble typing">Thinking…</div></div>}
        {messages.length === 1 && (
          <div className="chat-suggestions">
            {SUGGESTIONS.map((s) => <button key={s} onClick={() => send(s)}>{s}</button>)}
          </div>
        )}
        <div ref={endRef} />
      </div>
      <form className="chat-input" onSubmit={submit}>
        <input value={input} onChange={(e) => setInput(e.target.value)} placeholder="Ask about products, sizes, orders…"
               maxLength={2000} aria-label="Message" disabled={busy} />
        <button className="btn" disabled={busy || !input.trim()}>Send</button>
      </form>
    </aside>
  );
}
