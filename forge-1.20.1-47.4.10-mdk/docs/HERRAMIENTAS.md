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
| `tools/world_props_gen.py` | **Solo arte:** seis blockstates de farolas, postes y cables, siete modelos de bloque, seis de objeto 3D y cuatro texturas `world_props_*`. Semillas fijas; no modifica Java ni datos | `python tools/preview_world_props.py` → `build/postes/comparativa.png`, vistas de los cinco conjuntos, reversos, detalles e inventario |
| `tools/resize_textures.py <dir> <backup>` | Reescala texturas que no son potencia de 2 (guarda copia) | — |
| `tools/check_zfight.py` | Busca caras coplanares que parpadean (z-fighting) en los modelos; `gunkit.resolve_zfight` las corrige solas en armas, pistola y munición | — |
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

## Instalaciones y guardianes restantes del Overworld
- `python tools/overworld_sites_gen.py`: datos de antena, centro de datos, búnker, laboratorio y ciudad sumergida; botín de cofres/guardianes y libros de lore. También lo ejecuta `world_structures_gen.py`. La geometría está en `OverworldSitePiece`.
- `python tools/overworld_guardians_gen.py`: texturas 128x128 (normal, brillo, carga y aturdimiento del Vigía), iconos de las piezas del caza, modelos de huevos/componentes y textos ES/EN. Genera el modelo Java desde `guardian_rig.py` + `guardian_model.java.template` y exporta la misma geometría articulada a `build/guardian_previews/*.json`. Valida que todas las UV quepan en 128 px; editar el rig y la plantilla, no las cajas Java generadas.
- Después: `python tools/sync_lang.py`; `python tools/check_models.py`. Previsualizar con `python tools/render_zbuffer.py build/guardian_previews/network_watcher.png build/guardian_previews/network_watcher.json 205 20 10` (Matriarca: escala 9). Las vistas comparten cajas, jerarquía y rotaciones con el juego; solo omiten la animación. `render_zbuffer.py` admite `preview_vertices` para estos rigs, además de sus modelos JSON normales.
- `python tools/preview_guardians.py`: lámina `build/guardian_previews/guardianes_redisenados.png` con vista frontal/trasera del Vigía (condensadores), Matriarca y sus dos piezas del caza.
- `runGameTestServer` vuelca también las nuevas estructuras. Para interiores, usar `render_dump.py --maxy=N`; los volcados incluyen el terreno circundante, por lo que conviene recortarlo al volumen construido antes de renderizar.
- `python tools/preview_overworld_sites.py`: hace ese recorte y genera `build/overworld_previews/estructuras.png`, una comparativa de las cinco instalaciones; oculta techos interiores y agua para ver la ciudad. Son colores aproximados de bloques, no capturas del cliente.

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

## armor_t3_gen.py
- `python tools/armor_t3_gen.py [preview.png]`: armaduras del Núcleo, Piroxita y Nulita (capas + iconos), lingotes/en bruto de piroxita y nulita (recoloreados de los de inercita) y las capas de cristal de las menas (`block/*_ore_overlay.png`, 16x16 sobre la piedra vanilla).

## ship_texture.py
- `python tools/ship_texture.py [preview.png]`: textura 128x128 + brillo del Caza del Núcleo (UV = texOffs de `CoreFighterModel`). La miniatura del objeto (`core_fighter`) la genera `weapons_gen.py` con las mismas piezas escaladas.

## siege_spider_texture.py
- `python tools/siege_spider_texture.py [preview.png]`: Araña de Asedio (128x128 + brillos cian/rojo/blanco) y bloques de la arena (consola, barrera). Sonidos: `generate_siege()` de `sound_gen.py`.

## combat_armor_gen.py
- `python tools/combat_armor_gen.py [preview.png]`: armaduras de combate Mk I-III (capas + iconos). El Impulsor de Esquiva y su célula: `weapons_gen.py`.

## gunkit.py (texturas de las armas)

