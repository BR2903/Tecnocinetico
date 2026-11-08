# AGENTS.md — Tecnocinético (mod de Minecraft Forge 1.20.1)

Guía para cualquier agente de IA (Codex, Claude, etc.) que continúe este proyecto.
**Léela entera antes de tocar nada.** Documentación detallada en `docs/`:

| Archivo | Para qué |
|---|---|
| `docs/DISENO.md` | Lore, decisiones de diseño y **preferencias del usuario** (muy importante) |
| `docs/ARQUITECTURA.md` | Mapa del código: qué hace cada clase y cómo se conectan los sistemas |
| `docs/HERRAMIENTAS.md` | Scripts Python que generan modelos/texturas/interfaces y cómo usarlos |
| `docs/BITACORA.md` | Historial de lo que se ha hecho, en orden |
| `docs/PENDIENTES.md` | Ideas propuestas, tareas abiertas y limitaciones conocidas |
| `docs/PLAN_MUNDO.md` | Plan del Overworld post-apocalíptico (biomas, estructuras, enfoque técnico, fases) |

Al terminar una tarea, **añade una entrada a `docs/BITACORA.md`** y actualiza los demás documentos si cambiaste algo que describen.

---

## 1. Qué es

- Mod **Forge 1.20.1 (47.4.10)**, mappings oficiales de Mojang, Java 17. `mod_id = tecnocinetico`, paquete `com.bryanamaya.tecnocinetico`.
- Autor: Bryan Amaya. **Todo en español** (código comentado en español, textos del juego en `es_es.json` + traducción en `en_us.json`).
- Es un **complemento de Create**: las máquinas automáticas usan **electricidad (Forge Energy, FE)** como la que generan los alternadores de **Create Crafts & Additions**. **No** usan la energía cinética (rotación) de Create.
- Lore: `../Tecnocinetico_Lore.pdf` (un nivel por encima de esta carpeta). Resumen en `docs/DISENO.md`.

## 2. Comandos (Windows)

El proyecto está en `forge-1.20.1-47.4.10-mdk/`. Desde PowerShell, **ejecuta gradle a través de cmd** y desde esta carpeta:

```powershell
cmd /c ".\gradlew.bat compileJava -q 2>&1"        # compilar (rápido, ~30 s)
cmd /c ".\gradlew.bat runGameTestServer 2>&1"     # servidor de prueba: carga TODO el mod + datos y se cierra (~1-2 min)
cmd /c ".\gradlew.bat runClient 2>&1"             # abre Minecraft con el mod, JEI, Create y Crafts & Additions
cmd /c ".\gradlew.bat build 2>&1"                 # genera build/libs/tecnocinetico-1.0.0.jar
cmd /c ".\gradlew.bat runData 2>&1"               # regenera src/generated/resources (biomas, ruido, etiquetas)
```

- **No uses `--offline`** (falla: el plugin foojay no está en caché).
- Java 17 (Temurin) ya está instalado. Python 3.13 + Pillow para los scripts de `tools/`.

## 3. Cómo trabajar (flujo que funciona con este usuario)

1. **Compila** tras cada cambio.
2. Si tocaste datos (recetas, loot, tags, worldgen) o registros: **`runGameTestServer`** y busca en el log `Exception`, `Parsing error`, `Couldn't parse`, `ClassNotFound`. Si termina con `All N required tests passed` y `BUILD SUCCESSFUL`, el mod carga bien (el GameTest de `gametest/` además genera las estructuras; sus bloques quedan en `run/gametest_dump/` para `tools/render_dump.py`) (y además prueba que no hay código de cliente filtrado al servidor).
3. **No abras el juego tú** (gasta tokens del usuario). El usuario lo ejecuta desde **IntelliJ IDEA** con la configuración **`runClient`** (ya generada con `gradlew genIntellijRuns`; si faltara, vuelve a ejecutar esa tarea). Cuando termines, dile qué probar. Si te pasa un error o log, revísalo (`run/logs/latest.log`).
4. Para cambios visuales: genera con los scripts de `tools/` y **previsualiza con `tools/render_model.py`** antes de pedir al usuario que lo pruebe (ver `docs/HERRAMIENTAS.md`).
5. Cuando haya varias opciones visuales, al usuario le gusta **elegir viéndolas en el juego** (p. ej. objetos de prueba temporales en la pestaña creativa) o en una imagen comparativa. Luego se borran.
6. Para cambios con riesgo de duplicar objetos (menús/inventarios) conviene una **revisión independiente** del código.

## 3b. Git (repositorio compartido entre agentes)

