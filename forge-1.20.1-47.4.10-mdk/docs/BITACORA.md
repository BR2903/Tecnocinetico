# Bitácora de trabajo

Añade una entrada al final cada vez que termines una tarea: fecha, quién (Claude/Codex), qué se hizo, cómo se verificó y qué queda pendiente.

---

## 2026-11-06 — Claude — Revisión inicial y correcciones
- Revisión completa del mod original (MDK con prensa manual, prensa automática, pistola, minerales).
- Corregido: el martillo no tenía receta (bloqueaba toda la progresión), la pistola atravesaba paredes, `en_us.json` estaba en español, la pestaña creativa ignoraba la traducción, acentos de `gradle.properties` (ahora `é`), prensa automática sin hueco de salida (ahora caras de entrada/salida para tolvas), recetas duplicadas (ahora `PressRecipes`), rotación de bloques en estructuras.
- Texturas enormes/no potencia de 2 reescaladas a 128 px (originales en `../texturas_originales_backup/`). El jar pasó de ~20 MB a ~0,4 MB.

## 2026-11-06 — Claude — Cuerpo de Enjambre
- Mob biomecánico del lore: modelo en código con animaciones, salto cinético, capa luminosa, huevo, botín, aparición en el Overworld.

## 2026-11-06 — Claude — Martillo y pistola
- Martillo = pico de hierro al 70 %, modelo 3D nuevo (le gustó al usuario).
- Pistola: cargador de 6, recarga con tecla R (paquete de red), barra de munición.
- Revisión independiente: corregidos disparos sin daño (invulnerabilidad), recarga con la pistola fuera del inventario, dos pistolas.

## 2026-11-06 — Claude — Pistola realista, animaciones y habilidades
- El usuario pidió estilo realista: pistola rehecha por piezas (`tools/pistol_gen.py`), textura con sombreado.
- Animación de recarga (fotogramas por overrides + transformación de mano) y de retroceso; postura de apuntar en 3ª persona.
- Sistema de habilidades con Manejo de Armas (menos retroceso, recarga más rápida).

## 2026-11-06 — Claude — Inventario, partículas, detalles del mod
- Inventario con el modelo 3D (el usuario rechazó iconos planos). Se probaron 5 vistas en el juego con objetos temporales; eligió **inclinada**.
- Partículas propias (haz cian, onda expansiva, chispas) + fragmento de cinetrita que se rompe + polvo del bloque.
- Detalles del mod en la pistola según su receta (compensador, acoplador, cachas de sustrato, glifo, luces, placa de inercita).
- Disparo recto sin ayuda en el arma base (vanilla añadía 0,3 de margen).

## 2026-11-06 — Claude — Módulos, JEI y Banco de Armas
- 4 módulos con efecto y aspecto visible en el arma (80 modelos generados).
- JEI añadido al entorno de desarrollo con plugin propio (categoría de la prensa).
- Instalación de módulos movida de la mesa de crafteo a un bloque nuevo: **Banco de Armas Cinético** (interfaz con esquema del arma, montar/desmontar). Revisado contra duplicaciones; corregida pérdida del arma al morir/desconectarse con el menú abierto.

## 2026-11-06 — Claude — Create + electricidad
- Decisión del usuario: complemento de **Create** con electricidad de **Create Crafts & Additions**.
- Entorno de desarrollo con Create 6.0.8, Ponder, Flywheel, Registrate, MixinExtras y Crafts & Additions 1.3.3.
- Prensa Automática pasa a consumir Forge Energy (20 000 FE, 40 FE/tick).
- **Fuente de Energía Creativa** (solo pruebas, sin receta ni drops, irrompible, oculta en JEI).

## 2026-11-06 — Claude — Interfaz de la prensa automática
- Interfaz con ranuras, flecha de progreso, barra de energía (tooltip en FE) y estado; luz de estado en el bloque (blockstate `powered`, brilla en la oscuridad).

## 2026-11-07 — Claude — Más habilidades
- Ingeniería (prensa manual: menos golpes, ahorro de martillo) y Minería Cinética (velocidad del martillo).
- Pantalla de habilidades con la tecla K.
- Minerales del mod añadidos a la etiqueta `forge:ores`.

## 2026-11-07 — Claude — Documentación para traspaso
- `AGENTS.md` + `docs/` (diseño, arquitectura, herramientas, bitácora, pendientes).
- Scripts generadores copiados de la carpeta temporal a `tools/` y verificados: reproducen exactamente las texturas actuales. `tools_gen.py` convertido en `hammer_gen.py` (el original también regeneraba la pistola antigua).
- **Estado**: compila, `runGameTestServer` y `runClient` cargan sin errores. El usuario aún no ha probado a fondo la Prensa Automática con un alternador real de Crafts & Additions.

