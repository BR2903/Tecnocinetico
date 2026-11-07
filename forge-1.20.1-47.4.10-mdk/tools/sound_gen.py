"""Sintetiza los sonidos del mod (OGG Vorbis mono, 44,1 kHz) con técnicas "realistas".

python tools/sound_gen.py src/main/resources/assets/tecnocinetico/sounds
Requiere numpy, scipy y soundfile. Semillas fijas: el resultado es reproducible.

Criterio (pedido por el usuario): NADA de tonos puros ni barridos "arcade".
- Todo parte de RUIDO (impulsos, roces, aire) moldeado con filtros y RESONADORES (como golpear metal real).
- Las "voces" de la criatura pasan por un tubo metálico resonante (filtro peine) que cambia de tono:
  estilo máquinas orgánico-mecánicas (inspiración: Horizon Zero Dawn), sin copiar ningún sonido.
- Acústica: eco de exterior y reverberación sintética para que suene en un espacio.
"""
import sys
from pathlib import Path

import numpy as np
import soundfile as sf
from scipy import signal

SR = 44100
ROOT = Path(sys.argv[1]) if len(sys.argv) > 1 else Path(".")


# =================================================================== utilidades básicas
def n_of(dur):
    return int(SR * dur)


def rng_of(seed):
    return np.random.default_rng(seed)


def noise(dur, seed):
    return rng_of(seed).standard_normal(n_of(dur))


def _sos(kind, f, order=4):
    return signal.butter(order, f, btype=kind, fs=SR, output="sos")


def low(x, f, order=4):
    return signal.sosfilt(_sos("low", f, order), x)


def high(x, f, order=4):
    return signal.sosfilt(_sos("high", f, order), x)


def band(x, lo, hi, order=4):
    return signal.sosfilt(_sos("band", [lo, hi], order), x)


def decay(n, tau):
    return np.exp(-np.arange(n) / (SR * tau))


def env(n, attack, release):
    e = np.ones(n)
    a = max(1, n_of(attack))
    e[:a] = np.linspace(0, 1, a) ** 2
    r = max(1, n_of(release))
    if r < n:
        e[n - r:] *= np.linspace(1, 0, r) ** 2
    return e


def mix(total, *parts):
    """parts = (señal, inicio_s, ganancia)."""
    out = np.zeros(n_of(total))
    for x, start, gain in parts:
        s = n_of(start)
        seg = x[: max(0, len(out) - s)]
        out[s:s + len(seg)] += gain * seg
    return out


def resonate(x, modes):
    """Banco de resonadores: modes = [(frecuencia, Q, ganancia)]. Suena a material físico, no a tono."""
    out = np.zeros_like(x)
    for f, q, g in modes:
        if f < SR / 2 - 100:
            b, a = signal.iirpeak(f, q, fs=SR)
            out += g * signal.lfilter(b, a, x)
    return out


def metal_modes(base, seed, count=7, q=(25, 70)):
    """Modos inarmónicos de una pieza metálica (cada pieza 'suena' distinta)."""
    r = rng_of(seed)
    ratios = np.sort(np.concatenate([[1.0], r.uniform(1.4, 9.0, count - 1)]))
    return [(base * k, r.uniform(*q), r.uniform(0.4, 1.0) / (1 + 0.15 * i)) for i, k in enumerate(ratios)]


def impulse(dur, seed, tau=0.002, hp=200):
    return high(noise(dur, seed), hp) * decay(n_of(dur), tau)


def clank(dur, base, seed, tau=0.0015, q=(25, 70)):
    """Golpe metálico: impulso de ruido que excita los modos de una pieza."""
    return resonate(impulse(dur, seed, tau), metal_modes(base, seed + 1, q=q))


def grains(dur, seed, rate, lo, hi, grain=0.004, density_decay=None):
    """Textura granular (grava, roce, chispas): muchos micro-impulsos filtrados."""
    r = rng_of(seed)
    out = np.zeros(n_of(dur))
    g = max(8, n_of(grain))
    burst = band(r.standard_normal(g * 64), lo, hi)       # banco de granos ya filtrados
    tpos = 0.0
    while tpos < dur:
        s = n_of(tpos)
        if s + g >= len(out):
            break
        amp = r.uniform(0.3, 1.0)
        if density_decay:
            amp *= np.exp(-tpos / density_decay)
        k = r.integers(0, 63) * g
        out[s:s + g] += burst[k:k + g] * decay(g, grain / 3) * amp
        tpos += r.exponential(1.0 / rate)
    return out


