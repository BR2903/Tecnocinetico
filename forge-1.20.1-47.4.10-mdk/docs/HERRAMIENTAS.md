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
| `tools/pistol_gen.py $A` | Pistola: textura 128 px, `kinetic_pistol.json` (overrides), 160 modelos `kinetic_pistol*_3d.json` (32 combinaciones de módulos × 5 fotogramas de recarga), modelos de los 5 módulos | `gui_fit` (ver abajo) |
| `tools/hammer_gen.py $A` | Martillo: textura + `kinetic_hammer_3d.json` + `kinetic_hammer.json` | `python tools/gui_fit.py $A "0,25,-45" kinetic_hammer_3d` |
| `tools/bench_gen.py $A` | Banco de Armas: textura, modelo de bloque/item, blockstate | — |
| `tools/bench_gui_gen.py <png> <preview.png>` | Interfaz del banco (`textures/gui/weapon_bench.png`) | — |
| `tools/press_gui_gen.py <png> <preview.png>` | Interfaz de la prensa automática (`textures/gui/auto_kinetic_press.png`) | — |
| `tools/swarm_texture.py $A\textures\entity` | Textura + capa luminosa del Cuerpo de Enjambre (UV del modelo Java `SwarmBodyModel`) | — |
| `tools/sound_gen.py $A\sounds` | Sonidos sintetizados (OGG mono, numpy/scipy/soundfile). `sounds/swarm_body/`: ambiente ×3, daño ×2, muerte, pasos ×3, salto. `sounds/pistol/`: disparo ×3, impacto, acierto, clic en vacío, cargador fuera/dentro, corredera. Registrados en `sounds.json` + `sound/ModSounds.java` | — |
| `tools/import_sounds.py $A\sounds` | Sonidos de la pistola a partir de **grabaciones reales** de `tools/sound_sources/` (disparos ×4, cargador fuera/dentro, corredera, clic en vacío). Licencias en `CREDITOS_SONIDOS.md` | — |
| `tools/world_blocks_gen.py [preview.png]` | **Bloques del mundo** (fase 1 de PLAN_MUNDO): texturas 16×16, blockstates, modelos, loot, tags de herramienta, recetas y textos. Los bloques se registran en `block/ModWorldBlocks.java` con los mismos nombres | — |
| `tools/resize_textures.py <dir> <backup>` | Reescala texturas que no son potencia de 2 (guarda copia) | — |
| `tools/gui_fit.py` | Calcula la vista de inventario (`display.gui`) para que el modelo llene la casilla | — |
| `tools/render_model.py` | **Vista previa** de modelos JSON en PNG, sin abrir el juego | — |

## gui_fit.py (vista de inventario)

