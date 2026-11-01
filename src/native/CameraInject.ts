/**
 * CameraInject Native Module Bridge
 *
 * Provides a JavaScript interface to the native Android CameraInject module.
 * Allows injecting captured camera frames into Android's external camera,
 * making the feed available to other apps (WhatsApp, KYC apps, etc.)
 */

import { NativeModules } from 'react-native';

const CameraInject = NativeModules.CameraInject;

if (!CameraInject) {
  console.warn('[CameraInject] Native module not found. Injection may not work.');
}

export type FpsStats = {
  current: number;
  actual: number;
  width: number;
  height: number;
};

export type CameraIdList = string[];

export type InjectOptions = {
  /** Target width for injected frames (default: 1920) */
  width?: number;
  /** Target height for injected frames (default: 1080) */
  height?: number;
  /** Target frame rate (default: 15) */
  fps?: number;
};

/**
 * Start injecting camera frames into the external camera.
 * @param options Resolution and FPS settings
 * @returns true if injection started successfully
 */
export async function startInject(options: InjectOptions = {}): Promise<boolean> {
  if (!CameraInject) return false;

  const width = options.width ?? 1280;
  const height = options.height ?? 720;
  const fps = options.fps ?? 15;

  return CameraInject.startInject(width, height, fps);
}

/**
 * Inject a single frame from RGBA byte array.
 * @param rgbaData Uint8Array of RGBA pixel data (width × height × 4 bytes)
 * @returns true if frame was injected
 */
export async function injectFrame(rgbaData: Uint8Array): Promise<boolean> {
  if (!CameraInject) return false;

  // Convert to ReadableArray for native module
  const array = [];
  for (let i = 0; i < rgbaData.length; i++) {
    array.push(rgbaData[i]);
  }

  return CameraInject.injectFrame(array) || false;
}

/**
 * Inject a single frame from a PNG/JPEG image URI.
 * The native module will decode and resize the image.
 * @param imageUri The file:// URI of the image
 * @returns true if frame was injected
 */
export async function injectFrameFromUri(imageUri: string): Promise<boolean> {
  if (!CameraInject) return false;
  return CameraInject.injectFrameFromUri(imageUri) || false;
}

/**
 * Stop injection and release resources.
 * @returns true if stopped successfully
 */
export async function stopInject(): Promise<boolean> {
  if (!CameraInject) return false;
  return CameraInject.stopInject() || false;
}

/**
 * Check if injection is currently active.
 * @returns true if injection is active
 */
export async function isInjecting(): Promise<boolean> {
  if (!CameraInject) return false;
  return CameraInject.isInjecting() || false;
}

/**
 * Get current FPS and resolution stats.
 * @returns FPS stats object
 */
export async function getFpsStats(): Promise<FpsStats | null> {
  if (!CameraInject) return null;
  return CameraInject.getFpsStats() || null;
}

/**
 * Get list of available camera IDs.
 * @returns Array of camera ID strings
 */
export async function getCameraIds(): Promise<CameraIdList> {
  if (!CameraInject) return [];
  return CameraInject.getCameraIds() || [];
}
