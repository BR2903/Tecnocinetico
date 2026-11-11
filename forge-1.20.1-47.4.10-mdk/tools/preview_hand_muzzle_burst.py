"""Vista de la onda local usando el anillo original, su alfa y la misma expansión que el renderer."""
from pathlib import Path
from PIL import Image,ImageDraw,ImageFont
import numpy as np

ROOT=Path(__file__).resolve().parent.parent


def main():
    texture=Image.open(ROOT/"src/main/resources/assets/tecnocinetico/textures/particle/kinetic_ring.png").convert("RGBA")
    source=np.array(texture,dtype=float)/255
    sheet=Image.new("RGBA",(1000,285),(34,39,45,255))
    font=ImageFont.truetype("C:/Windows/Fonts/segoeui.ttf",18)
    for i,age in enumerate((0,2,4,6)):
        t=age/7; radius=(.12+(.9-.12)*t)*.45
        color=source.copy(); color[:,:,:3]*=np.array([.30,.88,1])
        color[:,:,3]*=.95*(1-t*t)
        color[source[:,:,3]<.1,3]=0 # Descarte alfa del shader entity_translucent_emissive.
        frame=Image.fromarray((color*255).astype("uint8"))
        size=max(1,round(radius*650)); frame=frame.resize((size,size),Image.Resampling.NEAREST)
        sheet.alpha_composite(frame,(i*250+(250-size)//2,45+(220-size)//2))
        ImageDraw.Draw(sheet).text((i*250+12,10),f"Onda · tick {age}",font=font,fill="#dbe4e5")
    out=ROOT/"build/weapon_muzzles/onda_celeste.png"; out.parent.mkdir(parents=True,exist_ok=True)
    sheet.convert("RGB").save(out); print(out)


if __name__=="__main__": main()
