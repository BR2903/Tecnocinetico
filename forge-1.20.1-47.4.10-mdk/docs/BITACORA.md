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

## 2026-11-07 — Claude — Fábrica abandonada
- Estructura por código `abandoned_factory` (`FactoryStructure` + `FactoryPiece`): nave de 23-31 x 15-21 y 9-11 de alto; pilares de viga, muro de hormigón abajo y chapa arriba con franja de ventanas rotas, portón de carga, techo con agujeros y claraboyas, esquina derrumbada con escombros; cinta transportadora, prensas, hornos con chimenea, tanques de cobre envejecido; pasarela de rejilla con escalera y oficina; grúa puente con cadena; 1-3 Cuerpos de Enjambre de guardia (persistentes). Variante invadida (musgo, enredaderas) en la Selva Industrial.
- Biomas: Selva Industrial, Praderas de Chatarra, Cicatrices de Extracción. Excluida a 3 chunks de los asentamientos.
- Botín con piezas de **Create sin depender de Create**: entradas `minecraft:tag` sobre etiquetas propias (`tecnocinetico:loot/create_common|create_rare|create_precious`) cuyas entradas son opcionales (`"required": false`); si Create/Crafts & Additions no están, la etiqueta queda vacía y no hay errores.
- `render_dump.py --maxy=N` corta el techo para ver el interior.

## 2026-11-07 — Claude — Enjambre de avispas inteligentes
- Decisiones del usuario: avispas **biomecánicas**, **colmena con Nodo** (reina), **territoriales**.
- `entity/swarm/`: `SwarmWaspEntity` (papeles `WaspRole`: exploradora, aguijón, escolta), `HiveNodeEntity` (Nodo: flota, cura a las heridas a su lado, crea avispas con presupuesto de 24, pulso cinético cuerpo a cuerpo; si muere, el enjambre queda huérfano y desorganizado) y `HiveMind` (estados PATROL → ALERT → ATTACK, ataque por turnos desde ángulos repartidos, refuerzos, memoria de adaptación guardada: si les hacen daño de lejos se abren y atacan más a la vez; si es cuerpo a cuerpo atacan cuando el jugador mira a otro lado).
- Las avispas NO usan búsqueda de rutas: vuelan con steering (llegada, separación entre ellas, subir ante obstáculos). Esquivan de lado si el jugador las apunta y rodean por la espalda si se cubre con escudo. Huyen a curarse con poca vida. La pistola hace daño de "ataque de jugador": la mente distingue distancia/cuerpo a cuerpo por la distancia del atacante (> 5 bloques = a distancia).
- Aspecto: `SwarmWaspModel` (tórax, cabeza con ojos compuestos, abdomen segmentado con aguijón, 4 alas translúcidas que baten, 6 patas), texturas por papel + capa luminosa (`tools/swarm_wasp_texture.py`); el Nodo usa el mismo modelo x2.6 con textura de reina. Sonidos sintetizados (`tools/sound_gen.py` → `generate_wasps`): zumbido de aleteo, alarma, picadura, daño, muerte; latido y pulso del Nodo.
- Colmena (`SwarmHiveStructure/Piece`): cúpula de **Panal de Enjambre** (bloque nuevo) medio enterrada, 3 túneles, óculo, cristales de cinetrita, cofre, Nodo + 8 avispas. Biomas: Ruinas Invadidas, Selva Industrial, Praderas de Chatarra, Taiga de Ceniza, Tierras Fracturadas.
- Botín: el Nodo suelta **Núcleo de Enjambre** (objeto nuevo, para la futura mejora de puntería asistida) + cinetrita.
- GameTest `swarmAttacksTarget`: Nodo + 4 aguijones matan a un cerdo objetivo (el último daño debe venir de una avispa). Al escribirlo salió un falso positivo (estaban enterradas y el cerdo moría asfixiado): ver trampas en AGENTS.md.

## 2026-11-07 — Claude — Trazo del disparo recto al acertar
- El trazo se torcía hacia abajo al dar a mobs pequeños/cercanos: `EntityHitResult#getLocation()` de `ProjectileUtil.getEntityHitResult(level, ...)` devuelve los **pies** del mob. Nuevo `KineticPistolItem.impactPoint`: punto donde el rayo recto entra en la caja del mob; solo si acertó gracias a la puntería asistida (Mira) se dibuja hacia el punto de la caja más cercano al rayo.

## 2026-11-07 — Claude — Módulo Guía de Enjambre (trayectoria automática)
- Petición del usuario: la puntería asistida debe ser una mejora aparte y **extremadamente difícil** de fabricar.
- La **Mira** ya no corrige la trayectoria (solo zoom y retícula; texto actualizado). Nuevo módulo `SEEKER` (bit 16) **Guía de Enjambre**: margen de búsqueda 1.1 bloques alrededor del rayo; el trazo se curva hacia el mob solo cuando acierta gracias a él.
- Receta (mesa de crafteo): 3 **Núcleos de Enjambre** (= 3 Nodos de Colmena derrotados) + 2 Sustratos Lógicos + 2 lingotes de Inercita + Transductor Piezo + **consume una Mira**. Ningún superviviente lo vende ni aparece en botín.
- Visual: sensor bajo el armazón delantero (riel, cuerpo de cobre, lente cian brillante, aletas). `pistol_gen.py` ahora genera 32 combinaciones (160 modelos, 191 overrides). Quinta ranura en el Banco de Armas (`MODULE_POS` + `bench_gui_gen.py`).

## 2026-11-07 — Claude — Munición Guiada
- Objeto `guided_round` (4 fragmentos de cinetrita + redstone + cobre → 2). **Solo funciona con la Guía de Enjambre**: sin el módulo la recarga no la carga (y avisa si es la única munición). Con el módulo se carga primero (`GuidedAmmo` en el NBT de la pistola) y se dispara antes que la normal.
- Bala guiada: busca al mob **hostil** (`Enemy`) más alineado con la mira en un cono de 25° con línea de visión (nunca supervivientes/animales); trazo curvo (Bézier). Sin enemigos en el cono va recta. Barra de munición violeta mientras quedan guiadas; tooltip con el recuento.

## 2026-11-07 — Claude — Cargadores y Bala Cinética
- Petición del usuario: los fragmentos de cinetrita **ya no son munición**. Nueva **Bala Cinética** (4 fragmentos + cobre → 4). La pistola necesita **cargador**: `MagazineItem` (Cargador 6; Cargador Ampliado 10, solo con el módulo Cargador Ampliado). NBT del cargador: `Rounds`, `Guided` (las guiadas siempre se disparan primero).
- Llenar: receta especial de la mesa `MagazineLoadingRecipe` (`tecnocinetico:magazine_loading`: cargador + 1-8 balas, sin resultado si no caben) o **R con el cargador en la mano** (Mayús+R = guiadas; `FillMagazinePacket`).
- Recarga = cambio de cargador: toma el cargador válido con más balas, lo guarda "en espera" (`PendingMag`) y al terminar la animación lo pone; el viejo vuelve al inventario con sus balas. Pistola: `Mag` (0/1/2), `Ammo`, `GuidedAmmo`; las pistolas antiguas sin `Mag` se migran solas. En creativo, recarga gratis.
- Banco: si al cambiar módulos el cargador deja de valer (Ampliado sin módulo, guiadas sin Guía), sale entero al inventario.
- Munición Guiada ahora: 2 Balas Cinéticas + redstone + fragmento → 2. Armero vende balas y cargadores.
- Modelos 3D (`tools/ammo_gen.py`, atlas `item/ammo.png`): balas (normal de cobre, guiada de acero con banda roja, aletas y sensor violeta) y cargadores con ventana y la bala de arriba visible cuando están cargados (override `tecnocinetico:loaded`).
- GameTests nuevos (`AmmoGameTests`): receta de carga, llenar con R, recarga y cambio de cargador, Ampliado solo con módulo.

## 2026-11-07 — Claude — Torreta Centinela e Instalación de la IA
- **Torreta Centinela** (`entity/custom/SentryTurretEntity`): anclada, gira la cabeza con velocidad limitada, carga ~1,2 s mostrando una línea láser roja y dispara hacia donde apuntaba al terminar la carga (esquivable). 34 de vida, 10 de armadura, 6 de daño. Modelo `SentryTurretModel` (base con franjas de peligro, cabeza con cañón doble, sensor que pasa de cian a rojo al cargar), texturas `tools/turret_texture.py`, sonido de carga `generate_turret()` en `sound_gen.py`. Ojo: se anula el `LookControl` vanilla (devolvería la cabeza al cuerpo) y el renderizador fija el cuerpo a 0.
- **Instalación de la IA** (`AiInstallationStructure/Piece`): búnker de hormigón con puerta de franjas de peligro y antena; escalera de 16 bloques; complejo de 3x3 salas de pizarra (seguridad con torreta y barricada, servidores, laboratorio con tanques de cinetrita, almacén, derrumbada) y la **cámara del Núcleo** en el centro (pilar de cristal tintado con cinetrita y luz, conductos, terminal, cofre `ai_core`, 2 torretas). Luces de techo con algunas rotas, 2 Cuerpos de Enjambre. Biomas: Cicatrices de Extracción, Tierras Fracturadas, Megaciudad Muerta (rara: espaciado 40).
- El render en planta del volcado del GameTest destapó pasillos tapiados (sus tapas caían dentro de las salas): ahora se construyen primero los pasillos, luego las salas y al final las puertas.

## 2026-11-07 — Claude — Equipo Táctico (pestaña extra del inventario) y gadgets
- Cargador normal: la receta da 4 (el usuario no quiere que sean caros).
- Petición: un apartado como el de Techguns (inspiración; diseño, código y recursos propios: su licencia no permite reutilizar nada). **Equipo Táctico** (`gear/TacticalGear`, capacidad de Forge en el jugador): 4 ranuras de munición, 3 de comida y 3 de gadgets (cara, espalda, pies). Se abre con la pestaña sobre el inventario (`client/GearTabs`) o la tecla **J**; pantalla `TacticalScreen` (personaje en el centro), menú `TacticalMenu`, textura `tools/tactical_gui_gen.py`.
- Munición: la pistola recarga primero desde estas ranuras y devuelve ahí el cargador gastado; los cargadores se llenan (R) también desde ellas (`TacticalGear.ammoSources`, `giveBackAmmo`).
- Comida: se come sola con hambre ≤ 14 sin desperdiciar (`GearEvents.autoEat`); no admite comida que devuelve recipiente.
- Gadgets (`gear/GadgetItem`, batería = durabilidad, no se rompen, recarga en yunque con fragmentos de cinetrita): **Visor de Visión Nocturna** (tecla **N**), **Jetpack Cinético** (mantener saltar en el aire; el empuje lo aplica el cliente en `GearClient`, el servidor gasta batería y anula la caída), **Propulsor Acuático** (Gracia del delfín en el agua). Modelos 3D en `tools/ammo_gen.py`; sonido del jetpack sintetizado.
- Al morir el equipo cae al suelo (salvo keepInventory). Sincronización servidor→cliente con `SyncGearPacket` (aplicado por `client/ClientGearHandler`).
- GameTests nuevos (`GearGameTests`): recarga desde las ranuras, devolución del cargador, comer solo, visor, filtros de ranura. Total: 7.

## 2026-11-07 — Claude — Gadgets visibles, recargas propias, Jetpack Mk I-III, más ranuras
- Ranuras del Equipo Táctico: munición 6, comida 9 (`TacticalGear`: 0-5, 6-14, 15 cara, 16 espalda, 17 pies). Los equipos guardados con el formato viejo de 10 ranuras se migran solos en `load`. Pantalla 176x218 (`tactical_gui_gen.py`).
- Cada gadget se recarga con su objeto (`gear/RechargeItem`): **Batería de Visor**, **Cartucho de Propelente** (jetpack), **Célula de Propulsión**. Clic derecho con él si llevas el gadget puesto, o en la mesa (receta especial `gadget_recharge`). Ya no se repara en el yunque.
- **Jetpack Mk I-III** (`gear/JetpackItem`, NBT `Level`): más batería (1200/2000/3200) y más empuje, subida máxima y avance por nivel. Mejora en la mesa: jetpack + **Kit de Mejora Mk II** (inercita, acoplador, bloques de redstone y cobre) → Mk II; + **Kit Mk III** (Núcleo de Enjambre, sustratos, inercita, transductor) → Mk III (receta especial `jetpack_upgrade`). El modelo del objeto cambia por nivel (override `tecnocinetico:level`).
- **Gadgets visibles en el jugador** (`client/GearLayer` + `WornGearModel`, textura `tools/worn_gear_texture.py`): visor en la cara, jetpack en la espalda (piezas extra en Mk II/III), propulsores en los tobillos; lentes y toberas brillan. Los demás jugadores los ven (`WornGadgetsPacket`, también al empezar a verte).
- Modelos 3D de las recargas, kits y jetpacks Mk II/III en `ammo_gen.py`. GameTests nuevos: migración, niveles, mejora, recarga en mesa y con clic derecho (9 en total).

## 2026-11-07 — Claude — Gadgets puestos: arreglo y rediseño
- **Fallo**: el jetpack no se veía y las botas parecían una hoja: `GearLayer` reutilizaba un VertexConsumer ya cerrado (al pedir el buffer de la capa luminosa, el anterior se cierra) y las piezas siguientes se dibujaban con la textura luminosa, casi transparente. Ahora se pide el buffer en cada pasada.
- Gafas rediseñadas como visión nocturna de verdad: soporte en la frente con brazo, cuerpo binocular, dos tubos que sobresalen con lentes verdes brillantes, correa alrededor y por encima de la cabeza, batería en la nuca con LED. Botas propulsoras: caña blindada con franja cian, puntera, suela, turbina en el talón con boca brillante y aletas.

## 2026-11-07 — Claude — Tier 1: armadura de chatarra, machete, escopeta y rifle
- Decisiones del usuario: armadura "chatarra de superviviente" con bono de set pequeño; armas cuerpo a cuerpo, escopeta de caño y rifle de cerrojo.
- **Armadura de Chatarra** (`item/armor/ScrapArmorItem`): protección 2/5/4/2 (13, algo menos que el hierro), durabilidad x14, se repara con Chatarra. Bono de set (4 piezas): -20 % de daño de proyectiles y explosiones (`LivingHurtEvent`). Texturas puestas e iconos: `tools/armor_gen.py` (casco con ranura de visión cian y respirador, peto de chapas con célula de cinetrita, grebas con rodilleras, botas de acero).
- **Machete de Chatarra** (`item/weapon/MacheteItem`): espada tier propio (6 de daño, 1.8 golpes/s, 320 usos), corta hojas, enredaderas y plantas deprisa.
- **Armas largas** (`item/weapon/LongGunItem`, una clase para las dos con `Stats`): carga cartucho a cartucho con R (desde el Equipo Táctico y el inventario), disparar interrumpe la recarga, varios perdigones con dispersión (menor apuntando). **Escopeta de Caño** (2 cartuchos, 8 perdigones de 2.5, 14 bloques) y **Rifle de Cerrojo** (5 balas, 16 de daño, 60 bloques, 24 ticks entre disparos, más zoom). Munición: Cartucho de Escopeta y Bala de Rifle (admitidas en las ranuras de munición). La entrada (`PistolInputHandler`), la red (disparo y R) y el zoom ahora aceptan cualquier arma; animación propia `client/LongGunClientExtensions`.
- Modelos 3D (`tools/weapons_gen.py`, atlas `item/weapons_t1.png`) con la empuñadura y las miras en las mismas coordenadas que la pistola: escopeta (abierta al recargar), rifle (cerrojo atrás al recargar), machete en diagonal como una espada, cartuchos y balas.
- Sonidos: de momento reutilizan los de la pistola con otro tono. Pendiente: grabaciones reales (CC0/CC-BY) de escopeta y rifle.
- GameTest nuevo: recarga cartucho a cartucho y bono de set (10 en total).

