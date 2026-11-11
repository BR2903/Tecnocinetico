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
| `block/entity/ModBlockEntities` | prensa manual, prensa automática, ensamblador de armas, consola del Núcleo, fuente creativa |
| `machine/MachineBlockEntities` | máquinas eléctricas (`MachineKind`): refinador, cargador, fabricador, cámara de reacción, laboratorio, reactor, ensamblador industrial |
| `entity/ModEntities` | enjambre (cuerpo, avispa, nodo de colmena), superviviente, torreta, dron, corrompidos (humano, tirador, araña, araña de cueva, ahogado, creeper), Anclador, granada, Caza del Núcleo y su misil, Araña de Asedio (jefe), cohete guiado |
| `menu/ModMenus` | Banco de Armas, Prensa Automática, Ensamblador de Armas, Equipo Táctico y una interfaz por máquina eléctrica (`MachineMenu`) |
| `particle/ModParticles` | `kinetic_tracer`, `kinetic_spark`, `kinetic_burst`, `flame_stream` |
| `fluid/ModFluids` | ácido sulfúrico, refrigerante, Celda Universal |
| `item/ModCreativeModeTabs` | pestañas creativas: principal (máquinas, gadgets, nave, huevos), Armas, Armaduras, Munición y cargadores, Materiales (cada una por tier 1→3) y Mundo |

- `block/ModWorldBlocks` — bloques del mundo post-apocalíptico (hormigón y familia, escombros, asfalto, metal oxidado, chatarra, ceniza, vitrificado, racimo de cinetrita, madera muerta) y su lista `ALL` para la pestaña creativa "Mundo". Clases especiales: `AshLayerBlock` (capas que no se derriten), `RoadLineBlock` (línea orientable).

## Sistemas

### Instalaciones y guardianes del Overworld
- `worldgen/structure/OverworldSiteStructure` + `OverworldSitePiece`: cinco estructuras, elegidas por `site` (0 antena, 1 centro de datos, 2 búnker, 3 laboratorio, 4 ciudad sumergida). Piezas por coordenadas absolutas, semilla guardada y posiciones locales para una generación independiente del orden de chunks. Se registran con el tipo `overworld_site`; cada estructura tiene su JSON, separación y etiquetas de biomas.
- `entity/boss/OverworldGuardianEntity`: dos tipos registrados (`network_watcher`, `swarm_matriarch`), modos sincronizados, tres fases y barra de jefe. Vigía: ráfagas/pulsos, dos condensadores traseros con daño acumulado y máscara de rotura sincronizada, interrupción de 60 ticks y daño recibido ×1,5 solo durante ese aturdimiento. Matriarca: embestida (una aplicación de daño por objetivo en cada carga) y tres zonas de bombardeo señalizadas; sin exposición ni puntos débiles. Consola, centro, roturas y aturdimiento se guardan por entidad; migración de atributos antiguos preservando la fracción de vida restante (`GuardianBalance=2`).
- Vigía: `watcherFlight` se ejecuta también durante las cargas y salvas; despega desde huevos, se separa del jugador y esquiva obstáculos. `watcherCombat` alterna ráfaga, bombardeo, barrido y EMP. Modos 0 reposo, 1 ráfaga/embestida, 2 bombardeo, 3 aturdimiento del Vigía, 4 barrido y 5 EMP; el cliente muestra carga en todos los modos de ataque. Los disparos de una ráfaga se fijan individualmente y tienen intervalo suficiente para no quedar anulados por la invulnerabilidad entre golpes vanilla.
- `entity/boss/MatriarchMinionEntity`: cría de `SwarmBodyEntity` (registro/atributos propios y renderer a escala .45 con recursos propios existentes). Objetivo coordinado con la madre, daño/objetivos aliados bloqueados y tabla de botín vacía, XP 0. La madre conserva hasta 6 UUID de invocaciones y su espera en NBT; cada cría guarda el UUID de la madre y comprueba que siga autorizada tras cargar. Limpieza por muerte/descarte, sin borrar entidades ajenas. No tiene spawn natural ni huevo de supervivencia.
- `CoreConsoleBlockEntity.configureGuardian`: tipo y punto de aparición para las nuevas arenas; NBT sin estos campos mantiene el comportamiento de la Araña. `defeated` abre sellos definitivamente y `reset` permite reintentar tras abandonar. Los nuevos jefes notifican ambos estados, también al descartarse por comandos o Pacífica.
- `entity/client/OverworldGuardianModel/Renderer`: modelo Java generado desde `tools/guardian_rig.py` + `guardian_model.java.template`; geometría compartida con las vistas para evitar aproximaciones. Turbinas/aspas, cañones y condensadores que desaparecen al romperse; abdomen/placas, rodillas/garras y mandíbulas de la Matriarca. Brillo de carga y blanco de aturdimiento del Vigía, exclusivamente en cliente.
- Datos y botín: `tools/overworld_sites_gen.py`, llamado también por `world_structures_gen.py`. `ModItems.VECTOR_THRUSTER` y `ADAPTIVE_FLIGHT_CORE`: piezas épicas incombustibles no apilables, una garantizada del jefe respectivo, sin otras fuentes. `GuardianGameTests` comprueba consolas, guardado, ataques reales, condensadores y tablas de botín; `WorldgenGameTests` incluye las cinco estructuras.

