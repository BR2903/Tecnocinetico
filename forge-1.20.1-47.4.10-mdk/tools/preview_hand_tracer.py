"""Vista de las partículas de la estela local con su textura, color y desvanecimiento reales."""
from pathlib import Path
from PIL import Image,ImageDraw,ImageFont
import numpy as np

ROOT=Path(__file__).resolve().parent.parent


def main():
    path=ROOT/"src/main/resources/assets/tecnocinetico/textures/particle/kinetic_glow.png"
    source=np.array(Image.open(path).convert("RGBA"),dtype=float)/255
    sheet=Image.new("RGBA",(900,300),(34,39,45,255))
    font=ImageFont.truetype("C:/Windows/Fonts/segoeui.ttf",18)
    for row,age in enumerate((0,2,4)):
        t=age/6; radius=.09+(.02-.09)*t
        color=source.copy(); color[:,:,:3]*=np.array([.30,.88,1])
        color[:,:,3]*=.95*(1-t*t)
        color[source[:,:,3]<.1,3]=0
        sprite=Image.fromarray((color*255).astype("uint8"))
        ImageDraw.Draw(sheet).text((12,row*100+8),f"Estela · tick {age}",font=font,fill="#dbe4e5")
        for i in range(12):
            size=max(1,round(radius*600/(1+i*.15)))
            particle=sprite.resize((size,size),Image.Resampling.NEAREST)
            sheet.alpha_composite(particle,(145+i*62-size//2,row*100+55-size//2))
    out=ROOT/"build/weapon_muzzles/estela_celeste.png"; out.parent.mkdir(parents=True,exist_ok=True)
    sheet.convert("RGB").save(out); print(out)


if __name__=="__main__": main()
