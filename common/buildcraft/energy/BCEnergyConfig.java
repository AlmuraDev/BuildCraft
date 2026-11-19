package buildcraft.energy;

import java.time.LocalDateTime;
import java.time.MonthDay;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.apache.logging.log4j.Level;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;

import buildcraft.lib.config.Property;

import buildcraft.api.core.BCLog;

import buildcraft.lib.config.EnumRestartRequirement;
import buildcraft.lib.misc.ConfigUtil;

import buildcraft.core.BCCoreConfig;

public class BCEnergyConfig {

    public static boolean enableOilOceanBiome;
    public static boolean enableOilDesertBiome;

    public static boolean enableOilGeneration;
    public static double oilWellGenerationRate;
    public static boolean enableOilSpouts;
    public static boolean enableOilBurn;
    public static boolean oilIsSticky;
    public static boolean enableRfEngine;
    public static boolean enableMjDynamo;

    public static int smallSpoutMinHeight;
    public static int smallSpoutMaxHeight;
    public static int largeSpoutMinHeight;
    public static int largeSpoutMaxHeight;

    public static double smallOilGenProb;
    public static double mediumOilGenProb;
    public static double largeOilGenProb;

    /** Dimensions, from the config's 1.12 ids (-1 nether, 0 overworld, 1 end) or 26.3 dimension names. */
    public static final Set<Identifier> excludedDimensions = new HashSet<>();
    /** If false then {@link #excludedDimensions} should be treated as a whitelist rather than a blacklist. */
    public static boolean excludedDimensionsIsBlackList;
    public static final Set<Identifier> excessiveBiomes = new HashSet<>();
    public static final Set<Identifier> surfaceDepositBiomes = new HashSet<>();
    public static final Set<Identifier> excludedBiomes = new HashSet<>();
    /** If false then {@link #excludedBiomes} should be treated as a whitelist rather than a blacklist. */
    public static boolean excludedBiomesIsBlackList;
    public static SpecialEventType christmasEventStatus = SpecialEventType.DAY_ONLY;

    private static Property propEnableOilOceanBiome;
    private static Property propEnableOilDesertBiome;

    private static Property propEnableOilGeneration;
    private static Property propOilWellGenerationRate;
    private static Property propEnableOilSpouts;
    private static Property propEnableOilBurn;
    private static Property propOilIsSticky;
    private static Property propEnableRfEngine;
    private static Property propEnableMjDynamo;

    private static Property propSmallSpoutMinHeight;
    private static Property propSmallSpoutMaxHeight;
    private static Property propLargeSpoutMinHeight;
    private static Property propLargeSpoutMaxHeight;

    private static Property propSmallOilGenProb;
    private static Property propMediumOilGenProb;
    private static Property propLargeOilGenProb;

    private static Property propExcessiveBiomes;
    private static Property propSurfaceDepositBiomes;
    private static Property propExcludedBiomes;
    private static Property propExcludedBiomesIsBlacklist;
    private static Property propExcludedDimensions;
    private static Property propExcludedDimensionsIsBlacklist;
    private static Property propChristmasEventType;

