"""Convierte las grabaciones reales de tools/sound_sources/ en los sonidos de la Pistola Cinética.

python tools/import_sounds.py src/main/resources/assets/tecnocinetico/sounds
Origen y licencia de cada archivo: ver CREDITOS_SONIDOS.md (raíz del mod). Requiere numpy, scipy y soundfile.

Proceso: mezcla a mono -> 44,1 kHz -> recorte en el golpe -> (opcional) toque cinético sutil -> fundido final
-> normalización. Los sonidos que no salen de aquí (impacto, acierto) siguen siendo de tools/sound_gen.py.
"""
import sys
from pathlib import Path

import numpy as np
import soundfile as sf
from scipy import signal

SR = 44100
SRC = Path(__file__).parent / "sound_sources"
OUT = Path(sys.argv[1]) / "pistol"


def load(name):
    x, sr = sf.read(SRC / name)
    if x.ndim > 1:
        x = x.mean(axis=1)                                  # mono (necesario para el sonido posicional)
    if sr != SR:
        x = signal.resample_poly(x, SR, sr)
    return x


def cut(x, start, end, pre=0.004, fade_out=0.08, attack_db=-14):
    """Recorta [start, end] pero empieza justo antes del GOLPE real (primer pico a attack_db del máximo),
    para que el sonido no tenga retraso respecto a la acción."""
    s = max(0, int(start * SR))
    e = min(len(x), int(end * SR))
    win = x[s:e]
    hit = np.nonzero(np.abs(win) > np.max(np.abs(win)) * 10 ** (attack_db / 20))[0]
    if len(hit):
        s = max(0, s + hit[0] - int(pre * SR))
    y = x[s:e].copy()
    f = min(len(y), int(fade_out * SR))
    y[-f:] *= np.linspace(1, 0, f) ** 2                     # fundido final suave
    return y


def kinetic_crackle(n, seed, level=0.06):
    """Chisporroteo eléctrico muy sutil en los primeros ~60 ms: el sello del arma cinética."""
    rng = np.random.default_rng(seed)
    out = np.zeros(n)
    sos = signal.butter(4, [3000, 12000], btype="band", fs=SR, output="sos")
    t = 0
    while t < int(0.06 * SR) and t < n - 64:
        g = 48
        out[t:t + g] += signal.sosfilt(sos, rng.standard_normal(g)) * np.exp(-np.arange(g) / 12) * rng.uniform(0.3, 1)
        t += int(rng.exponential(SR / 250))
    return out * level


def save(name, y, peak=0.95):
    OUT.mkdir(parents=True, exist_ok=True)
    y = y / (np.max(np.abs(y)) + 1e-9) * peak
    sf.write(OUT / f"{name}.ogg", y.astype(np.float32), SR, format="OGG", subtype="VORBIS")
    print(f"pistol/{name}.ogg  {len(y) / SR:.2f}s")


def main():
    # --- Disparos: 4 disparos reales de una pistola CZ-52 en un campo de tiro (con su eco natural) ---
    cz = load("cz52_gunshots.wav")
    shots = [(0.002, 1.25), (2.819, 4.0), (4.048, 5.0), (5.028, 6.6)]
    for i, (a, b) in enumerate(shots, start=1):
        # El disparo empieza en el pico más fuerte del trozo (puede haber ruidos más débiles antes)
        win = cz[int(a * SR):int(b * SR)]
        peak_t = a + np.argmax(np.abs(win)) / SR
        y = cut(cz, peak_t - 0.003, min(b, peak_t + 1.2), pre=0.003, fade_out=0.25, attack_db=-40)
        y = y / (np.max(np.abs(y)) + 1e-9)
        y = y + kinetic_crackle(len(y), 50 + i)
        save(f"fire{i}", y)

    # --- Recarga: una grabación con tres acciones (cargador fuera, cargador dentro, corredera) ---
    rl = load("handgun_reload.wav")
    save("mag_out", cut(rl, 0.08, 0.55), peak=0.85)
    save("mag_in", cut(rl, 0.58, 1.0), peak=0.8)
    save("slide", cut(rl, 1.01, 1.59), peak=0.7)

    # --- Clic en vacío: el clic metálico corto de singlebullet1 ---
    sb = load("singlebullet1.wav")
    save("dry_fire", cut(sb, 0.12, 0.3, fade_out=0.05), peak=0.8)


if __name__ == "__main__":
    main()