### Prensas (`block/custom`, `block/entity`)
- `PressRecipes` — **única fuente de recetas** (lista de `Recipe(inputs, output)`), `findResult`, `isIngredient`, `consumeInputs`. JEI lee `all()`.
- `KineticAssemblyPressBlock` + `KineticAssemblyPressBlockEntity` — prensa manual: clic derecho mete/saca (vía `PressInteraction`), golpes de martillo con `strike(level,pos,strikesNeeded)`. Usa Ingeniería.
- `AutoKineticPressBlock` + `AutoKineticPressBlockEntity` — eléctrica. `MenuProvider`; `ContainerData` (energía, progreso); `MachineEnergy` (EnergyStorage solo recepción); capability `ENERGY` en todas las caras e `ITEM_HANDLER` por cara (`SidedView`: arriba/lados entrada, abajo salida). Blockstate `powered` → luz de estado.
- `menu/AutoKineticPressMenu` + `client/AutoKineticPressScreen` — interfaz. Posiciones de ranuras/barras como constantes en el menú (deben coincidir con `textures/gui/auto_kinetic_press.png`).

### Energía
- Forge Energy (`ForgeCapabilities.ENERGY`). Compatible con Create Crafts & Additions.
- `CreativeEnergySourceBlock(Entity)` — solo pruebas; empuja 100 000 FE/tick a cada vecino y permite extracción infinita.

