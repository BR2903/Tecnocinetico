# End por etapas

Como el Nether: **una parte cada vez**, esperando los comentarios del usuario antes de la siguiente.

## Lore (usuario, 2026-11-14)
El End fue la sede de los servidores del Núcleo Directriz; tras la Singularidad la IA se mudó a otro planeta (extremadamente hostil, para más adelante). En el End quedan restos abandonados de su red que **corrompen** a las criaturas: implantes, cables, glitches de datos (magenta/violeta; el cian es solo de la cinetrita).

## Llegada y Dragón (decidido por el usuario, 2026-11-14; hecho en la parte 2)
- Al llegar **no se ve al dragón** y las **torres de los cristales están destruidas**.
- Al avanzar hacia el centro, **todo empieza a temblar y a corromperse**; después sale el **Dragón del End modificado** por la IA (implantes, más duro, botín propio, p. ej. el Acoplador del End para el caza).
- Aparece **un enemigo por cada cristal**: hacen lo mismo que los cristales (curan/protegen al dragón) pero además **te atacan**.
- Peligros ambientales del End: sin decidir (el usuario los dejó para luego).

## Parte 1 (2026-11-14, Claude + Codex): enemigos existentes corruptos — entregada, pendiente de prueba del usuario
Código en `end/EndMobEvents` (solo dentro del End; fuera vuelven a ser vanilla, guardando sus valores en NBT `EndCorrupted`/`EndOriginal*`).
- **Enderman**: 60 de vida, 6 de armadura, 10 de daño. Hostil al ver a un jugador en supervivencia a menos de 16 bloques (la armadura completa de Nulita lo evita). **Salto de desfase**: si el objetivo está a más de 4 bloques, 1 s de glitches + mirada (aviso) y aparece a su espalda golpeando.
- **Shulker**: 45 de vida. **Ráfaga**: cada 6 s, aviso de glitches y 3 balas a la vez.
- **Endermite**: 16 de vida, 4 de daño.
- Partícula propia `data_glitch` (`client/GlitchParticle`): píxeles rotos magenta/violeta que saltan de sitio y parpadean.
- Modelos corruptos (Codex, `EndRemodelLayers`) solo en el End (`EndRemodelRenderers`).
- Prueba `EndMobGameTests.endCorruptedMobs`.

Siguientes partes posibles: enemigos nuevos de los restos de la red; biomas y estructuras (torres destruidas, servidores abandonados); llegada y Dragón modificado con sus enemigos-cristal; peligros y equipo.

