# Applied Flooring

A Minecraft mod that adds cheap, functional ME flooring blocks to **Applied Energistics 2 (AE2)** with embedded multipart attachment, wireless charging, dense 32-channel transmission, and seamless high-tech aesthetics.

## Features

1. **Unified All-in-One ME Flooring Block (`appliedflooring:me_flooring`)**:
   - **Dense 32-Channel ME Transmission**: Conducts ME network data & power with full 32-channel dense capacity.
   - **Seamless Aesthetic Texture**: Near-solid clean slate background with subtle AE2-style micro-circuitry traces that tile seamlessly across adjacent blocks without border seams or center squares.
   - **16 AE Colors**: Available in 16 AE2 colors + uncolored default. Can be painted using the AE2 Color Applicator / Paint balls or crafted with dyes.

2. **Functional Part & Terminal Embedding (`IPartHost`)**:
   - Direct multipart hosting: Terminals, Import/Export buses, Storage buses, Level Emitters, and Illumination Panels can be placed and embedded directly on the floor.

3. **Integrated Wireless Charging**:
   - Automatically charges items (Wireless Terminals, Energy Cells, FE/Forge Energy powered tools and armor) in the player's inventory when standing on the flooring using energy from the connected AE network.

4. **Performance Optimized for Large Bases**:
   - Non-ticking base grid nodes (`GridFlags.PREFERRED`), lightweight quad caching, and seamless chunk load/unload handling.

## Multi-Loader Architecture

- Built using **Architectury Loom** and **Kotlin**.
- Direct root-level module structure supporting:
  - **1.20.1**: Fabric & Forge
  - **1.21.1**: NeoForge
  - **1.19.2**: Fabric & Forge

## Building

```bash
# Build Fabric 1.20.1
./gradlew :fabric-1.20.1:build

# Build Forge 1.20.1
./gradlew :forge-1.20.1:build
```