### Armas (`item/custom`)
- Armas lentas reportadas (dos escopetas, cerrojo, semiautomático, riel, francotirador, revólver, lanzagranadas y lanzacohetes): `HandMuzzleEffects` recibe el efecto autorizado por el servidor y lo dibuja en primera persona con la geometría de `HandMuzzleGeometry`, usando la pose del fotograma que está dibujando el arma. Fogonazo y comienzo de la trazadora quedan sujetos a la boca mientras cambia el retroceso, sin reutilizar una posición mundial del fotograma anterior. La punta de la trazadora conserva el impacto del servidor y se reproyecta al espacio de las manos. Onda: textura original `kinetic_ring.png`, material `entityTranslucentEmissive` con alfa real y expansión/desvanecimiento de `KineticParticle.BURST` durante siete ticks. Trazadora: partículas separadas de `kinetic_glow.png`, también con alfa real, color cian, reducción/desvanecimiento durante seis ticks y hasta 513 puntos desde boca a impacto. Cola local acotada y limpieza al salir/cambiar de dimensión o de arma. Automáticas y efectos para otros jugadores conservan su ruta anterior.
- Presentación del arsenal: `WeaponMuzzles` se genera desde `MUZZLES`/`muzzle_point` de los planos; incluye cañones recortados, estranguladores y cañones de rifle. `WeaponEffects` obtiene la salida del servidor, limitada por paredes, y envía `WeaponEffectPacket` (15, solo servidor→cliente; protocolo 2) a quienes siguen al tirador y al propio tirador. `ClientWeaponEffects` captura la pose completa de primera persona y el display del modelo real; `MuzzleProjection` compensa la distinta proyección de manos/mundo para el FOV y el zoom. Llamas, trazadoras, fogonazos, arco, riel y onda parten de ese origen visual. Cohete y granada del lanzador nacen en la salida física del servidor. Daño y detección siguen en servidor, sin coordenadas de disparo aportadas por el cliente.
- `LongGunItem.CycleSoundAt`: cerrojo/corredera se programan después del disparo (hasta 10/6 ticks), se consumen una sola vez y se cancelan al cambiar de arma, morir o recargar. El registro pendiente pertenece al ItemStack; no hay colas globales entre jugadores. Interrumpir una recarga para disparar no reproduce el cierre antes del estampido. Automáticas: impulso axial breve en `LongGunClientExtensions`; se conserva el retroceso de cámara y la dispersión existentes.
- Arte del arsenal: `tools/weapon_blueprints.py` construye desde cero 19 armas/objetos de combate; cada función usa un `Assembly` vacío y genera sus estados por `mods`, `reload` y `spin`. `weapons_gen.py` conserva los nombres/predicados y delega la geometría a `build_weapon`; sus antiguos cuerpos de geometría y `weapon_remodel.py` se eliminaron. Pistola Cinética independiente e intacta. `remodel_weapons.py` regenera solo esos recursos y ajusta inventario por variante. Atlas por arma mediante `gunkit`, máximo 128 px y caras coplanares corregidas antes del empaquetado. No cambia clases Java, estadísticas ni animación del jugador.
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
- Remodelación visual: `client/RemodeledMobLayers` se genera con `tools/remodel_mobs.py` a partir de `mob_remodel_rigs.py` y `mob_humanoid_rigs.py`. Los modelos pequeños delegan sus animaciones en `RemodeledMobModel`; las rodillas y poses de rotura/aturdimiento de la Araña mantienen sus anclajes originales. El Nodo tiene `HiveNodeModel` propio, ya no reutiliza una avispa gigante. `CorruptedSpiderModel` cubre arañas normales y de cueva; `RemodeledCreeperModel` conserva la animación y destello del renderer vanilla. `CorruptedHumanModel` y `SurvivorModel` conservan el esqueleto humanoide para armas y armaduras y muestran accesorios por variante/oficio. Atlas nuevos en `textures/entity/remodel/`, todos de 128×128. Las crías heredan el Cuerpo de Enjambre remodelado. `auditMobModels` construye los modelos reales sin abrir el cliente y comprueba sus enlaces, atlas y seis rodillas.
- `SentryTurretEntity` (+ `client/SentryTurretModel`, `SentryTurretRenderer`): torreta anclada que carga y dispara un rayo esquivable.
- `DroneEntity` (+ `client/DroneModel`, `DroneRenderer`): dron volador del Núcleo; patrulla alrededor de `Home`, escanea, alarma en grupo, ráfagas con carga de aviso. Modo sincronizado (0 patrulla, 1 ataque, 2 carga) para el brillo.
- **Alianza Araña de Asedio–drones**: ambas entidades se reconocen con `isAlliedTo`, rechazan elegirse como objetivo y bloquean el daño atribuido a la otra (incluidos misiles). `CoreShots` ignora aliados al buscar impactos y los misiles no colisionan con aliados de su propietario. No depende de equipos de scoreboard ni de quién invocó al dron.
- `AnchorEntity` (+ `client/AnchorModel`, `AnchorRenderer`): trípode antiaéreo; aplica el efecto `effect/InertiaAnchorEffect` (registro `effect/ModEffects`), cuyo tirón del jugador hace `client/GearClient`.
- `entity/corrupted/`: `CorruptionSpawns` (sustituye zombis/esqueletos/arañas en el Overworld al inicializarse la aparición), `CorruptedEntity`, `CorruptedGunnerEntity` (+ `GunnerAttackGoal`), `CorruptedSpiderEntity`; cliente `CorruptedHumanModel/Renderer`, `CorruptedSpiderRenderer`. Los disparos usan `entity/custom/CoreShots`.
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

### Tier 2 (`item/armor/Tier2ArmorItem`, `Tier2ArmorEvents`)
- Armaduras INERCITA / TACTICAL / EXO (enum `Kind` con su material). Bonos de set en `Tier2ArmorEvents` (daño, velocidad, carga del exoesqueleto en `getPersistentData`). Retroceso táctico: `Tier2ArmorItem.recoilMultiplier` (cliente).
- `LongGunItem.Action` (BREAK, BOLT, PUMP, SEMI, AUTO). Recetas de prensa con `powered` en `block/entity/PressRecipes`.

