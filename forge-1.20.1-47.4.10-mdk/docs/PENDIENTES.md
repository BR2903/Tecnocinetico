# Pendientes, ideas y limitaciones

## Por verificar con el usuario
- Prensa Automática alimentada por un **alternador real de Create Crafts & Additions** (solo se probó con la Fuente de Energía Creativa / sin pruebas a fondo).
- Ajustes de equilibrio de habilidades (XP por acción) tras jugar un rato.

## Ideas propuestas (el usuario elige el orden)
- **Ruinas antiguas**: estructuras tecnológicas con Aleación Antigua (sin glifos ni magia), Cuerpos de Enjambre de guardia, cofres con módulos/piezas (encaja con el lore).
- **Equipo de Inercita**: herramientas y armadura de nivel 2 (diseño realista + detalles del mod).
- **Más armas realistas** (p. ej. rifle cinético) con sus propios módulos en el banco.
- Más habilidades (el sistema está preparado: ver `Skill.java`).
- Materiales del lore aún no implementados: Radionita, Crioprisma (¿para las dimensiones nuevas de los tiers 4-5?); Núcleo Directriz (jefe final).
- Tiers 4 y 5: 3 dimensiones nuevas (por diseñar).

## Limitaciones conocidas / deuda técnica
- **Equilibrio del mundo**: la Inercita (nivel diamante) aparece en 9 vetas de 7 por chunk → más común que el diamante. Cinetrita: 14 vetas de 9. Revisar `worldgen/placed_feature`.
- **Receta de Aleación Antigua** usa 8 hierro + cinetrita, pero el lore dice que el hierro "se deformaba" con la cinetrita → quizá debería llevar Inercita.
- La Prensa Automática no sincroniza su inventario con el cliente fuera del menú (no hace falta salvo que se añada un renderizador de los objetos).
- Las bonificaciones de la pistola no se recalculan si se cambia de módulos durante una recarga (la recarga en curso termina con su duración original).
- `run/` contiene mundos de prueba con objetos que ya no existen (objetos "Vista 1-5"): el aviso de "registros no identificados" al cargarlos es inofensivo.
- Si se añade un 5.º módulo, `pistol_gen.py` generaría 160 modelos (2^5 × 5). Valorar un modelo dinámico (BakedModel propio) si crecen.

- Ajustar en el juego la posición en primera persona de la escopeta y el rifle si hace falta (`LongGunClientExtensions`).

- Drones: ya patrullan las antenas repetidoras. Ajustar su dificultad en el juego si hace falta (los valores actuales están en `DroneEntity`).

- **Nuevas estructuras del Overworld**: probar la integración con el terreno y la frecuencia de antenas, centros de datos, búnkeres, laboratorios y ciudades sumergidas en chunks nuevos. Autopistas/vías continuas y torres caídas siguen siendo una fase de infraestructura posterior.
- **Tres jefes del Overworld**: dificultad Araña < Vigía < Matriarca; probar en grupo los nuevos valores, condensadores traseros del Vigía y Matriarca sin debilidad (ya no existe la exposición automática de 24 ticks). Modelos rediseñados y previsualizados; comprobar articulaciones y contraste en el juego. Usan sonidos propios ya existentes del mod.
- Tras probarlos el usuario: la dificultad base de la Matriarca gusta; ahora tiene crías con límite de seis. Revisar en juego frecuencia/cantidad de las oleadas. Volver a probar el Vigía en supervivencia: vuelo continuo y retirada, cuatro patrones de ataque y avisos que permiten esquivarlos; ya no queda detenido a altura de espada al cargar ni dispara toda la ráfaga al mismo punto.
- **Crafteo del caza**: sigue pendiente. Ya están las tres piezas exclusivas garantizadas: Motor Dimensional (Araña), Propulsor Vectorial (Vigía), Núcleo de Vuelo Adaptativo (Matriarca). Diseñar su receta en el Ensamblador Industrial cuando se pida.

- Corruptos: decidir si también se sustituyen aldeanos zombi y otros mobs; sonidos propios (ahora usan los vanilla con tono más grave).

- Tier 2: módulos propios del subfusil; animación de bombeo de la corredera en primera persona; probar el equilibrio (daño del subfusil, freno de la inercita, carga del exoesqueleto).


- Caza del Núcleo: receta (¿fin de juego, con piezas de las dimensiones nuevas?), probar el manejo en el juego (giro, velocidades) y en multijugador; animación de cabina/HUD más elaborada si gusta.

- Armas: probar el equilibrio de las 8 nuevas en el juego; sonidos propios (lanzallamas, minigun, arco); cañones de la minigun que giren en el modelo. Habrá tiers 4 y 5 (dimensiones nuevas).
- Indicador en pantalla del enfriamiento del Impulsor de Esquiva (`DashClient.cooldown` ya existe); verlo puesto en el personaje.

- **Nave mejorable (idea del usuario, no implementar hasta que lo pida):** se fabrica con el Motor Dimensional (botín de la Araña de Asedio); primero solo por tierra y para viajar al siguiente mundo nuevo; vencer al jefe del Nether desbloquea saltar al Nether, y al del End, al End. El Caza del Núcleo actual vuela y salta libremente: habrá que adaptarlo.

