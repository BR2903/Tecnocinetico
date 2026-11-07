"""Sintetiza los sonidos del Cuerpo de Enjambre (OGG Vorbis mono, 44,1 kHz).

python tools/sound_gen.py src/main/resources/assets/tecnocinetico/sounds
Requiere numpy, scipy y soundfile. Semillas fijas: el resultado es reproducible.

Concepto (lore): criatura biomecánica = carne corrompida + estructura de Inercita + núcleo de Cinetrita.
Todos los sonidos mezclan una parte orgánica (gruñido, chillido) con una mecánica (metal, servos, energía).
"""
import sys
from pathlib import Path

import numpy as np
import soundfile as sf
from scipy import signal

SR = 44100
OUT = Path(sys.argv[1]) / "swarm_body"


# ---------------------------------------------------------------- utilidades
def t(dur):
    return np.arange(int(SR * dur)) / SR


def noise(dur, seed):
    return np.random.default_rng(seed).standard_normal(int(SR * dur))


def band(x, lo, hi, order=4):
    sos = signal.butter(order, [lo, hi], btype="band", fs=SR, output="sos")
    return signal.sosfilt(sos, x)


def low(x, f, order=4):
    return signal.sosfilt(signal.butter(order, f, btype="low", fs=SR, output="sos"), x)


def high(x, f, order=4):
    return signal.sosfilt(signal.butter(order, f, btype="high", fs=SR, output="sos"), x)


def env(n, attack, release, curve=3.0):
    """Envolvente: subida lineal y caída exponencial."""
    a = max(1, int(SR * attack))
    e = np.ones(n)
    e[:a] = np.linspace(0, 1, a)
    r = max(1, int(SR * release))
    if r < n:
        e[n - r:] *= np.exp(-curve * np.linspace(0, 1, r))
    return e


def decay(n, tau):
    return np.exp(-np.arange(n) / (SR * tau))


def sweep(f0, f1, dur, exp=True):
    """Fase de un barrido de frecuencia (para senos con tono variable)."""
    tt = t(dur)
    f = f0 * (f1 / f0) ** (tt / dur) if exp else f0 + (f1 - f0) * tt / dur
    return 2 * np.pi * np.cumsum(f) / SR


def pad(x, n):
    return np.pad(x, (0, max(0, n - len(x))))[:n]


def mix(total, *parts):
    """parts = (señal, inicio_s, ganancia)."""
    n = int(SR * total)
    out = np.zeros(n)
    for x, start, gain in parts:
        s = int(SR * start)
        seg = x[: max(0, n - s)]
        out[s:s + len(seg)] += gain * seg
    return out


def finish(x, peak=0.8):
    x = np.tanh(x * 1.2)                       # saturación suave (carácter "sucio")
    fade = min(len(x), int(SR * 0.01))
    x[:fade] *= np.linspace(0, 1, fade)
    x[-fade:] *= np.linspace(1, 0, fade)
    return x / (np.max(np.abs(x)) + 1e-9) * peak


def save(name, x, folder="swarm_body"):
    out = OUT.parent / folder
    out.mkdir(parents=True, exist_ok=True)
    sf.write(out / f"{name}.ogg", finish(x).astype(np.float32), SR, format="OGG", subtype="VORBIS")
    print(f"{folder}/{name}.ogg  {len(x) / SR:.2f}s")


# ---------------------------------------------------------------- componentes
def growl(dur, base, seed):
    """Gruñido orgánico: pulso glotal grave con formantes y traqueteo."""
    tt = t(dur)
    f = base * (1 + 0.08 * np.sin(2 * np.pi * 1.7 * tt) + 0.03 * np.random.default_rng(seed).standard_normal(len(tt)).cumsum() / 400)
    saw = signal.sawtooth(2 * np.pi * np.cumsum(f) / SR)
    voice = band(saw, 350, 700) * 0.9 + band(saw, 1100, 1700) * 0.5      # formantes "oscuros"
    rattle = 0.55 + 0.45 * np.sign(np.sin(2 * np.pi * 21 * tt))         # traqueteo de la garganta
    breath = band(noise(dur, seed + 1), 200, 900) * 0.35
    return (voice * rattle + breath) * env(len(tt), 0.15, dur * 0.5)


def clicks(dur, rate, seed, pitch=3200):
    """Chasquidos de mandíbulas/articulaciones metálicas."""
    rng = np.random.default_rng(seed)
    out = np.zeros(int(SR * dur))
    tpos = 0.05
    while tpos < dur - 0.05:
        n = int(SR * 0.025)
        c = band(rng.standard_normal(n), pitch * 0.7, pitch * 1.3) * decay(n, 0.004)
        s = int(SR * tpos)
        out[s:s + n] += c[: len(out) - s] * rng.uniform(0.5, 1.0)
        tpos += rng.uniform(0.6, 1.4) / rate
    return out


