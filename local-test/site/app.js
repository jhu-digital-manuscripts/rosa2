/**
 * app.js - IIIF Manifest Viewer powered by Mirador 4
 *
 * This module manages the Mirador viewer instance and provides
 * functions for loading IIIF manifests by URL.
 *
 * Mirador is loaded as a UMD global (window.Mirador) from the HTML page.
 * See: https://github.com/ProjectMirador/mirador-integration
 */

"use strict";

/**
 * Application state holding the current Mirador viewer instance.
 * The viewer object exposes a `.store` property (Redux store) for dispatching.
 * Action creators are available on the top-level Mirador global (Mirador.actions).
 */
let miradorInstance = null;

/**
 * A list of sample IIIF manifests for quick testing.
 * Each entry has a human-readable label and a manifest URL.
 */
const SAMPLE_MANIFESTS = [
  {
    label: "Local - Douce 195",
    url: "http://localhost:3000/iiif/rose/Douce195/manifest.json",
  },
  {
    label: "Stanford - Panoramic Photographs",
    url: "https://purl.stanford.edu/sn904cj3429/iiif/manifest",
  },
  {
    label: "Bodleian - Douce 195 (Romance of Alexander)",
    url: "https://iiif.bodleian.ox.ac.uk/iiif/manifest/60834383-7146-41ab-bfe1-48ee97bc04be.json",
  },
  {
    label: "Gallica - BnF Latin 8850",
    url: "https://gallica.bnf.fr/iiif/ark:/12148/btv1b10022508f/manifest.json",
  },
  {
    label: "Yale - Beinecke MS 748 (Book of Hours)",
    url: "https://collections.library.yale.edu/manifests/2005512",
  },
];

/**
 * Initialize the Mirador viewer in the target container element.
 * Creates a fresh viewer instance with an empty workspace.
 *
 * @param {string} containerId - The DOM element id where Mirador will render.
 * @returns {object} The Mirador viewer instance (has .store for Redux dispatch).
 */
function initializeViewer(containerId) {
  if (typeof Mirador === "undefined") {
    console.error("Mirador library not loaded. Check the script tag in HTML.");
    return null;
  }

  miradorInstance = Mirador.viewer({
    id: containerId,
    windows: [],
    window: {
      allowClose: true,
      allowMaximize: true,
      allowFullscreen: true,
    },
    workspace: {
      type: "mosaic",
    },
    workspaceControlPanel: {
      enabled: true,
    },
    theme: {
      palette: {
        primary: {
          main: "#2c3e50",
        },
      },
    },
  });

  return miradorInstance;
}

/**
 * Load a IIIF manifest into the viewer by opening a new window.
 * If the viewer has not been initialized, it will be created first.
 *
 * @param {string} manifestUrl - The URL of the IIIF manifest to display.
 * @param {string} [containerId='mirador-viewer'] - The container element id.
 */
function loadManifest(manifestUrl, containerId) {
  containerId = containerId || "mirador-viewer";

  if (!manifestUrl || !manifestUrl.trim()) {
    showStatus("Please enter a valid manifest URL.", "error");
    return;
  }

  manifestUrl = manifestUrl.trim();

  // Initialize the viewer if it doesn't exist yet
  if (!miradorInstance) {
    miradorInstance = initializeViewer(containerId);
  }

  if (!miradorInstance) {
    showStatus("Failed to initialize the Mirador viewer.", "error");
    return;
  }

  // Dispatch an action to add a new window with the given manifest.
  // In the UMD build, action creators like addWindow are exported
  // directly on the Mirador global object (not nested under .actions).
  const { store } = miradorInstance;
  store.dispatch(Mirador.addWindow({ manifestId: manifestUrl }));

  showStatus("Loading manifest: " + manifestUrl, "success");
}

/**
 * Handle form submission from the manifest URL input.
 * Reads the URL from the input field and loads it into the viewer.
 *
 * @param {Event} event - The form submit event.
 */
function handleFormSubmit(event) {
  event.preventDefault();

  const input = document.getElementById("manifest-url-input");
  if (!input) return;

  loadManifest(input.value);
}

/**
 * Load a sample manifest from the dropdown selector.
 * Populates the input field with the selected URL and triggers loading.
 *
 * @param {string} url - The manifest URL selected from the samples list.
 */
function loadSampleManifest(url) {
  if (!url) return;

  const input = document.getElementById("manifest-url-input");
  if (input) {
    input.value = url;
  }

  loadManifest(url);
}

/**
 * Populate the sample manifests dropdown with available options.
 * Called on page load to build the <select> element options.
 */
function populateSampleManifests() {
  const select = document.getElementById("sample-manifests");
  if (!select) return;

  SAMPLE_MANIFESTS.forEach(function (sample) {
    const option = document.createElement("option");
    option.value = sample.url;
    option.textContent = sample.label;
    select.appendChild(option);
  });
}

/**
 * Display a brief status message to the user.
 *
 * @param {string} message - The message text to display.
 * @param {string} type - The message type: "success", "error", or "info".
 */
function showStatus(message, type) {
  const statusEl = document.getElementById("status-message");
  if (!statusEl) return;

  statusEl.textContent = message;
  statusEl.className = "status-message " + (type || "info");

  // Auto-hide after 5 seconds
  clearTimeout(statusEl._timeout);
  statusEl._timeout = setTimeout(function () {
    statusEl.textContent = "";
    statusEl.className = "status-message";
  }, 5000);
}

/**
 * Initialize the application when the DOM is ready.
 * Sets up event listeners and populates the sample manifests list.
 */
function initApp() {
  // Populate the sample manifests dropdown
  populateSampleManifests();

  // Attach form submit handler
  const form = document.getElementById("manifest-form");
  if (form) {
    form.addEventListener("submit", handleFormSubmit);
  }

  // Attach sample manifest change handler
  const select = document.getElementById("sample-manifests");
  if (select) {
    select.addEventListener("change", function () {
      loadSampleManifest(this.value);
      // Reset dropdown to placeholder after selection
      this.selectedIndex = 0;
    });
  }

  // Initialize the viewer container (empty workspace)
  initializeViewer("mirador-viewer");
}

// Run initialization when the DOM is fully loaded
document.addEventListener("DOMContentLoaded", initApp);
