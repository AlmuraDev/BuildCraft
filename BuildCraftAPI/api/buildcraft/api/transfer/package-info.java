/** BuildCraft's own item and fluid handler interfaces, shaped like Forge 1.12's (slot based items; fill/drain fluids with a
 * do/simulate flag). BuildCraft's machines, pipes and tanks keep working through these; adapters in buildcraft lib turn
 * them into NeoForge's transaction based resource handlers (and back) wherever BuildCraft meets other blocks. */
@javax.annotation.ParametersAreNonnullByDefault
package buildcraft.api.transfer;
