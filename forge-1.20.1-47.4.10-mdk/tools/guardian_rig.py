"""Rig único para el modelo Java y sus vistas: evita divergencias entre la previsualización y el juego."""
import json
import math
from pathlib import Path
import numpy as np

ROOT = Path(__file__).resolve().parent.parent
UV = {"armor": (0, 0), "dark": (64, 40), "copper": (0, 64), "glow": (96, 64),
      "ceramic": (0, 88), "fiber": (64, 88)}


class Rig:
    def __init__(self):
        self.parts = []

    def part(self, name, parent="body", at=(0, 0, 0), rot=(0, 0, 0)):
        self.parts.append({"name": name, "parent": parent, "at": at, "rot": rot, "cubes": []})
        return name

    def cube(self, part, material, xyz, size):
        next(p for p in self.parts if p["name"] == part)["cubes"].append((material, xyz, size))


def watcher():
    r = Rig()
    r.part("body", "root", (0, 10, 0))
    r.cube("body", "armor", (-6, -3, -12), (12, 6, 24))
    r.cube("body", "armor", (-9, -5, -8), (18, 3, 16))
    r.cube("body", "dark", (-4, 3, -8), (8, 3, 14))
    r.part("nose", at=(0, 0, -13))
    r.cube("nose", "armor", (-5, -2, -6), (10, 5, 9))
    r.cube("nose", "dark", (-4, -1, -7), (8, 3, 1))
    for side in (-1, 1):
        r.cube("nose", "glow", (side*2-1, 0, -8), (2, 1, 1))
        p = r.part("wing" + str(side+1), at=(side*7, -4, 0), rot=(0, 0, side*.38))
        r.cube(p, "armor", (-3, -1, -10), (6, 2, 20))
        r.cube(p, "ceramic", (-2, -2, -7), (4, 1, 14))
        p = r.part("tail" + str(side+1), at=(side*5, -5, 10), rot=(.25, 0, side*.45))
        r.cube(p, "armor", (-1, -8, -2), (2, 9, 7))
        r.cube(p, "ceramic", (-1, -7, -3), (2, 5, 1))
    r.part("radar", at=(0, -8, 0))
    r.cube("radar", "copper", (-1, -5, 0), (2, 6, 2))
    r.cube("radar", "ceramic", (-6, -6, -1), (12, 2, 4))
    r.cube("radar", "glow", (-2, -6, -2), (4, 1, 1))
    for i, (side, z) in enumerate([(-1, -10), (1, -10), (-1, 11), (1, 11)]):
        p = r.part(f"strut{i}", at=(side*9, 0, z), rot=(0, 0, side*-.12))
        r.cube(p, "dark", (-9 if side < 0 else 0, -1, -2), (9, 3, 4))
        turbine = r.part(f"turbine{i}", at=(side*18, 0, z))
        # Carcasa octogonal de cada turbina; el hueco central deja ver las aspas.
        for k in range(8):
            a = k*math.pi/4
            ring = r.part(f"ring{i}_{k}", turbine, (math.sin(a)*5, 0, math.cos(a)*5), (0, a, 0))
            r.cube(ring, "armor", (-2.5, -2, -1), (5, 4, 2))
        fan = r.part(f"fan{i}", turbine, (0, -.5, 0))
        r.cube(fan, "copper", (-1, -2, -1), (2, 4, 2))
        for k in range(4):
            blade = r.part(f"blade{i}_{k}", fan, rot=(0, k*math.pi/2, 0))
            r.cube(blade, "dark", (0, -1, -1), (5, 1, 2))
        r.cube(turbine, "glow", (-1, 2, -1), (2, 1, 2))
    for i, side in enumerate((-1, 1)):
        gun = r.part(f"gun{i}", at=(side*9, 5, -6))
        r.cube(gun, "armor", (-2, -2, -4), (4, 4, 7))
        r.cube(gun, "dark", (-1, -1, -17), (2, 2, 14))
        for z in (-13, -8): r.cube(gun, "copper", (-1.5, -1.5, z), (3, 3, 1))
        r.cube(gun, "glow", (-.5, -.5, -18), (1, 1, 1))
        r.part(f"capacitorMount{i}", at=(side*9, 3, 12))
        r.cube(f"capacitorMount{i}", "dark", (-3, -4, -2), (6, 8, 4))
        cap = r.part(f"capacitor{i}", at=(side*9, 3, 12))
        r.cube(cap, "glow", (-1.5, -3, 0), (3, 6, 3))
        for y in (-3, 2): r.cube(cap, "copper", (-2, y, -1), (4, 1, 5))
    return r


