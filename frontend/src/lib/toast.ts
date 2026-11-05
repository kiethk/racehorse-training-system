import { toast as sonner } from 'sonner';

/** Optional button inside the toast, e.g. "View" to open what the toast is about. */
export interface ToastAction {
  label: string;
  onClick: () => void;
}

export interface ToastOptions {
  action?: ToastAction;
  /** Milliseconds on screen; give toasts with an action time to be clicked. Defaults to the Toaster setting. */
  duration?: number;
}

/**
 * Transient feedback for the result of a user action. Use instead of alert()
 * or a local "success message + setTimeout" state. Errors that block a form
 * still belong next to the form in a <Notice>.
 */
export const toast = {
  success: (message: string, description?: string, options?: ToastOptions) => sonner.success(message, { description, ...options }),
  error: (message: string, description?: string, options?: ToastOptions) => sonner.error(message, { description, ...options }),
  info: (message: string, description?: string, options?: ToastOptions) => sonner.info(message, { description, ...options }),
  warning: (message: string, description?: string, options?: ToastOptions) => sonner.warning(message, { description, ...options }),
};
