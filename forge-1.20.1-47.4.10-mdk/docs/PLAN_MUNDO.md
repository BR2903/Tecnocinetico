# Plan: Overworld post-apocalíptico

Decisiones del usuario (2026-11-07):
- **Reemplazar todo el Overworld** (los biomas vanilla no aparecen en mundos nuevos).
- Tono **"reclamado por la naturaleza"** (lore: *"El Overworld fue reclamado nuevamente por la naturaleza. Las estructuras tecnológicas quedaron cubiertas de vegetación. Las antiguas ciudades se convirtieron en ruinas"*), con zonas grises de batalla/ceniza como contraste. Referencias de estilo: The Last of Us, Horizon Zero Dawn.
- **Sustituir aldeas y estructuras vanilla** por estructuras del mod (las aldeas → Asentamientos de Supervivientes).
- Nether y End: más adelante (Piroxita / Nulita según el lore).

---

## 1. Biomas (cada uno sustituye a un grupo de biomas vanilla)

Se reutiliza la **forma del terreno vanilla** (montañas, ríos, cuevas…) y se cambia qué bioma va en cada sitio. Así el relieve sigue siendo bueno y la distribución climática coherente.

| Bioma nuevo | Sustituye a (vanilla) | Aspecto | Contenido |
|---|---|---|---|
| **Ruinas Invadidas** | forest, birch_forest, dark_forest, flower_forest, meadow | Bosque denso sobre ciudades derrumbadas: hormigón agrietado y con musgo, enredaderas en esqueletos de edificios | Ruinas pequeñas, supervivientes, Cuerpos de Enjambre |
| **Selva Industrial** | jungle, sparse_jungle, bamboo_jungle | Árboles gigantes que han crecido a través de complejos industriales, tuberías cubiertas de lianas | Fábricas abandonadas |
| **Praderas de Chatarra** | plains, savanna, savanna_plateau | Hierba alta con restos oxidados de máquinas y drones semienterrados | Chatarra reciclable, drones estrellados |
| **Megaciudad Muerta** (rara) | sunflower_plains, windswept_savanna | Rascacielos en ruinas, calles con escombros, autopistas elevadas rotas, árboles en las azoteas | El mejor botín de exploración, mucho peligro |
| **Taiga de Ceniza** | taiga, old_growth_*_taiga | Coníferas medio muertas, suelo con ceniza, niebla gris, ceniza cayendo | Zona inquietante, criaturas nocturnas |
| **Pantano de Refrigerante** | swamp, mangrove_swamp | Charcas de refrigerante cian-verdoso, tuberías y depósitos corroídos | Fluido peligroso pero útil |
| **Cicatrices de Extracción** | badlands, eroded_badlands, wooded_badlands | Minas a cielo abierto escalonadas, **cristales de cinetrita expuestos y brillantes**, excavadoras abandonadas | Cinetrita en superficie, máquinas de la IA activas |
| **Desierto Vitrificado** | desert | Arena fundida en vidrio por ataques de la IA, cráteres | Restos de armas experimentales |
| **Tierras Fracturadas** | windswept_hills, windswept_gravelly_hills, windswept_forest | Cráteres de impactos cinéticos, grietas profundas, rocas levantadas | Zona de batalla |
| **Cumbres de las Antenas** | jagged/frozen/stony_peaks, snowy_slopes, grove | Montañas con **antenas repetidoras de la IA** en las cimas | Estructura "Antena" |
| **Tundra en Ruinas** | snowy_plains, ice_spikes, snowy_taiga | Nieve sobre ruinas y vehículos congelados | |
| **Costa de Chatarra** | beach, snowy_beach, stony_shore | Playas con restos de barcos y metal arrastrado | |
| **Mar de Escombros** | ocean, lukewarm/warm/cold/frozen_ocean (+ deep) | Ciudades sumergidas, puentes colapsados, plataformas oxidadas | Ciudad sumergida |
| **Río Contaminado** | river, frozen_river | Ríos con restos y orillas de hormigón | |
| **Cuevas de Cinetrita** | lush_caves | Cuevas iluminadas por cristales de cinetrita cian | Cinetrita |
| **Túneles Industriales** | dripstone_caves | Metro y galerías de servicio derrumbadas | |
| **Instalación Profunda** | deep_dark | Complejo enterrado de la IA (sustituye a la ciudad antigua) | Laboratorios del Enjambre |

`mushroom_fields`: sustituir por una variante segura (p. ej. **Zona Despejada** sin hostiles) o por Megaciudad, por decidir.

## 2. Infraestructura repartida por el mundo
- Autopistas agrietadas y vías de tren que cruzan biomas (estructura lineal, fase avanzada).
- Torres eléctricas caídas, postes, señales.
- Cráteres y socavones que dejan ver instalaciones subterráneas.
- Drones estrellados desmontables (piezas).

