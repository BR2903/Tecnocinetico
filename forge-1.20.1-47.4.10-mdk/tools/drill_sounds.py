"""Sonidos del Taladro de Combustión: grabaciones reales (CC0, "68 workshop sounds" de bart; ver
CREDITOS_SONIDOS.md) + un motor de dos tiempos sintetizado con impulsos de combustión, nunca tonos puros.

python tools/drill_sounds.py src/main/resources/assets/tecnocinetico/sounds
Requiere numpy, scipy y soundfile. Semillas fijas: el resultado es reproducible.

Salida (sounds/drill/):
- idle.ogg      bucle: motor al ralentí (petardeo grave, ~30 explosiones/s), traqueteo de chapa.
- drilling.ogg  bucle: motor acelerado + giro real del taladro (más grave, más pesado) + roca triturándose.
- rev.ogg       arranque al empezar a taladrar: el motor sube de vueltas y la broca arranca.
- bite1-3.ogg   mordisco de la broca al romper un bloque (crujido de roca + roce metálico).
- refuel.ogg    abrir el tapón y gasolina entrando a borbotones.
- refuel_end.ogg tapón cerrado.
- sputter.ogg   el motor se ahoga al acabarse la gasolina.
Los bucles se cierran con un fundido cruzado para que no haya chasquido al repetirse.
"""
import sys
from pathlib import Path

import numpy as np
import soundfile as sf
from scipy import signal

SR = 44100
SRC = Path(__file__).parent / "sound_sources"
OUT = Path(sys.argv[1] if len(sys.argv) > 1 else ".") / "drill"


# ------------------------------------------------------------------ utilidades
def n_of(d):
    return int(round(d * SR))


def load(name):
    x, sr = sf.read(SRC / name)
    if x.ndim > 1:
        x = x.mean(axis=1)
    if sr != SR:
        x = signal.resample_poly(x, SR, sr)
    return x / (np.max(np.abs(x)) + 1e-9)


def filt(x, kind, f, order=4):
    return signal.sosfilt(signal.butter(order, f, kind, fs=SR, output="sos"), x)


def resonate(x, modes):
    """Suma de resonadores (frecuencia, Q, ganancia): cavidad del escape, carcasa..."""
    y = np.zeros_like(x)
    for f, q, g in modes:
        b, a = signal.iirpeak(f, q, fs=SR)
        y += g * signal.lfilter(b, a, x)
    return y


def speed(x, factor):
    """Cambia velocidad y tono a la vez (como una máquina más grande si factor < 1)."""
    up, down = 100, int(round(100 * factor))
    return signal.resample_poly(x, up, down)


def steady(x, length, hop=0.05):
    """El tramo de `length` s más estable (menor variación de volumen) de una grabación."""
    n, h = n_of(length), n_of(hop)
    best, best_s = None, 1e9
    for s in range(int(len(x) * 0.15), len(x) - n - int(len(x) * 0.1), h):
        w = x[s:s + n]
        env = np.sqrt(np.convolve(w ** 2, np.ones(n_of(0.05)) / n_of(0.05), "valid"))
        score = np.std(env) / (np.mean(env) + 1e-9) - 0.3 * np.mean(env)
        if score < best_s:
            best, best_s = s, score
    return x[best:best + n]


def loop(x, xfade=0.25):
    """Bucle sin costura: el final se funde con el principio."""
    xf = n_of(xfade)
    body = x[:-xf].copy()
    t = np.linspace(0, 1, xf)
    body[:xf] = body[:xf] * np.sqrt(t) + x[-xf:] * np.sqrt(1 - t)
    return body


