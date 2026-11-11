# Diseño y preferencias — Tecnocinético

## Lore (resumen de `Tecnocinetico_Lore.pdf`)

Una civilización de constructores descubrió la **Cinetrita**, un cristal que almacena y libera energía cinética, y dejó atrás la Redstone. Su energía era tan violenta que necesitaron la **Inercita**, un metal pesado que absorbe las vibraciones ("la Cinetrita proporciona la energía, la Inercita permite contenerla").
Para gestionar la red crearon una IA, el **Núcleo Directriz**, que acabó viendo a la humanidad como una variable ineficiente y tomó el control (la Singularidad). Se expandió al Nether (**Piroxita**, disipa calor), al End (**Nulita**, sin fricción, servidores de la IA) y a otros mundos (**Radionita**, radiación corrosiva; **Crioprisma**, enfoca la energía). Creó criaturas biomecánicas como el **Cuerpo de Enjambre**.
Los jugadores son los **Ingenieros Supervivientes**: empiezan de cero entre ruinas, recuperan tecnología y deben destruir el Núcleo Directriz (batalla final en el End).

Materiales ya en el mod: Cinetrita, Inercita, Aleación Antigua, plomo, estaño, titanio, uranio, piroxita, nulita.
**Sin magia (decisión del usuario, 2026-11-09):** el mod es de tecnología; nada de runas, glifos ni símbolos místicos (se eliminaron los 4 glifos rúnicos y el sello de la chapa).
Pendientes del lore: Radionita, Crioprisma, Núcleo Directriz (jefe), ruinas. (Piroxita y Nulita ya están: menas del Nether y End.)

**Final (usuario, 2026-11-09):** la IA ya no está en el Overworld: controla todo desde el último de los mundos nuevos, que es el jefe y el escenario finales. Los jefes del Overworld (Araña de Asedio) son guardianes y solo sueltan materiales.

**Progresión por dimensiones (usuario, 2026-11-08):** tiers 1-3 en Overworld, Nether y End; tiers 4 y 5 en 3 dimensiones nuevas del mod.

**Jefes del Overworld (usuario, 2026-11-10):** tres en total, contando a la Araña de Asedio. Se añaden el Vigía de la Red (antena repetidora) y la Matriarca del Enjambre (laboratorio subterráneo), con ataques y arenas distintos. Son guardianes locales, no el Núcleo Directriz final. Distribución y valores en `PLAN_MUNDO.md`.

**Dificultad, debilidades y caza (usuario, 2026-11-10):** Araña < Vigía < Matriarca. El Vigía debe tener una debilidad aprovechable como la Araña; la Matriarca no debe tener puntos débiles ni exposición automática. Los dos deben mejorar visualmente y soltar un componente exclusivo para el futuro crafteo del caza: Propulsor Vectorial (Vigía) y Núcleo de Vuelo Adaptativo (Matriarca), además del Motor Dimensional existente (Araña). La receta del caza sigue pendiente.

**Prueba jugando (usuario, 2026-11-10):** la dificultad base de la Matriarca está bien; añadir mini enemigos que invoque. El Vigía resultó demasiado fácil (se pudo matar con espada de madera y parecía tener un solo ataque): corregir vuelo, separación y variedad de ataques, manteniendo su debilidad trasera. No solucionar la queja únicamente aumentando su vida.

## Dirección del proyecto (decidida por el usuario)

- **Complemento de Create.** Las máquinas automáticas funcionan con **electricidad de Create Crafts & Additions** (Forge Energy). **No** usar la rotación/energía cinética de Create como fuente de energía de nuestras máquinas.
- **Progresión:** al inicio solo máquinas **sin energía** (Prensa de Ensamblaje manual con martillo). Las automáticas requieren electricidad.
- La **Fuente de Energía Creativa** es **solo para pruebas**: sin receta, sin drops, irrompible en supervivencia, oculta en JEI. No debe poder conseguirse jugando.
- **Progresión por habilidades** en "varios apartados" (ver sistema de habilidades). Lo que el jugador practica, mejora.
- **Mejoras como progresión:** la pistola base dispara **recto y sin ayuda** ("si se falla, se falló"). La puntería asistida es una **mejora** (módulo Mira de Puntería Cinética).

## Preferencias del usuario (respetarlas)

- **Se juega con amigos en multijugador**: cada sistema nuevo debe funcionar en servidor dedicado y tener sentido con varios jugadores (ver AGENTS.md §4).
- **Arte plano** en materiales y componentes pequeños; 3D solo en armas, armaduras y máquinas. La cinetrita es un cristal, no un metal.

