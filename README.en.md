# SPD Trainer

**A trainer and testing toolkit for [Shattered Pixel Dungeon](https://shatteredpixel.com)**
Runs on top of the **official game build**: the game jar is not modified, rebuilt or replaced — the trainer is loaded as a separate jar overlay at launch.

[Русская версия](README.md)

---

## Features

The menu opens in-game: **ESC → the "Trainer" button**. One large window with tabs at the bottom.

| Tab | What it does |
|---|---|
| **Cheats** | God mode (damage interception), full health, +1000 gold, +1 level, +1 strength, map reveal, hunger removal, wand recharging |
| **Items** | 18 categories: weapons, missiles, armor, wands, rings, artifacts, potions, scrolls, runestones, seeds, food, trinkets, exotic potions/scrolls, bombs, keys, quest items, misc. Item editor: quantity, upgrade level (±2 billion), wand charges (uncapped), curse/known toggles, weapon enchantments and curses, armor glyphs and curses |
| **Hero** | HP, max HP, level, strength, gold — exact values up to 2 billion; exact hunger stages (fed/hungry/starving); talent reset |
| **Buffs** | ~80 statuses with icons: positive, negative, class mechanics; special modes (Barrier — shield size, Healing — heal pool); an "on nearest mob" section (debuffs and sniper marks — on enemies); remove all buffs |
| **Mobs** | The game's full bestiary (from the internal `Bestiary` lists): 5 zones, bosses (all of Yog's fists and the larva), universal, rare, quest mobs (including the vault), NPCs, allies (mirrors, wand wards, etc.). Spawn with a chosen count, ally mode, floor cleanup |
| **Dungeon** | Teleport to floor 1–25, **teleport to an arbitrary point** (cell picking), to the floor entrance/exit, view and set the generation seed |
| **Info** | **Live state inspector**: the panel refreshes 3 times/sec — floor/branch/level type, hero coordinates, HP/HT/level/strength/XP, gold, seed, turn counter, statistics, mob factions, item piles and traps, active buffs with remaining turns. The **"Inspect cell"** button — pick a cell and get a debug summary: tile, trap, items, mob with HP and buffs |

