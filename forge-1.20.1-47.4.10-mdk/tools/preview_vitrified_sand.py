"""Vanilla / actual / A / B en 5x5. Vanilla solo se lee para las imágenes de referencia en build/.
python tools/preview_vitrified_sand.py
"""
import json,random
from pathlib import Path
from PIL import Image,ImageDraw
import render_zbuffer as rz
import preview_world_block_round2 as shared
from vitrified_sand_art import OUT


def scene(lib,option):
    m={'textures':{},'elements':[]};rng=random.Random(917)
    selection=None if option=='vanilla' else json.loads((OUT/option/'blockstates/vitrified_sand.json').read_text())['variants']['']
    for z in range(5):
        for x in range(5):
            if option=='vanilla':lib.put(m,'minecraft:block/sand',(x*16,0,z*16))
            else:lib.put(m,'tecnocinetico:block/vitrified_sand',(x*16,0,z*16),option,selection=selection,rng=rng)
    for n in range(5):
        lib.put(m,'minecraft:block/sand',(-16,0,n*16));lib.put(m,'minecraft:block/sand',(n*16,0,-16))
        lib.put(m,'minecraft:block/sandstone',(n*16,0,80))
        lib.put(m,'tecnocinetico:block/ash_block' if n%2==0 else 'minecraft:block/stone',(80,0,n*16))
    return m


def main():
    shared.R2=OUT;lib=shared.Models();original=rz.tex_path
    def path(ref,textures):return Path(ref[5:]) if ref.startswith('file:') else original(ref,textures)
    rz.tex_path=path
    sheet=Image.new('RGB',(2800,920),(31,35,36));labels=('VANILLA · referencia','ACTUAL · vidrio miel','A · detalles mínimos','B · algo más de vidrio')
    for col,(option,label) in enumerate(zip(('vanilla','actual','A','B'),labels)):
        pic=rz.render(scene(lib,option),215,56,5.9,size=1000)
        ImageDraw.Draw(pic).text((20,16),label,font=shared.font(29),fill='#eae2cf')
        pic.save(OUT/f'arena_{option}_5x5.png');sheet.paste(pic.resize((700,700),Image.Resampling.LANCZOS),(col*700,0))
        if option=='vanilla':ref=lib.texture('minecraft:block/sand',{},'A')
        else:ref=OUT/option/'textures/block/vitrified_sand.png'
        tile=Image.open(ref).convert('RGB').resize((160,160),Image.Resampling.NEAREST)
        sheet.paste(tile,(col*700+270,715));ImageDraw.Draw(sheet).text((col*700+15,885),label,font=shared.font(21),fill='#eae2cf')
    sheet.save(OUT.parent/'comparativa_arena.png')
    lib.jar.close();print('Lámina: build/remodelar_bloques/comparativa_arena.png')


if __name__=='__main__':main()