def sparks(dur, seed, rate=120):
    """Chisporroteo eléctrico: crujidos secos y agudos irregulares."""
    return grains(dur, seed, rate, 3000, 12000, grain=0.0015, density_decay=dur / 2.5)


def hiss(dur, seed, lo=1200, hi=7000, attack=0.01):
    """Siseo neumático/hidráulico con aleteo irregular."""
    n = n_of(dur)
    flutter = 1 + 0.6 * band(noise(dur, seed + 1), 8, 40, order=2)
    return band(noise(dur, seed), lo, hi) * flutter * env(n, attack, dur * 0.8)


def servo(dur, f0, f1, seed):
    """Servomotor real: tren de clics irregular (dientes de engranaje) por resonancias de carcasa + motor."""
    r = rng_of(seed)
    n = n_of(dur)
    f = np.linspace(f0, f1, n) * (1 + 0.03 * np.cumsum(r.standard_normal(n)) / np.sqrt(n))
    phase = np.cumsum(np.maximum(f, 1)) / SR
    teeth = (np.diff(np.floor(phase), prepend=0) > 0).astype(float) * r.uniform(0.5, 1.0, n)
    body = resonate(teeth, [(min(f0 * 3.1, 15000), 8, 1.0), (1800, 6, 0.6), (3400, 5, 0.4)])
    motor = band(noise(dur, seed + 2), max(30, min(f0, f1) * 0.8), max(f0, f1) * 1.6 + 40, order=2) * 0.3
    return (body + motor) * env(n, dur * 0.15, dur * 0.35)


def comb_glide(x, f0, f1, feedback=0.82):
    """Tubo metálico resonante cuyo tono va de f0 a f1 (filtro peine con retardo variable)."""
    n = len(x)
    d = SR / np.geomspace(f0, f1, n)
    y = np.zeros(n)
    for i in range(n):
        j = i - d[i]
        if j >= 1:
            k = int(j)
            frac = j - k
            y[i] = x[i] + feedback * ((1 - frac) * y[k] + frac * y[min(k + 1, i - 1)])
        else:
            y[i] = x[i]
    return y


def rasp(dur, f, seed, jitter=0.06):
    """Pulso rasposo tipo 'cuerdas vocales' de la máquina (irregular, no un tono limpio)."""
    r = rng_of(seed)
    n = n_of(dur)
    out = np.zeros(n)
    tpos = 0.0
    while tpos < dur:
        s = n_of(tpos)
        if s >= n:
            break
        out[s] = r.uniform(0.6, 1.0)
        tpos += (1.0 / f) * (1 + r.uniform(-jitter, jitter))
    return low(out, 3000) + 0.15 * band(noise(dur, seed + 1), 300, 3000)


def reverb(x, seed, tail=1.0, wet=0.25, damp=5000, predelay=0.01, early=6):
    """Reverberación sintética: reflexiones tempranas + cola difusa amortiguada."""
    r = rng_of(seed)
    n = n_of(tail)
    ir = r.standard_normal(n) * np.exp(-np.arange(n) / (SR * tail / 6.9))
    ir = low(ir, damp, order=2)
    for _ in range(early):
        ir[n_of(r.uniform(0.004, 0.05))] += r.uniform(0.4, 1.0) * r.choice([-1, 1]) * 3
    ir = np.concatenate([np.zeros(n_of(predelay)), ir])
    ir /= np.sqrt(np.sum(ir ** 2)) + 1e-9
    wet_sig = signal.fftconvolve(x, ir)
    dry = np.pad(x, (0, len(wet_sig) - len(x)))
    return dry * (1 - wet) + wet_sig * wet * 4


