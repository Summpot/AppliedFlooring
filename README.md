# Applied Flooring

A Minecraft mod that adds cheap, functional ME flooring blocks to **Applied Energistics 2 (AE2)** with embedded multipart attachment, wireless charging, and large-grid performance optimization.

## Features

1. **Economical ME Flooring Blocks**:
   - **ME Flooring (`appliedflooring:me_flooring`)**: Standard 8-channel floor block that conducts ME network data & power like an ME Glass Cable.
   - **Dense ME Flooring (`appliedflooring:dense_me_flooring`)**: 32-channel backbone floor block for dense routing.
   - **Cost-effective Crafting**: 1 ME Cable + 8 Stone/Slabs yields **8 ME Flooring blocks**, making base flooring extremely cost-effective.
   - **16 AE Colors**: Can be painted using the AE2 Color Applicator / Paint balls or crafted with dyes.

2. **Functional Part & Terminal Embedding**:
   - Built on `IPartHost` & `IGridHost`. Terminals, Import/Export buses, Storage buses, Level Emitters, and Illumination Panels can be placed and embedded directly on the floor.

3. **Wireless Charging Flooring (`appliedflooring:wireless_charger_flooring`)**:
   - Automatically charges items (Wireless Terminals, Energy Cells, FE/Forge Energy powered tools and armor) in the player's inventory when standing on the flooring using energy from the connected AE network.

4. **Terminal / Relay Flooring (`appliedflooring:terminal_flooring`)**:
   - Enhances wireless network access and direct terminal interaction on the floor.

5. **Performance Optimized for Large Bases**:
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