## Parte 2 (2026-11-14, Claude + Codex): llegada y Dragón modificado — entregada, pendiente de prueba del usuario
Elecciones del usuario: el evento empieza al acercarse al centro; torretas-repetidor en las torres; los 4 ataques nuevos; botín con Acoplador del End, mucha nulita, mejora de equipo y pieza del siguiente paso.
- `end/EndArena` + `EndArenaData` (estado por mundo: DORMANT → AWAKENING → FIGHT → DEFEATED). La pelea vanilla (`EndDragonFight`) se controla con un access transformer (`META-INF/accesstransformer.cfg`: `dragonEvent`, `dragonKilled`, `dragonUUID`).
- **Llegada**: sin Dragón ni barra (se marca "matado" mientras duerme; se borra un Dragón vanilla que existiera). Las 10 torres se rompen en cuanto su chunk está cargado (punta cortada irregular, sin cristal ni jaula, escombros de obsidiana al pie).
- **Despertar** (15 s) al acercarse un jugador en supervivencia a < 50 bloques del centro: temblor de cámara (`EndQuakePacket`, `client/EndClientEffects`), latidos y explosiones sordas, la Piedra del End de la superficie se corrompe en círculos crecientes (`corrupted_end_stone`) y rugido.
- **Dragón del End Corrupto**: sale de debajo de la isla; 400 de vida con dureza en grupo (÷ 1 + 0,5 por jugador extra, máx. ×3). Textura propia de Codex que sustituye la vanilla (solo existe este Dragón).
- **Torretas-repetidor** (`EndRelayTurretEntity`, modelo de Codex): una por torre; la más cercana cura al Dragón 1 cada 10 ticks con el haz del cristal; disparo cargado de 8 con línea de aviso que se fija (moverse lo esquiva); al morir explotan y, si las mató un jugador, el Dragón recibe 10.
- **Ataques**: aliento corrupto (las nubes del aliento y de las bolas dan Corrupción de datos: 1,5/s que atraviesa la armadura + glitches en pantalla); Pulso EMP al posarse (empuja, 4 de daño y apaga los gadgets 6 s salvo los Mk III); Lluvia de datos cada 14 s en vuelo (marca el suelo bajo hasta 3 jugadores, 1,5 s después impacto de 10 que reduce la armadura); a mitad de vida llama 2 endermen corruptos por jugador (máx. 6) y repara 2 torretas.
- **Botín** (junto al portal, invulnerable, uno por jugador que luchó, máx. 4): Acoplador del End, Núcleo de Datos Corrupto, Núcleo de Navegación y 16 lingotes de nulita. Huevo y portal vanilla.
- **Acoplador del End**: se instala en el Ensamblador Industrial en un caza que ya lleve el del Nether (+12 nulita, 1 núcleo de datos, 2 procesadores): salto al End.
- **Mk III** de los gadgets del Nether (Mk II + Núcleo de Datos en la mesa): triple batería, inmunes al EMP; el Filtro de Ceniza Mk III filtra la corrupción de datos.
- **Núcleo de Navegación**: ruta al planeta de la IA (sin uso todavía).
- Pruebas `EndArenaGameTests` (torre rota, despertar → Dragón y torretas, curación, EMP, lluvia, botín; acoplador y Mk III).

## Parte 3 (2026-11-14, Claude + Codex): enemigos nuevos de las islas exteriores — entregada, pendiente de prueba del usuario
Elecciones del usuario: Gusano de Datos y Espectro de Datos, solo en las islas exteriores, más duros que el enderman corrupto; en todos los textos "corrupto" en vez de "corrompido".
- **Gusano de Datos** (`end/DataWormEntity`, 110 de vida, 10 de armadura): bajo tierra es invulnerable y va hacia el jugador más cercano (32 bloques, sin necesidad de verlo) dejando polvo y glitches en la superficie; solo avanza bajo piedra del End o tierra blanda. Aviso de 0,7 s (el suelo tiembla bajo el objetivo) y **embestida** en arco: cabeza 14, cuerpo 6 al pasar. Si cae sobre algo que no puede excavar (obsidiana, bloques puestos) queda **aturdido** 3 s y recibe ×1,5. **6 segmentos + cola** que se golpean por separado (24/16 de vida; el golpe pasa la mitad al gusano; al romperse +12 y se acorta). Suelta 1-3 nulita en bruto y piedra del End.
- **Espectro de Datos** (`end/DataSpecterEntity`, 70 de vida): proyección de luz sólida alrededor de un emisor; **desfasado** nada le da (las balas lo atraviesan), ronda al jugador y salta de sitio. Se **materializa** 1 s para atacar (ya se le puede dar): **garras** a distancia (línea de aviso, 12 + Corrupción de datos) o **pulso** de cerca (anillo de aviso, 9 + Corrupción en 4,5 bloques). Luego queda **expuesto** 2 s (×1,5). Suelta perlas y, raro (6 %, +2 % por botín), un Núcleo de Datos Corrupto.
- Aparecen en tierras altas, medias, islas pequeñas y yermos del End, a más de 600 bloques del centro.
- Pruebas `EndEnemyGameTests` (excava/sale/se rompe un segmento/vuelve a hundirse; aturdido en obsidiana; espectro intangible, pulso y expuesto; aparición solo fuera de la isla central).