def core_hum(dur, f=110):
    """Zumbido del núcleo de cinetrita."""
    tt = t(dur)
    hum = np.sin(2 * np.pi * f * tt) + 0.4 * np.sin(2 * np.pi * f * 2.01 * tt) + 0.2 * np.sin(2 * np.pi * f * 3.98 * tt)
    return hum * (0.7 + 0.3 * np.sin(2 * np.pi * 6 * tt)) * env(len(tt), 0.2, 0.3)


def servo(dur, f0, f1):
    tt = t(dur)
    ph = sweep(f0, f1, dur)
    s = np.sin(ph) + 0.3 * np.sin(2 * ph) + 0.15 * np.sin(3 * ph)
    return s * env(len(tt), 0.05, dur * 0.6) * (0.8 + 0.2 * np.sin(2 * np.pi * 37 * tt))


def metal_hit(dur, base, seed):
    """Golpe metálico (placas de Inercita): parciales inarmónicos con caída rápida."""
    tt = t(dur)
    rng = np.random.default_rng(seed)
    out = np.zeros(len(tt))
    for ratio, amp, tau in [(1.0, 1.0, 0.16), (2.76, 0.7, 0.09), (5.40, 0.45, 0.06), (8.93, 0.3, 0.035), (13.3, 0.2, 0.02)]:
        f = base * ratio * rng.uniform(0.98, 1.02)
        out += amp * np.sin(2 * np.pi * f * tt + rng.uniform(0, 6.28)) * decay(len(tt), tau)
    out += band(noise(dur, seed + 7), 1500, 6000) * decay(len(tt), 0.012) * 0.8     # impacto
    return out


def screech(dur, f0, f1, seed):
    """Chillido (FM áspera) + banda de ruido aguda."""
    tt = t(dur)
    mod = 3.5 * np.sin(2 * np.pi * 190 * tt)
    car = np.sin(sweep(f0, f1, dur) + mod)
    hiss = band(noise(dur, seed), 2500, 7000) * 0.4
    return (car + hiss) * env(len(tt), 0.02, dur * 0.7)


# ---------------------------------------------------------------- sonidos
def ambient(i):
    seed = 100 + i * 10
    dur = 1.6 + 0.2 * i
    base = [62, 55, 70][i]
    return mix(dur,
               (growl(dur - 0.1, base, seed), 0.0, 1.0),
               (clicks(dur, 6 + 2 * i, seed + 3), 0.0, 0.55),
               (core_hum(dur), 0.0, 0.25),
               (servo(0.5, 420 + 60 * i, 780 + 40 * i), 0.35 + 0.2 * i, 0.22))


def hurt(i):
    seed = 200 + i * 10
    dur = 0.55
    return mix(dur,
               (metal_hit(0.5, [330, 410][i], seed), 0.0, 0.9),
               (screech(0.35, [1500, 1750][i], [2300, 2600][i], seed + 1), 0.03, 0.6),
               (low(noise(0.2, seed + 2), 500) * decay(int(SR * 0.2), 0.05), 0.0, 0.8))   # crujido de carne


def death():
    seed = 300
    dur = 2.2
    tt = t(1.6)
    power_down = np.tanh(3 * np.sin(sweep(900, 45, 1.6))) * decay(len(tt), 0.7)        # el núcleo se apaga
    rattle = clicks(1.4, 14, seed + 5, pitch=2400)
    return mix(dur,
               (screech(0.9, 2000, 600, seed), 0.0, 0.8),
               (growl(1.0, 48, seed + 2), 0.1, 0.6),
               (power_down, 0.25, 0.7),
               (rattle, 0.6, 0.6),
               (metal_hit(0.6, 260, seed + 9), 1.45, 0.7))                               # cae al suelo


def step(i):
    seed = 400 + i
    dur = 0.18
    n = int(SR * dur)
    thud = low(noise(dur, seed), 260) * decay(n, 0.03)
    tick = band(noise(dur, seed + 1), 2500 + 400 * i, 4500 + 400 * i) * decay(n, 0.006)
    return mix(dur, (thud, 0.0, 1.0), (tick, 0.0, 0.6))


def leap():
    seed = 500
    dur = 0.75
    n = int(SR * 0.6)
    hiss = band(noise(0.6, seed), 1200, 4500) * env(n, 0.01, 0.45)                       # hidráulica (sin agudos chillones)
    charge = servo(0.45, 300, 1400)                                                       # carga cinética
    return mix(dur, (hiss, 0.0, 0.45), (charge, 0.0, 0.7), (metal_hit(0.3, 520, seed + 1), 0.0, 0.5))


# ---------------------------------------------------------------- Pistola Cinética
# Concepto: arma real (chasquido, golpe, mecanismo) + firma "cinética" (zumbido eléctrico que cae de tono)
# y la cinetrita como cristal (tintineo al romperse el fragmento en el impacto).

def crack(dur, seed, lo=2000, hi=10000, tau=0.008):
    n = int(SR * dur)
    return band(noise(dur, seed), lo, hi) * decay(n, tau)


def thump(dur, f0, f1, tau):
    n = int(SR * dur)
    return np.sin(sweep(f0, f1, dur)) * decay(n, tau)