## 2026-11-07 — Claude — Rifle sin textura (cubo negro y rosa)
- El modelo del rifle tenía el brocal en z = -16.5 y Minecraft solo admite coordenadas de elemento en [-16, 32]: descarta el modelo entero. Cañón acortado 0.5. El servidor de prueba no carga modelos, así que no lo detectó: nuevo `tools/check_models.py` (límites, rotaciones, texturas y overrides) y regla en AGENTS.md.

## 2026-11-08 — Claude — Módulos para escopeta y rifle; jetpacks en creativo
- La pestaña creativa incluye el Jetpack Mk I, II y III.
- Decisión del usuario: la escopeta y el rifle se mejoran con **módulos en el Banco de Armas**, como la pistola. `WeaponModule` ahora tiene familia (PISTOL/SHOTGUN/RIFLE; los nuevos van al final para no cambiar los bits guardados), `appliesTo`, `conflictsWith` y `localMask` (máscara 0..7 de la familia, para el modelo).
- Escopeta: **Estrangulador** (-40 % dispersión), **Cañones Recortados** (+25 % daño, +50 % dispersión, -30 % alcance; no se combina con el estrangulador), **Cartuchera Rápida** (carga 40 % más rápida). Rifle: **Mira Telescópica** (zoom 0.22 + visor a pantalla completa con la textura del catalejo vanilla y sin dibujar el arma), **Cerrojo Pulido** (+35 % cadencia), **Cañón de Inercita para Rifle** (+6 daño, +20 alcance). Efectos en `LongGunItem` (`spread`, `damage`, `range`, `loadTicks`, `cooldown`, `zoom`).
- Banco: acepta cualquier arma con módulos; solo se ven las ranuras del arma puesta (`ModuleSlot.isActive`); la pantalla dibuja el modelo 3D del arma en grande y el marco de cada ranura activa (textura sin silueta fija, sprite del marco en (200,0)).
- Modelos (`weapons_gen.py`): 8 combinaciones x 2 fotogramas por arma (overrides `modules` + `reload`) y objetos de los 6 módulos. GameTest nuevo de módulos (11 en total).

## 2026-11-08 — Claude — Mira telescópica: pantalla negra
- La textura del visor (catalejo vanilla) es negra con un círculo transparente; sin `RenderSystem.enableBlend()` el centro también salía negro. Activada la mezcla al dibujarla.

## 2026-11-08 — Claude — Drones del Núcleo (IA)
- Elegido por el usuario: enemigo volador que patrulla, escanea con un haz y dispara; ronda instalaciones y antenas.
- `entity/custom/DroneEntity` (Monster, vuelo por "steering" sin rutas, como las avispas): patrulla en círculos alrededor de su punto de origen (`Home`, guardado en NBT) a 7-12 bloques sobre el suelo, barriendo el suelo con un haz de partículas cian. Detecta jugadores a <10 bloques o hasta 24 dentro de su cono de visión (con línea de visión), da la alarma y avisa a los drones en 32 bloques. En ataque orbita a 9 bloques por encima del objetivo, esquiva lateralmente cuando el jugador le apunta, carga 0,6 s con línea roja de aviso y dispara ráfagas de 3 tiros (dispersión 2,5°, daño 3) con 2,5 s de enfriamiento. Pierde al objetivo tras 10 s sin verlo. 16 de vida, 4 de armadura.
- Modelo `DroneModel` (casco con cúpula, sensor, 4 brazos en X con rotores que giran, cañón que apunta con el pitch, antena) y `DroneRenderer` (brillo cian en patrulla, rojo en ataque, parpadeo al cargar). Textura: `tools/drone_texture.py` (reutiliza `turret_texture.py`).
- Sonidos sintetizados (`sound_gen.py`, `generate_drone`): zumbido de rotores, barrido del escáner y alarma.
- Aparecen 2-3 drones rondando la antena del búnker de la Instalación de la IA. Huevo en la pestaña creativa. Botín: chatarra, a veces redstone/balas, raro transductor piezoeléctrico.
- GameTest `droneShootsTarget` (12 en total, todos pasan).

## 2026-11-08 — Claude — Torretas y drones: dos ataques y menos espera
- El usuario los vio demasiado fáciles (mucho tiempo muerto entre disparos). Ahora alternan dos ataques (código común en `entity/custom/CoreShots`):
  - **Ráfaga**: tiros rápidos cada 2 ticks con dispersión y poco daño, apuntados a donde estás. Dron: 6 x 2 (4°), 0,7 s de pausa después. Torreta: 8 x 2,5 (3,5°), 0,6 s de pausa; la cabeza gira a 7°/tick, así que corriendo de lado se puede dejar atrás.
  - **Disparo cargado** (35 % de las veces, o tras 2 ráfagas seguidas): línea roja que te sigue y **se fija** al final (dron 0,35 s, torreta 0,4 s antes de disparar); si sigues quieto te da fuerte (dron 10, torreta 14). Moverse en ese momento lo esquiva.
- El daño usa `mobAttack`, así que escala con la dificultad.

## 2026-11-08 — Claude — Corruptos: sustitutos de zombis, esqueletos y arañas
- Pedido del usuario: zombis, esqueletos y arañas no encajaban; versiones corruptas, unas con armas y otras sin.
- `entity/corrupted/CorruptionSpawns` (`MobSpawnEvent.FinalizeSpawn`): en el Overworld cancela la aparición de zombi/husk, esqueleto/stray y araña (natural, generadores de mazmorra, generación de chunks, refuerzos...) y crea el corrupto en el mismo sitio. Huevos y `/summon` siguen dando el vanilla.
- **Corrupto** (`CorruptedEntity extends Zombie`): 26 de vida, 4 de daño, 3 de armadura; no arde al sol, no se convierte en ahogado, nunca bebé. 22 % con Machete de Chatarra, 10 % con pala; a veces casco/peto de chatarra. Aspectos: errante, obrero, cristalizado.
- **Tirador Corrupto** (`CorruptedGunnerEntity extends AbstractSkeleton`, para que los generadores de mazmorra cuenten bien): IA propia `GunnerAttackGoal` (se acerca a su distancia, se mueve de lado). Pistola 55 % (ráfagas de 2 x 3), escopeta 30 % (6 perdigones x 2, de cerca), rifle 15 % (carga con línea roja que se fija al final, 11 de daño). El arma nunca se suelta; a veces suelta munición de su tipo. Aspectos: saqueador (máscara antigás) y soldado.
- **Araña Corrupta** (`CorruptedSpiderEntity extends Spider`): 22 de vida, más grande, hostil también de día, el mordisco ralentiza; sin jinete.
- Cliente: `CorruptedHumanModel` (modelo de jugador; brazos de zombi si va desarmado, apunta con el arma), `CorruptedHumanRenderer` (textura por variante + capa luminosa `_glow`, armadura, arma en mano), `CorruptedSpiderRenderer` (modelo vanilla x1,15).
- Texturas: `tools/corrupted_skin_gen.py` (reutiliza `survivor_skin_gen.py`) y `tools/corrupted_spider_texture.py`. Botín en `world_structures_gen.py`. Huevos en la pestaña creativa.
- GameTests `corruptedReplaceVanilla` y `gunnerShootsTarget` (14 en total, todos pasan).

## 2026-11-08 — Claude — Más corruptos (araña de cueva, ahogado, creeper) y arreglo del jetpack al recibir daño
- `CorruptionSpawns` sustituye también arañas de cueva, ahogados y creepers:
  - **Araña de Cueva Corrupta** (`extends CaveSpider`, para que los generadores de las minas la cuenten): veneno + lentitud, siempre hostil, 14 de vida. Mismo renderizador que la Araña Corrupta a escala 0,8 (`CorruptedSpiderRenderer(ctx, escala)`).
  - **Ahogado Corrupto** (`extends Drowned`): IA del ahogado, 24 de vida, nunca bebé; piel `drowned` (más pálida, algas, percebes) con el renderizador humano.
  - **Creeper Corrupto** (`extends Creeper`): mecha de 1,2 s (por NBT `Fuse`, el campo es privado), 24 de vida, algo más rápido; al estallar deja una nube cian de debilidad y náusea (`CorruptionSpawns.onDetonate`). `CorruptedCreeperRenderer` hereda del vanilla (hinchado, destello, carga eléctrica) y añade grietas de cinetrita que brillan más al hincharse. Textura: `tools/corrupted_creeper_texture.py`.
- **Jetpack**: al recibir daño el servidor manda al cliente su propia copia de la velocidad (`markHurt`), y como el movimiento del jugador lo calcula el cliente, esa copia había acumulado gravedad hasta la de caída máxima: cada golpe estampaba al jugador contra el suelo. Ahora, mientras empuja, la velocidad vertical del servidor no baja de 0 (`GearEvents`).
- GameTest `corruptedReplaceVanilla` ampliado a los 6 tipos (14 pruebas, todas pasan).

## 2026-11-08 — Claude — Tier 2: armas, armaduras y piezas de prensa con energía
- Decisiones del usuario: las 3 armaduras propuestas (placas de inercita, táctica, exoesqueleto cinético); subfusil, escopeta de corredera y rifle semiautomático; desbloqueo con la **Prensa Cinética Automática con electricidad**.
- **Prensa**: `PressRecipes.Recipe` tiene `powered`; las recetas powered solo las hace la automática (`findResult(..., powered)`, `isIngredient(..., powered)`), JEI lo indica en rojo. Piezas nuevas: Placa de Inercita (inercita + hierro + chatarra), Mecanismo de Precisión (hierro + transductor + sustrato), Servo Cinético (cinetrita + acoplador + cobre), Tela Balística x2 (cuero + cuerda + fragmento).
- **Armas** (`LongGunItem` ahora tiene `Action`: BREAK, BOLT, PUMP, SEMI, AUTO; SEMI/AUTO recargan el cargador entero de golpe):
  - Escopeta de Corredera (PUMP, módulos de escopeta): 6 cartuchos, 8 perdigones x 2,8, 18 bloques, bombea tras cada disparo.
  - Rifle Semiautomático (SEMI, módulos de rifle): cargador de 10, 13 de daño, 60 bloques, 6 ticks entre disparos.
  - Subfusil Cinético (AUTO, familia SMG sin módulos): 24 balas cinéticas sueltas, 3,5 de daño, 10 disparos/s mientras se mantiene el clic (`PistolInputHandler`); la ráfaga ("Heat") abre la dispersión. El servidor controla la cadencia con `LastShot` (no con el cooldown de objeto, para no perder disparos por latencia).
  - Modelos 3D en `weapons_gen.py` (`pump_shotgun`, `semi_rifle` con 8 combinaciones de módulos x 2 fotogramas; `kinetic_smg` + cargador fuera).
- **Armaduras** (`item/armor/Tier2ArmorItem` + `Tier2ArmorEvents`):
  - Placas de Inercita: 20 de protección, dureza 2,5, -3 % de velocidad por pieza; set: -35 % de proyectiles/explosiones.
  - Táctica: 15 de protección; set: +15 % de velocidad y -30 % de retroceso (cliente).
  - Exoesqueleto: 19 de protección; set: los golpes cargan energía (barra en la acción); agachado + saltar = salto cinético (`ExoLeapPacket`, sin daño al aterrizar); con la carga llena, el siguiente golpe cuerpo a cuerpo suelta una onda de choque; -50 % de daño por caída.
  - Texturas e iconos: `tools/armor_t2_gen.py` (también los iconos de las piezas de prensa).
- Las balas de torretas, drones y tiradores ahora son daño de **proyectil** (`mobProjectile`): cuentan para los bonos de la chatarra y la inercita.
- GameTest `tier2GunsAndArmor` (15 en total, todos pasan). Usa `FakePlayerFactory` para disparar en el servidor (el jugador simulado de GameTest no tiene conexión).

## 2026-11-08 — Claude — Anclador (enemigo antiaéreo) y efecto Lastre de Inercia
- Pedido del usuario: un enemigo que te atraiga al suelo "como pasaba con el bug" del jetpack.
- `effect/ModEffects` (registro nuevo) + `InertiaAnchorEffect` (**Lastre de Inercia**): -25 % de velocidad; en el aire arrastra hacia abajo (`pull`), corta los élitros y apaga el jetpack. El tirón del jugador lo aplica el cliente (`GearClient`, porque su movimiento lo calcula él); el de los demás mobs, el servidor. `GearEvents` deja de contar como "empujando" a quien lo tiene. Icono `textures/mob_effect/inertia_anchor.png`.
- `entity/custom/AnchorEntity` (**Anclador**): trípode pesado del Núcleo (40 de vida, 8 de armadura, casi no se le empuja, lento). Si el objetivo está en el aire (más de 2,5 bloques sin suelo debajo, o con élitros) y lo ve a menos de 28 bloques, carga 1 s un haz azul-violeta: si sigue viéndolo, le aplica el Lastre 3 s y 3 de daño (enfriamiento 4 s). Cortar la línea de visión lo evita. Cuerpo a cuerpo: pisotón (6 de daño, lentitud II, empuje) con 0,6 s de aviso.
- Modelo `AnchorModel` (casco de placas, emisor giratorio con cristal, 3 patas con rodillas de cobre y paso de trípode), `AnchorRenderer` (brillo azul-violeta; blanco parpadeante al cargar). Textura `tools/anchor_texture.py`; sonidos sintetizados `generate_anchor()` (zumbido, carga, lastre, pisotón).
- Uno vigila la entrada de cada Instalación de la IA. Huevo en la pestaña creativa. Botín: chatarra, inercita en bruto, rara vez una Placa de Inercita.
- GameTest `anchorGroundsFlyer` (16 en total, todos pasan).

## 2026-11-08 — Claude — Tier 3: Ensamblador de Armas, Procesador del Núcleo y 4 armas
- Decisiones del usuario: rifle de riel, fusil de asalto, lanzagranadas y cañón de impulso; desbloqueo con **ambos**: Procesador del Núcleo (solo de las instalaciones de la IA) + Ensamblador eléctrico.
- **Procesador del Núcleo** (`core_processor`): no se fabrica. 35 % por cofre del núcleo de la Instalación de la IA, 5 % del Anclador y 3 % de la Torreta (matados por el jugador).
- **Ensamblador de Armas** (`block/custom/WeaponAssemblerBlock`, `WeaponAssemblerBlockEntity`, `menu/WeaponAssemblerMenu`, `client/WeaponAssemblerScreen`; derivados de la prensa automática): 6 huecos de ingredientes con cantidad, 60.000 FE de batería, 100 FE/tick; recetas en `block/entity/AssemblerRecipes` (armas: 15-20 s y 30.000-40.000 FE; munición especial). Categoría propia en JEI. Se fabrica con la prensa automática + placas, mecanismos, servos y acoplador. GUI: `tools/assembler_gui_gen.py`; frente: `tools/tier3_art.py`.
- `LongGunItem`: acciones nuevas DRUM y CHARGE; ganchos `shoot(...)` y `boxRefill(...)` para armas especiales; `tryFire(..., charge)`. Familia `HEAVY` (sin módulos).
  - **Rifle de Riel** (`RailRifleItem`): mantener el clic carga 1,5 s (barra en la acción, `PistolInputHandler` + `FireChargedPacket`), al soltar dispara (mín. 30 %); 32 de daño a carga completa, atraviesa hasta 4 enemigos (-25 % cada uno) y bloques blandos (dureza < 1); cargador de 5 Balas de Riel.
  - **Fusil de Asalto**: automático, 30 balas de rifle, 7 de daño, 70 bloques, ~7 disparos/s.
  - **Lanzagranadas** (`GrenadeLauncherItem` + entidad `KineticGrenadeEntity`): tambor de 4; la granada rebota y estalla al tocar a un ser vivo o a los 2,5 s (explosión que no rompe bloques).
  - **Cañón de Impulso** (`PulseCannonItem`): cono de onda de 8 bloques que hiere (más de cerca), lanza y aturde (lentitud IV + debilidad); 6 disparos por Célula de Impulso; al recargar devuelve una Célula Vacía, que se recarga en la Prensa Automática (receta con energía).