## 3. Estructuras
| Estructura | Sustituye a | Dónde | Notas |
|---|---|---|---|
| **Asentamiento de Supervivientes** | aldeas | biomas verdes | Comerciantes (piezas, munición); futuro NPC "Superviviente" |
| Ruinas urbanas (pequeñas/medianas) | — | Ruinas Invadidas, Praderas | Botín común |
| Rascacielos colapsados | — | Megaciudad | Varias plantas, botín bueno |
| **Fábrica abandonada** | — | Selva Industrial, Praderas | Botín con piezas de **Create** |
| **Instalación de la IA** | mansión / fortalezas | Cicatrices, Tierras Fracturadas | Mazmorra con drones/torretas, módulos y planos |
| Antena repetidora | — | Cumbres | Señal de la IA; posible evento |
| Centro de datos | — | Varios | **Registros del Núcleo Directriz** (lore jugable) |
| Búnker | — | Subsuelo | Refugio con botín |
| Laboratorio del Enjambre | ciudad antigua | Instalación Profunda | Generador de Cuerpos de Enjambre |
| Ciudad sumergida | monumento / ruinas oceánicas | Mar de Escombros | |
| Fortaleza (stronghold) | — | Se **mantiene** (necesaria para ir al End) hasta diseñar otro acceso | Añadir nuestros biomas a su tag |

## 4. Ambiente
- Colores de cielo, niebla, agua, hierba y follaje por bioma (gris ceniza, cian en las minas, verde enfermizo en pantanos).
- Partículas ambientales (ceniza cayendo, polvo, esporas).
- Sonidos de ambiente: metal que cruje, viento entre ruinas, zumbidos lejanos de máquinas (grabaciones CC0 o síntesis).
- Clima nuevo: **tormentas de ceniza** (fase avanzada).

## 5. Bloques nuevos necesarios (fase 1)
Hormigón (normal, agrietado, con musgo; losas, escaleras, muros), escombros (capa), asfalto roto, metal oxidado (chapa, vigas, rejillas, tuberías), montón de chatarra, ceniza (bloque y capa), arena/vidrio vitrificado, cristal de cinetrita (racimo luminoso), madera muerta (tronco/tablones), y más adelante el fluido **refrigerante**.
Texturas 16×16 o 32×32 (potencia de 2), estilo coherente con el mod.

---

## Enfoque técnico (Forge 1.20.1)

1. **Biomas** como JSON de datos: `data/tecnocinetico/worldgen/biome/*.json` (clima, efectos, spawns, features).
2. **Sustituir el Overworld**: sobrescribir el preset de mundo `data/minecraft/worldgen/world_preset/normal.json` (y `large_biomes`, `amplified`) para usar un generador con:
   - **fuente de biomas multi-noise propia**: la lista de puntos climáticos de vanilla (`OverworldBiomeBuilder`) con cada bioma vanilla **reasignado** a su sustituto de la tabla. Se genera con un pequeño programa de *datagen* en Java que recorre `OverworldBiomeBuilder.addBiomes()` y aplica el mapa → JSON.
   - **noise_settings propios** (`data/tecnocinetico/worldgen/noise_settings/overworld.json`): copia de vanilla con **reglas de superficie** nuevas (hormigón, ceniza, vidrio, grava con chatarra…).
3. **Estructuras vanilla**: desaparecen solas porque dependen de tags de bioma (`#minecraft:has_structure/village_*`…) en los que nuestros biomas no estarán. Excepción: **stronghold** (añadir nuestros biomas a su tag).
4. **Compatibilidad**: añadir todos nuestros biomas a `#minecraft:is_overworld` y tags de Forge (`#forge:is_*`) para que **Create** (zinc), Crafts & Additions y otros mods sigan generando minerales y criaturas.
5. **Estructuras nuevas**: jigsaw (`template_pool`, `structure`, `structure_set`) con plantillas `.nbt` construidas en el juego con bloques de estructura.
6. Mundos ya existentes: los chunks ya generados no cambian; probar siempre en **mundos nuevos**.

## Fases propuestas
1. **Bloques del mundo** (paleta de la sección 5). ✅ Hecho (2026-11-07), falta el fluido refrigerante.
2. **Biomas + sustitución del Overworld** (datagen del mapa de biomas, efectos, superficies, tags). ✅ Hecho (2026-11-07): 19 biomas (se añadieron Mar de Escombros Profundo y Helado); `mushroom_fields` → Praderas de Chatarra, `cherry_grove` → Ruinas Invadidas. Se mantienen minas abandonadas y fortalezas; el resto de estructuras vanilla ya no aparecen.
3. **Detalles del terreno** (features): cráteres, chatarra, cristales, árboles muertos/invadidos, escombros. 🔶 Hecho en parte (`tools/world_features_gen.py`): montones, pedazos, ceniza, cráteres, restos de muros, árboles muertos, cristales de cinetrita. Falta: autopistas/vías, más vegetación invadida, refrigerante.
4. **Estructuras** empezando por Asentamiento de Supervivientes, ruinas y fábrica.
5. **Ambiente** (partículas, sonidos, clima).
6. Nether y End.

## Pendiente de decidir
- ¿Se mantienen los hostiles vanilla (zombis, esqueletos…) o se sustituyen por criaturas de la IA? (sugerencia: mantener algunos, reducirlos, y añadir drones/criaturas del mod).
- ¿Acceso al End: stronghold vanilla o una instalación propia?
