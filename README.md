# Tecnocinético

Mod de **Minecraft 1.20.1 (Forge)** de tecnología post-apocalíptica, complemento de **Create**. Un mundo destruido
por una IA, el **Núcleo Directriz**: empiezas de cero entre ruinas, recuperas tecnología y fabricas armas, máquinas
y equipo para llegar hasta ella. Progresión lenta y difícil, pensada para **jugar con amigos** (LAN o servidor).
Sin magia: todo es tecnología.

Autor: **Bryan Amaya**.

## Qué trae
- **Mundo post-apocalíptico**: 19 biomas, ruinas, asentamientos con supervivientes, fábricas, colmenas, antenas,
  búnkeres, laboratorios y una ciudad sumergida. Criaturas biomecánicas (Enjambre, drones, torretas, corrompidos).
- **Jefes**: Araña de Asedio, Vigía de la Red y Matriarca del Enjambre, con arenas y consolas.
- **Armas** por tiers (pistola cinética, escopetas, rifles, francotirador, ametralladora, minigun, lanzallamas,
  rifle de riel, lanzagranadas, lanzacohetes...) con módulos, munición en paquetes y un cargador para cada arma.
- **Armaduras 3D** de combate y de habilidad, equipo táctico (jetpack, visor, impulsor de esquiva...) y traje
  antirradiación.
- **Máquinas eléctricas** (Forge Energy, la de los alternadores de *Create Crafts & Additions*): prensa automática,
  refinador, cargador, fabricador de munición, laboratorio químico con líquidos, cámara de reacción, reactor de
  fisión de 9x9 y ensamblador industrial. Recetas visibles en JEI.
- **Nether** (por partes): enemigos vanilla mejorados y remodelados, Ácaros de Escoria y Centinelas de Ceniza.
- Herramientas como el **Taladro de Combustión** (3x3, gasolina, llega a la obsidiana).

## Requisitos
| | Versión |
|---|---|
| Minecraft | 1.20.1 |
| Forge | 47.4.10 o superior |
| Create | 6.0.x |
| Create Crafts & Additions | 1.3.x |
| JEI | opcional (recomendado; en servidor, para el botón **+** de las recetas) |

## Jugar
1. Instala Forge 1.20.1 y pon en `mods/` el `.jar` de Tecnocinético, Create y Create Crafts & Additions.
2. Al crear un mundo, en **Tipo de mundo** elige **Postapocalíptico** (también hay versión de biomas grandes y
   amplificada). Los tipos de Minecraft quedan intactos, así que el mod convive con otros mods de terreno.
3. **Con amigos**: todos necesitan los mismos mods y versiones.
   - LAN: al **Abrir en LAN** puedes activar o desactivar el **fuego amigo** (desactivado por defecto).
   - Servidor dedicado: en `server.properties` pon `level-type=tecnocinetico:post_apocalyptic` antes de generar el
     mundo; el fuego amigo se cambia en `<mundo>/serverconfig/tecnocinetico-server.toml` (`friendlyFire`).

## Compilar
Desde `forge-1.20.1-47.4.10-mdk/` (Java 17):
```
gradlew build              # build/libs/tecnocinetico-1.0.0.jar
gradlew runClient          # Minecraft de pruebas con el mod, Create, Crafts & Additions y JEI
gradlew runGameTestServer  # pruebas automáticas
```
Guía para colaboradores e IAs: `AGENTS.md`; documentación en `forge-1.20.1-47.4.10-mdk/docs/`.

## Licencia
**CC BY-NC-SA 4.0** — compartir y modificar dando crédito, sin uso comercial y con la misma licencia. Los sonidos
de terceros conservan su licencia (ver `CREDITOS_SONIDOS.md`). Detalles en `LICENSE.txt`.
Minecraft es de Mojang Studios; este mod no está afiliado a Mojang ni a Microsoft.
