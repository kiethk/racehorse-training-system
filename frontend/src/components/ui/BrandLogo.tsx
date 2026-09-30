import Image from 'next/image';

interface BrandLogoProps {
  className?: string;
  alt?: string;
}

export function BrandLogo({ className = 'h-8 w-8', alt = 'RTMS logo' }: BrandLogoProps) {
  return (
    <div className={`relative shrink-0 overflow-hidden rounded-full ${className}`}>
      <Image 
        src="/logo.png" 
        alt={alt} 
        fill
        className="object-cover"
        sizes="64px"
      />
    </div>
  );
}