- Modelos 3D en `weapons_gen.py` (+ fotograma de recarga) y munición 3D; zonas nuevas del atlas: olive, hazard, graphite.
- GameTest `tier3Weapons` (17 en total, todos pasan).

## 2026-11-08 — Claude — Granada de mano y animaciones (granada y carga del riel)
- `item/weapon/KineticGrenadeItem`: la Granada Cinética, además de munición del Lanzagranadas, se lanza a mano: mantener el clic derecho quita la anilla (sonido) y acumula fuerza (1 s para la máxima); al soltar se lanza. La mecha de 2,5 s empieza con la anilla (tic-tac cada 0,5 s): se puede "cocinar", pero si se agota en la mano estalla ahí. A mano rebota en bloques y cuerpos (no estalla al impacto); la del lanzagranadas sí. `KineticGrenadeEntity` tiene ahora `setFuse` y `setImpact`.
- Animaciones en primera persona: `client/GrenadeClientExtensions` (sube al quitar la anilla, el brazo se echa atrás según la fuerza, tiembla en el último segundo de mecha; tercera persona con la pose de lanzar tridente) y carga del Rifle de Riel en `LongGunClientExtensions` (se recoge contra el hombro y vibra más cuanto más carga; pulso a carga completa).
- GameTest `tier3Weapons` comprueba también el lanzamiento a mano.

## 2026-11-08 — Claude — Armaduras tier 3 (una por dimensión), Piroxita y Nulita
- Aclaración del usuario: los tiers 1-3 se consiguen en Overworld, Nether y End; los tiers 4 y 5 exigirán **3 dimensiones nuevas**. Decisiones: una armadura por dimensión, crear ya las menas del lore, fabricación en el Ensamblador de Armas.
- **Piroxita** (Nether, "disipa el calor"): `piroxite_ore` en el netherrack de todo el Nether (y=10-117, 8 vetas de 5 por chunk), emite algo de luz. **Nulita** (End, "sin fricción, servidores de la IA"): `nullite_ore` en el end stone solo de las islas exteriores (etiqueta `tecnocinetico:has_nullite`). Ambas: pico de diamante, fortuna, toque de seda; en bruto → lingote (horno y alto horno). Modelo de dos capas: la piedra vanilla (referenciada, no copiada) + capa de cristales `*_ore_overlay` (recorte).
- Armaduras tier 3 (nuevos `Tier2ArmorItem.Kind` CORE/PIROXITE/NULLITE; protección y dureza de netherita), en el Ensamblador (`AssemblerRecipes`, 15 s cada pieza):
  - **Égida del Núcleo** (Overworld; casco y coraza piden Procesador del Núcleo): escudo de 4 corazones (absorción) que se recarga 1 corazón/s tras 8 s sin recibir daño.
  - **Disipador de Piroxita** (Nether; resistente a la lava como objeto): inmune al fuego y la lava; quien golpea cuerpo a cuerpo arde 4 s.
  - **Desfase de Nulita** (End; con perlas de ender): 15 % de esquivar un golpe teletransportándose unos bloques; los enderman no se enfadan; las perlas de ender no hacen daño.
  - Eventos en `Tier2ArmorEvents` (`LivingAttackEvent`, `EnderManAngerEvent`, escudo en el tick del jugador).
- Arte: `tools/armor_t3_gen.py` (capas puestas e iconos; lingotes y en bruto recoloreados por luminancia desde los de inercita; capas de cristal de las menas).
- GameTest `tier3Armor` (18 en total, todos pasan).

## 2026-11-08 — Claude — Caza del Núcleo (nave de fin de juego) con motor de salto entre dimensiones
- Pedido del usuario: nave de fin de juego (sin receta por ahora) que vuela como un caza y viaja entre dimensiones. Elegido: caza del Núcleo, vuelo arcade, cañones + misiles guiados, motor de salto con menú y **con animación de salto**.
- `entity/ship/CoreFighterEntity` (Entity montable, sin gravedad): el movimiento lo calcula el cliente del piloto (como las barcas; las entradas las escribe `client/ClientShipHandler`). La nave sigue a la cámara con giro limitado (6°/tick), W acelera hasta 1,3 bloques/tick (~94 km/h), S frena hasta quedarse flotando (despegue vertical), espacio sube, Ctrl baja, Mayús baja de la nave (en el aire: caída lenta 5 s). 150 de casco (se repara con Placa de Inercita, clic derecho), inmune al fuego; chocar a mucha velocidad daña nave y piloto (lo detecta el servidor por el frenazo). Destruida: explosión sin romper bloques y suelta el objeto. En creativo, un golpe la quita.
  - Clic izquierdo: cañones cinéticos alternos (6 de daño, 80 bloques, cada 2 ticks). Clic derecho: misil guiado (`ShipMissileEntity`) que fija al ser vivo más centrado en la mira (prefiere enemigos), explosión 2,5 sin romper bloques, 2,5 s de recarga. Las armas de mano no disparan dentro (`PistolInputHandler.holdingPistol`).
  - Salto (`ShipJump`): J dentro de la nave abre `client/ShipJumpScreen` (un botón por dimensión del servidor: las futuras saldrán solas). Carga 3 s (`JUMP` sincronizado): anillo de partículas que se cierra, la nave vibra cada vez más, el campo de visión se abre, sonido que sube; en el último medio segundo la nave se estira hacia delante; destello blanco-cian en la pantalla del piloto. Llega al punto equivalente (escala 1:8 en el Nether) en el aire y libre: sobre el terreno +20, en el End sin isla a y=70, en el Nether el hueco más alto donde quepa (búsqueda en espiral; si no hay, lo abre). El piloto se teletransporta con ella y vuelve a montarse solo (`pendingPilot`).
  - Pantalla de vuelo: casco, velocidad, misil listo, objetivo fijado con distancia, barra del salto; se ocultan las manos.
- Cliente: `CoreFighterModel` (~5,8 bloques; fuselaje, morro, cabina que se oculta para el piloto en primera persona, alas en flecha con misiles, motores, derivas, tren que se recoge en vuelo), `CoreFighterRenderer` (rumbo, cabeceo, alabeo al girar, capa luminosa). Textura `tools/ship_texture.py`; sonidos `generate_ship()` (bucle de motor sin fundidos, cañón, misil, carga y salto); sonido de motor continuo con tono según la velocidad.
- Objeto `core_fighter` (miniatura 3D, épico) en la pestaña creativa; `core_missile` solo para dibujar el misil.
- Paquetes 11 `ShipControlPacket` y 12 `ShipJumpPacket`. GameTest `shipJumpToNether` (19 en total, todos pasan).

## 2026-11-08 — Claude — Caza del Núcleo: piloto dentro de la cabina y pantalla de vuelo tecnológica
- Problema del usuario: el piloto quedaba por debajo de la nave y en primera persona el modelo tapaba la vista; pidió una vista "más tecnológica".
- Asiento: `CoreFighterEntity.seat(pt)` pone al piloto en la cabina (0,85 sobre la base, 1 bloque hacia el morro) girando con el cabeceo alrededor del mismo pivote que el modelo; cabina más grande (9x14x18, cabe sentado con la cabeza dentro). Textura de la nave ahora 128x256 (la cabina en (0,128)).
- Primera persona: la nave no se dibuja para su piloto y se muestra `client/ShipHud`: viñeta y montantes de la cúpula, retícula, escalera de cabeceo (sigue la cámara; se inclina con el alabeo), brújula de rumbo con N/E/S/O, escalas de velocidad (km/h) y altitud (Y), barra de empuje, recuadro de fijado proyectado sobre el objetivo (matrices guardadas en `RenderLevelStageEvent`) con distancia y aviso "FIJADO" cuando el misil está listo, casco (parpadea en rojo bajo 30 %), estado del misil, barra del motor de salto. Se ocultan mira, barra rápida, experiencia y nombre del objeto. En tercera persona (F5) se ve la nave con el piloto y el HUD sencillo.

## 2026-11-08 — Claude — Caza del Núcleo más grande (x1,6)
- El usuario confirmó que la vista en primera persona está perfecta, pero en tercera persona el piloto atravesaba la nave: era demasiado pequeña para un personaje. `CoreFighterEntity.SCALE = 1,6` (el renderizador escala el modelo alrededor de la base): ~9 bloques de largo y 8 de envergadura. Ajustados con la escala: pivote de giro (`PIVOT`), asiento (1,55 de alto, 1,7 hacia el morro), caja de colisión 4,5x2,4, bocas de los cañones y de los misiles, toberas, anillo del salto, hueco que busca el salto (5,2x3,4), sombra. Cúpula algo más ancha (11 px) para que no asomen los brazos.

## 2026-11-08 — Claude — Caza del Núcleo x2,5 entero y piloto que se inclina con la nave
- El usuario rechazó el x1,6 (la cabina agrandada quedaba desproporcionada) y al girar el piloto atravesaba la cabina. Ahora: cabina con las proporciones del diseño original y **toda la nave** escalada x2,5 (`CoreFighterEntity.SCALE`): ~11 bloques de largo, 10 de envergadura; el piloto (asiento a 2,3) queda con la cabeza dentro de la cúpula y las piernas en el fuselaje. Todas las posiciones (asiento, cañones, misiles, toberas, anillo del salto, caja que busca el salto, culling, sombra) se calculan con `SCALE`. Caja de colisión 6x3,6.
- Al girar la nave se inclina (hasta 50°) y cabecea: el piloto ahora se dibuja inclinado con ella (`ClientShipHandler`, `RenderPlayerEvent.Pre/Post`) alrededor de su asiento, y la nave alabea alrededor del eje que pasa por el asiento; el cuerpo del piloto mira siempre al morro (la cabeza gira libre).

## 2026-11-08 — Claude — Caza del Núcleo más rápido
- El usuario la sentía lenta para la nave final. Velocidad máxima 1,3 → **3,5 bloques/tick (~250 km/h)**, aceleración 0,07/tick (a tope en ~2,5 s), frenada 0,12/tick. Misiles a 4,5 bloques/tick (más rápidos que la nave) y salen con 1 bloque/tick de más; cañones y fijado de objetivo a 120 bloques. Choque: cuenta si la velocidad cae a menos del 30 % de golpe; el piloto recibe el 25 % del daño de la nave.

## 2026-11-09 — Claude — Jefe: Araña de Asedio (guardián de la Instalación de la IA)
- Lore aclarado por el usuario: **la IA ya no está en el Overworld**; controla todo desde el último de los mundos nuevos (jefe y escenario finales). Este jefe es un guardián y **solo suelta materiales**.
- `entity/boss/SiegeSpiderEntity`: 450 de vida, 14 de armadura, inmune al empuje, al fuego, al Lastre y a la lentitud; barra de jefe (morada → rosa → roja). Fases: 1) ráfagas de la torreta doble (7 x 4), disparo cargado con línea roja que se fija al final (2 x 18) y pisotón (radio 7,5, 12 de daño, lentitud III); 2) desde el 60 %: haz de arrastre (te atrae hacia ella, combo con el pisotón; si vuelas, Lastre de Inercia) y llama 2 drones (máx. 4); 3) bajo el 30 %: enfurecida (+30 % velocidad, enfriamientos x0,65, ráfagas de 10) y salvas de 4 misiles guiados (`ShipMissileEntity`). Si nadie está cerca 60 s, se reinicia. Botín: procesadores, placas, mecanismos, servos, lingotes, chatarra y balas de riel; 500 de experiencia.
- Arena (`AiInstallationPiece.arena`): sala de 25x25x12 bajo la cámara del Núcleo (14 bloques más abajo) con columnas, anillo de peligro y conductos; pozo con escalera de mano desde la esquina noroeste de la cámara. **Consola del Núcleo** (`CoreConsoleBlock` + `CoreConsoleBlockEntity`, irrompible) en el centro: al activarla aparece la araña y la **Barrera del Núcleo** (`CoreBarrierBlock`, irrompible, translúcida) sella la boca del pozo (las posiciones del sello las fija la estructura); al morir la araña se abre y la consola queda apagada; si el combate se reinicia, se abre y se puede reintentar. La estructura necesita ahora 14 bloques más de profundidad.
- Cliente: `SiegeSpiderModel` (x2: ~7 bloques entre patas; tórax, placas, torreta giratoria con dos cañones, lanzamisiles, núcleo; 6 patas con marcha alterna; animaciones de carga, pisotón y haz) y `SiegeSpiderRenderer` (brillo cian / rojo enfurecida / blanco al cargar). Texturas `tools/siege_spider_texture.py` (también consola y barrera); sonidos `generate_siege()` (rugido, pisadas).
- Huevo y consola en la pestaña creativa (para probar). GameTest `siegeSpiderArena` (20 en total, todos pasan).

## 2026-11-09 — Claude — Impulsor de Esquiva (Alt) y armaduras de combate Mk I-III
- **Cuarto hueco del Equipo Táctico: cinturón** (`TacticalGear.BELT = 18`, `SIZE = 19`; huecos de gadget a 26 px en el panel, silueta de cinturón en `tactical_gui_gen.py`). La carga de equipos con otro tamaño ahora **copia hueco a hueco** (antes `setSize` los habría vaciado).
- **Impulsor de Esquiva** (`dash_thruster`, gadget de cinturón, batería 300, 10 por impulso; recarga con **Célula de Esquiva**). Tecla **Alt izquierdo** (`key.tecnocinetico.dash`): el cliente (`client/DashClient`) aplica un impulso horizontal de 1,15 hacia donde te mueves (WASD; quieto, hacia delante) y un saltito si estás en el suelo; el servidor (`gear/DashEvents`, `DashPacket` 13) comprueba gadget/batería/enfriamiento (1 s), gasta la carga y da **6 ticks de esquiva** en los que se anulan golpes y balas (no caídas ni vacío). Modelos 3D en `weapons_gen.py`.
- **Armaduras de combate** (nuevos `Tier2ArmorItem.Kind` con atributos por pieza; `Kind` ahora tiene velocidad/vida/daño y la inercita usa el mismo mecanismo): sin habilidades, pura resistencia y movilidad.
  - Mk I (tier 1, mesa: hierro, chatarra, cuero): 15 de protección, dureza 1; por pieza +2 % velocidad y +1 de vida.
  - Mk II (tier 2, mesa: placas de inercita, tela balística, servos): 19, dureza 2; por pieza +3 % velocidad, +2 de vida y +4 % daño.
  - Mk III (tier 3, Ensamblador: placas, piroxita, nulita, servos, mecanismo): 24, dureza 3,5; por pieza +4 % velocidad, +3 de vida y +6 % daño (set: +16 % velocidad, +6 corazones, +24 % daño).
  - Texturas `tools/combat_armor_gen.py`.