Numeric fields accept values up to the 32-bit limit. Deliberate restrictions: hero level 1–30 (the game's talent threshold array), floors 1–25, mobs per spawn ≤ 100 (performance).

Every action is written to the log (`[SPD-TRAINER] ...`), including stack traces on errors.

## Quick start

1. Install **JDK 17+** (tested on 25; `java`/`javac` must be in PATH).
2. Download the official **Shattered Pixel Dungeon (Java)** from [shatteredpixel.com](https://shatteredpixel.com) or the [game's releases](https://github.com/00-Evan/shattered-pixel-dungeon/releases) and put `ShatteredPD-vX.X.X-Java.jar` into the repository root.
3. Run **`SPD-Trainer.bat`** — the game starts with the trainer (a ready `dist/spd-trainer.jar` is included; the console minimizes itself, output goes to `trainer.log`).
4. In game: **ESC → "Trainer"**.

## Building from source

Only a JDK is required: a small overlay (~90 KB) is rebuilt, **the game is not rebuilt** — `javac` compiles our code directly against the official jar.

### Windows

```bat
build-trainer.bat
```

Result: `dist\spd-trainer.jar`. If `jar.exe` is not in PATH, the script locates it in the standard JDK folder by itself.

### Linux / macOS / manual

```bash
mkdir -p build/classes dist
javac --release 11 -encoding UTF-8 \
  -cp ShatteredPD-v4.0.2-Java.jar -d build/classes \
  src/main/java/com/shatteredpixel/shatteredpixeldungeon/trainer/*.java \
  src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/Char.java \
  src/main/java/com/shatteredpixel/shatteredpixeldungeon/windows/WndGame.java
jar --create --file dist/spd-trainer.jar -C build/classes .
```

Manual launch (what `spd-trainer-run.cmd` does):

```bash
java -DSpecification-Title="Shattered Pixel Dungeon" -DSpecification-Version=4.0.2 \
  -DImplementation-Title=com.shatteredpixel.shatteredpixeldungeon -DImplementation-Version=922 \
  --enable-native-access=ALL-UNNAMED \
  -cp "dist/spd-trainer.jar;ShatteredPD-v4.0.2-Java.jar" \
  com.shatteredpixel.shatteredpixeldungeon.desktop.DesktopLauncher
```

(on Linux/macOS the classpath separator is `:` instead of `;`).

The `-D` properties are mandatory: they replicate the official jar's manifest so the game uses **the same saves and settings** as a regular launch.

### Verifying the build

`tools/TestLoad.java` is a link test: it checks that every overlay class links against the official jar.

```bash
javac -cp ShatteredPD-v4.0.2-Java.jar -d build tools/TestLoad.java
java  -cp "dist/spd-trainer.jar;ShatteredPD-v4.0.2-Java.jar;build" TestLoad
```

Expected output: `ALL CLASSES LINKED` and a list of classes loaded from the overlay.

## How it works

The game is a Java application (libGDX on top of a custom noosa layer). The trainer uses **class shadowing**: our `dist/spd-trainer.jar` comes **first** in the classpath, the official jar second. The official jar's packages are not sealed (no `Sealed` in the manifest), so same-named classes load from the overlay while everything else comes from the untouched game.

Overlay contents:

- **The `trainer` package** (our code only, in `src/`): `Trainer` — all logic; `WndTrainer` — the main tabbed window; `WndTrainerItem` — the item editor; `TrainerSpinner` — a spinner with an input dialog (Set/Cancel); `WndInspectCell` — the cell inspection window.
- **Two patched game classes** (full copies of the v4.0.2 sources with minimal insertions; see the reference diff in `patches/game-over-v4.0.2.patch`):
  - `Char.java` — 5 lines: god mode in `damage()`;
  - `WndGame.java` — the "Trainer" button in the game menu.
- Compilation runs **against the official jar** — a full clone of the game sources is not needed for builds. The full v4.0.2 sources live in the [upstream repo](https://github.com/00-Evan/shattered-pixel-dungeon).

Key engine quirks we had to account for (documented so nobody steps on them again):

- trainer windows **ignore the inherited offset** (`Window.offset`) — otherwise scroll content "drifts" half a window;
- the scroll pane **self-heals every frame** (`RefreshableScrollPane`) — something in the game moves the content camera;
- the scroll pane is created **before** filling it with buttons, or its controller swallows clicks (event stack: whoever registers later is on top);
- floor teleportation goes through `InterlevelScene.Mode.RETURN` (the `DESCEND` path requires a transition object and crashes with an NPE);
- the game clips missile "sets" to `defaultQuantity()` when stacks merge — the trainer pre-splits the quantity into separate sets.

## Development

To browse the game sources alongside (not required for builds):

```bash
git clone --branch v4.0.2 --depth 1 https://github.com/00-Evan/shattered-pixel-dungeon.git game
```

The `game/` folder is in `.gitignore` — it never enters the repository.

Where to look:

| Task | File |
|---|---|
| New action (cheat) | `src/.../trainer/Trainer.java` — method + logging; call it from a tab |
| New tab | `src/.../trainer/WndTrainer.java`: a `TAB_*` constant, a name in `TAB_NAMES`, a `buildXxxTab` builder, a `switch` branch |
| New item category | `WndTrainer.java`: `CAT_NAMES` + an empty `CATEGORIES` entry + an `EXTRAS` list |
| New buffs/mobs in lists | `WndTrainer.java`: the `BUFF_SECTIONS` / `MOB_SECTIONS` arrays |
| Enchantments/glyphs | `WndTrainerItem.java`: the `WEAPON_ENCHANTS`, `ARMOR_GLYPHS` etc. arrays |
| List of compiled files | `build-trainer.bat` (it doubles as the registry of game modifications) |

Modified game classes must always be added to `build-trainer.bat` — forget to add one and the class won't get into the overlay.

## Troubleshooting

| Symptom | Cause and fix |
|---|---|
| The bat flashes and disappears, game doesn't start | `java` is not in PATH. Install a JDK and restart the terminal, or put the full path into `spd-trainer-run.cmd` |
| `PACKAGING FAILED` during build | The game is running and holds `dist/spd-trainer.jar` locked. Close the game and rebuild |
| The menu doesn't open | Make sure the game was launched via `SPD-Trainer.bat`, not on its own |
| Something broke in game | Check `trainer.log` — it holds the trainer's logs and stack traces |
| Numbers above 2,147,483,647 | That's the int (32-bit) ceiling — type the value in; the spinner arrows won't reach it |

## Updating to newer game versions

The overlay is pinned to v4.0.2. For a new version: download the new official jar, clone that version's sources (see "Development"), compare our two copies in `src/` (`Char.java`, `WndGame.java`) against the new sources and re-apply the insertions (`patches/game-over-v4.0.2.patch` shows how small the changes are); if APIs changed, adjust the calls in the `trainer` package.

## Repository structure

```
spd-trainer/
├── SPD-Trainer.bat          # launches the game with the trainer (minimized console → trainer.log)
├── spd-trainer-run.cmd      # one-line java launch (called by the bat)
├── build-trainer.bat        # builds dist/spd-trainer.jar
├── src/main/java/.../       # trainer sources:
│   ├── trainer/             #   our package (Trainer, windows, spinner)
│   ├── actors/Char.java     #   patched copy (god mode)
│   └── windows/WndGame.java #   patched copy (the "Trainer" button)
├── dist/spd-trainer.jar     # built overlay (not stored in git)
├── tools/TestLoad.java      # link test of the overlay against the official jar
├── patches/                 # reference diff of the modified game classes
├── game/                    # optional clone of the game sources (not stored in git)
└── README.md
```

## Status and roadmap

- [x] Core: jar overlay, menu, tabs, spinners, input dialogs
- [x] v0.1 cheats, item editor with enchantments, hero, buffs, bestiary, dungeon
- [x] Live state inspector (the "Info" tab) and cell inspection
- [ ] Hotkeys for individual cheats
- [ ] Automated release publishing

## License

The project is distributed under the **GNU GPL-3.0** (text in [LICENSE](LICENSE)), as a derivative work of Shattered Pixel Dungeon © Evan Debenham, which is itself based on Pixel Dungeon © Watabou (Oleg Dolya).

- Game: [00-Evan/shattered-pixel-dungeon](https://github.com/00-Evan/shattered-pixel-dungeon)
- Trainer author: [mcarabic98](https://github.com/mcarabic98)
