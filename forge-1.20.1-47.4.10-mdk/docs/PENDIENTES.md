# Pendientes, ideas y limitaciones

## Por verificar con el usuario
- Prensa Automática alimentada por un **alternador real de Create Crafts & Additions** (solo se probó con la Fuente de Energía Creativa / sin pruebas a fondo).
- Ajustes de equilibrio de habilidades (XP por acción) tras jugar un rato.

## Ideas propuestas (el usuario elige el orden)
- **Ruinas antiguas**: estructuras con Aleación Antigua y glifos, Cuerpos de Enjambre de guardia, cofres con módulos/piezas (encaja con el lore).
- **Equipo de Inercita**: herramientas y armadura de nivel 2 (diseño realista + detalles del mod).
- **Nuevas máquinas eléctricas** (Forge Energy, con interfaz y luz de estado como la prensa): p. ej. Refinador Cinético que duplica minerales.
- **Rediseño visual de la Prensa Automática** al estilo de las máquinas de Create (sigue con la textura antigua reescalada).
- **Más armas realistas** (p. ej. rifle cinético) con sus propios módulos en el banco.
- Más habilidades (el sistema está preparado: ver `Skill.java`).
- Materiales del lore aún no implementados: Radionita, Crioprisma (¿para las dimensiones nuevas de los tiers 4-5?); Núcleo Directriz (jefe final).
- Tiers 4 y 5: 3 dimensiones nuevas (por diseñar).

## Limitaciones conocidas / deuda técnica
- **Equilibrio del mundo**: la Inercita (nivel diamante) aparece en 9 vetas de 7 por chunk → más común que el diamante. Cinetrita: 14 vetas de 9. Revisar `worldgen/placed_feature`.
- **Receta de Aleación Antigua** usa 8 hierro + cinetrita, pero el lore dice que el hierro "se deformaba" con la cinetrita → quizá debería llevar Inercita.
- **Resistor 330 Ω** existe pero no tiene receta ni uso.
- La Prensa Automática no sincroniza su inventario con el cliente fuera del menú (no hace falta salvo que se añada un renderizador de los objetos).
- Las bonificaciones de la pistola no se recalculan si se cambia de módulos durante una recarga (la recarga en curso termina con su duración original).
- `run/` contiene mundos de prueba con objetos que ya no existen (objetos "Vista 1-5"): el aviso de "registros no identificados" al cargarlos es inofensivo.
- Si se añade un 5.º módulo, `pistol_gen.py` generaría 160 modelos (2^5 × 5). Valorar un modelo dinámico (BakedModel propio) si crecen.

- Sonidos reales (CC0/CC-BY) para la escopeta y el rifle (ahora reutilizan los de la pistola con otro tono).
- Ajustar en el juego la posición en primera persona de la escopeta y el rifle si hace falta (`LongGunClientExtensions`).

- Drones: más adelante, que también ronden las futuras antenas/estaciones de retransmisión; ajustar en el juego dificultad (daño 3 por tiro, ráfagas de 3) si es mucho o poco.

- Corruptos: decidir si también se sustituyen aldeanos zombi y otros mobs; sonidos propios (ahora usan los vanilla con tono más grave).

- Tier 2: módulos propios del subfusil; animación de bombeo de la corredera en primera persona; probar el equilibrio (daño del subfusil, freno de la inercita, carga del exoesqueleto).

- Tier 3: sonidos propios del riel, la granada y el cañón (ahora reutilizan los del Anclador y la pistola).

- Caza del Núcleo: receta (¿fin de juego, con piezas de las dimensiones nuevas?), probar el manejo en el juego (giro, velocidades) y en multijugador; animación de cabina/HUD más elaborada si gusta.

- **Más armas de los tiers 1 a 3** (pedido del usuario, 2026-11-09; aún no hechas). Habrá tiers 4 y 5 (dimensiones nuevas).
- Indicador en pantalla del enfriamiento del Impulsor de Esquiva (`DashClient.cooldown` ya existe); verlo puesto en el personaje.

- **Nave mejorable (idea del usuario, no implementar hasta que lo pida):** se fabrica con el Motor Dimensional (botín de la Araña de Asedio); primero solo por tierra y para viajar al siguiente mundo nuevo; vencer al jefe del Nether desbloquea saltar al Nether, y al del End, al End. El Caza del Núcleo actual vuela y salta libremente: habrá que adaptarlo.
