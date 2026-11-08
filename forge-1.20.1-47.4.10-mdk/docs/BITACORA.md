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
