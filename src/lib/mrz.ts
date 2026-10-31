/**
 * MRZ (Machine Readable Zone) – ICAO 9303 compliant computation.
 * Supports T=1 (passport, 2×30 chars) and T=2 (driving license, 2×30 chars).
 */

// ── Checksum helpers ──────────────────────────────────────────

const WEIGHTS_P1 = [8, 7, 9, 5, 3, 0, 2, 1, 6, 4, 8, 7, 9, 5, 3, 0, 2, 1]; // passport field 1
const WEIGHTS_P2 = [7, 9, 5, 3, 0, 2, 1, 8, 6, 4, 7, 9, 5, 3, 0, 2, 1, 8]; // passport field 2

const CHARS_P1 = '0123456789<';
const CHARS_P2 = '0123456789<';

function padRight(s: string, len: number): string {
  return s.padEnd(len, '<');
}

function padLeft(s: string, len: number): string {
  return s.padStart(len, '<');
}

function toUpperNoSpaces(s: string): string {
  return s.toUpperCase().replace(/[^A-Z0-9<]/g, '');
}

// Compute check digit for a field using weights
function computeCheck(field: string, weights: number[], charSet: string): string {
  let sum = 0;
  for (let i = 0; i < field.length; i++) {
    const idx = charSet.indexOf(field[i]);
    if (idx === -1) {
      console.warn(`[MRZ] Unknown char '${field[i]}' in check field`);
      sum += 0;
    } else {
      sum += idx * weights[i % weights.length];
    }
  }
  return String((sum % 10)).padStart(1, '0');
}

// ── Passport (T=1): 3 lines of 30 chars ──────────────────────
interface PassportMrz {
  line1: string;
  line2: string;
  line3: string;
}

function computePassportMrz(
  fullName: string,
  nationality: string,
  idNumber: string,
  dob: string,
  sex: string = 'M',
  expiry: string = '250101' // YYYYMMDD
): PassportMrz | null {
  const surnamePart = fullName.includes(' ') ? fullName.slice(fullName.lastIndexOf(' ') + 1) : fullName;
  const givenNamesPart = fullName.includes(' ') ? fullName.slice(0, fullName.lastIndexOf(' ')) : '';

  // Field 1: Surname << GivenNames  (30 chars)
  const field1 = padRight(`${surnamePart}<<${givenNamesPart}`, 30);

  // Field 2: Passport number << dob << sex  (30 chars)
  const field2Base = padRight(padLeft(idNumber, 9), 9)
    + padRight(dob, 9)
    + sex[0].toUpperCase()
    + padRight('', 1);
  const field2 = padRight(field2Base, 30);

  // Field 3: Nationality << expiry  (30 chars)
  const field3Base = padRight(nationality, 3)
    + padRight(expiry, 7)
    + padRight('', 1);
  const field3 = padRight(field3Base, 30);

  // Compute check digits
  const p1 = field1.slice(0, 18);
  const p1Check = computeCheck(p1, WEIGHTS_P1, CHARS_P1);

  const p2 = field2.slice(0, 18);
  const p2Check = computeCheck(p2, WEIGHTS_P2, CHARS_P2);

  const p3 = field3.slice(0, 17);
  const p3Check = computeCheck(p3, WEIGHTS_P1, CHARS_P1);

  return {
    line1: `${field1}${p1Check}`,
    line2: `${field2}${p2Check}`,
    line3: `${field3}${p3Check}`,
  };
}

// ── Driving License (T=2): 2 lines of 30 chars ───────────────
interface LicenseMrz {
  line1: string;
  line2: string;
}

function computeLicenseMrz(
  fullName: string,
  licenseNumber: string,
  dob: string,
  sex: string = 'M',
  expiry: string = '250101'
): LicenseMrz | null {
  const surnamePart = fullName.includes(' ') ? fullName.slice(fullName.lastIndexOf(' ') + 1) : fullName;
  const givenNamesPart = fullName.includes(' ') ? fullName.slice(0, fullName.lastIndexOf(' ')) : '';

  // Field 1: Surname << GivenNames  (30 chars)
  const field1 = padRight(`${surnamePart}<<${givenNamesPart}`, 30);

  // Field 2: license << dob << sex << expiry
  const field2Base = padRight(padLeft(licenseNumber, 9), 9)
    + padRight(dob, 9)
    + sex[0].toUpperCase()
    + padRight(expiry, 7)
    + padRight('', 1);
  const field2 = padRight(field2Base, 30);

  const p1 = field1.slice(0, 18);
  const p1Check = computeCheck(p1, WEIGHTS_P1, CHARS_P1);

  const p2 = field2.slice(0, 18);
  const p2Check = computeCheck(p2, WEIGHTS_P2, CHARS_P2);

  return {
    line1: `${field1}${p1Check}`,
    line2: `${field2}${p2Check}`,
  };
}

// ── Date helpers ──────────────────────────────────────────────

function formatDateForMrz(dateStr: string): string {
  // Accept YYYY-MM-DD or MM/DD/YYYY or just YYYYMMDD
  let clean = dateStr.replace(/[^0-9]/g, '');
  if (clean.length === 8) return clean;
  if (clean.length === 6) return clean.padEnd(8, '0');
  if (clean.length === 4) return clean.padEnd(6, '0');
  return clean.slice(0, 6).padEnd(6, '0');
}

// ── Public API ────────────────────────────────────────────────

interface MrzResult {
  lines: string[];
  type: 'passport' | 'license';
}

export function computeMrz(
  docType: 'passport' | 'license',
  fullName: string,
  dob: string,
  nationality: string = 'USA',
  idNumber: string,
  sex: string = 'M'
): MrzResult {
  const cleanName = toUpperNoSpaces(fullName);
  const cleanDob = formatDateForMrz(dob);

  if (docType === 'passport') {
    const mrz = computePassportMrz(cleanName, nationality, idNumber, cleanDob, sex);
    if (!mrz) throw new Error('Failed to compute passport MRZ');
    return { lines: [mrz.line1, mrz.line2, mrz.line3], type: 'passport' };
  }

  const mrz = computeLicenseMrz(cleanName, idNumber, cleanDob, sex);
  if (!mrz) throw new Error('Failed to compute license MRZ');
  return { lines: [mrz.line1, mrz.line2], type: 'license' };
}

export function computeMrzLine(fullName: string, docType: 'passport' | 'license'): string {
  const mrz = computeMrz(docType, fullName, '20000101', 'USA', '000000000', 'M');
  return mrz.lines.join('\n');
}