```powershell
# Pistola: una llamada por combinación de módulos (cada una con su propio tamaño), mismos valores para sus 5 fotogramas
foreach ($m in 0..31) {
  $p = if ($m -eq 0) { "kinetic_pistol" } else { "kinetic_pistol_m$m" }
  $names = @("${p}_3d") + (1..4 | % { "${p}_reload_${_}_3d" })
  python tools/gui_fit.py $A "y-90;z25" @names
}
foreach ($m in "optic","extended_mag","inercita_barrel","quick_reload","seeker") { python tools/gui_fit.py $A "y-135;x30" "module_$m" }
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
3. Un módulo nuevo: nuevo bit en `WeaponModule.java` **y** en `pistol_gen.py` (`OPTIC, EXTENDED_MAG, ...`); el script genera 2^n combinaciones (con 5 módulos ya son 160 modelos; un 6º módulo daría 320: plantéate una pieza sin variantes visuales).
4. Regenerar, ejecutar `gui_fit`, previsualizar.

Los resultados de los scripts actuales son reproducibles (semillas aleatorias fijas): regenerar sin cambios produce archivos idénticos.

## sync_lang.py (idiomas)
- `python tools/sync_lang.py` copia `es_es.json` a es_ar, es_cl, es_ec, es_mx, es_uy y es_ve. Ejecutar tras editar los textos en español (world_blocks_gen.py ya lo hace al final).

## world_features_gen.py (detalles del terreno)
- `python tools/world_features_gen.py` genera `worldgen/configured_feature`, `worldgen/placed_feature` y `forge/biome_modifier/features/` (montones de escombros/chatarra y pedazos por bioma). Cada placed_feature va en un único biome_modifier para evitar "Feature order cycle".

## world_structures_gen.py (estructuras)
- `python tools/world_structures_gen.py` genera `worldgen/structure`, `worldgen/structure_set`, etiquetas `has_structure/*` y botín `loot_tables/chests/`.

## render_dump.py (ver estructuras sin abrir el juego)
- `runGameTestServer` genera las estructuras del mod y vuelca sus bloques en `run/gametest_dump/<nombre>.txt`.
- `python tools/render_dump.py salida.png run/gametest_dump/a.txt [b.txt ...]` las dibuja en isométrico (colores aproximados; bloques sin color = magenta, añádelos a `COLORS`).
- `tools/empty_template_gen.py` creó `structures/empty.nbt` (plantilla vacía de los GameTests); no hace falta volver a ejecutarlo.

## survivor_skin_gen.py (pieles de los Supervivientes)
- `python tools/survivor_skin_gen.py [preview.png]` genera `textures/entity/survivor/{scrapper,gunsmith,farmer}.png` (formato piel de jugador 64x64 con capas exteriores) y una vista previa de frente y espalda.

## swarm_wasp_texture.py (enjambre)
- `python tools/swarm_wasp_texture.py [preview.png]` genera las texturas de las avispas (una por papel + capa luminosa), del Nodo de Colmena y el icono del Núcleo de Enjambre.
- Sonidos del enjambre: `generate_wasps()` en `tools/sound_gen.py` (carpetas `sounds/swarm_wasp/` y `sounds/hive_node/`).

## ammo_gen.py (munición y cargadores)
- `python tools/ammo_gen.py` genera `textures/item/ammo.png` y los modelos 3D de `kinetic_round`, `guided_round`, `magazine(_loaded|_guided)` y `extended_magazine(_loaded|_guided)`. Conserva el `display.gui` existente: tras cambiar la geometría, ejecutar `gui_fit.py $A "y-135;x30" <modelo>` en cada uno y volver a generar.

## turret_texture.py (Torreta Centinela)
- `python tools/turret_texture.py [preview.png]` genera `entity/sentry_turret.png` y las capas luminosas `sentry_turret_glow.png` (cian) y `sentry_turret_charge.png` (rojo, al cargar).

## tactical_gui_gen.py (Equipo Táctico)
- `python tools/tactical_gui_gen.py [preview.png]` genera `textures/gui/tactical_gear.png` (panel 176x200 + pestañas en x=176). Las ranuras deben coincidir con `TacticalMenu.POS`. Los modelos de los gadgets están en `ammo_gen.py`.

## worn_gear_texture.py (gadgets puestos)
- `python tools/worn_gear_texture.py [preview.png]` genera `entity/worn_gear.png` y `worn_gear_glow.png` para `client/WornGearModel` (las zonas deben coincidir con los `texOffs` del modelo).

## armor_gen.py y weapons_gen.py (tier 1)
- `python tools/armor_gen.py [preview.png]`: texturas puestas `models/armor/scrap_layer_1/2.png` e iconos de la armadura de chatarra.
- `python tools/weapons_gen.py`: atlas `item/weapons_t1.png` y modelos 3D de escopeta (+abierta), rifle (+cerrojo atrás), machete, cartuchos y balas; luego `gui_fit.py` ("y-90;z25" las armas largas, "y-135;x30" la munición y el machete) y volver a ejecutar (conserva `display.gui`).

## check_models.py (revisar modelos sin abrir el juego)
- `python tools/check_models.py`: todos los modelos JSON del mod; avisa de elementos fuera de [-16, 32], rotaciones no válidas, texturas que faltan y overrides/padres inexistentes. Ejecutarlo siempre tras generar modelos.

- `weapons_gen.py` genera además las 8 combinaciones de módulos de escopeta y rifle (`pipe_shotgun_m<n>`, `bolt_rifle_m<n>`, con su variante `_open` / `_bolt`) y los modelos de sus módulos. `gui_fit` por combinación: `"y-90;z25" <arma>_m<n> <arma>_m<n>_<open|bolt>`.

## drone_texture.py
- `python tools/drone_texture.py [preview.png]`: texturas del Dron del Núcleo (`drone.png`, `drone_glow.png`, `drone_alert.png`). Sonidos: `generate_drone()` de `sound_gen.py` (se puede llamar suelto importando el módulo).

## corrupted_skin_gen.py y corrupted_spider_texture.py
- `python tools/corrupted_skin_gen.py [preview.png]`: pieles 64x64 de los Corruptos (`textures/entity/corrupted/{wanderer,worker,crystal,raider,soldier}.png` + `_glow`).
- `python tools/corrupted_spider_texture.py [preview.png]`: `corrupted_spider.png` + `_glow` (UV del modelo vanilla de araña, 64x32).

## armor_t2_gen.py y armas tier 2
- `python tools/armor_t2_gen.py [preview.png]`: capas puestas de las armaduras de inercita, táctica y exoesqueleto (+ iconos) y los iconos 32x32 de las piezas de prensa. La vista previa las muestra sobre un maniquí.
- `weapons_gen.py` también genera `pump_shotgun`, `semi_rifle` (8 combinaciones x 2) y `kinetic_smg`; vista de inventario con `gui_fit.py $A "y-90;z25"` pasando todos los nombres de cada arma.

## anchor_texture.py
- `python tools/anchor_texture.py [preview.png]`: texturas del Anclador (`anchor.png`, `anchor_glow.png`, `anchor_charge.png`). Sonidos: `generate_anchor()` de `sound_gen.py`.

## Tier 3
- `python tools/tier3_art.py [preview.png]`: frente del Ensamblador (`block/weapon_assembler_front.png`) e icono del Procesador del Núcleo.
- `python tools/assembler_gui_gen.py <gui/weapon_assembler.png> <preview.png>`: interfaz del Ensamblador (6 huecos).
- `weapons_gen.py`: `rail_rifle`, `assault_rifle`, `grenade_launcher`, `pulse_cannon` (+ fotograma de recarga) y munición 3D; armas con `gui_fit "y-90;z25"`, munición con `"y-135;x30"`.
- Nota: la vista previa (`render_model.py`) a veces esconde un elemento tapado por otro más ancho (p. ej. el cuerpo de la granada bajo su banda); en el juego se ve bien.
