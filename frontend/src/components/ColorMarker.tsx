export const ColorMarker = ({ color }: { color?: string }) =>
  !!color && <div aria-hidden={true} style={{ backgroundColor: color }} className="absolute -left-2 top-0 bottom-0 w-3.5"></div>
