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
    blast = np.tanh(low(noise(0.2, seed + 1), 3500) * decay(n, 0.018) * 12) * 0.9      # más grueso
    # Cuerpo: medios-graves CORTOS (nada de "boom" largo)
    body = band(noise(0.2, seed + 2), 70, 500) * decay(n, 0.04) * 3.0
    punch = np.tanh(band(noise(0.2, seed + 5), 100, 300, order=2) * decay(n, 0.05) * 6) * 0.9   # 'puñetazo' grave-medio
    # Mecanismo, muy discreto
    action = clank(0.05, 2200 + 80 * i, seed + 3, tau=0.0008, q=(30, 60)) * 0.08
    shot = mix(0.2, (crack, 0, 0.8), (blast, 0, 1.0), (body, 0, 1.0), (punch, 0, 1.0), (action, 0.01, 1.0))
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
    # Disparo, cargador, corredera y clic en vacío son GRABACIONES REALES (tools/import_sounds.py).
    # Aquí solo se sintetizan el impacto (con el cristal de cinetrita) y la confirmación de acierto.
    save("impact", bullet_impact(), "pistol")
    save("hit", hit_confirm(), "pistol")


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


# =================================================================== Avispas del Enjambre y Nodo de Colmena
def wing_buzz(dur, rate, seed, wobble=0.08, bright=1.0):
    """Aleteo: ráfagas de ruido al ritmo de las alas (rate Hz, con deriva irregular) por resonancias de
    caparazón metálico. No es un tono: es ruido pulsado, como un insecto real (o una máquina que lo imita)."""
    n = n_of(dur)
    t = np.arange(n) / SR
    r = rng_of(seed)
    drift = 1.0 + wobble * low(r.standard_normal(n), 3, order=2) * 8      # deriva lenta de la frecuencia
    phase = np.cumsum(rate * drift / SR) % 1.0
    pulse = np.clip(1.0 - phase * 2.4, 0, 1) ** 1.5                       # golpe de ala: ataque seco, cola corta
    carrier = band(noise(dur, seed + 1), 180, 7000)
    body = carrier * (0.25 + pulse)
    shell = resonate(body, [(rate * 4.1, 6, 0.5), (1350 * bright, 5, 0.6), (2900 * bright, 8, 0.45), (4800, 10, 0.2)])
    return (shell + body * 0.35) * env(n, 0.05, 0.12)


def wasp_buzz(i):
    seed = 2600 + 17 * i
    dur = [1.3, 1.5, 1.1][i]
    return reverb(mix(dur,
                      (wing_buzz(dur, [196, 212, 182][i], seed), 0.0, 1.0),
                      (servo(dur * 0.6, [140, 160, 120][i], [190, 150, 170][i], seed + 2), dur * 0.2, 0.18)),
                  seed + 3, tail=0.5, wet=0.18)


def wasp_alarm(i):
    seed = 2700 + 23 * i
    chitter = grains(0.5, seed, [38, 46][i], 2500, 8000, grain=0.003)               # mandíbulas chasqueando
    rise = wing_buzz(0.6, [230, 250][i], seed + 1, wobble=0.15, bright=1.25)
    voice = machine_voice(0.35, [520, 600][i], [880, 940][i], seed + 2, rasp_f=140)   # chirrido que sube
    return reverb(mix(0.8, (chitter, 0.0, 0.9), (rise, 0.05, 0.8), (voice, 0.2, 0.5)),
                  seed + 3, tail=0.6, wet=0.2)


def wasp_sting():
    seed = 2800
    tick = clank(0.12, 1900, seed, tau=0.0008, q=(30, 80)) * 1.2                   # aguijón metálico
    jab = low(noise(0.08, seed + 1), 900) * decay(n_of(0.08), 0.015) * 2
    return reverb(mix(0.35, (tick, 0.0, 1.0), (jab, 0.0, 0.6), (hiss(0.18, seed + 2, 2500, 8000, attack=0.002), 0.01, 0.4)),
                  seed + 3, tail=0.3, wet=0.12)


def wasp_hurt(i):
    seed = 2900 + 19 * i
    stutter = wing_buzz(0.4, [150, 170][i], seed, wobble=0.35)                     # las alas fallan
    stutter *= (np.sin(np.arange(len(stutter)) / SR * 2 * np.pi * 14) > -0.2)       # entrecortado
    return reverb(mix(0.55,
                      (clank(0.25, [1300, 1500][i], seed + 1, tau=0.001, q=(20, 50)), 0.0, 0.9),
                      (sparks(0.2, seed + 2, rate=220), 0.0, 0.5),
                      (stutter, 0.03, 0.8)),
                  seed + 3, tail=0.5, wet=0.2)


