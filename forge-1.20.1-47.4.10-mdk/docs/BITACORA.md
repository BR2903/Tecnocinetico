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

## 2026-11-07 — Claude — Escombros y chatarra rehechos; textos en todas las variantes de español
- Al usuario no le gustaban nada `rubble` ni `scrap_pile` al verlos en el mundo. Escombros: grava de trozos de hormigón (algo de ladrillo y asfalto) con juntas finas, sin la varilla diagonal que marcaba el patrón. Montón de chatarra: fardo de chapa prensada en capas (óxido, acero, pintura vieja).
- El juego del usuario está en **es_ec**: los textos salían en inglés porque solo existía `es_es`. Nuevo `tools/sync_lang.py` copia `es_es` a es_ar/es_cl/es_ec/es_mx/es_uy/es_ve.

## 2026-11-07 — Claude — Escombros y chatarra como montones (no bloques completos)
- El usuario: "al hacerlo bloque pierde esa característica de parecer chatarra o escombro". `rubble` y `scrap_pile` ahora son `PileBlock`: montones 3D de 1-4 trozos (crecen al colocar más, como los pepinos de mar), necesitan suelo firme, pueden estar bajo el agua, sueltan tantos objetos como trozos. Modelos con trozos rotados (hormigón, ladrillo, varilla, asfalto / chapa oxidada, galvanizada, pintada, tubo, cobre) generados por `world_blocks_gen.py` (`PILE_RUBBLE`, `PILE_SCRAP`); textura = atlas de 4 zonas.
- "Capa de Escombros" → **Pedazos de Escombro**: trocitos sueltos sobre el suelo (sigue siendo alfombra).
- Ya no forman parte del suelo (reglas de superficie → grava/roca). Aparecen esparcidos como features (inicio de la fase 3): nuevo `tools/world_features_gen.py` (random_patch + biome_modifier por bioma; también en el lecho de mares y río).

## 2026-11-07 — Claude — Fase 3: detalles del terreno
- Features con código propio (`worldgen/feature/`, registradas en `ModWorldgen.FEATURES`): `crater` (cuenco con fondo y borde configurables; no se genera junto al agua), `ruin_wall` (muro roto recto o en L, vigas oxidadas, escombros al pie, enredaderas opcionales), `dead_tree` (tronco seco con ramas desnudas).
- JSON por `tools/world_features_gen.py`: cráteres vitrificados/de ceniza/chamuscados, muros de hormigón/invadidos/helados, árboles muertos, capas de ceniza, cristales de cinetrita en suelo y techo de las Cuevas de Cinetrita; un biome_modifier por bioma y paso de generación.
- Validado con `runGameTestServer` (los JSON se leen bien). La generación real la prueba el usuario en un mundo nuevo.

## 2026-11-07 — Claude — Comando /tpbioma
- `/tpbioma <bioma>` (operador): busca el bioma más cercano (hasta 6400 bloques) y teletransporta a la superficie, o a un hueco de cueva en los biomas subterráneos. Sin namespace se entiende `tecnocinetico:`; autocompleta los biomas del mod. Código: `command/TpBiomeCommand`.

## 2026-11-07 — Claude — Fase 4: edificios en ruinas + GameTest de generación
- Estructura por código (sin NBT) `ruined_building` (`worldgen/structure/`): planta rectangular, 1-6 pisos de 4 bloques, derrumbe diagonal desde una esquina intacta, ventanas rotas (alguna rejilla), forjados con agujero de derrumbe, vigas oxidadas, escombros/pedazos dentro, cofre (`chests/ruined_building`, botín escaso: chatarra, pepitas, cobre; raro: piezas y módulos). Estilos urban/overgrown (musgo, enredaderas)/frozen (nieve).
- 4 variantes (`tools/world_structures_gen.py`): Megaciudad (2-6 pisos, muy densa), urbana (Praderas, Tierras Fracturadas, Taiga de Ceniza), invadida (Ruinas Invadidas, Selva Industrial), helada (Tundra).
- **Primer GameTest** (`gametest/WorldgenGameTests`): genera de verdad las 4 variantes en el servidor de prueba (que usa el Overworld del mod) y vuelca sus bloques a `run/gametest_dump/`; `tools/render_dump.py` los dibuja en isométrico para revisar el aspecto sin abrir el juego. Ajustado el derrumbe tras verlo (era demasiado destructivo en edificios bajos).
- Importante para quien siga: en `postProcess` los aleatorios NO pueden depender del mundo (cada chunk dibuja su parte del mismo edificio); por eso `RuinedBuildingPiece` usa aleatorios por posición.

## 2026-11-07 — Claude — Asentamiento de Supervivientes
- Estructura por código `settlement` (`SettlementStructure` + `SettlementPiece`), sustituye a las aldeas. Recinto de 33x33: muralla (chapa, tablones muertos, hormigón) con postes cada 4 y puerta con faroles; torre de vigía en una esquina (escalera, plataforma, barrotes, farol); plaza con fogata y faroles; 5-7 parcelas: casas (cama, cofre, barril), taller (mesa, horno, yunque, cofre), almacén (barriles), huertos (trigo/patata/zanahoria/remolacha con canal de agua).
- Se adapta al terreno: `findGenerationPoint` guarda la altura de cada columna (desnivel ≤ 10, sin agua) y cada choza tiene plataforma con cimiento. Paso `top_layer_modification` para construirse después de los árboles y despejar los de dentro.
- Botín: `settlement_home` (comida, antorchas), `settlement_workshop` (metales, chatarra; raro: acoplador, transductor, módulos), `settlement_storage` (cosechas, troncos). Biomas: Praderas de Chatarra, Ruinas Invadidas, Taiga de Ceniza, Tundra. Los edificios en ruinas tienen zona de exclusión de 3 chunks alrededor.
- Aún sin habitantes: el NPC "Superviviente" con comercio es la siguiente tarea propuesta.

## 2026-11-07 — Claude — NPC Superviviente
- Decisión (con el usuario): crearlo desde cero en vez de depender de un mod de NPC (licencias de recursos, dependencia extra, identidad), reutilizando el comercio vanilla: `SurvivorEntity extends AbstractVillager` (interfaz de aldeano, `MerchantOffer`).
- 3 oficios (`Role`, sincronizado): Chatarrero, Armero, Granjero; ofertas en `SurvivorTrades` con **Chatarra como moneda** y precios altos (módulos = mucha chatarra + lingotes de cinetrita/inercita). Existencias se reponen cada día. Nombre por oficio (`getTypeName`).
- IA: pasea en un radio de 16 alrededor del centro del asentamiento (guardado en NBT), abre puertas, huye de los monstruos, no desaparece. Sonidos humanos (no de aldeano). Herramienta en mano según oficio.
- Aspecto: modelo de jugador con capas (`SurvivorRenderer`), pieles 64x64 generadas por `tools/survivor_skin_gen.py` (capucha+gafas cian / delantal+célula de cinetrita / pañuelo+poncho).
- Aparición: `SettlementPiece` crea uno por choza (armero en el taller, chatarrero en el almacén, granjero junto al huerto; casas: chatarrero o granjero). Huevo de aparición en la pestaña creativa. El GameTest comprueba que el asentamiento genera supervivientes con ofertas.