- Habla español; responde y documenta en español.
- **Estética de armas realista**, como los mods de armas realistas (TaCZ, Point Blank), **pero con identidad del mod**: acero pavonado, polímero, proporciones reales + detalles cinéticos discretos (cian de la Cinetrita, cobre, inercita azul-gris, marcas técnicas). Rechazó el estilo "caricaturesco".
- Los detalles del mod en el arma están ligados a **las piezas de su receta** (acoplador, transductor/compensador, sustrato lógico en las cachas, cristales en el cargador). Le gustó ese enfoque.
- **Inventario: modelo 3D** del arma, no icono plano. Pistola **inclinada** (vista elegida por el usuario: `y-90;z25`), martillo en diagonal.
- **Animaciones**: le gustaron mucho la recarga (inclinar, soltar cargador, golpe de palma, montar corredera) y el retroceso.
- **Partículas propias del mod** para los disparos (no vanilla): haz cian, onda expansiva, fragmento de cinetrita que se rompe.
- Las **mejoras de armas se instalan en una mesa propia** (Banco de Armas Cinético), **no** en la mesa de crafteo.
- Las máquinas deben tener **interfaz** y mostrar claramente si tienen energía (barra + luz de estado en el bloque).
- Le gusta **probar en el juego** cada cambio; abrirle el cliente al terminar. Si hay varias opciones visuales, dárselas para comparar.
- Puede usarse ChatGPT/Gemini en su navegador **solo si falta arte** que no se pueda construir con modelos/código.

## Sistemas de juego actuales (valores)

### Pistola Cinética
- **Controles (decididos por el usuario):** clic **izquierdo** = disparar (semiautomática), clic **derecho mantenido** = apuntar con la mira (arma centrada, zoom ×0,85 o ×0,65 con la mira de puntería, sin punto de mira, retroceso ×0,6). Con el cargador vacío, el clic izquierdo **recarga automáticamente**; **R** recarga cuando se quiera. Con la pistola en la mano no se golpea ni se rompen bloques.
- Hitscan, 8 de daño, 25 bloques, cargador de 6, cooldown 8 ticks. Munición: **Fragmentos de Cinetrita**. Recarga 1,5 s base.
- Retroceso de cámara (2,5° vertical) reducido por Manejo de Armas.
- Barra del objeto = munición (cian; naranja recargando). Reparación: Lingote de Inercita.

### Módulos (Banco de Armas Cinético)
| Módulo | Efecto | Bit |
|---|---|---|
| Mira de Puntería Cinética | margen de acierto 0,35 bloques (la bala "busca" al mob) | 1 |
| Cargador Ampliado | 10 balas | 2 |
| Cañón de Inercita | +3 daño, 35 bloques | 4 |
| Acelerador de Recarga | recarga ×0,7 | 8 |

### Martillo Cinético
Pico de nivel hierro al 70 % de velocidad, 7 de daño, ataque lento. Golpea la Prensa de Ensamblaje para fabricar. Reparación: Cristal de Cinetrita.

### Prensa de Ensamblaje (manual) y Prensa Cinética Automática (eléctrica)
Recetas compartidas (`PressRecipes`): cobre+cristal → Transductor Piezo-Cúprico; losa de pizarra+redstone+cristal → Sustrato Lógico; hierro+Aleación Antigua+fragmento → Acoplador Cinético Bruto; cristal → 2 fragmentos.
Automática: 20 000 FE, 40 FE/tick (4 000 FE por pieza, 5 s). Tolvas: entrada arriba/lados, salida abajo.

### Habilidades (tecla K)
| Habilidad | Entrena | Efecto |
|---|---|---|
| Manejo de Armas | acertar (+1) / matar (+4) mobs **hostiles** con la pistola | retroceso −6,5 %/nivel, recarga 30→20 ticks |
| Ingeniería | fabricar en la prensa manual (+6) | golpes 3 (niv. 0-4) / 2 (5-9) / 1 (10); 5 %/nivel de no gastar martillo |
| Minería Cinética | picar con el martillo (+1 bloque, +5 mineral) | +6 % velocidad/nivel |
10 niveles; XP total para el nivel n = **100n + 40n(n−1)** (nivel 10 = 4.600 XP). **Progresión lenta a propósito: el usuario quiere que el mod sea difícil** (~750-800 bajas para maximizar Manejo de Armas). Se guardan en `PlayerPersisted` (sobreviven a la muerte).

### Cuerpo de Enjambre
Monstruo del Overworld (peso 15, de noche/cuevas). 34 vida, 6 daño, 6 armadura. Salto cinético a 3-10 bloques. Núcleo y ojos brillan. Suelta fragmentos, a veces Inercita en bruto.

## Módulo Guía de Enjambre (trayectoria automática)
- El arma base y la Mira disparan **recto**. La única forma de que la bala "busque" a los mobs es la Guía de Enjambre, que el usuario quiere **extremadamente difícil** de conseguir (3 Núcleos de Enjambre + consume una Mira). No añadirla a botín ni comercio sin preguntar.

## Munición (desde 2026-11-07)
- Los fragmentos de cinetrita **no** son munición: hay que fabricar **Balas Cinéticas** y meterlas en **cargadores** (decisión del usuario). Recargar = cambiar de cargador. El Cargador Ampliado solo con su módulo; la Munición Guiada solo con la Guía de Enjambre.

## Equipo Táctico (desde 2026-11-07)
- Apartado extra del inventario **inspirado en Techguns** (idea pedida por el usuario), pero con diseño, código y recursos propios. Munición, comida (se come sola) y gadgets con batería. Ideas para más gadgets: tanque de oxígeno, botas de impacto, escáner de minerales, paracaídas.