    public static void preInit() {
        EnumRestartRequirement world = EnumRestartRequirement.WORLD;
        EnumRestartRequirement game = EnumRestartRequirement.GAME;

        propEnableOilOceanBiome = BCCoreConfig.config.get("worldgen.oil", "oil_ocean_biome", true,
            "Should Oil Ocean biomes generate at all?");
        propEnableOilDesertBiome = BCCoreConfig.config.get("worldgen.oil", "oil_desert_biome", true,
            "Should Oil Desert biomes generate at all?");

        propEnableOilGeneration = BCCoreConfig.config.get("worldgen.oil", "enable", true,
            "Should any oil sprouts or lakes be generated at all?");
        propEnableOilBurn = BCCoreConfig.config.get("worldgen.oil", "can_burn", true, "Can oil blocks burn?");
        propOilIsSticky = BCCoreConfig.config.get("general","oilIsDense", false, "Should oil be dense and drag entities down?");
        propEnableRfEngine = BCCoreConfig.config.get("general","enableRfEngine", false, "Enable the RF to MJ engine?");
        propEnableMjDynamo = BCCoreConfig.config.get("general","enableMjDynamo", false, "Enable the MJ to RF dynamo / engine / generator?");

        propOilWellGenerationRate =
            BCCoreConfig.config.get("worldgen.oil", "generationRate", 1.0, "The rate of occurrence of oil wells.");

        propSmallOilGenProb = BCCoreConfig.config.get("worldgen.oil.spawn_probability", "small", 2.0,
            "The percentage probability of a small oil spawn");
        propMediumOilGenProb = BCCoreConfig.config.get("worldgen.oil.spawn_probability", "medium", 0.1,
            "The percentage probability of a medium oil spawn");
        propLargeOilGenProb = BCCoreConfig.config.get("worldgen.oil.spawn_probability", "large", 0.04,
            "The percentage probability of a large oil spawn");

        propEnableOilSpouts = BCCoreConfig.config.get("worldgen.oil.spouts", "enable", true,
            "Whether oil spouts are generated or not. The oil spring at the bottom of large lakes will still exist.");

        propSmallSpoutMinHeight = BCCoreConfig.config.get("worldgen.oil.spouts", "small_min_height", 6,
            "The minimum height for small oil spouts");
        propSmallSpoutMaxHeight = BCCoreConfig.config.get("worldgen.oil.spouts", "small_max_height", 12,
            "The maximum height for small oil spouts");

        propLargeSpoutMinHeight = BCCoreConfig.config.get("worldgen.oil.spouts", "large_min_height", 10,
            "The minimum height for large oil spouts");
        propLargeSpoutMaxHeight = BCCoreConfig.config.get("worldgen.oil.spouts", "large_max_height", 20,
            "The maximum height for large oil spouts");

        game.setTo(propEnableOilOceanBiome);
        game.setTo(propEnableOilDesertBiome);
        game.setTo(propEnableOilGeneration);
        game.setTo(propOilWellGenerationRate);
        game.setTo(propEnableOilBurn);
        game.setTo(propEnableRfEngine);
        game.setTo(propEnableMjDynamo);
        game.setTo(propSmallOilGenProb);
        game.setTo(propMediumOilGenProb);
        game.setTo(propLargeOilGenProb);
        game.setTo(propEnableOilSpouts);
        game.setTo(propSmallSpoutMinHeight);
        game.setTo(propSmallSpoutMaxHeight);
        game.setTo(propLargeSpoutMinHeight);
        game.setTo(propLargeSpoutMaxHeight);

        String[] _excessive = { //
            BCEnergy.MODID + ":oil_desert", //
            BCEnergy.MODID + ":oil_ocean", //
        };
        propExcessiveBiomes = BCCoreConfig.config.get("worldgen.oil", "excessiveBiomes", _excessive,
            "Biome registry names (e.g. 'minecraft:ocean','minecraft:plains')"
                + " of biomes that should have GREATLY increased oil generation rates.");

        String[] _surface = {};
        propSurfaceDepositBiomes = BCCoreConfig.config.get("worldgen.oil", "surfaceDepositBiomes", _surface,
            "Biome registry names (e.g. 'minecraft:ocean','minecraft:hills')"
                + " of biomes that should have slightly increased oil generation rates.");

        String[] _excluded = { "minecraft:nether_wastes", "minecraft:the_end", };
        propExcludedBiomes = BCCoreConfig.config.get("worldgen.oil", "excludedBiomes", _excluded,
            "Biome registry names (e.g. 'minecraft:hell','minecraft:jungle') of biomes that should never generate oil.");

        propExcludedBiomesIsBlacklist = BCCoreConfig.config.get("worldgen.oil", "excludedBiomesIsBlacklist", true,
            "If true then the excluded biomes list will be treated as a blacklist, otherwise it will be treated as a whitelist.");

        String[] _dims = { "minecraft:the_nether", "minecraft:the_end" };
        propExcludedDimensions = BCCoreConfig.config.get("worldgen.oil", "excludedDimensions", _dims,
            "Dimension names (e.g. 'minecraft:the_nether') of dimensions that should never generate oil."
                + " The 1.12 ids -1, 0 and 1 still mean the nether, the overworld and the end.");

        propExcludedDimensionsIsBlacklist = BCCoreConfig.config.get("worldgen.oil", "excludedDimensionsIsBlacklist", true,
            "If true then the excluded dimenstions list will be treated as a blacklist, otherwise it will be treated as a whitelist.");

        world.setTo(propExcessiveBiomes);
        world.setTo(propSurfaceDepositBiomes);
        world.setTo(propExcludedBiomes);
        world.setTo(propExcludedBiomesIsBlacklist);
        world.setTo(propExcludedDimensions);
        world.setTo(propExcludedDimensionsIsBlacklist);

        propChristmasEventType =
            BCCoreConfig.config.get("events", "christmas_chocolate", SpecialEventType.DAY_ONLY.lowerCaseName);
        ConfigUtil.setEnumProperty(propChristmasEventType, SpecialEventType.values());
        game.setTo(propChristmasEventType);

        // (1.12.2 moved pre-7.99 "worldgen" keys into "worldgen.oil" here; those configs are long gone.)

        reloadConfig(EnumRestartRequirement.GAME);
        BCCoreConfig.addReloadListener(BCEnergyConfig::reloadConfig);
    }