- GameTest `dashAndCombatArmor` (21 en total, todos pasan).

## 2026-11-09 — Claude — Araña de Asedio: rodillas como puntos débiles y Motor Dimensional
- Ayuda al jugador (pedido del usuario): cada rodilla es un punto débil. `SiegeSpiderEntity.knee(i)` calcula su posición con la misma geometría que el modelo (`LEG_ANCHORS`, compartido); en `hurt` se mira si el impacto de un jugador (posición de la bala/granada, o la línea de mira en disparos instantáneos y golpes) pasó a menos de 1,2 bloques de una rodilla sana: esa pata se rompe (bit sincronizado `LEGS`), la araña se desploma 4 s (modo 4: cuerpo al suelo, patas abiertas, no se mueve ni ataca) y recibe +50 % de daño mientras está tumbada. Una vez por pata; las rotas quedan dobladas y echando chispas. Caja de colisión ampliada a 5,4 x 3,4 para que las rodillas se puedan acertar.
- Botín: siempre suelta el **Motor Dimensional** (`dimensional_drive`, épico, modelo 3D): pieza fundamental de la nave que llevará al siguiente mundo (todavía sin uso).
- Idea del usuario guardada para más adelante (no implementada): la nave será mejorable; con el Motor Dimensional se fabrica y al principio solo va por tierra y al siguiente mundo nuevo; tras vencer al jefe del Nether podrá ir al Nether, y tras el del End, al End.

## 2026-11-09 — Claude — 8 armas nuevas (tiers 1-3)
- Elegidas por el usuario. Todas heredan de `LongGunItem` (familia HEAVY, sin módulos) salvo la cuchilla:
  - **Revólver** (T1, mesa): tambor de 6 balas cinéticas cargadas una a una, 9 de daño.
  - **Lanzallamas** (T2, `FlamethrowerItem`): automático, cono de fuego de 7 bloques (1,5 de daño cada 2 ticks + 6 s ardiendo, atraviesa enemigos, no paredes ni prende bloques); 100 de combustible por **Depósito de Combustible**.
  - **Ametralladora ligera** (T2, `HeavyGunItem`): 60 balas de rifle, 6 de daño, 10 disparos/s; -15 % de velocidad con ella en la mano.
  - **Francotirador cinético** (T2, `SniperItem`): cerrojo, 5 balas de rifle, 26 de daño, 150 bloques, **mira integrada** (`LongGunItem.integratedScope`, zoom x6,5 con visor).
  - **Cuchilla cinética** (T2, `KineticBladeItem`, espada de inercita, 12 de daño): cada golpe carga (máx. 5, barra cian); llena, clic derecho = tajo de onda en arco (6 bloques, 14 de daño, empuja).
  - **Lanzacohetes** (T3, Ensamblador, `RocketLauncherItem` + `GuidedRocketEntity`): el cohete sigue tu mira mientras lleves el lanzacohetes; explosión potencia 4 sin romper bloques. **Cohete Guiado** en el Ensamblador (2 por receta).
  - **Arco voltaico** (T3, `ArcCasterItem`): rayo que salta en cadena a 5 objetivos (-20 % por salto), 12 descargas por Célula de Impulso.
  - **Minigun** (T3, `MinigunItem`): gira ~1 s antes de disparar (`readyToFire`), 200 balas cinéticas a 20 disparos/s; -35 % de velocidad.
- `LongGunItem`: ganchos `integratedScope()` y `readyToFire()`. **Corregido**: el enfriamiento mínimo de 4 ticks frenaba también a las automáticas (el subfusil disparaba 5/s en vez de 10); ahora es 1 en las AUTO. La minigun no da retroceso mientras gira sin disparar.
- Modelos 3D en `weapons_gen.py` (con fotograma de recarga) y munición 3D. GameTest `moreWeapons` (22 en total, todos pasan).

## 2026-11-09 — Claude — Calidad de las armas: textura propia por arma, rediseños, revólver y minigun
- Crítica del usuario: las armas se veían genéricas, "copy paste" y de mala calidad (solo la pistola inicial se veía bien); el revólver era demasiado grande; la minigun no debe tener el retroceso hacia arriba.
- Causa: todas usaban un atlas compartido de 8x8 con un color plano por material. Nuevo **`tools/gunkit.py`** (el sistema de `pistol_gen.py` generalizado): cada arma tiene su textura `textures/item/gun/<arma>.png` donde cada cara de cada pieza tiene su trozo (2-4 px por unidad, empaquetado automático; las variantes de módulos/recarga comparten textura) y la pinta el **pintor de su material**: acero pavonado con bisel, luz y arañazos, acero pulido con estrías, inercita cepillada, fibra de carbono, polímero punteado, madera con veta, cuero con costura, cobre/latón cepillado, cartuchos estriados, óxido con picaduras, cinta, pintura oliva desconchada, franjas de peligro, cinetrita luminosa y bocas de cañón con ánima; juntas y pasadores distintos en cada cara. `weapons_gen.py` usa el kit (mismas funciones de geometría; `model()` agrupa por arma; `flush()` al final). Se borró el atlas viejo `weapons_t1.png`.
- Rediseños de forma (más piezas y siluetas propias): fusil de asalto (raíl dentado, ranuras, apagallamas, culata telescópica sobre tubo, cargador curvo en dos tramos), subfusil (cuerpo anguloso, bobina con anillos de cobre, cargador inclinado), ametralladora ligera (escudo térmico perforado, tambor lateral, asa, bípode, culata de esqueleto), francotirador (freno de boca grande, torretas de la mira, carrillera, monópode), revólver (a tamaño de pistola, x0,72, tambor estriado que se abre al recargar, cachas de madera), lanzallamas (válvula, manómetro, abrazaderas, llama piloto), lanzacohetes (bocinas, guía láser, asa, mira lateral), minigun (anillos, eje, rejillas del motor, cinta).
- **Minigun**: 3 fotogramas de giro de los cañones (propiedad `tecnocinetico:spin`, más rápida a plena velocidad); sin retroceso hacia arriba: el arma vibra y se apoya contra el cuerpo mientras gira, y la cámara tiembla en todas direcciones.

## 2026-11-09 — Claude — Lanzallamas real, minigun a dos manos, machete, menú creativo e identidad de las armas
- **Lanzallamas**: nueva partícula `FLAME_STREAM` (`client/FlameStreamParticle`, texturas `particle/flame_stream_0..3`): gotas de combustible ardiendo que salen de la boquilla con velocidad, caen en arco por su peso, se frenan, crecen y pasan de blanco-amarillo a naranja, rojo y humo que sube; brillan en la oscuridad y se esparcen al tocar el suelo. El servidor las lanza con `sendParticles(count 0)` (velocidad exacta). Sin retroceso (solo un temblor leve por la presión mientras dispara) y rugido propio sintetizado (`sound_gen.generate_flame`, `item.flamethrower.roar`) en vez del sonido del blaze.
- **Menú creativo** reorganizado: Máquinas y equipo, Armas (con sus módulos), Armaduras, Munición y cargadores, Materiales —cada una ordenada por tier 1→3— y Mundo.
- **Minigun** a dos manos: agarradera metálica en D moleteada encima del motor para la mano delantera, empuñadura trasera con gatillo, batería trasera, corona dentada en los cañones (siguen girando).
- **Machete** de verdad: hoja que se ensancha hacia la punta, lomo recto, un filo, punta recortada, virola, cachas de madera con remaches de latón y culata en gancho. Vista de inventario en diagonal como una espada.
- **Identidad por arma**: escopeta de caño (tubos oxidados, abrazaderas de manguera, cinta, tablón clavado), rifle de cerrojo (madera hasta la boca, abrazaderas, alza de hoja, anillas, bola de la palanca), corredera táctica (banda ventilada, guardamanos estriado, linterna, anillo fantasma, polímero), semiautomático (tapa de madera del tubo de gases, bayoneta plegable, guía de peines, diópter), rifle de riel bullpup (raíles al aire con canal luminoso, condensadores laterales, batería tras la empuñadura), lanzagranadas de tambor con 6 recámaras y alza de escalera, cañón de impulso (anillos emisores escalonados, aletas disipadoras, tubos de refrigeración), arco voltaico (horquilla de Tesla, aisladores de porcelana, bobina en vidrio, bornes) y cuchilla cinética (espada con acanaladura luminosa y guarda con emisor). `gunkit` gana los materiales `ceramic` y `glass`.

## 2026-11-09 — Claude — Parpadeo del machete (z-fighting) y sonido propio para cada arma
- **Parpadeo**: el machete tenía piezas con caras en el mismo plano mirando al mismo lado (óxido y vena de cinetrita sobre la hoja, lomo y hoja en la punta, mango y gancho) y el juego alternaba cuál dibujar. Nuevo `tools/check_zfight.py` (encontró el mismo problema, en zonas pequeñas, en casi todas las armas, la pistola, los cargadores y el jetpack). `gunkit.resolve_zfight` adelanta 0,015 la pieza más pequeña por la cara repetida; lo usan `gunkit.model`, `pistol_gen.py` y `ammo_gen.py`. Ahora no queda ninguna.
- **Sonidos**: del mismo paquete CC-BY de la pistola ("Gunshot Sounds", Tabasco) se usan las grabaciones de Mosin, SKS y escopeta. Revólver (CZ más grave y con golpe), escopetas (la de caño más grave), rifle de cerrojo + sonido real del cerrojo (también en el francotirador), francotirador (Mosin más grave con cola larga), semiautomático (SKS), subfusil, fusil de asalto y ametralladora (versiones cortas para fuego automático), minigun (chasquido muy corto + arranque del motor propio). Sintetizados: riel (chasquido + "tiuuu" de los raíles), impulso (golpe de presión grave + barrido), arco voltaico (chispa + zumbido), lanzagranadas ("tump" de tubo), lanzacohetes (expulsión + motor que se aleja) y tajo de la cuchilla. Se quitaron los sonidos prestados (rayo, fuegos artificiales, Anclador, motor de la nave). El lanzallamas ya no suena además como pistola. Créditos en `CREDITOS_SONIDOS.md`.

## 2026-11-09 — Claude — Minigun agarrada por el asa de arriba
- Pedido del usuario: la minigun se agarra por arriba (como las miniguns de mano), no por una empuñadura inferior. Se quitó la empuñadura y el gatillo de abajo; el asa en D tiene ahora un gatillo de palanca dentro y un seguro rojo encima. Todo el modelo se traslada (`MINI_SHIFT`, función `moved`) para que el asa quede justo donde la mano agarra las armas largas: el arma cuelga bajo la mano. En primera persona se sube 5,5 px (`MINIGUN_DISPLAY`) para que siga a la vista.

## 2026-11-09 — Claude — Máquinas eléctricas (fase 1): base común, plomo/estaño/titanio, Refinador, Cargador y Fabricador
- Pedido del usuario: Fabricador de munición, Cargador de células, rediseño de la Prensa Automática, Refinador Cinético, un laboratorio químico y un reactor estilo Techguns (con líquidos; reactor = cámara de reacción y también generador), y los metales plomo, titanio y estaño integrados en las recetas. Se hace por fases; esta es la 1.
- **Base común** `machine/`: `MachineKind` (huecos, flecha, capacidad), `ElectricMachineBlockEntity` (batería FE por cualquier cara, entradas por arriba/lados, salidas por abajo, progreso, luz de estado, energía sincronizada /10), `ElectricMachineBlock`, `MachineMenu` + `client/MachineScreen` (una interfaz por máquina registrada en `ModMenus.machine(kind)`), `MachineBlockEntities`, `MachineRecipes`.
- **Refinador Cinético**: mena en bruto -> 2 polvos + subproducto con probabilidad (hierro->titanio, cobre<->oro, plomo<->estaño, titanio->hierro, cinetrita->esquirla...); también chatarra -> polvo de hierro. Los polvos se funden en horno/alto horno.
- **Cargador de Células**: célula de impulso vacía -> llena (8.000 FE), gadgets del Equipo Táctico (20 FE por punto de batería) y cualquier objeto con batería FE; lo lleno pasa solo a la salida.
- **Fabricador de Munición**: 3 ingredientes en cualquier orden -> munición en lote (balas cinéticas, de rifle, cartuchos, proyectiles de riel con titanio, granadas). Ya usa plomo.
- **Metales**: plomo (y -48..48), estaño (y -16..96), titanio (y -64..-8, pico de hierro): mena normal y de pizarra, en bruto, lingote, pepita, bloque; etiquetas `forge:` (compatibles con otros mods). 10 polvos. Todo generado por `tools/metals_gen.py`; interfaces por `tools/machine_gui_gen.py`. Recetas de las máquinas en tier 2 (el Cargador usa por fin la Resistencia 330 Ω).
- GameTests `refinerMakesDust`, `chargerFillsCellsAndGadgets`, `fabricatorMakesAmmo`. La prueba del enjambre va en su propio turno (`batch = "enjambre"`): al moverse la distribución de las pruebas, los enemigos de otra la distraían. 25 pruebas, todas pasan.

## 2026-11-09 — Claude — Uranio y Cámara de Reacción (titanio y uranio solo se procesan ahí)
- Pedido del usuario: faltaba el uranio, y el titanio y el uranio solo deben poder procesarse "en el reactor". Se adelantó la **Cámara de Reacción** (fase 2).
- **Uranio**: mena normal y de pizarra (brillan un poco, y -64..-24, escasa, pico de hierro), en bruto, lingote, pepita y bloque (brilla). Etiquetas forge.
- **Titanio y uranio sin horno ni Refinador**: se borraron las recetas de horno/alto horno del titanio (en bruto, mena, polvo) y el Refinador ya no acepta titanio. El polvo de titanio (subproducto del hierro) también va a la Cámara. Su descripción lo avisa.
- **Cámara de Reacción** (`ReactionChamberBlockEntity`, `client/ReactionChamberScreen`): 100.000 FE, 150-320 FE/tick. Cada material resuena a una **frecuencia** y una **intensidad** (1-10) que se ajustan con botones (`clickMenuButton`, sin paquetes propios). Barra de **resonancia** (lo cerca del ajuste, -12 % por punto de error) y de **estabilidad**: exacto = avanza y se estabiliza; a 1 punto = mitad de velocidad y pierde estabilidad; más lejos = se detiene y la pierde deprisa (~17 s); a 0 la carga se desintegra (se pierde una unidad, chispas y humo). Recetas: titanio en bruto (4/6), menas de titanio (4/8, x2), polvo de titanio (4/3), uranio en bruto (7/5), menas de uranio (7/7, x2); subproducto: pepitas.
- La base de máquinas admite datos extra para la interfaz (`MachineKind.extraData`, `getExtra/setExtra`) y botones (`onButton`).
- Receta de la Cámara: placas de inercita, vidrio tintado, mecanismos de precisión, bloque de redstone, acoplador y **bloques de plomo** (blindaje). GameTest `reactionChamberOnlyWhenTuned` (26 pruebas, todas pasan).

