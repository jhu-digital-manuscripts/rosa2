import { describe, it, expect } from 'vitest';
import { canvasImageId, canvasName, canvasIdForName, nameForCanvasId } from './canvasNames';

const base = 'https://example.org/iiif/rose/Add12042';

function canvas(index, imagePath) {
  return {
    id: `${base}/canvas/${index}`,
    type: 'Canvas',
    items: [
      {
        type: 'AnnotationPage',
        items: [
          {
            type: 'Annotation',
            body: {
              type: 'Image',
              service: [{ id: `https://images.example.org/iiif/${encodeURIComponent(imagePath)}` }],
            },
          },
        ],
      },
    ],
  };
}

const manifest = {
  items: [
    canvas(0, 'rose/Add12042/Add12042.binding.frontcover'),
    canvas(1, 'rose/Add12042/cropped/Add12042.001r'),
    canvas(2, 'rose/Add12042/Add12042.001v'),
    canvas(3, 'rose/missing_image'),
  ],
};

describe('canvas names', () => {
  it('reads the decoded image identifier from the image service', () => {
    expect(canvasImageId(manifest.items[1])).toBe('rose/Add12042/cropped/Add12042.001r');
    expect(canvasImageId({})).toBeNull();
  });

  it('names a canvas by its image without the book prefix', () => {
    expect(canvasName(manifest.items[0], 0, 'Add12042')).toBe('binding.frontcover');
    expect(canvasName(manifest.items[1], 1, 'Add12042')).toBe('001r');
    expect(canvasName(manifest.items[2], 2, 'Add12042')).toBe('001v');
  });

  it('names a canvas without an image of its own by index', () => {
    expect(canvasName(manifest.items[3], 3, 'Add12042')).toBe('i3');
    expect(canvasName({}, 7, 'Add12042')).toBe('i7');
  });

  it('maps between canvas ids and names', () => {
    expect(nameForCanvasId(manifest, `${base}/canvas/2`, 'Add12042')).toBe('001v');
    expect(nameForCanvasId(manifest, `${base}/canvas/99`, 'Add12042')).toBeNull();
    expect(canvasIdForName(manifest, '001v', 'Add12042')).toBe(`${base}/canvas/2`);
    expect(canvasIdForName(manifest, 'i3', 'Add12042')).toBe(`${base}/canvas/3`);
    expect(canvasIdForName(manifest, '999r', 'Add12042')).toBeNull();
    expect(canvasIdForName(manifest, null, 'Add12042')).toBeNull();
  });
});
