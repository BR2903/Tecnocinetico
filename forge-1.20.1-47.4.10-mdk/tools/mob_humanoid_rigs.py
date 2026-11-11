"""Relieve de corruptos y supervivientes sobre el esqueleto humano vanilla: conserva armas y armaduras."""
from guardian_rig import Rig, UV
from mob_remodel_rigs import panel

UV.update({"skin_head":(0,0), "skin_body":(16,16), "skin_arm":(40,16), "skin_leg":(0,16),
           "human_metal":(0,64), "human_copper":(64,64), "human_glow":(96,64),
           "cloth":(0,96), "human_fiber":(64,96)})
BASE_PARTS={"head","body","right_arm","left_arm","right_leg","left_leg"}


def human(corrupted):
    r=Rig()
    for name,at in [("head",(0,0,0)),("body",(0,0,0)),("right_arm",(-5,2,0)),("left_arm",(5,2,0)),
                    ("right_leg",(-1.9,12,0)),("left_leg",(1.9,12,0))]:
        r.part(name,"root",at)
        if name=="head": r.cube(name,"skin_head",(-4,-8,-4),(8,8,8))
        elif name=="body": r.cube(name,"skin_body",(-4,0,-2),(8,12,4))
        elif "arm" in name: r.cube(name,"skin_arm",(-1 if name=="left_arm" else -3,-2,-2),(4,12,4))
        else: r.cube(name,"skin_leg",(-2,0,-2),(4,12,4))
    if corrupted:
        panel(r,"cheekImplant","head",(-2.8,-2.5,-4.5),(3,4,2),(0,0,-.14),"human_metal")
        panel(r,"clavicle","body",(2.8,1.5,-2.7),(4,2,2),(0,0,-.4),"human_metal")
        panel(r,"ribCable","body",(2,5.5,-2.8),(1,7,1),(0,0,.25),"human_copper")
        panel(r,"infectedShoulder","left_arm",(1,-2,0),(5,3,5),(0,0,-.2),"human_fiber")
        claw=panel(r,"leftClaw","left_arm",(1,8,0),(5,4,5),material="human_fiber")
        for x in (-1,1,3): r.cube(claw,"human_metal",(x-1,1,-2),(1,3,2))
        panel(r,"prostheticKnee","right_leg",(0,6,-2.5),(4,3,2),material="human_metal")
        for i in range(3): panel(r,f"tendon{i}","body",(3.8,4+i*2,1),(1,1,5),(0,0,-.2),"human_copper")
        panel(r,"armBrace","right_arm",(-1,5,-2),(4,6,1),material="human_metal")
        panel(r,"spinalRail","body",(0,5,2.7),(2,9,1),material="human_copper")
        pack=panel(r,"worker_pack","body",(0,6,4),(7,8,3),material="human_metal")
        r.cube(pack,"human_copper",(-2,-3,1.5),(4,1,1))
        growth=r.part("crystal_growth","left_arm",(1,-3,0))
        for i in range(3): panel(r,f"crystal{i}",growth,(i*1.4-1,-i,0),(1,5,2),(0,.2*i,.25*i),"human_glow")
        panel(r,"raider_pad","right_arm",(-1,-3,0),(5,2,6),(0,0,.12),"human_metal")
        helmet=panel(r,"soldier_helmet","head",(0,-8,0),(9,2,9),material="human_metal")
        r.cube(helmet,"human_metal",(-4,-.5,-5),(8,2,1))
        panel(r,"soldier_chest","body",(0,4.5,-3),(7,7,2),material="human_metal")
        panel(r,"worker_helmet","head",(0,-8.5,0),(10,2,10),material="human_copper")
        panel(r,"raider_mask","head",(0,-1,-4.8),(5,3,2),material="human_fiber")
        panel(r,"drowned_tank","body",(0,6,4),(6,9,4),material="human_copper")
        mask=panel(r,"drowned_mask","head",(0,-3,-4.6),(5,3,2),material="human_metal")
        r.cube(mask,"human_glow",(-1,-.5,-1.5),(2,1,1))
    else:
        for side,arm in [(-1,"right_arm"),(1,"left_arm")]:
            panel(r,"elbow"+str(side+1),arm,(side,5,1.8),(4,3,1),material="cloth")
        for side,leg in [(-1,"right_leg"),(1,"left_leg")]:
            panel(r,"kneepad"+str(side+1),leg,(0,5,-2),(3,3,1),material="cloth")
        panel(r,"beltPouch","body",(-3,10,-2.8),(3,3,2),material="cloth")
        panel(r,"shoulderStrap","body",(1,5,-2.8),(1,10,1),(0,0,.3),"cloth")
        pack=panel(r,"scrapper_pack","body",(0,5,4.5),(8,9,4),material="cloth")
        r.cube(pack,"human_copper",(-3,-3,2),(1,7,1))
        r.cube(pack,"human_metal",(1,-5,2),(3,4,1))
        goggles=panel(r,"scrapper_goggles","head",(0,-6,-4.4),(7,2,1),material="human_copper")
        for x in (-2,1): r.cube(goggles,"human_metal",(x,-.5,-1),(2,1,1))
        panel(r,"gunsmith_apron","body",(0,6,-2.6),(7,10,1),material="cloth")
        panel(r,"gunsmith_holster","body",(4.5,10,0),(2,5,4),material="cloth")
        hat=panel(r,"farmer_hat","head",(0,-8,0),(12,1,12),material="cloth")
        r.cube(hat,"cloth",(-4,-3,-4),(8,3,8))
        panel(r,"farmer_basket","body",(0,6,4.5),(7,8,5),material="cloth")
    return r