## 2026-11-09 — Claude — Radiación del uranio y lingotes estilo vanilla
- **Radiación** (pedida): efecto `RadiationEffect` (daño propio `tecnocinetico:radiation` que ignora la armadura y no empuja, + hambre; nivel I cada 3 s, II cada 1,75 s, III cada 1 s). `RadiationEvents` mide cada segundo la exposición del jugador: uranio que lleva encima (en bruto, lingote, mena = 1; bloque = 9; pepita = 0,12) y bloques de uranio (6) o menas (0,6) a 3 bloques, menos con la distancia. Nivel I desde 0,5, II desde 8, III desde 32. Al alejarse se pasa en 4 s. Los objetos de uranio avisan "☢ Radiactivo". Icono propio. Mensaje de muerte propio.
- **Lingotes**: el usuario los veía raros (eran el lingote de inercita de 128 px recoloreado). Ahora plomo, estaño, titanio y uranio tienen lingote y mena en bruto en pixel art de 16 px estilo vanilla (`metals_gen.ingot_tex` / `raw_chunk`: prisma con aristas de pendiente 1:2, arista brillante, cara frontal y contorno).
- GameTest `uraniumIsRadioactive` (27 pruebas, todas pasan).

## 2026-11-09 — Claude — Lingotes idénticos a los de hierro vanilla (teñidos)
- El usuario quería que los lingotes fueran literalmente como el de hierro de Minecraft, con el color de cada metal. Ahora los modelos de lingote (plomo, estaño, titanio, uranio, cinetrita, inercita, piroxita, nulita), pepitas y menas en bruto de los metales nuevos usan la textura vanilla (`minecraft:item/iron_ingot`, `iron_nugget`, `raw_iron`) y se tiñen en el cliente (`client/MetalColors`, `RegisterColorHandlersEvent.Item`). No se copia ninguna imagen de Mojang. Los colores de la mena en bruto compensan el tono beige del hierro en bruto.
- `metals_gen.py` escribe esos modelos (`vanilla_tinted`) y borra las texturas propias de lingote/pepita/en bruto de los metales nuevos. Las imágenes viejas de cinetrita/inercita/piroxita/nulita se conservan (las usa `armor_t3_gen.py` como base), pero ya no las usa ningún modelo.

## 2026-11-09 — Claude — Recetas de las máquinas en JEI
- Pedido del usuario: ver en JEI las recetas de las máquinas nuevas. Las de horno/alto horno (polvos, metales) y crafteo (máquinas, pepitas, bloques) ya salían solas por ser recetas vanilla; faltaban las de las máquinas, que están en código.
- `compat/jei/MachineRecipeCategory`: una categoría por máquina (Refinador, Cargador, Fabricador, Cámara de Reacción) con su bloque como catalizador. Muestra entradas, salida y subproducto (probabilidad en su ventana emergente), tiempo y energía; la Cámara indica la frecuencia e intensidad de cada receta; el Cargador lista la célula de impulso y cada gadget. Páginas de información para el titanio (solo Cámara) y el uranio (radiactivo).

## 2026-11-09 — Claude — Laboratorio Químico con líquidos (ácido sulfúrico y refrigerante)
- Orden pedido por el usuario a partir de aquí: Laboratorio Químico -> reactor generador -> protección antirradiación -> metales en recetas + rediseño de la Prensa Automática.
- **Líquidos** (`fluid/ModFluids`, clase `FluidSet`: tipo, fuente, flujo, bloque y cubo): **ácido sulfúrico** (amarillo verdoso; su bloque daña 2 cada medio segundo a quien esté dentro) y **refrigerante** (azul, para el futuro reactor). Usan la textura del agua vanilla teñida; cubos con el modelo `forge:fluid_container` teñido (`DynamicFluidContainerModel.Colors`); capa translúcida.
- **Laboratorio Químico** (`ChemicalLabBlockEntity`, `client/ChemicalLabScreen`): 2 ingredientes en cualquier orden + tanque de entrada (8.000 mB) -> producto y/o tanque de salida (8.000 mB). Tuberías/bombas de Create: lo que entra va al tanque de entrada, lo que se saca sale del de salida; con cubos, clic derecho al bloque (llena la entrada o vacía la salida). La interfaz dibuja los líquidos con su textura y color, con su nombre y cantidad al pasar el ratón.
- Recetas (`MachineRecipes.LAB`): agua + 2 pólvora -> 1.000 mB de ácido; agua + 4 bolas de nieve + lapislázuli -> refrigerante; **lixiviación**: mena en bruto + 250 mB de ácido -> 3 polvos (el Refinador da 2; inercita/piroxita/nulita gastan 500 mB); 2 chatarras + 100 mB -> 3 polvos de hierro. Titanio y uranio siguen siendo solo de la Cámara.
- Receta del laboratorio con **estaño** (soldadura): lingotes de estaño, soporte para pociones, caldero, placas de inercita y servo. JEI: categoría propia con los líquidos. GameTest `chemicalLabMakesAcidAndLeaches` (28 pruebas, todas pasan).

## 2026-11-09 — Claude — Fuera la magia (glifos rúnicos) y Laboratorio Químico en 3D
- El usuario: el mod es de **tecnología**, no de magia; los glifos rúnicos venían de una idea vieja que se coló. Eliminados: los 4 bloques de glifo (`ancient_alloy_tl/tr/bl/br`, Zyr/Vex/Kael/Oth: registros, estados, modelos, botín, etiquetas, textos y pestaña), el **sello rúnico** que tenía la chapa `ancient_alloy_plating` (se veía en el Bloque de Aleación Antigua y en los lados del Ensamblador y de todas las máquinas) y el glifo grabado en la pistola (ahora una placa de número de serie). Nueva chapa limpia generada por `tools/plating_gen.py` (placas con bisel, remaches, arañazos y óxido). En mundos viejos, los bloques de glifo colocados desaparecen. Documentado en DISENO como decisión.
- **Laboratorio Químico en 3D** (inspirado en el de Techguns, diseño y texturas propios): armario con panel de control y rejilla, dos tanques de vidrio con ácido y refrigerante, matraz con mezcla verde, tuberías de cobre con válvula y luz de estado. `tools/lab_model_gen.py` (atlas 64x64 por zonas, render translúcido). `ElectricMachineBlock` admite `fullCube = false` (noOcclusion).

## 2026-11-09 — Claude — Botones del Laboratorio Químico: vaciar tanques e intercambiar
- Pedido del usuario: una ✕ bajo cada tanque para vaciarlo (el líquido se pierde) y un botón ⇄ para intercambiar los líquidos de entrada y salida (p. ej. pasar el ácido recién hecho a la entrada sin cubos). `ChemicalLabBlockEntity.onButton` (BTN_CLEAR_IN/OUT, BTN_SWAP) vía `clickMenuButton`; reinicia el progreso. Los tanques de la interfaz pasan a 16x44 para dejar sitio a los botones. El GameTest del laboratorio usa ahora vaciar + intercambiar.

## 2026-11-09 — Claude — Reactor de Fisión y Reservorio Universal
- **Reactor de Fisión** (`FissionReactorBlockEntity`, `client/FissionReactorScreen`): quema **Barras de combustible de uranio** (3 lingotes de uranio + 6 de titanio; 24.000 ticks a plena potencia) y genera hasta **600 FE/t**, que empuja solo a los vecinos (2.000 FE/t por cara; batería de 200.000 que solo se vacía hacia fuera). **Barras de control** 0-100 % (botones). Calor: +4 °/t al 100 %; refrigerante quita hasta 6 °/t (1 mB cada 2 °), agua hasta 3 °/t (1 mB por grado), pasivo 0,4 °/t. Alarma a 900°, **fusión a 1000°: explosión de potencia 6 que rompe bloques** y deja magma. Con la batería llena se pausa. Mientras quema irradia (8 en `RadiationEvents`). Sale la **Barra agotada** (radiactiva, 5) que se reprocesa en la Cámara de Reacción (frecuencia 8, intensidad 9 -> lingote de uranio + pepita de titanio). Refrigerante/agua por cubos o tuberías (`FluidMachine`, interfaz común con el laboratorio).
- **Reservorio Universal** (`reservoir/`): guarda cualquier líquido, varios a la vez, en cualquier cantidad (contador de 64 bits por líquido). Tuberías/cubos meten cualquier líquido; sale el elegido en la interfaz (lista con icono, nombre y cantidad; clic = elegir salida, ✕ = desechar; rueda para desplazar). Se sincroniza al cliente cada 10 ticks como mucho. Al romperlo conserva el contenido (botín `copy_nbt` -> `BlockEntityTag`) y su descripción lo muestra. No acepta líquidos con NBT (no perder datos). Receta: titanio, vidrio y caldero.
- Base de máquinas: `isLit()` (la luz del reactor = quemando), `Battery.produce`, `FluidMachine` para los cubos; `serverTick` sale si el bloque desapareció (explosión). JEI: páginas de información del reactor y del reservorio; el reprocesado sale en la Cámara de Reacción. GameTests `reactorGeneratesAndStaysCool`, `reservoirStoresAnything` (30 pruebas, todas pasan).

## 2026-11-09 — Claude — Celda Universal de Fluidos (en lugar del bloque Reservorio)
- Malentendido: el usuario quería la **Celda Universal de Fluidos** de IndustrialCraft 2, no un bloque. Se borró el Reservorio Universal (`reservoir/`, pantalla, recursos, receta, botín) y se hizo `fluid/UniversalCellItem`: 1 cubo de **cualquier** líquido (vanilla, del mod o de otros mods), se apila hasta 64, clic derecho recoge una fuente o la vierte, funciona con máquinas/tuberías (`FluidHandlerItemStack`). Modelo `forge:fluid_container` con textura propia (casquillos de estaño, vidrio, máscara y brillo): dibuja el líquido que lleva. El nombre muestra el líquido. Receta: 4 estaño + panel de vidrio -> 4 celdas. GameTest `universalCellHoldsAnyFluid`.
- Plan pedido a continuación: **Ensamblador Industrial** 3x3x3 (se despliega de una vez; brazos que se ven trabajando y objetos visibles) que fabrica lo complejo, empezando por el **reactor 9x9x6** que se despliega desde un kit (núcleo con las barras visibles).

## 2026-11-09 — Claude — Estructuras grandes: Ensamblador Industrial 3x3x3 y Reactor de Fisión 9x9x6 (kits que se despliegan)
- Pedido del usuario: reactor de 9x9 (no construirlo bloque a bloque como en Techguns) y un ensamblador que fabrique lo más complejo del mod, viendo los objetos mientras se procesan. Elegido: **kits que se despliegan** de una vez; ensamblador 3x3x3; reactor 9x9 de base y 6 de alto.
- **Base de estructuras** (`structure/`): `MultiblockTemplate` (plano por capas con caracteres; rotación según hacia dónde mira el jugador; comprueba que el volumen esté libre), `StructureKitItem` (despliega delante del jugador, centrado; si algo estorba avisa qué y dónde), `StructurePartBlock` + `StructurePartBlockEntity` (pieza que recuerda su núcleo: clic, energía, líquidos y objetos van al núcleo; puede ser invisible), `MultiblockMachineBlock` (núcleo; al romper cualquier pieza se desmonta todo y suelta el kit + inventario; la fusión del reactor no suelta kit), `Structures` (planos).
- **Ensamblador Industrial** (`IndustrialAssemblerBlockEntity`): 9 ingredientes en cualquier orden -> producto; 200.000 FE. Receta inicial: **kit del Reactor de Fisión** (16 bloques de plomo, 32 titanio, 24 vidrio, 2 Procesadores del Núcleo, 8 mecanismos, 6 servos, 16 placas de inercita, 8 bloques de cobre, 4 de redstone; 60 s a 400 FE/t). Modelo 3x3x3 dibujado por el núcleo (coordenadas -16..32): base con franja de peligro, columnas, pórtico con raíl y foco, mesa de trabajo, pared de maquinaria, consola con pantalla y luz. **Renderizador** (`client/IndustrialAssemblerRenderer`): ingredientes sobre la mesa, holograma del producto que crece con el progreso, **dos brazos robóticos** (modelo aparte) que se mueven y chispas. Se fabrica con el Ensamblador de Armas + titanio, pistones, mecanismos y un Procesador del Núcleo.
- **Reactor de Fisión 9x9x6**: el bloque `fission_reactor` es ahora el núcleo (sin objeto propio). Suelo y muros de blindaje, escotilla con radiación, ventanas en los 4 lados, **vasija de vidrio 5x5** con el núcleo abajo, tejado con claraboya y respiraderos. **Renderizador** (`client/FissionReactorRenderer`): la barra que quema brilla en el centro de la vasija (late con la potencia) y las que esperan (hasta 8) la rodean apagadas. Refrigerante, energía y barras entran/salen por cualquier pieza.
- Las máquinas pueden sincronizar su contenido con el cliente (`syncsToClient`, cada 5 ticks). Generador `tools/structures_gen.py`. JEI: categoría del Ensamblador Industrial. GameTests `assemblerDeploysBuildsAndDismantles` y `reactorStructureDeploys` (turno propio). 32 pruebas, todas pasan.

## 2026-11-09 — Claude — El reactor reparte su energía por toda la estructura
- Al preguntar el usuario cómo se le da energía al reactor (no se le da: la genera), se vio un fallo: solo empujaba energía a los vecinos del núcleo, que está escondido dentro de la vasija. Ahora `FissionReactorBlockEntity.push` calcula las caras exteriores de la estructura 9x9x6 (una vez) y cada 2 s busca cables/máquinas pegados por fuera que acepten energía; cada tick les reparte hasta 2.000 FE/t. Los cables también pueden sacar energía de cualquier pieza. El GameTest del reactor comprueba que un Cargador pegado al muro por fuera se carga.

## 2026-11-09 — Claude — Franja de estado en todas las máquinas y arreglo de la Cámara de Reacción
- El texto de estado (sin electricidad, trabajando, listo...) se montaba sobre otros textos o partes del diseño. Ahora todas las interfaces de máquinas (prensa automática, Ensamblador de Armas y todas las de `MachineScreen`) miden 178 px: el inventario baja 12 px y en el hueco hay una **franja oscura de estado** (y 72..83) donde se escribe, en colores claros. `tools/status_strip.py` la añade en los tres generadores de interfaces; `MachineScreen.status()` la sobreescriben la Cámara (inestable), el Laboratorio y el Reactor (genera/sobrecalentamiento/apagado).
- **Cámara de Reacción**: sin energía seguía perdiendo estabilidad (y podía desintegrar la carga). Ahora sin energía el haz está apagado y nada cambia. GameTest con una cámara sin energía mal ajustada que conserva la carga.

## 2026-11-09 — Claude — El reactor se ve como UNA sola máquina
- El usuario: el chiste de desplegarlo de una vez es que se vea uniforme, no como un conjunto de bloques. Ahora la estructura 9x9x6 es maciza de piezas **invisibles** (`SegmentedPartBlock`, bloque `reactor_shell`) y el aspecto lo da un **modelo continuo** diseñado entero (`tools/reactor_model_gen.py`): zócalo, franja de peligro corrida, columnas, blindaje en paneles grandes sin juntas por bloque, escotilla con el símbolo de radiación, ventanas enmarcadas, carteles, panel de control, tuberías de cobre, luces cian, tejado estriado con claraboya sobre la vasija y 4 respiraderos; dentro, la vasija de vidrio con las barras (renderizador). Como un modelo no puede pasar de 3 bloques, se trocea en **18 modelos de 3x3x3** que dibujan los centros de cada bloque de 3x3x3 (propiedad `segment` puesta al desplegar con `MultiblockTemplate.withPartState`; el núcleo dibuja el trozo 5). Las UV se calculan con la posición real en el reactor, así que los trozos encajan sin costuras. Se borraron las piezas visibles anteriores (blindaje, escotilla, ventana, vasija, respiradero).
- Nuevo `tools/render_zbuffer.py`: vista previa con búfer de profundidad para modelos grandes (render_model.py se confunde con tantas caras).

