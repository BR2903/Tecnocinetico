# Nether por etapas

El usuario pidió el 2026-11-11 implementar y dejar probar **una parte cada vez**. Después de entregar una parte, esperar sus comentarios antes de continuar con la siguiente. No activar el overhaul completo de una sola vez.

## Primera entrega: enemigos existentes

- Piglins: 36 de vida; brutos: 72 de vida. Más armadura y daño; hacha de netherita o rifle de asalto. El rifle avisa con chispas y dispara tres tiros espaciados, seguidos de una pausa.
- Piglins y zombificados: hostiles aunque se lleve oro. Se conserva la memoria de ira para que la IA vanilla no cancele el ataque. Endermen hostiles al ver jugadores de supervivencia.
- Ghasts: 44 de vida y bolas de fuego con potencia explosiva 3.
- Blazes: 44 de vida y un abanico adicional de tres bolas, anunciado con llamas.
- Esqueletos wither: 48 de vida, más defensa/daño y avance rápido anunciado con partículas de fuego de alma.
- Cambios limitados al Nether. Al trasladar un enemigo mejorado fuera de él se restauran sus atributos y arma anteriores. No se seleccionan objetivos creativos/espectadores. Compatible con mundos existentes, sin regenerar terreno.

Probar en IntelliJ con `runClient`, entrando al Nether en supervivencia y con oro. Comparar piglins con hacha y rifle; comprobar ghasts, aviso/abanico de blazes y avance del esqueleto wither. También comprobar que fuera del Nether conservan su comportamiento habitual. No hace falta crear otro mundo para esta parte.

## Avance guardado para después

La rama local `work/nether-por-etapas`, commit `49353b8`, conserva el trabajo adelantado de biomas, estructuras, mobs nuevos, gadgets, armaduras y dos jefes. **No está activado en `main`.** Pasó 50 GameTests y auditoría de 17 modelos antes de separarlo; requiere revisión con el usuario y entregar sus componentes gradualmente.

Posibles siguientes partes, eligiendo con los comentarios del usuario: enemigos nuevos de túneles; biomas y estructuras; equipo de preparación; primer jefe; segundo jefe y recompensas. La idea global se mantiene: Nether industrial y biomecánico, dos jefes originales, mejora del caza y buenas recompensas. No mezclar la rama entera sobre `main` para introducir la siguiente parte: extraer solo lo acordado y evitar registrar dos veces los eventos de hostilidad.

## Parte 2 (2026-11-11, Claude): enemigos nuevos — entregada, pendiente de prueba del usuario

Ácaro de Escoria (18 de vida, túneles bajo Y 80 y al excavar roca) y Centinela de Ceniza (64 de vida, láser de aviso y disparo a un punto fijado). Código en `nether/`; modelos de Codex (`InfernalModel`). Los piglins ya no sueltan el Fusil de Asalto. En paralelo, Codex remodela los mobs vanilla del Nether (piglin, bruto, zombificado, ghast, blaze, esqueleto wither); Claude conecta los renderizadores (solo dentro del Nether).

Probar en `runClient`, en el Nether y en supervivencia: excavar netherrack en túneles hasta que salgan ácaros; buscar centinelas (o usar los huevos de la pestaña principal) y comprobar que el láser avisa y que moverse esquiva el tiro. Funciona en mundos existentes (aparecen en chunks ya generados).

Siguientes partes posibles: biomas y estructuras; equipo de preparación; primer jefe; segundo jefe y recompensas.

Mobs vanilla remodelados (Codex, conectados por Claude): piglin, bruto, zombificado, ghast, blaze y esqueleto wither cambian de aspecto solo dentro del Nether. Comprobarlos también en `runClient` (animaciones, armas en la mano, bebés piglin, armadura).

## Parte 3 (2026-11-12, Claude): biomas y estructuras — entregada, pendiente de prueba del usuario
Cinco biomas del Nether industrial (mundos nuevos tipo Postapocalíptico), depósitos que decoran cualquier Nether en chunks nuevos, Depósito de Refrigeración y Puesto de Vigilancia (con centinelas). Probar: `/locate biome tecnocinetico:sulfur_pits` (y los otros cuatro) y `/locate structure tecnocinetico:cooling_depot` / `infernal_watchpost` en un mundo nuevo.

Siguientes partes: equipo de preparación (refrigerador térmico, filtro de ceniza, botas de anclaje, piroxita reforzada); primer jefe (Fundidor del Abismo con su fundición); segundo jefe (Devorador de Escoria con su excavación) y recompensas.

## Parte 4 (2026-11-12, Claude + Codex): equipo de preparación — entregada, pendiente de prueba del usuario
El usuario pidió peligros de ambiente (calor y ceniza tóxica) y, a más dificultad, más recompensa.
- **Calor** (`nether/NetherHazards`, 0-100 por jugador, una vez por segundo en el servidor): +0,6/s en todo el Nether, más en Forjas de Escoria (+1), Pozos de Azufre (+0,6), Páramos de Obsidiana (+0,4), cerca de lava (hasta +1,5) y ardiendo (+3); bosques y valle de almas -0,2. 70 = lentitud; 85 = debilidad y cansancio; 100 = 2 de daño cada 2 s (`tecnocinetico:overheat`, ignora armadura). Baja fuera del Nether (4/s) y en el agua.
- **Ceniza**: +1,2/s en Pozos de Azufre, Bosque Carbonizado, Canteras de Basalto, deltas de basalto y valle de almas. 50 = debilidad; 80 = hambre y tos; 100 = daño (`tecnocinetico:ash_lungs`).
- HUD: `client/HazardHud` (barras naranja y gris a la derecha de la barra rápida); `ModNetwork.HazardPacket` (id 17) solo al jugador; `nether/ClientHazardData`.
- Equipo (gadgets del Equipo Táctico, batería = durabilidad): Refrigerador Térmico (espalda: frena el calor, enfría 3/s y apaga llamas; gasta 1 + calor), Filtro de Ceniza (cara), Botas de Anclaje (pies: -85 % empuje, -75 % caída). Recargas: Carga de Refrigerante, Cartucho Filtrante, Célula de Anclaje. Recetas de mesa con materiales del Overworld (nieve, cobre, plomo, carbón vegetal, pistón...). El traje antirradiación completo también filtra la ceniza.
- Armadura: piroxita -20 % de calor por pieza; **Piroxita Abisal** (Ensamblador de Armas: pieza de piroxita + aleación de escoria + lingotes de piroxita + azufre) -25 % por pieza y completa inmune al calor; piroxita y abisal mezcladas cuentan como set de fuego.
- Recompensas: enemigos mejorados del Nether dan el doble de experiencia y, si los mata un jugador, piroxita en bruto (15 %), azufre (25 %), oro y (brutos y esqueletos wither) aleación de escoria (30 %); los cofres de las estructuras traen recargas, piroxita y a veces chatarra de netherita.
- Arte de Codex (`tools/nether_gear_art.py`, `nether_gear_rig.py`, `preview_nether_gear.py`): objetos, recargas planas, armadura y equipo puesto (`WornGearModel` partes `cooler`/`filter`/`anchor`, atlas `worn_gear.png` 128x128).
- GameTests `NetherHazardGameTests` (4). Probar en `runClient`: entrar al Nether en supervivencia, ver subir las barras, ir a Forjas de Escoria / Pozos de Azufre, probar el equipo y la armadura.

Siguientes partes: primer jefe (Fundidor del Abismo con su fundición); segundo jefe (Devorador de Escoria con su excavación) y recompensas.