### Tier 3 (`item/weapon/`, `block/entity/AssemblerRecipes`)
- `RailRifleItem`, `GrenadeLauncherItem` (+ `entity/custom/KineticGrenadeEntity`), `PulseCannonItem`: heredan de `LongGunItem` y cambian `shoot`/`boxRefill`. Disparo cargado: `PistolInputHandler.railCharge` → `ModNetwork.FireChargedPacket`.
- Ensamblador de Armas: `WeaponAssemblerBlock`/`BlockEntity`/`Menu`/`Screen` (energía sincronizada /10), recetas `AssemblerRecipes`, JEI `AssemblerRecipeCategory`.

### Jefes (`entity/boss/`)
- `SiegeSpiderEntity` (fases, barra de jefe, ataques en `SiegeAttackGoal`) + `block/custom/CoreConsoleBlock`/`CoreBarrierBlock` y `block/entity/CoreConsoleBlockEntity` (despierta al jefe, sella/abre la arena). La arena la construye `AiInstallationPiece.arena`.

### Nave (`entity/ship/`)
- `CoreFighterEntity` (vuelo en el cliente del piloto; armas, daño y salto en el servidor), `ShipMissileEntity`, `ShipJump` (destino seguro + `ITeleporter`). Cliente: `ClientShipHandler` (entradas, HUD, FOV, destello, motor), `ShipJumpScreen`, `entity/client/CoreFighterModel/Renderer`. Tecla J (= Equipo Táctico) abre el salto si vas en la nave.

### Munición (`item/custom/`)
- `MagazineItem` (Cargador 6 / Ampliado 10; NBT `Rounds`, `Guided`), `MagazineLoadingRecipe` (receta especial registrada en `item/ModRecipes`), `GuidedRoundItem`. La pistola guarda `Mag`, `Ammo`, `GuidedAmmo` y durante la recarga `PendingMag`. R con un cargador en la mano → `FillMagazinePacket`.

### Comandos (`command/`) y pruebas (`gametest/`)
- `TpBiomeCommand`: `/tpbioma <bioma>`.
- `WorldgenGameTests`: GameTest que genera las estructuras y vuelca sus bloques (ver `docs/HERRAMIENTAS.md`, render_dump).

### Compatibilidad (`compat/jei/`)
- `TecnocineticoJeiPlugin` (categoría de la prensa, información de módulos/banco, oculta la fuente creativa) y `PressRecipeCategory`. JEI es dependencia solo de compilación/desarrollo; el mod funciona sin él.
- Botón **"+"** (pasar la receta): `registerRecipeTransferHandlers` registra el manejador básico de JEI para las 6 máquinas eléctricas (`MachineMenu`, uno por `MenuType`), la Prensa Automática y el Ensamblador de Armas. Envuelto en `itemsOnly`, que quita de la receta las entradas que no son objetos (los tanques del laboratorio) para que cuadren con los huecos. Las entradas se pasan en el orden en que la categoría las añade: **ese orden debe coincidir con el de los huecos de la máquina**. En servidor dedicado, JEI tiene que estar también en el servidor para que funcione.

### Reglas de partida (`rules/`)
- `FriendlyFire`: configuración de SERVIDOR (`<mundo>/serverconfig/tecnocinetico-server.toml`, `friendlyFire`, por defecto false). Si está apagado, cancela `LivingAttackEvent` cuando un jugador (o su proyectil/explosión) ataca a OTRO jugador; las armas de área del mod (lanzallamas, cuchilla, cañón de pulso, rifle de riel) además se saltan a los compañeros para no quemarlos ni empujarlos. `client/LanFriendlyFireOption` añade el botón a la pantalla "Abrir en LAN" y lo aplica solo si el mundo se abre de verdad. GameTest `RulesGameTests` (lote "reglas").

## Datos (`src/main/resources/data`)
- `tecnocinetico/recipes/` — recetas de crafteo (martillo, pistola, prensas, banco, módulos, bloques).
- `tecnocinetico/loot_tables/` — bloques y Cuerpo de Enjambre. (La fuente creativa usa `noLootTable`.)
- `tecnocinetico/worldgen/` + `forge/biome_modifier/` — minerales y aparición del Cuerpo de Enjambre.
- `minecraft/tags/blocks/` — herramienta/nivel para picar. `forge/tags/{blocks,items}/ores.json` — minerales del mod.

