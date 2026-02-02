/**
 * LED Strip Preview Client
 * Connects to the backend WebSocket and renders the LED viewport in real-time
 */
class LEDPreview {
   constructor() {
      this.canvas = document.getElementById('canvas');
      this.ctx = this.canvas.getContext('2d');
      this.ledCanvas = document.getElementById('led-canvas');
      this.ledCtx = this.ledCanvas.getContext('2d');
      this.ws = null;
      this.connected = false;
      this.pixelSize = 10; // Size of each LED in viewport view
      this.ledSize = 5; // Size of each LED square in strips view (half of pixelSize)
      this.width = 0;
      this.height = 0;
      this.reconnectTimeout = null;
      this.showViewport = true;
      this.showStrips = false;
      this.ledStripsUpdateInterval = null;

      this.connect();
      this.setupControls();
      this.loadPatterns();
      this.startStatsUpdate();
   }

   connect() {
      const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
      const wsUrl = `${protocol}//${window.location.host}/viewport`;

      console.log('Connecting to', wsUrl);
      this.ws = new WebSocket(wsUrl);

      this.ws.onopen = () => {
         this.connected = true;
         this.updateConnectionStatus();
         console.log('Connected to LED server');

         // Clear any pending reconnect
         if(this.reconnectTimeout) {
            clearTimeout(this.reconnectTimeout);
            this.reconnectTimeout = null;
         }
      };

      this.ws.onclose = () => {
         this.connected = false;
         this.updateConnectionStatus();
         console.log('Disconnected from LED server');

         // Reconnect after 2 seconds
         if(!this.reconnectTimeout) {
            this.reconnectTimeout = setTimeout(() => {
               this.reconnectTimeout = null;
               this.connect();
            }, 2000);
         }
      };

      this.ws.onerror = (error) => {
         console.error('WebSocket error:', error);
      };

      this.ws.onmessage = (event) => {
         try {
            const data = JSON.parse(event.data);
            if (data.type === 'viewport') {
               this.renderViewport(data);
            }
         }
         catch (e) {
            console.error('Failed to parse message:', e);
         }
      };
   }

   renderViewport(data) {
      // Resize canvas if needed
      if(this.width !== data.width || this.height !== data.height) {
         this.width = data.width;
         this.height = data.height;
         this.canvas.width = this.width * this.pixelSize;
         this.canvas.height = this.height * this.pixelSize;
         document.getElementById('resolution').textContent =
            `${this.width}x${this.height}`;
      }

      // Decode Base64 RGBA bitmap
      const binaryString = atob(data.data);
      const bytes = new Uint8Array(binaryString.length);
      for (let i = 0; i < binaryString.length; i++) {
         bytes[i] = binaryString.charCodeAt(i);
      }

      // Create image data for the canvas
      const imageData = this.ctx.createImageData(
         this.width * this.pixelSize,
         this.height * this.pixelSize
      );

      // Render each pixel from the decoded bitmap
      for(let y = 0; y < this.height; y++) {
         for(let x = 0; x < this.width; x++) {
            const pixelIdx = (y * this.width + x) * 4;
            const r = bytes[pixelIdx];
            const g = bytes[pixelIdx + 1];
            const b = bytes[pixelIdx + 2];

            // Draw pixelSize x pixelSize block for each LED
            for (let py = 0; py < this.pixelSize; py++) {
               for (let px = 0; px < this.pixelSize; px++) {
                  const idx = ((y * this.pixelSize + py) * this.canvas.width + (x * this.pixelSize + px)) * 4;
                  imageData.data[idx] = r;
                  imageData.data[idx + 1] = g;
                  imageData.data[idx + 2] = b;
                  imageData.data[idx + 3] = 255;
               }
            }
         }
      }

      this.ctx.putImageData(imageData, 0, 0);
   }

   updateConnectionStatus() {
      const status = document.getElementById('connection-status');
      if (this.connected) {
         status.textContent = 'Connected';
         status.className = 'connected';
      } else {
         status.textContent = 'Disconnected';
         status.className = 'disconnected';
      }
   }

   async loadPatterns() {
      try {
         const response = await fetch('/api/patterns');
         const patterns = await response.json();
         const select = document.getElementById('pattern-select');

         if(patterns.length === 0) {
            select.innerHTML = '<option value="">No patterns available</option>';
         }
         else {
            select.innerHTML = patterns.map(p =>
               `<option value="${p}">${p}</option>`
            ).join('');
         }
      }
      catch (e) {
         console.error('Failed to load patterns:', e);
         const select = document.getElementById('pattern-select');
         select.innerHTML = '<option value="">Error loading patterns</option>';
      }
   }

