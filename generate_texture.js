/**
 * Генератор текстуры Песчаного Эндермена (Node.js, без зависимостей)
 * Запуск: node generate_texture.js
 *
 * Создаёт PNG 64x32 вручную — берёт стандартный layout текстуры эндермена,
 * меняет тело на песчано-коричневые цвета, а глаза — на жёлтый.
 */

const fs = require('fs');
const path = require('path');
const zlib = require('zlib');

const W = 64, H = 32;
const pixels = new Uint8Array(W * H * 4);

function setPixel(x, y, r, g, b, a = 255) {
    if (x < 0 || x >= W || y < 0 || y >= H) return;
    const i = (y * W + x) * 4;
    pixels[i] = r; pixels[i+1] = g; pixels[i+2] = b; pixels[i+3] = a;
}

function fillRect(x, y, w, h, r, g, b, a = 255) {
    for (let cy = y; cy < y + h; cy++)
        for (let cx = x; cx < x + w; cx++)
            setPixel(cx, cy, r, g, b, a);
}

const DARK  = [32, 22, 8];
const MID   = [48, 34, 12];
const LIGHT = [62, 46, 18];
const EYE   = [255, 210, 0];
const TRANS = [0, 0, 0, 0];

// --- HEAD (top-left 8x8) ---
fillRect(0, 0, 8, 8, ...MID);
// outline
for (let i = 0; i < 8; i++) { setPixel(i, 0, ...DARK); setPixel(i, 7, ...DARK); setPixel(0, i, ...DARK); setPixel(7, i, ...DARK); }
// eyes (yellow)
fillRect(1, 2, 2, 2, ...EYE);
fillRect(5, 2, 2, 2, ...EYE);

// --- HEAD OVERLAY (8-16, 0-8) ---
fillRect(8, 0, 8, 8, ...DARK, 120);

// --- BODY (16-24, 16-28) ---
fillRect(16, 16, 8, 12, ...MID);
for (let i = 0; i < 8; i++) { setPixel(16+i, 16, ...DARK); setPixel(16+i, 27, ...DARK); }
for (let i = 0; i < 12; i++) { setPixel(16, 16+i, ...DARK); setPixel(23, 16+i, ...DARK); }

// --- BODY SIDES (24-32 and 28-32, 16-20) ---
fillRect(24, 16, 4, 4, ...LIGHT);
fillRect(28, 16, 4, 4, ...LIGHT);

// --- BODY BACK (32-40, 16-28) ---
fillRect(32, 16, 8, 12, ...MID);

// --- RIGHT ARM (40-44, 16-28) ---
fillRect(40, 16, 4, 12, ...DARK);
fillRect(44, 16, 4, 12, ...MID);
fillRect(48, 16, 4, 12, ...DARK);
fillRect(52, 16, 4, 12, ...MID);

// --- LEFT ARM (32-36, 0-12 or similar layout) ---
fillRect(56, 16, 4, 12, ...DARK);
fillRect(60, 16, 4, 12, ...MID);

// --- RIGHT LEG (0-4, 16-28) ---
fillRect(0, 16, 4, 12, ...MID);
for (let i = 0; i < 4; i++) { setPixel(i, 16, ...DARK); setPixel(i, 27, ...DARK); }
for (let i = 0; i < 12; i++) { setPixel(0, 16+i, ...DARK); setPixel(3, 16+i, ...DARK); }
fillRect(4, 16, 4, 12, ...DARK);
fillRect(8, 16, 4, 12, ...MID);
fillRect(12, 16, 4, 12, ...DARK);

// --- LEFT LEG (16-20 area) ---
fillRect(16, 0, 4, 12, ...MID);
fillRect(20, 0, 4, 12, ...DARK);
fillRect(24, 0, 4, 12, ...MID);
fillRect(28, 0, 4, 12, ...DARK);

// --- PNG encode (manual, no deps) ---
function adler32(data) {
    let a = 1, b = 0;
    for (let i = 0; i < data.length; i++) { a = (a + data[i]) % 65521; b = (b + a) % 65521; }
    return (b << 16) | a;
}

function u32be(n) {
    return [(n >>> 24) & 0xFF, (n >>> 16) & 0xFF, (n >>> 8) & 0xFF, n & 0xFF];
}

function chunk(type, data) {
    const typeBytes = [...type].map(c => c.charCodeAt(0));
    const combined = typeBytes.concat(Array.from(data));
    // CRC32 (simplified — use zlib)
    const crcBuf = Buffer.from(combined);
    const crc = crc32(crcBuf);
    return Buffer.from([
        ...u32be(data.length),
        ...typeBytes,
        ...Array.from(data),
        ...u32be(crc)
    ]);
}

function crc32(buf) {
    const table = makeCrcTable();
    let crc = 0xFFFFFFFF;
    for (let i = 0; i < buf.length; i++) crc = (crc >>> 8) ^ table[(crc ^ buf[i]) & 0xFF];
    return (crc ^ 0xFFFFFFFF) >>> 0;
}

let _crcTable = null;
function makeCrcTable() {
    if (_crcTable) return _crcTable;
    _crcTable = new Uint32Array(256);
    for (let n = 0; n < 256; n++) {
        let c = n;
        for (let k = 0; k < 8; k++) c = (c & 1) ? (0xEDB88320 ^ (c >>> 1)) : (c >>> 1);
        _crcTable[n] = c;
    }
    return _crcTable;
}

// Build raw image data (filter byte + row)
const rawRows = [];
for (let y = 0; y < H; y++) {
    rawRows.push(0); // filter type None
    for (let x = 0; x < W; x++) {
        const i = (y * W + x) * 4;
        rawRows.push(pixels[i], pixels[i+1], pixels[i+2], pixels[i+3]);
    }
}
const rawData = Buffer.from(rawRows);
const compressed = zlib.deflateSync(rawData, { level: 9 });

const PNG_SIGNATURE = Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]);

const IHDR_data = Buffer.from([
    ...u32be(W), ...u32be(H),
    8, 6, 0, 0, 0
]);

const png = Buffer.concat([
    PNG_SIGNATURE,
    chunk('IHDR', IHDR_data),
    chunk('IDAT', compressed),
    chunk('IEND', Buffer.alloc(0))
]);

const outDir = path.join(__dirname, 'src', 'main', 'resources', 'assets', 'sandenderman', 'textures', 'entity');
fs.mkdirSync(outDir, { recursive: true });
const outPath = path.join(outDir, 'sand_enderman.png');
fs.writeFileSync(outPath, png);
console.log('Текстура создана:', outPath);
console.log('Теперь запустите: gradlew build');