## Dependencias (`build.gradle`, versiones en `gradle.properties`)
- JEI 15.74.0.234 (compileOnly API + runtimeOnly).
- Solo desarrollo (runtimeOnly): Create 6.0.8-291 (slim), Ponder 1.0.91, Flywheel 1.0.6-beta-266, Registrate MC1.20-1.3.3, mixinextras-forge 0.4.1, Create Crafts & Additions forge-1.20.1-1.3.3.
- El código **no compila contra Create**: solo usa Forge Energy. Si en el futuro se necesita la API de Create, cambiar a `implementation` y declarar la dependencia en `mods.toml`.

## Máquinas eléctricas (`machine/`)
- `MachineKind`: cada máquina (id, posiciones de huecos y flecha, capacidad FE). Debe coincidir con `tools/machine_gui_gen.py`.
- `ElectricMachineBlockEntity`: base (batería `Battery` que solo recibe, inventario entradas+salidas con vistas por cara, `advance(ticks, FE/tick)`, `fits`/`output`, datos de interfaz con energía /10). Subclases: `RefinerBlockEntity`, `ChargerBlockEntity`, `AmmoFabricatorBlockEntity`, `ReactionChamberBlockEntity` (frecuencia/intensidad/estabilidad como datos extra, botones por `MachineMenu.clickMenuButton` -> `onButton`; pantalla propia `client/ReactionChamberScreen`).
- `ElectricMachineBlock` (FACING + POWERED, abre `MachineMenu`), `MachineBlockEntities` (registro), `MachineRecipes` (refinado y fabricación en código).
- `menu/MachineMenu` + `client/MachineScreen`: comunes; `ModMenus.machine(kind)` da el MenuType de cada una.
- Para añadir una máquina: valor en `MachineKind`, subclase de la entidad, tipo en `MachineBlockEntities`, bloque en `ModBlocks`, entrada en `machine_gui_gen.py` y `metals_gen.py` (`MACHINES`, frente).
- `ChemicalLabBlockEntity`: dos `FluidTank` y una vista `IFluidHandler` para tuberías/cubos (llenar = entrada, vaciar = salida); `ElectricMachineBlock.use` deriva los cubos a `useContainer`. Pantalla `client/ChemicalLabScreen` (líquidos por id de registro en los datos extra).
- Líquidos: `fluid/ModFluids` (`FluidSet` registra tipo, fuente, flujo, bloque y cubo; los bloques y cubos entran en los registros de `ModBlocks`/`ModItems`).
- `FissionReactorBlockEntity`: genera con `Battery.produce`, expone una vista de energía solo de salida y empuja a los vecinos; tanque de refrigeración; `FluidMachine` (cubos). Pantalla `client/FissionReactorScreen`.
- `fluid/UniversalCellItem`: Celda Universal de Fluidos (1 cubo de cualquier líquido, modelo forge:fluid_container).
- `structure/`: estructuras grandes que se despliegan desde un kit (`MultiblockTemplate`, `StructureKitItem`, `StructurePartBlock(Entity)`, `MultiblockMachineBlock`, `Structures`). El núcleo es una máquina eléctrica normal; las piezas le pasan todo. Renderizadores: `client/IndustrialAssemblerRenderer`, `client/FissionReactorRenderer` (modelos extra registrados en `ModelEvent.RegisterAdditional`).


## Nether, primera entrega
- `nether/NetherMobEvents`: suscriptor Forge para mejorar enemigos vanilla exclusivamente en el Nether. Guarda atributos/arma originales en NBT, evita acumular mejoras al cargar y los restaura al salir de la dimensión. Memorias de ira para piglins aun con oro; rifle con ráfagas; bolas del ghast, abanico del blaze y avance del esqueleto wither. No registra contenido nuevo ni cambia generadores o equipo.
- `gametest/NetherMobGameTests`: prueba las mejoras, la hostilidad con oro, persistencia/restauración, potencia de proyectiles, abanico y avance, respetando creativo.
- El resto del Nether queda en una rama local de trabajo para entregarlo por partes; ver `docs/PLAN_NETHER.md`. No mezclarla entera ni duplicar los suscriptores de combate.