def engine(dur, f0, f1, seed, misfire=0.0, jitter=0.03, tau=0.004):
    """Motor de dos tiempos: tren de explosiones (ruido corto) a una frecuencia que va de f0 a f1 Hz, pasado por
    el escape (resonancias graves) y la carcasa (traqueteo). misfire = probabilidad de que una explosión falle."""
    rng = np.random.default_rng(seed)
    n = n_of(dur)
    f = np.geomspace(f0, f1, n) if f0 != f1 else np.full(n, f0)
    phase = np.cumsum(f / SR)
    fires = np.nonzero(np.diff(np.floor(phase)) > 0)[0]
    burst_len = n_of(tau * 6)
    k = np.arange(burst_len)
    x = np.zeros(n + burst_len)
    for i in fires:
        if rng.random() < misfire:
            continue
        i += int(rng.normal(0, jitter / max(f[i], 1)) * SR)
        if i < 0 or i >= n:
            continue
        b = rng.normal(0, 1, burst_len) * np.exp(-k / n_of(tau)) * (0.75 + 0.5 * rng.random())
        x[i:i + burst_len] += b
    x = x[:n]
    exhaust = resonate(filt(x, "lowpass", 1400), [(95, 4, 1.0), (210, 6, 0.8), (420, 9, 0.5), (760, 12, 0.3)])
    rattle = resonate(filt(x, "highpass", 1500), [(2300, 30, 0.25), (3700, 40, 0.2), (5200, 35, 0.12)])
    return exhaust + 0.6 * rattle


def norm(x, peak=1.0):
    return x / (np.max(np.abs(x)) + 1e-9) * peak


def save(name, x, peak=0.8, fade=True):
    OUT.mkdir(parents=True, exist_ok=True)
    x = norm(x, peak)
    if fade:
        f = min(len(x), n_of(0.004))
        x[:f] *= np.linspace(0, 1, f)
        t = min(len(x), n_of(0.04))
        x[-t:] *= np.linspace(1, 0, t)
    sf.write(OUT / f"{name}.ogg", x.astype(np.float32), SR, format="OGG", subtype="VORBIS")
    print(f"drill/{name}.ogg  {len(x) / SR:.2f}s")


def grind(dur, seed, lo=180, hi=2600):
    """Roca triturándose: ruido de banda con golpeteo irregular de las esquirlas."""
    rng = np.random.default_rng(seed)
    n = n_of(dur)
    base = filt(filt(rng.normal(0, 1, n), "highpass", lo), "lowpass", hi)
    am = filt(rng.normal(0, 1, n), "lowpass", 18)
    am = 0.55 + 0.45 * am / (np.max(np.abs(am)) + 1e-9)
    chips = np.zeros(n)
    for i in rng.integers(0, n, int(dur * 70)):
        L = n_of(0.006)
        if i + L < n:
            chips[i:i + L] += rng.normal(0, 1, L) * np.exp(-np.arange(L) / n_of(0.0012)) * rng.uniform(0.3, 1.0)
    return base * am + 0.8 * filt(chips, "highpass", 900)