def wasp_death():
    seed = 3000
    n = n_of(1.4)
    t = np.arange(n) / SR
    rate = 200 * np.exp(-t * 2.2) + 8                                            # el aleteo se apaga
    phase = np.cumsum(rate / SR) % 1.0
    pulse = np.clip(1.0 - phase * 2.4, 0, 1) ** 1.5
    dying = resonate(band(noise(1.4, seed), 180, 6000) * (0.2 + pulse), [(1200, 5, 0.6), (2600, 7, 0.4)])
    dying *= np.exp(-t * 1.6)
    return reverb(mix(1.6,
                      (dying, 0.0, 1.0),
                      (sparks(0.6, seed + 1, rate=160), 0.05, 0.5),
                      (clank(0.3, 1100, seed + 2, q=(15, 40)), 1.05, 0.7),                  # cae al suelo
                      (grains(0.3, seed + 3, 200, 1500, 6000, density_decay=0.06), 1.05, 0.4)),
                  seed + 4, tail=0.8, wet=0.22)


def node_ambient(i):
    seed = 3100 + 41 * i
    dur = 3.0
    n = n_of(dur)
    t = np.arange(n) / SR
    swarm = sum(wing_buzz(dur, 150 + 23 * k, seed + k, wobble=0.12) * (0.5 + 0.1 * k) for k in range(4))  # muchas alas
    hum = machine_voice(dur, [62, 70][i], [70, 58][i], seed + 10, rasp_f=36)
    breath = 0.6 + 0.4 * np.sin(2 * np.pi * 0.5 * t)                              # late despacio
    return reverb(mix(dur, (hum * breath, 0.0, 1.0), (swarm * breath, 0.0, 0.45)),
                  seed + 11, tail=1.5, wet=0.35, damp=3000)


def node_pulse():
    seed = 3200
    thump = low(noise(0.4, seed), 120, order=6) * decay(n_of(0.4), 0.08) * 4
    return reverb(mix(1.0,
                      (thump, 0.0, 1.0),
                      (sparks(0.5, seed + 1, rate=260), 0.0, 0.7),                      # descarga eléctrica
                      (hiss(0.6, seed + 2, 700, 5000, attack=0.003), 0.02, 0.6),
                      (machine_voice(0.5, 90, 160, seed + 3, rasp_f=50), 0.0, 0.5)),
                  seed + 4, tail=1.0, wet=0.3)


def generate_wasps():
    for i in range(3):
        save(f"buzz{i + 1}", wasp_buzz(i), "swarm_wasp")
    for i in range(2):
        save(f"alarm{i + 1}", wasp_alarm(i), "swarm_wasp")
        save(f"hurt{i + 1}", wasp_hurt(i), "swarm_wasp")
        save(f"ambient{i + 1}", node_ambient(i), "hive_node")
    save("sting", wasp_sting(), "swarm_wasp")
    save("death", wasp_death(), "swarm_wasp")
    save("pulse", node_pulse(), "hive_node")


# =================================================================== Torreta Centinela
def turret_charge():
    """Carga del disparo: tubo resonante que sube de tono (condensador cargándose) + servos que ajustan."""
    seed = 3300
    dur = 1.3
    src = band(noise(dur, seed), 200, 6000)
    whine = comb_glide(src, 220, 1500, feedback=0.86)
    whine = band(whine, 300, 7000) * env(n_of(dur), dur * 0.8, 0.05)
    return reverb(mix(dur + 0.2,
                      (whine, 0.0, 1.0),
                      (servo(0.5, 60, 110, seed + 1), 0.0, 0.6),
                      (sparks(dur, seed + 2, rate=40), 0.3, 0.25)),
                  seed + 3, tail=0.4, wet=0.2)


def jetpack_thrust():
    """Empuje del jetpack: chorro de gas (siseo ancho con rugido grave) que respira, no un tono."""
    seed = 3400
    dur = 0.6
    n = n_of(dur)
    roar = low(noise(dur, seed), 400, order=4) * 1.4
    hiss_ = band(noise(dur, seed + 1), 1200, 8000) * 0.6
    flutter = 0.75 + 0.25 * np.sin(2 * np.pi * 23 * np.arange(n) / SR)
    return reverb((roar + hiss_) * flutter * env(n, 0.04, 0.12), seed + 2, tail=0.3, wet=0.15)


def generate_gadgets():
    save("thrust", jetpack_thrust(), "jetpack")


# =================================================================== Dron del Núcleo
def rotor_whir(dur, rate, seed):
    """Un rotor: pulsos de aire al paso de cada aspa (no un tono) por un filtro de banda ancha."""
    n = n_of(dur)
    r = rng_of(seed)
    drift = 1.0 + 0.02 * low(r.standard_normal(n), 2, order=2) * 8
    phase = np.cumsum(rate * drift / SR) % 1.0
    chop = 0.35 + 0.65 * np.clip(1.0 - phase * 2.0, 0, 1) ** 2
    return band(noise(dur, seed + 1), 120, 5000) * chop