   setupControls() {
      // Speed slider
      document.getElementById('speed').addEventListener('input', (e) => {
         document.getElementById('speed-value').textContent = `${parseFloat(e.target.value).toFixed(1)}x`;
      });

      // Brightness slider
      document.getElementById('brightness').addEventListener('input', (e) => {
         document.getElementById('brightness-value').textContent = `${e.target.value}%`;
      });

      // Pattern select - apply on change
      document.getElementById('pattern-select').addEventListener('change', () => {
         this.applyPattern();
      });
   }

   async applyPattern() {
      const pattern = document.getElementById('pattern-select').value;
      if(!pattern) {
         return;
      }

      const speed = parseFloat(document.getElementById('speed').value);
      const brightness = parseInt(document.getElementById('brightness').value) / 100;

      try {
         const response = await fetch(`/api/pattern/${pattern}`, {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({speed, brightness})
         });

         if(!response.ok) {
            console.error('Failed to apply pattern:', response.status);
         }
      }
      catch (e) {
         console.error('Exception applying pattern:', e);
      }
   }

   async startStatsUpdate() {
      setInterval(async () => {
         if(!this.connected) {
            return;
         }

         try {
            // Update stats
            const statsResponse = await fetch('/api/stats');
            const stats = await statsResponse.json();

            document.getElementById('fps').textContent = stats.fps.toFixed(1);
            document.getElementById('frame-time').textContent = `${stats.frameTime.toFixed(2)}ms`;

            // Update client count
            const clientsResponse = await fetch('/api/clients');
            const clientsData = await clientsResponse.json();
            document.getElementById('clients').textContent = clientsData.count;
         }
         catch(e) {
            // Silently ignore errors (server might not be ready)
         }
      }, 1000);
   }

   toggleView() {
      // Get checkbox states
      this.showViewport = document.getElementById('viewport-view-checkbox').checked;
      this.showStrips = document.getElementById('strips-view-checkbox').checked;

      // Show/hide viewport canvas
      this.canvas.style.display = this.showViewport ? 'block' : 'none';

      // Show/hide LED strips canvas
      this.ledCanvas.style.display = this.showStrips ? 'block' : 'none';

      // Handle LED strips polling
      if (this.showStrips) {
         // Initialize LED canvas size if needed
         if (this.width > 0 && this.height > 0) {
            this.ledCanvas.width = this.width * this.pixelSize;
            this.ledCanvas.height = this.height * this.pixelSize;
         }

         // Start LED strips polling if not already running
         this.fetchAndRenderLEDStrips();
         if (!this.ledStripsUpdateInterval) {
            this.ledStripsUpdateInterval = setInterval(() => {
               this.fetchAndRenderLEDStrips();
            }, 50); // Update at 20 FPS
         }
      } else {
         // Stop LED strips polling when not shown
         if (this.ledStripsUpdateInterval) {
            clearInterval(this.ledStripsUpdateInterval);
            this.ledStripsUpdateInterval = null;
         }
      }
   }

   async fetchAndRenderLEDStrips() {
      try {
         const response = await fetch('/api/led-strips');
         const strips = await response.json();
         this.renderLEDStrips(strips);
      } catch (e) {
         console.error('Failed to fetch LED strips:', e);
      }
   }

   renderLEDStrips(strips) {
      // Clear canvas with transparency
      this.ledCtx.clearRect(0, 0, this.ledCanvas.width, this.ledCanvas.height);

      // Render each LED at its viewport position
      strips.forEach(strip => {
         strip.leds.forEach(led => {
            // Calculate pixel position on canvas
            const canvasX = led.x * this.pixelSize;
            const canvasY = led.y * this.pixelSize;

            // Center the LED square within the pixel area
            const offsetX = (this.pixelSize - this.ledSize) / 2;
            const offsetY = (this.pixelSize - this.ledSize) / 2;

            // Draw LED as a small square
            this.ledCtx.fillStyle = `rgb(${led.r}, ${led.g}, ${led.b})`;
            this.ledCtx.fillRect(
               canvasX + offsetX,
               canvasY + offsetY,
               this.ledSize,
               this.ledSize
            );
         });
      });
   }
}

// Global functions for button onclick
function applyPattern() {
   window.preview.applyPattern();
}

function toggleView() {
   window.preview.toggleView();
}

// Initialize when page loads
window.addEventListener('load', () => {
   window.preview = new LEDPreview();
});
