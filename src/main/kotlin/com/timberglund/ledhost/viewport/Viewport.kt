package com.timberglund.ledhost.viewport

/**
 * Viewport provides a 2D pixel buffer that patterns can draw to.
 * All coordinates are 0-based, with (0,0) at the top-left corner.
 */
interface Viewport {
   /**
    * Width of the viewport in pixels
    */
   val width: Int

   /**
    * Height of the viewport in pixels
    */
   val height: Int

   /**
    * Sets the color of a single pixel.
    * Coordinates outside the viewport bounds are ignored.
    *
    * @param x X coordinate (0-based)
    * @param y Y coordinate (0-based)
    * @param color Color to set
    */
   fun setPixel(x: Int, y: Int, color: Color)

   /**
    * Gets the color of a single pixel.
    *
    * @param x X coordinate (0-based)
    * @param y Y coordinate (0-based)
    * @return Color at the specified position, or BLACK if out of bounds
    */
   fun getPixel(x: Int, y: Int): Color

   /**
    * Fills the entire viewport with a single color.
    *
    * @param color Color to fill with
    */
   fun fill(color: Color)

   /**
    * Clears the viewport (fills with black).
    */
   fun clear()

   /**
    * Draws a line from (x1, y1) to (x2, y2) using Bresenham's algorithm.
    *
    * @param x1 Starting X coordinate
    * @param y1 Starting Y coordinate
    * @param x2 Ending X coordinate
    * @param y2 Ending Y coordinate
    * @param color Line color
    */
   fun drawLine(x1: Int, y1: Int, x2: Int, y2: Int, color: Color)

   /**
    * Draws a rectangle.
    *
    * @param x Top-left X coordinate
    * @param y Top-left Y coordinate
    * @param width Rectangle width
    * @param height Rectangle height
    * @param color Rectangle color
    * @param filled Whether to fill the rectangle (default: false, outline only)
    */
   fun drawRect(x: Int, y: Int, width: Int, height: Int, color: Color, filled: Boolean = false)

   /**
    * Draws a circle using midpoint circle algorithm.
    *
    * @param cx Center X coordinate
    * @param cy Center Y coordinate
    * @param radius Circle radius
    * @param color Circle color
    * @param filled Whether to fill the circle (default: false, outline only)
    */
   fun drawCircle(cx: Int, cy: Int, radius: Int, color: Color, filled: Boolean = false)

   /**
    * Sets multiple pixels at once (batch operation).
    * More efficient than calling setPixel() repeatedly.
    *
    * @param pixels List of point-color pairs
    */
   fun setPixels(pixels: List<Pair<Point, Color>>)

   /**
    * Returns a copy of the pixel buffer as a 2D array of color integers.
    * Format: buffer[y][x] = color as integer (0xRRGGBB)
    *
    * @return Copy of the pixel buffer
    */
   fun getBuffer(): Array<IntArray>
}