def matriarch():
    r = Rig()
    r.part("body", "root", (0, 8, 0))
    r.cube("body", "fiber", (-6, -2, -10), (12, 7, 20))
    r.cube("body", "armor", (-7, -4, -9), (14, 4, 16))
    abdomen = r.part("abdomen", at=(0, -1, 8), rot=(-.12, 0, 0))
    r.cube(abdomen, "fiber", (-6, -2, -3), (12, 10, 20))
    for i, w in enumerate((16, 20, 18, 12)):
        shell = r.part(f"shell{i}", abdomen, (0, -4 + i*.5, i*5))
        r.cube(shell, "armor", (-w/2, -2, -3), (w, 5, 7))
        for j, side in enumerate((-1, 1)):
            plate = r.part(f"shellFlank{i}_{j}", shell, (side*(w/2-2), 2, 0), (0, 0, side*-.5))
            r.cube(plate, "armor", (-2, -1, -3), (4, 5, 7))
            r.cube(plate, "copper", (-1, 3, -2), (2, 1, 5))
        spine = r.part(f"spine{i}", shell, (0, -4, 1), rot=(-.45, 0, 0))
        r.cube(spine, "ceramic", (-1, -3, -1), (2, 5, 3))
    head = r.part("head", at=(0, 0, -13))
    r.cube(head, "armor", (-5, -3, -7), (10, 7, 10))
    r.cube(head, "dark", (-3, 1, -8), (6, 3, 1))
    for side in (-1, 1):
        plate = r.part(f"cheek{side+1}", head, (side*5, 0, -2), rot=(0, side*.32, side*-.15))
        r.cube(plate, "armor", (-1, -3, -4), (2, 7, 9))
        for eye in range(3): r.cube(head, "glow", (side*(eye+1)-.5, -2 + eye*.6, -8), (1, 1, 1))
    for i, side in enumerate((-1, 1)):
        jaw = r.part(f"jaw{i}", head, (side*4, 3, -5), rot=(0, side*.28, 0))
        r.cube(jaw, "dark", (-1, -1, -11), (2, 3, 12))
        hook = r.part(f"hook{i}", jaw, (0, 0, -10), rot=(.4, side*-.5, 0))
        r.cube(hook, "ceramic", (-1, -3, -2), (2, 7, 2))
    for i in range(6):
        side = -1 if i < 3 else 1
        leg = r.part(f"leg{i}", at=(side*7, 3, (i%3)*11-8), rot=(0, -side*(math.pi/2 + (i%3-1)*.35), 0))
        r.cube(leg, "dark", (-2, -2, -12), (4, 4, 12))
        r.cube(leg, "armor", (-2, -4, -10), (4, 2, 8))
        r.cube(leg, "copper", (-2, -2, -3), (4, 4, 1))
        knee = r.part(f"knee{i}", leg, (0, 1, -12))
        r.cube(knee, "armor", (-2.5, -2.5, -2.5), (5, 5, 5))
        r.cube(knee, "copper", (-1, -1, -3), (2, 2, 1))
        shin = r.part(f"shin{i}", knee, (0, 1, 0), rot=(-.2, 0, 0))
        r.cube(shin, "dark", (-1.5, 0, -1.5), (3, 10, 3))
        r.cube(shin, "armor", (-2, 1, -2), (4, 6, 2))
        foot = r.part(f"foot{i}", shin, (0, 10, -2))
        r.cube(foot, "armor", (-3, 0, -3), (6, 2, 6))
        for x in (-2, 1): r.cube(foot, "ceramic", (x, 0, -6), (1, 2, 4))
    return r


