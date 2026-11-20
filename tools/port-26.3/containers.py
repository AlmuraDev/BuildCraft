"""
Container/screen pass: 26.3 containers are menus, built with their menu type and window id (BuildCraft's BCMenu
passes both), and screens are built from the menu:

  public ContainerFoo(Player player, TileFoo tile) { super(player, tile); ... }
      -> public ContainerFoo(MenuType<?> type, int containerId, Player player, TileFoo tile) {
             super(type, containerId, player, tile); ... }
  addSlotToContainer(...)          -> addSlot(...)
  canInteractWith(Player p)        -> stillValid(Player p)
  inventorySlots / slot.xPos/yPos  -> slots / slot.x/y
  public void initGui() { super.initGui(); ... }  -> protected void init() { super.init(); ... }

Run after rename.py:  python tools/port-26.3/containers.py <file-or-dir>...   (idempotent)
"""
import os
import re
import sys

RULES = [
    # (Player, Tile) container constructors
    (r'public (\w+)\(Player (\w+), ([\w.<>?]+) (\w+)\) \{(\s*)super\(\2, \4\);',
     r'public \1(MenuType<?> type, int containerId, Player \2, \3 \4) {\5super(type, containerId, \2, \4);'),
    # (Player) container constructors
    (r'public (Container\w+)\(Player (\w+)\) \{(\s*)super\(\2\);',
     r'public \1(MenuType<?> type, int containerId, Player \2) {\3super(type, containerId, \2);'),
    (r'\baddSlotToContainer\(', 'addSlot('),
    (r'public boolean canInteractWith\(Player ', 'public boolean stillValid(Player '),
    (r'\binventorySlots\b', 'slots'),
    (r'\.xPos\b', '.x'),
    (r'\.yPos\b', '.y'),
    (r'public void initGui\(\)', 'protected void init()'),
    (r'super\.initGui\(\);', 'super.init();'),
    (r'public ItemStack transferStackInSlot\(Player (\w+), int (\w+)\)', r'public ItemStack quickMoveStack(Player \1, int \2)'),
    (r'super\.transferStackInSlot\(', 'super.quickMoveStack('),
    (r'\bslot\.getStack\(\)', 'slot.getItem()'),
    (r'\bslot\.putStack\(', 'slot.set('),
    (r'\bslot\.getHasStack\(\)', 'slot.hasItem()'),
    (r'ItemStack\.areItemStacksEqual\(', 'ItemStack.matches('),
    (r'new ScaledResolution\((\w+)\)\.getScaleFactor\(\)', r'\1.getWindow().getGuiScale()'),
    (r'(\w+(?:\.\w+|\(\))*)\.getItemStackDisplayName\(([^;]*?)\)(?=;)', r'(\2).getHoverName().getString()'),
    (r'^import net\.minecraft\.client\.gui\.ScaledResolution;\n', ''),
    (r'public void updateScreen\(\)', 'protected void containerTick()'),
    (r'super\.updateScreen\(\);', 'super.containerTick();'),
]


def fix(text):
    for pattern, repl in RULES:
        text = re.sub(pattern, repl, text, flags=re.M)
    if 'MenuType<' in text and 'import net.minecraft.world.inventory.MenuType;' not in text:
        text = re.sub(r'^(package [\w.]+;\n)', r'\1\nimport net.minecraft.world.inventory.MenuType;\n', text, count=1,
                      flags=re.M)
    return text


def main(paths):
    changed = 0
    total = 0
    for root in paths:
        files = [root] if os.path.isfile(root) else [os.path.join(d, f) for d, _, fs in os.walk(root) for f in fs]
        for path in files:
            if not path.endswith('.java'):
                continue
            total += 1
            with open(path, encoding='utf-8') as f:
                text = f.read()
            new = fix(text)
            if new != text:
                with open(path, 'w', encoding='utf-8', newline='') as f:
                    f.write(new)
                changed += 1
    print('%d of %d files changed' % (changed, total))


if __name__ == '__main__':
    main(sys.argv[1:])