## 2026-11-09 — Claude — Parpadeo del Ensamblador, partículas y sonidos de las máquinas, Cámara de Reacción 2x2x2
- **Parpadeo** en la parte baja del frente del Ensamblador Industrial: la franja de peligro coincidía con las columnas delanteras (y el foco con la viga). `structures_gen` y `reactor_model_gen` pasan ahora sus elementos por `gunkit.resolve_zfight` (que, si la pieza pequeña se saldría de -16..32, mete un poco la grande). `check_zfight.py` revisa también `models/block` (ignora las menas: su capa encima es a propósito).
- **Efectos de trabajo** (`ElectricMachineBlockEntity.effects`, solo trabajando y con energía; el reactor, mientras genera): sonidos propios sintetizados (`sound_gen.generate_machines`: refinador moliendo, cargador zumbando, fabricador troquelando, cámara resonando, laboratorio burbujeando, reactor retumbando con contador Geiger, ensamblador con servos y chispas) y partículas (polvo de la mena, chispas eléctricas, humo y chispas del troquel, chispas cinéticas en el emisor, burbujas y vapores, vapor por los 4 respiraderos del reactor). Ayuda `local(u, v, w)` para situar efectos en estructuras.
- **Cámara de Reacción 2x2x2** (la que procesa el uranio y el titanio): ahora es una estructura que se despliega desde su kit (`reaction_chamber`, la receta de siempre da el kit), con modelo único (`tools/chamber_model_gen.py`, mismo sistema que el reactor): base con franja de peligro, columnas, paneles claros, mirilla con el soporte de la muestra, panel de control con pantalla y diales y luz de estado, emisor del haz con bobinas en el techo y tuberías. **Renderizador** (`client/ReactionChamberRenderer`): la carga flota dentro (se ve por la mirilla), gira más deprisa cuanto mejor la resonancia, y el haz cian baja del emisor mientras trabaja. GameTest `reactionChamberDeploys` (33 pruebas, todas pasan).

## 2026-11-09 — Claude — Animación del Ensamblador Industrial y traje antirradiación
- **Ensamblador sin animación**: el cliente recibía el progreso pero no la duración total (`maxProgress` no se guardaba en el NBT), así que para él nunca estaba trabajando y los brazos no se movían. Ahora `ElectricMachineBlockEntity` guarda `max_progress`.
- **Equipo de protección** (`Tier2ArmorItem.Kind.HAZMAT`): casco, traje, pantalones y botas antirradiación, forrados de plomo (piezas de cuero rodeadas de lingotes de plomo; el casco lleva un panel de vidrio de visor). Cada pieza quita un 20 % de la radiación (`RadiationEvents`); el traje completo la anula y protege del ácido sulfúrico. Poca protección contra golpes y un -2 % de velocidad por pieza (el plomo pesa). Texturas propias que cubren todo el cuerpo (`tools/hazmat_gen.py`): amarillo con pliegues, visor con filtro, bandas reflectantes, dosímetro, guantes y suelas de goma. El GameTest de radiación comprueba las piezas y el traje completo.

## 2026-11-09 — Claude — Armaduras en 3D (estilo Techguns)
- El usuario veía las armaduras genéricas y quería las de combate en 3D como en Techguns; eligió **todas en 3D** y estilo **militar realista** para las de combate.
- `client/Armor3DModels`: cada set tiene un modelo por pieza (casco; pecho + brazos; pantalones; botas) hecho de cubos que sobresalen del cuerpo, leído de `assets/tecnocinetico/armor_models/<set>.json`, con textura `textures/models/armor/3d/<set>.png`. Se construye la primera vez que se dibuja y se vacía al recargar recursos (F3+T). `Tier2ArmorItem` y `ScrapArmorItem` lo usan (`initializeClient` + `getArmorTexture`).
- `tools/armor3d_gen.py` define los 11 diseños (piezas del lado derecho, el izquierdo se refleja solo) y pinta cada cara según su material (placa con bisel y remaches, tela con trama, correas con costura, bolsillos con solapa, visores con brillo, luces, óxido, rejillas). Combate Mk I (portaplacas, cargadores, casco con gafas, rodilleras), Mk II (casco con visor y cascos de radio, hombreras en capas, antebrazos, placas de muslo, espinilleras), Mk III (casco cerrado con visor en T luminoso, mochila de energía, guanteletes, pistones de rodilla y tobillo); chatarra (olla con asas y gafas de soldar, neumático, cuerdas), inercita (placas macizas, cresta, rendija), táctica (soporte de visión nocturna, chaleco negro), exoesqueleto (barras, columna, servos), núcleo (blanca, halo, núcleo y generador), piroxita (aletas de calor, grietas incandescentes), nulita (cuernos y hombreras en punta) y antirradiación (capucha, visor grande, filtro, botella de aire). Con una carpeta de vista previa genera maniquíes para `render_zbuffer.py`.

## 2026-11-09 — Claude — Iconos de las armaduras a juego (modelo 3D en el inventario)
- Los iconos planos de las armaduras no casaban con los diseños 3D. Ahora `tools/armor3d_gen.py` escribe también el modelo de inventario de cada pieza (`models/item/<set>_<pieza>.json`): los mismos cubos y la misma textura que la pieza puesta, de frente y algo girada, escalada para caber en la casilla (como las armas: el usuario prefiere el modelo 3D a los iconos planos). `weapons_gen.py` ya no escribe modelos planos de armaduras.
- Arreglo: los iconos se veían **morados y negros** porque la textura de la armadura (`textures/models/armor/3d/`) no estaba en el atlas de bloques/objetos (solo entran `block/` e `item/`). Se añadió `assets/minecraft/atlases/blocks.json` con esa carpeta, y `check_models.py` avisa ahora de texturas fuera del atlas.

## 2026-11-09 — Claude — Plomo, estaño y titanio en las recetas de siempre
- **Plomo** (munición y pilas): balas cinéticas = 2 esquirlas + cobre + 2 pepitas de plomo (antes 4 esquirlas + cobre); balas de rifle y cartuchos llevan una pepita de plomo (en vez de pepita de hierro / chatarra); pilas del visor, del propulsor y del impulsor de esquiva llevan una pepita de plomo; la Célula de Impulso del Ensamblador lleva 2 lingotes de plomo.
- **Estaño** (soldadura): el Sustrato Lógico de Pizarra se prensa con estaño en vez de cinetrita.
- **Titanio** (solo tier 3, porque sale de la Cámara de Reacción, que se fabrica en tier 2): rifle de riel, fusil de asalto, lanzagranadas, lanzacohetes, minigun, proyectiles de riel y las armaduras del Núcleo y de combate Mk III (2-3 lingotes por pieza).

## 2026-11-09 — Claude — Prensa Automática al estilo Create y armaduras sin parpadeo
- **Prensa Cinética Automática** rediseñada (`tools/press_model_gen.py`): deja de ser un cubo; base con rejillas y manómetro, franja de peligro, soporte de la pieza, cuatro columnas con remates de latón, cabecera con el pasante del eje y luz de estado. El **cabezal** es un modelo aparte que mueve `client/AutoPressRenderer`: baja poco a poco y golpea cada segundo mientras trabaja (chispas, humo y golpe metálico en el servidor); los ingredientes se ven sobre el soporte y, al terminar, la pieza fabricada. La prensa envía su contenido y progreso al cliente cada 4 ticks; el bloque usa noOcclusion.
- **Armaduras 3D**: había caras coplanares (parpadeo) entre cubos de la misma pieza; `armor3d_gen.fix_zfight` infla un poco la pieza más pequeña (sin tocar la UV). `check_zfight` ya no encuentra ninguna en todo el mod.

## 2026-11-09 — Claude — Nuevos modelos: cinetrita, piroxita y nulita en bruto, fragmento, resistencia y transductor
- **En bruto** (cinetrita, piroxita, nulita): como los metales nuevos, el hierro en bruto vanilla teñido (`client/MetalColors`, colores compensados; modelos de `metals_gen.py`).
- **Fragmento de cinetrita, Resistencia 330 Ω y Transductor Piezo-Cúprico**: modelos 3D en el inventario (`weapons_gen.py`, textura propia por objeto con `gunkit`): racimo de cristales cian sobre roca; resistencia con extremos abombados, bandas naranja-naranja-marrón-dorado y patas; disco de cobre octogonal con cerámica, electrodo, punto de cinetrita y cables rojo y negro.
- **Cambio (a pedido del usuario)**: fragmento, resistencia y transductor vuelven a **arte plano 16x16** (prefiere arte normal en materiales y componentes pequeños; 3D solo en armas, armaduras y máquinas). Nuevo `tools/parts_art.py`: cristal cian en diagonal, resistencia beige en diagonal con bandas y patas, disco de cobre con cerámica, electrodo y cables. Se quitaron sus modelos 3D de `weapons_gen.py`.

## 2026-11-09 — Claude — La cinetrita se ve como cristal, no como metal
- **Cinetrita en Bruto**: trozo de roca con puntas de cristal cian (ya no el hierro en bruto teñido).
- **Cristal de Cinetrita** (id `cinetrite_ingot`, se conserva para no romper recetas ni mundos): cristal tallado (prisma hexagonal con dos puntas y destello), ya no el lingote vanilla teñido.
- Arte en `tools/parts_art.py`; se quitaron de `metals_gen.py` (OLD_INGOTS y en bruto) y de `client/MetalColors`.

## 2026-11-09 — Claude — El mod es para multijugador
- El usuario lo jugará con amigos. Revisión rápida: paquetes validados en el servidor, capacidades sincronizadas (entrar, reaparecer, dimensión, seguimiento), efectos de armas y máquinas enviados desde el servidor, menús con distancia, estado estático con clave por dimensión/UUID. No se encontraron fallos. Regla añadida en AGENTS.md §4 y DISENO.md.

## 2026-11-10 — Claude — Fuego amigo y botón "+" de JEI en las máquinas
- **Fuego amigo** (`rules/FriendlyFire`): regla general, por defecto desactivada. En LAN se elige con un botón nuevo en la pantalla "Abrir en LAN" (solo ahí; se aplica si de verdad se abre). En servidor dedicado: `serverconfig/tecnocinetico-server.toml`. Desactivado, ningún jugador hiere a otro (armas, golpes, proyectiles, explosiones) y el lanzallamas/cuchilla/cañón de pulso/rifle de riel tampoco queman ni empujan a compañeros. GameTest nuevo (34 pasan).
- **JEI "+"**: pasar la receta a las máquinas (refinador, cargador, fabricador, cámara de reacción, laboratorio, ensamblador industrial, prensa automática y ensamblador de armas), con cantidades.

## 2026-11-10 — Claude — Revisión externa: errores confirmados y arreglados
- **Jetpack en servidor dedicado**: vanilla expulsaba por "volar" (allow-flight=false). Mientras empuja se reinicia `aboveGroundTickCount` (`gear/FlightKick`, reflexión con nombre SRG `f_9737_`). La nave no tenía el problema (sin gravedad: vanilla no la revisa; y un pasajero no se comprueba).
- **Rifle de Riel**: la carga ya no la dicta el cliente: `RailChargePacket` al empezar; el servidor limita la carga al tiempo real (+3 ticks).
- **Ensamblador de Armas**: recetas con los mismos tipos (Lanzagranadas/Lanzacohetes/Minigun, cascos/pantalones de Piroxita, Nulita y Mk III, Rifle de Riel/Peto del Núcleo) → gana la más exigente que se cumpla; con las cantidades justas (o el "+" de JEI) sale cada una. GameTest que lo prueba con todas.
- **Jefe en Pacífico**: si la Araña de Asedio se descarta sin morir, la consola se reinicia y la barrera se abre.
- **Reactor**: ahora sí irradia (busca núcleos encendidos a 12 bloques; antes buscaba a 3 y el núcleo queda a ≥5).
- **Recarga del Impulsor de Esquiva** con clic derecho: faltaba el cinturón.
- **Prensa Automática**: espera 1 s con la receta estable antes de empezar y reinicia el progreso si cambia (tolvas). Sigue siendo cierto que si falta el cobre los cristales solos se convierten en fragmentos (anotado en PENDIENTES).
- **Caza del Núcleo**: el choque solo cuenta si hay algo sólido delante (el lag ya no da daño fantasma); al destruirse suelta los restos con 15/150 de casco.
- **Menores**: la resonancia de la Cámara llega al cliente; las máquinas y la prensa solo se sincronizan si trabajan o cambió algo; R con un arma larga en la mano secundaria no la recarga; gadgets sin Reparación por yunque; los kits no se despliegan encima de jugadores o criaturas.
- ARQUITECTURA (registros) al día. 35 GameTests pasan.


## 2026-11-10 — Codex — Revisión del estado actual
- Revisadas las instrucciones, documentación, controles de red, disparo, menú común de máquinas y despliegue de estructuras. Sin cambios en las mecánicas.
- Registrados en PENDIENTES dos fallos de validación del servidor: disparo normal que evita la carga del Rifle de Riel y cadencia automática que permite varios disparos de minigun en el mismo tick. Hallazgos por lectura de código; no reproducidos con un cliente modificado.
- Validación: compileJava + runGameTestServer, 35 pruebas requeridas superadas y BUILD SUCCESSFUL; tools/check_models.py sin errores. Falta valoración visual y equilibrio jugando en supervivencia/multijugador.

## 2026-11-10 — Codex — La Araña de Asedio y los drones no se atacan
- Corregido el fuego entre jefe y drones: `isAlliedTo` mutuo, rechazo de objetivos aliados y bloqueo del daño atribuido a la otra entidad antes de aplicar daño o registrar represalias. Incluye las explosiones de los misiles del jefe y funciona también con drones ya existentes.
- `CoreShots` ignora aliados al buscar impactos; `ShipMissileEntity` no colisiona con aliados de su propietario. Un dron interpuesto ya no recibe los tiros del jefe ni interrumpe su trayectoria hacia el enemigo, y viceversa.
- GameTest `siegeSpiderAndDronesAreAllies`: daño cuerpo a cuerpo, balas, explosión atribuida al jefe, objetivos, agresor de represalia, disparos con aliados interpuestos en ambos sentidos y vulnerabilidad al jugador. `compileJava` correcto y las 36 pruebas requeridas de `runGameTestServer` superadas.

