package buildcraft.silicon;

import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.entity.BlockEntityType;

import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;

import buildcraft.api.transport.pipe.PipeApiClient;
import buildcraft.api.transport.pipe.PipeApiClient.IClientRegistry;
import buildcraft.api.transport.pluggable.IPluggableStaticBaker;

import buildcraft.lib.client.model.ModelHolderStatic;
import buildcraft.lib.client.model.ModelHolderVariable;
import buildcraft.lib.client.model.MutableQuad;
import buildcraft.lib.client.model.plug.PlugBakerSimple;
import buildcraft.lib.expression.DefaultContexts;
import buildcraft.lib.expression.FunctionContext;
import buildcraft.lib.expression.node.value.NodeVariableBoolean;
import buildcraft.lib.expression.node.value.NodeVariableObject;
import buildcraft.lib.misc.data.ModelVariableData;
import buildcraft.lib.registry.TileTypes;

import buildcraft.silicon.client.model.key.KeyPlugFacade;
import buildcraft.silicon.client.model.key.KeyPlugGate;
import buildcraft.silicon.client.model.key.KeyPlugLens;
import buildcraft.silicon.client.model.key.KeyPlugLightSensor;
import buildcraft.silicon.client.model.key.KeyPlugPulsar;
import buildcraft.silicon.client.model.key.KeyPlugTimer;
import buildcraft.silicon.client.model.plug.PlugBakerFacade;
import buildcraft.silicon.client.model.plug.PlugBakerLens;
import buildcraft.silicon.client.model.plug.PlugGateBaker;
import buildcraft.silicon.client.render.PlugGateRenderer;
import buildcraft.silicon.client.render.PlugPulsarRenderer;
import buildcraft.silicon.client.render.RenderLaser;
import buildcraft.silicon.client.render.RenderProgrammingTable;
import buildcraft.silicon.gate.GateVariant;
import buildcraft.silicon.plug.PluggableGate;
import buildcraft.silicon.plug.PluggablePulsar;

public class BCSiliconModels {
    public static final ModelHolderStatic LIGHT_SENSOR;
    public static final ModelHolderStatic TIMER;

    public static final ModelHolderVariable GATE_STATIC;
    public static final ModelHolderVariable GATE_DYNAMIC;
    private static final ModelVariableData GATE_VAR_DATA_STATIC = new ModelVariableData();

    private static final ModelHolderVariable LENS, FILTER;
    private static final NodeVariableBoolean LENS_HAS_COLOUR;
    private static final NodeVariableObject<DyeColor> LENS_COLOUR;
    private static final NodeVariableObject<Direction> LENS_SIDE;

    public static final ModelHolderStatic PULSAR_STATIC;
    public static final ModelHolderVariable PULSAR_DYNAMIC;

    public static final IPluggableStaticBaker<KeyPlugPulsar> BAKER_PLUG_PULSAR;
    public static final IPluggableStaticBaker<KeyPlugTimer> BAKER_PLUG_TIMER;
    public static final IPluggableStaticBaker<KeyPlugLightSensor> BAKER_PLUG_LIGHT_SENSOR;

    static {
        LIGHT_SENSOR = getStaticModel("plugs/light_sensor");
        TIMER = getStaticModel("plugs/timer");
        GATE_STATIC = getModel("plugs/gate", PluggableGate.MODEL_FUNC_CTX_STATIC);
        GATE_DYNAMIC = getModel("plugs/gate_dynamic", PluggableGate.MODEL_FUNC_CTX_DYNAMIC);
        PULSAR_STATIC = getStaticModel("plugs/pulsar_static");
        PULSAR_DYNAMIC = getModel("plugs/pulsar_dynamic", PluggablePulsar.MODEL_FUNC_CTX);

        BAKER_PLUG_PULSAR = new PlugBakerSimple<>(PULSAR_STATIC::getCutoutQuads);
        BAKER_PLUG_TIMER = new PlugBakerSimple<>(TIMER::getCutoutQuads);
        BAKER_PLUG_LIGHT_SENSOR = new PlugBakerSimple<>(LIGHT_SENSOR::getCutoutQuads);

        {
            FunctionContext fnCtx = DefaultContexts.createWithAll();
            LENS_COLOUR = fnCtx.putVariableObject("colour", DyeColor.class);
            LENS_SIDE = fnCtx.putVariableObject("side", Direction.class);
            LENS_HAS_COLOUR = fnCtx.putVariableBoolean("has_colour");
            LENS = getModel("plugs/lens", fnCtx);
            FILTER = getModel("plugs/filter", fnCtx);
        }
    }