- Remodelación actual: `python tools/remodel_weapons.py` genera **solo el arsenal sin pistola**, ajusta el inventario para las 93 variantes/fotogramas y produce `build/weapon_remodel/arsenal_remodelado.png` y `variantes_y_recargas.png`.
- Las 19 geometrías completas están en `tools/weapon_blueprints.py`: cada función empieza con un `Assembly` vacío. `weapons_gen.py` delega las armas a `build_weapon`; se eliminaron los cuerpos de las funciones antiguas y el parcheador `weapon_remodel.py`. Las variantes se construyen con `mods`, `reload` y `spin`. Solo se comparten materiales y contratos de agarre/estados, sin importar modelos anteriores. `weapons_gen.py --weapons-only` regenera solo modelos/atlas; usar el script completo anterior cuando cambie el tamaño.
- `preview_weapon_remodel.py` usa texturas finales, transforma las rotaciones de cada elemento JSON y renderiza con profundidad real. Incluye recargas, variantes con todos los módulos y giro de la Minigun. No modifica el renderizador compartido.
- Bocas: `python tools/weapon_muzzles_gen.py` genera `WeaponMuzzles.java` desde los centros de salida de `weapon_blueprints.py` y exporta 44 perfiles en `build/weapon_muzzles/profiles.json`. `remodel_weapons.py` lo incluye automáticamente. `python tools/preview_weapon_muzzles.py` marca boca/eje sobre los modelos en `build/weapon_muzzles/bocas_comprobadas.png`, sin cambiar sus recursos.
- `auditMobModels` genera los perfiles de auditoría por una tarea Gradle dependiente y ejecuta también `WeaponMuzzleAudit`: ambas manos, cadera/mira y FOV 35/70/110, 528 proyecciones. No abre Minecraft.
- La auditoría incluye además **1.056 fotogramas de retroceso**: comprueba los vértices reales del fogonazo y del inicio de trazadora frente al vértice de boca del modelo actual. `python tools/preview_recoil_muzzles.py` produce `build/weapon_muzzles/retroceso_corregido.png`, una comparación geométrica del error de reutilizar la posición anterior (rojo) frente a la actual (cian); no es una captura del juego.
- `python tools/preview_hand_muzzle_burst.py`: vista de los ticks 0/2/4/6 de la onda celeste local en `build/weapon_muzzles/onda_celeste.png`. Reutiliza el anillo original y simula su alfa, tamaño y desvanecimiento. Para texturas de brillo con RGB blanco en texeles transparentes, no usar mezcla aditiva `RenderType.eyes` como fogonazo: aparece un cuadrado blanco; la onda usa transparencia emisiva normal.
- `python tools/preview_hand_tracer.py`: vista de la estela celeste en los ticks 0/2/4 (`build/weapon_muzzles/estela_celeste.png`). Usa la textura de partícula original y el mismo alfa/color/reducción del renderer. La estela local usa partículas separadas, no un rectángulo con el brillo estirado a todo el recorrido; tampoco debe usar `RenderType.eyes`.
- Vista de inventario: armas largas `gui_fit.py $A "y-90;z25" <modelos>`; machete y cuchilla (geometría ya en diagonal) `gui_fit.py $A "0,0,0" scrap_machete kinetic_blade`.
- La vista previa oculta caras cuyo centro cae dentro de otra pieza (acanaladuras, espigas, tambores): para revisar esas armas, copia `render_model.py` con `hidden()` devolviendo `False`.
- Lo usa `weapons_gen.py`: cada arma se pinta en su propia textura `textures/item/gun/<arma>.png` por pintores de material (ver la cabecera del archivo). Para un arma nueva: escribe su función de geometría con `el(desde, hasta, material, front=..., rot=...)`, regístrala con `model(...)` (añade su nombre base a `GROUPS` si tiene variantes) y ejecuta `python tools/weapons_gen.py`; luego `gui_fit.py` y otra vez `weapons_gen.py`.
- Materiales: steel_dark, steel, steel_hi, inercita, graphite, rubber, wood, wood_dark, leather, copper, brass, shell_red, rust, blade, edge, tape, olive, hazard, cyan, black (boca de cañón), ceramic (aislador de porcelana con faldones), glass (tubo de vidrio con espiras de cobre).


