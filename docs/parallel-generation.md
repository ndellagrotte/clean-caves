# Parallel cave generation

Clean Caves computes the pure parts of cave generation on helper threads. Everything that touches
the chunk and everything that draws a random number stays on the thread that is generating the
chunk. This document describes what is split, what is not, and why the result is identical to a
fully sequential run.

The idea comes from [CaveSpeedThing](https://www.curseforge.com/minecraft/mc-mods/cavespeedthing)
by ChristKind, which parallelizes Better Caves carving by generating the noise for a chunk in
parallel and applying it sequentially, and from
[NoiseThreader](https://www.curseforge.com/minecraft/mc-mods/noisethreader) by fonnymunkey, which
inspired it. The implementation here is independent and shaped around Clean Caves' own generators.

## The rule

A piece of work may run on a helper thread only if:

1. its result does not depend on the order it is computed in,
2. it only reads state that is not being written, and
3. it only writes state that no other task touches.

The server thread then consumes the results in exactly the order the sequential implementation
would have produced them, making every random choice and every block write. Splitting a workload
therefore changes *when* a value is computed, never *what* the value is or the order the values are
used in.

The generators were already written for a single chunk at a time, so a generator instance's
mutable buffers (`MapGenerator.rand`, `PositionFlags`, `RavineGenerator.mut`, ...) are not touched
by helper threads. Only the compiled, immutable settings, the noise generators, and the
per-column result arrays are shared.

## What is split

### Columns: stone layers, caverns and clusters

These generators are whole-column loops: for each of the 256 columns of a chunk they decide a
height range and evaluate noise for some or all of the column's y coordinates. Those decisions are
pure -- they depend on the world seed, the noise settings and (for layers and caverns) the chunk's
heightmap or primer -- so one task computes one column and records its decisions in a bitmask.

- **Stone layers** evaluate biome, region and 2D noise, then record the y coordinates whose block
  matches the layer's `matchers`.
- **Caverns** evaluate the nearest region border, the floor/ceiling height, the vertical offset and
  the 3D cave noise, then record whether each y coordinate is carved (`place`), a shell only, or
  neither. For carved blocks they also record which of the cavern's `generators` would carve the
  block and which would shell it, so the sequential pass does not evaluate that noise again.
- **Clusters** evaluate the floor/ceiling height and 3D noise for every group of clusters.

The server thread then walks the columns in the original x-then-z order and performs the same
writes, in the same y order, with the same random draws. Biome lookups are made on the server
thread before the split, because `BiomeProvider` and its `GenLayer` caches are not thread-safe.

### Sphere fills: tunnels, ravines and rooms

A tunnel, ravine or room segment is a chain of spheres. Building the set of block positions inside
a sphere is a pure triple loop over its bounding box. When the bounding box is large enough, its x
axis is divided into contiguous bands that are filled on separate threads; each band writes its own
position list and the lists are merged in ascending x order. That is the order the single-threaded
loop appends positions in, so the decoration pass sees the same list, in the same order, and draws
the same random numbers.

The shape choice (a plain sphere or a sphere with an outer shell) is still drawn on the server
thread before the fill, and the water test, shell placement, block replacement and decoration all
stay sequential.

## What is not split

- Block writes, decoration, ponds and wall/shell placement.
- Anything that consumes `Random`.
- Biome lookups through `BiomeProvider`.
- Feature generation (`WorldContext`), structures and speleothems, which run during population.
- Burrows and tunnel connectors: their per-column loops mix random choices with the writes, so
  they keep the sequential path until the same separation can be made for them.

Vanilla 1.12.2 generates one chunk at a time on the server thread, which is what makes sharing the
compiled generators safe. The helper pool is an implementation detail of *one* chunk's generation;
it does not generate several chunks at once.

## Workload detection

Splitting is only worth it when the work is large enough to pay for handing it out. Every call site
(`generation.layers`, `generation.caverns`, `generation.clusters`, `generation.spheres`) measures
its own cost per unit of work and the speedup of previous splits, per size class, and keeps small
work inline. The first calls of a session are inline, which is what teaches the site its cost.
When a size class stops paying off, it is retried occasionally rather than never, because other
work sharing the server thread can change how much a split is worth. A split always includes the
server thread itself, and helpers pull indices from one shared counter, so no thread waits on work
that has not started.

## Configuration

`config/cleancaves.cfg`:

| Option | Default | Meaning |
|---|---|---|
| `parallelGeneration` | `true` | `false` keeps every call inline on the server thread. |
| `maxGeneratorThreads` | `0` | Helper threads; `0` chooses automatically. Capped at 7 either way. |

The automatic choice is CPU threads minus a reserve for the server thread, the client renderer
and, when present, Pulsar's light workers (a third of the CPU threads, at least one). Machines with
two or fewer CPU threads stay sequential.

Helper threads are daemon threads named `CleanCaves-Worker-N`. They are created lazily and shut
down when the server stops.
