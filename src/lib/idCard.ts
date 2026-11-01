/**
 * ID Card Rendering
 * Generates a PNG image with MRZ lines, face photo, and document details.
 */

import { Paths, File } from 'expo-file-system';

const CARD_WIDTH = 1080; // px
const CARD_HEIGHT = 680; // px
const MRZ_LINE_HEIGHT = 60;
const FACE_X = 750;
const FACE_Y = 100;
const FACE_SIZE = 200;

interface IdCardOptions {
  name: string;
  dob: string;
  nationality: string;
  idNumber: string;
  mrzLines: string[];
  faceUri?: string; // optional face image URI
}

/**
 * Draw a circular image on canvas context.
 */
function drawCircleImage(
  ctx: CanvasRenderingContext2D,
  image: HTMLImageElement,
  cx: number,
  cy: number,
  radius: number
) {
  ctx.save();
  ctx.beginPath();
  ctx.arc(cx, cy, radius, 0, Math.PI * 2);
  ctx.closePath();
  ctx.clip();
  // Calculate source rectangle to draw the center of the image
  const size = Math.min(image.width, image.height);
  const sx = (image.width - size) / 2;
  const sy = (image.height - size) / 2;
  ctx.drawImage(image, sx, sy, size, size, cx - radius, cy - radius, radius * 2, radius * 2);
  ctx.restore();
}

/**
 * Generate an ID card PNG at the given output path.
 * Returns the path to the generated file.
 */
export async function renderIdCard(options: IdCardOptions, outputPath?: string): Promise<string> {
  // Use HTML canvas approach (works in Expo with proper setup)
  const canvas = document.createElement('canvas') as unknown as HTMLCanvasElement;
  const ctx = canvas.getContext('2d') as CanvasRenderingContext2D | null;
  if (!ctx) throw new Error('Canvas context not available');

  canvas.width = CARD_WIDTH;
  canvas.height = CARD_HEIGHT;

  // ── Background ──
  ctx.fillStyle = '#0f0a1e';
  ctx.fillRect(0, 0, CARD_WIDTH, CARD_HEIGHT);

  // ── Gradient header ──
  const gradient = ctx.createLinearGradient(0, 0, CARD_WIDTH, 0);
  gradient.addColorStop(0, '#7c3aed');
  gradient.addColorStop(1, '#c425fa');
  ctx.fillStyle = gradient;
  ctx.fillRect(0, 0, CARD_WIDTH, 80);

  // ── Header text ──
  ctx.fillStyle = '#ffffff';
  ctx.font = 'bold 32px sans-serif';
  ctx.textAlign = 'center';
  ctx.fillText('KYC BYPASS SUITE', CARD_WIDTH / 2, 52);

  // ── Face area ──
  const faceRadius = FACE_SIZE / 2;
  const faceCx = FACE_X + FACE_SIZE / 2;
  const faceCy = FACE_Y + FACE_SIZE / 2;

  if (options.faceUri) {
    // Load the face image
    const faceImage = document.createElement('img') as HTMLImageElement;
    faceImage.src = options.faceUri;
    faceImage.onload = () => {
      // Draw circular face photo
      drawCircleImage(ctx, faceImage, faceCx, faceCy, faceRadius - 2);
      // Draw border
      ctx.save();
      ctx.beginPath();
      ctx.arc(faceCx, faceCy, faceRadius, 0, Math.PI * 2);
      ctx.closePath();
      ctx.strokeStyle = '#c425fa';
      ctx.lineWidth = 4;
      ctx.stroke();
      ctx.restore();
    };
    faceImage.onerror = () => {
      // Fallback to placeholder
      ctx.fillStyle = '#1a1230';
      ctx.beginPath();
      ctx.arc(faceCx, faceCy, faceRadius, 0, Math.PI * 2);
      ctx.fill();
      ctx.strokeStyle = '#c425fa';
      ctx.lineWidth = 3;
      ctx.stroke();
      ctx.fillStyle = '#8b5cf6';
      ctx.font = '60px sans-serif';
      ctx.textAlign = 'center';
      ctx.fillText('👤', faceCx, faceCy + 18);
    };
    // If image is already loaded, draw it immediately
    if (faceImage.complete) {
      drawCircleImage(ctx, faceImage, faceCx, faceCy, faceRadius - 2);
      ctx.save();
      ctx.beginPath();
      ctx.arc(faceCx, faceCy, faceRadius, 0, Math.PI * 2);
      ctx.closePath();
      ctx.strokeStyle = '#c425fa';
      ctx.lineWidth = 4;
      ctx.stroke();
    }
  } else {
    // Draw placeholder
    ctx.fillStyle = '#1a1230';
    ctx.beginPath();
    ctx.arc(faceCx, faceCy, faceRadius, 0, Math.PI * 2);
    ctx.fill();
    ctx.strokeStyle = '#c425fa';
    ctx.lineWidth = 3;
    ctx.stroke();
    ctx.fillStyle = '#8b5cf6';
    ctx.font = '60px sans-serif';
    ctx.textAlign = 'center';
    ctx.fillText('👤', faceCx, faceCy + 18);
  }

  // ── Name ──
  ctx.fillStyle = '#ffffff';
  ctx.font = 'bold 28px sans-serif';
  ctx.textAlign = 'left';
  // Truncate long names
  const displayName = options.name.length > 35 ? options.name.slice(0, 35) + '…' : options.name;
  ctx.fillText(displayName, 60, 130);

  // ── Details ──
  ctx.font = '22px sans-serif';
  ctx.fillStyle = '#a78bfa';
  const details = [
    `DOB: ${options.dob}`,
    `Nationality: ${options.nationality}`,
    `ID Number: ${options.idNumber}`,
  ];

  details.forEach((d, i) => {
    ctx.fillText(d, 60, 180 + i * 40);
  });

  // ── MRZ lines ──
  ctx.fillStyle = '#4ade80';
  ctx.font = '18px monospace';
  ctx.textAlign = 'left';
  options.mrzLines.forEach((line, i) => {
    ctx.fillText(line, 60, 380 + i * MRZ_LINE_HEIGHT);
  });

  // ── Bottom bar ──
  const bottomGradient = ctx.createLinearGradient(0, CARD_HEIGHT - 50, CARD_WIDTH, CARD_HEIGHT - 50);
  bottomGradient.addColorStop(0, '#7c3aed');
  bottomGradient.addColorStop(1, '#c425fa');
  ctx.fillStyle = bottomGradient;
  ctx.fillRect(0, CARD_HEIGHT - 50, CARD_WIDTH, 50);

  ctx.fillStyle = '#ffffff';
  ctx.font = '16px sans-serif';
  ctx.textAlign = 'center';
  ctx.fillText('Generated by KYC Bypass Suite', CARD_WIDTH / 2, CARD_HEIGHT - 20);

  // ── Output ──
  const output = outputPath || `${Paths.cache.uri}id_card_${Date.now()}.png`;

  // Convert canvas to base64 PNG and write to file
  const dataUrl = canvas.toDataURL('image/png');
  const base64 = dataUrl.split(',')[1];

  const outputFile = new File(Paths.cache, `id_card_${Date.now()}.png`);
  await outputFile.write(base64, { encoding: 'base64' });

  return outputFile.uri;
}

/**
 * Generate ID card PNG synchronously-ish (returns Promise).
 */
export async function generateIdCardPath(options: IdCardOptions): Promise<string> {
  const outputFile = new File(Paths.cache, `id_card_${Date.now()}.png`);
  return renderIdCard(options, outputFile.uri);
}