    private static ModelHolderStatic getStaticModel(String str) {
        return new ModelHolderStatic("buildcraftsilicon:models/" + str + ".json");
    }

    private static ModelHolderVariable getModel(String str, FunctionContext fnCtx) {
        return new ModelHolderVariable("buildcraftsilicon:models/" + str + ".json", fnCtx);
    }

    public static void fmlPreInit() {
        BCSilicon.MOD_BUS.addListener(BCSiliconModels::registerRenderers);
        BCSilicon.MOD_BUS.addListener((ModelEvent.BakingCompleted event) -> onModelBake());
    }

    public static void fmlInit() {
        IClientRegistry pipeRegistryClient = PipeApiClient.registry;
        if (pipeRegistryClient != null) {
            pipeRegistryClient.registerBaker(KeyPlugGate.class, PlugGateBaker.INSTANCE);
            pipeRegistryClient.registerBaker(KeyPlugPulsar.class, BAKER_PLUG_PULSAR);
            pipeRegistryClient.registerBaker(KeyPlugLightSensor.class, BAKER_PLUG_LIGHT_SENSOR);
            pipeRegistryClient.registerBaker(KeyPlugTimer.class, BAKER_PLUG_TIMER);
            pipeRegistryClient.registerBaker(KeyPlugLens.class, PlugBakerLens.INSTANCE);
            pipeRegistryClient.registerBaker(KeyPlugFacade.class, PlugBakerFacade.INSTANCE);

            pipeRegistryClient.registerRenderer(PluggableGate.class, PlugGateRenderer.INSTANCE);
            pipeRegistryClient.registerRenderer(PluggablePulsar.class, PlugPulsarRenderer.INSTANCE);
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        if (BCSiliconBlocks.laser != null) {
            BlockEntityType type = TileTypes.forState(BCSiliconBlocks.laser.defaultBlockState());
            event.registerBlockEntityRenderer(type, ctx -> new RenderLaser());
        }
        if (BCSiliconBlocks.programmingTable != null) {
            BlockEntityType type = TileTypes.forState(BCSiliconBlocks.programmingTable.defaultBlockState());
            event.registerBlockEntityRenderer(type, ctx -> new RenderProgrammingTable());
        }
    }

    /** The variable models were reloaded: forget everything baked from them. TODO(26.3): the gate, lens, pulsar,
     * light sensor, timer and facade items used models baked from these (ModelGateItem, ModelLensItem,
     * ModelPluggableItem, ModelFacadeItem); they show flat item models until they have special item renderers. */
    private static void onModelBake() {
        PlugGateBaker.onModelBake();
        PlugBakerLens.onModelBake();
        PlugPulsarRenderer.onModelBake();
        PlugGateRenderer.onModelBake();
    }

    public static MutableQuad[] getGateStaticQuads(Direction side, GateVariant variant) {
        PluggableGate.setClientModelVariables(side, variant);
        if (GATE_VAR_DATA_STATIC.hasNoNodes()) {
            GATE_VAR_DATA_STATIC.setNodes(GATE_STATIC.createTickableNodes());
        }
        GATE_VAR_DATA_STATIC.refresh();
        return GATE_STATIC.getCutoutQuads();
    }

    private static void setupLensVariables(ModelHolderVariable model, Direction side, DyeColor colour) {
        LENS_COLOUR.value = colour == null ? DyeColor.WHITE : colour;
        LENS_SIDE.value = side;
        LENS_HAS_COLOUR.value = colour != null;
        ModelVariableData varData = new ModelVariableData();
        varData.setNodes(model.createTickableNodes());
        varData.tick();
        varData.refresh();
    }

    public static MutableQuad[] getLensCutoutQuads(Direction side, DyeColor colour) {
        setupLensVariables(LENS, side, colour);
        return LENS.getCutoutQuads();
    }

    public static MutableQuad[] getLensTranslucentQuads(Direction side, DyeColor colour) {
        setupLensVariables(LENS, side, colour);
        return LENS.getTranslucentQuads();
    }

    public static MutableQuad[] getFilterCutoutQuads(Direction side, DyeColor colour) {
        setupLensVariables(FILTER, side, colour);
        return FILTER.getCutoutQuads();
    }

    public static MutableQuad[] getFilterTranslucentQuads(Direction side, DyeColor colour) {
        setupLensVariables(FILTER, side, colour);
        return FILTER.getTranslucentQuads();
    }
}
