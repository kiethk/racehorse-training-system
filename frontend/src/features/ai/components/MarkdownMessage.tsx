'use client';

import React from 'react';
import Link from 'next/link';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import { AiSvgChart, parseChartConfig } from './AiSvgChart';

interface MarkdownMessageProps {
  content: string;
}

export function MarkdownMessage({ content }: MarkdownMessageProps) {
  return (
    <div className="prose-ai text-xs leading-relaxed text-[var(--color-text-primary)] break-words">
      <ReactMarkdown
        remarkPlugins={[remarkGfm]}
        components={{
          p: ({ children }) => <p className="mb-2 last:mb-0 leading-relaxed">{children}</p>,
          strong: ({ children }) => <strong className="font-semibold text-[var(--color-text-primary)]">{children}</strong>,
          em: ({ children }) => <em className="italic">{children}</em>,
          h1: ({ children }) => <h1 className="text-sm font-bold mt-2.5 mb-1.5 text-[var(--color-text-primary)]">{children}</h1>,
          h2: ({ children }) => <h2 className="text-xs font-bold mt-2 mb-1 text-[var(--color-text-primary)]">{children}</h2>,
          h3: ({ children }) => <h3 className="text-xs font-semibold mt-1.5 mb-1 text-[var(--color-text-primary)]">{children}</h3>,
          ul: ({ children }) => <ul className="list-disc pl-4 mb-2 space-y-0.5">{children}</ul>,
          ol: ({ children }) => <ol className="list-decimal pl-4 mb-2 space-y-0.5">{children}</ol>,
          li: ({ children }) => <li className="leading-relaxed">{children}</li>,
          blockquote: ({ children }) => (
            <blockquote className="border-l-2 border-emerald-500 pl-2.5 py-0.5 my-2 italic text-[var(--color-text-secondary)] bg-[var(--color-surface-muted)]/40 rounded-r">
              {children}
            </blockquote>
          ),
          table: ({ children }) => (
            <div className="my-2.5 w-full overflow-x-auto rounded-lg border border-[var(--color-border)]">
              <table className="w-full text-left text-[11px] border-collapse">{children}</table>
            </div>
          ),
          thead: ({ children }) => (
            <thead className="bg-[var(--color-surface-muted)] text-[var(--color-text-primary)] font-semibold border-b border-[var(--color-border)]">
              {children}
            </thead>
          ),
          tbody: ({ children }) => <tbody className="divide-y divide-[var(--color-border)]">{children}</tbody>,
          tr: ({ children }) => <tr className="hover:bg-[var(--color-surface-muted)]/30 transition-colors">{children}</tr>,
          th: ({ children }) => <th className="px-2.5 py-1.5 font-semibold text-[var(--color-text-primary)]">{children}</th>,
          td: ({ children }) => <td className="px-2.5 py-1.5 text-[var(--color-text-secondary)] whitespace-nowrap">{children}</td>,
          hr: () => <hr className="my-2.5 border-[var(--color-border)]" />,
          pre: ({ children, ...props }: any) => {
            let isChart = false;
            React.Children.forEach(children, (child) => {
              if (
                React.isValidElement(child) &&
                typeof (child.props as any)?.className === 'string' &&
                (child.props as any).className.includes('language-chart')
              ) {
                isChart = true;
              }
            });

            if (isChart) {
              return <div className="my-2.5 w-full not-prose">{children}</div>;
            }

            return (
              <pre
                className="my-2 overflow-x-auto rounded-xl bg-neutral-900 p-3 font-mono text-[11px] text-neutral-100 shadow-inner"
                {...props}
              >
                {children}
              </pre>
            );
          },
          code: ({ className, children, ...props }: any) => {
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
                className="rounded bg-neutral-200/60 px-1 py-0.5 font-mono text-[11px] font-medium text-emerald-700 dark:bg-neutral-800 dark:text-emerald-400"
                {...props}
              >
                {children}
              </code>
            );
          },
          a: ({ href, children, ...props }: any) => {
            if (href && href.startsWith('/')) {
              return (
                <Link
                  href={href}
                  className="inline-flex items-center gap-1 font-medium text-emerald-600 underline underline-offset-2 hover:text-emerald-700 dark:text-emerald-400 dark:hover:text-emerald-300 transition-colors"
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
                className="inline-flex items-center gap-1 font-medium text-emerald-600 underline underline-offset-2 hover:text-emerald-700 dark:text-emerald-400 dark:hover:text-emerald-300 transition-colors"
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