def click(seed, pitch=4000, tau=0.003, dur=0.03):
    n = int(SR * dur)
    return band(noise(dur, seed), pitch * 0.7, min(pitch * 1.4, 20000)) * decay(n, tau)


def scrape(dur, seed, lo=800, hi=2600):
    n = int(SR * dur)
    grain = 0.6 + 0.4 * np.sign(np.sin(2 * np.pi * 55 * t(dur)))          # textura rasposa
    return band(noise(dur, seed), lo, hi) * grain * env(n, dur * 0.6, dur * 0.4)


def tinkle(dur, seed, count=14):
    """Cristal haciéndose añicos: muchos 'pings' agudos y cortos en momentos aleatorios."""
    rng = np.random.default_rng(seed)
    out = np.zeros(int(SR * dur))
    for _ in range(count):
        f = rng.uniform(3000, 7500)
        d = rng.uniform(0.03, 0.09)
        n = int(SR * d)
        ping = np.sin(2 * np.pi * f * t(d)) * decay(n, d / 4)
        s = int(SR * rng.uniform(0, dur * 0.7) ** 1.6 / (dur * 0.7) ** 0.6)
        out[s:s + n] += ping[: len(out) - s] * rng.uniform(0.3, 1.0)
    return out


def pistol_fire(i):
    seed = 600 + i * 7
    dur = 0.9
    tail_n = int(SR * 0.8)
    tail = band(noise(0.8, seed + 4), 250, 3000) * decay(tail_n, 0.22)                    # eco corto
    zap = np.sin(sweep(2400 + 150 * i, 280, 0.2) + 4 * np.sin(2 * np.pi * 610 * t(0.2)))         * decay(int(SR * 0.2), 0.06)                                                        # firma cinética
    return mix(dur,
               (crack(0.08, seed), 0.0, 1.0),
               (thump(0.25, 90 - 6 * i, 38, 0.06), 0.0, 0.9),
               (low(noise(0.2, seed + 1), 220) * decay(int(SR * 0.2), 0.05), 0.0, 0.7),
               (zap, 0.005, 0.5),
               (metal_hit(0.12, 950 + 40 * i, seed + 2), 0.035, 0.28),                         # corredera
               (tail, 0.02, 0.3))


def pistol_impact():
    seed = 700
    return mix(0.6,
               (crack(0.06, seed, 1500, 8000, 0.01), 0.0, 0.8),
               (thump(0.2, 140, 50, 0.05), 0.0, 0.6),
               (tinkle(0.5, seed + 1), 0.01, 0.55))


def pistol_hit():
    tt = t(0.12)
    ping = np.sin(2 * np.pi * 2200 * tt) + 0.4 * np.sin(2 * np.pi * 4400 * tt)
    return ping * decay(len(tt), 0.035)


def pistol_dry():
    return mix(0.15, (click(800, 4500), 0.0, 1.0), (click(801, 2800, 0.004), 0.05, 0.7))


def pistol_mag_out():
    seed = 810
    return mix(0.4,
               (click(seed, 3600), 0.0, 1.0),                                              # retén
               (scrape(0.18, seed + 1), 0.03, 0.6),                                         # sale el cargador
               (metal_hit(0.15, 700, seed + 2), 0.2, 0.25))


def pistol_mag_in():
    seed = 820
    return mix(0.35,
               (scrape(0.1, seed, 900, 3000), 0.0, 0.5),
               (metal_hit(0.2, 620, seed + 1), 0.1, 0.9),                                   # golpe de palma
               (low(noise(0.1, seed + 2), 300) * decay(int(SR * 0.1), 0.03), 0.1, 0.8),
               (click(seed + 3, 4200), 0.16, 0.7))                                          # encaja


def pistol_slide():
    seed = 830
    return mix(0.45,
               (click(seed, 3000, 0.004), 0.0, 0.7),
               (scrape(0.12, seed + 1, 1200, 3500), 0.0, 0.5),                               # tirón atrás
               (metal_hit(0.2, 820, seed + 2), 0.17, 1.0),                                   # cierra de golpe
               (low(noise(0.1, seed + 3), 280) * decay(int(SR * 0.1), 0.025), 0.17, 0.7))


def generate_pistol():
    for i in range(3):
        save(f"fire{i + 1}", pistol_fire(i), "pistol")
    save("impact", pistol_impact(), "pistol")
    save("hit", pistol_hit(), "pistol")
    save("dry_fire", pistol_dry(), "pistol")
    save("mag_out", pistol_mag_out(), "pistol")
    save("mag_in", pistol_mag_in(), "pistol")
    save("slide", pistol_slide(), "pistol")


if __name__ == "__main__":
    for i in range(3):
        save(f"ambient{i + 1}", ambient(i))
        save(f"step{i + 1}", step(i))
    for i in range(2):
        save(f"hurt{i + 1}", hurt(i))
    save("death", death())
    save("leap", leap())
    generate_pistol()