## 2026-11-10 — Codex — Cinco estructuras restantes y tres jefes del Overworld
- Pedido del usuario: completar las estructuras del Overworld y tener tres jefes **contando** a la Araña de Asedio. Añadidos **Vigía de la Red** (360 vida, antena) y **Matriarca del Enjambre** (520 vida, laboratorio). La IA final sigue reservada a las dimensiones futuras; la Araña mantiene el Motor Dimensional.
- `OverworldSiteStructure/Piece`: antena repetidora con drones y plataforma de combate, centro de datos con torretas/racks, búnker subterráneo con acceso por escalera, laboratorio profundo con tanques/cuerpos de enjambre/arena y ciudad sumergida con tres edificios, avenida y pasarela. Semilla, variante y alturas persistentes; construcción independiente del orden de chunks. Datos, separación, biomas, cofres y libros de lore en `overworld_sites_gen.py` (también llamado desde `world_structures_gen.py`).
- Ciudad: muestrea nueve puntos del fondo para evitar islas y desniveles extremos; limpia el volumen con agua hasta el nivel del mar antes de construir. El búnker y el laboratorio tienen un pozo hasta la superficie; se comprobó la conexión interior.
- `OverworldGuardianEntity`: tres fases, modos sincronizados, barra de jefe y ventanas de núcleo expuesto de 24 ticks (+50 % de daño). Vigía volador con rayos y pulso; Matriarca terrestre con embestidas y bombardeo de posición fijada antes del impacto. Ataques y efectos desde el servidor; no dañan aliados del Enjambre/drones. Sin Guía de Enjambre en botín.
- Consola ampliada con tipo de guardián y punto de aparición persistentes (las antiguas conservan la Araña). Una activación por combate, sellos restaurados al vencer/abandonar, victoria permanente y reinicio tras 60 s sin jugadores de supervivencia cerca.
- Arte propio: dos modelos articulados Java (rotores/patas), texturas de 128 px con brillo/carga/exposición, huevos en creativo y textos ES/EN + variantes de español. `overworld_guardians_gen.py` genera recursos y aproximaciones estáticas para previsualizar; `preview_overworld_sites.py` compone los volcados reales de estructuras. Vistas revisadas en `build/guardian_previews/` y `build/overworld_previews/estructuras.png`; no se abrió el cliente.
- Validación final: `compileJava runGameTestServer`, **39 pruebas requeridas superadas** y `BUILD SUCCESSFUL`. `GuardianGameTests`: persistencia/activación/reintento/victoria de las consolas y combate real de ambos jefes. `WorldgenGameTests`: las 13 estructuras, tipos de consola, acceso del búnker y agua en la avenida submarina. `check_models.py` correcto. La primera búsqueda del búnker cayó en una región oceánica: se amplió el muestreo de terreno para las nuevas estructuras. Ajustes de equilibrio y apariencia en multijugador quedan para probar jugando.
- Prueba desde IntelliJ `runClient`: mundo nuevo o chunks nuevos; `/locate structure tecnocinetico:relay_antenna`, `data_center`, `survivor_bunker`, `swarm_laboratory`, `sunken_city` (repetir el prefijo en cada comando). Clic derecho en la consola de cada arena. Los huevos permiten ver los jefes en creativo sin buscar la estructura.

## 2026-11-10 — Codex — Guardianes rediseñados, dificultad creciente y piezas exclusivas del caza
- Pedido del usuario: Vigía más difícil que Araña; Matriarca más difícil que Vigía; mejorar los dos modelos, darles un drop exclusivo para el caza y dejar debilidad solo en el Vigía. El Motor Dimensional de la Araña y la futura receta del caza se mantienen como estaban.
- **Vigía**: 700 vida, 18 armadura, 8 dureza; ráfagas de 3 rayos (4 en fase final), pulsos de radio 10, preparación y espera más cortas. **Dos condensadores traseros** sincronizados: acumulan 24 daño de impactos de jugador, se rompen una sola vez cada uno, cancelan ataques y aturden 60 ticks (+50 % daño recibido mientras está aturdido). El frente blindado no activa esta debilidad. Roturas, daño acumulado y aturdimiento se guardan en NBT.
- **Matriarca**: 1.000 vida, 22 armadura, 12 dureza; embestida más veloz/dañina y bombardeo de tres zonas de radio 5,5, todas señalizadas antes de impactar. Un objetivo solo recibe daño una vez por embestida. Eliminadas la exposición automática y cualquier bonificación de daño/aturdimiento por componentes; no tiene puntos débiles.
- **Piezas exclusivas**: una garantizada por muerte del jefe respectivo, sin cofres/comercios/recetas. Vigía → **Propulsor Vectorial** (`vector_thruster`); Matriarca → **Núcleo de Vuelo Adaptativo** (`adaptive_flight_core`). Épicas, incombustibles, no apilables, iconos planos 32 px, textos ES/EN + variantes y pestaña Materiales junto al Motor Dimensional. Ninguno entrega la pieza del otro jefe.
- **Arte**: Vigía de silueta de interceptor, casco escalonado, alas inclinadas, radar, aletas traseras, cuatro turbinas octogonales con aspas, cañones dobles y condensadores visibles que desaparecen al romperse. Matriarca con fibras bajo placas superpuestas, abdomen segmentado/respiración, espinas, seis ojos, mandíbulas articuladas y seis patas con rodilla, espinillera y garras. Texturas propias 128 px, cobre/acero/inercita y acentos cian/verde; sin magia. Se corrigieron caras coincidentes en blindaje y radar.
- **Herramientas**: `guardian_rig.py` y `guardian_model.java.template` son la fuente del modelo Java generado y sus vistas con la misma jerarquía/rotaciones. `overworld_guardians_gen.py` genera todo el arte, valida UV y conserva los textos. `render_zbuffer.py` admite vértices transformados de los rigs; `preview_guardians.py` compone una comparativa con vista trasera y drops en `build/guardian_previews/guardianes_redisenados.png`. Vistas revisadas sin abrir el cliente.
- **Mundos existentes**: migración `GuardianBalance=2` actualiza los atributos antiguos preservando el porcentaje de vida; no hace falta generar otro mundo para ver los modelos o combatir con el equilibrio nuevo.
- **Validación**: `compileJava runGameTestServer`, **41 pruebas requeridas superadas** y `BUILD SUCCESSFUL`; incluye ambas tablas reales de botín, condensadores (daño parcial, rotura, aturdimiento, guardado, frontal bloqueado y no repetir la misma rotura), Matriarca sin debilidad, migración de vida y combate real. `check_models.py` correcto; compilación final tras el pequeño ajuste visual del radar. Pendiente: probar en grupo el equilibrio y revisar las animaciones en `runClient`.

## 2026-11-10 — Codex — Crías de la Matriarca y combate aéreo del Vigía
- Prueba del usuario: la Matriarca tiene buena dificultad, pero quiere mini enemigos; el Vigía resultó decepcionante (parecía tener un solo ataque y se pudo vencer con espada de madera). Se mantienen sus vidas/armaduras, drops y debilidades anteriores; el cambio se centra en el comportamiento.
- **Vigía**: se separó su cerebro aéreo del terrestre. `watcherFlight` trabaja también al cargar/disparar, despega correctamente desde huevos, mantiene altura respecto al suelo/objetivo, se separa 12–16 bloques y se retira de golpes cercanos. Ante obstáculos asciende y vira lateralmente. Ya no se queda casi inmóvil a altura de espada durante las salvas.
- **Cuatro patrones en ciclo**: ráfaga que vuelve a apuntar entre disparos (fija cada tiro 5 ticks antes y espacia los impactos 10 ticks para respetar la invulnerabilidad vanilla); bombardeo de tres zonas pequeñas; barrido de 60° con 7/9 rayos; EMP dirigido a una posición fijada con radio 6 y Lastre de Inercia durante 40 ticks. Cada ataque tiene su aviso y margen para esquivar. El pulso anterior dependía de la altura de aparición y a veces quedaba fuera del combate; el EMP actual usa la posición del objetivo. Modos 4/5 nuevos para barrido/EMP y brillo de carga actualizado. Los dos condensadores traseros siguen siendo su debilidad.
- **Matriarca**: conserva daño, cadencias, 1.000 vida y ausencia de puntos débiles. Invoca **Crías de la Matriarca** (`MatriarchMinionEntity`), pequeñas variantes del Cuerpo de Enjambre (escala .45, colisión .54, 10 vida, 3 daño, salto y persecución). Primer intento a los 4 s; oleadas de 2/3/4 cada 12/10/8 s según fase, máximo 6 activas por madre. Solo aparecen con suelo firme, espacio libre y sin entidades dentro.
- **Ciclo de las crías**: objetivo coordinado con la madre y alianza mutua (también con máquinas/Enjambre aliados); daño y selección de objetivos aliados bloqueados. Sin XP ni botín, sin spawn natural. UUID de madre/crías y espera guardados; validación de propiedad tras cargar y limpieza al morir/descartar el jefe. Las crías de chunks descargados no pueden volver a sumarse por encima del límite. Renderer reutiliza modelo/texturas propios del Enjambre; textos ES/EN y variantes de español sincronizados.
- **Validación final**: `compileJava runGameTestServer`, **43 pruebas requeridas superadas** y `BUILD SUCCESSFUL`; `check_models.py` correcto y `git diff --check` sin errores. Nuevos GameTests: despegue desde el suelo y altura durante la persecución, cuatro patrones observados y daño real; invocación real de crías, ataque al enemigo, alianzas/daño mutuo, guardado, límite y limpieza por muerte de la madre. Se ajustó la simulación de persecución a velocidad real de jugador (no teletransporte continuo bajo el jefe) y se registra la autoría del daño cuando ocurre.
- Prueba en `runClient`, en supervivencia y tras reiniciar el cliente: perseguir al Vigía desde el suelo y observar el ciclo completo; combatir a la Matriarca más de cuatro segundos para ver la primera oleada. Funciona en mundos existentes.

## 2026-11-10 — Codex — Batería triple para el Impulsor de Esquiva
- Pedido del usuario: al menos tres veces más batería. `ModItems.DASH_THRUSTER`: capacidad de **300 a 900**, conservando coste de 10 por esquiva, enfriamiento y efectos (30 → 90 esquivas por carga completa).
- Los impulsores existentes adoptan la capacidad nueva conservando el desgaste ya consumido. La célula sigue recargando al máximo; el Cargador de Células usa automáticamente la capacidad nueva (20 FE por punto). Sin cambios en los otros gadgets.
- Validación: `compileJava runGameTestServer`, **43 pruebas requeridas superadas** y `BUILD SUCCESSFUL`, incluida `dashAndCombatArmor`; `git diff --check` correcto. Probar tras reiniciar `runClient`.

## 2026-11-11 — Codex — Remodelación de la Araña y los demás mobs
- Pedido del usuario: extender el detalle de los nuevos jefes a las criaturas existentes antes de implementar el Nether. Vigía y Matriarca conservan sus modelos aprobados; este cambio es visual y no modifica IA, daño, vida, botín ni colisiones.
- **Araña de Asedio**: caparazón con placas superpuestas, ventilación, reactor, lanzamisiles, cañones dobles y patas con pistones. Conserva los seis anclajes originales de rodillas y las animaciones de rotura, pisotón y aturdimiento.
- **Máquinas**: Dron con cuatro rotores carenados, sensor y arma inferior; Torreta con estabilizadores, blindaje y dos cañones; Anclador con emisor cerámico, bobinas y trípode articulado.
- **Enjambre**: Cuerpo con placas, mandíbula y cuchillas mecánicas (también lo heredan las crías); avispas con cuatro alas, aguijón, sensores de exploradora y placas de escolta visibles según rol; Nodo con modelo propio de reactor suspendido y seis pétalos, en lugar de una avispa gigante.
- **Corruptos y supervivientes**: arañas normales/de cueva con placas, colmillos y articulaciones; creeper con costillas, máscara y depósitos, conservando hinchado/destello vanilla. Humanos con implantes, tendones y prótesis, más casco/mochila de trabajador, cristales, máscara de saqueador, blindaje de soldado o tanque de ahogado. Supervivientes con protecciones, correas y equipo de chatarrero, armero o agricultor; mantienen poses de armas y capas de armadura humanoides.
- **Fuentes editables**: `tools/mob_remodel_rigs.py`, `mob_humanoid_rigs.py` y `remodel_mobs.py` generan capas Java y atlas propios de 128×128 en `textures/entity/remodel/`; originales conservados. `preview_mob_remodel.py` compone 20 vistas en `build/mob_remodel/todos_los_mobs.png`, revisadas con profundidad real y la misma geometría del juego.
- **Validación**: `auditMobModels` construye los 13 modelos reales (incluidos Vigía y Matriarca), comprueba enlaces, seis rodillas y 101 atlas; `auditMobModels runGameTestServer` termina con **43 pruebas requeridas superadas** y `BUILD SUCCESSFUL`. `check_models.py` correcto. Pendiente: revisar movimiento, brillo, equipo y variantes en `runClient`; no se abrió el juego. Los modelos se actualizan también en mundos existentes.

## 2026-11-11 — Codex — Correcciones visuales tras probar los mobs
- Quitado `cheekImplant`, el bloque de metal que sobresalía de la cara de todos los corruptos humanos. Se conservan los accesorios específicos de cada variante.
- Arañas corruptas normales y de cueva: añadido un cefalotórax continuo que une cabeza, abdomen y anclajes de las ocho patas. Los anclajes delanteros quedaban fuera del abdomen; ahora permanecen dentro del torso también durante la elevación de las patas. Sin cambios en IA ni colisiones.
- Torreta Centinela: estabilizadores y detalles encastrados a distintas alturas dentro del espesor de la placa; eliminadas las caras coplanares que provocaban parpadeo en la base.
- Cambios realizados en los generadores y regenerado `RemodeledMobLayers`. Vistas de araña, zombi y torreta revisadas; galería actualizada. `auditMobModels` compila y construye los 13 modelos y termina con `BUILD SUCCESSFUL`. Pendiente: confirmar en `runClient` la unión durante el movimiento y la ausencia de parpadeo al rodear la torreta; no se abrió el juego.

## 2026-11-11 — Codex — Arsenal remodelado, conservando la pistola
- Pedido: abrir IntelliJ y extender al arsenal el detalle visual que gustó en los mobs, exceptuando la Pistola Cinética. IDE abierta; no se abrió Minecraft.
- **19 armas/objetos de combate**: dos escopetas, rifle de cerrojo, semiautomático, subfusil, fusil de asalto, revólver, ametralladora ligera, francotirador, rifle de riel, lanzagranadas, lanzallamas, lanzacohetes, Minigun, cañón de impulso, arco voltaico, machete, cuchilla y granada cinética. Refuerzos y pasadores, placas de mecanismos, guardamanos con aberturas reales, jaulas térmicas, collares octogonales, recámaras individuales del lanzagranadas, aisladores y bobinas; hojas nuevas con filo escalonado y mango detallado. Granada con carcasa segmentada, espoleta, palanca y anilla.
- Fuentes: `weapon_remodel.py` aplica geometría antes de `gunkit` en `weapons_gen.py`; `--weapons-only` evita regenerar recursos ajenos. `remodel_weapons.py` genera y encuadra inventario por combinación de módulos, manteniendo el mismo encuadre entre sus fotogramas de recarga. `preview_weapon_remodel.py` transforma rotaciones JSON y usa profundidad real; láminas del arsenal y variantes en `build/weapon_remodel/`, revisadas visualmente.
- **Validación**: 93 modelos/fotogramas y 19 atlas válidos, sin caras coplanares; predicados de módulos/recarga/giro y transformaciones de agarre comparados contra Git y conservados. Pistola intacta; munición, módulos sueltos, nave y gadgets sin cambios. `check_models.py` correcto y `compileJava` con `BUILD SUCCESSFUL`. No se cambian estadísticas, IA, crafteos ni Java; no hizo falta repetir pruebas de servidor.
- Pendiente: probar en `runClient` disparo/apuntado, recargas, módulos y Minigun girando. Los recursos actualizan las armas de mundos existentes.