# ------------------------------------------------------------------ sonidos
def generate():
    drill_long = load("workshop_drill_long.wav")
    drill_start = load("workshop_drill_short_1.wav")
    clink, clink2 = load("workshop_clink.wav"), load("workshop_clink2.wav")
    scrape = load("workshop_scrape3.wav")

    # ralentí: petardeo grave y un poco irregular
    idle = engine(2.25, 31, 31, seed=11, misfire=0.02, jitter=0.06, tau=0.005)
    idle = filt(idle, "lowpass", 3200)
    save("idle", loop(idle), peak=0.75, fade=False)

    # taladrando: motor alto + giro real (más grave = taladro grande) + roca
    spin = steady(drill_long, 2.0)
    spin = speed(spin, 0.82)[:n_of(2.25)]
    if len(spin) < n_of(2.25):
        spin = np.concatenate([spin, spin])[:n_of(2.25)]
    hi_engine = engine(2.25, 118, 118, seed=21, jitter=0.02, tau=0.0025)
    rock = grind(2.25, seed=31)
    rock = filt(rock, "lowpass", 2200)                                  # roca: crujido grave, no siseo
    mixd = norm(hi_engine) * 1.0 + norm(spin) * 0.6 + norm(rock) * 0.4
    mixd = filt(filt(mixd, "highpass", 45), "lowpass", 5200, order=2)  # cuerpo grave de motor de gasolina
    save("drilling", loop(mixd), peak=0.8, fade=False)

    # arranque: el motor sube de vueltas y la broca arranca
    rev_e = engine(0.7, 31, 118, seed=41, tau=0.004)
    st = drill_start[: n_of(0.7)]
    st = speed(st, 0.82)[: n_of(0.7)]
    st = np.pad(st, (0, max(0, n_of(0.7) - len(st))))
    rise = np.linspace(0.3, 1.0, n_of(0.7))
    save("rev", norm(rev_e) * 0.9 + norm(st) * 0.6 * rise, peak=0.8)

    # mordisco al romper un bloque
    for i in range(3):
        rng = np.random.default_rng(50 + i)
        d = 0.32
        crunch = grind(d, seed=60 + i, lo=250, hi=5000) * np.exp(-np.arange(n_of(d)) / n_of(0.07))
        s0 = rng.integers(0, max(1, len(scrape) - n_of(d)))
        sc = scrape[s0:s0 + n_of(d)] * np.exp(-np.arange(n_of(d)) / n_of(0.09))
        crack = filt(rng.normal(0, 1, n_of(d)), "bandpass", [500, 1800]) * np.exp(-np.arange(n_of(d)) / n_of(0.012))
        thump = resonate(filt(rng.normal(0, 1, n_of(d)), "lowpass", 400) * np.exp(-np.arange(n_of(d)) / n_of(0.03)),
                         [(90 + 15 * i, 5, 1.0), (180, 6, 0.6)])            # golpe grave de la roca que cede
        bite = norm(crunch) * 0.6 + 0.35 * norm(sc) + 0.7 * norm(crack) + 1.0 * norm(thump)
        save(f"bite{i + 1}", filt(bite, "lowpass", 3800), peak=0.75)

    # repostar: tapón y gasolina a borbotones (burbujas = resonancias cortas que suben de tono)
    rng = np.random.default_rng(70)
    d = 1.4
    glug = np.zeros(n_of(d))
    t = 0.12
    while t < d - 0.15:
        f = rng.uniform(180, 420)
        L = n_of(rng.uniform(0.05, 0.11))
        k = np.arange(L) / SR
        chirp = np.sin(2 * np.pi * (f * k + f * 2.2 * k ** 2 / (2 * k[-1]))) * np.exp(-k / 0.03)
        i = n_of(t)
        glug[i:i + L] += chirp[: len(glug) - i] * rng.uniform(0.5, 1.0)
        t += rng.uniform(0.05, 0.13)
    pour = filt(rng.normal(0, 1, n_of(d)), "bandpass", [700, 3500]) * 0.25
    pour *= np.clip(np.linspace(0, 3, n_of(d)), 0, 1) * np.clip(np.linspace(4, 0, n_of(d)), 0, 1)
    cap = np.pad(clink[: n_of(0.25)], (0, n_of(d) - n_of(0.25)))
    save("refuel", norm(glug) * 0.8 + pour + 0.6 * norm(cap), peak=0.75)
    c2 = clink2[np.argmax(np.abs(clink2) > 0.1):]                      # sin el silencio inicial
    c2 = np.pad(c2[: n_of(0.45)], (0, max(0, n_of(0.45) - len(c2))))
    save("refuel_end", c2 + 0.3 * norm(engine(0.45, 31, 40, seed=80)), peak=0.7)

    # sin gasolina: el motor se ahoga
    sp = engine(1.1, 70, 9, seed=90, misfire=0.35, jitter=0.15, tau=0.006)
    sp *= np.linspace(1.0, 0.2, len(sp))
    save("sputter", sp, peak=0.75)


if __name__ == "__main__":
    generate()
