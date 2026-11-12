"""Actual/A/B en superficies 5x5 junto a netherrack, basalto, piedra negra y placa.
python tools/preview_nether_surface_art.py
Usa modelos/blockstates reales y texturas vanilla de la caché local; solo escribe build/.
"""
import json,random
from pathlib import Path
from PIL import Image,ImageDraw
import render_zbuffer as rz
import preview_world_block_round2 as shared
from nether_surface_art import A,OUT,IDS


def scene(lib,name,choice):
    result={'textures':{},'elements':[]};rng=random.Random(8715)
    selection=json.loads((OUT/choice/f'blockstates/{name}.json').read_text())['variants']['']
    for z in range(5):
        for x in range(5):lib.put(result,'tecnocinetico:block/'+name,(x*16,0,z*16),choice,selection=selection,rng=rng)
    for p in range(5):
        lib.put(result,'minecraft:block/netherrack',(-16,0,p*16))
        lib.put(result,'minecraft:block/basalt',(80,0,p*16))
        lib.put(result,'minecraft:block/blackstone',(p*16,0,80))
        lib.put(result,'tecnocinetico:block/carbonized_plate',(p*16,0,-16))
    return result


def main():
    # Reutiliza el lector de recursos, con una salida independiente de la lámina del Overworld.
    shared.R2=OUT;lib=shared.Models();original=rz.tex_path
    def paths(ref,textures):return Path(ref[5:]) if ref.startswith('file:') else original(ref,textures)
    rz.tex_path=paths
    sheet=Image.new('RGB',(2100,1500),(31,35,36))
    labels=('Escoria · A: fundición porosa / B: clinker ferroso enfriado','Azufre · A: costra ocre / B: racimos pálidos verdosos')
    detail=Image.new('RGB',(3*512,2*340),(31,35,36))
    for row,(name,label) in enumerate(zip(IDS,labels)):
        for col,choice in enumerate(('actual','A','B')):
            pic=rz.render(scene(lib,name,choice),215,56,5.9,size=1000)
            d=ImageDraw.Draw(pic);d.text((20,14),choice.upper()+' · '+name,font=shared.font(26),fill='#eee4d1')
            d.text((20,960),'5 × 5 · netherrack / basalto / piedra negra / placa carbonizada',font=shared.font(19),fill='#ddd8ca')
            pic.save(OUT/f'{name}_{choice}_5x5.png')
            sheet.paste(pic.resize((700,700),Image.Resampling.LANCZOS),(col*700,row*750+40))
            path=OUT/choice/f'textures/block/{name}.png'
            im=Image.open(path).convert('RGB').resize((256,256),Image.Resampling.NEAREST)
            detail.paste(im,(col*512+128,row*340+45))
            ImageDraw.Draw(detail).text((col*512+20,row*340+10),choice.upper()+' · '+name,font=shared.font(21),fill='#eee4d1')
        ImageDraw.Draw(sheet).text((15,row*750+8),label,font=shared.font(25),fill='#eee4d1')
    sheet.save(OUT/'comparativa.png');detail.save(OUT/'detalle_texturas.png')
    lib.jar.close();print('Lámina: build/nether_bloques/comparativa.png')


if __name__=='__main__':main()
