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