def f(n): return f"{float(n):.6f}F"


def java_parts(r):
    lines = []
    for p in r.parts:
        builder = "CubeListBuilder.create()"
        for material, xyz, size in p["cubes"]:
            u, v = UV[material]
            builder += f".texOffs({u}, {v}).addBox(" + ", ".join(f(n) for n in (*xyz, *size)) + ")"
        pose = "PartPose.offsetAndRotation(" + ", ".join(f(n) for n in (*p["at"], *p["rot"])) + ")"
        lines.append(f'        var {p["name"]} = {p["parent"]}.addOrReplaceChild("{p["name"]}", {builder}, {pose});')
    return "\n".join(lines)


def preview(r, name):
    import render_zbuffer
    transforms = {"root": np.eye(4)}
    elements = []
    for p in r.parts:
        x, y, z = p["rot"]
        cx, sx, cy, sy, cz, sz = math.cos(x), math.sin(x), math.cos(y), math.sin(y), math.cos(z), math.sin(z)
        rx = np.array([[1,0,0],[0,cx,-sx],[0,sx,cx]])
        ry = np.array([[cy,0,sy],[0,1,0],[-sy,0,cy]])
        rz = np.array([[cz,-sz,0],[sz,cz,0],[0,0,1]])
        m = np.eye(4); m[:3,:3] = rz @ ry @ rx; m[:3,3] = p["at"]
        m = transforms[p["parent"]] @ m
        transforms[p["name"]] = m
        for material, xyz, size in p["cubes"]:
            u, v = UV[material]; w, h, d = size
            faces = {"west": [u,v+d,u+d,v+d+h], "north": [u+d,v+d,u+d+w,v+d+h],
                     "east": [u+d+w,v+d,u+2*d+w,v+d+h], "south": [u+2*d+w,v+d,u+2*d+2*w,v+d+h],
                     "up": [u+d,v,u+d+w,v+d], "down": [u+d+w,v,u+d+2*w,v+d]}
            end = [a+b for a,b in zip(xyz, size)]
            verts = {}
            for face in faces:
                verts[face] = []
                for vertex in render_zbuffer.corners(xyz, end, face):
                    pworld = m @ np.array([*vertex, 1])
                    verts[face].append([float(pworld[0])+8, 24-float(pworld[1]), float(pworld[2])+8])
            pts = [p for face in verts.values() for p in face]
            elements.append({"from": np.min(pts,0).tolist(), "to": np.max(pts,0).tolist(), "preview_vertices": verts,
                             "faces": {face: {"texture": "#all", "uv": [n/8 for n in uv]} for face,uv in faces.items()}})
    out = ROOT / "build/guardian_previews"; out.mkdir(parents=True, exist_ok=True)
    (out/f"{name}.json").write_text(json.dumps({"textures": {"all": f"tecnocinetico:entity/{name}"}, "elements": elements}), encoding="utf-8")


def generate_model(template):
    wr, mr = watcher(), matriarch()
    for rig in (wr, mr):
        for part in rig.parts:
            for material, xyz, size in part["cubes"]:
                u, v = UV[material]; w, h, d = size
                if u + 2*(w+d) > 128 or v + d + h > 128:
                    raise ValueError(f"UV fuera del atlas: {part['name']} / {material} / {size}")
    target = ROOT / "src/main/java/com/bryanamaya/tecnocinetico/entity/client/OverworldGuardianModel.java"
    target.write_text(template.replace("WATCHER_PARTS", java_parts(wr)).replace("MATRIARCH_PARTS", java_parts(mr)), encoding="utf-8")
    preview(wr, "network_watcher"); preview(mr, "swarm_matriarch")
