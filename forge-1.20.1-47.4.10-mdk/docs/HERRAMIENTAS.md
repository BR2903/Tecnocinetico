# Herramientas: generación de modelos, texturas e interfaces

Los modelos y texturas de las piezas complejas **no se dibujan a mano**: los generan scripts Python de `tools/`.
Cada elemento del modelo recibe su propio trozo de textura (UV empaquetada automáticamente), así que textura y geometría siempre coinciden.
**Si editas el JSON o el PNG a mano y luego alguien vuelve a ejecutar el script, se pierden los cambios** → modifica el script.

Requisitos: Python 3.13 + Pillow (`pip install pillow`). Ejecutar desde `forge-1.20.1-47.4.10-mdk/`:

```powershell
$A = "src\main\resources\assets\tecnocinetico"
```

| Script | Genera | Después ejecutar |
|---|---|---|
| `tools/pistol_gen.py $A` | Pistola: textura 128 px, `kinetic_pistol.json` (overrides), 80 modelos `kinetic_pistol*_3d.json` (16 combinaciones de módulos × 5 fotogramas de recarga), modelos de los 4 módulos | `gui_fit` (ver abajo) |
| `tools/hammer_gen.py $A` | Martillo: textura + `kinetic_hammer_3d.json` + `kinetic_hammer.json` | `python tools/gui_fit.py $A "0,25,-45" kinetic_hammer_3d` |
| `tools/bench_gen.py $A` | Banco de Armas: textura, modelo de bloque/item, blockstate | — |
| `tools/bench_gui_gen.py <png> <preview.png>` | Interfaz del banco (`textures/gui/weapon_bench.png`) | — |
| `tools/press_gui_gen.py <png> <preview.png>` | Interfaz de la prensa automática (`textures/gui/auto_kinetic_press.png`) | — |
| `tools/swarm_texture.py $A\textures\entity` | Textura + capa luminosa del Cuerpo de Enjambre (UV del modelo Java `SwarmBodyModel`) | — |
| `tools/sound_gen.py $A\sounds` | Sonidos sintetizados (OGG mono, numpy/scipy/soundfile). `sounds/swarm_body/`: ambiente ×3, daño ×2, muerte, pasos ×3, salto. `sounds/pistol/`: disparo ×3, impacto, acierto, clic en vacío, cargador fuera/dentro, corredera. Registrados en `sounds.json` + `sound/ModSounds.java` | — |
| `tools/import_sounds.py $A\sounds` | Sonidos de la pistola a partir de **grabaciones reales** de `tools/sound_sources/` (disparos ×4, cargador fuera/dentro, corredera, clic en vacío). Licencias en `CREDITOS_SONIDOS.md` | — |
| `tools/resize_textures.py <dir> <backup>` | Reescala texturas que no son potencia de 2 (guarda copia) | — |
| `tools/gui_fit.py` | Calcula la vista de inventario (`display.gui`) para que el modelo llene la casilla | — |
| `tools/render_model.py` | **Vista previa** de modelos JSON en PNG, sin abrir el juego | — |

## gui_fit.py (vista de inventario)

```powershell
# Pistola: una llamada por combinación de módulos (cada una con su propio tamaño), mismos valores para sus 5 fotogramas
foreach ($m in 0..15) {
  $p = if ($m -eq 0) { "kinetic_pistol" } else { "kinetic_pistol_m$m" }
  $names = @("${p}_3d") + (1..4 | % { "${p}_reload_${_}_3d" })
  python tools/gui_fit.py $A "y-90;z25" @names
}
foreach ($m in "optic","extended_mag","inercita_barrel","quick_reload") { python tools/gui_fit.py $A "y-135;x30" "module_$m" }
```

- Rotación como `"rx,ry,rz"` (ángulos de Minecraft) o como pasos en pantalla `"y-90;z25"` (primero Y −90, luego Z +25).
- `fill=19` como primer nombre para que sobresalga de la casilla.
- La pistola usa `"y-90;z25"` (inclinada) — **elegido por el usuario**.

## render_model.py (vista previa)

```powershell
python tools/render_model.py $A preview.png kinetic_pistol_3d kinetic_pistol_m15_3d@gui
```
- Sin sufijo: 3 vistas (lado, 3/4, otro lado). Con `@gui`: como se verá en la casilla del inventario.
- Para bloques: copiar el modelo a una carpeta con estructura `models/item/` + `textures/...` y apuntar ahí (ver `bench_gen.py`).

## Añadir piezas a la pistola

En `pistol_gen.py`:
1. Añadir un material (función `m_xxx(f, x, y)` que devuelve RGBA) y registrarlo en `MATERIALS`.
2. Añadir el elemento a `ELEMENTS` con `"mat"`. Opcional: `"group": "slide"` (se mueve al montar), `"group": "mag"` (cae al recargar), `"if": BIT` / `"unless": BIT` (solo con/sin un módulo), `"rotation": GRIP_ROT` (inclinado con la empuñadura).
3. Un módulo nuevo: nuevo bit en `WeaponModule.java` **y** en `pistol_gen.py` (`OPTIC, EXTENDED_MAG, ...`); el script genera 2^n combinaciones (ojo: con 5 módulos serían 160 modelos).
4. Regenerar, ejecutar `gui_fit`, previsualizar.

Los resultados de los scripts actuales son reproducibles (semillas aleatorias fijas): regenerar sin cambios produce archivos idénticos.