def slap_echo(x, delays_gains, lp=1500):
    """Ecos de rebote en el entorno (exterior)."""
    total = len(x) + n_of(max(d for d, _ in delays_gains)) + 1
    out = np.pad(x, (0, total - len(x)))
    for d, g in delays_gains:
        s = n_of(d)
        out[s:s + len(x)] += g * low(x, lp, order=2)
    return out


def finish(x, peak=0.8, drive=1.0):
    x = x / (np.max(np.abs(x)) + 1e-9)
    loud = np.nonzero(np.abs(x) > 10 ** (-60 / 20))[0]
    if len(loud):
        x = x[: min(len(x), loud[-1] + n_of(0.05))]          # sin silencio sobrante al final
    if drive != 1.0:
        x = np.tanh(x * drive)
    x = x.copy()
    tail = min(len(x), n_of(0.03))
    x[-tail:] *= np.linspace(1, 0, tail)
    fade = min(len(x), n_of(0.002))
    x[:fade] *= np.linspace(0, 1, fade)
    return x / (np.max(np.abs(x)) + 1e-9) * peak


def save(name, x, folder, peak=0.8, drive=1.0):
    out = ROOT / folder
    out.mkdir(parents=True, exist_ok=True)
    sf.write(out / f"{name}.ogg", finish(x, peak, drive).astype(np.float32), SR, format="OGG", subtype="VORBIS")
    print(f"{folder}/{name}.ogg  {len(x) / SR:.2f}s")


# =================================================================== Pistola Cinética (arma real)
def gunshot(i):
    """Disparo: estallido MUY seco y fuerte + cola de reverberación continua.
    Sin ecos discretos ni graves largos (sonaban a "pelota que rebota")."""
    seed = 1000 + 17 * i
    n = n_of(0.2)
    r = rng_of(seed)
    # Onda de choque (N-wave): subida instantánea, ~1,5 ms, banda ancha
    crack = high(noise(0.2, seed), 800) * decay(n, 0.0012) * 2.0
    # Fogonazo: ráfaga de ruido muy saturada (como una grabación real que satura el micro)
    blast = np.tanh(low(noise(0.2, seed + 1), 4000) * decay(n, 0.012) * 9) * 0.8
    # Cuerpo: medios-graves CORTOS (nada de "boom" largo)
    body = band(noise(0.2, seed + 2), 70, 500) * decay(n, 0.028) * 2.2
    # Mecanismo, muy discreto
    action = clank(0.05, 2200 + 80 * i, seed + 3, tau=0.0008, q=(30, 60)) * 0.08
    shot = mix(0.2, (crack, 0, 1.0), (blast, 0, 1.0), (body, 0, 1.0), (action, 0.01, 1.0))
    # Cola de exterior: ruido difuso que se apaga (reverberación continua, sin repeticiones)
    tail_n = n_of(0.9)
    tail = band(noise(0.9, seed + 4), 250, 2500) * np.exp(-np.arange(tail_n) / (SR * 0.18)) * 0.22
    tail *= np.clip(np.arange(tail_n) / n_of(0.012), 0, 1)            # entra suave tras el estallido
    return mix(1.0, (shot, 0, 1.0), (tail, 0.006 + r.uniform(0, 0.004), 1.0))


def bullet_impact():
    seed = 1100
    n = n_of(0.3)
    hit = high(noise(0.3, seed), 600) * decay(n, 0.004)
    thud = low(noise(0.3, seed + 1), 400) * decay(n, 0.03) * 1.5
    debris = grains(0.45, seed + 2, 500, 900, 6000, density_decay=0.08)            # polvo y esquirlas
    shards = resonate(grains(0.35, seed + 3, 160, 2000, 9000, density_decay=0.07),
                      [(4200, 60, 1.0), (5900, 70, 0.8), (7700, 80, 0.6)]) * 0.5    # cristal de cinetrita
    return reverb(mix(0.5, (hit, 0, 1.0), (thud, 0, 1.0), (debris, 0.003, 0.6), (shards, 0.005, 0.7)),
                  seed + 4, tail=0.6, wet=0.18)


def hit_confirm():
    """Impacto en una criatura: golpe sordo y seco (como dar a una armadura), suave."""
    seed = 1200
    n = n_of(0.12)
    thwack = low(noise(0.12, seed), 1400) * decay(n, 0.012)
    tick = clank(0.08, 2600, seed + 1, tau=0.0008, q=(20, 40)) * 0.25
    return mix(0.12, (thwack, 0, 1.0), (tick, 0, 1.0))


