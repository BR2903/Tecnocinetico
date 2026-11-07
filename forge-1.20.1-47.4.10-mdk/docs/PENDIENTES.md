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
- Materiales del lore aún no implementados: Piroxita (Nether), Nulita (End), Radionita, Crioprisma; drones; Núcleo Directriz (jefe final).

## Limitaciones conocidas / deuda técnica
- **Equilibrio del mundo**: la Inercita (nivel diamante) aparece en 9 vetas de 7 por chunk → más común que el diamante. Cinetrita: 14 vetas de 9. Revisar `worldgen/placed_feature`.
- **Receta de Aleación Antigua** usa 8 hierro + cinetrita, pero el lore dice que el hierro "se deformaba" con la cinetrita → quizá debería llevar Inercita.
- **Resistor 330 Ω** existe pero no tiene receta ni uso.
- La Prensa Automática no sincroniza su inventario con el cliente fuera del menú (no hace falta salvo que se añada un renderizador de los objetos).
- Las bonificaciones de la pistola no se recalculan si se cambia de módulos durante una recarga (la recarga en curso termina con su duración original).
- `run/` contiene mundos de prueba con objetos que ya no existen (objetos "Vista 1-5"): el aviso de "registros no identificados" al cargarlos es inofensivo.
- Si se añade un 5.º módulo, `pistol_gen.py` generaría 160 modelos (2^5 × 5). Valorar un modelo dinámico (BakedModel propio) si crecen.
