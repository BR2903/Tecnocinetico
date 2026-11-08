# Créditos de sonidos

Sonidos de terceros usados en Tecnocinético. Los originales están en `tools/sound_sources/` y se procesan con
`tools/import_sounds.py` (mezcla a mono, recorte, normalización y un chisporroteo cinético sutil en los disparos).
**No se usa material de ningún otro mod** (se revisó Techguns: su licencia no permite reutilizar sus recursos).

| Archivo del mod | Origen | Autor | Licencia |
|---|---|---|---|
| `sounds/pistol/fire1-4.ogg` | "Gunshot Sounds" — `cz.wav` (pistola CZ-52 en campo de tiro) — https://opengameart.org/content/gunshot-sounds | Vincent Sevedge ("Tabasco"), 2009 | **CC-BY 3.0** (según el archivo `creativecommons.txt` incluido en el paquete; la página de OpenGameArt indica CC0 — se aplica la más estricta y se da crédito) https://creativecommons.org/licenses/by/3.0/ |
| `sounds/guns/revolver1-2`, `smg1-3.ogg` | Mismo paquete "Gunshot Sounds" — `cz.wav` (más grave / recortado) | Vincent Sevedge ("Tabasco"), 2009 | **CC-BY 3.0** |
| `sounds/guns/rifle1-3`, `bolt`, `sniper1-2.ogg` | Mismo paquete — `mosin.wav` (rifle Mosin-Nagant; `bolt` es el cerrojo grabado entre disparos) | Vincent Sevedge ("Tabasco"), 2009 | **CC-BY 3.0** |
| `sounds/guns/semi1-3`, `auto1-3`, `minigun1-2.ogg` | Mismo paquete — `sks.wav` (fusil SKS) | Vincent Sevedge ("Tabasco"), 2009 | **CC-BY 3.0** |
| `sounds/guns/shotgun1-2.ogg` | Mismo paquete — `shotty.wav` (escopeta) | Vincent Sevedge ("Tabasco"), 2009 | **CC-BY 3.0** |
| `sounds/pistol/mag_out.ogg`, `mag_in.ogg`, `slide.ogg` | "Handgun Reload Sound Effect" — `reload.wav` — https://opengameart.org/content/handgun-reload-sound-effect | zer0_sol | CC0 |
| `sounds/pistol/dry_fire.ogg` | "Gun Reload Sound Effects" — `singlebullet1.wav` — https://opengameart.org/content/gun-reload-sound-effects | BMacZero | CC0 |
| (fuente disponible, no usada aún) `clipload1.wav`, `clipload2.wav` | "Gun Reload Sound Effects" | BMacZero | CC0 |

Sonidos **propios** (sintetizados con `tools/sound_gen.py`, sin material de terceros): impacto y confirmación de
acierto de la pistola, todos los sonidos del Cuerpo de Enjambre, y en `sounds/guns/` el riel, el cañón de impulso, el arco
voltaico, el lanzagranadas, el lanzacohetes, el giro de la minigun y el tajo de la cuchilla; el rugido del lanzallamas.

Cambios realizados sobre las grabaciones (exigido por CC-BY): recortadas, convertidas a mono 44,1 kHz,
normalizadas y, en los disparos, mezcladas con un chisporroteo eléctrico sintetizado.
