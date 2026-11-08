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


# =================================================================== armas largas (mismo paquete "Gunshot Sounds")
GUNS = Path(sys.argv[1]) / "guns" if len(sys.argv) > 1 else None


def save_gun(name, y, peak=0.95):
    GUNS.mkdir(parents=True, exist_ok=True)
    y = y / (np.max(np.abs(y)) + 1e-9) * peak
    sf.write(GUNS / f"{name}.ogg", y.astype(np.float32), SR, format="OGG", subtype="VORBIS")
    print(f"guns/{name}.ogg  {len(y) / SR:.2f}s")


def shot_at(x, t, length, fade=0.25):
    """Un disparo empezando en su pico más fuerte cerca de t (±0,15 s)."""
    a, b = max(0, int((t - 0.05) * SR)), int((t + 0.15) * SR)
    peak = (a + int(np.argmax(np.abs(x[a:b])))) / SR
    y = cut(x, peak - 0.003, peak + length, pre=0.003, fade_out=fade, attack_db=-40)
    return y / (np.max(np.abs(y)) + 1e-9)


def pitch(y, k):
    """Cambia el tono (y la duración) como una cinta: k < 1 = más grave y más largo."""
    up, down = 1000, int(round(1000 * k))
    return signal.resample_poly(y, up, down)


def thump(y, cutoff=180, gain=1.2):
    """Refuerza el golpe grave (armas más pesadas): graves del propio disparo, saturados."""
    sos = signal.butter(4, cutoff, btype="low", fs=SR, output="sos")
    low_ = signal.sosfilt(sos, y)
    return y + np.tanh(low_ * 4) * gain * np.exp(-np.arange(len(y)) / (SR * 0.12))


def short(y, length, fade=0.06):
    """Versión corta para armas automáticas (la cola larga se solaparía a 10-20 disparos/s)."""
    n = min(len(y), int(length * SR))
    z = y[:n].copy()
    f = min(n, int(fade * SR))
    z[-f:] *= np.linspace(1, 0, f) ** 2
    return z


def import_guns():
    cz = load("cz52_gunshots.wav")
    mosin = load("mosin_gunshots.wav")                       # rifle Mosin-Nagant de cerrojo
    sks = load("sks_gunshots.wav")                           # fusil semiautomático SKS
    shotty = load("shotgun_gunshot.wav")                     # escopeta
    cz_t = [0.004, 2.82, 4.05, 5.03]
    mosin_t = [0.44, 3.56, 5.98, 8.75, 11.69]
    sks_t = [0.34, 2.29, 6.00, 7.70, 9.72, 11.45]

    # Revólver: la pistola más grave y con más cuerpo (cartucho más potente)
    for i, t in enumerate(cz_t[:2], start=1):
        y = thump(pitch(shot_at(cz, t, 1.2), 0.84), 220, 0.8)
        save_gun(f"revolver{i}", y + kinetic_crackle(len(y), 70 + i))
    # Escopetas
    sg = shot_at(shotty, 0.03, 0.66, fade=0.2)
    save_gun("shotgun1", thump(sg, 160, 0.6))
    save_gun("shotgun2", thump(pitch(sg, 0.93), 160, 0.7))
    # Rifle de cerrojo y su cerrojo (abrir + cerrar, grabado entre disparos)
    for i, t in enumerate(mosin_t[1:4], start=1):
        save_gun(f"rifle{i}", shot_at(mosin, t, 1.25))
    save_gun("bolt", cut(mosin, 7.45, 8.12, fade_out=0.05, attack_db=-30), peak=0.8)
    # Francotirador: el rifle más grave, con más golpe y la cola larga del campo de tiro
    save_gun("sniper1", thump(pitch(shot_at(mosin, mosin_t[0], 1.9, fade=0.5), 0.9), 150, 1.0))
    save_gun("sniper2", thump(pitch(shot_at(mosin, mosin_t[4], 1.7, fade=0.5), 0.88), 150, 1.0))
    # Semiautomático
    for i, t in enumerate(sks_t[:3], start=1):
        save_gun(f"semi{i}", shot_at(sks, t, 0.85))
    # Automáticas: subfusil (pistola corta y aguda), fusil de asalto / ametralladora (SKS corto), minigun (chasquido)
    for i, t in enumerate(cz_t[:3], start=1):
        save_gun(f"smg{i}", short(pitch(shot_at(cz, t, 0.5), 1.08), 0.32))
    for i, t in enumerate(sks_t[3:6], start=1):
        save_gun(f"auto{i}", short(shot_at(sks, t, 0.6), 0.42, fade=0.12))
    for i, t in enumerate(sks_t[1:3], start=1):
        save_gun(f"minigun{i}", short(pitch(shot_at(sks, t, 0.3), 1.18), 0.16, fade=0.06), peak=0.85)


if __name__ == "__main__":
    main()
    import_guns()