def dry_fire():
    seed = 1300
    return mix(0.2, (clank(0.08, 3100, seed, tau=0.0008, q=(35, 80)), 0, 1.0),
               (low(noise(0.05, seed + 1), 500) * decay(n_of(0.05), 0.008), 0, 0.5))


def friction(dur, seed, lo=900, hi=4500):
    """Roce de metal/polímero: granular con modulación irregular."""
    return grains(dur, seed, 900, lo, hi, grain=0.003) * env(n_of(dur), dur * 0.3, dur * 0.4)


def mag_out():
    seed = 1400
    return reverb(mix(0.45,
                      (clank(0.06, 2800, seed, tau=0.001, q=(30, 60)), 0.0, 0.8),        # botón del retén
                      (friction(0.16, seed + 1), 0.02, 0.6),                              # sale el cargador
                      (clank(0.1, 1300, seed + 2, q=(15, 40)), 0.18, 0.35)),              # roza al soltarse
                  seed + 3, tail=0.35, wet=0.12)


def mag_in():
    seed = 1500
    n = n_of(0.1)
    return reverb(mix(0.4,
                      (friction(0.09, seed, 700, 3500), 0.0, 0.5),
                      (clank(0.15, 1500, seed + 1, q=(20, 50)), 0.085, 1.0),              # golpe de palma
                      (low(noise(0.1, seed + 2), 350) * decay(n, 0.02), 0.085, 0.9),
                      (clank(0.05, 3300, seed + 3, tau=0.0008, q=(40, 80)), 0.14, 0.6)),  # encaja
                  seed + 4, tail=0.35, wet=0.12)


def slide_rack():
    seed = 1600
    n = n_of(0.12)
    return reverb(mix(0.5,
                      (friction(0.12, seed, 1200, 5500), 0.0, 0.6),                       # tirón atrás
                      (clank(0.06, 2400, seed + 1, q=(30, 60)), 0.11, 0.5),               # tope trasero
                      (clank(0.2, 1250, seed + 2, q=(25, 55)), 0.19, 1.0),                # cierra de golpe
                      (low(noise(0.12, seed + 3), 300) * decay(n, 0.02), 0.19, 0.8)),
                  seed + 5, tail=0.35, wet=0.12)


def generate_pistol():
    for i in range(3):
        save(f"fire{i + 1}", gunshot(i), "pistol", peak=0.9, drive=1.6)
    save("impact", bullet_impact(), "pistol")
    save("hit", hit_confirm(), "pistol")
    save("dry_fire", dry_fire(), "pistol")
    save("mag_out", mag_out(), "pistol")
    save("mag_in", mag_in(), "pistol")
    save("slide", slide_rack(), "pistol")


# =================================================================== Cuerpo de Enjambre (máquina orgánica)
def machine_voice(dur, f0, f1, seed, rasp_f=70):
    """Voz de la máquina: aire + pulso rasposo por un tubo metálico resonante que cambia de tono."""
    n = n_of(dur)
    src = rasp(dur, rasp_f, seed) * 0.7 + band(noise(dur, seed + 1), 200, 5000) * 0.5
    tube = comb_glide(src, f0, f1, feedback=0.82)
    body = band(tube, 120, 4500, order=2)
    body = resonate(body, [(f0 * 2.2, 3, 0.5), (900, 4, 0.4), (2300, 5, 0.3)]) + body * 0.6
    return body * env(n, dur * 0.18, dur * 0.45)


def swarm_ambient(i):
    seed = 2000 + 31 * i
    dur = [1.9, 2.2, 1.7][i]
    voice = machine_voice(dur * 0.8, [140, 110, 170][i], [95, 150, 120][i], seed, rasp_f=[60, 52, 74][i])
    return reverb(mix(dur,
                      (voice, 0.05, 1.0),
                      (servo(0.5, [55, 48, 62][i], [80, 70, 90][i], seed + 2), 0.2 + 0.25 * i, 0.5),
                      (hiss(0.35, seed + 3, 1500, 6000), dur * 0.55, 0.25),                # respiración neumática
                      (grains(dur, seed + 4, 7, 1500, 5000, grain=0.006), 0.0, 0.35)),     # chasquidos de juntas
                  seed + 5, tail=1.2, wet=0.3, damp=4000)


