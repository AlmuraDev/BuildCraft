"""
Second-pass rewrites for code that rename.py has already been over: recurring 1.12.2 idioms that need a 26.3
replacement which rename.py's simple renames can't express. Run it on a module after rename.py, then compile:

    python tools/port-26.3/common_fixes.py common/buildcraft/<module>

Every rule is idempotent. Imports for CapUtil / FluidUtilBC / NbtStacks are added by imports.py afterwards.
"""
import os
import re
import sys

RULES = [
    # Capabilities on tiles: BuildCraft's sided capability lookup
    (r'\b(\w+)\.getCapability\(((?:CapUtil|MjAPI|PipeApi)\.CAP_\w+), ', r'CapUtil.getCapability(\1, \2, '),
    (r'\b(\w+)\.hasCapability\(((?:CapUtil|MjAPI|PipeApi)\.CAP_\w+), ((?:[^()]|\(\))*)\)',
     r'(CapUtil.getCapability(\1, \2, \3) != null)'),
    # Capability.cast(x) -> an unchecked cast to the method's T
    (r'\breturn ((?:CapUtil|MjAPI|PipeApi)\.CAP_\w+)\.cast\(', r'return (T) ('),
    # FluidStack (only on names that are FluidStacks in BuildCraft: plenty of BuildCraft classes have an int "amount")
    (r'\b(fluid|stack|toAdd|filter|extracted|target|resource|fluidToPush|drained|fs|currentFluid|liquid|toFill|contained)\.amount\s*=(?!=)\s*([^;\n]+);',
     r'\1.setAmount(\2);'),
    (r'\b(fluid|stack|toAdd|filter|extracted|target|resource|fluidToPush|drained|f|fs|current|currentFluid|liquid|drain|toFill|fill|contents|contained)\.amount\b',
     r'\1.getAmount()'),
    (r'\b(?!FluidUtilBC\b)(\w+)\.isFluidEqual\(', r'FluidUtilBC.isFluidEqual(\1, '),
    (r'\bnew FluidStack\((\w+), ([^()]+|[^()]*\([^()]*\)[^()]*)\)', r'\1.copyWithAmount(\2)'),
    (r'\bFluidStack\.loadFluidStackFromNBT\(', 'NbtStacks.readFluidStack('),
    (r'\bFluid\.BUCKET_VOLUME\b', 'net.neoforged.neoforge.fluids.FluidType.BUCKET_VOLUME'),
    (r'\.getLocalizedName\(\)', '.getHoverName().getString()'),
    # ItemStack NBT
    (r'\bnew ItemStack\(((?:\w+\.)*getCompoundOrEmpty\([^)]*\)|\w+Nbt|nbt)\)', r'NbtStacks.readItemStack(\1)'),
    (r'\b(stack|item|is)\.serializeNBT\(\)', r'NbtStacks.writeItemStack(\1)'),
    # Entities
    (r'\.isSneaking\(\)', '.isShiftKeyDown()'),
    (r'\b(\w+)\.isServerWorld\(\)', r'!\1.level().isClientSide()'),
    (r'\b(\w+)\.isDead\b', r'\1.isRemoved()'),
    (r'\b(\w+(?:\(\))?)\.getDistanceSqToCenter\(([^)]*\)?)\)', r'\1.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(\2))'),
    (r'\.getOwner\(\)\.getId\(\)', '.getOwner().id()'),
    (r'\bplayer\.inventory\.currentItem\b', 'player.getInventory().getSelectedSlot()'),
    (r'\bplayer\.inventory\b', 'player.getInventory()'),
    # Worlds
    (r'\.getPipeWorld\(\)\.rand\b', '.getPipeWorld().getRandom()'),
    (r'\b(world|getWorld\(\))\.rand\b', r'\1.getRandom()'),
    (r'\.sendBlockBreakProgress\(', '.destroyBlockProgress('),
    # Boxes / positions
    (r'\b(\w+)\.grow\(', r'\1.inflate('),
    (r'\bpos\.offset\((direction|face|side|facing|dir|d)\)', r'pos.relative(\1)'),
    (r'\.getPipePos\(\)\.offset\((direction|face|side|facing|dir|d)\)', r'.getPipePos().relative(\1)'),
    (r'\bTextComponentTranslation\b', 'net.minecraft.network.chat.MutableComponent'),
]


def fix(path):
    s = open(path, encoding='utf-8').read()
    o = s
    for a, b in RULES:
        s = re.sub(a, b, s)
    # AABB.offset(x, y, z) -> move(x, y, z) on obvious box variables
    s = re.sub(r'\b(bb|aabb|box)\.offset\(', r'\1.move(', s)
    if s != o:
        open(path, 'w', encoding='utf-8', newline='\n').write(s)
        return True
    return False


def main(paths):
    n = 0
    for p in paths:
        for dp, _, fs in os.walk(p):
            for fn in fs:
                if fn.endswith('.java') and fix(os.path.join(dp, fn)):
                    n += 1
    print('%d files changed' % n)


if __name__ == '__main__':
    main(sys.argv[1:])