    public static void reloadConfig(EnumRestartRequirement restarted) {
        if (EnumRestartRequirement.WORLD.hasBeenRestarted(restarted)) {

            addBiomeNames(propExcludedBiomes, excludedBiomes);
            addBiomeNames(propExcessiveBiomes, excessiveBiomes);
            addBiomeNames(propSurfaceDepositBiomes, surfaceDepositBiomes);
            excludedDimensions.clear();
            for (String dim : propExcludedDimensions.getStringList()) {
                Identifier id = parseDimension(dim.trim());
                if (id != null) {
                    excludedDimensions.add(id);
                }
            }
            excludedBiomesIsBlackList = propExcludedBiomesIsBlacklist.getBoolean();
            excludedDimensionsIsBlackList = propExcludedDimensionsIsBlacklist.getBoolean();

            if (EnumRestartRequirement.GAME.hasBeenRestarted(restarted)) {
                enableOilOceanBiome = propEnableOilOceanBiome.getBoolean();
                enableOilDesertBiome = propEnableOilDesertBiome.getBoolean();

                enableOilGeneration = propEnableOilGeneration.getBoolean();
                oilWellGenerationRate = propOilWellGenerationRate.getDouble();
                enableOilSpouts = propEnableOilSpouts.getBoolean();
                enableOilBurn = propEnableOilBurn.getBoolean();
                oilIsSticky = propOilIsSticky.getBoolean();
                enableRfEngine = propEnableRfEngine.getBoolean();
                enableMjDynamo = propEnableMjDynamo.getBoolean();

                smallSpoutMinHeight = propSmallSpoutMinHeight.getInt();
                smallSpoutMaxHeight = propSmallSpoutMaxHeight.getInt();
                largeSpoutMinHeight = propLargeSpoutMinHeight.getInt();
                largeSpoutMaxHeight = propLargeSpoutMaxHeight.getInt();

                smallOilGenProb = propSmallOilGenProb.getDouble() / 100;
                mediumOilGenProb = propMediumOilGenProb.getDouble() / 100;
                largeOilGenProb = propLargeOilGenProb.getDouble() / 100;

                christmasEventStatus = ConfigUtil.parseEnumForConfig(propChristmasEventType, SpecialEventType.DAY_ONLY);
            }
        }
    }

    private static Identifier parseDimension(String dim) {
        switch (dim) {
            case "-1":
                return Identifier.withDefaultNamespace("the_nether");
            case "0":
                return Identifier.withDefaultNamespace("overworld");
            case "1":
                return Identifier.withDefaultNamespace("the_end");
            default:
                Identifier id = Identifier.tryParse(dim);
                if (id == null || dim.matches("-?[0-9]+")) {
                    BCLog.logger.warn("[energy.config] Unknown dimension '" + dim + "' in excludedDimensions (1.12"
                        + " numeric ids other than -1, 0 and 1 have no 26.3 name)");
                    return null;
                }
                return id;
        }
    }