## Sonidos de las armas
- Correcciones de disparo/mecanismos: `python tools/import_sounds.py src/main/resources/assets/tecnocinetico/sounds --feedback-only` regenera solo escopetas, rifles de cerrojo, francotirador, cerrojo y corredera. Evita regenerar la pistola y otros sonidos. Los estampidos se recortan antes del mecanismo grabado; el último disparo Mosin está en 12,24 s, no en 11,69 s. La corredera tiene su propio `guns/pump.ogg` corto (CC0, ver créditos). Textos nuevos de subtítulos: sincronizar con `sync_lang.py`.
- `python tools/import_sounds.py src/main/resources/assets/tecnocinetico/sounds`: además de la pistola, `import_guns()` saca de las grabaciones reales (`tools/sound_sources/`: CZ-52, Mosin, SKS, escopeta) los disparos de `sounds/guns/` (revólver más grave, escopeta, rifle y su cerrojo, francotirador con más golpe, semiautomático, subfusil/fusil de asalto/minigun recortados para el fuego automático).
- `sound_gen.generate_weapons()`: riel, impulso, arco, lanzagranadas, lanzacohetes, giro de la minigun y tajo de onda (sintetizados).
- El sonido de cada arma lo elige `LongGunItem.fireSound()` por id del objeto.
- `pistol_gen.py` no conserva `display.gui`: tras regenerarla, vuelve a pasar `gui_fit` (o recupera la vista de la versión anterior).

## Metales y máquinas eléctricas
- `python tools/metals_gen.py`: plomo, estaño, titanio (texturas, modelos, botín, etiquetas forge, generación, recetas), los 10 polvos del Refinador y los bloques de las máquinas (frente 64 px + cuerpo del Ensamblador). Luego `python tools/sync_lang.py`.
- `python tools/machine_gui_gen.py`: interfaces `gui/<máquina>.png` (posiciones = `MachineKind`).

- `python tools/structures_gen.py`: Ensamblador Industrial (atlas, modelo 3x3x3 en -16..32, brazo), Reactor (texturas de piezas, núcleo, barras, kit) y estados. El reactor ya no sale de `metals_gen.py`.
- `python tools/reactor_model_gen.py`: reactor 9x9x6 como modelo continuo troceado en 18 modelos 3x3x3 (+ atlas `block/reactor.png`, estados de `reactor_shell`, modelo del núcleo).
- `python tools/render_zbuffer.py <salida.png> <modelo.json> [yaw pitch escala]`: vista previa con profundidad real (para unir los 18 trozos, ver la entrada de la bitácora del 2026-11-09).
- `python tools/chamber_model_gen.py`: Cámara de Reacción 2x2x2 (usa el sistema de `reactor_model_gen.py` con su propio atlas), haz y kit.
- `python tools/armor3d_gen.py [carpeta]`: armaduras 3D (modelos por pieza + texturas); con carpeta, vistas previas de cada set en un maniquí (`render_zbuffer.py <png> <carpeta>/<set>_preview.json 200 15 14`).
- `python tools/press_model_gen.py`: Prensa Automática estilo Create (atlas, cuerpo, cabezal animado y modelo del objeto).

## Remodelación de mobs (2026-11-11)
- `python tools/remodel_mobs.py`: regenera modelos Java, atlas de 128 px y geometrías de previsualización de la Araña, máquinas, enjambre, corruptos y supervivientes. Fuentes editables: `mob_remodel_rigs.py` y `mob_humanoid_rigs.py`. Conserva los modelos aprobados del Vigía y la Matriarca.
- `python tools/preview_mob_remodel.py`: genera 20 vistas y la comparativa `build/mob_remodel/todos_los_mobs.png`, usando la misma geometría que Minecraft y profundidad real de `render_zbuffer.py`.
- `cmd /c ".\gradlew.bat auditMobModels"`: construye 13 modelos reales sin ventana ni contexto OpenGL; comprueba piezas usadas por animaciones, anclajes de las seis rodillas y atlas. Complementa las pruebas de servidor, que no cargan modelos de cliente.
- Los atlas finales están en `textures/entity/remodel/`. Los scripts antiguos de pieles siguen siendo las fuentes base de los humanoides: tras modificarlos, ejecutar `remodel_mobs.py` para actualizar las nuevas pieles y accesorios.

