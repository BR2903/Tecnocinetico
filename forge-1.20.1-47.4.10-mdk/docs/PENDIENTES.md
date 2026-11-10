# Pendientes, ideas y limitaciones

## Por verificar con el usuario
- Prensa Automática alimentada por un **alternador real de Create Crafts & Additions** (solo se probó con la Fuente de Energía Creativa / sin pruebas a fondo).
- Ajustes de equilibrio de habilidades (XP por acción) tras jugar un rato.

## Ideas propuestas (el usuario elige el orden)
- **Ruinas antiguas**: estructuras tecnológicas con Aleación Antigua (sin glifos ni magia), Cuerpos de Enjambre de guardia, cofres con módulos/piezas (encaja con el lore).
- **Equipo de Inercita**: herramientas y armadura de nivel 2 (diseño realista + detalles del mod).
- **Rediseño visual de la Prensa Automática** al estilo de las máquinas de Create (sigue con la textura antigua reescalada).
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

- Drones: más adelante, que también ronden las futuras antenas/estaciones de retransmisión; ajustar en el juego dificultad (daño 3 por tiro, ráfagas de 3) si es mucho o poco.

- Corruptos: decidir si también se sustituyen aldeanos zombi y otros mobs; sonidos propios (ahora usan los vanilla con tono más grave).

- Tier 2: módulos propios del subfusil; animación de bombeo de la corredera en primera persona; probar el equilibrio (daño del subfusil, freno de la inercita, carga del exoesqueleto).


- Caza del Núcleo: receta (¿fin de juego, con piezas de las dimensiones nuevas?), probar el manejo en el juego (giro, velocidades) y en multijugador; animación de cabina/HUD más elaborada si gusta.

- Armas: probar el equilibrio de las 8 nuevas en el juego; sonidos propios (lanzallamas, minigun, arco); cañones de la minigun que giren en el modelo. Habrá tiers 4 y 5 (dimensiones nuevas).
- Indicador en pantalla del enfriamiento del Impulsor de Esquiva (`DashClient.cooldown` ya existe); verlo puesto en el personaje.

- **Nave mejorable (idea del usuario, no implementar hasta que lo pida):** se fabrica con el Motor Dimensional (botín de la Araña de Asedio); primero solo por tierra y para viajar al siguiente mundo nuevo; vencer al jefe del Nether desbloquea saltar al Nether, y al del End, al End. El Caza del Núcleo actual vuela y salta libremente: habrá que adaptarlo.

- **Siguiente, en este orden (pedido)**: protección antirradiación (traje de plomo / caja de plomo) -> metales en recetas + Prensa Automática estilo Create. Detalle: **reactor generador** de FE (combustible de uranio, sobrecalentamiento), **rediseño de la Prensa Automática** estilo Create, e **integrar plomo/estaño/titanio** en recetas existentes (balas, circuitos/soldadura, armaduras y armas de tier 2-3).
- Radiación: protección (traje antirradiación / blindaje de plomo) aún no existe. Equilibrio de la Cámara (FE, tiempos, pérdida de estabilidad) tras probarla.
- Reactor: probar el equilibrio en el juego (600 FE/t, duración de la barra, calor). ¿Modelo 3D propio? ¿Multibloque más adelante?
- Estructuras grandes: probar en el juego (despliegue, desmontaje, brazos y barras visibles). Más recetas para el Ensamblador Industrial (nave mejorable, tier 4-5).
