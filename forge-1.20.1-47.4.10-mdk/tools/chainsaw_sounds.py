"""Sonidos de la Motosierra: grabaciones reales de una Stihl MS260 (CC0, Joseph SARDIN - BigSoundBank.com; ver
CREDITOS_SONIDOS.md). Sin síntesis.

python tools/chainsaw_sounds.py src/main/resources/assets/tecnocinetico/sounds
Requiere numpy, scipy y soundfile.

Fuentes (tools/sound_sources/, solo los tramos usados de los originales, para no cargar el repositorio):
- bigsoundbank_0982_chainsaw_starting.ogg  0-12 s de "Chainsaw (Starting)" (s0982): tirón de arranque y acelerones.
- bigsoundbank_0983_chainsaw_using.ogg     95-112 s de "Chainsaw, Using" (s0983): ralentí y acelerón.
- bigsoundbank_0707_chainsaw_2.ogg         25-65 s de "Chainsaw #2" (s0707): cortando un tronco y apagado.
Si no existen, se recortan de los originales completos (`--from <carpeta con 0707.ogg, 0982.ogg, 0983.ogg>`).

Salida (sounds/chainsaw/):
- start.ogg   tirón de la cuerda y el motor que arranca (al sacarla con gasolina).
- idle.ogg    bucle: ralentí.
- cut.ogg     bucle: cortando madera con el motor cargado.
- rev.ogg     acelerón al empezar a cortar.
- stop.ogg    el motor se apaga (se acabó la gasolina).
Los bucles se cierran con un fundido cruzado para que no haya chasquido.
"""
import sys
from pathlib import Path

import numpy as np
import soundfile as sf
from scipy import signal

SR = 44100
SRC = Path(__file__).parent / "sound_sources"
SOURCES = {  # nombre en sound_sources: (original, desde, hasta) en segundos del original
    "bigsoundbank_0982_chainsaw_starting.ogg": ("0982.ogg", 0.0, 12.0),
    "bigsoundbank_0983_chainsaw_using.ogg": ("0983.ogg", 95.0, 112.0),
    "bigsoundbank_0707_chainsaw_2.ogg": ("0707.ogg", 25.0, 65.0),
}


def write_ogg(path, x, sr):
    """OGG Vorbis por trozos (libsndfile se cae escribiendo búferes largos de golpe)."""
    with sf.SoundFile(path, "w", sr, 1, format="OGG", subtype="VORBIS") as f:
        for i in range(0, len(x), 4096):
            f.write(x[i:i + 4096])


def mono(x):
    return x.mean(axis=1) if x.ndim > 1 else x


def prepare_sources(orig_dir):
    for name, (orig, a, b) in SOURCES.items():
        x, sr = sf.read(Path(orig_dir) / orig)
        x = mono(x)[int(a * sr):int(b * sr)]
        write_ogg(SRC / name, x, sr)
        print("fuente:", name)


def load(name):
    x, sr = sf.read(SRC / name)
    x = mono(x)
    if sr != SR:
        x = signal.resample_poly(x, SR, sr)
    return x


def cut(x, a, b):
    return x[int(a * SR):int(b * SR)].copy()


def fade(x, fin=0.01, fout=0.05):
    n_in, n_out = int(fin * SR), int(fout * SR)
    if n_in:
        x[:n_in] *= np.linspace(0, 1, n_in)
    if n_out:
        x[-n_out:] *= np.linspace(1, 0, n_out)
    return x


def loop(x, xf=0.35):
    """Bucle sin chasquido: el final se funde con el principio."""
    n = int(xf * SR)
    head, body, tail = x[:n], x[n:-n], x[-n:]
    t = np.linspace(0, 1, n)
    return np.concatenate([body, tail * (1 - t) + head * t])


def clean(x):
    x = signal.sosfilt(signal.butter(2, 40, "highpass", fs=SR, output="sos"), x)   # retumbe del viento
    return x


def norm(x, peak=0.89):
    return x / (np.max(np.abs(x)) + 1e-9) * peak


def main():
    if "--from" in sys.argv:
        prepare_sources(sys.argv[sys.argv.index("--from") + 1])
    out = Path(sys.argv[1] if len(sys.argv) > 1 and not sys.argv[1].startswith("--") else ".") / "chainsaw"
    out.mkdir(parents=True, exist_ok=True)
    starting = clean(load("bigsoundbank_0982_chainsaw_starting.ogg"))
    using = clean(load("bigsoundbank_0983_chainsaw_using.ogg"))       # 0 s = 95 s del original
    log = clean(load("bigsoundbank_0707_chainsaw_2.ogg"))             # 0 s = 25 s del original
    sounds = {
        "start": fade(cut(starting, 0.4, 3.2), 0.005, 0.25),
        "idle": loop(cut(using, 9.2, 14.2)),                          # 104-109 s: ralentí estable
        "cut": loop(cut(log, 4.0, 10.0)),                             # 29-35 s: cortando el tronco
        "rev": fade(cut(using, 3.8, 6.3), 0.01, 0.4),                 # 99-101 s: acelerón
        "stop": fade(cut(log, 36.5, 39.5), 0.02, 0.6),                # 61,5-64,5 s: se apaga
    }
    # el ralentí y el corte, al mismo nivel relativo que en la grabación (el corte suena más)
    ref = max(np.max(np.abs(sounds["cut"])), 1e-9)
    for name, x in sounds.items():
        peak = 0.89 if name in ("cut", "rev", "start") else min(0.89, np.max(np.abs(x)) / ref * 0.89 * 1.6)
        write_ogg(out / f"{name}.ogg", norm(x, peak), SR)
        print(f"{name}.ogg {len(x) / SR:.2f} s")


if __name__ == "__main__":
    main()
