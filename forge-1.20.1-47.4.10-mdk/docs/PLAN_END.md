# End por etapas

Como el Nether: **una parte cada vez**, esperando los comentarios del usuario antes de la siguiente.

## Lore (usuario, 2026-11-14)
El End fue la sede de los servidores del Núcleo Directriz; tras la Singularidad la IA se mudó a otro planeta (extremadamente hostil, para más adelante). En el End quedan restos abandonados de su red que **corrompen** a las criaturas: implantes, cables, glitches de datos (magenta/violeta; el cian es solo de la cinetrita).

## Llegada y Dragón (decidido por el usuario, 2026-11-14; parte futura)
- Al llegar **no se ve al dragón** y las **torres de los cristales están destruidas**.
- Al avanzar hacia el centro, **todo empieza a temblar y a corromperse**; después sale el **Dragón del End modificado** por la IA (implantes, más duro, botín propio, p. ej. el Acoplador del End para el caza).
- Aparece **un enemigo por cada cristal**: hacen lo mismo que los cristales (curan/protegen al dragón) pero además **te atacan**.
- Peligros ambientales del End: sin decidir (el usuario los dejó para luego).

## Parte 1 (2026-11-14, Claude + Codex): enemigos existentes corrompidos — entregada, pendiente de prueba del usuario
Código en `end/EndMobEvents` (solo dentro del End; fuera vuelven a ser vanilla, guardando sus valores en NBT `EndCorrupted`/`EndOriginal*`).
- **Enderman**: 60 de vida, 6 de armadura, 10 de daño. Hostil al ver a un jugador en supervivencia a menos de 16 bloques (la armadura completa de Nulita lo evita). **Salto de desfase**: si el objetivo está a más de 4 bloques, 1 s de glitches + mirada (aviso) y aparece a su espalda golpeando.
- **Shulker**: 45 de vida. **Ráfaga**: cada 6 s, aviso de glitches y 3 balas a la vez.
- **Endermite**: 16 de vida, 4 de daño.
- Partícula propia `data_glitch` (`client/GlitchParticle`): píxeles rotos magenta/violeta que saltan de sitio y parpadean.
- Modelos corrompidos (Codex, `EndRemodelLayers`) solo en el End (`EndRemodelRenderers`).
- Prueba `EndMobGameTests.endCorruptedMobs`.

Siguientes partes posibles: enemigos nuevos de los restos de la red; biomas y estructuras (torres destruidas, servidores abandonados); llegada y Dragón modificado con sus enemigos-cristal; peligros y equipo.
