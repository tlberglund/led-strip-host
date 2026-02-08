interface BackgroundImageProps {
   visible: boolean;
   imageUrl: string;
}

export function BackgroundImage({ visible, imageUrl }: BackgroundImageProps) {
   if (!visible) return null;

   return (
      <img
         id="background-image"
         src={imageUrl}
         alt="Background"
      />
   );
}