## 2026-11-11 — Codex — Modelos completos de armas desde cero
- El usuario aclara que quiere geometrías nuevas completas, porque el resultado anterior parecía una modificación de lo existente. Punto de retorno: **`d269d67`**, conservado en el historial, sin reescribirlo.
- `weapon_blueprints.py`: 19 funciones independientes que parten de un `Assembly` vacío. `weapons_gen.py` ahora delega sus armas a estas funciones; se eliminaron las geometrías antiguas y el parcheador `weapon_remodel.py`. Se reutilizan materiales y contratos de estados/agarre, sin importar ni extender modelos anteriores.
- Nuevas siluetas: escopeta artesanal con cañones superpuestos y culata de tubos/madera; corredera bullpup; rifle de cerrojo con culata abierta; semiautomático con pistón y bastidor oliva; PDW con barras telescópicas; fusil con chasis abierto; ametralladora con caja izquierda; francotirador con culata ajustable; riel con acelerador en horquilla y condensadores a un costado. Nuevos tambores, jaulas y cuerpos del lanzagranadas, revólver, lanzallamas, lanzacohetes y Minigun. Emisor de campana del cañón de impulso y dos electrodos del arco voltaico. Machete ancho, sable cinético asimétrico y granada segmentada también construidos de nuevo.
- Uniones revisadas: soportes de miras, puentes de bípodes, culatas, apoyo de hombro y radios que conectan los anillos del emisor. Inventario ajustado por variante; vistas del arsenal y recargas actualizadas en `build/weapon_remodel/` y revisadas. Copia local de la galería anterior: `arsenal_commit_d269d67.png`.
- **Validación**: 93 modelos/fotogramas y 19 atlas de potencia de dos, máximo 128 px; `check_models.py` correcto y comprobación de caras coplanares limpia en todos los modelos. Comparación con Git de predicados y transformaciones de agarre: conservados. Pistola y recursos ajenos intactos. `compileJava`: `BUILD SUCCESSFUL`. Solo arte/generadores; sin cambios de combate, registros ni datos de servidor.
- Pendiente: revisión del usuario en `runClient`, especialmente apuntado, recargas y giro. No se abrió el juego. Puede recuperarse el arsenal anterior revirtiendo este commit, conservando el historial.

## 2026-11-11 — Codex — Sonidos, bocas y retroceso del arsenal aprobado
- El usuario acepta los modelos de `f27edea`; se conservan todos sus JSON y texturas. Reporta corredera mezclada con el disparo, francotirador con sonidos fuera de orden, fuego desde el depósito y retroceso visual excesivo en automáticas.
- **Audio**: corregido el tiempo del último estampido Mosin (12,24 s; el recorte anterior empezaba en un cerrojo); estampidos de escopeta/rifle/francotirador recortados antes de los mecanismos grabados. Nuevos OGG de disparo empiezan directamente en el ataque. `guns/pump.ogg` es un recorte corto independiente, con registro, subtítulos ES/EN y variantes españolas sincronizadas; crédito CC0 añadido. `--feedback-only` reproduce estos cambios sin regenerar sonidos ajenos.
- **Secuencia**: `LongGunItem` programa cerrojo y corredera después del disparo, con hasta 10 y 6 ticks de separación. Consume el aviso una sola vez, cancela al cambiar de arma, morir o comenzar recarga y descarta avisos antiguos. Interrumpir una recarga para disparar ya no mete un sonido de cierre antes del estampido. No se cambian cadencias, munición ni daño.
- **Alineación**: centros reales de 16 armas largas/rangos y sus 44 variantes exportados desde los planos a `WeaponMuzzles.java`. Llamas, fogonazos, trazadoras, primer arco, estela del riel y onda de impulso usan salida común mediante un paquete visual servidor→cliente. Captura de la pose completa y del display real de primera persona, compensando las distintas proyecciones de manos y mundo; incluye apuntado, zoom, ambas manos, vibración y retroceso. Corregido el centrado de la mano izquierda. Cohete y granada del lanzador nacen en la salida física; la salida se limita ante paredes cercanas. Impactos/daño siguen decididos por servidor.
- **Automáticas**: SMG, fusil y ametralladora usan un impulso axial corto, sin el cabeceo y balanceo prolongados de las armas lentas. Minigun/lanzallamas conservan su vibración específica; escopeta, revólver y demás armas lentas conservan la animación amplia. Retroceso de cámara, habilidades, armaduras y dispersión no cambian.
- **Verificación**: compilación y `runGameTestServer` con **46 pruebas requeridas superadas**, `BUILD SUCCESSFUL`. Tres pruebas nuevas cubren orden real de eventos de sonido, cancelación de ciclos, interrupción de recarga, salida por mano/cabeceo, paredes y serialización de efectos. Auditoría sin ventana: 44 perfiles y **528 proyecciones** con ambas manos, cadera/mira y tres FOV; además siguen construyéndose los 13 modelos de mobs. `check_models.py` correcto, galería con bocas/ejes revisada en `build/weapon_muzzles/bocas_comprobadas.png`. Sin cambios en modelos/texturas aprobados ni recursos de la pistola.
- Pendiente: confirmar sensaciones y alineación en `runClient` tras reiniciarlo (ráfagas, cambios de FOV/apuntado y lanzallamas). No se abrió Minecraft. Protocolo de red del mod pasa a 2 por el nuevo paquete de presentación; cliente y servidor deben usar esta versión.

## 2026-11-11 — Codex — Altura de salida durante el retroceso de armas lentas
- El usuario aprueba automáticas y reporta salida extremadamente alta en ambas escopetas, cerrojo, semiautomático, riel, francotirador, revólver, lanzagranadas y, en mensaje posterior, lanzacohetes. Se mantienen modelos, retrocesos y ruta de efectos de las automáticas.
- Corregido el uso de una posición mundial guardada del fotograma anterior en esas nueve armas: con su retroceso amplio, el fogonazo y las trazadoras quedaban por encima del cañón al volver el arma. `HandMuzzleEffects` dibuja sus efectos locales en el mismo espacio de primera persona y con la pose del fotograma actual. `HandMuzzleGeometry` suministra punta y vértices compartidos entre captura, dibujo y auditoría. El extremo de la trazadora conserva el impacto autorizado del servidor, reproyectado para el FOV de las manos.
- La ruta nueva se limita al tirador local, primera persona sin visor telescópico y las nueve armas indicadas. Efectos de otros jugadores, automáticas, lanzallamas y otras armas conservan su ruta. Cola acotada a 64 avisos y efectos de hasta dos ticks, limpiada al salir o cambiar de mundo/arma. Sin cambios de red, física, daño, sonidos ni recursos de modelos.
- **Verificación**: `auditMobModels` compila y termina con `BUILD SUCCESSFUL`; 13 modelos de mobs, 44 perfiles, 528 proyecciones y nueva regresión de **1.056 fotogramas de retroceso**. Comprueba que los vértices que dibujan el centro del fogonazo y el inicio de la trazadora coincidan con la boca del modelo de cada fotograma, incluyendo recuperación y contragolpe, ambas manos y cadera/mira. Comparación geométrica revisada en `build/weapon_muzzles/retroceso_corregido.png`, generada por `preview_recoil_muzzles.py`. No se repitieron pruebas de servidor por ser un cambio de presentación solo en cliente.
- Pendiente: confirmar visualmente en `runClient` tras reiniciarlo, incluyendo ambos lanzadores; no se abrió el juego.

## 2026-11-11 — Codex — Recuperar la onda celeste del fogonazo local
- El usuario confirma alineación y proyectiles correctos, pero reporta cuadros/píxeles blancos en lugar de la onda celeste solo en las nueve armas del ajuste anterior.
- `HandMuzzleEffects`: fogonazo separado de la trazadora, con el anillo original `kinetic_ring.png` y material `entityTranslucentEmissive` que respeta el alfa. El modo aditivo de ojos sobre `kinetic_glow.png` dejaba visible el RGB blanco de sus texeles transparentes. Quitadas las dos pequeñas capas blancas del fogonazo anterior.
- Recuperados color cian, tamaño inicial/final, expansión y desvanecimiento de `KineticParticle.BURST` a escala .45 y siete ticks; la trazadora conserva su material, geometría y dos ticks. La onda sigue anclada a la boca del fotograma actual. Sin cambios de alineación, modelos, proyectiles, automáticas, sonidos, daño o servidor.
- **Validación**: `auditMobModels` con `BUILD SUCCESSFUL`, 13 modelos, 44 perfiles, 528 proyecciones y 1.056 fotogramas de retroceso correctos. Vista de la onda en cuatro edades revisada (`build/weapon_muzzles/onda_celeste.png`, generada por `preview_hand_muzzle_burst.py`), con alfa real y fondo visible alrededor del anillo. Cambio solo visual del cliente; no se abrió Minecraft. Pendiente: confirmar la onda en `runClient` tras reiniciarlo.

## 2026-11-11 — Codex — Partículas celestes de la estela local
- El usuario reporta que la bala todavía deja blanco en vez de partículas. La trazadora local conservaba `RenderType.eyes` y una textura de brillo estirada a todo el recorrido, aunque el fogonazo ya se había corregido.
- `HandMuzzleEffects`: trazadora con transparencia emisiva real y textura original `kinetic_glow.png`. Restituida una sucesión de partículas cian (.30/.88/1), dos puntos por unidad y máximo 513; su tamaño baja de .09 a .02 y se desvanece durante seis ticks, como las partículas del mundo. Se conserva mayor grosor en el riel. Fogonazo y trazadora tienen materiales y duración propios.
- `HandMuzzleGeometry.trail` suministra las posiciones entre la boca del fotograma actual y el impacto del servidor; se conserva la alineación aprobada. Sin cambios en onda de boca, automáticas, modelos, sonidos, física o servidor.
- **Validación**: `auditMobModels`, `BUILD SUCCESSFUL`; 13 modelos, 44 perfiles, 528 proyecciones y 1.056 fotogramas de retroceso, ahora verificando el centro de la primera partícula, el extremo en el impacto y el límite de la estela. Vista de partículas a tres edades revisada (`build/weapon_muzzles/estela_celeste.png`, generada por `preview_hand_tracer.py`), con transparencia y color celeste. No se abrió el juego; pendiente confirmar en `runClient` tras reiniciar.


## 2026-11-11 — Codex — Primera entrega del Nether, por partes
- El usuario pide poder probar cada parte antes de continuar. Se registra esta preferencia en `DISENO.md` y `PLAN_NETHER.md`; detenerse tras esta entrega y esperar sus comentarios.
- Activo en `main`: enemigos vanilla mejorados solo en el Nether (`NetherMobEvents`): piglins/brutos armados con hacha de netherita o rifle, hostilidad pese al oro con memoria de ira, zombificados y endermen hostiles, ghasts resistentes con bolas de potencia 3, blazes con abanico y esqueletos wither con avance anunciado. Brutos: 72 de vida, para reforzar sus 50 originales; piglins/zombificados: 36.
- Las mejoras no se acumulan al guardar/cargar y se restauran los atributos/arma al salir del Nether. Se limpian objetivos y memoria de ataque al pasar el jugador a creativo/espectador.
- Se conserva el avance mayor en la rama local `work/nether-por-etapas`, commit `49353b8`, sin activarlo en la entrega: biomas, estructuras, nuevos mobs, equipo, caza y jefes se retoman gradualmente. No mezclar esa rama entera ni duplicar eventos. El avance había pasado 50 GameTests y auditoría de 17 modelos.
- Validación de esta entrega: compilación y servidor GameTest (47 pruebas), incluidos oro/ira, brutos, persistencia y restauración, proyectiles del ghast, abanico del blaze, avance wither y creativo. `prepareRunClient` prepara IntelliJ sin abrir el juego. Se limpiaron los compilados del avance aplazado; las partidas del usuario no se han modificado. El mundo automático `run/world` puede advertir de registros del avance retirado, que solo se usó allí.
- Pendiente: el usuario prueba la primera parte en `runClient`, en su mundo existente. Ajustar el combate con sus comentarios antes de activar otra parte.

## 2026-11-11 — Claude — Revisión del avance de Codex y dos validaciones de red
- Revisado lo hecho por Codex desde `b840a62` (jefes Vigía y Matriarca, 5 estructuras, remodelado de mobs y arsenal, efectos de boca, primera parte del Nether). `runGameTestServer`: 47 pruebas; `check_models` y `check_zfight` limpios.
- Arreglados los dos fallos de validación que Codex dejó en PENDIENTES: el Rifle de Riel ya no dispara por `FirePistolPacket` (solo recarga cuando está vacío; el disparo va por `FireChargedPacket`, que mide la carga) y las automáticas nunca disparan dos veces en el mismo tick (minigun).
- Observaciones para el usuario (sin cambiar): piglins con Fusil de Asalto lo sueltan con 2,5 % (arma de tier 3 sin Procesador del Núcleo); piglins siempre hostiles = sin trueque; `NetherMobEvents` está sin comentarios ni formato del proyecto; la rama `work/nether-por-etapas` solo existe en local.

## 2026-11-11 — Claude — Conexión con Codex para los modelos
- Decisión del usuario: Codex hace modelos y arte; Claude, código y lógica. Probada la conexión con el chat de Codex del usuario (`codex exec resume`, respondió bien). `tools/ask_codex.sh` manda los encargos; reglas del reparto en `AGENTS.md` §3c.

## 2026-11-11 — Claude — Nether, parte 2: enemigos nuevos (Ácaro de Escoria y Centinela de Ceniza)
- Decisiones del usuario: activar los enemigos nuevos, que Codex remodele los mobs vanilla del Nether (encargo enviado por `ask_codex.sh`) y que los piglins sigan hostiles pero **sin soltar el Fusil de Asalto**.
- **Código nuevo y legible** (no se mezcló la rama `work/nether-por-etapas`; de ella solo se tomó el arte de Codex: `InfernalModel`, texturas, huevos, botín y `infernal_spawns.json`): `nether/NetherEntities` (registro, atributos, reglas de aparición), `nether/InfernalEntity` (ácaro cuerpo a cuerpo y saltarín; centinela que fija un punto con láser 18 ticks y dispara ahí, golpea de cerca; aliados entre sí) y `entity/client/InfernalRenderer`.
- `NetherMobEvents` reescrito con formato y comentarios del proyecto, mismo comportamiento de la parte 1; el fusil tiene probabilidad 0 de caer (también en piglins ya existentes; el hacha conserva 2,5 %). Nuevo: al excavar roca o minerales del Nether bajo Y 80, dentro de la roca, a veces sale un ácaro (1/18, espera de 12 s por jugador, máximo 6 cerca).
- Aparición natural en todo el Nether (`forge:add_spawns`): ácaros (peso 18, bajo Y 80) y centinelas (peso 12). Sueltan Azufre Industrial y Aleación de Escoria (materiales del futuro equipo del Nether, ya registrados). Huevos en la pestaña principal.
- GameTests `NetherEnemyGameTests`: aviso y disparo del centinela, alianza, ácaros al excavar (límite, sin aparecer en bloques ni al aire libre) y el fusil que nunca cae. 49 pruebas pasan.
- Ojo: `GuardianGameTests.watcherAttacks` depende del terreno de `run/world`; en un mundo de prueba recién creado (otra semilla) falló 3 de 3. Anotado en PENDIENTES.
