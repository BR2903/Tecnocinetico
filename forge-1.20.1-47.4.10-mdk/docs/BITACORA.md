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