def swarm_hurt(i):
    seed = 2100 + 29 * i
    voice = machine_voice(0.45, [260, 300][i], [420, 380][i], seed, rasp_f=95)           # quejido que sube
    return reverb(mix(0.7,
                      (clank(0.4, [480, 560][i], seed + 1, tau=0.002, q=(12, 35)) * 1.4, 0.0, 1.0),  # placa golpeada
                      (low(noise(0.15, seed + 2), 600) * decay(n_of(0.15), 0.03), 0.0, 0.7),
                      (sparks(0.3, seed + 3, rate=180), 0.01, 0.55),                      # chispas
                      (voice, 0.04, 0.8)),
                  seed + 4, tail=0.8, wet=0.25)


def swarm_death():
    seed = 2200
    dur = 3.0
    groan = machine_voice(1.6, 180, 55, seed, rasp_f=48)                                  # se apaga, cayendo de tono
    wind_down = servo(1.4, 90, 12, seed + 1)                                              # motores deteniéndose
    collapse = mix(1.0,
                   (low(noise(0.4, seed + 2), 180, order=6) * decay(n_of(0.4), 0.09) * 3, 0.0, 1.0),  # golpe contra el suelo
                   (clank(0.6, 380, seed + 3, q=(15, 40)), 0.0, 0.9),
                   (clank(0.5, 720, seed + 4, q=(20, 50)), 0.12, 0.6),
                   (grains(0.6, seed + 5, 300, 800, 5000, density_decay=0.12), 0.0, 0.5))
    return reverb(mix(dur,
                      (groan, 0.0, 1.0),
                      (sparks(1.2, seed + 6, rate=150), 0.1, 0.5),
                      (wind_down, 0.3, 0.6),
                      (hiss(0.9, seed + 7, 900, 5000, attack=0.005), 0.9, 0.45),         # se libera la presión
                      (collapse, 1.5, 1.0),
                      (grains(0.6, seed + 8, 12, 2500, 7000, grain=0.008), 2.2, 0.35)),  # piezas que se asientan
                  seed + 9, tail=1.5, wet=0.3, damp=3500)


def swarm_step(i):
    seed = 2300 + 13 * i
    n = n_of(0.2)
    thud = low(noise(0.2, seed), 140, order=6) * decay(n, 0.04) * 6.0
    plate = clank(0.15, [620, 700, 560][i], seed + 1, tau=0.001, q=(15, 40)) * 0.5
    gravel = grains(0.18, seed + 2, 700, 600, 5000, grain=0.003, density_decay=0.04)
    return reverb(mix(0.25, (thud, 0, 1.0), (plate, 0.002, 0.8), (gravel, 0.0, 0.3)),
                  seed + 3, tail=0.4, wet=0.15)


def swarm_leap():
    seed = 2400
    return reverb(mix(0.9,
                      (hiss(0.5, seed, 800, 6000, attack=0.003) * 1.2, 0.0, 1.0),        # descarga hidráulica
                      (servo(0.45, 70, 160, seed + 1), 0.0, 0.7),
                      (clank(0.3, 450, seed + 2, q=(10, 30)), 0.02, 0.6),                # estructura bajo tensión
                      (low(noise(0.2, seed + 3), 150, order=6) * decay(n_of(0.2), 0.04) * 2, 0.05, 0.8)),  # impulso
                  seed + 4, tail=0.8, wet=0.22)


def generate_swarm():
    for i in range(3):
        save(f"ambient{i + 1}", swarm_ambient(i), "swarm_body")
        save(f"step{i + 1}", swarm_step(i), "swarm_body")
    for i in range(2):
        save(f"hurt{i + 1}", swarm_hurt(i), "swarm_body")
    save("death", swarm_death(), "swarm_body")
    save("leap", swarm_leap(), "swarm_body")


if __name__ == "__main__":
    generate_swarm()
    generate_pistol()
