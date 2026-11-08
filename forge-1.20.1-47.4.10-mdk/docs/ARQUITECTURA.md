# Arquitectura del código

Paquete raíz: `src/main/java/com/bryanamaya/tecnocinetico/`. Recursos: `src/main/resources/` (`assets/tecnocinetico`, `data/tecnocinetico`, `data/minecraft`, `data/forge`).

## Entrada

- **`Tecnocinetico.java`** — clase `@Mod`. Registra todos los `DeferredRegister` (items, bloques, block entities, entidades, menús, partículas, red) y contiene dos suscriptores de eventos del bus del mod:
  - `CommonModEvents`: atributos de entidades, reglas de aparición.
  - `ClientModEvents` (solo cliente): renderizadores, capas de modelos, partículas, pantallas de menús (`MenuScreens.register`), propiedades de items (`tecnocinetico:reload`, `tecnocinetico:modules`).

## Registro

| Clase | Contiene |
|---|---|
| `item/ModItems` | todos los items (minerales, componentes, martillo, pistola, módulos, huevo). `moduleItem(WeaponModule)` mapea módulo → item |
| `block/ModBlocks` | bloques + su BlockItem (`registerBlock`) |
| `block/entity/ModBlockEntities` | prensa manual, prensa automática, fuente creativa |
| `entity/ModEntities` | Cuerpo de Enjambre |
| `menu/ModMenus` | Banco de Armas, Prensa Automática |
| `particle/ModParticles` | `kinetic_tracer`, `kinetic_spark`, `kinetic_burst` |
| `item/ModCreativeModeTabs` | pestaña creativa (orden de los objetos) |

- `block/ModWorldBlocks` — bloques del mundo post-apocalíptico (hormigón y familia, escombros, asfalto, metal oxidado, chatarra, ceniza, vitrificado, racimo de cinetrita, madera muerta) y su lista `ALL` para la pestaña creativa "Mundo". Clases especiales: `AshLayerBlock` (capas que no se derriten), `RoadLineBlock` (línea orientable).

## Sistemas

### Prensas (`block/custom`, `block/entity`)
- `PressRecipes` — **única fuente de recetas** (lista de `Recipe(inputs, output)`), `findResult`, `isIngredient`, `consumeInputs`. JEI lee `all()`.
- `KineticAssemblyPressBlock` + `KineticAssemblyPressBlockEntity` — prensa manual: clic derecho mete/saca (vía `PressInteraction`), golpes de martillo con `strike(level,pos,strikesNeeded)`. Usa Ingeniería.
- `AutoKineticPressBlock` + `AutoKineticPressBlockEntity` — eléctrica. `MenuProvider`; `ContainerData` (energía, progreso); `MachineEnergy` (EnergyStorage solo recepción); capability `ENERGY` en todas las caras e `ITEM_HANDLER` por cara (`SidedView`: arriba/lados entrada, abajo salida). Blockstate `powered` → luz de estado.
- `menu/AutoKineticPressMenu` + `client/AutoKineticPressScreen` — interfaz. Posiciones de ranuras/barras como constantes en el menú (deben coincidir con `textures/gui/auto_kinetic_press.png`).

### Energía
- Forge Energy (`ForgeCapabilities.ENERGY`). Compatible con Create Crafts & Additions.
- `CreativeEnergySourceBlock(Entity)` — solo pruebas; empuja 100 000 FE/tick a cada vecino y permite extracción infinita.

### Armas (`item/custom`)
- `KineticPistolItem` — disparo hitscan (`fire`), efectos (`spawnShotEffects`: partículas propias + `ItemParticleOption` del fragmento + polvo del bloque), munición/recarga en NBT (`Ammo`, `ReloadTicks`, `ReloadTotal`), estadísticas según módulos (`getMagSize`, `getShotDamage`, `getRange`, `getAimAssist`, `getReloadTicks`), XP de Manejo de Armas. `initializeClient` → `PistolClientExtensions`.
- `GunAnimState` — último disparo (para la animación de retroceso). Solo datos.
- `WeaponModule` (enum, máscara de bits en NBT `Modules`) + `WeaponModuleItem`.
- `KineticHammerItem` — `PickaxeItem` de nivel hierro con velocidad ×0,7.
- `client/PistolInputHandler` — controles: intercepta el clic izquierdo (`InteractionKeyMappingTriggered`) → `FirePistolPacket` (el servidor llama a `KineticPistolItem.tryFire`, que dispara o recarga), calcula el progreso de apuntar (`GunAnimState.aim`), zoom (`ComputeFovModifierEvent`) y oculta el punto de mira.
- `client/PistolClientExtensions` — `applyForgeHandTransform` (animación de recarga y retroceso en 1ª persona) y `getArmPose` (apuntar en 3ª persona).
- **Fotogramas de recarga y módulos** = modelos JSON distintos elegidos por overrides (`tecnocinetico:modules` 0-15 × `tecnocinetico:reload` 0-1). 80 modelos generados por `tools/pistol_gen.py`.

