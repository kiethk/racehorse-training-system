/**
 * Shared UI primitives. Feature code imports from here and never re-implements
 * a control, overlay, table or badge locally. See AGENTS.md for the lookup table.
 */
export { Avatar } from './Avatar';
export { BackLink } from './BackLink';
export { AvatarUpload } from './AvatarUpload';
export { BrandLogo } from './BrandLogo';
export { Button, IconButton, LinkButton, buttonClassName } from './Button';
export { ConfirmDialog } from './ConfirmDialog';
export { DataTable } from './DataTable';
export { createDragDrop } from './DragDrop';
export {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from './DropdownMenu';
export { FilterBar } from './FilterBar';
export { FormField } from './FormField';
export { HorseAvatar } from './HorseAvatar';
export { Icon, type IconName } from './Icon';
export { ImageLightbox, ImageThumbnail, type LightboxImage } from './ImageLightbox';
export { Checkbox, ChoiceInput, FileInput, Input, SearchInput, Select, Textarea } from './Input';
export { MetricCard } from './MetricCard';
export { Modal } from './Modal';
export { Notice } from './Notice';
export { PageHeader } from './PageHeader';
export { Pagination } from './Pagination';
export { FieldLabel, Panel, SectionTitle } from './Panel';
export { ProgressBar, type ProgressSegment } from './ProgressBar';
export { ProtectedImage } from './ProtectedImage';
export { Popover, PopoverContent, PopoverTrigger } from './Popover';
export { PageSection, ScreenLayout } from './ScreenLayout';
export { FilterChips, SegmentedControl, type SegmentOption } from './SegmentedControl';
export { SelectableCard } from './SelectableCard';
export { Skeleton } from './Skeleton';
export { LoadingScreen, Spinner } from './Spinner';
export { DetailSkeleton, EmptyState, ListSkeleton } from './states';
export { PeriodBarChart, type PeriodBarChartProps } from './PeriodBarChart';
export { Pill, StatusBadge } from './StatusBadge';
export { Table, TBody, Td, Th, THead, Tr } from './Table';
export { Tabs, type TabItem } from './Tabs';
export { Tooltip } from './Tooltip';
export { PdfViewer } from './PdfViewer';
