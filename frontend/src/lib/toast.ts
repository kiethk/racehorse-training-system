import { toast as sonner } from 'sonner';

/**
 * Transient feedback for the result of a user action. Use instead of alert()
 * or a local "success message + setTimeout" state. Errors that block a form
 * still belong next to the form in a <Notice>.
 */
export const toast = {
  success: (message: string, description?: string) => sonner.success(message, { description }),
  error: (message: string, description?: string) => sonner.error(message, { description }),
  info: (message: string, description?: string) => sonner.info(message, { description }),
  warning: (message: string, description?: string) => sonner.warning(message, { description }),
};