def drone_hum(i):
    seed = 3500 + 29 * i
    dur = 2.0
    rotors = sum(rotor_whir(dur, [88, 93, 97, 102][k] + 6 * i, seed + k) for k in range(4)) * 0.4
    motor = resonate(band(noise(dur, seed + 9), 300, 3000, order=2), [(620 + 40 * i, 12, 0.5), (1240 + 80 * i, 14, 0.3)])
    return reverb(mix(dur, (rotors, 0.0, 1.0), (motor, 0.0, 0.25)) * env(n_of(dur), 0.25, 0.25),
                  seed + 10, tail=0.6, wet=0.2)


def drone_scan():
    """Barrido del escáner: servo que gira + tubo resonante que sube y baja suave (zumbido electrónico sucio)."""
    seed = 3600
    dur = 1.0
    src = band(noise(dur, seed), 400, 6000)
    up = comb_glide(src[:n_of(0.5)], 700, 1300, feedback=0.78)
    down = comb_glide(src[n_of(0.5):], 1300, 800, feedback=0.78)
    sweep = band(np.concatenate([up, down]), 500, 6000) * env(n_of(dur), 0.15, 0.3)
    return reverb(mix(dur + 0.2, (sweep, 0.0, 0.6), (servo(0.6, 80, 140, seed + 1), 0.1, 0.8)),
                  seed + 2, tail=0.4, wet=0.2)


def drone_alert():
    """Alarma: tres chasquidos de relé y una voz de máquina que sube con fuerza."""
    seed = 3700
    clicks = mix(0.4, *[(clank(0.08, 2400, seed + k, tau=0.0007), 0.09 * k, 1.0) for k in range(3)])
    voice = machine_voice(0.7, 420, 1100, seed + 5, rasp_f=120)
    return reverb(mix(1.1, (clicks, 0.0, 0.8), (voice, 0.25, 1.0), (sparks(0.4, seed + 6, rate=90), 0.25, 0.3)),
                  seed + 7, tail=0.7, wet=0.25)


def generate_drone():
    for i in range(2):
        save(f"hum{i + 1}", drone_hum(i), "drone")
    save("scan", drone_scan(), "drone")
    save("alert", drone_alert(), "drone")


# =================================================================== Anclador
def anchor_ambient():
    """Zumbido grave del núcleo de inercita con un servo que ajusta las patas."""
    seed = 3800
    dur = 2.2
    hum = machine_voice(dur, 48, 52, seed, rasp_f=30)
    return reverb(mix(dur, (hum, 0.0, 1.0), (servo(0.6, 50, 90, seed + 1), 0.9, 0.5)), seed + 2, tail=1.0, wet=0.3, damp=2500)


def anchor_charge():
    """Carga del haz: tubo resonante que sube de grave a agudo, con chisporroteo creciente."""
    seed = 3900
    dur = 1.1
    src = band(noise(dur, seed), 100, 5000)
    rise = band(comb_glide(src, 110, 900, feedback=0.88), 90, 6000) * env(n_of(dur), dur * 0.9, 0.05)
    return reverb(mix(dur + 0.2, (rise, 0.0, 1.0), (sparks(dur, seed + 1, rate=80), 0.4, 0.35)), seed + 2, tail=0.6, wet=0.25)


def anchor_pull():
    """El lastre: golpe sordo enorme y un tono que se hunde (la gravedad tirando)."""
    seed = 4000
    dur = 1.4
    thump = low(noise(0.6, seed), 90, order=6) * decay(n_of(0.6), 0.12) * 5
    src = band(noise(dur, seed + 1), 80, 3000)
    fall = band(comb_glide(src, 500, 50, feedback=0.86), 40, 3000) * env(n_of(dur), 0.02, dur * 0.6)
    return reverb(mix(dur + 0.3, (thump, 0.0, 1.0), (fall, 0.0, 0.8), (sparks(0.4, seed + 2, rate=200), 0.0, 0.4)),
                  seed + 3, tail=1.2, wet=0.35, damp=2500)


def anchor_stomp():
    """Pisotón: impacto pesado de metal contra el suelo y escombros."""
    seed = 4100
    boom = low(noise(0.5, seed), 120, order=6) * decay(n_of(0.5), 0.1) * 5
    return reverb(mix(1.0, (boom, 0.0, 1.0), (clank(0.4, 180, seed + 1, tau=0.004, q=(10, 30)), 0.0, 0.8),
                      (grains(0.5, seed + 2, 160, 600, 4000, density_decay=0.12), 0.02, 0.5)),
                  seed + 3, tail=0.8, wet=0.25)


def generate_anchor():
    save("ambient", anchor_ambient(), "anchor")
    save("charge", anchor_charge(), "anchor")
    save("pull", anchor_pull(), "anchor")
    save("stomp", anchor_stomp(), "anchor")


def generate_turret():
    save("charge", turret_charge(), "sentry_turret")


if __name__ == "__main__":
    generate_swarm()
    generate_pistol()
    generate_wasps()
    generate_turret()
    generate_gadgets()
    generate_drone()
    generate_anchor()