### Banco de Armas (`block/custom/WeaponBenchBlock`, `menu/WeaponBenchMenu`, `client/WeaponBenchScreen`)
- Hueco 0 = arma; huecos 1-4 = un módulo por ranura, **vista del NBT del arma** (poner módulo = instalar, sacar = desinstalar, sacar arma = vaciar ranuras). Lógica solo en servidor dentro de `slotsChanged`. Al cerrar solo se devuelve el arma (se tira al suelo si el jugador murió/se desconectó).
- Revisado contra duplicaciones (clics, Mayús, teclas numéricas, Q, arrastrar, doble clic).

### Red (`network/ModNetwork`)
- `ReloadPistolPacket` (cliente→servidor, tecla R) y `SyncSkillsPacket` (servidor→cliente).

### Teclas (`client/ModKeyBindings`)
- R = recargar, K = habilidades. Categoría "Tecnocinético".

### Habilidades (`skill/`)
- `Skill` (enum con efectos y curva de XP), `PlayerSkills` (guarda en `PlayerPersisted`, `addXp`, sincroniza al entrar/reaparecer/cambiar de dimensión), `ClientSkillData` (copia en cliente), `MiningSkillEvents` (XP y velocidad del martillo). Pantalla `client/SkillsScreen`.

### Partículas (`particle/ModParticles`, `client/KineticParticle`)
- Tres tipos con brillo propio; texturas en `textures/particle/`, definiciones en `assets/tecnocinetico/particles/`.

### Sonidos (`sound/ModSounds`)
- `SoundEvent`s propios declarados en `assets/tecnocinetico/sounds.json` (con subtítulos). Archivos en `sounds/swarm_body/`, generados por `tools/sound_gen.py`. Pistola: disparo (atenuación 48 bloques), impacto (en el punto de impacto), acierto (solo el tirador), clic en vacío, cargador fuera/dentro y corredera (sincronizados con la animación de recarga).

### Entidad (`entity/`)
- `SentryTurretEntity` (+ `client/SentryTurretModel`, `SentryTurretRenderer`): torreta anclada que carga y dispara un rayo esquivable.
- `swarm/`: Avispas del Enjambre (`SwarmWaspEntity`, `WaspRole`), `HiveNodeEntity` y `HiveMind` (la mente colmena que decide estados, turnos de ataque, refuerzos y adaptación). Render: `client/SwarmWaspModel`, `SwarmWaspRenderer`, `HiveNodeRenderer`. Colmena: `worldgen/structure/SwarmHive*`.
- `SurvivorEntity` (+ `SurvivorTrades`, `client/SurvivorRenderer`): NPC comerciante de los asentamientos; oficios, ofertas con Chatarra, reposición diaria, casa en NBT.
- `SwarmBodyEntity` (IA + `KineticLeapGoal`), `SwarmBodyModel` (modelo en código, animaciones), `SwarmBodyRenderer` (+ capa luminosa `EyesLayer`).

### Mundo / Overworld (`worldgen/`, `datagen/`)
- Bloques del mundo en `block/ModWorldBlocks`; `block/custom/PileBlock` = montones de 1-4 trozos (escombros, chatarra), anegables. Features del terreno: código en `worldgen/feature/` (CraterFeature, RuinWallFeature, DeadTreeFeature) y JSON generados por `tools/world_features_gen.py`.
- `ModBiomes`: 19 biomas (claves, `bootstrap` para datagen y `VANILLA_TO_MOD`, el mapa bioma vanilla → bioma del mod). Cada bioma copia clima, generación y animales de su equivalente vanilla y cambia cielo/niebla/agua/hierba/partículas.
- `RemappedBiomeSource` (tipo `tecnocinetico:remapped`, registrado en `ModWorldgen`): envuelve la fuente multi-ruido vanilla y sustituye cada bioma → mismo relieve vanilla, biomas nuevos.
- `ModSurfaceRules`: reglas propias (parches de asfalto, ceniza, arena vitrificada, escombros...) + las reglas vanilla con sus condiciones de bioma traducidas (serializa a JSON, cambia `biome_is`, vuelve a leer).
- `ModWorldgen`: ajustes de ruido `tecnocinetico:overworld|large_biomes|amplified` (vanilla + nuestras superficies).
- Presets sobrescritos a mano: `data/minecraft/worldgen/world_preset/{normal,large_biomes,amplified}.json`.
- `datagen/ModDataGenerators`: `gradlew runData` escribe biomas, ruido y etiquetas de biomas en `src/generated/resources` (**se sube a git**; volver a ejecutarlo tras tocar `worldgen/`).
- Etiquetas: todos en `#minecraft:is_overworld` (minerales del mod/Create, Cuerpo de Enjambre, fortalezas), minas abandonadas, y algunas de comportamiento. **No** se añaden a `is_forest`, `is_ocean`, etc. a propósito: esas etiquetas activan estructuras vanilla (naufragios, templos, portales en ruinas...).