## 2026-11-07 — Claude — Git
- Repositorio creado y subido a https://github.com/BR2903/Tecnocinetico (rama `main`). Sección "Git" añadida a `AGENTS.md` con el flujo de trabajo entre agentes.

## 2026-11-07 — Claude — IntelliJ
- Configuraciones de ejecución de IntelliJ generadas (`genIntellijRuns`). El usuario ejecuta `runClient` desde IntelliJ; los agentes no deben abrir el juego.

## 2026-11-07 — Claude — Pantalla de habilidades
- Textos que se salían del panel: panel más ancho (280 px), ajuste automático de línea (`font.split`) y altura de cada fila según su contenido; el texto de XP va a la derecha de la barra.

## 2026-11-07 — Claude — Retroceso realista de la pistola
- El arma "bajaba" al disparar: era la animación vanilla de re-equipar (`itemUsed`); se ignora `equipProcess` durante el retroceso.
- Nuevo retroceso: golpe hacia atrás + subida de la boca + giro lateral aleatorio, con curva de muelle amortiguado (`PistolClientExtensions.recoilCurve`).
- La corredera retrocede 2 ticks tras cada disparo (la propiedad `tecnocinetico:reload` devuelve 0.75 → fotograma "corredera atrás").

## 2026-11-07 — Claude — Controles de la pistola
- Clic izquierdo dispara (semiautomática, `FirePistolPacket` → `KineticPistolItem.tryFire`); con el cargador vacío recarga sola.
- Clic derecho mantenido apunta: arma al centro (`AIM_X/Y/Z` en `PistolClientExtensions`, valores aproximados: **ajustar si las miras no quedan centradas**), zoom, sin punto de mira, retroceso ×0,6.
- La pistola ya no rompe bloques ni golpea (`canAttackBlock` false + cancelación del ataque en el cliente).

## 2026-11-07 — Claude — Disparo apuntando + sonidos del Cuerpo de Enjambre
- Al apuntar, el disparo salía de la cadera: `FirePistolPacket` lleva ahora `aiming` y el servidor centra la boca del cañón.
- Sonidos propios sintetizados (`tools/sound_gen.py`) para el Cuerpo de Enjambre: ambiente (gruñido + chasquidos + zumbido del núcleo + servo), daño (metal + chillido), muerte (el núcleo se apaga), pasos, salto. Sustituyen a los de araña/gólem. El usuario debe escucharlos y decir si ajustar.

## 2026-11-07 — Claude — Mira holográfica funcional
- La Mira de Puntería Cinética era una caja maciza que tapaba la vista: ahora es un marco hueco + cristal translúcido + punto de retícula luminoso (`pistol_gen.py`: elementos `optic_*`, modelos con `render_type: translucent`).
- Al apuntar con la mira, el arma se alinea con el centro de la ventana (`AIM_Y_OPTIC`, `AIM_Z_OPTIC` en `PistolClientExtensions`; **valores estimados, ajustar si el punto no queda centrado**).
- Nota: `render_model.py` no dibuja transparencias (el cristal sale opaco en la vista previa, en el juego es translúcido).

## 2026-11-07 — Claude — Sonidos de la pistola
- Sonidos propios sintetizados (`tools/sound_gen.py`, carpeta `sounds/pistol/`): disparo ×3 (chasquido + golpe + zumbido cinético + corredera + eco), impacto con tintineo de cristal (solo si da en algo), acierto, clic en vacío, cargador fuera/dentro y corredera. Sustituyen a los vanilla (rayo, trampilla, ballesta).

## 2026-11-07 — Claude — Sonidos realistas (menos "arcade")
- El usuario encontró los sonidos muy arcade. `tools/sound_gen.py` reescrito: nada de tonos puros/barridos; todo con ruido + resonadores (modos metálicos), texturas granulares, servos con engranajes, siseos neumáticos, voces de máquina por tubo resonante (filtro peine), eco de exterior y reverberación sintética. Estilo de máquinas orgánico-mecánicas (inspiración Horizon Zero Dawn, sin copiar sonidos).
- Disparo: estallido saturado + pólvora + retumbe grave + mecanismo + eco; brillo medio ~1,8 kHz (antes ~6 kHz). Se recorta el silencio final de cada archivo.

## 2026-11-07 — Claude — Disparo sin efecto "pelota"
- El disparo sonaba a pelota que rebota: lo causaban los ecos discretos (`slap_echo`) y un retumbe grave largo. Nuevo `gunshot`: onda de choque de ~1,5 ms + fogonazo saturado + cuerpo corto + cola de reverberación continua; ~90% de la energía en los primeros 30 ms.
- Para máximo realismo se propuso al usuario usar grabaciones reales CC0 en lugar de síntesis.