## parts_art.py (componentes)
- `python tools/parts_art.py`: arte plano 16x16 (`item/generated`) del fragmento de cinetrita, la Resistencia 330 Ω y el Transductor Piezo-Cúprico. Materiales y componentes pequeños van en plano (preferencia del usuario).

## ask_codex.sh (encargar modelos a Codex)
- `bash tools/ask_codex.sh <encargo.md> [respuesta.txt]`: manda el encargo al chat de Codex del usuario (sesión fija en el script, variable `CODEX_SESSION` para cambiarla) con `codex exec resume`. Respuesta final en el archivo de respuesta y registro completo en `.log`. Reparto y reglas en `AGENTS.md` §3c. Los encargos pueden tardar varios minutos: lanzarlo en segundo plano.

## Remodelación vanilla del Nether (arte, 2026-11-11)

- `python tools/nether_remodel_gen.py`: geometría original en `nether_remodel_rigs.py`; genera **solo** `entity/client/NetherRemodelLayers.java`, seis texturas base y seis máscaras `_glow` de 128×128 en `textures/entity/nether_remodel/`. No registra capas ni cambia renderizadores, lógica, datos o modelos anteriores. No requiere imágenes externas.
- Conserva los huesos/pivotes vanilla de PiglinModel (incluidas orejas y raíces vacías de PlayerModel), SkeletonModel, GhastModel y BlazeModel. Se mantiene el agarre, las capas de armadura, el escalado de bebés y las animaciones vanilla. La estrechez de botas evita que se solapen cuando las piernas están juntas. Los accesorios son hijos de los huesos; no necesitan subclases ni animaciones nuevas.
- El generador empaqueta UV por caja/material con márgenes; comprueba raíces, jerarquía, tamaño del atlas y caras coplanares en reposo y marcha. Exporta geometría idéntica a Java a `build/nether_remodel/*.json` y el informe `rig_report.json`.
- `python tools/preview_nether_remodel.py`: vistas frontales, tres cuartos y articulación/ángulo opuesto, con profundidad real. Resultado conjunto: `build/nether_remodel/comparativa.png`; 18 PNG individuales. En los humanoides la tercera vista comprueba la marcha, sin modificar el modelo de producción.
- `cmd /c ".\gradlew.bat auditMobModels"`: auditoría ampliada para construir las seis capas con las **clases vanilla reales**, comparar sus pivotes y giros contra las capas nativas y verificar dimensiones/transparencia de los doce PNG. No abre el juego. `MobModelAudit <raíz> --nether-only` permite ejecutar solo este contrato desde Java.
- Integración de Claude: registrar/bakear cada capa y usar PiglinModel, GhastModel, BlazeModel o SkeletonModel con ella; sustituir modelo y textura únicamente en el Nether. Usar `entityCutoutNoCull` para la base y `eyes` para la máscara emisiva, renderizada con el **mismo modelo y pose**. Fuera del Nether conservar modelos y texturas vanilla. El ghast debe usar la base nueva también cuando carga el disparo; se puede intensificar su máscara en ese estado. Para blaze, intensificarla al arder; humanos y esqueleto pueden dejar la brasa fija. Esqueleto wither: conservar escala 1,2; ghast: conservar transformación vanilla. Nada de esto lo registra el generador.

## Taladro de Combustión (arte, tier 3)