- Repositorio: **https://github.com/BR2903/Tecnocinetico** (rama `main`). La raíz del repo es la carpeta `Tecnocinetico/` (una por encima de esta).
- **Al empezar**: `git pull` y lee las últimas entradas de `docs/BITACORA.md` (otro agente pudo cambiar cosas).
- **Al terminar una tarea que compila y carga** (y que el usuario aprobó o pidió): añade la entrada en la bitácora, `git add -A`, commit con un mensaje en español que diga qué y por qué, y `git push`.
- No subas `build/`, `run/`, `.gradle/` (ya están en `.gitignore`). No reescribas el historial (`push --force`) sin que el usuario lo pida.
- Si algo se rompe, `git log` / `git diff` para ver qué cambió; `git revert` para deshacer un commit sin perder historial.

## 4. Reglas y convenciones

- Registros con `DeferredRegister` y la constante `Tecnocinetico.MOD_ID` (no escribir `"tecnocinetico"` a mano).
- `ResourceLocation.fromNamespaceAndPath(...)` (el constructor está obsoleto). El constructor del mod recibe `FMLJavaModLoadingContext`.
- Código solo-cliente en `client/` y referenciado solo desde suscriptores `Dist.CLIENT` o `initializeClient`. Clases "de datos" compartidas (p. ej. `ClientSkillData`, `GunAnimState`) **no** importan clases de cliente.
- Texturas: **potencia de 2 y ≤128 px** (las grandes o raras rompen los mipmaps de todo el juego). Los originales grandes están en `../texturas_originales_backup/`.
- Modelos de armas/herramientas: **el inventario muestra el modelo 3D** (vista `gui` calculada con `tools/gui_fit.py`), **no** iconos planos — el usuario los rechazó.
- Textos nuevos: añadir clave en `es_es.json` **y** `en_us.json`, y luego `python tools/sync_lang.py` (copia `es_es` a `es_mx`, `es_ec`, `es_ar`…; el usuario juega en **es_ec** y Minecraft no usa `es_es` como respaldo: sin la copia ve el inglés). `world_blocks_gen.py` ya lo ejecuta al final.
- **Recursos de terceros** (sonidos, texturas, modelos): solo con licencia que lo permita (CC0, CC-BY…), comprobando también el archivo de licencia dentro del paquete descargado. Registrar origen, autor y licencia en `CREDITOS_SONIDOS.md` (y en `credits` de `mods.toml` si es CC-BY). **No usar recursos de otros mods** (p. ej. Techguns: su licencia no lo permite).
- `gradle.properties` se lee como ISO-8859-1: los acentos deben ir como `é`.

## 5. Trampas conocidas (ya nos mordieron)

- PowerShell 5.1 muestra UTF-8 como `Ã©` — los archivos están bien; no "arregles" eso. Para escribir archivos con acentos usa Python o la herramienta de edición, no `Set-Content` sin `-Encoding utf8`.
- `CubeListBuilder.mirror()` solo afecta a cubos añadidos **después**: crea un builder nuevo por cada lado.
- Item `getDamage(ItemStack)` choca con `IForgeItem`: no uses ese nombre para métodos estáticos.
- En menús: al sacar un objeto, Minecraft deja **el mismo `ItemStack` con cantidad 0** en el contenedor → compara con `isEmpty()`, no por referencia (ver `WeaponBenchMenu`).
- `ContainerData` sincroniza valores de **16 bits** (máx. 32 767). La energía de la prensa (20 000) cabe; si subes capacidades, divide el valor.
- Overrides de modelos: Minecraft elige el **último** override cuyos predicados se cumplen → ordenar por máscara de módulos y luego por progreso de recarga (lo hace `pistol_gen.py`).
- Repos de dependencias: Registrate está en `maven.tterrag.com`; Crafts & Additions viene del maven de Modrinth **sin `exclusiveContent`** (rompe `fg.deobf`). Create necesita `mixinextras-forge` y las propiedades `mixin.env.*` de `build.gradle`.
- Botín o recetas con objetos de **otros mods** (Create): no los nombres directamente (si el mod falta, la tabla entera falla). Usa una etiqueta propia con entradas `{"id": ..., "required": false}` y una entrada de botín `minecraft:tag` (ver `tools/world_structures_gen.py`).
- En `postProcess` de estructuras los aleatorios no pueden depender del mundo ni del chunk (ver `RuinedBuildingPiece`).
- GameTests con entidades: créalas **en la zona del test** (chunk activo; fuera de ella se descargan y no hacen nada) y **al aire libre** (usa el heightmap; enterradas mueren asfixiadas y la prueba "pasa" por error). Comprueba quién hizo el daño (`getLastHurtByMob`) y evalúa antes de `discard()`.
- El renderizador de previsualización no tiene z-buffer: oculta caras interiores con una heurística; si algo "se ve encima", puede ser artefacto de la vista previa, no del juego.