### Estructuras (`worldgen/structure/`)
- `ModStructures`: tipos de estructura y de pieza. `RuinedBuildingStructure` (busca terreno llano y seco) + `RuinedBuildingPiece` (construye el edificio por código; aleatorios por posición para que todos los chunks coincidan). `FactoryStructure` + `FactoryPiece` (fábrica abandonada). `SettlementStructure` + `SettlementPiece` (asentamiento; alturas del terreno guardadas por columna). Datos por `tools/world_structures_gen.py`.

- `AiInstallationStructure` + `AiInstallationPiece`: búnker + escalera + complejo subterráneo 3x3 con cámara del Núcleo.

### Equipo Táctico (`gear/`, `menu/TacticalMenu`, `client/TacticalScreen`, `GearTabs`, `GearClient`)
- `TacticalGear` (capacidad: ranuras 0-5 munición, 6-14 comida, 15 cara, 16 espalda, 17 pies; migra el formato viejo de 10), `GadgetItem`, `JetpackItem` (niveles), `RechargeItem`, `GearRecipes` (recarga y mejora en la mesa), `client/GearLayer` + `WornGearModel` (gadgets puestos), `GearEvents` (adjuntar, morir, comer solo, visor, propulsor, jetpack, sincronizar). Paquetes `OpenGearPacket`, `ToggleVisorPacket`, `JetpackPacket`, `SyncGearPacket`. Teclas J (equipo) y N (visor).

### Tier 1 (`item/armor/`, `item/weapon/`)
- `ScrapArmorItem` (material propio + bono de set), `MacheteItem` (tier propio), `LongGunItem` (escopeta y rifle con `Stats`; NBT `Ammo`, `Loading`, `LoadTimer`) + `client/LongGunClientExtensions`.

### Munición (`item/custom/`)
- `MagazineItem` (Cargador 6 / Ampliado 10; NBT `Rounds`, `Guided`), `MagazineLoadingRecipe` (receta especial registrada en `item/ModRecipes`), `GuidedRoundItem`. La pistola guarda `Mag`, `Ammo`, `GuidedAmmo` y durante la recarga `PendingMag`. R con un cargador en la mano → `FillMagazinePacket`.

### Comandos (`command/`) y pruebas (`gametest/`)
- `TpBiomeCommand`: `/tpbioma <bioma>`.
- `WorldgenGameTests`: GameTest que genera las estructuras y vuelca sus bloques (ver `docs/HERRAMIENTAS.md`, render_dump).

### Compatibilidad (`compat/jei/`)
- `TecnocineticoJeiPlugin` (categoría de la prensa, información de módulos/banco, oculta la fuente creativa) y `PressRecipeCategory`. JEI es dependencia solo de compilación/desarrollo; el mod funciona sin él.

## Datos (`src/main/resources/data`)
- `tecnocinetico/recipes/` — recetas de crafteo (martillo, pistola, prensas, banco, módulos, bloques).
- `tecnocinetico/loot_tables/` — bloques y Cuerpo de Enjambre. (La fuente creativa usa `noLootTable`.)
- `tecnocinetico/worldgen/` + `forge/biome_modifier/` — minerales y aparición del Cuerpo de Enjambre.
- `minecraft/tags/blocks/` — herramienta/nivel para picar. `forge/tags/{blocks,items}/ores.json` — minerales del mod.

## Dependencias (`build.gradle`, versiones en `gradle.properties`)
- JEI 15.74.0.234 (compileOnly API + runtimeOnly).
- Solo desarrollo (runtimeOnly): Create 6.0.8-291 (slim), Ponder 1.0.91, Flywheel 1.0.6-beta-266, Registrate MC1.20-1.3.3, mixinextras-forge 0.4.1, Create Crafts & Additions forge-1.20.1-1.3.3.
- El código **no compila contra Create**: solo usa Forge Energy. Si en el futuro se necesita la API de Create, cambiar a `implementation` y declarar la dependencia en `mods.toml`.