- (Pedidos de máquinas terminados: metales en recetas y prensa estilo Create hechos.) + Prensa Automática estilo Create. Detalle: **reactor generador** de FE (combustible de uranio, sobrecalentamiento), **rediseño de la Prensa Automática** estilo Create, e **integrar plomo/estaño/titanio** en recetas existentes (balas, circuitos/soldadura, armaduras y armas de tier 2-3).
- Radiación: hay traje; ¿caja de plomo para guardar uranio sin que irradie? Equilibrio de la Cámara (FE, tiempos, pérdida de estabilidad) tras probarla.
- Reactor: probar el equilibrio en el juego (600 FE/t, duración de la barra, calor). ¿Modelo 3D propio? ¿Multibloque más adelante?
- Estructuras grandes: probar en el juego (despliegue, desmontaje, brazos y barras visibles). Más recetas para el Ensamblador Industrial (nave mejorable, tier 4-5).

## De la revisión externa (2026-11-10), sin arreglar: decide el usuario
- ~~**World presets vanilla sobrescritos**~~ RESUELTO 2026-11-12 (tipo de mundo propio "Postapocalíptico"): (`data/minecraft/worldgen/world_preset/`): chocan con otros mods de worldgen (Terralith, BoP). Alternativa: TerraBlender o un preset propio.
- ~~**Licencia contradictoria**~~ RESUELTO 2026-11-12 (CC BY-NC-SA 4.0, `LICENSE.txt`):: `mod_license=All Rights Reserved` pero `LICENSE.txt` es el LGPL de Forge del MDK. Elegir una.
- ~~**README.txt / CREDITS.txt**~~ RESUELTO 2026-11-12 (`README.md` en la raíz; README/CREDITS del MDK reemplazados): del MDK: falta un README del mod.
- **Tamaño del repo** (DECIDIDO 2026-11-12: se quedan en el repo):: `texturas_originales_backup/` (21 MB) y `tools/sound_sources/` (7 MB): Git LFS o fuera del repo.
- **Prensa**: receta de fragmentación (cristal solo) es subconjunto de Transductor y Servo; con tolvas, si falta el cobre los cristales se fragmentan.

## Pruebas frágiles
- `GuardianGameTests.watcherAttacks` (y probablemente otras que usan el heightmap) dependen del terreno del mundo de prueba `run/world`, que no está en git y tiene semilla al azar: en una copia nueva del repo puede fallar. Fijar `level-seed` para el servidor de pruebas o montar el suelo en la propia prueba.

## ~~Bloqueo de compilación reportado por Bryan (2026-11-11, para Claude)~~ — RESUELTO (2026-11-12)
- Resuelto: era la integración de los sonidos del taladro a medias (la sesión se cortó a mitad). Registrados en `ModSounds` y `sounds.json`; compila y pasan las pruebas.
- Nota original: `compileJava` falla en `client/DrillSounds.java:90,103,104`: usa `ModSounds.DRILL_REV`, `DRILL_IDLE` y `DRILL_LOOP`, pero no existen en `sound/ModSounds.java`. El cliente y los audios `sounds/drill/` est?n sin seguimiento: parece una integraci?n todav?a incompleta. Completar los tres registros y sus entradas en `sounds.json` apuntando a los OGG adecuados; comprobar despu?s `compileJava` y `runGameTestServer`. Codex solo diagnostic?; no modific? Java ni los sonidos. El aviso de Gradle 9 no causa este fallo.

## Pendiente de revisión del usuario en el juego (2026-11-12)
Bryan lo probará más adelante; no dar por aprobado hasta entonces.
- **Taladro de Combustión**: encuadre en primera/tercera persona, animaciones (taladrar sin golpe de brazo, vibración, giro de la broca, repostaje de 1,5 s) y **sonidos** (ralentí, taladrado, arranque, mordisco, repostaje, ahogo; volumen y si el bucle se nota).
- **Munición en paquetes y cargadores**: modelos de Codex, llenar con R, recarga de cada arma desde su cargador, revólver con 1 paquete, escopetas con paquetes abiertos, cinta y cajas de minigun/ametralladora, recetas de pólvora (mesa, Refinador, Laboratorio). Confirmar la interpretación de "francotiradores con las mismas balas y cargadores" (Rifle de Riel = Balas de Rifle + Peine; Balas de Riel obsoletas).
- **Nether parte 2**: Ácaro de Escoria, Centinela de Ceniza, mobs vanilla remodelados (solo en el Nether), todos los piglins armados.
- **Overworld arrasado (2026-11-12)**: vegetación mitad y mitad, densidad de restos y edificios, llanuras = Megaciudad Muerta, farolas y postes. Probar en un mundo NUEVO de tipo "Postapocalíptico" (o chunks nuevos). Ajustar densidades si se pasa o se queda corto.

## Overworld arrasado: lo que falta de la lista acordada (2026-11-12)
- ~~Carreteras y autopistas rotas~~ HECHO (pendiente de revisión en el juego).
- ~~Vehículos abandonados~~ ELIMINADOS (usuario, 2026-11-12: "se ven bien pero no los quiero").
- ~~Ambiente gris y ceniza~~ HECHO (pendiente de revisión).
- ~~Criaturas~~: animales vanilla se quedan (usuario); patrullas nocturnas de drones HECHO (pendiente de revisión: frecuencia y dificultad). Sin vehículos volcados ni atascos (usuario).
- Cabeza de farola tumbada (Codex: hace falta otra orientación para que la cabeza caída quede de lado).
- **Nether parte 3 (2026-11-12)**: biomas del Nether industrial, depósitos, Depósito de Refrigeración y Puesto de Vigilancia. Probar en un mundo nuevo Postapocalíptico.