## 2026-11-07 — Claude — Disparo más potente y progresión lenta
- Disparo con más pegada (sonaba a pistola de balines): fogonazo más grueso, "puñetazo" de 100-300 Hz y compresión fuerte (drive 2.6).
- Progresión **mucho más lenta** (petición del usuario: el mod debe ser difícil): curva 100n + 40n(n−1) (nivel 10 = 4.600 XP, antes 1.300) para todas las habilidades; pistola +1 por acierto y +4 por baja, **solo mobs hostiles** (`Enemy`).

## 2026-11-07 — Claude — Sonidos de la pistola con grabaciones reales
- El usuario pidió usar sonidos de Techguns: revisada su licencia (`LICENSE.txt` del repo): solo permite copiar fragmentos de código, **no** reutilizar recursos → descartado.
- Se usan grabaciones reales de OpenGameArt: disparos CZ-52 (Vincent Sevedge, **CC-BY 3.0** según el archivo de licencia del paquete), recarga (zer0_sol, CC0), clic (BMacZero, CC0). Originales en `tools/sound_sources/`, procesado con `tools/import_sounds.py`, créditos en `CREDITOS_SONIDOS.md` y `mods.toml`.
- `sound_gen.py` ya no genera disparo/recarga (solo impacto y acierto) para no pisar las grabaciones.

## 2026-11-07 — Claude — Plan del Overworld
- Plan completo en `docs/PLAN_MUNDO.md` (decisiones del usuario: reemplazar todo el Overworld, tono "reclamado por la naturaleza", sustituir aldeas/estructuras vanilla). Pendiente: empezar por la fase 1 (bloques).

## 2026-11-07 — Claude — Fase 1 del mundo: bloques
- 26 bloques + objeto Chatarra (`ModWorldBlocks`, pestaña creativa "Tecnocinético: Mundo"): hormigón/agrietado/musgoso con losa-escalera-muro, escombros (cae) y capa de escombros, asfalto/agrietado/con línea orientable, chapa y viga oxidadas, rejilla oxidada (transparente), montón de chatarra (suelta Chatarra → se funde en pepitas de hierro), bloque y capas de ceniza (no se derriten), arena y vidrio vitrificados, racimo de cinetrita (luz 6, suelta fragmentos), tronco y tablones muertos.
- Todo lo de recursos lo genera `tools/world_blocks_gen.py`. Validado: `runGameTestServer` sin errores y todas las referencias de modelos/texturas existen. Pendiente: que el usuario los vea en el juego.

## 2026-11-07 — Claude — Texturas del mundo rehechas
- Al usuario solo le gustaron los tablones: el resto (ruido píxel a píxel) se veía sucio. Reescritas en `world_blocks_gen.py` con reglas de pixel art vanilla: rampas de 4-6 tonos, grupos de píxeles, bisel de luz arriba-izquierda, formas reconocibles (paneles de encofrado con tensores, adoquines rotos, chapa ondulada, viga en I, anillos cuadrados, racimo dibujado a mano, costra vitrificada cuarteada). Las texturas vanilla se usaron solo como referencia visual (no se copian).

## 2026-11-07 — Claude — Texturas del mundo v3 (referencia: Create)
- Segunda revisión pedida por el usuario ("toma referencia de otros mods sin copiar"): se estudiaron texturas de Create (solo como referencia visual) y se rediseñaron con su lenguaje: marcos con contorno y bisel, tornillos, formas limpias, poco ruido, acentos intencionados. Hormigón y tablones se mantienen (le gustaron).

## 2026-11-07 — Claude — Hormigón agrietado con fractura real y variantes
- La grieta era demasiado uniforme: ahora fractura por desplazamiento del punto medio (borde a borde, ancha en el centro), ramas secundarias y desconchón con árido. **3 variantes** elegidas al azar por el blockstate (como la piedra vanilla), para que un muro no repita el dibujo.

## 2026-11-07 — Claude — Fase 2 del mundo: biomas y sustitución del Overworld
- 19 biomas del mod (`worldgen/ModBiomes`) que sustituyen a todos los vanilla del Overworld en los tipos de mundo Normal, Biomas grandes y Amplificado (presets sobrescritos). Relieve vanilla intacto; cada bioma cambia ambiente (cielo, niebla, agua, hierba, ceniza en el aire) y suelo (`ModSurfaceRules`).
- Técnica: fuente de biomas `tecnocinetico:remapped` (envuelve la multi-ruido vanilla) + datagen (`gradlew runData`) para biomas, ajustes de ruido y etiquetas. Estructuras vanilla desactivadas salvo minas abandonadas y fortalezas.
- Validado: compila, `runData` y `runGameTestServer` sin errores. **Solo funciona en mundos nuevos.** Pendiente: que el usuario lo pruebe en el juego.

