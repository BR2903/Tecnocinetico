"""Entrada de arte de bloques, segunda ronda. Geometría y píxel en world_block_round2.py.
python tools/world_blocks_gen.py --remodel-only --variant A
python tools/world_block_remodel.py --variant B --block mossy_concrete
Solo arte: no escribe Java, datos, recetas o idiomas.
"""
import argparse
from world_block_round2 import A, OUT, textures, beam_model, install


def main():
    parser=argparse.ArgumentParser()
    parser.add_argument('--variant',choices=('A','B'),default='A')
    parser.add_argument('--remodel-only',action='store_true')
    parser.add_argument('--block',choices=('all','mossy_concrete','rusted_metal_beam','dead_log','vitrified_sand'),default='all')
    args=parser.parse_args();install(args.variant,args.block)


if __name__=='__main__':main()
