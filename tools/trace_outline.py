"""Developer tool (not part of the game): turns a track outline picture into a centerline CSV.

Usage: python3 tools/trace_outline.py <outline.png> <out.csv> <start_x> <start_y> <lap_meters> [min_gap_meters]

The picture must show the circuit as one dark closed line on a light background (like a circuit map
icon). The tool thins the line to a one-pixel skeleton (Zhang-Suen), walks it into an ordered loop,
smooths it, orders it counter-clockwise or clockwise to match the real circuit (pass a negative
lap_meters for clockwise), starts it at the pixel nearest (start_x, start_y) (the start/finish line),
scales it to the lap length and, if parts of the circuit that are far apart along the lap come closer
than min_gap_meters (default 66, the game's wide track plus a strip of grass), gently pushes them
apart. The result is APPROXIMATE: a traced drawing, not survey or telemetry data.
Needs numpy and Pillow.
"""
import sys

import numpy as np
from PIL import Image


def thin(mask):
    """Zhang-Suen thinning of a 0/1 array."""
    img = mask.copy()
    changed = True
    while changed:
        changed = False
        for step in (0, 1):
            p = np.pad(img, 1)
            n = [p[:-2, 1:-1], p[:-2, 2:], p[1:-1, 2:], p[2:, 2:], p[2:, 1:-1], p[2:, :-2], p[1:-1, :-2], p[:-2, :-2]]
            b = sum(n)
            seq = n + [n[0]]
            a = sum(((seq[i] == 0) & (seq[i + 1] == 1)).astype(int) for i in range(8))
            p2, p3, p4, p5, p6, p7, p8, p9 = n
            if step == 0:
                c = (p2 * p4 * p6 == 0) & (p4 * p6 * p8 == 0)
            else:
                c = (p2 * p4 * p8 == 0) & (p2 * p6 * p8 == 0)
            remove = (img == 1) & (b >= 2) & (b <= 6) & (a == 1) & c
            if remove.any():
                img[remove] = 0
                changed = True
    for _ in range(40):  # prune short spurs left by the thinning
        p = np.pad(img, 1)
        b = sum([p[:-2, 1:-1], p[:-2, 2:], p[1:-1, 2:], p[2:, 2:], p[2:, 1:-1], p[2:, :-2], p[1:-1, :-2], p[:-2, :-2]])
        ends = (img == 1) & (b <= 1)
        if not ends.any():
            break
        img[ends] = 0
    return img


def walk(skeleton):
    """Orders the skeleton pixels into a loop, preferring to keep going straight at forks."""
    ys, xs = np.nonzero(skeleton)
    pts = set(zip(ys.tolist(), xs.tolist()))
    start = min(pts, key=lambda q: (q[1], q[0]))
    path, seen, prev, cur = [start], {start}, None, start
    steps = [(-1, 0), (-1, 1), (0, 1), (1, 1), (1, 0), (1, -1), (0, -1), (-1, -1)]
    while True:
        cands = [(cur[0] + dy, cur[1] + dx) for dy, dx in steps
                 if (cur[0] + dy, cur[1] + dx) in pts and (cur[0] + dy, cur[1] + dx) not in seen]
        if not cands:
            break
        if prev is not None:
            vy, vx = cur[0] - prev[0], cur[1] - prev[1]
            cands.sort(key=lambda c: -((c[0] - cur[0]) * vy + (c[1] - cur[1]) * vx))
        prev, cur = cur, cands[0]
        path.append(cur)
        seen.add(cur)
    return np.array([(x, y) for y, x in path], float)


def separate(points, lap, min_gap, passes=300):
    """Pushes apart parts of the loop that are far apart along the lap but close on the map."""
    p = points.copy()
    n = len(p)
    for _ in range(passes):
        seg = np.hypot(*np.diff(np.vstack([p, p[:1]]), axis=0).T)
        cum = np.concatenate([[0], np.cumsum(seg)])[:n]
        along = np.abs(cum[:, None] - cum[None, :])
        along = np.minimum(along, cum[-1] + seg[-1] - along)
        dx = p[:, None, 0] - p[None, :, 0]
        dy = p[:, None, 1] - p[None, :, 1]
        dist = np.hypot(dx, dy) + 1e-9
        close = (along > 3 * min_gap) & (dist < min_gap)
        if not close.any():
            break
        push = np.where(close, (min_gap - dist) * 0.25, 0.0)
        p[:, 0] += (push * dx / dist).sum(1)
        p[:, 1] += (push * dy / dist).sum(1)
        p = 0.8 * p + 0.1 * np.roll(p, 1, 0) + 0.1 * np.roll(p, -1, 0)
    return p * (lap / np.sum(np.hypot(*np.diff(np.vstack([p, p[:1]]), axis=0).T)))


def main():
    src, out = sys.argv[1], sys.argv[2]
    sx, sy, lap = float(sys.argv[3]), float(sys.argv[4]), float(sys.argv[5])
    min_gap = float(sys.argv[6]) if len(sys.argv) > 6 else 66.0
    gray = np.array(Image.open(src).convert('L')).astype(int)
    loop = walk(thin((gray < 110).astype(np.uint8)))
    k = 7
    loop = np.array([loop[np.arange(i - k, i + k + 1) % len(loop)].mean(0) for i in range(len(loop))])[::3]
    area = 0.5 * np.sum(loop[:, 0] * np.roll(loop[:, 1], -1) - np.roll(loop[:, 0], -1) * loop[:, 1])
    counter_clockwise = lap > 0
    if (area > 0) == counter_clockwise:  # screen y points down, so positive area is clockwise on screen
        loop = loop[::-1]
    loop = np.roll(loop, -int(np.argmin(((loop - [sx, sy]) ** 2).sum(1))), axis=0)
    loop[:, 1] = -loop[:, 1]  # north up
    lap = abs(lap)
    loop *= lap / np.sum(np.hypot(*np.diff(np.vstack([loop, loop[:1]]), axis=0).T))
    loop = separate(loop, lap, min_gap)
    with open(out, 'w') as f:
        f.write('# APPROXIMATE TRACED CENTERLINE. NOT FastF1 OR SURVEY DATA.\n')
        f.write('# Traced by tools/trace_outline.py from a circuit outline picture; x,y in meters.\n')
        for x, y in loop:
            f.write('%.1f,%.1f\n' % (x, y))
    print('wrote', len(loop), 'points to', out)


if __name__ == '__main__':
    main()
