# Verification

Target: Minecraft 26.3, Fabric Loader 0.20.1, Fabric API 0.165.2+26.3 and Java 25. Local verification used Temurin 25.0.5+8, Gradle 9.6.1 and Fabric Loom 1.17.21.

## Build and packaging

The Gradle build completed successfully. The installable JAR was inspected separately: it contains two mod classes, Fabric metadata, Mixin configuration, the default block tag and the MIT notice. Game classes, Fabric dependencies, native test code and runtime data are not included.

```text
deepslate-instamine-1.1.0.jar
SHA-256 f3029999e2e8934228f7967bf8a949f66bcae9703a03e6941b224eb3f804344a
```

## Native mining checks

On **2026-11-08**, both fresh native server phases passed, with successful process exits and phase-specific receipts:

| Phase | Checks | Result |
| --- | ---: | --- |
| Default tag | 20 | PASS |
| Datapack adds stone and bedrock | 20 | PASS |

**Total: 40 passing native checks.**

The test harness launches disposable dedicated servers through Loom. It uses a synthetic server player with real item enchantments, native equipment-attribute updates and Minecraft's mining-progress methods. It does not mock the mining-speed calculation. The water check supplies a synthetic submerged-eye condition; Minecraft applies the actual submerged-speed attribute.

These are **development-runtime checks against the source-set outputs**, not proof of a graphical client connection or of the packaged JAR being loaded by that fixture. The fixture record includes the contemporaneous build's hash for traceability; packaging is verified separately.

The checks cover:

- Exact default tag contents and a second datapack adding stone and bedrock.
- Netherite Efficiency V + Haste II reaching one-tick stone-equivalent progress.
- Higher Haste, wrong tools, missing Haste, Haste I, Efficiency IV and missing Efficiency.
- Retained Mining Fatigue, airborne and submerged mining penalties.
- Untagged cobbled deepslate remaining unchanged.
- Stone recursion prevention and bedrock retaining zero progress.

Run both phases with Python 3 and JDK 25:

```text
python tests/run.py --java-home <JDK-25-directory> --accept-eula
```

`--accept-eula` is explicit acceptance for the disposable Minecraft servers. Each run uses a new ignored directory under `local/`, a generated flat world, loopback networking and no RCON. No existing world is loaded or deleted. The helper requires a fresh phase-specific PASS receipt as well as a successful process exit. Logs, receipts and worlds remain local. `--phase default` or `--phase optin` selects one phase.

Normal `gradlew build` compiles and packages the mod; it has no standalone unit-test suite and does not run Minecraft. `gradlew -PintegrationTests testmodClasses` additionally compiles the native harness. CI performs compilation/packaging and harness compilation, not server or client gameplay tests.

## Remaining manual checks

Not performed: actual graphical-client mining/prediction, real multiplayer connections, actual block breaking with drops/durability, Adventure/Spectator enforcement in gameplay, anti-cheat compatibility and interactions with other mining mods.

Before adding it to an important world, use a disposable client-and-server installation and check ordinary deepslate, each missing requirement, underwater/airborne mining, Mining Fatigue, Silk Touch, durability and your protection/anti-cheat mods. The mod delegates actual breaking to Minecraft; this design fact is not a substitute for those acceptance checks.
