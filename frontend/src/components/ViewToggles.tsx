interface ViewTogglesProps {
   showViewport: boolean;
   showStrips: boolean;
   showBackground: boolean;
   hasBackgroundImage: boolean;
   onShowViewportChange: (show: boolean) => void;
   onShowStripsChange: (show: boolean) => void;
   onShowBackgroundChange: (show: boolean) => void;
}

export function ViewToggles({
   showViewport,
   showStrips,
   showBackground,
   hasBackgroundImage,
   onShowViewportChange,
   onShowStripsChange,
   onShowBackgroundChange,
}: ViewTogglesProps) {
   return (
      <div className="view-controls">
         <div className="checkbox-group">
            <input
               type="checkbox"
               id="viewport-view-checkbox"
               checked={showViewport}
               onChange={(e) => onShowViewportChange(e.target.checked)}
            />
            <label htmlFor="viewport-view-checkbox">Show Viewport</label>
         </div>
         <div className="checkbox-group">
            <input
               type="checkbox"
               id="strips-view-checkbox"
               checked={showStrips}
               onChange={(e) => onShowStripsChange(e.target.checked)}
            />
            <label htmlFor="strips-view-checkbox">Show LED Strips</label>
         </div>
         {hasBackgroundImage && (
            <div className="checkbox-group">
               <input
                  type="checkbox"
                  id="background-view-checkbox"
                  checked={showBackground}
                  onChange={(e) => onShowBackgroundChange(e.target.checked)}
               />
               <label htmlFor="background-view-checkbox">Show Background</label>
            </div>
         )}
      </div>
   );
}
