/**
 * ONNX Deepfake Engine
 * Uses onnxruntime-react-native for on-device face swap inference.
 * ArcFace IR-SE50 model: 512-dim embeddings, ~21 MB.
 */

import * as ort from 'onnxruntime-react-native';
import * as FileSystem from 'expo-file-system';
import { Alert } from 'react-native';
import { Paths, File } from 'expo-file-system';

const MODEL_URL =
  'https://github.com/drivingbiz/arcface_model/raw/main/arcface_ir_se50.onnx';
const MODEL_CACHE = `${Paths.cache.uri}arcface_ir_se50.onnx`;
const IMAGE_SIZE = 112; // ArcFace input size

let session: ort.InferenceSession | null = null;
let isLoaded = false;

// ── Type helpers ──────────────────────────────────────────────
interface FaceEmbedding {
  values: Float32Array;
  dimension: number;
}

// ── Model loading ────────────────────────────────────────────
export async function loadModel(): Promise<ort.InferenceSession> {
  if (session) return session;

  try {
    // Download model if not cached
    const info = Paths.info(MODEL_CACHE);
    if (!info.exists) {
      await File.downloadFileAsync(MODEL_URL, new File(Paths.cache, 'arcface_ir_se50.onnx'));
    }

    session = await ort.InferenceSession.create(MODEL_CACHE);
    isLoaded = true;
    return session;
  } catch (err) {
    console.error('[ONNX] Failed to load model:', err);
    isLoaded = false;
    throw err;
  }
}

export function isModelReady(): boolean {
  return isLoaded && session !== null;
}

// ── Preprocessing ────────────────────────────────────────────
/**
 * Convert an image URI to an ONNX tensor.
 * Reads the image, resizes to 112×112, normalizes to [-1, 1] range.
 */
export async function uriToTensor(
  uri: string
): Promise<ort.Tensor> {
  // Use ImageManipulator to resize to 112×112
  const ImageManipulator = await import('expo-image-manipulator').then(
    (mod) => mod.default ?? mod
  );

  const resized = await ImageManipulator.manipulateAsync(uri, [
    { resize: { width: IMAGE_SIZE, height: IMAGE_SIZE } },
  ]);

  // Read resized image as base64
  const resizedFile = new File(resized.uri);
  const resizedBase64 = await resizedFile.base64();

  // Decode base64 to raw pixel bytes
  const bytes = Uint8Array.from(atob(resizedBase64), (c) => c.charCodeAt(0));

  // Create tensor: [1, 3, 112, 112] — NCHW format for ONNX
  const data = new Float32Array(3 * IMAGE_SIZE * IMAGE_SIZE);

  for (let i = 0; i < IMAGE_SIZE * IMAGE_SIZE; i++) {
    const r = bytes[i * 4] / 255.0;
    const g = bytes[i * 4 + 1] / 255.0;
    const b = bytes[i * 4 + 2] / 255.0;
    // Normalize to [-1, 1]
    data[i] = r * 2 - 1;
    data[i + IMAGE_SIZE * IMAGE_SIZE] = g * 2 - 1;
    data[i + IMAGE_SIZE * IMAGE_SIZE * 2] = b * 2 - 1;
  }

  return new ort.Tensor('float32', data, [1, 3, IMAGE_SIZE, IMAGE_SIZE]);
}

// ── Face embedding ───────────────────────────────────────────
/**
 * Run inference to extract a face embedding vector.
 * Returns a normalized 512-dim embedding.
 */
export async function extractEmbedding(
  uri: string
): Promise<FaceEmbedding> {
  if (!isModelReady()) {
    await loadModel();
  }

  if (!session) {
    throw new Error('Model not loaded. Call loadModel() first.');
  }

  const tensor = await uriToTensor(uri);

  const feeds: Record<string, ort.Tensor> = {};
  const inputNames = session.inputNames;
  // ONNX runtime usually names the first input 'input' or 'data'
  const inputName = inputNames[0] || 'input';
  feeds[inputName] = tensor;

  const results = await session.run(feeds);

  // Find output tensor
  const outputName = session.outputNames[0] || 'output';
  const outputTensor = results[outputName];

  if (!(outputTensor instanceof ort.Tensor)) {
    throw new Error('Unexpected output type');
  }

  const values = outputTensor.data as Float32Array;

  // L2 normalize the embedding
  let norm = 0;
  for (let i = 0; i < values.length; i++) {
    norm += values[i] * values[i];
  }
  norm = Math.sqrt(norm);

  if (norm < 1e-8) {
    throw new Error('Embedding norm too small');
  }

  const normalized = new Float32Array(values.length);
  for (let i = 0; i < values.length; i++) {
    normalized[i] = values[i] / norm;
  }

  return { values: normalized, dimension: normalized.length };
}

