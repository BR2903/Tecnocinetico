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
- `SoundEvent`s propios declarados en `assets/tecnocinetico/sounds.json` (con subtítulos). Archivos en `sounds/swarm_body/`, generados por `tools/sound_gen.py`. Sonidos de la pistola: todavía vanilla.

### Entidad (`entity/`)
- `SwarmBodyEntity` (IA + `KineticLeapGoal`), `SwarmBodyModel` (modelo en código, animaciones), `SwarmBodyRenderer` (+ capa luminosa `EyesLayer`).

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
