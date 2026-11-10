'use client';

import React, { useState } from 'react';
import Link from 'next/link';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import { Copy, Check } from 'lucide-react';
import { AiSvgChart, parseChartConfig } from './AiSvgChart';

interface MarkdownMessageProps {
  content: string;
}

function CodeBlockHeader({
  language,
  rawContent,
}: {
  language: string;
  rawContent: string;
}) {
  const [copied, setCopied] = useState(false);

  const handleCopy = async () => {
    try {
      await navigator.clipboard.writeText(rawContent.replace(/\n$/, ''));
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch {
      // Fallback if clipboard API fails
    }
  };

  return (
    <div className="flex items-center justify-between border-b border-neutral-800 bg-neutral-900/90 px-3 py-1.5 text-[10px] text-neutral-400 font-mono">
      <span className="uppercase tracking-wider font-semibold">{language || 'CODE'}</span>
      <button
        type="button"
        onClick={handleCopy}
        aria-label="Sao chép đoạn mã"
        className="flex items-center gap-1 rounded px-1.5 py-0.5 text-[10px] text-neutral-300 transition-colors hover:bg-neutral-800 hover:text-white"
      >
        {copied ? (
          <>
            <Check className="size-3 text-[var(--color-primary)]" />
            <span className="text-[var(--color-primary)]">Đã chép</span>
          </>
        ) : (
          <>
            <Copy className="size-3" />
            <span>Sao chép</span>
          </>
        )}
      </button>
    </div>
  );
}

export function MarkdownMessage({ content }: MarkdownMessageProps) {
  return (
    <div className="prose-ai text-xs sm:text-sm leading-relaxed text-[var(--color-text-primary)] break-words">
      <ReactMarkdown
        remarkPlugins={[remarkGfm]}
        components={{
          p: ({ children }) => <p className="mb-2 last:mb-0 leading-relaxed">{children}</p>,
          strong: ({ children }) => (
            <strong className="font-semibold text-[var(--color-text-primary)]">{children}</strong>
          ),
          em: ({ children }) => <em className="italic">{children}</em>,
          h1: ({ children }) => (
            <h1 className="text-sm sm:text-base font-bold mt-3 mb-1.5 text-[var(--color-text-primary)]">
              {children}
            </h1>
          ),
          h2: ({ children }) => (
            <h2 className="text-xs sm:text-sm font-bold mt-2.5 mb-1 text-[var(--color-text-primary)]">
              {children}
            </h2>
          ),
          h3: ({ children }) => (
            <h3 className="text-xs sm:text-sm font-semibold mt-2 mb-1 text-[var(--color-text-primary)]">
              {children}
            </h3>
          ),
          ul: ({ children }) => <ul className="list-disc pl-4 mb-2 space-y-1">{children}</ul>,
          ol: ({ children }) => <ol className="list-decimal pl-4 mb-2 space-y-1">{children}</ol>,
          li: ({ children }) => <li className="leading-relaxed">{children}</li>,
          blockquote: ({ children }) => (
            <blockquote className="border-l-2 border-[var(--color-primary)] pl-3 py-1 my-2 italic text-[var(--color-text-secondary)] bg-[var(--color-surface-muted)]/60 rounded-r-md">
              {children}
            </blockquote>
          ),
          table: ({ children }) => (
            <div className="my-2.5 w-full overflow-hidden rounded-[var(--radius-lg)] border border-[var(--color-border)] bg-[var(--color-surface)] shadow-[var(--shadow-panel)]">
              <div className="scroll-slim relative w-full overflow-x-auto">
                <table className="w-max min-w-full text-left text-xs border-collapse">{children}</table>
              </div>
            </div>
          ),
          thead: ({ children }) => (
            <thead className="bg-[var(--color-surface-muted)] text-[var(--color-text-primary)] font-semibold border-b border-[var(--color-border)]">
              {children}
            </thead>
          ),
          tbody: ({ children }) => <tbody className="divide-y divide-[var(--color-border)]">{children}</tbody>,
          tr: ({ children }) => (
            <tr className="group/row hover:bg-[var(--color-surface-muted)]/40 transition-colors">{children}</tr>
          ),
          th: ({ children }) => (
            <th className="px-3.5 py-2.5 font-semibold text-[var(--color-text-primary)] whitespace-nowrap">
              {children}
            </th>
          ),
          td: ({ children }) => (
            <td className="px-3.5 py-2 text-[var(--color-text-secondary)] whitespace-nowrap [overflow-wrap:normal] break-normal align-middle group-hover/row:text-[var(--color-text-primary)] transition-colors">
              {children}
            </td>
          ),
          hr: () => <hr className="my-3 border-[var(--color-border)]" />,
          img: ({ src, alt, ...props }: React.ComponentPropsWithoutRef<'img'>) => (
            <span className="my-2 block overflow-hidden rounded-xl border border-[var(--color-border)] bg-[var(--color-surface-muted)] shadow-sm">
              <img
                src={src}
                alt={alt || 'Hình ảnh đính kèm'}
                loading="lazy"
                className="max-h-56 w-auto max-w-full rounded-lg object-contain mx-auto transition-transform hover:scale-[1.02]"
                {...props}
              />
              {alt && (
                <span className="block px-2.5 py-1 text-center text-[11px] text-[var(--color-text-secondary)] italic border-t border-[var(--color-border)]">
                  {alt}
                </span>
              )}
            </span>
          ),
          pre: ({ children, ...props }: React.ComponentPropsWithoutRef<'pre'>) => {
            let isChart = false;
            let language = '';
            let rawCode = '';

            React.Children.forEach(children, (child) => {
              if (React.isValidElement(child)) {
                const childProps = child.props as Record<string, unknown>;
                const className = String(childProps?.className || '');
                if (className.includes('language-chart')) {
                  isChart = true;
                }
                const match = className.match(/language-([a-zA-Z0-9_-]+)/);
                if (match) {
                  language = match[1];
                }
                const codeChildren = childProps?.children;
                if (typeof codeChildren === 'string') {
                  rawCode = codeChildren;
                } else if (Array.isArray(codeChildren)) {
                  rawCode = codeChildren.join('');
                }
              }
            });

            if (isChart) {
              return <div className="my-2.5 w-full not-prose">{children}</div>;
            }

            return (
              <div className="my-2.5 overflow-hidden rounded-xl border border-neutral-800 bg-neutral-950 text-neutral-100 shadow-sm">
                <CodeBlockHeader language={language} rawContent={rawCode} />
                <pre className="overflow-x-auto p-3 font-mono text-xs leading-relaxed" {...props}>
                  {children}
                </pre>
              </div>
            );
          },
          code: ({ className, children, ...props }: React.ComponentPropsWithoutRef<'code'>) => {
            const isChart = Boolean(className?.includes('language-chart'));
            if (isChart) {
              const rawContent = Array.isArray(children)
                ? children.join('')
                : typeof children === 'string'
                ? children
                : String(children ?? '');
              const chartConfig = parseChartConfig(rawContent);

              if (chartConfig) {
                return <AiSvgChart config={chartConfig} />;
              }

              // Fallback khi JSON không hợp lệ
              return (
                <div className="my-2 rounded-xl border border-amber-500/30 bg-amber-500/10 p-3 text-xs">
                  <div className="mb-1.5 font-medium text-amber-700 dark:text-amber-400">
                    Dữ liệu biểu đồ không hợp lệ
                  </div>
                  <pre className="overflow-x-auto rounded-lg bg-neutral-900 p-2.5 font-mono text-[11px] text-neutral-200">
                    <code>{rawContent}</code>
                  </pre>
                </div>
              );
            }

            const isCodeBlock =
              Boolean(className?.startsWith('language-')) ||
              (typeof children === 'string' && children.includes('\n'));

            if (isCodeBlock) {
              return (
                <code className={className} {...props}>
                  {children}
                </code>
              );
            }
            return (
              <code
                className="rounded-md bg-[var(--color-surface-muted)] border border-[var(--color-border)] px-1.5 py-0.5 font-mono text-[11px] font-medium text-[var(--color-primary)]"
                {...props}
              >
                {children}
              </code>
            );
          },
          a: ({ href, children, ...props }: React.ComponentPropsWithoutRef<'a'>) => {
            if (href && href.startsWith('/')) {
              return (
                <Link
                  href={href}
                  className="inline-flex items-center gap-1 font-medium text-[var(--color-primary)] underline underline-offset-2 hover:text-[var(--color-primary-hover)] transition-colors"
                >
                  {children}
                </Link>
              );
            }
            return (
              <a
                href={href}
                target="_blank"
                rel="noopener noreferrer"
                className="inline-flex items-center gap-1 font-medium text-[var(--color-primary)] underline underline-offset-2 hover:text-[var(--color-primary-hover)] transition-colors"
                {...props}
              >
                {children}
              </a>
            );
          },
        }}
      >
        {content}
      </ReactMarkdown>
    </div>
  );
}