- `python tools/combustion_drill_gen.py`: crea `combustion_drill.json`, `combustion_drill_spin.json`, atlas compartido `textures/item/gun/combustion_drill.png` de 128×128, y componentes planos de 16×16 `drill_head` / `combustion_engine` (texturas y modelos `item/generated`). Geometría propia con materiales de `gunkit`; no regenera ni modifica el arsenal existente.
- La broca apunta hacia −Z, alrededor del eje `(8,12,0)`. Predicado acordado `tecnocinetico:drill_spin`: 0 en reposo y alternancia 0/1 cada dos ticks al trabajar. El segundo JSON se deriva del primero conservando exactamente cuerpo, UV y transforms; solo las 62 piezas de la broca reciben rotación Z de 45°. 79 piezas fijas; sin renderer especial obligatorio. Inventario 3D ajustado con `gui_fit.py` y encuadre idéntico entre fotogramas.
- `python tools/preview_combustion_drill.py`: vistas con profundidad real en `build/combustion_drill/`: `comparativa.png`, `three_quarter_0/1.png`, `firstperson_0/1.png`, `firstperson_left.png`, `inventory.png`, `bit_front_0/1.png`, `other_side_0/1.png`, `components.png` y `spin.gif` (100 ms por fotograma). La primera persona aplica el transform de mano vanilla en reposo y el `display` del JSON, con FOV 70; es una referencia de encuadre sin balanceo/equipamiento/extensiones Java.
- Validación del arte: `python tools/check_models.py` y `python tools/check_zfight.py combustion_drill combustion_drill_spin`. El generador también comprueba igualdad del cuerpo/UV/encuadre, límites de los vértices girados y escribe `build/combustion_drill/contract.json`. Claude registra herramienta, predicado, combustible y minería; el generador no toca Java, datos, recetas ni lang.

## Munición en paquetes y cargadores de rifle (arte)

- `python tools/ammo_packages_gen.py`: genera exclusivamente los diez modelos `kinetic_round`, `guided_round`, `rifle_round`, `shotgun_shell`, `rifle_belt`, `stripper_clip`, `short_rifle_magazine`, `rifle_magazine`, `belt_box` y `minigun_box`. Cada uno tiene atlas propio en `textures/item/gun/<id>.png`, potencia de dos y ≤128 px. Reutiliza los pintores de materiales de `gunkit`, sin modificar sus fuentes ni los cargadores de pistola o `ammo.png`.
- **Orden de regeneración:** ejecutar este generador **después** de `ammo_gen.py` y `weapons_gen.py` si se usan los generadores antiguos: todavía escriben las versiones individuales anteriores de las cuatro municiones. El script nuevo funciona solo y no requiere regenerar armas, pistola o gadgets. Los generadores antiguos se han conservado intactos.
- Paquetes abiertos de 6 cinéticas/guiadas, montón suelto de 5 balas de rifle, grupo de 6 cartuchos, cinta articulada de 15 y peine de 5. Cargador recto corto, cargador largo curvo y cajas con cinta saliente para 60/180. La caja industrial lleva refuerzos y una caída de cinta más larga. Los números de las cajas usan islas UV reservadas para conservar legibilidad. Los modelos de cargadores son estáticos: sin predicados ni variantes de carga; el código muestra la cantidad mediante la barra del objeto.
- `gui_fit.py "y-135;x30"` encuadra los diez iconos 3D. Se incluyen transforms para ambas manos, suelo y marco. El generador comprueba cantidades visibles de las seis primeras geometrías y límites antes/después de las rotaciones; escribe `build/municion/manifest.json` con cantidades y tamaños.
- `python tools/preview_ammo_packages.py`: vistas con textura y profundidad real. `build/municion/comparativa.png` reúne inventario y mano de los diez; `comparativa_gui.png` y `comparativa_mano.png` separan ambas vistas. También exporta PNG individuales de inventario, tres cuartos y ambas manos, y `rifle_cinco_vs_cinta_quince.png` a igual escala física. La mano es la pose vanilla en reposo a FOV 70, sin balanceo ni extensiones Java.
- Validar con `python tools/check_models.py` y `python tools/check_zfight.py kinetic_round guided_round rifle_round shotgun_shell rifle_belt stripper_clip short_rifle_magazine rifle_magazine belt_box minigun_box`. El arte no modifica registros, recetas, traducciones ni capacidades reales.

## drill_sounds.py (sonidos del Taladro de Combustión)
- `python tools/drill_sounds.py src/main/resources/assets/tecnocinetico/sounds`: genera `sounds/drill/` (bucles de ralentí y taladrado, arranque, mordisco x3, repostaje, tapón y ahogo). Mezcla grabaciones CC0 de `tools/sound_sources/workshop_*.wav` (ver `CREDITOS_SONIDOS.md`) con un motor de dos tiempos sintetizado. Los bucles se cierran con fundido cruzado.