    /** @return True if oil may generate in the given dimension. */
    public static boolean isDimensionAllowed(ResourceKey<net.minecraft.world.level.Level> dimension) {
        boolean excluded = excludedDimensions.contains(dimension.identifier());
        return excluded != excludedDimensionsIsBlackList;
    }

    /** 1.12.2 names of vanilla biomes that a config may still hold. */
    private static Identifier convertLegacyBiome(Identifier id) {
        if (!"minecraft".equals(id.getNamespace())) {
            return id;
        }
        switch (id.getPath()) {
            case "hell":
                return Identifier.withDefaultNamespace("nether_wastes");
            case "sky":
                return Identifier.withDefaultNamespace("the_end");
            default:
                return id;
        }
    }

    private static void addBiomeNames(Property prop, Set<Identifier> set) {
        set.clear();
        for (String s : prop.getStringList()) {
            Identifier id = Identifier.tryParse(s.trim());
            if (id != null) {
                set.add(convertLegacyBiome(id));
            }
        }
    }

    /** Called when a server starts (biomes are data driven on 26.3, so they are only known then). */
    public static void validateBiomeNames(MinecraftServer server) {
        Registry<net.minecraft.world.level.biome.Biome> biomes =
            server.registryAccess().lookupOrThrow(Registries.BIOME);
        Set<Identifier> invalids = new HashSet<>();
        addInvalidBiomeNames(biomes, excessiveBiomes, invalids);
        addInvalidBiomeNames(biomes, excludedBiomes, invalids);
        addInvalidBiomeNames(biomes, surfaceDepositBiomes, invalids);

        if (invalids.isEmpty()) {
            return;
        }

        List<Identifier> invalidList = new ArrayList<>();
        invalidList.addAll(invalids);
        Collections.sort(invalidList, Comparator.comparing(Identifier::toString));

        List<Identifier> allValid = new ArrayList<>();
        allValid.addAll(biomes.keySet());
        Collections.sort(allValid, Comparator.comparing(Identifier::toString));

        BCLog.logger.warn("****************************************************");
        BCLog.logger.warn("*");
        BCLog.logger.warn("* Unknown biome name detected in buildcraft config!");
        BCLog.logger.warn("* (Config file = " + BCCoreConfig.config.getConfigFile().getAbsolutePath() + ")");
        BCLog.logger.warn("*");
        BCLog.logger.warn("* Unknown biomes: ");
        printList(Level.WARN, invalidList);
        BCLog.logger.warn("*");
        BCLog.logger.info("* All possible known names: ");
        printList(Level.INFO, allValid);
        BCLog.logger.info("*");
        BCLog.logger.warn("****************************************************");
    }

    private static void printList(Level level, List<Identifier> list) {
        for (Identifier location : list) {
            BCLog.logger.log(level, "*    - " + location);
        }
    }

    private static void addInvalidBiomeNames(Registry<net.minecraft.world.level.biome.Biome> biomes,
        Set<Identifier> toTest, Set<Identifier> invalidDest) {
        for (Identifier test : toTest) {
            if (!biomes.containsKey(test)) {
                invalidDest.add(test);
            }
        }
    }

    public enum SpecialEventType {
        DISABLED,
        DAY_ONLY,
        MONTH,
        ENABLED;

        public final String lowerCaseName = name().toLowerCase(Locale.ROOT);

        public boolean isEnabled(MonthDay date) {
            if (this == DISABLED) {
                return false;
            }
            if (this == ENABLED) {
                return true;
            }
            LocalDateTime now = LocalDateTime.now();
            if (now.getMonth() != date.getMonth()) {
                return false;
            }
            if (this == MONTH) {
                return true;
            }
            int thisDay = now.getDayOfMonth();
            int wantedDay = date.getDayOfMonth();
            return thisDay >= wantedDay - 1 && thisDay <= wantedDay + 1;
        }
    }
}