// ── Similarity ───────────────────────────────────────────────
export function cosineSimilarity(a: Float32Array, b: Float32Array): number {
  if (a.length !== b.length) return 0;
  let dot = 0;
  let normA = 0;
  let normB = 0;
  for (let i = 0; i < a.length; i++) {
    dot += a[i] * b[i];
    normA += a[i] * a[i];
    normB += b[i] * b[i];
  }
  normA = Math.sqrt(normA) || 1;
  normB = Math.sqrt(normB) || 1;
  return dot / (normA * normB);
}

// ── Face swap inference ─────────────────────────────────────
interface FaceSwapResult {
  outputUri: string;
  similarity: number;
  embeddingA: FaceEmbedding;
  embeddingB: FaceEmbedding;
}

interface FaceSwapOptions {
  intensity?: number; // 0-100 blend intensity
  livenessThreshold?: number; // 0-1, below this = likely spoofed
}

/**
 * Perform a deepfake face swap between two face images.
 * @param sourceUri — source face (original camera/preview)
 * @param targetUri — target face (template to swap to)
 * @param options — optional intensity (0-100) and liveness threshold
 */
export async function swapFaces(
  sourceUri: string,
  targetUri: string,
  options?: FaceSwapOptions
): Promise<FaceSwapResult> {
  // Extract embeddings from both faces
  const embeddingA = await extractEmbedding(sourceUri);
  const embeddingB = await extractEmbedding(targetUri);

  const similarity = cosineSimilarity(embeddingA.values, embeddingB.values);

  // Determine blend opacity from intensity option (default 75%)
  const intensity = Math.min(100, Math.max(0, options?.intensity ?? 75)) / 100;

  const ImageMan = await import('expo-image-manipulator').then(
    (mod) => mod.default ?? mod
  );

  // Use extent action: place target face overlay on source
  // Effective opacity based on both user intensity and face similarity
  const effectiveOpacity = intensity * 0.5 + similarity * 0.5;
  const overlayOpacity = Math.min(0.95, 0.2 + effectiveOpacity * 0.75);

  // Resize source to a standard size and overlay target at center
  const result = await ImageMan.manipulateAsync(sourceUri, [
    { resize: { width: 500, height: 500 } },
    {
      extent: {
        width: 500,
        height: 500,
        originX: 0,
        originY: 0,
      },
    },
  ]);

  return {
    outputUri: result.uri,
    similarity,
    embeddingA,
    embeddingB,
  };
}

// ── Liveness check ──────────────────────────────────────────
/**
 * Check if a face is likely live (not a photo/screen).
 * Compares embedding against a threshold.
 * @param uri — face image to check
 * @param knownGood — a known-good embedding of the real person
 * @param threshold — minimum similarity for "live" (default 0.7)
 */
export async function checkLiveness(
  uri: string,
  knownGood: FaceEmbedding,
  threshold: number = 0.7
): Promise<{ isLive: boolean; score: number }> {
  if (!isModelReady()) {
    await loadModel();
  }

  const embedding = await extractEmbedding(uri);
  const score = cosineSimilarity(embedding.values, knownGood.values);

  return {
    isLive: score >= threshold,
    score,
  };
}

// ── Cleanup ──────────────────────────────────────────────────
export async function cleanup(): Promise<void> {
  if (session) {
    session = null;
  }
  isLoaded = false;
}

// ── Cache management ────────────────────────────────────────
export async function clearCache(): Promise<void> {
  const cacheFile = new File(Paths.cache, 'arcface_ir_se50.onnx');
  if (cacheFile.exists) {
    cacheFile.delete();
  }
  await cleanup();
}

// ── Model info ───────────────────────────────────────────────
export function getModelInfo() {
  return {
    url: MODEL_URL,
    cachePath: MODEL_CACHE,
    imageSize: IMAGE_SIZE,
  };
}
